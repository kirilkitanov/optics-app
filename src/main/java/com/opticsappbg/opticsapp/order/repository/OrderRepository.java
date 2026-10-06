package com.opticsappbg.opticsapp.order.repository;



import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatusNotInOrderByCreatedOnDesc(List<OrderStatus> statuses);
}
