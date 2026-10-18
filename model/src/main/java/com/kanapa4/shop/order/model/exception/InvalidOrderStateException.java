package com.kanapa4.shop.order.model.exception;

import com.kanapa4.shop.order.model.OrderStatus;

public class InvalidOrderStateException extends OrderException {

    public InvalidOrderStateException(OrderStatus status, String action) {
        super("Cannot " + action + " an order in status " + status);
    }
}