package com.opticsappbg.opticsapp.web.dto;

import lombok.Data;

@Data
public class SupplierFormRequest {

    private String companyName;
    private String eik;
    private String vat;
    private String mol;
    private String phone;
    private String email;
    private String address;
    private String note;
}