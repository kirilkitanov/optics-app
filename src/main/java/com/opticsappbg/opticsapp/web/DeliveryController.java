package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import com.opticsappbg.opticsapp.supplier.service.SupplierPaymentService;
import com.opticsappbg.opticsapp.supplier.service.SupplierPaymentService;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

@Controller
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final SupplierPaymentService supplierPaymentService;

    @GetMapping
    public String showDeliveriesList(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0") int page,

            Model model) {

        int pageSize = 15;

        if (page < 0) {
            page = 0;
        }

        Page<Delivery> deliveriesPage = deliveryService.searchDeliveries(
                search,
                startDate,
                endDate,
                PageRequest.of(page, pageSize)
        );

        if (deliveriesPage.getTotalPages() > 0
                && page >= deliveriesPage.getTotalPages()) {

            page = deliveriesPage.getTotalPages() - 1;

            deliveriesPage = deliveryService.searchDeliveries(
                    search,
                    startDate,
                    endDate,
                    PageRequest.of(page, pageSize)
            );
        }

        Map<Long, BigDecimal> paidAmounts = new HashMap<>();
        Map<Long, BigDecimal> remainingAmounts = new HashMap<>();

        for (Delivery delivery : deliveriesPage.getContent()) {

            BigDecimal paid = supplierPaymentService.getTotalPaid(delivery.getId());

            BigDecimal remaining = delivery.getTotalAmount().subtract(paid);

            paidAmounts.put(delivery.getId(), paid);
            remainingAmounts.put(delivery.getId(), remaining);
        }

        model.addAttribute("deliveries", deliveriesPage.getContent());
        model.addAttribute("deliveriesPage", deliveriesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", deliveriesPage.getTotalPages());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);
        model.addAttribute("paidAmounts", paidAmounts);
        model.addAttribute("remainingAmounts", remainingAmounts);

        return "deliveries/deliveries-list";
    }

    @GetMapping("/{id}")
    public String deliveryDetails(
            @PathVariable Long id,
            Model model) {

        Delivery delivery = deliveryService.getDeliveryById(id);

        model.addAttribute("delivery", delivery);
        model.addAttribute(
                "payments",
                supplierPaymentService.getPaymentsForDelivery(id)
        );
        model.addAttribute(
                "paidAmount",
                supplierPaymentService.getTotalPaid(id)
        );
        model.addAttribute(
                "remainingAmount",
                supplierPaymentService.getRemainingAmount(id)
        );

        return "deliveries/delivery-details";
    }
}