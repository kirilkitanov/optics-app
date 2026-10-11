package com.opticsappbg.opticsapp.web.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

@Data
public class CreateOrderRequest {

    @NotNull(message = "Моля, изберете клиент.")
    private Long customerId;

    private BigDecimal totalBeforeDiscount;

    private BigDecimal discountPercentage;

    private BigDecimal total;

    private BigDecimal manualTotal;

    private String note;

    @Valid
    private List<CreateOrderItemRequest> items = new ArrayList<>();
}
