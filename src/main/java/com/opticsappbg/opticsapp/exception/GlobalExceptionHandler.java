package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.product.service.StockAdjustmentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = ProductPageController.class)
public class StockAdjustmentExceptionHandler {

    @ExceptionHandler(StockAdjustmentException.class)
    public ResponseEntity<Map<String, String>> handleStockAdjustmentException(
            StockAdjustmentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}

