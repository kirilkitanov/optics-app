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

        String generatedOrderNumber = "ORD-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"));
        order.setOrderNumber(generatedOrderNumber);

        addItemsToOrder(order, request);

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

        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();

            itemRequest.setCategory(product.getCategory());
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
            itemRequest.setPriceAtPurchase(item.getPriceAtPurchase());

            request.getItems().add(itemRequest);
        }

        return request;
    }

    @Transactional
    public void updateOrder(Long id, CreateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setCustomer(
                customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"))
        );

        order.getItems().clear();
        addItemsToOrder(order, request);
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
}
