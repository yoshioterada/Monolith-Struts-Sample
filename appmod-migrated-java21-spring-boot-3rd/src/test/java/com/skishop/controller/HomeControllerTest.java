package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.model.Product;
import com.skishop.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(TestSecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("トップページにアクセスすると注目商品が表示される")
    void should_displayFeaturedProducts_when_homePageAccessed() throws Exception {
        when(productService.findByStatus("ACTIVE")).thenReturn(List.of(new Product()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("featuredProducts"));
    }

    @Test
    @DisplayName("商品がない場合でもトップページが正常に表示される")
    void should_displayEmptyHome_when_noActiveProducts() throws Exception {
        when(productService.findByStatus("ACTIVE")).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("featuredProducts"));
    }
}
