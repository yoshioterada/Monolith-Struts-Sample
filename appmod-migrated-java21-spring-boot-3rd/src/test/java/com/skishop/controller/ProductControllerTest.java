package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.service.CategoryService;
import com.skishop.service.ProductService;
import com.skishop.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("商品一覧を表示する")
    void should_displayProductList_when_listEndpointCalled() throws Exception {
        when(productService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(categoryService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products", "categories"));
    }

    @Test
    @DisplayName("商品詳細を表示する")
    void should_displayProductDetail_when_validIdProvided() throws Exception {
        Product product = new Product();
        product.setId("P001");
        product.setName("Test Product");
        product.setStatus("ACTIVE");
        when(productService.findById("P001")).thenReturn(product);

        mockMvc.perform(get("/products/P001"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(model().attributeExists("product"));
    }
}
