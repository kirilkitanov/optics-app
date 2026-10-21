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

            // Баркодът е нов. Преди създаване проверяваме
            // дали вече има продукт със същите характеристики.
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
            newProduct.setPurchasePrice(null);
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

        validateProductIdentity(product, null);

        product.setQuantity(0);

        if (product.getCreatedOn() == null) {
            product.setCreatedOn(java.time.LocalDateTime.now());
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, Product formProduct) {
        Product existingProduct = getProductById(productId);

        // Проверяваме новите стойности, без да променяме текущия продукт
        // преди успешната валидация.
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

        // Запазваме досегашната логика за доставните цени
        // и историческите данни по поръчките.
        if (formProduct.getPurchasePrice() != null) {
            updatePurchasePrice(productId, formProduct.getPurchasePrice());
        }
        // Количеството не се променя при редакция.
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

        // Ако вече е имало доставна цена, запазваме старите
        // исторически цени в поръчките.
        if (previousPurchasePrice != null) {
            return;
        }

        // Първо въвеждане на доставна цена:
        // намираме всички исторически редове без записана цена.
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

        // Преизчисляваме печалбата само за засегнатите поръчки.
        // RecalculateProfits сам обработва анулираните поръчки,
        // като оставя печалбата им null.
        for (Order order : ordersToRecalculate.values()) {
            orderPricingService.recalculateProfits(order);
        }
    }

    private void validateProductIdentity(Product product, Long excludedProductId) {
        // Празният баркод се приема за липсващ.
        String barcode = product.getBarcode();

        if (barcode != null) {
            barcode = barcode.trim();
            if (barcode.isEmpty()) {
                barcode = null;
            }
        }

        product.setBarcode(barcode);

        // Баркодът трябва да е уникален.
        if (barcode != null) {
            Optional<Product> productWithBarcode =
                    productRepository.findByBarcode(barcode);

            if (productWithBarcode.isPresent()
                    && !Objects.equals(
                    productWithBarcode.get().getId(),
                    excludedProductId)) {

                throw new IllegalArgumentException(
                        "Баркодът " + barcode
                                + " вече се използва за друг продукт.");
            }
        }

        // Не допускаме втори продукт със същите характеристики,
        // независимо дали баркодът е различен или липсва.
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
                .filter(existing -> !Objects.equals(
                        existing.getId(), excludedProductId))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                    describeDuplicateProducts(duplicates));
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
