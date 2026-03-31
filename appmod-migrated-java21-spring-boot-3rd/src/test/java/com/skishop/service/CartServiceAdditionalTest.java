package com.skishop.service;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional CartService tests covering getOrCreateCart, mergeSessionCart,
 * addItem (new item), updateItemQuantity, removeItem branches.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceAdditionalTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private CartService cartService;

    // -------- getOrCreateCart --------

    @Test
    @DisplayName("ユーザーのアクティブカートが存在する場合、既存カートを返す")
    void should_returnExistingCart_when_userActiveCartExists() {
        // Arrange
        var existingCart = new Cart();
        existingCart.setId("cart-existing");
        when(cartRepository.findByUserIdAndStatus("user-1", "ACTIVE"))
                .thenReturn(List.of(existingCart));

        // Act
        var result = cartService.getOrCreateCart("user-1", "session-1");

        // Assert
        assertThat(result.getId()).isEqualTo("cart-existing");
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("セッションカートが存在する場合、セッションカートを返す")
    void should_returnSessionCart_when_sessionCartExists() {
        // Arrange
        var sessionCart = new Cart();
        sessionCart.setId("cart-session");
        when(cartRepository.findBySessionId("sess-1")).thenReturn(Optional.of(sessionCart));

        // Act
        var result = cartService.getOrCreateCart(null, "sess-1");

        // Assert
        assertThat(result.getId()).isEqualTo("cart-session");
    }

    @Test
    @DisplayName("カートが存在しない場合、新規カートを作成する")
    void should_createNewCart_when_noExistingCart() {
        // Arrange
        when(cartRepository.findBySessionId(anyString())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = cartService.getOrCreateCart(null, "sess-new");

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository).save(any(Cart.class));
    }

    // -------- addItem (new item) --------

    @Test
    @DisplayName("カートに存在しない商品を追加した場合、新規アイテムが作成される")
    void should_createNewItem_when_productNotInCart() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId("cart-1", "prod-new")).thenReturn(Optional.empty());
        when(priceRepository.findByProductId("prod-new")).thenReturn(List.of());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.addItem("cart-1", "prod-new", 2);

        // Assert
        var captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo("prod-new");
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("数量が0以下の場合、カートに追加しない")
    void should_notAddItem_when_quantityZero() {
        // Act
        cartService.addItem("cart-1", "prod-1", 0);

        // Assert
        verify(cartRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("ユニット価格が取得できる場合、価格が設定される")
    void should_setUnitPrice_when_priceExists() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-2");
        var price = new Price();
        price.setRegularPrice(new BigDecimal("5000"));
        when(cartRepository.findById("cart-2")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId("cart-2", "prod-priced")).thenReturn(Optional.empty());
        when(priceRepository.findByProductId("prod-priced")).thenReturn(List.of(price));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.addItem("cart-2", "prod-priced", 1);

        // Assert
        var captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    // -------- updateItemQuantity --------

    @Test
    @DisplayName("数量を0に更新した場合、アイテムが削除される")
    void should_deleteItem_when_quantitySetToZero() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        var item = new CartItem();
        item.setId("item-1");
        item.setCart(cart);
        when(cartItemRepository.findById("item-1")).thenReturn(Optional.of(item));

        // Act
        cartService.updateItemQuantity("item-1", 0, "cart-1");

        // Assert
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("数量を正の値に更新した場合、アイテム数量が変更される")
    void should_updateQuantity_when_quantityPositive() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        var item = new CartItem();
        item.setId("item-2");
        item.setQuantity(3);
        item.setCart(cart);
        when(cartItemRepository.findById("item-2")).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.updateItemQuantity("item-2", 5, "cart-1");

        // Assert
        var captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    // -------- removeItem --------

    @Test
    @DisplayName("アイテムを削除した場合、deleteが呼ばれる")
    void should_callDeleteById_when_removeItem() {
        // Arrange
        var cart = new Cart();
        cart.setId("cart-1");
        var item = new CartItem();
        item.setId("item-3");
        item.setCart(cart);
        when(cartItemRepository.findById("item-3")).thenReturn(Optional.of(item));

        // Act
        cartService.removeItem("item-3", "cart-1");

        // Assert
        verify(cartItemRepository).delete(item);
    }

    // -------- mergeSessionCart --------

    @Test
    @DisplayName("セッションカートが存在しない場合、マージしない")
    void should_doNothing_when_noSessionCart() {
        // Arrange
        when(cartRepository.findBySessionId("sess-empty")).thenReturn(Optional.empty());

        // Act
        cartService.mergeSessionCart("sess-empty", "user-1");

        // Assert
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("ユーザーカートが存在しない場合、セッションカートをユーザーカートに変換する")
    void should_assignSessionCartToUser_when_noUserCart() {
        // Arrange
        var sessionCart = new Cart();
        sessionCart.setId("cart-sess");
        when(cartRepository.findBySessionId("sess-1")).thenReturn(Optional.of(sessionCart));
        when(cartRepository.findByUserIdAndStatus("user-2", "ACTIVE")).thenReturn(List.of());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        cartService.mergeSessionCart("sess-1", "user-2");

        // Assert
        assertThat(sessionCart.getUserId()).isEqualTo("user-2");
        verify(cartRepository).save(sessionCart);
    }
}
