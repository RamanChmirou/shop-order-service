package com.kanapa4.shop.order.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartClientResponse(
        UUID id,
        UUID userId,
        String status,
        List<CartItemClientResponse> items,
        int itemCount,
        BigDecimal totalAmount,
        String currency) {}