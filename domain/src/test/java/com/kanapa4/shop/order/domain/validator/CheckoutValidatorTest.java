package com.kanapa4.shop.order.domain.validator;

import com.kanapa4.shop.order.model.exception.EmptyCartException;
import com.kanapa4.shop.order.model.exception.InvalidOrderDataException;
import com.kanapa4.shop.order.model.exception.PriceChangedException;
import com.kanapa4.shop.order.pub.type.DeliveryMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.kanapa4.shop.order.domain.testdata.OrderFixtures.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutValidatorTest {

    private final CheckoutValidator validator = new CheckoutValidator();

    @Test
    void validate_CourierWithShippingAddressAndSameTotal_Passes() {
        assertThatCode(() -> validator.validate(USER_ID, checkoutRequest(CART_TOTAL), cartSnapshot()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_SameTotalWithDifferentScale_Passes() {
        assertThatCode(() -> validator.validate(USER_ID, checkoutRequest(new BigDecimal("10398.0")), cartSnapshot()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_EmptyCart_ThrowsEmptyCart() {
        assertThatThrownBy(() -> validator.validate(USER_ID, checkoutRequest(CART_TOTAL), emptyCart()))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void validate_DifferentTotal_ThrowsPriceChanged() {
        assertThatThrownBy(() -> validator.validate(USER_ID, checkoutRequest(new BigDecimal("1.00")), cartSnapshot()))
                .isInstanceOf(PriceChangedException.class);
    }

    @Test
    void validate_CourierWithoutShippingAddress_Throws() {
        var request = checkoutRequest(DeliveryMethod.COURIER, null, addressRequest(), CART_TOTAL);

        assertThatThrownBy(() -> validator.validate(USER_ID, request, cartSnapshot()))
                .isInstanceOf(InvalidOrderDataException.class)
                .hasMessageContaining("Shipping address");
    }

    @Test
    void validate_PickupWithoutAnyAddress_Throws() {
        var request = checkoutRequest(DeliveryMethod.PICKUP, null, null, CART_TOTAL);

        assertThatThrownBy(() -> validator.validate(USER_ID, request, cartSnapshot()))
                .isInstanceOf(InvalidOrderDataException.class)
                .hasMessageContaining("Billing address");
    }

    @Test
    void validate_PickupWithBillingAddressOnly_Passes() {
        var request = checkoutRequest(DeliveryMethod.PICKUP, null, addressRequest(), CART_TOTAL);

        assertThatCode(() -> validator.validate(USER_ID, request, cartSnapshot()))
                .doesNotThrowAnyException();
    }
}