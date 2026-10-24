package com.opticsappbg.opticsapp.product.dto;

public record StockAdjustmentRequest(
        Integer newQuantity,
        String reason
) {
}

