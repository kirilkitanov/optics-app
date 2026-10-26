package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.customer.model.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CustomerSummaryDto {

    private Customer customer;
    private BigDecimal totalOrders;
    private BigDecimal paid;
    private BigDecimal outstanding;
}

