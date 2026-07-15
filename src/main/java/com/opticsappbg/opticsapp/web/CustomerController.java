package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.service.CustomerService;
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

    private final CustomerService customerService;

    @PostMapping("/customers/quick-create")
    @ResponseBody
    public CustomerDto quickCreate(@RequestBody CreateCustomerRequest req) {

        return customerService.quickCreateCustomer(req);
    }
}
