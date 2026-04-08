package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.model.Order;
import com.skishop.service.CheckoutService;
import com.skishop.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.skishop.security.WithSkiShopUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CheckoutService checkoutService;

    @Test
    @DisplayName("注文一覧を表示する")
    @WithSkiShopUser(userId = "user-id-1")
    void should_displayOrderList_when_authenticated() throws Exception {
        when(orderService.listByUserId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @DisplayName("注文詳細を表示する（IDOR防止: ユーザーの注文のみ）")
    @WithSkiShopUser(userId = "user-id-1")
    void should_displayOrderDetail_when_ownerAccesses() throws Exception {
        Order order = new Order();
        order.setId("order-1");
        order.setUserId("user-id-1");
        when(orderService.findByIdAndUserId("order-1", "user-id-1")).thenReturn(order);
        when(orderService.listItems("order-1")).thenReturn(List.of());

        mockMvc.perform(get("/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"))
                .andExpect(model().attributeExists("order", "items"));
    }

    @Test
    @DisplayName("未認証ユーザーも一覧にアクセスできる (TestSecurityConfig: permitAll)")
    @WithSkiShopUser(userId = "user-id-anon")
    void should_accessOrderList_when_unauthenticated() throws Exception {
        when(orderService.listByUserId(any())).thenReturn(List.of());
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }
}
