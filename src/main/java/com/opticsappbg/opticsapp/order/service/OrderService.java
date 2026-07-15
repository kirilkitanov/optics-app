package com.opticsappbg.opticsapp.order.service;

import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.order.model.*;
import com.opticsappbg.opticsapp.order.repository.OrderRepository;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.web.dto.CreateOrderItemRequest;
import com.opticsappbg.opticsapp.web.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductService productService;
    private final OrderPricingService pricingService;

    @Transactional
    public Order createOrder(CreateOrderRequest request, User user) {

        Order order = new Order();
        order.setCustomer(
                customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"))
        );

        order.setCreatedOn(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedBy(user);

        for (CreateOrderItemRequest itemReq : request.getItems()) {

            Product product = productService.findOrThrow(itemReq);

            OrderItem item = buildItem(itemReq, product);

            item.setOrder(order);
            order.getItems().add(item);
        }

        pricingService.calculate(order, request);

        return orderRepository.save(order);
    }

    private OrderItem buildItem(CreateOrderItemRequest req, Product product) {

        return OrderItem.builder()
                .product(product)
                .eyeSide(req.getEyeSide())
                .axis(req.getAxis())
                .prismBase(req.getPrismBase())
                .quantity(req.getQuantity())
                .priceAtPurchase(req.getPriceAtPurchase())
                .purchasePriceAtPurchase(product.getPurchasePrice())
                .createdOn(LocalDateTime.now())
                .build();
    }
}
