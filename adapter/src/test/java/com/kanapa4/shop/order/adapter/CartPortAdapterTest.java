package com.kanapa4.shop.order.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.exception.CartServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.EmptyCartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(
        classes = CartPortAdapterTest.TestConfig.class,
        properties = "spring.cloud.openfeign.client.config.cart-service.url=${wiremock.server.baseUrl}")
@EnableWireMock
class CartPortAdapterTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @EnableFeignClients(clients = CartClient.class)
    @Import({CartPortAdapter.class, CartClientFallbackFactory.class, CartClientMapperImpl.class})
    static class TestConfig {
    }

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String URL = "/carts/" + USER_ID + "/reprice";

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private CartPortAdapter cartPort;

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @Test
    void shouldReturnSnapshot_whenCartServiceResponds() {
        wireMock.stubFor(post(urlPathEqualTo(URL)).willReturn(okJson(CART_JSON)));

        CartSnapshot snapshot = cartPort.repriceCart(USER_ID);

        assertThat(snapshot.userId()).isEqualTo(USER_ID);
        assertThat(snapshot.items()).hasSize(1);
        assertThat(snapshot.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(snapshot.currency()).isEqualTo("PLN");
    }

    @Test
    void shouldThrowEmptyCart_whenCartNotFound() {
        wireMock.stubFor(post(urlPathEqualTo(URL)).willReturn(notFound()));

        assertThatThrownBy(() -> cartPort.repriceCart(USER_ID))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void shouldRetryAndSucceed_whenServerFailsOnce() {
        wireMock.stubFor(post(urlPathEqualTo(URL)).inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(serverError())
                .willSetStateTo("recovered"));
        wireMock.stubFor(post(urlPathEqualTo(URL)).inScenario("retry")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(CART_JSON)));

        CartSnapshot snapshot = cartPort.repriceCart(USER_ID);

        assertThat(snapshot.items()).hasSize(1);
        wireMock.verify(2, postRequestedFor(urlPathEqualTo(URL)));
    }

    @Test
    void shouldThrowUnavailable_whenServerAlwaysFails() {
        wireMock.stubFor(post(urlPathEqualTo(URL)).willReturn(serverError()));

        assertThatThrownBy(() -> cartPort.repriceCart(USER_ID))
                .isInstanceOf(CartServiceUnavailableException.class);
        wireMock.verify(3, postRequestedFor(urlPathEqualTo(URL)));
    }

    @Test
    void shouldThrowUnavailable_whenConnectionReset() {
        wireMock.stubFor(post(urlPathEqualTo(URL))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> cartPort.repriceCart(USER_ID))
                .isInstanceOf(CartServiceUnavailableException.class);
    }

    private static final String CART_JSON = """
            {
              "cart": {
                "id": "22222222-2222-2222-2222-222222222222",
                "userId": "11111111-1111-1111-1111-111111111111",
                "status": "ACTIVE",
                "items": [{
                  "id": "33333333-3333-3333-3333-333333333333",
                  "productId": "44444444-4444-4444-4444-444444444444",
                  "productName": "Laptop",
                  "unitPrice": {"amount": 100.00, "currency": "PLN", "vatRate": "VAT_23"},
                  "quantity": 2,
                  "lineTotal": {"amount": 200.00, "currency": "PLN", "vatRate": "VAT_23"},
                  "selectedOptions": []
                }],
                "itemCount": 2,
                "totalAmount": 200.00,
                "currency": "PLN"
              },
              "changed": false
            }
            """;
}