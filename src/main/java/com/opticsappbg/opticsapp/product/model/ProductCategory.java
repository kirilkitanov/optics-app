package com.opticsappbg.opticsapp.product.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    FRAME("Рамка"),
    SUNGLASSES("Слънчеви очила"),
    LENS("Диоптрично стъкло"),
    CONTACT_LENS("Контактна леща"),
    ACCESSORY("Аксесоар");

    private final String displayName;
}