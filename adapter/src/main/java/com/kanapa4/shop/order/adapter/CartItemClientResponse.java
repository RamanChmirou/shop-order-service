package com.kanapa4.shop.order.adapter;

import java.util.List;
import java.util.UUID;

public record CartItemClientResponse(
        UUID id,
        UUID productId,
        String productName,
        PriceClientResponse unitPrice,
        int quantity,
        PriceClientResponse lineTotal,
        List<SelectedOptionClientResponse> selectedOptions) {}