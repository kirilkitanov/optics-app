package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.service.CustomerService;
import com.opticsappbg.opticsapp.order.service.OrderService;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final UserService userService;
    private final ProductService productService;

    // 🔹 SHOW FORM
    @GetMapping("/new")
    public String showCreateOrderForm(Model model) {

        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("createOrderRequest", new CreateOrderRequest());

        return "orders/new-order";
    }

    // 🔹 CREATE ORDER
    @PostMapping("/new")
    public String createOrder(
            @AuthenticationPrincipal AuthenticationDetails principal,
            @ModelAttribute CreateOrderRequest request) {

        User user = userService.getById(principal.getUserId());

        orderService.createOrder(request, user);

        return "redirect:/orders";
    }

    @GetMapping
    public String showOrdersList(Model model) {

        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/orders-list";
    }

}
