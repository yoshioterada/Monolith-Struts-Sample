package com.skishop.service;

import com.skishop.exception.BusinessException;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.CartItem;
import com.skishop.model.Inventory;
import com.skishop.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private CartItem createCartItem(String productId, int quantity) {
        var item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Inventory createInventory(int quantity, int reserved) {
        var inv = new Inventory();
        inv.setQuantity(quantity);
        inv.setReservedQuantity(reserved);
        return inv;
    }

    @Test
    @DisplayName("在庫が十分な場合、予約数が加算される")
    void should_reserveItems_when_stockSufficient() {
        // Arrange
        var item = createCartItem("P001", 2);
        var inventory = createInventory(10, 0);
        when(inventoryRepository.findByProductId("P001")).thenReturn(Optional.of(inventory));

        // Act
        inventoryService.reserveItems(List.of(item));

        // Assert
        var captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getReservedQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("在庫が不足している場合、BusinessExceptionをスローする")
    void should_throwException_when_stockInsufficient() {
        // Arrange
        var item = createCartItem("P002", 5);
        var inventory = createInventory(3, 0);
        when(inventoryRepository.findByProductId("P002")).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveItems(List.of(item)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("アイテムリストがnullの場合、何もしない")
    void should_doNothing_when_reserveItemsWithNull() {
        // Act
        inventoryService.reserveItems(null);

        // Assert
        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    @DisplayName("アイテムリストが空の場合、何もしない")
    void should_doNothing_when_reserveItemsEmpty() {
        // Act
        inventoryService.reserveItems(List.of());

        // Assert
        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    @DisplayName("在庫解放した場合、予約数が減少する")
    void should_releaseItems_when_itemsReleased() {
        // Arrange
        var item = createCartItem("P003", 2);
        var inventory = createInventory(10, 5);
        when(inventoryRepository.findByProductId("P003")).thenReturn(Optional.of(inventory));

        // Act
        inventoryService.releaseItems(List.of(item));

        // Assert
        var captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getReservedQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("在庫控除した場合、数量と予約数が減少する")
    void should_deductStock_when_itemsDeducted() {
        // Arrange
        var item = createCartItem("P004", 3);
        var inventory = createInventory(10, 3);
        when(inventoryRepository.findByProductId("P004")).thenReturn(Optional.of(inventory));

        // Act
        inventoryService.deductStock(List.of(item));

        // Assert
        var captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(7);
        assertThat(captor.getValue().getReservedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("在庫があり数量が十分な場合、trueを返す")
    void should_returnTrue_when_stockSufficient() {
        // Arrange
        var inventory = createInventory(10, 2);
        when(inventoryRepository.findByProductId("P005")).thenReturn(Optional.of(inventory));

        // Act
        var result = inventoryService.checkStock("P005", 5);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("在庫が不足している場合、falseを返す")
    void should_returnFalse_when_stockInsufficient() {
        // Arrange
        var inventory = createInventory(3, 2);
        when(inventoryRepository.findByProductId("P006")).thenReturn(Optional.of(inventory));

        // Act
        var result = inventoryService.checkStock("P006", 5);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("在庫レコードが存在しない場合、falseを返す")
    void should_returnFalse_when_inventoryNotFound() {
        // Arrange
        when(inventoryRepository.findByProductId("P007")).thenReturn(Optional.empty());

        // Act
        var result = inventoryService.checkStock("P007", 1);

        // Assert
        assertThat(result).isFalse();
    }
}
