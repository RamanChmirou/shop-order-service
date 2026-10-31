package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.domain.port.*;
import com.kanapa4.shop.order.domain.validator.CheckoutValidator;
import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.Invoice;
import com.kanapa4.shop.order.model.IssuedInvoice;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.exception.InvalidOrderStateException;
import com.kanapa4.shop.order.model.exception.InvoiceServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.OrderConcurrentModificationException;
import com.kanapa4.shop.order.model.exception.OrderDoesNotExistException;
import com.kanapa4.shop.order.pub.event.OrderPlacedEvent;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;
    private final OrderMapperPort orderMapper;
    private final CartPort cartPort;
    private final InvoicePort invoicePort;
    private final OrderEventPublisherPort eventPublisher;
    private final CheckoutValidator checkoutValidator;
    private final OrderFactory orderFactory;
    private final PaymentPort paymentPort;
    private final TransactionPort transactionPort;
    private final Clock clock;

    public OrderResponse checkout(UUID userId, CheckoutRequest request) {
        log.info("Checkout of user {}", userId);
        CartSnapshot cart = cartPort.repriceCart(userId);
        checkoutValidator.validate(userId, request, cart);
        Order order = orderFactory.create(userId, request, cart);
        Order saved = transactionPort.execute(() -> saveWithEvent(order, cart.id()));
        log.info("Order {} created for user {}", saved.getOrderNumber(), userId);
        return orderMapper.toResponse(saved);
    }

    public Page<OrderSummaryResponse> getOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders of user {}, pageable={}", userId, pageable);
        return orderRepository.findAllByUserId(userId, pageable).map(orderMapper::toSummary);
    }

    public OrderResponse getOrder(UUID userId, UUID orderId) {
        log.debug("Fetching order {} of user {}", orderId, userId);
        return orderMapper.toResponse(findOrThrow(userId, orderId));
    }

    public OrderResponse pay(UUID userId, UUID orderId) {
        log.info("Payment of order {} by user {}", orderId, userId);
        Order order = findOrThrow(userId, orderId);
        order.pay();
        paymentPort.charge(order);
        issueInvoice(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse retryInvoice(UUID userId, UUID orderId) {
        log.info("Retrying the invoice of order {} by user {}", orderId, userId);
        Order order = findOrThrow(userId, orderId);
        issueInvoice(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse cancel(UUID userId, UUID orderId) {
        log.info("Cancelling order {} by user {}", orderId, userId);
        Order order = findOrThrow(userId, orderId);
        order.cancel();
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse ship(UUID orderId) {
        log.info("Shipping order {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderDoesNotExistException(orderId));
        order.ship();
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public int cancelUnpaidOrders(Duration maxAge, int batchSize) {
        Instant threshold = Instant.now(clock).minus(maxAge);
        List<Order> expired = orderRepository.findUnpaidCreatedBefore(threshold, batchSize);
        expired.forEach(this::cancelExpired);
        return expired.size();
    }

    private void cancelExpired(Order order) {
        try {
            order.cancel();
            orderRepository.save(order);
            log.info("Unpaid order {} cancelled by timeout", order.getOrderNumber());
        } catch (OrderConcurrentModificationException | InvalidOrderStateException e) {
            log.warn("Order {} skipped by cleanup: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private Order saveWithEvent(Order order, UUID cartId) {
        Order saved = orderRepository.save(order);
        eventPublisher.publishOrderPlaced(placedEvent(saved, cartId));
        return saved;
    }
    private Order findOrThrow(UUID userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderDoesNotExistException(orderId));
    }

    private void issueInvoice(Order order) {
        Invoice invoice = order.invoiceToIssue();
        try {
            IssuedInvoice issued = invoicePort.issue(order);
            invoice.markIssued(issued.externalId(), issued.number(), issued.pdfUrl(), issued.issuedAt());
            log.info("Invoice {} issued for order {}", issued.number(), order.getOrderNumber());
        } catch (InvoiceServiceUnavailableException e) {
            log.warn("Invoice of order {} was not issued: {}", order.getOrderNumber(), e.getMessage());
            invoice.markFailed(e.getMessage());
        }
    }

    private OrderPlacedEvent placedEvent(Order order, UUID cartId) {
        return new OrderPlacedEvent(UUID.randomUUID(), order.getId(), order.getOrderNumber(), order.getUserId(),
                cartId, order.getGrandTotal(), order.getCurrency(), Instant.now(clock));
    }
}