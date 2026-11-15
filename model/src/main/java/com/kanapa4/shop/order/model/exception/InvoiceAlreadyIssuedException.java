package com.kanapa4.shop.order.model.exception;

import java.util.UUID;

public class InvoiceAlreadyIssuedException extends OrderException {

    public InvoiceAlreadyIssuedException(UUID orderId) {
        super("Invoice of order " + orderId + " is already issued");
    }
}