package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.OrderItem;
import com.kanapa4.shop.order.model.Price;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartClientMapper {

    CartSnapshot toSnapshot(CartClientResponse response);

    @Mapping(target = "id", ignore = true)
    OrderItem toOrderItem(CartItemClientResponse item);

    @Mapping(target = "plus", ignore = true)
    @Mapping(target = "times", ignore = true)
    Price toPrice(PriceClientResponse response);
}