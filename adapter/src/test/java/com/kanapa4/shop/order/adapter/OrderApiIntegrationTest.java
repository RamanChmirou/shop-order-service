package com.kanapa4.shop.order.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.jayway.jsonpath.JsonPath;
import com.kanapa4.shop.order.domain.port.OrderRepositoryPort;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.exception.OrderConcurrentModificationException;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;
import com.kanapa4.shop.order.pub.type.DeliveryMethod;
import com.kanapa4.shop.order.pub.type.PaymentMethod;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.openfeign.client.config.cart-service.url=${wiremock.server.baseUrl}",
        "outbox.relay-delay=300"
})
@AutoConfigureMockMvc
@EnableWireMock
@Testcontainers
class OrderApiIntegrationTest {

    private static final String TOPIC = "order.placed";
    private static final BigDecimal CART_TOTAL = new BigDecimal("10398.00");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    // ---------- checkout ----------

    @Test
    void checkout_ValidRequest_CreatesOrderAndPublishesEvent() throws Exception {
        UUID userId = UUID.randomUUID();
        stubCart(userId, 2);

        MvcResult result = mockMvc.perform(post("/orders/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(CART_TOTAL, PaymentMethod.CARD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.grandTotal").value(10413.00))
                .andExpect(jsonPath("$.items[0].lineTotal.amount").value(10398.00))
                .andReturn();

        String orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String orderNumber = JsonPath.read(result.getResponse().getContentAsString(), "$.orderNumber");
        String event = awaitEventFor(orderId);
        assertThat(event).contains(orderNumber).contains(userId.toString());
    }

    @Test
    void checkout_PriceChangedSinceUserSawIt_Returns409AndCreatesNothing() throws Exception {
        UUID userId = UUID.randomUUID();
        stubCart(userId, 2);

        mockMvc.perform(post("/orders/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(new BigDecimal("10000.00"), PaymentMethod.CARD)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/orders/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void checkout_EmptyCart_Returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        stubEmptyCart(userId);

        mockMvc.perform(post("/orders/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(BigDecimal.ONE, PaymentMethod.CARD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_CartServiceDown_Returns503() throws Exception {
        UUID userId = UUID.randomUUID();
        wireMock.stubFor(post(urlPathEqualTo(repriceUrl(userId))).willReturn(serverError()));

        mockMvc.perform(post("/orders/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(CART_TOTAL, PaymentMethod.CARD)))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------- payment ----------

    @Test
    void pay_NewOrder_MarksPaidAndIssuesInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderId = createOrder(userId, PaymentMethod.CARD);

        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.invoice.status").value("ISSUED"));
    }

    @Test
    void pay_DeclinedPaymentMethod_Returns402AndOrderStaysNew() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderId = createOrder(userId, PaymentMethod.TRANSFER);

        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, orderId))
                .andExpect(status().isPaymentRequired());

        mockMvc.perform(get("/orders/{userId}/{orderId}", userId, orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void pay_AlreadyPaid_Returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderId = createOrder(userId, PaymentMethod.CARD);
        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, orderId))
                .andExpect(status().isConflict());
    }

    // ---------- lifecycle ----------

    @Test
    void cancel_NewOrder_CancelsItAndPaidOrderCannotBeCancelled() throws Exception {
        UUID userId = UUID.randomUUID();
        String cancelled = createOrder(userId, PaymentMethod.CARD);
        String paid = createOrder(userId, PaymentMethod.CARD);
        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, paid)).andExpect(status().isOk());

        mockMvc.perform(post("/orders/{userId}/{orderId}/cancel", userId, cancelled))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/orders/{userId}/{orderId}/cancel", userId, paid))
                .andExpect(status().isConflict());
    }

    @Test
    void ship_PaidOrder_Ships() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderId = createOrder(userId, PaymentMethod.CARD);
        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", userId, orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/orders/{orderId}/ship", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void getOrder_OfAnotherUser_Returns404() throws Exception {
        UUID owner = UUID.randomUUID();
        String orderId = createOrder(owner, PaymentMethod.CARD);

        mockMvc.perform(get("/orders/{userId}/{orderId}", UUID.randomUUID(), orderId))
                .andExpect(status().isNotFound());
    }

    // ---------- list, pagination, sorting ----------

    @Test
    void getOrders_PaginationAndSorting_ReturnsPagesInRequestedOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        String first = createOrder(userId, PaymentMethod.CARD);
        String second = createOrder(userId, PaymentMethod.CARD);
        String third = createOrder(userId, PaymentMethod.CARD);

        mockMvc.perform(get("/orders/{userId}", userId)
                        .param("page", "0").param("size", "2")
                        .param("sortBy", "createdAt").param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(third))
                .andExpect(jsonPath("$.content[1].id").value(second));

        mockMvc.perform(get("/orders/{userId}", userId)
                        .param("page", "1").param("size", "2")
                        .param("sortBy", "createdAt").param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(first));
    }

    @Test
    void getOrders_UnsupportedSortField_Returns400() throws Exception {
        mockMvc.perform(get("/orders/{userId}", UUID.randomUUID()).param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    // ---------- optimistic locking ----------

    @Test
    void save_StaleOrder_ThrowsConcurrentModification() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.fromString(createOrder(userId, PaymentMethod.CARD));
        Order firstCopy = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow();
        Order staleCopy = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow();

        firstCopy.cancel();
        orderRepository.save(firstCopy);
        staleCopy.cancel();

        assertThatThrownBy(() -> orderRepository.save(staleCopy))
                .isInstanceOf(OrderConcurrentModificationException.class);
    }

    // ---------- helpers ----------

    private String createOrder(UUID userId, PaymentMethod paymentMethod) throws Exception {
        stubCart(userId, 2);
        MvcResult result = mockMvc.perform(post("/orders/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(CART_TOTAL, paymentMethod)))
                .andExpect(status().isCreated())
                .andReturn();
        Thread.sleep(5); // distinct createdAt for the sorting test
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String checkoutBody(BigDecimal expectedTotal, PaymentMethod paymentMethod) {
        CheckoutRequest request = new CheckoutRequest(
                new CustomerRequest("Jan", "Kowalski", "jan@example.com", "+48123456789"),
                DeliveryMethod.COURIER,
                paymentMethod,
                new AddressRequest("Marszalkowska 1", "Warszawa", "00-001", "PL"),
                null,
                expectedTotal);
        return objectMapper.writeValueAsString(request);
    }

    private String repriceUrl(UUID userId) {
        return "/carts/" + userId + "/reprice";
    }

    private void stubCart(UUID userId, int quantity) {
        wireMock.stubFor(post(urlPathEqualTo(repriceUrl(userId))).willReturn(okJson("""
                {
                  "cart": {
                    "id": "%s",
                    "userId": "%s",
                    "status": "ACTIVE",
                    "items": [{
                      "id": "%s",
                      "productId": "%s",
                      "productName": "Laptop Pro 15",
                      "unitPrice": {"amount": 5199.00, "currency": "PLN", "vatRate": "VAT_23"},
                      "quantity": %d,
                      "lineTotal": {"amount": 10398.00, "currency": "PLN", "vatRate": "VAT_23"},
                      "selectedOptions": [
                        {"groupCode": "RAM", "valueCode": "RAM_32", "valueLabel": "32 GB", "priceDelta": 400.00}
                      ]
                    }],
                    "itemCount": %d,
                    "totalAmount": 10398.00,
                    "currency": "PLN"
                  },
                  "changed": false,
                  "changes": []
                }
                """.formatted(UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(),
                quantity, quantity))));
    }

    private void stubEmptyCart(UUID userId) {
        wireMock.stubFor(post(urlPathEqualTo(repriceUrl(userId))).willReturn(okJson("""
                {
                  "cart": {"id": "%s", "userId": "%s", "status": "ACTIVE", "items": [],
                           "itemCount": 0, "totalAmount": 0, "currency": "PLN"},
                  "changed": false,
                  "changes": []
                }
                """.formatted(UUID.randomUUID(), userId))));
    }

    private String awaitEventFor(String orderId) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(TOPIC));
            Instant deadline = Instant.now().plusSeconds(20);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (orderId.equals(record.key())) {
                        return record.value();
                    }
                }
            }
        }
        throw new AssertionError("No order.placed event for order " + orderId);
    }
}