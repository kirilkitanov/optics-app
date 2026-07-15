package com.opticsappbg.opticsapp.web.dto;


import com.opticsappbg.opticsapp.order.model.EyeSide;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class CreateOrderItemRequest {

    private ProductCategory category;

    private EyeSide eyeSide;

    private String barcode;

    private String brand;

    private String model;

    private String sizeOrDiameter;

    private BigDecimal sph;

    private BigDecimal cyl;

    private BigDecimal addPower;

    private BigDecimal prism;

    private Integer axis;

    private String prismBase;

    private Integer quantity;

    private BigDecimal priceAtPurchase;
}
