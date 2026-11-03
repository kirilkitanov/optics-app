package com.opticsappbg.opticsapp.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateDeliveryRequest {

    private String invoiceNumber;
    private LocalDate invoiceDate;
    private Long supplierId;
    private List<CreateDeliveryItemRequest> items;
    private String note;
    private BigDecimal discountPercentage;
    private BigDecimal totalAmount;
}