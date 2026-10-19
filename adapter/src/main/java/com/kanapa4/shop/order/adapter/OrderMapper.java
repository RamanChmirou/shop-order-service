package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.*;
import com.kanapa4.shop.order.pub.request.AddressRequest;
import com.kanapa4.shop.order.pub.request.CustomerRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(Order order);

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toEntity(OrderItem item);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    InvoiceEntity toEntity(Invoice invoice);

    Order toPojo(OrderEntity entity);

    @Mapping(target = "plus", ignore = true)
    @Mapping(target = "times", ignore = true)
    Price toPojo(PriceEmbeddable embeddable);

    OrderItem toPojo(OrderItemEntity entity);

    Invoice toPojo(InvoiceEntity entity);

    OrderResponse toResponse(Order order);

    OrderSummaryResponse toSummary(Order order);

    Customer toCustomer(CustomerRequest request);

    Address toAddress(AddressRequest request);

    DeliveryMethod toDeliveryMethod(com.kanapa4.shop.order.pub.type.DeliveryMethod method);

    PaymentMethod toPaymentMethod(com.kanapa4.shop.order.pub.type.PaymentMethod method);
}