package com.opticsappbg.opticsapp.web.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateOrderRequest {

    private Long customerId;

    private BigDecimal totalBeforeDiscount;

    private BigDecimal discountPercentage;

    private BigDecimal total;

    private BigDecimal manualTotal;

    private List<CreateOrderItemRequest> items = new ArrayList<>();
}
