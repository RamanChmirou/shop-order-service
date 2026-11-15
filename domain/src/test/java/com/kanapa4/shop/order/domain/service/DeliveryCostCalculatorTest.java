package com.kanapa4.shop.order.domain.service;

import com.kanapa4.shop.order.model.DeliveryMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryCostCalculatorTest {

    private final DeliveryCostCalculator calculator = new DeliveryCostCalculator();

    @ParameterizedTest
    @CsvSource({"COURIER, 15.00", "PARCEL_LOCKER, 9.99", "PICKUP, 0.00"})
    void calculate_EachMethod_ReturnsItsCost(DeliveryMethod method, String expected) {
        assertThat(calculator.calculate(method)).isEqualByComparingTo(expected);
    }
}