package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingRefactoringTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({
        "10, 10, regular, , 100.00",
        "10, 11, regular, , 104.50",
        "10, 11, vip, SAVE10, 84.05",
        "10, 11, wholesale, SAVE20PERCENT, 71.06",
        "5, 1, regular, SAVE10, 0.00",
        "1000, 1, regular, , 1000.00",
        "1000.01, 1, regular, , 980.01",
        "1010, 1, regular, SAVE10, 1000.00",
        "1250, 1, regular, SAVE20PERCENT, 1000.00",
        "0.005, 1, regular, , 0.01",
        "10, 1, unknown, UNKNOWN, 10.00"
    })
    void preservesDiscountOrderThresholdsAndRounding(String price, int quantity,
            String customerType, String coupon, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), customerType, coupon);

        assertThat(total).isEqualTo(new BigDecimal(expected));
    }
}
