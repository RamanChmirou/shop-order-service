package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.OrderEventPublisherPort;
import com.kanapa4.shop.order.pub.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisherAdapter implements OrderEventPublisherPort {

    private static final String ORDER_PLACED = "ORDER_PLACED";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderPlaced(OrderPlacedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        outboxRepository.save(OutboxEventEntity.pending(event.orderId(), ORDER_PLACED, payload));
        log.info("Event {} for order {} stored in outbox", ORDER_PLACED, event.orderId());
    }
}