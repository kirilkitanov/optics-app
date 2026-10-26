package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.model.Customer;
import com.opticsappbg.opticsapp.customer.service.CustomerPaymentService;
import com.opticsappbg.opticsapp.customer.service.CustomerService;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import com.opticsappbg.opticsapp.web.dto.CreateCustomerRequest;
import com.opticsappbg.opticsapp.web.dto.CustomerDto;
import com.opticsappbg.opticsapp.web.dto.CustomerFormRequest;
import com.opticsappbg.opticsapp.web.dto.CustomerPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;

@RequiredArgsConstructor
@Controller
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerPaymentService customerPaymentService;
    private final UserRepository userRepository;

    @PostMapping("/customers/quick-create")
    @ResponseBody
    public CustomerDto quickCreate(@RequestBody CreateCustomerRequest req) {
        return customerService.quickCreateCustomer(req);
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute(
                "summaries",
                customerService.getAllCustomerSummaries()
        );
        return "customers/customers";
    }

    @GetMapping("/customers/new")
    public String newCustomer(Model model) {
        model.addAttribute("customerForm", new CustomerFormRequest());
        model.addAttribute("isEdit", false);
        return "customers/customer-form";
    }

    @GetMapping("/customers/{id}/edit")
    public String editCustomer(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);

        CustomerFormRequest form = new CustomerFormRequest();
        form.setFullName(customer.getFullName());
        form.setPhone(customer.getPhone());
        form.setCity(customer.getCity());
        form.setEmail(customer.getEmail());
        form.setNote(customer.getNote());

        model.addAttribute("customerForm", form);
        model.addAttribute("customerId", id);
        model.addAttribute("isEdit", true);

        return "customers/customer-form";
    }

    @PostMapping("/customers")
    public String createCustomer(
            @ModelAttribute("customerForm") CustomerFormRequest req,
            RedirectAttributes redirectAttributes) {

        customerService.createCustomer(req);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Клиентът е създаден успешно.");

        return "redirect:/customers";
    }

    @PostMapping("/customers/{id}")
    public String updateCustomer(
            @PathVariable Long id,
            @ModelAttribute("customerForm") CustomerFormRequest req,
            RedirectAttributes redirectAttributes) {

        customerService.updateCustomer(id, req);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Промените са записани успешно.");

        return "redirect:/customers";
    }
    @GetMapping("/customers/{id}")
    public String customerDetails(
            @PathVariable Long id,
            Model model) {

        model.addAttribute("customer", customerService.getCustomerById(id));

        var orderSummaries = customerService.getCustomerOrderSummaries(id);
        model.addAttribute("orderSummaries", orderSummaries);

        java.util.Map<Long, java.util.List<com.opticsappbg.opticsapp.customer.model.CustomerPayment>>
                paymentsByOrder = new java.util.HashMap<>();

        for (var summary : orderSummaries) {
            Long orderId = summary.getOrder().getId();

            paymentsByOrder.put(
                    orderId,
                    customerPaymentService.getPaymentsForOrder(orderId)
            );
        }

        model.addAttribute("paymentsByOrder", paymentsByOrder);

        return "customers/customer-details";
    }

    @PostMapping("/customers/{customerId}/orders/{orderId}/payments")
    public String addPayment(
            @PathVariable Long customerId,
            @PathVariable Long orderId,
            @ModelAttribute CustomerPaymentRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            AuthenticationDetails details =
                    (AuthenticationDetails) authentication.getPrincipal();

            User currentUser = userRepository.findById(details.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Потребителят не е намерен."));

            customerPaymentService.addPayment(
                    orderId,
                    request,
                    currentUser
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Плащането е записано успешно."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/customers/" + customerId;
    }

}
