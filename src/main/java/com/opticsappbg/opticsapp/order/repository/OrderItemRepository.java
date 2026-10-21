package com.opticsappbg.opticsapp.order.repository;

import com.opticsappbg.opticsapp.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByProductIdAndPurchasePriceAtSaleIsNull(Long productId);
}
