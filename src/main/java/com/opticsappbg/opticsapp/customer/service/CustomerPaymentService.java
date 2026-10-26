
package com.opticsappbg.opticsapp.customer.service;

import com.opticsappbg.opticsapp.customer.model.CustomerPayment;
import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
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

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумата на плащането трябва да е по-голяма от 0.");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException(
                    "Изберете начин на плащане.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Поръчката не е намерена: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Не може да се добавя плащане към анулирана поръчка.");
        }

        CustomerPayment payment = CustomerPayment.builder()
                .order(order)
                .amount(request.getAmount())
                .paymentDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .createdBy(currentUser)
                .build();

        customerPaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<CustomerPayment> getPaymentsForOrder(Long orderId) {
        return customerPaymentRepository
                .findByOrder_IdOrderByPaymentDateDesc(orderId);
    }
}
