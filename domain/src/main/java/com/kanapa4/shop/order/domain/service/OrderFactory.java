package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.domain.port.OrderMapperPort;
import com.kanapa4.shop.order.model.Address;
import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Invoice;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderFactory {

    private final OrderMapperPort orderMapper;
    private final OrderNumberGenerator numberGenerator;
    private final DeliveryCostCalculator deliveryCostCalculator;

    public Order create(UUID userId, CheckoutRequest request, CartSnapshot cart) {
        DeliveryMethod deliveryMethod = orderMapper.toDeliveryMethod(request.deliveryMethod());
        BigDecimal deliveryCost = deliveryCostCalculator.calculate(deliveryMethod);
        Address shippingAddress = orderMapper.toAddress(request.shippingAddress());
        return Order.builder()
                .orderNumber(numberGenerator.next())
                .userId(userId)
                .deliveryMethod(deliveryMethod)
                .paymentMethod(orderMapper.toPaymentMethod(request.paymentMethod()))
                .customer(orderMapper.toCustomer(request.customer()))
                .shippingAddress(shippingAddress)
                .billingAddress(resolveBillingAddress(request, shippingAddress))
                .items(cart.items())
                .itemsTotal(cart.totalAmount())
                .deliveryCost(deliveryCost)
                .grandTotal(cart.totalAmount().add(deliveryCost))
                .currency(cart.currency())
                .invoice(Invoice.builder().build())
                .build();
    }

    private Address resolveBillingAddress(CheckoutRequest request, Address shippingAddress) {
        if (request.billingAddress() == null) {
            return shippingAddress;
        }
        return orderMapper.toAddress(request.billingAddress());
    }
}