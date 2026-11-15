package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.OrderMapperPort;
import com.kanapa4.shop.order.model.Address;
import com.kanapa4.shop.order.model.Customer;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.PaymentMethod;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapperAdapter implements OrderMapperPort {

    private final OrderMapper orderMapper;

    @Override
    public OrderResponse toResponse(Order order) { return orderMapper.toResponse(order); }

    @Override
    public OrderSummaryResponse toSummary(Order order) { return orderMapper.toSummary(order); }

    @Override
    public Customer toCustomer(CustomerRequest request) { return orderMapper.toCustomer(request); }

    @Override
    public Address toAddress(AddressRequest request) { return orderMapper.toAddress(request); }

    @Override
    public DeliveryMethod toDeliveryMethod(com.kanapa4.shop.order.pub.type.DeliveryMethod method) {
        return orderMapper.toDeliveryMethod(method);
    }

    @Override
    public PaymentMethod toPaymentMethod(com.kanapa4.shop.order.pub.type.PaymentMethod method) {
        return orderMapper.toPaymentMethod(method);
    }
}