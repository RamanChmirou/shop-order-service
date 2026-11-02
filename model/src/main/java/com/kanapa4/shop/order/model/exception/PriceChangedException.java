package com.kanapa4.shop.order.model.exception;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PriceChangedException extends OrderException {

    private final BigDecimal expectedTotal;
    private final BigDecimal actualTotal;

    public PriceChangedException(BigDecimal expectedTotal, BigDecimal actualTotal) {
        super("Prices have changed: expected total " + expectedTotal + ", actual total " + actualTotal);
        this.expectedTotal = expectedTotal;
        this.actualTotal = actualTotal;
    }
}