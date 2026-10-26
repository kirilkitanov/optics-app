package com.opticsappbg.opticsapp.customer.repository;

import com.opticsappbg.opticsapp.customer.model.CustomerPayment;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerPaymentRepository
        extends JpaRepository<CustomerPayment, Long> {

    List<CustomerPayment> findByOrder_Customer_IdAndOrder_StatusNot(
            Long customerId,
            OrderStatus status
    );

    List<CustomerPayment> findByOrder_IdAndOrder_StatusNot(
            Long orderId,
            OrderStatus status
    );

    List<CustomerPayment> findByOrder_IdOrderByPaymentDateDesc(Long orderId);

}
