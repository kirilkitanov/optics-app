package com.opticsappbg.opticsapp.customer.service;

import com.opticsappbg.opticsapp.customer.model.Customer;
import com.opticsappbg.opticsapp.customer.model.CustomerPayment;
import com.opticsappbg.opticsapp.customer.repository.CustomerPaymentRepository;
import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPaymentRepository customerPaymentRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Клиентът не е намерен: " + id));
    }

    public CustomerDto quickCreateCustomer(CreateCustomerRequest req) {
        Customer customer = new Customer();
        customer.setFullName(req.getName());
        customer.setPhone(req.getPhoneNumber());
        customer.setCreatedOn(LocalDateTime.now());

        Customer saved = customerRepository.save(customer);

        return new CustomerDto(
                saved.getId(),
                saved.getFullName(),
                saved.getPhone()
        );
    }

    // Създаване на клиент от пълната форма
    public Customer createCustomer(CustomerFormRequest req) {
        Customer customer = new Customer();

        customer.setFullName(req.getFullName().trim());
        customer.setPhone(req.getPhone().trim());
        customer.setCity(req.getCity());
        customer.setEmail(req.getEmail());
        customer.setNote(req.getNote());
        customer.setCreatedOn(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    // Редакция на съществуващ клиент
    public Customer updateCustomer(Long id, CustomerFormRequest req) {
        Customer customer = getCustomerById(id);

        customer.setFullName(req.getFullName().trim());
        customer.setPhone(req.getPhone().trim());
        customer.setCity(req.getCity());
        customer.setEmail(req.getEmail());
        customer.setNote(req.getNote());
        customer.setUpdatedOn(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> getAllCustomerSummaries() {
        List<Customer> customers = customerRepository.findAll();
        List<CustomerSummaryDto> summaries = new ArrayList<>();

        for (Customer customer : customers) {
            BigDecimal totalOrders = customer.getOrders().stream()
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .map(Order::getTotal)
                    .filter(total -> total != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal paid = customerPaymentRepository
                    .findByOrder_Customer_IdAndOrder_StatusNot(
                            customer.getId(),
                            OrderStatus.CANCELLED
                    )
                    .stream()
                    .map(CustomerPayment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outstanding = totalOrders.subtract(paid);

            summaries.add(new CustomerSummaryDto(
                    customer,
                    totalOrders,
                    paid,
                    outstanding
            ));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryDto> getCustomerOrderSummaries(Long customerId) {
        Customer customer = getCustomerById(customerId);

        List<CustomerOrderSummaryDto> summaries = new ArrayList<>();

                customer.getOrders().stream()
                .sorted(Comparator.comparing(
                        Order::getCreatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .forEach(order -> {
                    BigDecimal paid = customerPaymentRepository
                            .findByOrder_IdAndOrder_StatusNot(
                                    order.getId(),
                                    OrderStatus.CANCELLED
                            )
                            .stream()
                            .map(CustomerPayment::getAmount)
                            .filter(amount -> amount != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal total = order.getTotal() != null
                            ? order.getTotal()
                            : BigDecimal.ZERO;

                    BigDecimal outstanding = order.getStatus() == OrderStatus.CANCELLED
                            ? BigDecimal.ZERO
                            : total.subtract(paid);

                    summaries.add(new CustomerOrderSummaryDto(
                            order,
                            paid,
                            outstanding
                    ));
                });

        return summaries;
    }

}

