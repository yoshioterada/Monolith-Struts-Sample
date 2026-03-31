package com.skishop.repository;

import com.skishop.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order createOrder(String id, String orderNumber, String userId, String status) {
        var order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setStatus(status);
        order.setPaymentStatus("PENDING");
        order.setSubtotal(BigDecimal.valueOf(10000));
        order.setTax(BigDecimal.valueOf(1000));
        order.setShippingFee(BigDecimal.valueOf(500));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(11500));
        order.setUsedPoints(0);
        return order;
    }

    @Test
    @DisplayName("ユーザーIDで注文を検索した場合、該当ユーザーの注文リストを返す")
    void should_returnOrders_when_userIdMatches() {
        // Arrange
        var order1 = createOrder("o-1", "ORD-0001", "user-1", "CONFIRMED");
        var order2 = createOrder("o-2", "ORD-0002", "user-1", "SHIPPED");
        var order3 = createOrder("o-3", "ORD-0003", "user-2", "CONFIRMED");
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);

        // Act
        List<Order> user1Orders = orderRepository.findByUserId("user-1");

        // Assert
        assertThat(user1Orders).hasSize(2);
        assertThat(user1Orders).extracting(Order::getOrderNumber)
                .containsExactlyInAnyOrder("ORD-0001", "ORD-0002");
    }

    @Test
    @DisplayName("注文番号で注文を検索した場合、対応する注文を返す")
    void should_returnOrder_when_orderNumberExists() {
        // Arrange
        var order = createOrder("o-4", "ORD-0004", "user-3", "CONFIRMED");
        entityManager.persistAndFlush(order);

        // Act
        var result = orderRepository.findByOrderNumber("ORD-0004");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo("o-4");
        assertThat(result.orElseThrow().getUserId()).isEqualTo("user-3");
    }

    @Test
    @DisplayName("存在しない注文番号で検索した場合、空のOptionalを返す")
    void should_returnEmpty_when_orderNumberNotFound() {
        // Act
        var result = orderRepository.findByOrderNumber("ORD-NONEXISTENT");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("注文のないユーザーIDで検索した場合、空のリストを返す")
    void should_returnEmptyList_when_userHasNoOrders() {
        // Arrange
        var order = createOrder("o-5", "ORD-0005", "user-4", "CONFIRMED");
        entityManager.persistAndFlush(order);

        // Act
        List<Order> result = orderRepository.findByUserId("user-999");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("注文を保存および取得できる")
    void should_saveAndFind_when_orderPersisted() {
        // Arrange
        var order = createOrder("o-6", "ORD-0006", "user-5", "PENDING");

        // Act
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();
        var found = orderRepository.findById("o-6");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getOrderNumber()).isEqualTo("ORD-0006");
        assertThat(found.orElseThrow().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(11500));
        assertThat(found.orElseThrow().getCreatedAt()).isNotNull();
        assertThat(found.orElseThrow().getUpdatedAt()).isNotNull();
    }
}
