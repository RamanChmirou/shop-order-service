package com.kanapa4.shop.order.domain.port;

import com.kanapa4.shop.order.pub.event.OrderPlacedEvent;

public interface OrderEventPublisherPort {

    void publishOrderPlaced(OrderPlacedEvent event);
}