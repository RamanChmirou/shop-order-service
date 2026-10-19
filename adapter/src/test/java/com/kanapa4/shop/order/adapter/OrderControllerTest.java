package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.service.OrderService;
import com.kanapa4.shop.order.model.OrderStatus;
import com.kanapa4.shop.order.model.exception.CartServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.EmptyCartException;
import com.kanapa4.shop.order.model.exception.InvalidOrderStateException;
import com.kanapa4.shop.order.model.exception.InvoiceAlreadyIssuedException;
import com.kanapa4.shop.order.model.exception.OrderDoesNotExistException;
import com.kanapa4.shop.order.model.exception.PriceChangedException;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import com.kanapa4.shop.order.pub.page.SortDirection;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.type.DeliveryMethod;
import com.kanapa4.shop.order.pub.type.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private String validCheckoutJson() {
        CheckoutRequest request = new CheckoutRequest(
                new CustomerRequest("Jan", "Kowalski", "jan@example.com", "+48123456789"),
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                new AddressRequest("Marszalkowska 1", "Warszawa", "00-001", "PL"),
                null,
                new BigDecimal("10398.00"));
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void checkout_ValidRequest_Returns201AndCallsService() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CheckoutRequest.class))).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isCreated());

        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(orderService).checkout(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().expectedTotal()).isEqualByComparingTo("10398.00");
        assertThat(captor.getValue().deliveryMethod()).isEqualTo(DeliveryMethod.COURIER);
    }

    @Test
    void checkout_EmptyBody_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.customer").exists())
                .andExpect(jsonPath("$.details.expectedTotal").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void checkout_BrokenJson_Returns400() throws Exception {
        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body or parameter"));
    }

    @Test
    void checkout_PriceChanged_Returns409WithBothTotals() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CheckoutRequest.class)))
                .thenThrow(new PriceChangedException(new BigDecimal("10000.00"), new BigDecimal("10398.00")));

        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.expectedTotal").value(10000.00))
                .andExpect(jsonPath("$.details.actualTotal").value(10398.00));
    }

    @Test
    void checkout_EmptyCart_Returns400() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CheckoutRequest.class)))
                .thenThrow(new EmptyCartException(USER_ID));

        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_CartServiceDown_Returns503() throws Exception {
        when(orderService.checkout(eq(USER_ID), any(CheckoutRequest.class)))
                .thenThrow(new CartServiceUnavailableException());

        mockMvc.perform(post("/orders/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void getOrders_NoParams_UsesDefaultPageable() throws Exception {
        Pageable expected = new Pageable(0, 20, "createdAt", SortDirection.DESC);
        when(orderService.getOrders(USER_ID, expected)).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/orders/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isArray());

        verify(orderService).getOrders(USER_ID, expected);
    }

    @Test
    void getOrders_CustomParams_PassesThemToService() throws Exception {
        Pageable expected = new Pageable(1, 5, "grandTotal", SortDirection.ASC);
        when(orderService.getOrders(USER_ID, expected)).thenReturn(new Page<>(List.of(), 1, 5, 0, 0));

        mockMvc.perform(get("/orders/{userId}", USER_ID)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "grandTotal")
                        .param("direction", "ASC"))
                .andExpect(status().isOk());

        verify(orderService).getOrders(USER_ID, expected);
    }

    @Test
    void getOrder_Existing_Returns200() throws Exception {
        when(orderService.getOrder(USER_ID, ORDER_ID)).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(get("/orders/{userId}/{orderId}", USER_ID, ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void getOrder_Unknown_Returns404() throws Exception {
        when(orderService.getOrder(USER_ID, ORDER_ID)).thenThrow(new OrderDoesNotExistException(ORDER_ID));

        mockMvc.perform(get("/orders/{userId}/{orderId}", USER_ID, ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getOrder_InvalidUuid_Returns400() throws Exception {
        mockMvc.perform(get("/orders/{userId}/{orderId}", USER_ID, "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_NewOrder_Returns200() throws Exception {
        when(orderService.pay(USER_ID, ORDER_ID)).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", USER_ID, ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void pay_AlreadyPaid_Returns409() throws Exception {
        when(orderService.pay(USER_ID, ORDER_ID))
                .thenThrow(new InvalidOrderStateException(OrderStatus.PAID, "pay"));

        mockMvc.perform(post("/orders/{userId}/{orderId}/pay", USER_ID, ORDER_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_NewOrder_Returns200() throws Exception {
        when(orderService.cancel(USER_ID, ORDER_ID)).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(post("/orders/{userId}/{orderId}/cancel", USER_ID, ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void retryInvoice_AlreadyIssued_Returns409() throws Exception {
        when(orderService.retryInvoice(USER_ID, ORDER_ID)).thenThrow(new InvoiceAlreadyIssuedException(ORDER_ID));

        mockMvc.perform(post("/orders/{userId}/{orderId}/invoice", USER_ID, ORDER_ID))
                .andExpect(status().isConflict());
    }

    @Test
    void retryInvoice_FailedInvoice_Returns200() throws Exception {
        when(orderService.retryInvoice(USER_ID, ORDER_ID)).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(post("/orders/{userId}/{orderId}/invoice", USER_ID, ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void ship_PaidOrder_Returns200() throws Exception {
        when(orderService.ship(ORDER_ID)).thenReturn(mock(OrderResponse.class));

        mockMvc.perform(post("/orders/{orderId}/ship", ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void ship_UnpaidOrder_Returns409() throws Exception {
        when(orderService.ship(ORDER_ID)).thenThrow(new InvalidOrderStateException(OrderStatus.NEW, "ship"));

        mockMvc.perform(post("/orders/{orderId}/ship", ORDER_ID))
                .andExpect(status().isConflict());
    }
}