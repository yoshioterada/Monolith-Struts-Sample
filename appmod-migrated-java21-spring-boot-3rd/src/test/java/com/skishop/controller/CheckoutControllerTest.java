package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.dto.request.PlaceOrderCommand;
import com.skishop.dto.response.CheckoutSummary;
import com.skishop.model.Cart;
import com.skishop.model.Order;
import com.skishop.service.CartService;
import com.skishop.service.CheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.skishop.security.WithSkiShopUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@WebMvcTest(CheckoutController.class)
@Import(TestSecurityConfig.class)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private CheckoutService checkoutService;

    @Test
    @DisplayName("チェックアウト画面を表示する（認証済みユーザー）")
    @WithSkiShopUser(userId = "user-id-1")
    void should_displayCheckoutPage_when_authenticated() throws Exception {
        // Arrange
        Cart cart = new Cart();
        cart.setId("cart-1");
        var summary = new CheckoutSummary(cart, List.of(),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        when(checkoutService.prepareCheckoutSummary(anyString(), anyString())).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/index"))
                .andExpect(model().attributeExists("cart", "items", "subtotal"));
    }

    @Test
    @DisplayName("バリデーションエラーがある場合、チェックアウト画面を再表示する")
    @WithSkiShopUser(userId = "user-id-1")
    void should_redirectToCheckout_when_validationErrors() throws Exception {
        // Arrange: mock prepareCheckoutSummary for re-rendering
        Cart cart = new Cart();
        cart.setId("cart-1");
        var summary = new CheckoutSummary(cart, List.of(),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        when(checkoutService.prepareCheckoutSummary(anyString(), anyString())).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(post("/checkout")
                        .param("paymentMethod", "")   // @NotBlank: 空はエラー
                        .param("usePoints", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/index"));
    }

    @Test
    @DisplayName("正常な注文確定で注文詳細へリダイレクトする")
    @WithSkiShopUser(userId = "user-id-1")
    void should_redirectToOrderDetail_when_orderPlacedSuccessfully() throws Exception {
        // Arrange
        Cart cart = new Cart();
        cart.setId("cart-1");
        Order order = new Order();
        order.setId("order-1");
        when(cartService.getOrCreateCart(anyString(), anyString())).thenReturn(cart);
        when(checkoutService.placeOrder(any(PlaceOrderCommand.class)))
                .thenReturn(order);

        // Act & Assert
        mockMvc.perform(post("/checkout")
                        .param("paymentMethod", "CREDIT_CARD")
                        .param("cardNumber", "4111111111111111")
                        .param("cardExpMonth", "12")
                        .param("cardExpYear", "2027")
                        .param("cardCvv", "123")
                        .param("billingZip", "1000001")
                        .param("usePoints", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));
    }
}
