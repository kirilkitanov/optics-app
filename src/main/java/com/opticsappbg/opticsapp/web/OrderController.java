package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.customer.service.CustomerService;
import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.order.service.ExcelOrderExportService;
import com.opticsappbg.opticsapp.order.service.OrderService;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.LocalDate;


@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final UserService userService;
    private final ProductService productService;
    private final ExcelOrderExportService excelOrderExportService;

    // 🔹 SHOW FORM
    @GetMapping("/new")
    public String showCreateOrderForm(Model model) {

        addOrderFormData(model);
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
            addOrderFormData(model);

            model.addAttribute(
                    "productError",
                    bindingResult.getAllErrors()
                            .stream()
                            .findFirst()
                            .map(error -> error.getDefaultMessage())
                            .orElse("Моля, проверете въведените данни.")
            );

            return "orders/new-order";
        }

        User user = userService.getById(principal.getUserId());

        try {
            orderService.createOrder(request, user);
            return "redirect:/orders";

        } catch (IllegalArgumentException e) {

            addOrderFormData(model);
            model.addAttribute("productError", e.getMessage());

            return "orders/new-order";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditOrderForm(@PathVariable Long id, Model model) {

        Order order = orderService.getOrderById(id);

        CreateOrderRequest request = orderService.buildEditRequest(order);

        model.addAttribute("order", order);
        model.addAttribute("createOrderRequest", request);
        model.addAttribute(
                "readOnly",
                order.getStatus() == OrderStatus.CANCELLED);
        addOrderFormData(model);

        return "orders/new-order";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute("createOrderRequest") CreateOrderRequest request,
            BindingResult bindingResult, Model model) {

        Order existingOrder = orderService.getOrderById(id);
        if (existingOrder.getStatus() == OrderStatus.CANCELLED) {
            return "redirect:/orders";
        }

        if (bindingResult.hasErrors()) {

            model.addAttribute("order", existingOrder);
            addOrderFormData(model);

            model.addAttribute(
                    "productError",
                    bindingResult.getAllErrors()
                            .stream()
                            .findFirst()
                            .map(error -> error.getDefaultMessage())
                            .orElse("Моля, проверете въведените данни.")
            );

            return "orders/new-order";
        }

        try {
            orderService.updateOrder(id, request);
            return "redirect:/home";

        } catch (IllegalArgumentException e) {

            Order order = orderService.getOrderById(id);

            model.addAttribute("order", order);
            addOrderFormData(model);
            model.addAttribute("productError", e.getMessage());

            return "orders/new-order";
        }
    }

    @GetMapping
    public String showOrdersList(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            Model model) {

        int pageSize = 10;

        if (page < 0) {
            page = 0;
        }

        Page<Order> ordersPage = orderService.searchOrders(
                search,
                startDate,
                endDate,
                PageRequest.of(page, pageSize)
        );

        if (ordersPage.getTotalPages() > 0
                && page >= ordersPage.getTotalPages()) {

            page = ordersPage.getTotalPages() - 1;

            ordersPage = orderService.searchOrders(
                    search,
                    startDate,
                    endDate,
                    PageRequest.of(page, pageSize)
            );
        }

        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);

        return "orders/orders-list";
    }

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {

        orderService.updateOrderStatus(id, status);

        return "redirect:/home";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) throws IOException {

        byte[] excelFile = excelOrderExportService.exportExcel(startDate, endDate);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"orders.xlsx\""
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(excelFile);
    }

    private void addOrderFormData(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
        model.addAttribute("brands", productService.getUniqueBrandNames());
    }

}
