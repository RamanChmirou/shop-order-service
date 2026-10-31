package com.kanapa4.shop.order.pub.response;

import com.kanapa4.shop.order.pub.type.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(UUID id, InvoiceStatus status, String number, String pdfUrl, Instant issuedAt) {
}