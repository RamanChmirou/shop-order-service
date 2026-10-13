package com.kanapa4.shop.order.adapter;

import java.math.BigDecimal;

public record SelectedOptionJson(
        String groupCode,
        String valueCode,
        String valueLabel,
        BigDecimal priceDelta) {
}
