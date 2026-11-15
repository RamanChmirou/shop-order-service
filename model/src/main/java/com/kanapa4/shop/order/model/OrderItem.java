package com.kanapa4.shop.order.model;

import java.util.List;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        String productName,
        Price unitPrice,
        int quantity,
        List<SelectedOption> selectedOptions) {

    public Price lineTotal() {
        return unitPrice.times(quantity);
    }
}