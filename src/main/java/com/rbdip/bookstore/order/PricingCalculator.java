package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Applies quantity, customer, coupon and large-order discounts, then rounds once. */
@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_PRICE_FACTOR = new BigDecimal("0.95");
    private static final BigDecimal VIP_PRICE_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal WHOLESALE_PRICE_FACTOR = new BigDecimal("0.85");
    private static final BigDecimal COUPON_PRICE_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_PRICE_FACTOR = new BigDecimal("0.98");
    private static final String VIP_CUSTOMER_TYPE = "vip";
    private static final String WHOLESALE_CUSTOMER_TYPE = "wholesale";
    private static final String FIXED_DISCOUNT_COUPON = "SAVE10";
    private static final String PERCENT_DISCOUNT_COUPON = "SAVE20PERCENT";
    private static final BigDecimal FIXED_COUPON_DISCOUNT = BigDecimal.TEN;
    private static final int MONEY_SCALE = 2;

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_PRICE_FACTOR);
            }
            total = total.add(linePrice);
        }
        total = total.multiply(customerPriceFactor(customerType));
        total = applyCoupon(total, couponCode).max(BigDecimal.ZERO);
        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_PRICE_FACTOR);
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal customerPriceFactor(String customerType) {
        if (VIP_CUSTOMER_TYPE.equals(customerType)) {
            return VIP_PRICE_FACTOR;
        }
        if (WHOLESALE_CUSTOMER_TYPE.equals(customerType)) {
            return WHOLESALE_PRICE_FACTOR;
        }
        return BigDecimal.ONE;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (FIXED_DISCOUNT_COUPON.equals(couponCode)) {
            return total.subtract(FIXED_COUPON_DISCOUNT);
        }
        if (PERCENT_DISCOUNT_COUPON.equals(couponCode)) {
            return total.multiply(COUPON_PRICE_FACTOR);
        }
        return total;
    }
}
