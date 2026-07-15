package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.model.Customer;
import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.web.dto.CreateCustomerRequest;
import com.opticsappbg.opticsapp.web.dto.CustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@RequiredArgsConstructor
@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;

    @PostMapping("/customers/quick-create")
    @ResponseBody
    public CustomerDto quickCreate(@RequestBody CreateCustomerRequest req) {

        Customer customer = new Customer();

        customer.setFullName(req.getName());
        customer.setPhone(req.getPhoneNumber());

        Customer saved = customerRepository.save(customer);

        return new CustomerDto(
                saved.getId(),
                saved.getFullName(),
                saved.getPhone()
        );
    }
}
