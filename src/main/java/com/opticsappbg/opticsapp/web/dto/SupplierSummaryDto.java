package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.supplier.model.Supplier;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SupplierSummaryDto {

    private Supplier supplier;
    private BigDecimal totalDeliveries;
    private BigDecimal paid;
    private BigDecimal outstanding;
}