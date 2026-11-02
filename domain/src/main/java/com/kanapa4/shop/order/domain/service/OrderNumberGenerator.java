package com.kanapa4.shop.order.domain.service;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final int SUFFIX_LENGTH = 6;

    private final Clock clock;

    public String next() {
        String date = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, SUFFIX_LENGTH).toUpperCase();
        return "ORD-" + date + "-" + suffix;
    }
}