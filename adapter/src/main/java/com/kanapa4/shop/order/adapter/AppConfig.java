package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.*;
import com.kanapa4.shop.order.domain.service.DeliveryCostCalculator;
import com.kanapa4.shop.order.domain.service.OrderFactory;
import com.kanapa4.shop.order.domain.service.OrderNumberGenerator;
import com.kanapa4.shop.order.domain.service.OrderService;
import com.kanapa4.shop.order.domain.validator.CheckoutValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({OutboxProperties.class, PaymentGatewayProperties.class, UnpaidOrdersProperties.class})
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
                                     PaymentPort paymentPort,
                                     TransactionPort transactionPort,
                                     Clock clock) {
        return new OrderService(orderRepository, orderMapper, cartPort, invoicePort,
                eventPublisher, checkoutValidator, orderFactory, paymentPort, transactionPort, clock);
    }
}