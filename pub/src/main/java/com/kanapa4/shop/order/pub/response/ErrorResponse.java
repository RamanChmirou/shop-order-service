package com.kanapa4.shop.order.pub.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, Object> details) {
}