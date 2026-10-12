package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class OrderCreationTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final OrderNotificationService notifications = mock(OrderNotificationService.class);
    private final OrderService service = new OrderService(new OrderValidator(products), new PricingCalculator(),
            new OrderPersistence(orders, items), notifications);

    @BeforeEach
    void configureProducts() {
        when(products.findById(1L)).thenReturn(Optional.of(
                new Product("Refactoring", new BigDecimal("40.00"), null)));
    }

    @Test
    void keepsProductsAndQuantitiesTogetherIncludingRepeatedProducts() {
        Order saved = mock(Order.class);
        when(saved.getId()).thenReturn(1L);
        when(orders.save(any(Order.class))).thenReturn(saved);
        CreateOrderRequest request = request(null, List.of(
                new CreateOrderRequest.Item(1L, null), new CreateOrderRequest.Item(1L, 2)));

        assertThat(service.createOrder(request)).isSameAs(saved);

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(order.capture());
        assertThat(order.getValue().getCustomerFullName()).isEqualTo("Anna");
        assertThat(order.getValue().getCustomerAddress()).isEqualTo("Moscow");
        assertThat(order.getValue().getCustomerPhone()).isEqualTo("123");
        assertThat(order.getValue().getStatus()).isEqualTo("new");
        ArgumentCaptor<OrderItem> lines = ArgumentCaptor.forClass(OrderItem.class);
        verify(items, org.mockito.Mockito.times(2)).save(lines.capture());
        assertThat(lines.getAllValues()).extracting(OrderItem::getQuantity).containsExactly(1, 2);
        assertThat(lines.getAllValues()).allSatisfy(line -> {
            assertThat(line.getOrderId()).isEqualTo(1L);
            assertThat(line.getProductName()).isEqualTo("Refactoring");
            assertThat(line.getProductPrice()).isEqualByComparingTo("40.00");
        });
        verify(notifications).sendConfirmationEmail("Anna", 1L, new BigDecimal("120.00"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidLaterItemBeforeWritingAnything(int quantity) {
        CreateOrderRequest request = request("regular", List.of(
                new CreateOrderRequest.Item(1L, 1), new CreateOrderRequest.Item(1L, quantity)));

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("quantity must be positive");
        verifyNoInteractions(orders, items, notifications);
    }

    @Test
    void rejectsMissingProductBeforeWritingAnything() {
        CreateOrderRequest request = request("regular", List.of(new CreateOrderRequest.Item(2L, 1)));

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("product 2 not found");
        verifyNoInteractions(orders, items, notifications);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void rejectsMissingCustomerName(String name) {
        CreateOrderRequest request = new CreateOrderRequest(name, "Moscow", null, null, null,
                List.of(new CreateOrderRequest.Item(1L, 1)));

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("customerFullName is required");
        verifyNoInteractions(orders, items, notifications);
        verify(products, never()).findById(any());
    }

    @Test
    void doesNotNotifyWhenSavingAnItemFails() {
        Order saved = mock(Order.class);
        when(saved.getId()).thenReturn(1L);
        when(orders.save(any(Order.class))).thenReturn(saved);
        when(items.save(any(OrderItem.class))).thenThrow(new IllegalStateException("write failed"));

        assertThatThrownBy(() -> service.createOrder(request("regular",
                List.of(new CreateOrderRequest.Item(1L, 1)))))
                .isInstanceOf(IllegalStateException.class).hasMessage("write failed");
        verifyNoInteractions(notifications);
    }

    private CreateOrderRequest request(String customerType, List<CreateOrderRequest.Item> lines) {
        return new CreateOrderRequest("Anna", "Moscow", "123", customerType, null, lines);
    }
}
