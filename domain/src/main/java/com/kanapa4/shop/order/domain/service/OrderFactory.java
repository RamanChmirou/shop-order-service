package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.domain.port.OrderMapperPort;
import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class OrderFactory {

    private final OrderMapperPort orderMapper;
    private final OrderNumberGenerator numberGenerator;
    private final DeliveryCostCalculator deliveryCostCalculator;

    public Order create(UUID userId, CheckoutRequest request, CartSnapshot cart) {
        DeliveryMethod deliveryMethod = orderMapper.toDeliveryMethod(request.deliveryMethod());
        return Order.create(
                numberGenerator.next(),
                userId,
                orderMapper.toCustomer(request.customer()),
                deliveryMethod,
                orderMapper.toPaymentMethod(request.paymentMethod()),
                orderMapper.toAddress(request.shippingAddress()),
                orderMapper.toAddress(request.billingAddress()),
                cart,
                deliveryCostCalculator.calculate(deliveryMethod));
    }
}