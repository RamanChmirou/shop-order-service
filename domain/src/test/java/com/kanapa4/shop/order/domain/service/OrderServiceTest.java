package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.domain.port.*;
import com.kanapa4.shop.order.domain.validator.CheckoutValidator;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Invoice;
import com.kanapa4.shop.order.model.InvoiceStatus;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.OrderStatus;
import com.kanapa4.shop.order.model.PaymentMethod;
import com.kanapa4.shop.order.model.exception.*;
import com.kanapa4.shop.order.pub.event.OrderPlacedEvent;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import com.kanapa4.shop.order.pub.page.SortDirection;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.kanapa4.shop.order.domain.testdata.OrderFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderMapperPort orderMapper;

    @Mock
    private CartPort cartPort;

    @Mock
    private InvoicePort invoicePort;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @Mock
    private OrderResponse response;
    @Mock
    PaymentPort paymentPort;

    @Mock
    TransactionPort transactionPort;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        OrderFactory orderFactory = new OrderFactory(
                orderMapper, new OrderNumberGenerator(CLOCK), new DeliveryCostCalculator());
        orderService = new OrderService(orderRepository, orderMapper, cartPort, invoicePort,
                eventPublisher, new CheckoutValidator(), orderFactory, paymentPort, transactionPort, CLOCK);
    }

    private void givenMapperConvertsRequest() {
        when(orderMapper.toDeliveryMethod(any())).thenReturn(DeliveryMethod.COURIER);
        when(orderMapper.toPaymentMethod(any())).thenReturn(PaymentMethod.CARD);
        when(orderMapper.toCustomer(any())).thenReturn(customer());
        when(orderMapper.toAddress(addressRequest())).thenReturn(address());
        lenient().when(orderMapper.toAddress(null)).thenReturn(null);
    }

    private void givenOrder(Order order) {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
    }

    private void givenSaveReturnsArgumentAndMapsToResponse(Order order) {
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private void givenTransactionRunsAction() {
        when(transactionPort.execute(any())).thenAnswer(
                invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
    }

    @Test
    void checkout_ValidRequest_SavesNewOrderAndPublishesEvent() {
        givenMapperConvertsRequest();
        givenTransactionRunsAction();
        when(cartPort.repriceCart(USER_ID)).thenReturn(cartSnapshot());
        givenMapperConvertsRequest();
        Order saved = order(OrderStatus.NEW);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(response);

        OrderResponse result = orderService.checkout(USER_ID, checkoutRequest(CART_TOTAL));

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order created = orderCaptor.getValue();
        assertThat(created.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(created.getUserId()).isEqualTo(USER_ID);
        assertThat(created.getOrderNumber()).startsWith("ORD-20261018-");
        assertThat(created.getItems()).hasSize(1);
        assertThat(created.getItemsTotal()).isEqualByComparingTo("10398.00");
        assertThat(created.getDeliveryCost()).isEqualByComparingTo("15.00");
        assertThat(created.getGrandTotal()).isEqualByComparingTo("10413.00");
        assertThat(created.getInvoice().getStatus()).isEqualTo(InvoiceStatus.PENDING);
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(eventPublisher).publishOrderPlaced(eventCaptor.capture());
        OrderPlacedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.cartId()).isEqualTo(CART_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.grandTotal()).isEqualByComparingTo("10413.00");
        assertThat(event.occurredAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void checkout_NoBillingAddress_UsesShippingAddressForBilling() {
        givenMapperConvertsRequest();
        givenTransactionRunsAction();
        when(cartPort.repriceCart(USER_ID)).thenReturn(cartSnapshot());
        givenMapperConvertsRequest();
        Order saved = order(OrderStatus.NEW);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(response);

        orderService.checkout(USER_ID, checkoutRequest(CART_TOTAL));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getBillingAddress())
                .isNotNull()
                .isEqualTo(orderCaptor.getValue().getShippingAddress());
    }

    @Test
    void checkout_EmptyCart_ThrowsAndSavesNothing() {
        when(cartPort.repriceCart(USER_ID)).thenReturn(emptyCart());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest(BigDecimal.ONE)))
                .isInstanceOf(EmptyCartException.class);
        verifyNoInteractions(orderRepository, eventPublisher);
    }

    @Test
    void checkout_TotalChangedSinceUserSawIt_ThrowsPriceChangedAndSavesNothing() {
        when(cartPort.repriceCart(USER_ID)).thenReturn(cartSnapshot());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest(new BigDecimal("10000.00"))))
                .isInstanceOfSatisfying(PriceChangedException.class, exception -> {
                    assertThat(exception.getExpectedTotal()).isEqualByComparingTo("10000.00");
                    assertThat(exception.getActualTotal()).isEqualByComparingTo("10398.00");
                });
        verifyNoInteractions(orderRepository, eventPublisher);
    }

    @Test
    void checkout_CartServiceUnavailable_PropagatesAndSavesNothing() {
        when(cartPort.repriceCart(USER_ID)).thenThrow(new CartServiceUnavailableException());

        assertThatThrownBy(() -> orderService.checkout(USER_ID, checkoutRequest(CART_TOTAL)))
                .isInstanceOf(CartServiceUnavailableException.class);
        verifyNoInteractions(orderRepository, eventPublisher);
    }

    @Test
    void pay_NewOrder_MarksPaidAndIssuesInvoice() {
        Order order = order(OrderStatus.NEW);
        givenOrder(order);
        when(invoicePort.issue(order)).thenReturn(issuedInvoice());
        givenSaveReturnsArgumentAndMapsToResponse(order);

        OrderResponse result = orderService.pay(USER_ID, ORDER_ID);

        assertThat(result).isSameAs(response);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getInvoice().getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(order.getInvoice().getNumber()).isEqualTo("FV/2026/10/1");
        assertThat(order.getInvoice().getAttempts()).isEqualTo(1);
    }

    @Test
    void pay_InvoiceServiceDown_OrderIsPaidAndInvoiceIsFailed() {
        Order order = order(OrderStatus.NEW);
        givenOrder(order);
        when(invoicePort.issue(order)).thenThrow(new InvoiceServiceUnavailableException());
        givenSaveReturnsArgumentAndMapsToResponse(order);

        orderService.pay(USER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getInvoice().getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(order.getInvoice().getAttempts()).isEqualTo(1);
        assertThat(order.getInvoice().getLastError()).contains("invoice-service");
        verify(orderRepository).save(order);
    }

    @Test
    void pay_AlreadyPaidOrder_ThrowsAndDoesNotIssueInvoice() {
        givenOrder(order(OrderStatus.PAID));

        assertThatThrownBy(() -> orderService.pay(USER_ID, ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
        verifyNoInteractions(invoicePort, paymentPort);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void pay_UnknownOrder_ThrowsNotFound() {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(USER_ID, ORDER_ID))
                .isInstanceOf(OrderDoesNotExistException.class);
    }

    @Test
    void retryInvoice_FailedInvoice_IssuesIt() {
        Invoice failed = Invoice.builder().id(INVOICE_ID).build();
        failed.markFailed("down");
        Order order = order(OrderStatus.PAID, failed);
        givenOrder(order);
        when(invoicePort.issue(order)).thenReturn(issuedInvoice());
        givenSaveReturnsArgumentAndMapsToResponse(order);

        orderService.retryInvoice(USER_ID, ORDER_ID);

        assertThat(order.getInvoice().getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(order.getInvoice().getAttempts()).isEqualTo(2);
        assertThat(order.getInvoice().getLastError()).isNull();
    }

    @Test
    void retryInvoice_AlreadyIssued_ThrowsAndDoesNotCallInvoiceService() {
        Invoice issued = Invoice.builder().id(INVOICE_ID).build();
        issued.markIssued("ext-1", "FV/1", "https://x", Instant.parse("2026-10-18T12:00:00Z"));
        givenOrder(order(OrderStatus.PAID, issued));

        assertThatThrownBy(() -> orderService.retryInvoice(USER_ID, ORDER_ID))
                .isInstanceOf(InvoiceAlreadyIssuedException.class);
        verifyNoInteractions(invoicePort);
    }

    @Test
    void retryInvoice_UnpaidOrder_Throws() {
        givenOrder(order(OrderStatus.NEW));

        assertThatThrownBy(() -> orderService.retryInvoice(USER_ID, ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
        verifyNoInteractions(invoicePort);
    }

    @Test
    void cancel_NewOrder_CancelsIt() {
        Order order = order(OrderStatus.NEW);
        givenOrder(order);
        givenSaveReturnsArgumentAndMapsToResponse(order);

        orderService.cancel(USER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_PaidOrder_Throws() {
        givenOrder(order(OrderStatus.PAID));

        assertThatThrownBy(() -> orderService.cancel(USER_ID, ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void ship_PaidOrder_ShipsIt() {
        Order order = order(OrderStatus.PAID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        givenSaveReturnsArgumentAndMapsToResponse(order);

        orderService.ship(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void ship_NewOrder_Throws() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.NEW)));

        assertThatThrownBy(() -> orderService.ship(ORDER_ID))
                .isInstanceOf(InvalidOrderStateException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_OrderOfUser_ReturnsResponse() {
        Order order = order(OrderStatus.NEW);
        givenOrder(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        assertThat(orderService.getOrder(USER_ID, ORDER_ID)).isSameAs(response);
    }

    @Test
    void getOrder_OrderOfAnotherUser_ThrowsNotFound() {
        UUID otherUser = UUID.randomUUID();
        when(orderRepository.findByIdAndUserId(ORDER_ID, otherUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(otherUser, ORDER_ID))
                .isInstanceOf(OrderDoesNotExistException.class);
    }

    @Test
    void getOrders_UserHasOrders_ReturnsMappedPage(@Mock OrderSummaryResponse summary) {
        Order order = order(OrderStatus.NEW);
        Pageable pageable = new Pageable(0, 20, "createdAt", SortDirection.DESC);
        when(orderRepository.findAllByUserId(USER_ID, pageable))
                .thenReturn(new Page<>(List.of(order), 0, 20, 1, 1));
        when(orderMapper.toSummary(order)).thenReturn(summary);

        Page<OrderSummaryResponse> result = orderService.getOrders(USER_ID, pageable);

        assertThat(result.content()).containsExactly(summary);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void pay_PaymentDeclined_DoesNotSaveAndDoesNotIssueInvoice() {
        Order order = order(OrderStatus.NEW);
        when(orderRepository.findByIdAndUserId(order.getId(), USER_ID)).thenReturn(Optional.of(order));
        doThrow(new PaymentDeclinedException(order.getOrderNumber(), "TRANSFER"))
                .when(paymentPort).charge(order);

        assertThatThrownBy(() -> orderService.pay(USER_ID, order.getId()))
                .isInstanceOf(PaymentDeclinedException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(invoicePort);
    }

    @Test
    void pay_Success_ChargesBeforeIssuingInvoice() {
        Order order = order(OrderStatus.NEW);
        when(orderRepository.findByIdAndUserId(order.getId(), USER_ID)).thenReturn(Optional.of(order));
        when(invoicePort.issue(order)).thenReturn(issuedInvoice());
        when(orderRepository.save(order)).thenReturn(order);

        orderService.pay(USER_ID, order.getId());

        InOrder inOrder = inOrder(paymentPort, invoicePort, orderRepository);
        inOrder.verify(paymentPort).charge(order);
        inOrder.verify(invoicePort).issue(order);
        inOrder.verify(orderRepository).save(order);
    }

    @Test
    void cancelUnpaidOrders_ExpiredOrders_CancelsAndSavesEach() {
        Order first = order(OrderStatus.NEW);
        Order second = order(OrderStatus.NEW);
        Instant threshold = CLOCK.instant().minus(Duration.ofHours(24));
        when(orderRepository.findUnpaidCreatedBefore(threshold, 100)).thenReturn(List.of(first, second));

        int cancelled = orderService.cancelUnpaidOrders(Duration.ofHours(24), 100);

        assertThat(cancelled).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void cancelUnpaidOrders_ConcurrentlyPaidOrder_IsSkipped() {
        Order raced = order(OrderStatus.NEW);
        when(orderRepository.findUnpaidCreatedBefore(any(), anyInt())).thenReturn(List.of(raced));
        when(orderRepository.save(raced)).thenThrow(new OrderConcurrentModificationException(ORDER_ID));

        int result = orderService.cancelUnpaidOrders(Duration.ofHours(24), 100);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void cancelUnpaidOrders_NothingExpired_DoesNothing() {
        when(orderRepository.findUnpaidCreatedBefore(any(), anyInt())).thenReturn(List.of());

        assertThat(orderService.cancelUnpaidOrders(Duration.ofHours(24), 100)).isZero();
        verify(orderRepository, never()).save(any());
    }
}