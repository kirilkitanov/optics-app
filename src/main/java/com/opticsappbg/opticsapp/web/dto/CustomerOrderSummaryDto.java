package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.order.model.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CustomerOrderSummaryDto {

    private Order order;
    private BigDecimal paid;
    private BigDecimal outstanding;
}

