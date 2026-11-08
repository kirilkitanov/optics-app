package com.opticsappbg.opticsapp.delivery.repository;

import com.opticsappbg.opticsapp.delivery.model.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Query("""
        SELECT d FROM Delivery d
        JOIN d.supplier s
        WHERE (
            :search IS NULL
            OR LOWER(d.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:startDate IS NULL OR d.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR d.invoiceDate < :endDate)
        ORDER BY d.invoiceDate DESC
        """)

    Page<Delivery> searchDeliveries(
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    List<Delivery> findBySupplier_Id(Long supplierId);

    Optional<Delivery> findFirstByItems_Product_IdOrderByInvoiceDateDescIdDesc(Long productId);

    @Query("""
    SELECT d FROM Delivery d
    JOIN FETCH d.supplier s
    WHERE (:startDate IS NULL OR d.invoiceDate >= :startDate)
      AND (:endDate IS NULL OR d.invoiceDate < :endDate)
    ORDER BY d.invoiceDate DESC
    """)
    List<Delivery> findAllForSearch(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
