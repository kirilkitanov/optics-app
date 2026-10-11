package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import com.opticsappbg.opticsapp.web.dto.ProductLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor

public class ProductController {

    private final ProductService productService;

    @GetMapping("/lookup")
    public List<ProductLookupResponse> productLookup(
            @RequestParam ProductCategory category,
            @RequestParam String brand,
            @RequestParam String model,
            @RequestParam(required = false) String sizeOrDiameter,
            @RequestParam BigDecimal sph,
            @RequestParam BigDecimal cyl,
            @RequestParam BigDecimal addPower,
            @RequestParam BigDecimal prism) {

        CreateOrderItemRequest request = new CreateOrderItemRequest();

        request.setCategory(category);
        request.setBrand(brand);
        request.setModel(model);
        request.setSizeOrDiameter(sizeOrDiameter);
        request.setSph(sph);
        request.setCyl(cyl);
        request.setAddPower(addPower);
        request.setPrism(prism);

        return productService.findProductsByCharacteristics(request)
                .stream()
                .map(product -> new ProductLookupResponse(
                        product.getId(),
                        product.getBarcode(),
                        product.getCategory(),
                        product.getBrand(),
                        product.getModel(),
                        product.getSizeOrDiameter(),
                        product.getSph(),
                        product.getCyl(),
                        product.getAddPower(),
                        product.getPrism(),
                        product.getRetailPrice()
                ))
                .toList();
    }


    @GetMapping("/lookup-by-barcode")
    public ResponseEntity<?> lookupByBarcode(
            @RequestParam String barcode) {

        try {
            Product product = productService.getByBarcode(barcode);

            return ResponseEntity.ok(
                    mapToLookupResponse(product)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of(
                            "message", e.getMessage()
                    ));
        }
    }

    private ProductLookupResponse mapToLookupResponse(Product product) {
        return new ProductLookupResponse(
                product.getId(),
                product.getBarcode(),
                product.getCategory(),
                product.getBrand(),
                product.getModel(),
                product.getSizeOrDiameter(),
                product.getSph(),
                product.getCyl(),
                product.getAddPower(),
                product.getPrism(),
                product.getRetailPrice()
        );
    }

}


