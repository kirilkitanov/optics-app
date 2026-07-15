package com.opticsappbg.opticsapp.order.service;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderItem;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderPricingService {
    public void calculate(Order order, CreateOrderRequest req) {

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {

            BigDecimal lineTotal = item.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal costTotal = item.getPurchasePriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal profit = lineTotal.subtract(costTotal);

            item.setProfitAtPurchase(profit);

            totalBeforeDiscount = totalBeforeDiscount.add(lineTotal);
        }

        order.setTotalBeforeDiscount(totalBeforeDiscount);

        // default discount
        BigDecimal discount = req.getDiscountPercentage() != null
                ? req.getDiscountPercentage()
                : BigDecimal.ZERO;

        // 🔥 MANUAL TOTAL OVERRIDE (ако служителят пише крайна сума)
        if (req.getManualTotal() != null) {

            order.setTotal(req.getManualTotal());

            order.setDiscountPercentage(
                    calculateDiscountFromManual(totalBeforeDiscount, req.getManualTotal())
            );

            return;
        }

        order.setDiscountPercentage(discount);
        order.setTotal(applyDiscount(totalBeforeDiscount, discount));
    }

    private BigDecimal applyDiscount(BigDecimal total, BigDecimal discountPercent) {

        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return total;
        }

        return total.subtract(
                total.multiply(discountPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateDiscountFromManual(BigDecimal original, BigDecimal manual) {

        if (manual == null || original == null || original.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (manual.compareTo(original) >= 0) {
            return BigDecimal.ZERO;
        }

        return original.subtract(manual)
                .multiply(BigDecimal.valueOf(100))
                .divide(original, 2, RoundingMode.HALF_UP);
    }
}
