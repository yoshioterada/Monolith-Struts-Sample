package com.skishop.controller.admin;

import com.skishop.config.TestSecurityConfig;
import com.skishop.model.Order;
import com.skishop.model.Product;
import com.skishop.service.CategoryService;
import com.skishop.service.CouponService;
import com.skishop.service.CheckoutService;
import com.skishop.service.OrderService;
import com.skishop.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AdminProductController.class, AdminOrderController.class,
             AdminCouponController.class, AdminShippingMethodController.class})
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CheckoutService checkoutService;

    @MockBean
    private CouponService couponService;

    @MockBean
    private com.skishop.service.AdminShippingMethodService adminShippingMethodService;

    // ===================== AdminProductController =====================

    @Test
    @DisplayName("管理者商品一覧ページが表示される")
    @WithMockUser(roles = "ADMIN")
    void should_displayProductList_when_adminProductListRequested() throws Exception {
        // Arrange
        when(productService.findByStatus(anyString())).thenReturn(List.of(new Product()));

        // Act & Assert
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    @DisplayName("管理者商品追加フォームが表示される")
    @WithMockUser(roles = "ADMIN")
    void should_displayNewProductForm_when_adminNewProductRequested() throws Exception {
        // Arrange
        when(categoryService.listAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().attributeExists("adminProductRequest", "categories"));
    }

    // ===================== AdminOrderController =====================

    @Test
    @DisplayName("管理者注文一覧ページが表示される")
    @WithMockUser(roles = "ADMIN")
    void should_displayOrderList_when_adminOrderListRequested() throws Exception {
        // Arrange
        when(orderService.listAll(anyInt())).thenReturn(List.of(new Order()));

        // Act & Assert
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders/list"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @DisplayName("管理者注文詳細ページが表示される")
    @WithMockUser(roles = "ADMIN")
    void should_displayOrderDetail_when_adminOrderDetailRequested() throws Exception {
        // Arrange
        var order = new Order();
        order.setId("o-1");
        when(orderService.findById("o-1")).thenReturn(order);
        when(orderService.listItems("o-1")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/admin/orders/o-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders/detail"))
                .andExpect(model().attributeExists("order", "items"));
    }

    // ===================== AdminCouponController =====================

    @Test
    @DisplayName("管理者クーポン一覧ページが表示される")
    @WithMockUser(roles = "ADMIN")
    void should_displayCouponList_when_adminCouponListRequested() throws Exception {
        // Arrange
        when(couponService.listAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/coupons/list"))
                .andExpect(model().attributeExists("coupons"));
    }
}
