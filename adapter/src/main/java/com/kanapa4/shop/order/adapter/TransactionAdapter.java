package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.port.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}