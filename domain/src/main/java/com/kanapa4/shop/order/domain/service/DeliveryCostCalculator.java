package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.model.DeliveryMethod;

import java.math.BigDecimal;

public class DeliveryCostCalculator {

    public BigDecimal calculate(DeliveryMethod method) {
        return method.getCost();
    }
}