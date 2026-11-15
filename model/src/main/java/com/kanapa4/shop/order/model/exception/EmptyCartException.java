package com.kanapa4.shop.order.model.exception;

import java.util.UUID;

public class EmptyCartException extends OrderException {

    public EmptyCartException(UUID userId) {
        super("Cart of user " + userId + " is empty");
    }
}