package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnpaidOrdersCleanupJobTest {

    @Mock
    private OrderService orderService;

    @Test
    void shouldCallServiceWithConfiguredLimits() {
        UnpaidOrdersCleanupJob job = new UnpaidOrdersCleanupJob(
                orderService, new UnpaidOrdersProperties(Duration.ofHours(24), 100));

        job.cancelUnpaidOrders();

        verify(orderService).cancelUnpaidOrders(Duration.ofHours(24), 100);
    }
}