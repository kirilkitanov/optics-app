package com.opticsappbg.opticsapp.customer.service;

import com.opticsappbg.opticsapp.customer.model.Customer;
import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.web.dto.CreateCustomerRequest;
import com.opticsappbg.opticsapp.web.dto.CustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
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
}
