package com.kanapa4.shop.order.pub.request;

import com.kanapa4.shop.order.pub.type.DeliveryMethod;
import com.kanapa4.shop.order.pub.type.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CheckoutRequest(
        @NotNull @Valid CustomerRequest customer,
        @NotNull DeliveryMethod deliveryMethod,
        @NotNull PaymentMethod paymentMethod,
        @Valid AddressRequest shippingAddress,
        @Valid AddressRequest billingAddress,
        @NotNull @Positive BigDecimal expectedTotal) {
}