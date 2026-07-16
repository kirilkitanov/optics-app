package com.opticsappbg.opticsapp.product.service;


import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product findOrCreate(CreateOrderItemRequest req) {

        Optional<Product> productOpt = Optional.empty();

        // 1. Първо пробваме да намерим продукта по Баркод (ако е сканиран/въведен)
        if (req.getBarcode() != null && !req.getBarcode().trim().isEmpty()) {
            productOpt = productRepository.findByBarcode(req.getBarcode().trim());
        }
        // 2. Ако няма баркод, пробваме да го намерим по Бранд, Модел и Категория
        else {
            productOpt = productRepository.findByBrandAndModelAndCategory(
                    req.getBrand(),
                    req.getModel(),
                    req.getCategory()
            );
        }
        // Ако продуктът съществува в базата, директно го връщаме
        if (productOpt.isPresent()) {
            return productOpt.get();
        }
        // 3. Ако продуктът НЕ съществува, го създаваме "в движение"
        Product newProduct = new Product();
        newProduct.setCategory(req.getCategory());
        newProduct.setBarcode(req.getBarcode() != null ? req.getBarcode().trim() : null);
        newProduct.setBrand(req.getBrand());
        newProduct.setModel(req.getModel());
        newProduct.setSizeOrDiameter(req.getSizeOrDiameter());
        newProduct.setCreatedOn(java.time.LocalDateTime.now());
        newProduct.setSph(req.getSph());
        newProduct.setCyl(req.getCyl());
        newProduct.setAddPower(req.getAddPower());
        newProduct.setPrism(req.getPrism());
        newProduct.setPurchasePrice(BigDecimal.ZERO);
        newProduct.setQuantity(0);
        newProduct.setRetailPrice(req.getPriceAtPurchase());

        return productRepository.save(newProduct);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
