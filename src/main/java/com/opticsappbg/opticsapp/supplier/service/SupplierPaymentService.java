package com.opticsappbg.opticsapp.supplier.service;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.service.ActivityLogService;
import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.repository.DeliveryRepository;
import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import com.opticsappbg.opticsapp.supplier.repository.SupplierPaymentRepository;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.model.UserRole;
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
    private final ActivityLogService activityLogService;

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

        SupplierPayment payment = new SupplierPayment();
        payment.setDelivery(delivery);
        payment.setSupplier(delivery.getSupplier());
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod);
        payment.setNote(note);
        payment.setCreatedOn(LocalDateTime.now());
        payment.setCreatedBy(currentUser);

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        activityLogService.log(
                ActivityAction.PAYMENT,
                "SUPPLIER_PAYMENT",
                savedPayment.getId(),
                "Извършено плащане към доставчик "
                        + delivery.getSupplier().getCompanyName()
                        + " по доставка №" + delivery.getInvoiceNumber()
        );

        return savedPayment;
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

        if (amount == null || amount.compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException(
                    "Сумата трябва да бъде отрицателна.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Изберете начин на плащане.");
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Фактурата не е намерена."));

        SupplierPayment refund = new SupplierPayment();
        refund.setDelivery(delivery);
        refund.setSupplier(delivery.getSupplier());
        refund.setAmount(amount);
        refund.setPaymentDate(LocalDateTime.now());
        refund.setPaymentMethod(paymentMethod);
        refund.setNote(note);
        refund.setCreatedOn(LocalDateTime.now());
        refund.setCreatedBy(currentUser);

        SupplierPayment savedRefund = supplierPaymentRepository.save(refund);

        activityLogService.log(
                ActivityAction.REFUND,
                "SUPPLIER_PAYMENT",
                savedRefund.getId(),
                "Получено възстановяване от доставчик "
                        + delivery.getSupplier().getCompanyName()
                        + " по доставка №" + delivery.getInvoiceNumber()
        );

        return savedRefund;
    }

    @Transactional
    public void editPayment(
            Long deliveryId,
            Long paymentId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String note,
            User currentUser) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Сумата не може да бъде нула.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Изберете начин на плащане.");
        }

        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Плащането не е намерено."));

        if (!payment.getDelivery().getId().equals(deliveryId)) {
            throw new IllegalArgumentException(
                    "Плащането не принадлежи на тази фактура.");
        }

        validatePaymentPermission(payment, currentUser);

        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setNote(note);
        payment.setUpdatedOn(LocalDateTime.now());

        supplierPaymentRepository.save(payment);

        activityLogService.log(
                ActivityAction.UPDATE,
                "SUPPLIER_PAYMENT",
                payment.getId(),
                "Редактиран финансов запис към доставка №"
                        + payment.getDelivery().getInvoiceNumber()
        );
    }

    @Transactional
    public void deletePayment(
            Long deliveryId,
            Long paymentId,
            User currentUser) {

        SupplierPayment payment = supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Плащането не е намерено."));

        if (!payment.getDelivery().getId().equals(deliveryId)) {
            throw new IllegalArgumentException(
                    "Плащането не принадлежи на тази фактура.");
        }

        validatePaymentPermission(payment, currentUser);

        activityLogService.log(
                ActivityAction.DELETE,
                "SUPPLIER_PAYMENT",
                payment.getId(),
                (payment.getAmount().compareTo(BigDecimal.ZERO) < 0
                        ? "Изтрито възстановяване по доставка №"
                        : "Изтрито плащане по доставка №")
                        + payment.getDelivery().getInvoiceNumber()
        );

        supplierPaymentRepository.delete(payment);    }

    private void validatePaymentPermission(
            SupplierPayment payment,
            User currentUser) {

        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }

        if (!payment.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(
                    "Нямате права да редактирате или изтривате това плащане.");
        }
    }

}
