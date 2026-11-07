package com.opticsappbg.opticsapp.delivery.model;

import com.opticsappbg.opticsapp.supplier.model.Supplier;
import com.opticsappbg.opticsapp.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(length = 500)
    private String note;

    @Column(name = "total_before_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBeforeDiscount;

    @Column(name = "discount_percentage", nullable = false, precision = 8, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "update_product_purchase_prices", nullable = false)
    @Builder.Default
    private boolean updateProductPurchasePrices = true;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime updatedOn;

    @Builder.Default
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;
}

