package com.kanapa4.shop.order.domain.validator;

import com.kanapa4.shop.order.model.CartSnapshot;
import com.kanapa4.shop.order.model.exception.EmptyCartException;
import com.kanapa4.shop.order.model.exception.InvalidOrderDataException;
import com.kanapa4.shop.order.model.exception.PriceChangedException;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.type.DeliveryMethod;

import java.math.BigDecimal;
import java.util.UUID;

public class CheckoutValidator {

    public void validate(UUID userId, CheckoutRequest request, CartSnapshot cart) {
        requireItems(userId, cart);
        requireAddresses(request);
        requireSameTotal(request.expectedTotal(), cart.totalAmount());
    }

    private void requireItems(UUID userId, CartSnapshot cart) {
        if (cart.items().isEmpty()) {
            throw new EmptyCartException(userId);
        }
    }

    private void requireAddresses(CheckoutRequest request) {
        if (request.deliveryMethod() != DeliveryMethod.PICKUP && request.shippingAddress() == null) {
            throw new InvalidOrderDataException(
                    "Shipping address is required for " + request.deliveryMethod() + " delivery");
        }
        if (request.shippingAddress() == null && request.billingAddress() == null) {
            throw new InvalidOrderDataException("Billing address is required when there is no shipping address");
        }
    }

    private void requireSameTotal(BigDecimal expectedTotal, BigDecimal actualTotal) {
        if (expectedTotal.compareTo(actualTotal) != 0) {
            throw new PriceChangedException(expectedTotal, actualTotal);
        }
    }
}