package com.opticsappbg.opticsapp.product.service;


import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product findOrThrow(CreateOrderItemRequest req) {

        if (req.getBarcode() != null) {
            return productRepository.findByBarcode(req.getBarcode())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
        }

        return productRepository
                .findByBrandAndModelAndCategory(
                        req.getBrand(),
                        req.getModel(),
                        req.getCategory()
                )
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
