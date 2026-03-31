package com.skishop.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security authorization tests using the full Spring context.
 * Verifies URL-level access control and CSRF protection per PLAN.md Phase 7 completion criteria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Unauthenticated access ---

    @Test
    @DisplayName("未認証ユーザーが/admin/**にアクセスした場合、ログインページにリダイレクトされる")
    void should_redirectToLogin_when_unauthenticatedAccessesAdmin() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("未認証ユーザーが/ordersにアクセスした場合、ログインページにリダイレクトされる")
    void should_redirectToLogin_when_unauthenticatedAccessesOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("未認証ユーザーが/productsにアクセスした場合、200 OKが返る（公開ページ）")
    void should_return200_when_unauthenticatedAccessesProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("未認証ユーザーが/auth/loginにアクセスした場合、200 OKが返る")
    void should_return200_when_unauthenticatedAccessesLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk());
    }

    // --- Role-based access ---

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USERロールが/admin/**にアクセスした場合、403が返る")
    void should_return403_when_userAccessesAdminPage() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMINロールが/admin/**にアクセスした場合、200が返る")
    void should_return200_when_adminAccessesAdminPage() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USERロールが/ordersにアクセスした場合、200が返る")
    void should_return200_when_userAccessesOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }

    // --- CSRF protection ---

    @Test
    @DisplayName("CSRFトークンなしのPOSTは403が返る")
    void should_return403_when_postWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                .param("username", "user@example.com")
                .param("password", "password"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRFトークン付きのPOSTは認証処理が実行される（302リダイレクト）")
    void should_process_when_postWithCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                .param("username", "user@example.com")
                .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection());
    }
}
