package com.opticsappbg.opticsapp.order.service;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderItem;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderPricingService {

//        public void calculate(Order order, CreateOrderRequest req) {
//
//        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;
//
//        for (OrderItem item : order.getItems()) {
//
//            BigDecimal lineTotal = item.getSalePriceAtSale()
//                    .multiply(BigDecimal.valueOf(item.getQuantity()));
//
//            BigDecimal costTotal = item.getPurchasePriceAtSale() != null
//                    ? item.getPurchasePriceAtSale()
//                    .multiply(BigDecimal.valueOf(item.getQuantity()))
//                    : BigDecimal.ZERO;
//
//            if (item.getPurchasePriceAtSale() != null) {
//                BigDecimal profit = lineTotal.subtract(costTotal);
//                item.setProfitAtSale(profit);
//            } else {
//                item.setProfitAtSale(null);
//            }
//
//            totalBeforeDiscount = totalBeforeDiscount.add(lineTotal);
//        }
//
//        order.setTotalBeforeDiscount(totalBeforeDiscount);
//
//        // default discount
//        BigDecimal discount = req.getDiscountPercentage() != null
//                ? req.getDiscountPercentage()
//                : BigDecimal.ZERO;
//
//        if (req.getManualTotal() != null) {
//            order.setTotal(req.getManualTotal());
//            order.setDiscountPercentage(
//                    calculateDiscountFromManual(totalBeforeDiscount, req.getManualTotal())
//            );
//            return;
//        }
//
//        order.setDiscountPercentage(discount);
//        order.setTotal(applyDiscount(totalBeforeDiscount, discount));
//    }

    public void calculate(Order order, CreateOrderRequest req) {

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getSalePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalBeforeDiscount = totalBeforeDiscount.add(lineTotal);
        }

        order.setTotalBeforeDiscount(totalBeforeDiscount);

        BigDecimal discount = req.getDiscountPercentage() != null
                ? req.getDiscountPercentage()
                : BigDecimal.ZERO;

        if (req.getManualTotal() != null) {
            order.setTotal(req.getManualTotal());

            order.setDiscountPercentage(
                    calculateDiscountFromManual(
                            totalBeforeDiscount,
                            req.getManualTotal()
                    )
            );

            calculateProfitsAfterDiscount(
                    order,
                    totalBeforeDiscount,
                    req.getManualTotal(),
                    null
            );

            return;
        }

        order.setDiscountPercentage(discount);

        BigDecimal totalAfterDiscount = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getSalePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal lineDiscount = lineTotal
                    .multiply(discount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            totalAfterDiscount = totalAfterDiscount.add(
                    lineTotal.subtract(lineDiscount)
            );
        }

        order.setTotal(totalAfterDiscount);

        calculateProfitsAfterDiscount(
                order,
                totalBeforeDiscount,
                totalAfterDiscount,
                discount
        );
    }

    private void calculateProfitsAfterDiscount(
            Order order, BigDecimal totalBeforeDiscount, BigDecimal totalAfterDiscount, BigDecimal discountPercentage) {

        if (order.getStatus() == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                item.setProfitAtSale(null);
            }
            return;
        }

        BigDecimal allocatedTotal = BigDecimal.ZERO;

        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);

            BigDecimal lineTotal = item.getSalePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal discountedLineTotal;

            if (discountPercentage != null) {
                BigDecimal lineDiscount = lineTotal
                        .multiply(discountPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                discountedLineTotal = lineTotal.subtract(lineDiscount);
            } else if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {

                if (i == order.getItems().size() - 1) {
                    discountedLineTotal = totalAfterDiscount.subtract(allocatedTotal);
                } else {
                    discountedLineTotal = lineTotal
                            .multiply(totalAfterDiscount)
                            .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);

                    allocatedTotal = allocatedTotal.add(discountedLineTotal);
                }
            } else {
                discountedLineTotal = BigDecimal.ZERO;
            }

            if (item.getPurchasePriceAtSale() == null) {
                item.setProfitAtSale(null);
                continue;
            }

            BigDecimal costTotal = item.getPurchasePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal profit = discountedLineTotal.subtract(costTotal);

            item.setProfitAtSale(profit.setScale(2, RoundingMode.HALF_UP));
        }
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


    public void recalculateProfits(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                item.setProfitAtSale(null);
            }
            return;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getSalePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalBeforeDiscount = totalBeforeDiscount.add(lineTotal);
        }

        BigDecimal orderTotal = order.getTotal() != null
                ? order.getTotal()
                : totalBeforeDiscount;

        BigDecimal allocatedTotal = BigDecimal.ZERO;

        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);

            BigDecimal lineTotal = item.getSalePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal discountedLineTotal;

            if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                if (i == order.getItems().size() - 1) {
                    discountedLineTotal = orderTotal.subtract(allocatedTotal);
                } else {
                    discountedLineTotal = lineTotal
                            .multiply(orderTotal)
                            .divide(totalBeforeDiscount, 2, RoundingMode.HALF_UP);

                    allocatedTotal = allocatedTotal.add(discountedLineTotal);
                }
            } else {
                discountedLineTotal = BigDecimal.ZERO;
            }

            if (item.getPurchasePriceAtSale() == null) {
                item.setProfitAtSale(null);
                continue;
            }

            BigDecimal costTotal = item.getPurchasePriceAtSale()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal profit = discountedLineTotal.subtract(costTotal);

            item.setProfitAtSale(profit.setScale(2, RoundingMode.HALF_UP));
        }
    }
}
