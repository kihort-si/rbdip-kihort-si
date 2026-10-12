package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

/** Keeps a resolved product together with its validated quantity. */
public record ValidatedOrderItem(Product product, int quantity) {

    public PricingCalculator.LineItem toLineItem() {
        return new PricingCalculator.LineItem(product.getPrice(), quantity);
    }
}
