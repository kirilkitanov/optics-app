package com.opticsappbg.opticsapp.order.repository;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusNotInOrderByCreatedOnDesc(List<OrderStatus> statuses);

    List<Order> findByCreatedOnBetweenOrderByCreatedOnDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Order> findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
            LocalDateTime startDate
    );

    List<Order> findByCreatedOnLessThanOrderByCreatedOnDesc(
            LocalDateTime endDate
    );

    List<Order> findAllByOrderByCreatedOnDesc();
}


