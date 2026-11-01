package com.opticsappbg.opticsapp.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateDeliveryRequest {

    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private Long supplierId;
    private List<CreateDeliveryItemRequest> items;
}