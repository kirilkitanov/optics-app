package com.opticsappbg.opticsapp.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Column
    private String brand;

    @Column
    private String model;

    @Column
    private String barcode;

    @Column(name = "size_or_diameter")
    private String sizeOrDiameter;

    @Column
    private BigDecimal sph;

    @Column
    private BigDecimal cyl;

    @Column(name = "add_power")
    private BigDecimal addPower;

    @Column
    private BigDecimal prism;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "retail_price", nullable = false)
    private BigDecimal retailPrice;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime updatedOn;

    @Column(name = "is_active")
    private boolean active = true;

}

