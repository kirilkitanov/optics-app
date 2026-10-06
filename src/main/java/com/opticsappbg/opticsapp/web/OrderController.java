package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.service.CustomerService;
import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.service.OrderService;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

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
        model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
        model.addAttribute("brands", productService.getUniqueBrandNames());
        CreateOrderRequest request = new CreateOrderRequest();
        request.getItems().add(new CreateOrderItemRequest());
        model.addAttribute("createOrderRequest", request);

        return "orders/new-order";
    }

    // 🔹 CREATE ORDER
    @PostMapping("/new")
    public String createOrder(
            @AuthenticationPrincipal AuthenticationDetails principal,
            @Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
            BindingResult bindingResult,
            Model model){

        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.getAllCustomers());
            model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
            model.addAttribute("brands", productService.getUniqueBrandNames());
            return "orders/new-order";
        }

            User user = userService.getById(principal.getUserId());

        orderService.createOrder(request, user);

        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String showEditOrderForm(
            @PathVariable Long id,
            Model model) {

        Order order = orderService.getOrderById(id);

        CreateOrderRequest request = orderService.buildEditRequest(order);

        model.addAttribute("order", order);
        model.addAttribute("createOrderRequest", request);

        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
        model.addAttribute("brands", productService.getUniqueBrandNames());

        return "orders/new-order";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            Order order = orderService.getOrderById(id);

            model.addAttribute("order", order);
            model.addAttribute("customers", customerService.getAllCustomers());
            model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
            model.addAttribute("brands", productService.getUniqueBrandNames());

            return "orders/new-order";
        }

        orderService.updateOrder(id, request);

        return "redirect:/orders";
    }

    @GetMapping
    public String showOrdersList(Model model) {

        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/orders-list";
    }

}
