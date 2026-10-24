package com.opticsappbg.opticsapp.product.service;


import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.repository.OrderItemRepository;
import com.opticsappbg.opticsapp.order.service.OrderPricingService;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.opticsappbg.opticsapp.order.model.OrderItem;

import java.util.*;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPricingService orderPricingService;

    @Transactional
    public Product findOrCreate(CreateOrderItemRequest req) {

        String barcode = req.getBarcode() != null
                ? req.getBarcode().trim()
                : null;

        if (barcode != null && barcode.isEmpty()) {
            barcode = null;
        }

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
                if (java.util.Objects.equals(
                        selectedProduct.getBarcode(),
                        barcode
                )) {
                    return selectedProduct;
                }
            }

            if (java.util.Objects.equals(
                    selectedProduct.getBarcode(),
                    barcode
            )) {
                barcode = null;
            }
        }

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

            List<Product> matchingProducts =
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

            if (!matchingProducts.isEmpty()) {
                throw new IllegalArgumentException(
                        describeDuplicateProducts(matchingProducts));
            }

            return createProductFromRequest(req, barcode);
        }

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

        if (products.isEmpty()) {
            return createProductFromRequest(req, null);
        }

        if (products.size() == 1) {
            return products.get(0);
        }

        throw new IllegalArgumentException(
                "Намерени са няколко продукта с тези характеристики. " +
                        "Моля, изберете продукт чрез баркода."
        );
    }

    private Product createProductFromRequest(
            CreateOrderItemRequest req,
            String barcode) {

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
        newProduct.setPurchasePrice(null);
        newProduct.setQuantity(0);
        newProduct.setRetailPrice(req.getSalePriceAtSale());

        return productRepository.save(newProduct);
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

    @Transactional
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Продуктът не е намерен.")
                );
    }

    @Transactional
    public Product createProduct(Product product) {
        product.setId(null);

        // Проверка дали изобщо са въведени данни за продукта
        if (product.getCategory() == null
                && isBlank(product.getBrand())
                && isBlank(product.getModel())
                && isBlank(product.getBarcode())
                && isBlank(product.getSizeOrDiameter())
                && product.getRetailPrice() == null) {

            throw new IllegalArgumentException(
                    "Не са въведени данни за продукта. Моля, попълнете формата."
            );
        }

        // Задължителни данни за идентифициране на продукта
        if (product.getCategory() == null) {
            throw new IllegalArgumentException(
                    "Моля, изберете категория на продукта."
            );
        }

        if (isBlank(product.getBrand())) {
            throw new IllegalArgumentException(
                    "Моля, въведете бранд на продукта."
            );
        }

        if (isBlank(product.getModel())) {
            throw new IllegalArgumentException(
                    "Моля, въведете модел на продукта."
            );
        }

        if (product.getRetailPrice() == null) {
            throw new IllegalArgumentException(
                    "Моля, въведете продажна цена на продукта."
            );
        }

        if (product.getRetailPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Продажната цена не може да бъде отрицателна."
            );
        }

        validateProductIdentity(product, null);

        product.setQuantity(0);

        if (product.getCreatedOn() == null) {
            product.setCreatedOn(java.time.LocalDateTime.now());
        }

        return productRepository.save(product);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public Product updateProduct(Long productId, Product formProduct) {
        Product existingProduct = getProductById(productId);

        validateProductIdentity(formProduct, productId);

        existingProduct.setCategory(formProduct.getCategory());
        existingProduct.setBarcode(formProduct.getBarcode());
        existingProduct.setBrand(formProduct.getBrand());
        existingProduct.setModel(formProduct.getModel());
        existingProduct.setSizeOrDiameter(formProduct.getSizeOrDiameter());
        existingProduct.setSph(formProduct.getSph());
        existingProduct.setCyl(formProduct.getCyl());
        existingProduct.setAddPower(formProduct.getAddPower());
        existingProduct.setPrism(formProduct.getPrism());
        existingProduct.setRetailPrice(formProduct.getRetailPrice());
        existingProduct.setNote(formProduct.getNote());

        if (formProduct.getPurchasePrice() != null) {
            updatePurchasePrice(productId, formProduct.getPurchasePrice());
        }

        return productRepository.save(existingProduct);
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

    @Transactional
    public void updatePurchasePrice(Long productId, BigDecimal purchasePrice) {

        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Доставната цена трябва да бъде нула или положителна стойност."
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Продуктът не е намерен.")
                );

        BigDecimal previousPurchasePrice = product.getPurchasePrice();

        product.setPurchasePrice(purchasePrice);
        productRepository.save(product);

        if (previousPurchasePrice != null) {
            return;
        }

        List<OrderItem> items =
                orderItemRepository.findByProductIdAndPurchasePriceAtSaleIsNull(
                        productId
                );

        Map<Long, Order> ordersToRecalculate = new LinkedHashMap<>();

        for (OrderItem item : items) {
            item.setPurchasePriceAtSale(purchasePrice);

            Order order = item.getOrder();
            ordersToRecalculate.putIfAbsent(order.getId(), order);
        }

        for (Order order : ordersToRecalculate.values()) {
            orderPricingService.recalculateProfits(order);
        }
    }

    private void validateProductIdentity(Product product, Long excludedProductId) {

        String barcode = product.getBarcode();

        if (barcode != null) {
            barcode = barcode.trim();

            if (barcode.isEmpty()) {
                barcode = null;
            }
        }

        product.setBarcode(barcode);

        if (barcode != null) {
            List<Product> productsWithBarcode =
                    productRepository.findAllByBarcode(barcode);

            boolean barcodeUsedByAnotherProduct =
                    productsWithBarcode.stream()
                            .anyMatch(existing ->
                                    !Objects.equals(
                                            existing.getId(),
                                            excludedProductId
                                    )
                            );

            if (barcodeUsedByAnotherProduct) {
                throw new IllegalArgumentException(
                        "Баркодът " + barcode
                                + " вече се използва за друг продукт."
                );
            }
        }

        List<Product> matchingProducts =
                productRepository.findProductsByCharacteristics(
                        product.getCategory(),
                        product.getBrand(),
                        product.getModel(),
                        product.getSizeOrDiameter(),
                        product.getSph(),
                        product.getCyl(),
                        product.getAddPower(),
                        product.getPrism()
                );

        List<Product> duplicates = matchingProducts.stream()
                .filter(existing ->
                        !Objects.equals(existing.getId(), excludedProductId))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                    describeDuplicateProducts(duplicates)
            );
        }
    }

    private String describeDuplicateProducts(List<Product> products) {
        if (products.size() == 1) {
            Product existing = products.get(0);
            String existingBarcode = existing.getBarcode();

            if (existingBarcode != null && !existingBarcode.isBlank()) {
                return "Вече има продукт със същите характеристики и баркод "
                        + existingBarcode
                        + ". Използвайте съществуващия продукт.";
            }

            return "Вече има продукт със същите характеристики, но без баркод. "
                    + "Използвайте съществуващия продукт.";
        }

        String barcodes = products.stream()
                .map(Product::getBarcode)
                .filter(barcode -> barcode != null && !barcode.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));

        if (barcodes.isBlank()) {
            return "Намерени са няколко продукта със същите характеристики. "
                    + "Моля, проверете съществуващите продукти.";
        }

        return "Намерени са няколко продукта със същите характеристики. "
                + "Съществуващите баркодове са: " + barcodes
                + ". Моля, проверете кой продукт трябва да използвате.";
    }

}
