package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates order creation within a single transaction. */
@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";

    private final OrderValidator validator;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistence persistence;
    private final OrderNotificationService notificationService;

    public OrderService(OrderValidator validator, PricingCalculator pricingCalculator,
            OrderPersistence persistence, OrderNotificationService notificationService) {
        this.validator = validator;
        this.pricingCalculator = pricingCalculator;
        this.persistence = persistence;
        this.notificationService = notificationService;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<ValidatedOrderItem> items = validator.validate(request);
        String customerType = request.customerType() == null ? DEFAULT_CUSTOMER_TYPE : request.customerType();
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(ValidatedOrderItem::toLineItem).toList(), customerType, request.couponCode());
        Order order = persistence.save(request, items);
        notificationService.sendConfirmationEmail(request.customerFullName(), order.getId(), total);
        return order;
    }
}
