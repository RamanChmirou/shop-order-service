package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.model.CartSnapshot;

import java.util.UUID;

public interface CartPort {

    CartSnapshot repriceCart(UUID userId);
}