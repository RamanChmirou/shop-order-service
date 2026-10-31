package com.kanapa4.shop.order.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxRepository, kafkaTemplate, new OutboxProperties("order.placed", 50));
    }

    @Test
    void shouldMarkEventSent_whenKafkaAccepts() {
        OutboxEventEntity event = OutboxEventEntity.pending(UUID.randomUUID(), "ORDER_PLACED", "{}");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("order.placed"), anyString(), eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(null));

        relay.relay();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    void shouldKeepEventPending_whenKafkaFails() {
        OutboxEventEntity event = OutboxEventEntity.pending(UUID.randomUUID(), "ORDER_PLACED", "{}");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka down")));

        relay.relay();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}