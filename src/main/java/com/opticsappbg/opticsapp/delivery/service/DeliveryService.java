package com.opticsappbg.opticsapp.delivery.service;

import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.model.DeliveryItem;
import com.opticsappbg.opticsapp.delivery.repository.DeliveryRepository;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.supplier.model.Supplier;
import com.opticsappbg.opticsapp.supplier.repository.SupplierRepository;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.web.dto.CreateDeliveryRequest;
import com.opticsappbg.opticsapp.web.dto.CreateDeliveryItemRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductService productService;

    @Transactional
    public Delivery createDelivery(CreateDeliveryRequest request, User user) {

        // Проверка на основните данни.
        if (request.getInvoiceNumber() == null
                || request.getInvoiceNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Моля, въведете номер на фактурата."
            );
        }

        if (request.getInvoiceDate() == null) {
            throw new IllegalArgumentException(
                    "Моля, въведете дата на фактурата."
            );
        }

        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException(
                    "Моля, изберете доставчик."
            );
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Доставката трябва да съдържа поне един продукт."
            );
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Доставчикът не е намерен.")
                );

        LocalDateTime now = LocalDateTime.now();

        Delivery delivery = new Delivery();

        delivery.setInvoiceNumber(request.getInvoiceNumber().trim());
        delivery.setInvoiceDate(request.getInvoiceDate().atStartOfDay());
        delivery.setSupplier(supplier);
        delivery.setCreatedOn(now);
        delivery.setCreatedBy(user);
        delivery.setNote(request.getNote());

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        // Първо проверяваме редовете и изчисляваме сумата преди отстъпката.
        for (CreateDeliveryItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Количеството трябва да бъде по-голямо от нула."
                );
            }

            BigDecimal purchasePrice = itemRequest.getPurchasePriceAtDelivery();

            if (purchasePrice == null
                    || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Доставната цена трябва да бъде нула или положителна."
                );
            }

            BigDecimal itemTotal = purchasePrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            totalBeforeDiscount = totalBeforeDiscount.add(itemTotal);
        }

        totalBeforeDiscount = totalBeforeDiscount.setScale(2, RoundingMode.HALF_UP);

        // Определяме крайната сума и процента отстъпка.
        BigDecimal discountPercentage = request.getDiscountPercentage() != null
                ? request.getDiscountPercentage()
                : BigDecimal.ZERO;

        BigDecimal totalAmount;

        if (request.getTotalAmount() != null) {
            totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Крайната сума не може да бъде отрицателна."
                );
            }

            if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discountPercentage = totalBeforeDiscount
                        .subtract(totalAmount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);
            } else if (totalAmount.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(
                        "Крайната сума не може да бъде по-голяма от нула."
                );
            }

        } else {

            totalAmount = totalBeforeDiscount
                    .multiply(
                            BigDecimal.ONE.subtract(
                                    discountPercentage.divide(
                                            BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP
                                    )
                            )
                    )
                    .setScale(2, RoundingMode.HALF_UP);
        }

        delivery.setTotalBeforeDiscount(totalBeforeDiscount);
        delivery.setDiscountPercentage(discountPercentage);
        delivery.setTotalAmount(totalAmount);

        // Разпределяме отстъпката между редовете.
        BigDecimal allocatedTotal = BigDecimal.ZERO;

        for (int i = 0; i < request.getItems().size(); i++) {

            CreateDeliveryItemRequest itemRequest = request.getItems().get(i);

            BigDecimal originalLineTotal = itemRequest.getPurchasePriceAtDelivery()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            BigDecimal discountedLineTotal;

            if (i == request.getItems().size() - 1) {
                // Последният ред поема разликата от закръгляването.
                discountedLineTotal = totalAmount.subtract(allocatedTotal);
            } else if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discountedLineTotal = originalLineTotal
                        .multiply(totalAmount)
                        .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);

                allocatedTotal = allocatedTotal.add(discountedLineTotal);
            } else {
                discountedLineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal actualPurchasePrice = discountedLineTotal
                    .divide(
                            BigDecimal.valueOf(itemRequest.getQuantity()),
                            2,
                            RoundingMode.HALF_UP
                    );

            Product product = productService.findOrCreateFromDelivery(itemRequest);

            DeliveryItem deliveryItem = new DeliveryItem();
            deliveryItem.setDelivery(delivery);
            deliveryItem.setProduct(product);
            deliveryItem.setQuantity(itemRequest.getQuantity());
            deliveryItem.setPurchasePriceAtDelivery(actualPurchasePrice);
            deliveryItem.setCreatedOn(now);

            delivery.getItems().add(deliveryItem);

            productService.adjustQuantity(
                    product.getId(),
                    itemRequest.getQuantity()
            );

            productService.updatePurchasePrice(
                    product.getId(),
                    actualPurchasePrice
            );
        }

        return deliveryRepository.save(delivery);
    }


    public Page<Delivery> searchDeliveries(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        if (search != null) {
            search = search.trim();

            if (search.isEmpty()) {
                search = null;
            }
        }

        LocalDateTime start = startDate != null
                ? startDate.atStartOfDay()
                : null;

        LocalDateTime end = endDate != null
                ? endDate.plusDays(1).atStartOfDay()
                : null;

        return deliveryRepository.searchDeliveries(
                search,
                start,
                end,
                pageable
        );
    }

    @Transactional
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Фактурата не е намерена."));
    }

    @Transactional
    public List<Delivery> getDeliveriesBySupplierId(Long supplierId) {
        return deliveryRepository.findBySupplier_Id(supplierId)
                .stream()
                .sorted((a, b) -> b.getInvoiceDate().compareTo(a.getInvoiceDate()))
                .toList();
    }

    @Transactional
    public CreateDeliveryRequest getDeliveryForEdit(Long id) {

        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Фактурата не е намерена.")
                );

        CreateDeliveryRequest request = new CreateDeliveryRequest();

        request.setInvoiceNumber(delivery.getInvoiceNumber());
        request.setInvoiceDate(delivery.getInvoiceDate().toLocalDate());
        request.setSupplierId(delivery.getSupplier().getId());
        request.setNote(delivery.getNote());
        request.setDiscountPercentage(delivery.getDiscountPercentage());
        request.setTotalAmount(delivery.getTotalAmount());

        List<CreateDeliveryItemRequest> items = delivery.getItems().stream()
                .map(deliveryItem -> {
                    Product product = deliveryItem.getProduct();

                    CreateDeliveryItemRequest item = new CreateDeliveryItemRequest();

                    item.setProductId(product.getId());
                    item.setCategory(product.getCategory());
                    item.setBrand(product.getBrand());
                    item.setModel(product.getModel());
                    item.setBarcode(product.getBarcode());
                    item.setSizeOrDiameter(product.getSizeOrDiameter());
                    item.setSph(product.getSph());
                    item.setCyl(product.getCyl());
                    item.setAddPower(product.getAddPower());
                    item.setPrism(product.getPrism());

                    item.setQuantity(deliveryItem.getQuantity());

                    // Взимаме историческата цена от фактурата.
                    item.setPurchasePriceAtDelivery(
                            deliveryItem.getPurchasePriceAtDelivery()
                    );

                    return item;
                })
                .toList();

        request.setItems(items);

        return request;
    }

    @Transactional
    public void updateDelivery(Long id, CreateDeliveryRequest request) {

        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Фактурата не е намерена.")
                );

        if (request.getInvoiceNumber() == null
                || request.getInvoiceNumber().isBlank()) {
            throw new IllegalArgumentException("Моля, въведете номер на фактурата.");
        }

        if (request.getInvoiceDate() == null) {
            throw new IllegalArgumentException("Моля, въведете дата на фактурата.");
        }

        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("Моля, изберете доставчик.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Доставката трябва да съдържа поне един продукт."
            );
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Доставчикът не е намерен.")
                );

        // Запазваме продуктите, чиито количества или цени може да се променят.
        java.util.Set<Long> affectedProductIds = new java.util.LinkedHashSet<>();

        // Връщаме старите количества в склада.
        for (DeliveryItem oldItem : delivery.getItems()) {
            Product product = oldItem.getProduct();

            affectedProductIds.add(product.getId());

            productService.adjustQuantity(
                    product.getId(),
                    -oldItem.getQuantity()
            );
        }

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        // Проверяваме новите редове и изчисляваме сумата преди отстъпката.
        for (CreateDeliveryItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Количеството трябва да бъде по-голямо от нула."
                );
            }

            BigDecimal purchasePrice = itemRequest.getPurchasePriceAtDelivery();

            if (purchasePrice == null
                    || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Доставната цена трябва да бъде нула или положителна."
                );
            }

            totalBeforeDiscount = totalBeforeDiscount.add(
                    purchasePrice.multiply(
                            BigDecimal.valueOf(itemRequest.getQuantity())
                    )
            );
        }

        totalBeforeDiscount = totalBeforeDiscount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountPercentage = request.getDiscountPercentage() != null
                ? request.getDiscountPercentage()
                : BigDecimal.ZERO;

        BigDecimal totalAmount;

        if (request.getTotalAmount() != null) {

            totalAmount = request.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP);

            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Крайната сума не може да бъде отрицателна."
                );
            }

            if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discountPercentage = totalBeforeDiscount
                        .subtract(totalAmount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);

            } else if (totalAmount.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(
                        "Крайната сума не може да бъде по-голяма от нула."
                );
            }

        } else {

            totalAmount = totalBeforeDiscount
                    .multiply(
                            BigDecimal.ONE.subtract(
                                    discountPercentage.divide(
                                            BigDecimal.valueOf(100),
                                            6,
                                            RoundingMode.HALF_UP
                                    )
                            )
                    )
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Обновяваме самата доставка, без да променяме нейното ID.
        delivery.setInvoiceNumber(request.getInvoiceNumber().trim());
        delivery.setInvoiceDate(request.getInvoiceDate().atStartOfDay());
        delivery.setSupplier(supplier);
        delivery.setNote(request.getNote());
        delivery.setTotalBeforeDiscount(totalBeforeDiscount);
        delivery.setDiscountPercentage(discountPercentage);
        delivery.setTotalAmount(totalAmount);
        delivery.setUpdatedOn(LocalDateTime.now());

        // Премахваме старите редове.
        delivery.getItems().clear();

        // Добавяме новите редове и количествата.
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < request.getItems().size(); i++) {

            CreateDeliveryItemRequest itemRequest = request.getItems().get(i);

            BigDecimal originalLineTotal = itemRequest.getPurchasePriceAtDelivery()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            BigDecimal discountedLineTotal;

            if (i == request.getItems().size() - 1) {

                discountedLineTotal = totalAmount.subtract(allocatedTotal);

            } else if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {

                discountedLineTotal = originalLineTotal
                        .multiply(totalAmount)
                        .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);

                allocatedTotal = allocatedTotal.add(discountedLineTotal);

            } else {

                discountedLineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal actualPurchasePrice = discountedLineTotal
                    .divide(
                            BigDecimal.valueOf(itemRequest.getQuantity()),
                            2,
                            RoundingMode.HALF_UP
                    );

            Product product = productService.findOrCreateFromDelivery(itemRequest);

            affectedProductIds.add(product.getId());

            DeliveryItem deliveryItem = new DeliveryItem();
            deliveryItem.setDelivery(delivery);
            deliveryItem.setProduct(product);
            deliveryItem.setQuantity(itemRequest.getQuantity());
            deliveryItem.setPurchasePriceAtDelivery(actualPurchasePrice);
            deliveryItem.setCreatedOn(now);

            delivery.getItems().add(deliveryItem);

            productService.adjustQuantity(
                    product.getId(),
                    itemRequest.getQuantity()
            );
        }

        deliveryRepository.save(delivery);

        // Обновяваме текущата доставна цена според последната фактура
        // за всеки засегнат продукт.
        for (Long productId : affectedProductIds) {

            deliveryRepository
                    .findFirstByItems_Product_IdOrderByInvoiceDateDescIdDesc(productId)
                    .ifPresent(latestDelivery -> {

                        DeliveryItem latestItem = latestDelivery.getItems()
                                .stream()
                                .filter(item ->
                                        item.getProduct().getId().equals(productId)
                                )
                                .reduce((first, second) -> second)
                                .orElseThrow();

                        productService.updatePurchasePrice(
                                productId,
                                latestItem.getPurchasePriceAtDelivery()
                        );
                    });
        }
    }
}