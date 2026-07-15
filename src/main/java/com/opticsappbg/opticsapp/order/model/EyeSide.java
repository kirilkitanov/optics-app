package com.opticsappbg.opticsapp.order.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EyeSide {
    RIGHT("Дясно око"),
    LEFT("Ляво око"),
    NONE("Продукт");

    private final String displayName;
}
