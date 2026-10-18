package com.kanapa4.shop.order.pub.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 16) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country) {
}