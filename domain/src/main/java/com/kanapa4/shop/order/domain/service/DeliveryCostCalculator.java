package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.model.DeliveryMethod;

import java.math.BigDecimal;

public class DeliveryCostCalculator {

    private static final BigDecimal COURIER_COST = new BigDecimal("15.00");
    private static final BigDecimal PARCEL_LOCKER_COST = new BigDecimal("9.99");
    private static final BigDecimal PICKUP_COST = new BigDecimal("0.00");

    public BigDecimal calculate(DeliveryMethod method) {
        return switch (method) {
            case COURIER -> COURIER_COST;
            case PARCEL_LOCKER -> PARCEL_LOCKER_COST;
            case PICKUP -> PICKUP_COST;
        };
    }
}