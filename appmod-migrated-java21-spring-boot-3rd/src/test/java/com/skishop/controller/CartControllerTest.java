package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.model.Cart;
import com.skishop.service.CartService;
import com.skishop.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartController.class)
@Import(TestSecurityConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private CouponService couponService;

    @Test
    @DisplayName("ログイン済みユーザーでカートを表示する")
    @WithMockUser(username = "user-id-1")
    void should_displayCart_when_userLoggedIn() throws Exception {
        Cart cart = new Cart();
        cart.setId("cart-1");
        when(cartService.resolveCart(anyString(), anyString(), any())).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of());
        when(cartService.calculateSubtotal(any())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attributeExists("cart", "items", "subtotal"));
    }

    @Test
    @DisplayName("バリデーションエラーがあった場合カートにリダイレクトする")
    @WithMockUser(username = "user-id-1")
    void should_redirectToCart_when_invalidCartItem() throws Exception {
        Cart cart = new Cart();
        cart.setId("cart-1");
        when(cartService.resolveCart(anyString(), anyString(), any())).thenReturn(cart);

        mockMvc.perform(post("/cart/items")
                        .param("productId", "")
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection());
    }
}
