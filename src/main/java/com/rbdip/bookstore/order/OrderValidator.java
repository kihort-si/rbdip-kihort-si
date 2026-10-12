package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Validates input and resolves the products before any order is written. */
@Component
public class OrderValidator {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    public OrderValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ValidatedOrderItem> validate(CreateOrderRequest request) {
        requireText(request.customerFullName(), "customerFullName");
        requireText(request.customerAddress(), "customerAddress");
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        return request.items().stream().map(this::validateItem).toList();
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private ValidatedOrderItem validateItem(CreateOrderRequest.Item item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
        int quantity = item.quantity() == null ? DEFAULT_QUANTITY : item.quantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return new ValidatedOrderItem(product, quantity);
    }
}
