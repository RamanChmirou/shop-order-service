package com.kanapa4.shop.order.pub.response;

import com.kanapa4.shop.order.pub.type.VatRate;

import java.math.BigDecimal;

public record PriceResponse(BigDecimal amount, String currency, VatRate vatRate) {
}