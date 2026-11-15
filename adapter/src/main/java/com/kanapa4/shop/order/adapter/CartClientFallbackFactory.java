package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.exception.CartServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.EmptyCartException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        return userId -> {
            log.warn("Cart-service call failed for user {}: {}", userId, cause.toString());
            if (cause instanceof FeignException.NotFound) {
                throw new EmptyCartException(userId);
            }
            throw new CartServiceUnavailableException();
        };
    }
}