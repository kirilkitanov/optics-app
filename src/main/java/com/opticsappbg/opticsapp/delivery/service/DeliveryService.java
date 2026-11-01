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
        delivery.setInvoiceDate(request.getInvoiceDate());
        delivery.setSupplier(supplier);
        delivery.setCreatedOn(now);
        delivery.setCreatedBy(user);

        BigDecimal totalAmount = BigDecimal.ZERO;

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

            // Намираме или създаваме продукта.
            Product product = productService.findOrCreateFromDelivery(itemRequest);

            // Създаваме реда в доставката.
            DeliveryItem deliveryItem = new DeliveryItem();

            deliveryItem.setDelivery(delivery);
            deliveryItem.setProduct(product);
            deliveryItem.setQuantity(itemRequest.getQuantity());
            deliveryItem.setPurchasePriceAtDelivery(purchasePrice);
            deliveryItem.setCreatedOn(now);

            delivery.getItems().add(deliveryItem);

            // Увеличаваме наличността.
            productService.adjustQuantity(
                    product.getId(),
                    itemRequest.getQuantity()
            );

            // Актуализираме текущата доставна цена.
            productService.updatePurchasePrice(
                    product.getId(),
                    purchasePrice
            );

            // Добавяме стойността на реда към общата сума.
            BigDecimal itemTotal = purchasePrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            totalAmount = totalAmount.add(itemTotal);
        }

        delivery.setTotalAmount(
                totalAmount.setScale(2, RoundingMode.HALF_UP)
        );

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
}