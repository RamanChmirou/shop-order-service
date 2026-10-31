package com.kanapa4.shop.order.pub.response;

import com.kanapa4.shop.order.pub.type.DeliveryMethod;
import com.kanapa4.shop.order.pub.type.OrderStatus;
import com.kanapa4.shop.order.pub.type.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        DeliveryMethod deliveryMethod,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        List<OrderItemResponse> items,
        BigDecimal itemsTotal,
        BigDecimal deliveryCost,
        BigDecimal grandTotal,
        String currency,
        InvoiceResponse invoice,
        Instant createdAt) {
}