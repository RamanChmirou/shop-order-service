package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.PaymentPort;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.PaymentMethod;
import com.kanapa4.shop.order.model.exception.PaymentDeclinedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedPaymentGateway implements PaymentPort {

    private final PaymentGatewayProperties properties;

    @Override
    public void charge(Order order) {
        PaymentMethod method = order.getPaymentMethod();
        if (properties.declinedMethods().contains(method)) {
            log.warn("Simulated gateway declined payment of order {} ({})", order.getOrderNumber(), method);
            throw new PaymentDeclinedException(order.getOrderNumber(), method.name());
        }
        log.info("Simulated gateway approved payment of order {} ({}, {} {})",
                order.getOrderNumber(), method, order.getGrandTotal(), order.getCurrency());
    }
}