package com.kanapa4.shop.order.adapter;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CartClientConfig {

    @Bean
    public Retryer cartRetryer() {
        return new Retryer.Default(200, 1000, 3);
    }

    @Bean
    public ErrorDecoder cartErrorDecoder() {
        return new CartClientErrorDecoder();
    }
}