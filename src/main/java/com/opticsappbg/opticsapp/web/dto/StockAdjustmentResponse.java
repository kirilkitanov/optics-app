package com.opticsappbg.opticsapp.product.dto;

public record StockAdjustmentResponse(
        boolean changed,
        Long productId,
        Integer previousQuantity,
        Integer newQuantity,
        Integer difference,
        String message
) {
}
