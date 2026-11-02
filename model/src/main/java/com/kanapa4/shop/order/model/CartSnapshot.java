package com.kanapa4.shop.order.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartSnapshot(
        UUID id,
        UUID userId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        String currency) {
}