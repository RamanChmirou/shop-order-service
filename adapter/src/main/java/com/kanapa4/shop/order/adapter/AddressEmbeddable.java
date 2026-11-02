package com.kanapa4.shop.order.adapter;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AddressEmbeddable {

    private String street;
    private String city;
    private String postalCode;
    private String country;
}
