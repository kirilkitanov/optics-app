
package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.service.DeliveryService;
import com.opticsappbg.opticsapp.supplier.model.Supplier;
import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import com.opticsappbg.opticsapp.supplier.service.SupplierPaymentService;
import com.opticsappbg.opticsapp.supplier.service.SupplierService;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.SupplierFormRequest;
import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final DeliveryService deliveryService;
    private final SupplierPaymentService supplierPaymentService;
    private final UserService userService;

    @GetMapping
    public String showSuppliers(Model model) {
        model.addAttribute(
                "suppliers",
                supplierService.getAllSupplierSummaries());
        return "suppliers/suppliers";
    }

    @GetMapping("/{id}")
    public String showSupplierDetails(
            @PathVariable Long id,
            Model model) {

        Supplier supplier = supplierService.getSupplierById(id);

        var deliveries = deliveryService.getDeliveriesBySupplierId(id);

        Map<Long, BigDecimal> paidAmounts = new HashMap<>();
        Map<Long, BigDecimal> remainingAmounts = new HashMap<>();
        Map<Long, List<SupplierPayment>> paymentsByDelivery = new HashMap<>();

        for (Delivery delivery : deliveries) {
            BigDecimal paid =
                    supplierPaymentService.getTotalPaid(delivery.getId());

            BigDecimal remaining =
                    delivery.getTotalAmount().subtract(paid);

            paidAmounts.put(delivery.getId(), paid);
            remainingAmounts.put(delivery.getId(), remaining);

            paymentsByDelivery.put(
                    delivery.getId(),
                    supplierPaymentService.getPaymentsForDelivery(delivery.getId())
            );
        }

        model.addAttribute("supplier", supplier);
        model.addAttribute("deliveries", deliveries);
        model.addAttribute("paidAmounts", paidAmounts);
        model.addAttribute("remainingAmounts", remainingAmounts);
        model.addAttribute("paymentsByDelivery", paymentsByDelivery);

        return "suppliers/supplier-details";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("supplierForm", new SupplierFormRequest());
        model.addAttribute("isEdit", false);
        return "suppliers/supplier-form";
    }

    @PostMapping
    public String createSupplier(
            @ModelAttribute("supplierForm") SupplierFormRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.createSupplier(request);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Доставчикът е добавен успешно.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("supplierForm", request);
            return "redirect:/suppliers/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getSupplierById(id);

        SupplierFormRequest form = new SupplierFormRequest();
        form.setCompanyName(supplier.getCompanyName());
        form.setEik(supplier.getEik());
        form.setVat(supplier.getVat());
        form.setMol(supplier.getMol());
        form.setPhone(supplier.getPhone());
        form.setEmail(supplier.getEmail());
        form.setAddress(supplier.getAddress());
        form.setNote(supplier.getNote());

        model.addAttribute("supplierForm", form);
        model.addAttribute("supplierId", id);
        model.addAttribute("isEdit", true);

        return "suppliers/supplier-form";
    }

    @PostMapping("/{id}")
    public String updateSupplier(
            @PathVariable Long id,
            @ModelAttribute("supplierForm") SupplierFormRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            supplierService.updateSupplier(id, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Доставчикът е редактиран успешно.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("supplierForm", request);
            return "redirect:/suppliers/" + id + "/edit";
        }
    }

    @PostMapping("/{supplierId}/deliveries/{deliveryId}/payments")
    public String addPayment(
            @PathVariable Long supplierId,
            @PathVariable Long deliveryId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.getCurrentUser(authentication);

            supplierPaymentService.addPayment(
                    deliveryId, amount, paymentMethod, note, currentUser);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Плащането е записано успешно.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/suppliers/" + supplierId;
    }

    @PostMapping("/{supplierId}/deliveries/{deliveryId}/refunds")
    public String addRefund(
            @PathVariable Long supplierId,
            @PathVariable Long deliveryId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.getCurrentUser(authentication);

            supplierPaymentService.addRefund(
                    deliveryId, amount, paymentMethod, note, currentUser);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Върнатата сума е записана успешно.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/suppliers/" + supplierId;
    }


    @PostMapping("/{supplierId}/deliveries/{deliveryId}/payments/{paymentId}/edit")
    public String editPayment(
            @PathVariable Long supplierId,
            @PathVariable Long deliveryId,
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        try {
            supplierPaymentService.editPayment(
                    deliveryId, paymentId, amount, paymentMethod, note);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Плащането е редактирано успешно.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/suppliers/" + supplierId;
    }

    @PostMapping("/{supplierId}/deliveries/{deliveryId}/payments/{paymentId}/delete")
    public String deletePayment(
            @PathVariable Long supplierId,
            @PathVariable Long deliveryId,
            @PathVariable Long paymentId,
            RedirectAttributes redirectAttributes) {

        try {
            supplierPaymentService.deletePayment(deliveryId, paymentId);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Плащането е изтрито успешно.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/suppliers/" + supplierId;
    }

}
