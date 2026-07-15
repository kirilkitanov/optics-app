package com.opticsappbg.opticsapp.order.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    WAITING_LENSES("Чака доставка на стъкла"),
    READY_FOR_CRAFT("Предадени за изработка"),
    READY_FOR_PICKUP("Готови, за получаване"),
    COMPLETED("Получени от клиента"),
    CANCELLED("Анулирана"),
    CREATED ("Създадена нова поръчка");

    private final String displayName;
}
