package com.kanapa4.shop.order.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(
        name = "cart-service",
        configuration = CartClientConfig.class,
        fallbackFactory = CartClientFallbackFactory.class
)
public interface CartClient {

    @PostMapping("/carts/{userId}/reprice")
    RepriceClientResponse reprice(@PathVariable("userId") UUID userId);
}