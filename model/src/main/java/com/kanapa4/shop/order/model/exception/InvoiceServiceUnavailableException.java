package com.kanapa4.shop.order.model.exception;

public class InvoiceServiceUnavailableException extends OrderException {

    public InvoiceServiceUnavailableException() {
        super("invoice-service is currently unavailable");
    }
}