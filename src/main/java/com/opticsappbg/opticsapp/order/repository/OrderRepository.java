package com.opticsappbg.opticsapp.order.repository;



import com.opticsappbg.opticsapp.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
