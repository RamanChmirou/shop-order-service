package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.OrderRepositoryPort;
import com.kanapa4.shop.order.model.Order;
import com.kanapa4.shop.order.model.exception.OrderConcurrentModificationException;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public Order save(Order order) {
        log.debug("Saving order {}", order.getOrderNumber());
        OrderEntity entity = orderMapper.toEntity(order);
        bindRelations(entity);
        try {
            return orderMapper.toPojo(orderRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification of order {}", order.getId());
            throw new OrderConcurrentModificationException(order.getId());
        }
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        log.debug("Loading order {}", orderId);
        return orderRepository.findById(orderId).map(orderMapper::toPojo);
    }

    @Override
    public Optional<Order> findByIdAndUserId(UUID orderId, UUID userId) {
        log.debug("Loading order {} of user {}", orderId, userId);
        return orderRepository.findByIdAndUserId(orderId, userId).map(orderMapper::toPojo);
    }

    @Override
    public Page<Order> findAllByUserId(UUID userId, Pageable pageable) {
        log.debug("Loading orders of user {}, pageable={}", userId, pageable);
        var springPage = orderRepository.findAllByUserId(userId, PageMapper.toSpringPageable(pageable));
        return PageMapper.toPojoPage(springPage, orderMapper::toPojo);
    }

    private void bindRelations(OrderEntity entity) {
        entity.getItems().forEach(item -> item.setOrder(entity));
        if (entity.getInvoice() != null) {
            entity.getInvoice().setOrder(entity);
        }
    }
}