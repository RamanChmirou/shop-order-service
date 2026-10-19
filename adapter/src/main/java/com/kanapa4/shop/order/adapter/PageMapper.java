package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.exception.InvalidOrderDataException;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class PageMapper {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "grandTotal", "orderNumber", "status");

    private PageMapper() {
    }

    public static org.springframework.data.domain.Pageable toSpringPageable(Pageable pageable) {
        if (!SORTABLE_FIELDS.contains(pageable.sortBy())) {
            throw new InvalidOrderDataException("Unsupported sort field: " + pageable.sortBy());
        }
        Sort.Direction direction = Sort.Direction.valueOf(pageable.direction().name());
        Sort sort = Sort.by(direction, pageable.sortBy()).and(Sort.by("id"));
        return PageRequest.of(pageable.page(), pageable.size(), sort);
    }

    public static <S, T> Page<T> toPojoPage(org.springframework.data.domain.Page<S> springPage,
                                            Function<S, T> mapper) {
        List<T> content = springPage.getContent().stream().map(mapper).toList();
        return new Page<>(content, springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements(), springPage.getTotalPages());
    }
}