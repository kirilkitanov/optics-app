package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.product.model.ProductCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DeliveryImportPreviewItem {

    // Номер на реда в Excel
    private int excelRowNumber;

    // Характеристики на продукта
    private ProductCategory category;
    private String brand;
    private String model;
    private String barcode;
    private String sizeOrDiameter;

    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private BigDecimal prism;

    // Данни за доставката
    private Integer quantity;
    private BigDecimal purchasePriceAtDelivery;

    // Резултат от съпоставянето
    private Long productId;

    // Идентификатор за нов продукт.
    // Еднакъв за редове, които трябва да използват един и същ нов продукт.
    private String newProductKey;

    // FOUND или NEW
    private String productMatchType;

    // Информация за показване в предварителния преглед
    private String productDescription;

    // Грешка конкретно за този ред
    private String errorMessage;
}