package com.kanapa4.shop.order.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class Invoice {

    private final UUID id;
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;
    private String externalId;
    private String number;
    private String pdfUrl;
    private int attempts;
    private String lastError;
    private Instant issuedAt;

    public boolean isIssued() {
        return status == InvoiceStatus.ISSUED;
    }

    public void markIssued(String externalId, String number, String pdfUrl, Instant issuedAt) {
        this.status = InvoiceStatus.ISSUED;
        this.externalId = externalId;
        this.number = number;
        this.pdfUrl = pdfUrl;
        this.issuedAt = issuedAt;
        this.lastError = null;
        this.attempts++;
    }

    public void markFailed(String error) {
        this.status = InvoiceStatus.FAILED;
        this.lastError = error;
        this.attempts++;
    }
}