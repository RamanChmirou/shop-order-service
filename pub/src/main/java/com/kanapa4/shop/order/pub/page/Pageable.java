package com.kanapa4.shop.order.pub.page;

public record Pageable(int page, int size, String sortBy, SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT = "createdAt";
}