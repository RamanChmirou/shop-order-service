package com.kanapa4.shop.order.model;

import java.time.Instant;

public record IssuedInvoice(String externalId, String number, String pdfUrl, Instant issuedAt) {
}