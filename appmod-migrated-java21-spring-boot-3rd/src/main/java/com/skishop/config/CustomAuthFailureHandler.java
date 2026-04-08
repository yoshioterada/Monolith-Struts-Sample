package com.skishop.config;

import com.skishop.service.AuthService;
import com.skishop.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

/**
 * ログイン失敗時にセキュリティログを記録し、アカウントロックアウトを実施する認証失敗ハンドラー。
 *
 * <p>AuthService.recordLoginFailure() を呼び出して失敗回数を記録し、
 * 一定回数（5回）を超えるとアカウントをロックする。</p>
 */
@Slf4j
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;

    public CustomAuthFailureHandler(AuthService authService,
                                     CustomUserDetailsService userDetailsService) {
        super("/auth/login?error=true");
        this.authService = authService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && !email.isBlank()) {
            try {
                var userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails instanceof com.skishop.security.SkiShopUserDetails skiUser) {
                    authService.recordLoginFailure(skiUser.getUserId(), request.getRemoteAddr(), request.getHeader("User-Agent"));
                }
            } catch (UsernameNotFoundException e) {
                log.debug("Login failure for non-existent user");
            } catch (RuntimeException e) {
                log.warn("Failed to record login failure: {}", e.getMessage());
            }
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
