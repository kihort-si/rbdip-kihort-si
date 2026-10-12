package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes the order and its items in the transaction opened by OrderService. */
@Component
public class OrderPersistence {

    private static final String INITIAL_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistence(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Order save(CreateOrderRequest request, List<ValidatedOrderItem> items) {
        Order order = orderRepository.save(new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), INITIAL_STATUS));
        for (ValidatedOrderItem item : items) {
            orderItemRepository.save(new OrderItem(
                    order.getId(), item.product().getName(), item.product().getPrice(), item.quantity()));
        }
        return order;
    }
}
