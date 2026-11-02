package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.model.IssuedInvoice;
import com.kanapa4.shop.order.model.Order;

public interface InvoicePort {

    IssuedInvoice issue(Order order);
}