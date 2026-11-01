package com.opticsappbg.opticsapp.supplier.service;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.repository.DeliveryRepository;
import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import com.opticsappbg.opticsapp.supplier.repository.SupplierPaymentRepository;
import com.opticsappbg.opticsapp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final DeliveryRepository deliveryRepository;

    @Transactional
    public SupplierPayment addPayment(
            Long deliveryId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String note,
            User currentUser) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумата трябва да бъде по-голяма от 0.");
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Фактурата не е намерена."));

        List<SupplierPayment> payments =
                supplierPaymentRepository.findByDelivery_IdOrderByPaymentDateDesc(deliveryId);

        BigDecimal alreadyPaid = payments.stream()
                .map(SupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SupplierPayment payment = new SupplierPayment();
        payment.setDelivery(delivery);
        payment.setSupplier(delivery.getSupplier());
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod);
        payment.setNote(note);
        payment.setCreatedOn(LocalDateTime.now());
        payment.setCreatedBy(currentUser);

        return supplierPaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<SupplierPayment> getPaymentsForDelivery(Long deliveryId) {
        return supplierPaymentRepository
                .findByDelivery_IdOrderByPaymentDateDesc(deliveryId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaid(Long deliveryId) {
        return supplierPaymentRepository
                .findByDelivery_IdOrderByPaymentDateDesc(deliveryId)
                .stream()
                .map(SupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getRemainingAmount(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Фактурата не е намерена."));

        return delivery.getTotalAmount().subtract(getTotalPaid(deliveryId));
    }

    @Transactional
    public SupplierPayment addRefund(
            Long deliveryId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String note,
            User currentUser) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумата трябва да бъде по-голяма от 0.");
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Фактурата не е намерена."));

        BigDecimal remaining = getRemainingAmount(deliveryId);
        BigDecimal overpaid = remaining.negate();

        if (remaining.compareTo(BigDecimal.ZERO) >= 0 || amount.compareTo(overpaid) > 0) {
            throw new IllegalArgumentException(
                    "Сумата надвишава сумата, която доставчикът трябва да ни върне.");
        }

        SupplierPayment refund = new SupplierPayment();
        refund.setDelivery(delivery);
        refund.setSupplier(delivery.getSupplier());
        refund.setAmount(amount.negate());
        refund.setPaymentDate(LocalDateTime.now());
        refund.setPaymentMethod(paymentMethod);
        refund.setNote(note);
        refund.setCreatedOn(LocalDateTime.now());
        refund.setCreatedBy(currentUser);

        return supplierPaymentRepository.save(refund);
    }
}
