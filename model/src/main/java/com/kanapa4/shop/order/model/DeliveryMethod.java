package com.kanapa4.shop.order.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum DeliveryMethod {
    COURIER(new BigDecimal("15.00")),
    PARCEL_LOCKER(new BigDecimal("9.99")),
    PICKUP(BigDecimal.ZERO);

    private final BigDecimal cost;
}
