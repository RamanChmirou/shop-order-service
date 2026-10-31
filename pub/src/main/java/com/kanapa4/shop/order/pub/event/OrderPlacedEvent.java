package com.kanapa4.shop.order.pub.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        String orderNumber,
        UUID userId,
        UUID cartId,
        BigDecimal grandTotal,
        String currency,
        Instant occurredAt) {
}