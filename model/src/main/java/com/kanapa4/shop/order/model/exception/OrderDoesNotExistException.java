package com.kanapa4.shop.order.model.exception;

import java.util.UUID;

public class OrderDoesNotExistException extends OrderException {

    public OrderDoesNotExistException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}