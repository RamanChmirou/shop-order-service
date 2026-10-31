package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.model.Order;

public interface PaymentPort {

    void charge(Order order);
}