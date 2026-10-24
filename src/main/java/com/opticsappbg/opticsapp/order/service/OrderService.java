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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
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

        String generatedOrderNumber = "O-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss"));
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

            if (itemReq.getBrand() == null || itemReq.getBrand().isBlank()) {
                throw new IllegalArgumentException(
                        "Моля, въведете бранд за всеки продукт в поръчката."
                );
            }

            Product product = productService.findOrCreate(itemReq);
            OrderItem item = buildItem(itemReq, product);
            item.setOrder(order);
            order.getItems().add(item);
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedOnDesc();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getOrdersByPeriod(
            LocalDate startDate,
            LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate
                .plusDays(1)
                .atStartOfDay()
                .minusNanos(1);

        return orderRepository.findByCreatedOnBetweenOrderByCreatedOnDesc(
                startDateTime,
                endDateTime
        );
    }

    public List<Order> getOrdersForPeriod(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null && endDate == null) {
            return getAllOrders();
        }

        if (startDate == null) {
            return orderRepository
                    .findByCreatedOnLessThanOrderByCreatedOnDesc(
                            endDate.plusDays(1).atStartOfDay()
                    );
        }

        if (endDate == null) {
            return orderRepository
                    .findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
                            startDate.atStartOfDay()
                    );
        }

        return getOrdersByPeriod(startDate, endDate);
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

    @Transactional
    public void updateOrder(Long id, CreateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Анулирана поръчка не може да бъде редактирана."
            );
        }

        // Запазваме старите редове, преди да бъдат премахнати.
        List<OrderItem> previousItems = new java.util.ArrayList<>(order.getItems());

        // Връщаме старите количества в склада.
        for (OrderItem item : previousItems) {
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

        // Премахваме старите редове и изграждаме актуализираните.
        order.getItems().clear();
        addItemsToOrder(order, request);

        // Запазваме историческата доставна цена за продуктите,
        // които вече са присъствали в поръчката.
        for (OrderItem newItem : order.getItems()) {

            for (int i = 0; i < previousItems.size(); i++) {
                OrderItem oldItem = previousItems.get(i);

                if (oldItem.getProduct().getId()
                        .equals(newItem.getProduct().getId())) {

                    newItem.setPurchasePriceAtSale(
                            oldItem.getPurchasePriceAtSale()
                    );

                    // Всеки стар ред може да бъде използван само веднъж.
                    previousItems.remove(i);
                    break;
                }
            }
        }

        // Изписваме новите количества от склада.
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

                item.setProfitAtSale(null);
            }
        }

        order.setStatus(newStatus);
        order.setUpdatedOn(LocalDateTime.now());

        orderRepository.save(order);
    }

    public Page<Order> getOrdersForPeriod(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        if (startDate == null && endDate == null) {
            return orderRepository
                    .findAllByOrderByCreatedOnDesc(pageable);
        }

        if (startDate == null) {
            return orderRepository
                    .findByCreatedOnLessThanOrderByCreatedOnDesc(
                            endDate.plusDays(1).atStartOfDay(),
                            pageable
                    );
        }

        if (endDate == null) {
            return orderRepository
                    .findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
                            startDate.atStartOfDay(),
                            pageable
                    );
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate
                .plusDays(1)
                .atStartOfDay()
                .minusNanos(1);

        return orderRepository
                .findByCreatedOnBetweenOrderByCreatedOnDesc(
                        startDateTime,
                        endDateTime,
                        pageable
                );
    }

    public Page<Order> searchOrders(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Началната дата не може да бъде след крайната дата."
            );
        }

        String searchText = (search == null || search.isBlank())
                ? null
                : search.trim();

        LocalDate searchDate = null;
        Integer searchDay = null;
        Integer searchMonth = null;

        if (searchText != null) {

            // Кратка дата: например 13.10
            if (searchText.matches("\\d{1,2}\\.\\d{1,2}")) {
                try {
                    String[] parts = searchText.split("\\.");

                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);

                    // Проверяваме дали денят и месецът са валидни.
                    MonthDay.of(month, day);

                    searchDay = day;
                    searchMonth = month;
                    searchText = null;

                } catch (java.time.DateTimeException
                         | NumberFormatException ignored) {
                    // Невалидната дата остава обикновен текст за търсене.
                }
            } else {
                // Пълна дата: 13.10.2026 или 2026-10-13
                try {
                    searchDate = LocalDate.parse(
                            searchText,
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    );
                } catch (java.time.format.DateTimeParseException ignored) {
                    try {
                        searchDate = LocalDate.parse(searchText);
                    } catch (java.time.format.DateTimeParseException ignoredAgain) {
                        // Не е дата — търсим по номер, клиент или телефон.
                    }
                }

                if (searchDate != null) {
                    searchText = null;
                }
            }
        }

        LocalDateTime searchDateStart = searchDate == null
                ? null
                : searchDate.atStartOfDay();

        LocalDateTime searchDateEnd = searchDate == null
                ? null
                : searchDate.plusDays(1).atStartOfDay();

        LocalDateTime startDateTime = startDate == null
                ? null
                : startDate.atStartOfDay();

        LocalDateTime endDateTime = endDate == null
                ? null
                : endDate.plusDays(1).atStartOfDay();

        return orderRepository.searchOrders(
                searchText,
                searchDateStart,
                searchDateEnd,
                searchDay,
                searchMonth,
                startDateTime,
                endDateTime,
                pageable
        );
    }

}
