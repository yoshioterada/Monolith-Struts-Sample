package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Order;
import com.skishop.model.OrderItem;
import com.skishop.model.Return;
import com.skishop.repository.OrderItemRepository;
import com.skishop.repository.OrderRepository;
import com.skishop.repository.ReturnRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReturnRepository returnRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("注文とアイテムを作成できる")
    void should_createOrderAndItems_when_called() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        var item = new OrderItem();
        item.setId("item-1");

        // Act
        var result = orderService.createOrder(order, List.of(item));

        // Assert
        assertThat(result.getId()).isEqualTo("order-1");
        verify(orderItemRepository).save(item);
    }

    @Test
    @DisplayName("IDで注文を取得できる")
    void should_findOrder_when_idExists() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        // Act
        var result = orderService.findById("order-1");

        // Assert
        assertThat(result.getId()).isEqualTo("order-1");
    }

    @Test
    @DisplayName("存在しないIDの場合、例外をスローする")
    void should_throwException_when_orderNotFound() {
        // Arrange
        when(orderRepository.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.findById("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("ユーザーIDとオーナーシップが一致しない場合、例外をスローする")
    void should_throwException_when_userIdMismatch() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        when(orderRepository.findWithItemsById("order-1")).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.findByIdAndUserId("order-1", "user-2"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("返品記録が正しく作成される")
    void should_createReturnRecords_when_recordReturn() {
        // Arrange
        var item = new OrderItem();
        item.setId("item-1");
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("10000"));
        when(returnRepository.save(any(Return.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        orderService.recordReturn("order-1", List.of(item));

        // Assert
        var captor = ArgumentCaptor.forClass(Return.class);
        verify(returnRepository).save(captor.capture());
        var ret = captor.getValue();
        assertThat(ret.getOrderId()).isEqualTo("order-1");
        assertThat(ret.getOrderItemId()).isEqualTo("item-1");
        assertThat(ret.getStatus()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("注文ステータスを更新できる")
    void should_updateStatus_when_called() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        order.setStatus("CREATED");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        orderService.updateStatus("order-1", "SHIPPED");

        // Assert
        assertThat(order.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("buildOrderが正しいフィールドで注文を作成する")
    void should_buildOrder_when_called() {
        // Act
        var request = new com.skishop.dto.request.OrderBuildRequest("id-1", "ORD-001", "user-1",
                new BigDecimal("10000"), new BigDecimal("1000"),
                new BigDecimal("800"), new BigDecimal("500"),
                new BigDecimal("11300"), "SAVE10", 200);
        var order = orderService.buildOrder(request);

        // Assert
        assertThat(order.getId()).isEqualTo("id-1");
        assertThat(order.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(order.getStatus()).isEqualTo("CREATED");
        assertThat(order.getPaymentStatus()).isEqualTo("AUTHORIZED");
        assertThat(order.getUsedPoints()).isEqualTo(200);
        assertThat(order.getCouponCode()).isEqualTo("SAVE10");
    }
}
