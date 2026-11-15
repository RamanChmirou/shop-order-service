package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.model.Address;
import com.kanapa4.shop.order.model.Customer;
import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.PaymentMethod;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;

public interface OrderMapperPort {

    OrderResponse toResponse(Order order);

    OrderSummaryResponse toSummary(Order order);

    Customer toCustomer(CustomerRequest request);

    Address toAddress(AddressRequest request);

    DeliveryMethod toDeliveryMethod(com.kanapa4.shop.order.pub.type.DeliveryMethod method);

    PaymentMethod toPaymentMethod(com.kanapa4.shop.order.pub.type.PaymentMethod method);
}