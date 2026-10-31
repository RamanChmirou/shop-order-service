package com.kanapa4.shop.order.model.exception;

public abstract class OrderException extends RuntimeException {

    protected OrderException(String message) {
        super(message);
    }
}