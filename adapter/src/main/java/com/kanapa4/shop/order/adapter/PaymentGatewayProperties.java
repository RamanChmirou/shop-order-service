package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.PaymentMethod;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;
import java.util.Set;

@ConfigurationProperties(prefix = "payment.gateway")
public record PaymentGatewayProperties(Set<PaymentMethod> declinedMethods) {

    public PaymentGatewayProperties {
        declinedMethods = Objects.requireNonNullElse(declinedMethods, Set.of());
    }
}