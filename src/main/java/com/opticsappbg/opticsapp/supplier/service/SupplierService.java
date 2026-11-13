package com.opticsappbg.opticsapp.supplier.service;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.service.ActivityLogService;
import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.repository.DeliveryRepository;
import com.opticsappbg.opticsapp.supplier.model.Supplier;
import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import com.opticsappbg.opticsapp.supplier.repository.SupplierPaymentRepository;
import com.opticsappbg.opticsapp.supplier.repository.SupplierRepository;
import com.opticsappbg.opticsapp.web.dto.SupplierFormRequest;
import com.opticsappbg.opticsapp.web.dto.SupplierSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final DeliveryRepository deliveryRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Доставчикът не е намерен."));
    }

    @Transactional
    public Supplier createSupplier(SupplierFormRequest request) {
        validateCompanyName(request.getCompanyName());

        supplierRepository.findByCompanyNameIgnoreCase(request.getCompanyName().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Вече съществува доставчик с това име.");
                });

        LocalDateTime now = LocalDateTime.now();

        Supplier supplier = Supplier.builder()
                .companyName(request.getCompanyName().trim())
                .eik(request.getEik())
                .vat(request.getVat())
                .mol(request.getMol())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .note(request.getNote())
                .createdOn(now)
                .updatedOn(now)
                .build();

        Supplier saved = supplierRepository.save(supplier);

        activityLogService.log(
                ActivityAction.CREATE,
                "SUPPLIER",
                saved.getId(),
                "Създаден доставчик: " + saved.getCompanyName()
        );

        return saved;
    }

    @Transactional
    public Supplier updateSupplier(Long id, SupplierFormRequest request) {
        validateCompanyName(request.getCompanyName());

        Supplier supplier = getSupplierById(id);

        supplierRepository.findByCompanyNameIgnoreCase(request.getCompanyName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Вече съществува доставчик с това име.");
                });

        supplier.setCompanyName(request.getCompanyName().trim());
        supplier.setEik(request.getEik());
        supplier.setVat(request.getVat());
        supplier.setMol(request.getMol());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setNote(request.getNote());
        supplier.setUpdatedOn(LocalDateTime.now());

        Supplier saved = supplierRepository.save(supplier);

        activityLogService.log(
                ActivityAction.UPDATE,
                "SUPPLIER",
                saved.getId(),
                "Редактиран доставчик: " + saved.getCompanyName()
        );

        return saved;
    }

    private void validateCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Името на фирмата е задължително.");
        }
    }

    @Transactional(readOnly = true)
    public List<SupplierSummaryDto> getAllSupplierSummaries() {
        List<Supplier> suppliers = supplierRepository.findAllByOrderByCompanyNameAsc();
        List<SupplierSummaryDto> summaries = new ArrayList<>();

        for (Supplier supplier : suppliers) {

            BigDecimal totalDeliveries = deliveryRepository
                    .findBySupplier_Id(supplier.getId())
                    .stream()
                    .map(Delivery::getTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal paid = supplierPaymentRepository
                    .findBySupplier_IdOrderByPaymentDateDesc(supplier.getId())
                    .stream()
                    .map(SupplierPayment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outstanding = totalDeliveries.subtract(paid);

            summaries.add(new SupplierSummaryDto(
                    supplier,
                    totalDeliveries,
                    paid,
                    outstanding
            ));
        }

        return summaries;
    }
}