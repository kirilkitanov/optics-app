
package com.opticsappbg.opticsapp.web.dto;

import lombok.Data;

@Data
public class CustomerFormRequest {
    private String fullName;
    private String phone;
    private String city;
    private String email;
    private String note;
}
