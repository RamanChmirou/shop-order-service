package com.kanapa4.shop.order.domain.port;

import java.util.function.Supplier;

public interface TransactionPort {

    <T> T execute(Supplier<T> action);
}