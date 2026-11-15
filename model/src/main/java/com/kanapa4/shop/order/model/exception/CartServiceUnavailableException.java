package com.kanapa4.shop.order.model.exception;

public class CartServiceUnavailableException extends OrderException {

    public CartServiceUnavailableException() {
        super("cart-service is currently unavailable");
    }
}