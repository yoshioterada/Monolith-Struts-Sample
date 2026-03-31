package com.skishop.controller;

import com.skishop.config.TestSecurityConfig;
import com.skishop.service.MailService;
import com.skishop.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private MailService mailService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("ログインページが表示される")
    void should_displayLoginPage_when_getLoginCalled() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("登録ページが表示される")
    void should_displayRegisterPage_when_getRegisterCalled() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("バリデーションエラーがある場合、登録ページに戻る")
    void should_returnRegisterPage_when_validationErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("email", "invalid-email")
                        .param("password", "short")
                        .param("passwordConfirm", "short")
                        .param("username", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("パスワードリセットリクエストページが表示される")
    void should_displayForgotPasswordPage_when_getForgotCalled() throws Exception {
        mockMvc.perform(get("/auth/password/forgot"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password-forgot"));
    }
}
