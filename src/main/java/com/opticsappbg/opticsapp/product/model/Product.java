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
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_barcode", columnNames = "barcode")})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column
    private String barcode;

    @Column(name = "size_or_diameter")
    private String sizeOrDiameter;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal sph = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal cyl = BigDecimal.ZERO;

    @Column(name = "add_power", nullable = false)
    @Builder.Default
    private BigDecimal addPower = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal prism = BigDecimal.ZERO;

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

