package com.opticsappbg.opticsapp.product.service;

import com.opticsappbg.opticsapp.web.dto.StockAdjustmentRequest;
import com.opticsappbg.opticsapp.web.dto.StockAdjustmentResponse;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.StockAdjustment;
import com.opticsappbg.opticsapp.product.repository.StockAdjustmentRepository;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final ProductRepository productRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final UserRepository userRepository;


    @Transactional
    public StockAdjustmentResponse adjustStock(
            Long productId,
            StockAdjustmentRequest request,
            Long userId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new StockAdjustmentException(
                        "Продуктът не е намерен."));

        int previousQuantity = product.getQuantity() != null
                ? product.getQuantity()
                : 0;

        Integer requestedQuantity = request.newQuantity();

        int actualNewQuantity = requestedQuantity != null
                ? requestedQuantity
                : previousQuantity;

        int difference = actualNewQuantity - previousQuantity;

        if (difference == 0) {
            return new StockAdjustmentResponse(
                    false,
                    productId,
                    previousQuantity,
                    previousQuantity,
                    0,
                    "Няма промяна в наличността."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new StockAdjustmentException(
                        "Потребителят не е намерен."));

        StockAdjustment adjustment = StockAdjustment.builder()
                .product(product)
                .previousQuantity(previousQuantity)
                .newQuantity(actualNewQuantity)
                .difference(difference)
                .createdOn(LocalDateTime.now())
                .reason(request.reason() != null
                        && !request.reason().isBlank()
                        ? request.reason().trim()
                        : null)
                .performedBy(user)
                .build();

        product.setQuantity(actualNewQuantity);

        productRepository.save(product);
        stockAdjustmentRepository.save(adjustment);

        return new StockAdjustmentResponse(
                true,
                productId,
                previousQuantity,
                actualNewQuantity,
                difference,
                "Наличността е коригирана успешно."
        );
    }

}

