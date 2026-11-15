package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidOrdersCleanupJob {

    private final OrderService orderService;
    private final UnpaidOrdersProperties properties;

    @Scheduled(cron = "${orders.cancel-unpaid.cron:0 */10 * * * *}")
    public void cancelUnpaidOrders() {
        int cancelled = orderService.cancelUnpaidOrders(properties.maxAge(), properties.batchSize());
        log.info("Unpaid orders cleanup finished, cancelled {}", cancelled);
    }
}