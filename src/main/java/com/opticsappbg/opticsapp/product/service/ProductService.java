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

        String barcode = req.getBarcode() != null
                ? req.getBarcode().trim()
                : null;

        if (barcode != null && barcode.isEmpty()) {
            barcode = null;
        }

// Ако има избран Product, първо проверяваме дали
// характеристиките все още съответстват на него.
        if (req.getProductId() != null) {

            Product selectedProduct = productRepository.findById(req.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Избраният продукт не съществува."
                            )
                    );

            boolean sameCharacteristics =
                    selectedProduct.getCategory() == req.getCategory()
                            && selectedProduct.getBrand().equals(req.getBrand())
                            && selectedProduct.getModel().equals(req.getModel())
                            && java.util.Objects.equals(
                            selectedProduct.getSizeOrDiameter(),
                            req.getSizeOrDiameter()
                    )
                            && selectedProduct.getSph().compareTo(req.getSph()) == 0
                            && selectedProduct.getCyl().compareTo(req.getCyl()) == 0
                            && selectedProduct.getAddPower().compareTo(req.getAddPower()) == 0
                            && selectedProduct.getPrism().compareTo(req.getPrism()) == 0;

            if (sameCharacteristics) {

                // Характеристиките са същите.
                // Ако и баркодът е същият, използваме избрания Product.
                if (java.util.Objects.equals(
                        selectedProduct.getBarcode(),
                        barcode
                )) {
                    return selectedProduct;
                }

                // Характеристиките са същите, но е въведен друг баркод.
                // Старият Product не се променя.
                // Продължаваме надолу, за да обработим новия баркод.
            }

// Характеристиките са променени.
// Ако баркодът все още е старият баркод на продукта,
// той вече не трябва да се използва.
            if (java.util.Objects.equals(
                    selectedProduct.getBarcode(),
                    barcode
            )) {
                barcode = null;
            }
        }

        // 1. Ако е въведен баркод, той трябва да е уникален.
        if (barcode != null) {

            Optional<Product> productWithBarcode =
                    productRepository.findByBarcode(barcode);

            if (productWithBarcode.isPresent()) {

                Optional<Product> exactProduct =
                        productRepository.findExactProduct(
                                req.getCategory(),
                                req.getBrand(),
                                req.getModel(),
                                barcode,
                                req.getSizeOrDiameter(),
                                req.getSph(),
                                req.getCyl(),
                                req.getAddPower(),
                                req.getPrism()
                        );

                if (exactProduct.isPresent()) {
                    return exactProduct.get();
                }

                throw new IllegalArgumentException(
                        "Баркодът " + barcode +
                                " вече се използва за друг продукт."
                );
            }

            /*
             * Баркодът е нов.
             * Създаваме нов Product с този баркод.
             *
             * Възможно е характеристиките да са същите като на
             * друг продукт, но това е допустимо, ако баркодът е различен.
             */
            Product newProduct = new Product();

            newProduct.setCategory(req.getCategory());
            newProduct.setBarcode(barcode);
            newProduct.setBrand(req.getBrand());
            newProduct.setModel(req.getModel());
            newProduct.setSizeOrDiameter(req.getSizeOrDiameter());

            newProduct.setSph(req.getSph());
            newProduct.setCyl(req.getCyl());
            newProduct.setAddPower(req.getAddPower());
            newProduct.setPrism(req.getPrism());

            newProduct.setCreatedOn(java.time.LocalDateTime.now());
            newProduct.setPurchasePrice(BigDecimal.ZERO);
            newProduct.setQuantity(0);
            newProduct.setRetailPrice(req.getSalePriceAtSale());

            return productRepository.save(newProduct);
        }

        // 2. Няма баркод.
        // Търсим по характеристиките.
        List<Product> products =
                productRepository.findProductsByCharacteristics(
                        req.getCategory(),
                        req.getBrand(),
                        req.getModel(),
                        req.getSizeOrDiameter(),
                        req.getSph(),
                        req.getCyl(),
                        req.getAddPower(),
                        req.getPrism()
                );

        // Няма съществуващ продукт -> създаваме нов без баркод.
        if (products.isEmpty()) {
            Product newProduct = new Product();

            newProduct.setCategory(req.getCategory());
            newProduct.setBarcode(null);
            newProduct.setBrand(req.getBrand());
            newProduct.setModel(req.getModel());
            newProduct.setSizeOrDiameter(req.getSizeOrDiameter());

            newProduct.setSph(req.getSph());
            newProduct.setCyl(req.getCyl());
            newProduct.setAddPower(req.getAddPower());
            newProduct.setPrism(req.getPrism());

            newProduct.setCreatedOn(java.time.LocalDateTime.now());
            newProduct.setPurchasePrice(BigDecimal.ZERO);
            newProduct.setQuantity(0);
            newProduct.setRetailPrice(req.getSalePriceAtSale());

            return productRepository.save(newProduct);
        }

        // Има точно един продукт -> използваме него.
        if (products.size() == 1) {
            return products.get(0);
        }

        // Има няколко продукта с еднакви характеристики.
        // Без productId няма как да изберем правилния.
        throw new IllegalArgumentException(
                "Намерени са няколко продукта с тези характеристики. " +
                        "Моля, изберете продукт чрез баркода."
        );
    }

    public List<Product> findProductsByCharacteristics(CreateOrderItemRequest req) {

        return productRepository.findProductsByCharacteristics(
                req.getCategory(),
                req.getBrand(),
                req.getModel(),
                req.getSizeOrDiameter(),
                req.getSph(),
                req.getCyl(),
                req.getAddPower(),
                req.getPrism()
        );
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<String[]> getBrandModelPairs() {
        return productRepository.findAll().stream()
                .map(p -> new String[]{p.getBrand(), p.getModel()})
                .toList();
    }

    public List<String> getUniqueBrandNames() {
        return productRepository.findDistinctBrands();
    }

    public Product getByBarcode(String barcode) {
        String normalizedBarcode = barcode != null
                ? barcode.trim()
                : null;

        if (normalizedBarcode == null || normalizedBarcode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Баркодът не може да бъде празен."
            );
        }
        return productRepository.findByBarcode(normalizedBarcode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Продукт с баркод " + normalizedBarcode +
                                        " не е намерен."
                        )
                );
    }


    @Transactional
    public void adjustQuantity(Long productId, int quantityChange) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Продуктът не е намерен.")
                );

        int currentQuantity = product.getQuantity() != null
                ? product.getQuantity()
                : 0;

        product.setQuantity(currentQuantity + quantityChange);

        productRepository.save(product);
    }

}
