package com.skishop.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntitySchemaValidationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("User エンティティが正しくスキーマにマッピングされる")
    void should_persistUser_when_validEntity() {
        // Arrange
        var user = new User();
        user.setId("u-test-1");
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPasswordHash("hash123");
        user.setSalt("salt123");
        user.setStatus("ACTIVE");
        user.setRole("USER");

        // Act
        var persisted = entityManager.persistAndFlush(user);

        // Assert
        assertThat(persisted.getId()).isEqualTo("u-test-1");
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SecurityLog エンティティが正しくスキーマにマッピングされる")
    void should_persistSecurityLog_when_validEntity() {
        var log = new SecurityLog();
        log.setId("sl-1");
        log.setUserId("u-1");
        log.setEventType("LOGIN_SUCCESS");
        log.setIpAddress("127.0.0.1");

        var persisted = entityManager.persistAndFlush(log);
        assertThat(persisted.getId()).isEqualTo("sl-1");
    }

    @Test
    @DisplayName("Product エンティティが正しくスキーマにマッピングされる")
    void should_persistProduct_when_validEntity() {
        var product = new Product();
        product.setId("P001");
        product.setName("Test Ski");
        product.setStatus("ACTIVE");

        var persisted = entityManager.persistAndFlush(product);
        assertThat(persisted.getId()).isEqualTo("P001");
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Order-OrderItem の親子関係が正しくマッピングされる")
    void should_persistOrderWithItems_when_validEntities() {
        // Arrange
        var order = new Order();
        order.setId("o-1");
        order.setOrderNumber("ORD-001");
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setSubtotal(BigDecimal.valueOf(10000));
        order.setTax(BigDecimal.valueOf(1000));
        order.setShippingFee(BigDecimal.valueOf(500));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(11500));
        order.setUsedPoints(0);

        var item = new OrderItem();
        item.setId("oi-1");
        item.setOrder(order);
        item.setProductId("P001");
        item.setProductName("Test Ski");
        item.setUnitPrice(BigDecimal.valueOf(10000));
        item.setQuantity(1);
        item.setSubtotal(BigDecimal.valueOf(10000));
        order.getItems().add(item);

        // Act
        entityManager.persistAndFlush(order);
        entityManager.clear();

        // Assert
        var found = entityManager.find(Order.class, "o-1");
        assertThat(found).isNotNull();
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getProductName()).isEqualTo("Test Ski");
    }

    @Test
    @DisplayName("Cart-CartItem の親子関係が正しくマッピングされる")
    void should_persistCartWithItems_when_validEntities() {
        var cart = new Cart();
        cart.setId("cart-1");
        cart.setStatus("ACTIVE");
        cart.setSessionId("session-123");

        var item = new CartItem();
        item.setId("ci-1");
        item.setCart(cart);
        item.setProductId("P001");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(5000));
        cart.getItems().add(item);

        entityManager.persistAndFlush(cart);
        entityManager.clear();

        var found = entityManager.find(Cart.class, "cart-1");
        assertThat(found).isNotNull();
        assertThat(found.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Category の自己参照（parent）が正しくマッピングされる")
    void should_persistCategoryWithParent_when_validEntities() {
        var parent = new Category();
        parent.setId("c-root");
        parent.setName("Sports");
        entityManager.persistAndFlush(parent);

        var child = new Category();
        child.setId("c-child");
        child.setName("Skiing");
        child.setParent(parent);
        entityManager.persistAndFlush(child);
        entityManager.clear();

        var found = entityManager.find(Category.class, "c-child");
        assertThat(found).isNotNull();
        assertThat(found.getParent().getId()).isEqualTo("c-root");
    }

    @Test
    @DisplayName("全エンティティのスキーマ生成が成功する（DDL 検証）")
    void should_createAllTables_when_contextLoads() {
        // このテストは @DataJpaTest のコンテキスト起動自体が
        // create-drop DDL でテーブル生成を検証する
        assertThat(entityManager).isNotNull();
    }
}
