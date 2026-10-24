package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.product.service.ExcelProductExportService;
import com.opticsappbg.opticsapp.product.service.ExcelProductImportService;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.product.service.StockAdjustmentService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.web.dto.StockAdjustmentRequest;
import com.opticsappbg.opticsapp.web.dto.StockAdjustmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;

import java.io.IOException;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductPageController {

    private final ProductService productService;
    private final ExcelProductImportService excelProductImportService;
    private final ExcelProductExportService excelProductExportService;
    private final StockAdjustmentService stockAdjustmentService;

    @GetMapping
    public String productsPage(
            @RequestParam(required = false) Long selectedProductId,
            Model model) {

        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("selectedProductId", selectedProductId);
        model.addAttribute("activeSection", "products");

        return "products/products-list";
    }

    @GetMapping("/new")
    public String newProductPage(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeSection", "products");
        model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
        model.addAttribute("brands", productService.getUniqueBrandNames());

        return "products/product-form";
    }

    @PostMapping("/new")
    public String createProduct(
            @ModelAttribute("product") Product product,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            productService.createProduct(product);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Продуктът е създаден успешно.");

            return "redirect:/products";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeSection", "products");

            model.addAttribute("brandModelPairs",
                    productService.getBrandModelPairs());

            model.addAttribute("brands",
                    productService.getUniqueBrandNames());

            return "products/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editProductPage(
            @PathVariable Long id,
            Model model) {

        Product product = productService.getAllProducts()
                .stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Продуктът не е намерен."));

        model.addAttribute("product", product);
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("activeSection", "products");
        model.addAttribute("brandModelPairs", productService.getBrandModelPairs());
        model.addAttribute("brands", productService.getUniqueBrandNames());

        return "products/product-form";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.ModelAttribute("product")
            Product product,
            RedirectAttributes redirectAttributes) {

        try {
            productService.updateProduct(id, product);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Продуктът е редактиран успешно."
            );

            return "redirect:/products?selectedProductId=" + id;

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/products/" + id + "/edit";
        }
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("activeSection", "products");
        return "products/products-import";
    }

    @PostMapping("/import")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            ExcelProductImportService.ImportResult result =
                    excelProductImportService.importExcel(file);

            redirectAttributes.addFlashAttribute(
                    "importResult", result
            );

        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute(
                    "importError", ex.getMessage()
            );
        }

        return "redirect:/products/import";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws IOException {

        byte[] file = excelProductExportService.exportExcel();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"products.xlsx\""
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(file.length)
                .body(file);
    }

    @PostMapping("/{id}/stock-adjustments")
    @ResponseBody
    public StockAdjustmentResponse adjustStock(
            @PathVariable Long id,
            @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal AuthenticationDetails details) {

        return stockAdjustmentService.adjustStock(
                id,
                request,
                details.getUserId()
        );
    }

}

