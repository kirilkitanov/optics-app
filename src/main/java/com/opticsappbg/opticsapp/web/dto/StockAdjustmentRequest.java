package com.opticsappbg.opticsapp.web.dto;

public record StockAdjustmentRequest(
        Integer newQuantity,
        String reason
) {
}

