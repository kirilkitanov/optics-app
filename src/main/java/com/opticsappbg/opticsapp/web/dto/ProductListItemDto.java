package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;

import java.math.BigDecimal;

public record ProductListItemDto(
        Long id,
        ProductCategory category,
        String brand,
        String model,
        String barcode,
        String sizeOrDiameter,
        BigDecimal sph,
        BigDecimal cyl,
        BigDecimal addPower,
        BigDecimal prism,
        Integer quantity,
        BigDecimal retailPrice,
        BigDecimal purchasePrice,
        String note
) {
    public static ProductListItemDto from(Product product, boolean canViewPurchasePrice) {
        return new ProductListItemDto(
                product.getId(),
                product.getCategory(),
                product.getBrand(),
                product.getModel(),
                product.getBarcode(),
                product.getSizeOrDiameter(),
                product.getSph(),
                product.getCyl(),
                product.getAddPower(),
                product.getPrism(),
                product.getQuantity(),
                product.getRetailPrice(),
                canViewPurchasePrice ? product.getPurchasePrice() : null,
                product.getNote()
        );
    }
}
