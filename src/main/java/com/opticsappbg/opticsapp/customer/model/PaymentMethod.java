package com.opticsappbg.opticsapp.customer.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH("в брой"),
    BANK("по банка");

    private final String displayName;
}
