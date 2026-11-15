package com.kanapa4.shop.order.pub.response;

import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        PriceResponse unitPrice,
        int quantity,
        PriceResponse lineTotal,
        List<SelectedOptionResponse> selectedOptions) {
}