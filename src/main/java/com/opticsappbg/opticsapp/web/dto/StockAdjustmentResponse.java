package com.opticsappbg.opticsapp.web.dto;

public record StockAdjustmentResponse(
        boolean changed,
        Long productId,
        Integer previousQuantity,
        Integer newQuantity,
        Integer difference,
        String message
) {
}
