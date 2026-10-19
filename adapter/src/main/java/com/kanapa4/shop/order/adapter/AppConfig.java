package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.CartPort;
import com.kanapa4.shop.order.domain.port.InvoicePort;
import com.kanapa4.shop.order.domain.port.OrderEventPublisherPort;
import com.kanapa4.shop.order.domain.port.OrderMapperPort;
import com.kanapa4.shop.order.domain.port.OrderRepositoryPort;
import com.kanapa4.shop.order.domain.service.DeliveryCostCalculator;
import com.kanapa4.shop.order.domain.service.OrderFactory;
import com.kanapa4.shop.order.domain.service.OrderNumberGenerator;
import com.kanapa4.shop.order.domain.service.OrderService;
import com.kanapa4.shop.order.domain.validator.CheckoutValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DeliveryCostCalculator deliveryCostCalculator() {
        return new DeliveryCostCalculator();
    }

    @Bean
    public OrderNumberGenerator orderNumberGenerator(Clock clock) {
        return new OrderNumberGenerator(clock);
    }

    @Bean
    public CheckoutValidator checkoutValidator() {
        return new CheckoutValidator();
    }

    @Bean
    public OrderFactory orderFactory(OrderMapperPort orderMapper,
                                     OrderNumberGenerator numberGenerator,
                                     DeliveryCostCalculator deliveryCostCalculator) {
        return new OrderFactory(orderMapper, numberGenerator, deliveryCostCalculator);
    }

    @Bean
    public OrderService orderService(OrderRepositoryPort orderRepository,
                                     OrderMapperPort orderMapper,
                                     CartPort cartPort,
                                     InvoicePort invoicePort,
                                     OrderEventPublisherPort eventPublisher,
                                     CheckoutValidator checkoutValidator,
                                     OrderFactory orderFactory,
                                     Clock clock) {
        return new OrderService(orderRepository, orderMapper, cartPort, invoicePort,
                eventPublisher, checkoutValidator, orderFactory, clock);
    }
}