package com.kanapa4.shop.order.pub.page;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}