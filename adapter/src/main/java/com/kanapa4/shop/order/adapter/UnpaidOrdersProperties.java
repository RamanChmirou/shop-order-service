package com.kanapa4.shop.order.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "orders.cancel-unpaid")
public record UnpaidOrdersProperties(Duration maxAge, int batchSize) {}