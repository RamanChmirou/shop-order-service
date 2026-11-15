package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.pub.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherAdapterTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    private OutboxEventPublisherAdapter publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisherAdapter(outboxRepository, JsonMapper.builder().build());
    }

    @Test
    void shouldStorePendingEventWithJsonPayload() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), orderId, "ORD-1",
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("215.00"), "PLN", Instant.now());

        publisher.publishOrderPlaced(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getAggregateId()).isEqualTo(orderId);
        assertThat(captor.getValue().getType()).isEqualTo("ORDER_PLACED");
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(captor.getValue().getPayload()).contains("ORD-1");
    }
}