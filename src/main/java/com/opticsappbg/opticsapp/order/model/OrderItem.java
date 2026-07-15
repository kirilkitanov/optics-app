package com.opticsappbg.opticsapp.order.model;

import com.opticsappbg.opticsapp.product.model.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Към коя поръчка принадлежи редът

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // Коя стока от склада се изписва

    @Column(name = "eye_side")
    @Enumerated(EnumType.STRING)
    private EyeSide eyeSide;

    @Column
    private Integer axis;

    @Column(name = "prism_base")
    private String prismBase;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchase_price_at_purchase", nullable = false)
    private BigDecimal purchasePriceAtPurchase; // За доставната цена и маржа,
                                               // копие от склада (за да не се променя историята)

    @Column(name = "price_at_purchase", nullable = false)
    private BigDecimal priceAtPurchase; // ЗАМРАЗЕНАТА ЦЕНА реалната продажна цена в момента

    @Column(name = "profit_at_purchase")
    private BigDecimal profitAtPurchase;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime updatedOn;
}
