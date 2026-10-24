package com.opticsappbg.opticsapp.order.repository;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;


public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusNotInOrderByCreatedOnDesc(
            List<OrderStatus> statuses
    );

    Page<Order> findAllByOrderByCreatedOnDesc(Pageable pageable);

    Page<Order> findByCreatedOnBetweenOrderByCreatedOnDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<Order> findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
            LocalDateTime startDate,
            Pageable pageable
    );

    Page<Order> findByCreatedOnLessThanOrderByCreatedOnDesc(
            LocalDateTime endDate,
            Pageable pageable
    );

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

    @Query(
            value = """
            SELECT o FROM Order o
            JOIN o.customer c
            WHERE (
                (
                    :search IS NULL
                    AND :searchDateStart IS NULL
                    AND :searchDay IS NULL
                )
                OR (
                    :search IS NOT NULL
                    AND (
                        LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR c.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (
                    :searchDateStart IS NOT NULL
                    AND o.createdOn >= :searchDateStart
                    AND o.createdOn < :searchDateEnd
                )
                OR (
                    :searchDay IS NOT NULL
                    AND CAST(strftime('%d', o.createdOn) AS INTEGER) = :searchDay
                    AND CAST(strftime('%m', o.createdOn) AS INTEGER) = :searchMonth
                )
            )
            AND (:startDate IS NULL OR o.createdOn >= :startDate)
            AND (:endDate IS NULL OR o.createdOn < :endDate)
            ORDER BY o.createdOn DESC
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            JOIN o.customer c
            WHERE (
                (
                    :search IS NULL
                    AND :searchDateStart IS NULL
                    AND :searchDay IS NULL
                )
                OR (
                    :search IS NOT NULL
                    AND (
                        LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR c.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (
                    :searchDateStart IS NOT NULL
                    AND o.createdOn >= :searchDateStart
                    AND o.createdOn < :searchDateEnd
                )
                OR (
                    :searchDay IS NOT NULL
                    AND CAST(strftime('%d', o.createdOn) AS INTEGER) = :searchDay
                    AND CAST(strftime('%m', o.createdOn) AS INTEGER) = :searchMonth
                )
            )
            AND (:startDate IS NULL OR o.createdOn >= :startDate)
            AND (:endDate IS NULL OR o.createdOn < :endDate)
            """
    )
    Page<Order> searchOrders(
            @Param("search") String search,
            @Param("searchDateStart") LocalDateTime searchDateStart,
            @Param("searchDateEnd") LocalDateTime searchDateEnd,
            @Param("searchDay") Integer searchDay,
            @Param("searchMonth") Integer searchMonth,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
