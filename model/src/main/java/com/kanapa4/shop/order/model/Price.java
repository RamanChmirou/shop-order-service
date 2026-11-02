package com.kanapa4.shop.order.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Price(BigDecimal amount, String currency, VatRate vatRate) {

    public Price {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(vatRate, "vatRate");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal netAmount() {
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100).add(vatRate.percent()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal taxAmount() {
        return amount.subtract(netAmount());
    }

    public Price plus(Price other) {
        requireSame(other);
        return new Price(amount.add(other.amount), currency, vatRate);
    }

    public Price times(int quantity) {
        return new Price(amount.multiply(BigDecimal.valueOf(quantity)), currency, vatRate);
    }

    private void requireSame(Price other) {
        if (!currency.equals(other.currency) || vatRate != other.vatRate) {
            throw new IllegalArgumentException("Currency or VAT rate mismatch");
        }
    }
}