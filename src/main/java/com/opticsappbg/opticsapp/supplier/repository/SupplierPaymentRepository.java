package com.opticsappbg.opticsapp.supplier.repository;

import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findByDelivery_IdOrderByPaymentDateDesc(Long deliveryId);

    List<SupplierPayment> findBySupplier_IdOrderByPaymentDateDesc(Long supplierId);
}