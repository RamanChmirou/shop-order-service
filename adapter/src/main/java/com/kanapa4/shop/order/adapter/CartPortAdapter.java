package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.CartPort;
import com.kanapa4.shop.order.model.CartSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartPortAdapter implements CartPort {

    private final CartClient cartClient;
    private final CartClientMapper cartClientMapper;

    @Override
    public CartSnapshot repriceCart(UUID userId) {
        log.info("Repricing cart for user {}", userId);
        RepriceClientResponse response = cartClient.reprice(userId);
        return cartClientMapper.toSnapshot(response.cart());
    }
}