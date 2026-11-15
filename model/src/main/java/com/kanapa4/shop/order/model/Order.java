package com.kanapa4.shop.order.model;

import com.kanapa4.shop.order.model.exception.InvalidOrderStateException;
import com.kanapa4.shop.order.model.exception.InvoiceAlreadyIssuedException;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder
public class Order {

    private final UUID id;
    private final String orderNumber;
    private final UUID userId;
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;
    private final DeliveryMethod deliveryMethod;
    private final PaymentMethod paymentMethod;
    private final Customer customer;
    private final Address shippingAddress;
    private final Address billingAddress;
    private final List<OrderItem> items;
    private final BigDecimal itemsTotal;
    private final BigDecimal deliveryCost;
    private final BigDecimal grandTotal;
    private final String currency;
    private final Invoice invoice;
    private final Instant createdAt;
    private Long version;

    public void pay() {
        changeStatus(OrderStatus.NEW, OrderStatus.PAID, "pay");
    }

    public void ship() {
        changeStatus(OrderStatus.PAID, OrderStatus.SHIPPED, "ship");
    }

    public void cancel() {
        changeStatus(OrderStatus.NEW, OrderStatus.CANCELLED, "cancel");
    }

    public Invoice invoiceToIssue() {
        if (status == OrderStatus.NEW || status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(status, "issue an invoice for");
        }
        if (invoice.isIssued()) {
            throw new InvoiceAlreadyIssuedException(id);
        }
        return invoice;
    }

    private void changeStatus(OrderStatus required, OrderStatus target, String action) {
        if (status != required) {
            throw new InvalidOrderStateException(status, action);
        }
        status = target;
    }

    public static Order create(String orderNumber, UUID userId, Customer customer,
                               DeliveryMethod deliveryMethod, PaymentMethod paymentMethod,
                               Address shippingAddress, Address billingAddress,
                               CartSnapshot cart, BigDecimal deliveryCost) {
        return Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .customer(customer)
                .deliveryMethod(deliveryMethod)
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .billingAddress(Objects.requireNonNullElse(billingAddress, shippingAddress))
                .items(cart.items())
                .itemsTotal(cart.totalAmount())
                .deliveryCost(deliveryCost)
                .grandTotal(cart.totalAmount().add(deliveryCost))
                .currency(cart.currency())
                .invoice(Invoice.builder().build())
                .build();
    }
}