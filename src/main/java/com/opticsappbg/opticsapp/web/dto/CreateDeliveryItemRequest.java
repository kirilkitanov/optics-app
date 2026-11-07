package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.product.model.ProductCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateDeliveryItemRequest {

    private Long productId;
    private String newProductKey;
    private ProductCategory category;
    private String brand;
    private String model;
    private String barcode;
    private String sizeOrDiameter;

    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private BigDecimal prism;

    private Integer quantity;
    private BigDecimal purchasePriceAtDelivery;
}
