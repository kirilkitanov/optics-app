package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.product.model.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductLookupResponse {

    private Long id;
    private String barcode;
    private ProductCategory category;
    private String brand;
    private String model;
    private String sizeOrDiameter;
    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private BigDecimal prism;
    private BigDecimal retailPrice;
}

