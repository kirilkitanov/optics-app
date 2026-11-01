package com.opticsappbg.opticsapp.supplier.repository;

import com.opticsappbg.opticsapp.supplier.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCompanyNameIgnoreCase(String companyName);

    List<Supplier> findAllByOrderByCompanyNameAsc();
}