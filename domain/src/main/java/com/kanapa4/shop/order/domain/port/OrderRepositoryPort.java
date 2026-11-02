package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);

    Page<Order> findAllByUserId(UUID userId, Pageable pageable);

    List<Order> findUnpaidCreatedBefore(Instant threshold, int limit);
}