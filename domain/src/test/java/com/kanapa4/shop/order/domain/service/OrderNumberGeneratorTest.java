package com.kanapa4.shop.order.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberGeneratorTest {

    @Test
    void next_UsesClockDateAndRandomSuffix() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);
        OrderNumberGenerator generator = new OrderNumberGenerator(clock);

        assertThat(generator.next()).matches("ORD-20261018-[0-9A-F]{6}");
        assertThat(generator.next()).isNotEqualTo(generator.next());
    }
}