package com.kanapa4.shop.order.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${outbox.relay-delay:2000}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, Limit.of(properties.batchSize()));
        for (OutboxEventEntity event : pending) {
            if (!send(event)) {
                return;
            }
        }
    }

    private boolean send(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(properties.topic(), event.getAggregateId().toString(), event.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            event.markSent();
            log.info("Outbox event {} ({}) sent for aggregate {}",
                    event.getId(), event.getType(), event.getAggregateId());
            return true;
        } catch (Exception exception) {
            log.warn("Outbox event {} not sent, will retry: {}", event.getId(), exception.toString());
            return false;
        }
    }
}