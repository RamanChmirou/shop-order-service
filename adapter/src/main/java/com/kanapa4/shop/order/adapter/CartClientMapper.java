package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.CartSnapshot;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartClientMapper {

    CartSnapshot toSnapshot(CartClientResponse response);
}