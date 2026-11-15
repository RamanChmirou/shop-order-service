package com.kanapa4.shop.order.adapter;

import java.math.BigDecimal;

public record PriceClientResponse(BigDecimal amount, String currency, String vatRate) {}
