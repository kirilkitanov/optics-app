package com.opticsappbg.opticsapp.delivery.repository;

import com.opticsappbg.opticsapp.delivery.model.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, Long> {

    boolean existsByProduct_Id(Long productId);

}