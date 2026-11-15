package com.kanapa4.shop.order.model.exception;

import java.util.UUID;

public class OrderConcurrentModificationException extends OrderException {

    public OrderConcurrentModificationException(UUID orderId) {
        super("Order " + orderId + " was modified concurrently, please retry");
    }
}