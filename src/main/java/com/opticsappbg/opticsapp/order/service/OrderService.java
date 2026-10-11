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
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        order.setNote(request.getNote());

        String generatedOrderNumber = "ORD-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"));
        order.setOrderNumber(generatedOrderNumber);

        addItemsToOrder(order, request);

        pricingService.calculate(order, request);

        for (OrderItem item : order.getItems()) {
            productService.adjustQuantity(
                    item.getProduct().getId(),
                    -item.getQuantity());
        }

        return orderRepository.save(order);
    }

    private OrderItem buildItem(CreateOrderItemRequest req, Product product) {

        return OrderItem.builder()
                .product(product)
                .eyeSide(req.getEyeSide())
                .axis(req.getAxis())
                .prismBase(req.getPrismBase())
                .quantity(req.getQuantity())
                .salePriceAtSale(req.getSalePriceAtSale())
                .purchasePriceAtSale(product.getPurchasePrice())
                .createdOn(LocalDateTime.now())
                .build();
    }

    private void addItemsToOrder(Order order, CreateOrderRequest request) {
        for (CreateOrderItemRequest itemReq : request.getItems()) {
            Product product = productService.findOrCreate(itemReq);
            OrderItem item = buildItem(itemReq, product);
            item.setOrder(order);
            order.getItems().add(item);
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public CreateOrderRequest buildEditRequest(Order order) {

        CreateOrderRequest request = new CreateOrderRequest();

        request.setCustomerId(order.getCustomer().getId());
        request.setTotalBeforeDiscount(order.getTotalBeforeDiscount());
        request.setDiscountPercentage(order.getDiscountPercentage());
        request.setTotal(order.getTotal());
        request.setNote(order.getNote());

        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();

            itemRequest.setCategory(product.getCategory());
            itemRequest.setProductId(product.getId());
            itemRequest.setBarcode(product.getBarcode());
            itemRequest.setBrand(product.getBrand());
            itemRequest.setModel(product.getModel());
            itemRequest.setSizeOrDiameter(product.getSizeOrDiameter());

            itemRequest.setSph(product.getSph());
            itemRequest.setCyl(product.getCyl());
            itemRequest.setAddPower(product.getAddPower());
            itemRequest.setPrism(product.getPrism());

            itemRequest.setEyeSide(item.getEyeSide());
            itemRequest.setAxis(item.getAxis());
            itemRequest.setPrismBase(item.getPrismBase());

            itemRequest.setQuantity(item.getQuantity());
            itemRequest.setSalePriceAtSale(item.getSalePriceAtSale());

            request.getItems().add(itemRequest);
        }

        return request;
    }

//    @Transactional
//    public void updateOrder(Long id, CreateOrderRequest request) {
//
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        order.setCustomer(
//                customerRepository.findById(request.getCustomerId())
//                        .orElseThrow(() -> new RuntimeException("Customer not found"))
//        );
//        order.setNote(request.getNote());
//
//        order.getItems().clear();
//        addItemsToOrder(order, request);
//        pricingService.calculate(order, request);
//        order.setUpdatedOn(LocalDateTime.now());
//
//        orderRepository.save(order);
//    }

    @Transactional
    public void updateOrder(Long id, CreateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Анулирана поръчка не може да бъде редактирана."
            );
        }
        // Връщаме старите количества в склада.
        for (OrderItem item : order.getItems()) {
            productService.adjustQuantity(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
        order.setCustomer(
                customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"))
        );

        order.setNote(request.getNote());

        order.getItems().clear();
        addItemsToOrder(order, request);
        // Изписваме новите количества.
        for (OrderItem item : order.getItems()) {
            productService.adjustQuantity(
                    item.getProduct().getId(),
                    -item.getQuantity()
            );
        }

        pricingService.calculate(order, request);
        order.setUpdatedOn(LocalDateTime.now());

        orderRepository.save(order);
    }

    public List<Order> getActiveOrders() {
        return orderRepository.findByStatusNotInOrderByCreatedOnDesc(
                List.of(
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELLED
                )
        );
    }

    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatus(Long id, OrderStatus newStatus) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Анулирана поръчка не може да се променя повторно.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // При анулиране връщаме всички количества в склада.
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                productService.adjustQuantity(
                        item.getProduct().getId(),
                        item.getQuantity()
                );
            }
        }

        order.setStatus(newStatus);
        order.setUpdatedOn(LocalDateTime.now());

        orderRepository.save(order);
    }
}
