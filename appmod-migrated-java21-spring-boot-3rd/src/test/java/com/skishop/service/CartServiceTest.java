package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Cart;
import com.skishop.model.CartItem;
import com.skishop.model.Price;
import com.skishop.repository.CartItemRepository;
import com.skishop.repository.CartRepository;
import com.skishop.repository.PriceRepository;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("カートIDでカートを取得できる")
    void should_returnCart_when_cartExists() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        cart.setStatus("ACTIVE");
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));

        // Act
        var result = cartService.getCart("cart-1");

        // Assert
        assertThat(result.getId()).isEqualTo("cart-1");
    }

    @Test
    @DisplayName("存在しないカートIDの場合、例外をスローする")
    void should_throwException_when_cartNotFound() {
        // Arrange
        when(cartRepository.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.getCart("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("新しいカートを作成できる")
    void should_createNewCart_when_called() {
        // Arrange
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = cartService.createCart("user-1", "session-1");

        // Assert
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getSessionId()).isEqualTo("session-1");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("既存の商品のアイテム数量が加算される")
    void should_incrementQuantity_when_productAlreadyInCart() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));

        var existingItem = new CartItem();
        existingItem.setId("item-1");
        existingItem.setProductId("prod-1");
        existingItem.setQuantity(2);
        when(cartItemRepository.findByCartId("cart-1")).thenReturn(List.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.addItem("cart-1", "prod-1", 3);

        // Assert
        var captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("小計が正しく計算される")
    void should_calculateCorrectSubtotal_when_multipleItems() {
        // Arrange
        var item1 = new CartItem();
        item1.setUnitPrice(new BigDecimal("1000"));
        item1.setQuantity(2);

        var item2 = new CartItem();
        item2.setUnitPrice(new BigDecimal("500"));
        item2.setQuantity(3);

        // Act
        var result = cartService.calculateSubtotal(List.of(item1, item2));

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("3500"));
    }

    @Test
    @DisplayName("空のリストの場合、小計が0になる")
    void should_returnZero_when_itemsEmpty() {
        // Act
        var result = cartService.calculateSubtotal(List.of());

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("nullの場合、小計が0になる")
    void should_returnZero_when_itemsNull() {
        // Act
        var result = cartService.calculateSubtotal(null);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("カートをクリアするとステータスがCHECKED_OUTに変わる")
    void should_setStatusCheckedOut_when_clearCart() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        cart.setStatus("ACTIVE");
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.clearCart("cart-1");

        // Assert
        verify(cartItemRepository).deleteByCartId("cart-1");
        assertThat(cart.getStatus()).isEqualTo("CHECKED_OUT");
    }
}
