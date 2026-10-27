package com.opticsappbg.opticsapp.customer.service;

import com.opticsappbg.opticsapp.customer.model.CustomerPayment;
import com.opticsappbg.opticsapp.customer.repository.CustomerPaymentRepository;
import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.order.repository.OrderRepository;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.web.dto.CustomerPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerPaymentRepository customerPaymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void addPayment(
            Long orderId,
            CustomerPaymentRequest request,
            User currentUser) {

        validateRequest(request);

        Order order = findOrder(orderId);
        validatePaymentForOrder(order, request.getAmount());

        CustomerPayment payment = CustomerPayment.builder()
                .order(order)
                .amount(request.getAmount())
                .paymentDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .createdBy(currentUser)
                .build();

        customerPaymentRepository.save(payment);
    }

    @Transactional
    public void editPayment(
            Long orderId,
            Long paymentId,
            CustomerPaymentRequest request) {

        validateRequest(request);

        Order order = findOrder(orderId);

        CustomerPayment payment = customerPaymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Плащането не е намерено."));

        if (!payment.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException(
                    "Плащането не принадлежи на тази поръчка.");
        }

        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());

        customerPaymentRepository.save(payment);
    }

    @Transactional
    public void deletePayment(Long orderId, Long paymentId) {

        CustomerPayment payment = customerPaymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Плащането не е намерено."));

        if (!payment.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException(
                    "Плащането не принадлежи на тази поръчка.");
        }

        customerPaymentRepository.delete(payment);
    }

    private void validateRequest(CustomerPaymentRequest request) {

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "Сумата не може да бъде нула.");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException(
                    "Изберете начин на плащане.");
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Поръчката не е намерена: " + orderId));
    }

    private void validatePaymentForOrder(
            Order order,
            BigDecimal amount) {

        if (order.getStatus() == OrderStatus.CANCELLED
                && amount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Към анулирана поръчка не може да се добавя положително плащане. "
                            + "Може да се запише връщане на пари с отрицателна сума.");
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerPayment> getPaymentsForOrder(Long orderId) {
        return customerPaymentRepository
                .findByOrder_IdOrderByPaymentDateDesc(orderId);
    }
}
