package com.opticsappbg.opticsapp.web.dto;


import com.opticsappbg.opticsapp.order.model.EyeSide;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class CreateOrderItemRequest {

    @NotNull
    private ProductCategory category;

    private Long productId;

    private EyeSide eyeSide;

    private String barcode;

    @NotNull
    private String brand;

    @NotNull
    private String model;

    private String sizeOrDiameter;

    @NotNull
    private BigDecimal sph = BigDecimal.ZERO;

    @NotNull
    private BigDecimal cyl = BigDecimal.ZERO;

    @NotNull
    private BigDecimal addPower = BigDecimal.ZERO;

    @NotNull
    private BigDecimal prism = BigDecimal.ZERO;

    private Integer axis;

    private String prismBase;

    @NotNull(message = "Количеството е задължително.")
    @Min(value = 1, message = "Количеството трябва да бъде поне 1.")
    private Integer quantity;

    @NotNull(message = "Цената е задължителна.")
    @DecimalMin(value = "0.0", message = "Цената не може да бъде отрицателна.")
    private BigDecimal salePriceAtSale;
}
