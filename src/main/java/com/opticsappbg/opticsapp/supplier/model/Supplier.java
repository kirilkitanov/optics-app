package com.opticsappbg.opticsapp.supplier.model;


import jakarta.persistence.*;
        import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, unique = true)
    private String companyName;

    @Column
    private String eik;

    @Column
    private String vat;

    @Column
    private String mol;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private String address;

    @Column(length = 500)
    private String note;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column
    private LocalDateTime updatedOn;
}

