package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.InvoicePort;
import com.kanapa4.shop.order.model.IssuedInvoice;
import com.kanapa4.shop.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StubInvoiceAdapter implements InvoicePort {

    private final Clock clock;

    @Override
    public IssuedInvoice issue(Order order) {
        String number = "INV/" + order.getOrderNumber();
        log.info("Stub invoice {} issued for order {}", number, order.getOrderNumber());
        return new IssuedInvoice(UUID.randomUUID().toString(), number,
                "http://localhost/invoices/" + number + ".pdf", Instant.now(clock));
    }
}