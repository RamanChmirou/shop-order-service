package com.kanapa4.shop.order.adapter;

import java.math.BigDecimal;

public record SelectedOptionClientResponse(
        String groupCode,
        String valueCode,
        String valueLabel,
        BigDecimal priceDelta) {}