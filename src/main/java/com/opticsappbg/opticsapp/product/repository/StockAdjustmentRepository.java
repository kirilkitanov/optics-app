package com.opticsappbg.opticsapp.product.repository;

import com.opticsappbg.opticsapp.product.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentRepository
        extends JpaRepository<StockAdjustment, Long> {

    List<StockAdjustment> findByProductIdOrderByCreatedOnDesc(Long productId);
}

