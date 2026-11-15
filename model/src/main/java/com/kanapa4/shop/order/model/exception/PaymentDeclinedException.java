package com.kanapa4.shop.order.model.exception;

public class PaymentDeclinedException extends OrderException {

    public PaymentDeclinedException(String orderNumber, String paymentMethod) {
        super("Payment of order " + orderNumber + " was declined (" + paymentMethod + ")");
    }
}