package com.opticsappbg.opticsapp.web.dto;


import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String name;
    private String phoneNumber;
}
