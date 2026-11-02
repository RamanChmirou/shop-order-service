package com.kanapa4.shop.order.pub.response;

import java.math.BigDecimal;

public record SelectedOptionResponse(String groupCode, String valueCode, String valueLabel, BigDecimal priceDelta) {
}