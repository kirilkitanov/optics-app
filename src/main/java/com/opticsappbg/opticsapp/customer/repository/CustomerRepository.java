package com.opticsappbg.opticsapp.customer.repository;

import com.opticsappbg.opticsapp.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository <Customer, Long> {
}
