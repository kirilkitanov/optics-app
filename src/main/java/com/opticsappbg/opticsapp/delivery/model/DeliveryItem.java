package com.opticsappbg.opticsapp.delivery.model;

import com.opticsappbg.opticsapp.product.model.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "delivery_items")
public class DeliveryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery; // Към коя доставка/фактура принадлежи редът

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // Кой продукт от склада се заприходява/връща

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchase_price_at_delivery", nullable = false)
    private BigDecimal purchasePriceAtDelivery; // Доставната цена по тази конкретна фактура

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime updatedOn;
}

