package com.skishop.config;

import com.skishop.security.SkiShopUserDetails;
import com.skishop.service.AuthService;
import com.skishop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * ログイン成功時に匿名カートのマージとセキュリティログの記録を行う認証成功ハンドラー。
 *
 * <p>CartMergeSuccessHandler を置き換え、AuthService.recordLoginSuccess() の呼び出しを追加。</p>
 */
@Slf4j
public class CustomAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CartService cartService;
    private final AuthService authService;

    public CustomAuthSuccessHandler(CartService cartService, AuthService authService) {
        super("/");
        this.cartService = cartService;
        this.authService = authService;
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String userId = null;
        if (authentication.getPrincipal() instanceof SkiShopUserDetails skiUser) {
            userId = skiUser.getUserId();
        }

        // Record login success (H-8)
        if (userId != null) {
            try {
                authService.recordLoginSuccess(userId, request.getRemoteAddr(), request.getHeader("User-Agent"));
            } catch (RuntimeException e) {
                log.warn("Failed to record login success for userId={}", userId, e);
            }
        }

        // Cart merge (existing CartMergeSuccessHandler logic)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String cartId = (String) session.getAttribute("cartId");
            if (cartId != null && userId != null) {
                try {
                    cartService.mergeCartById(cartId, userId);
                    session.removeAttribute("cartId");
                } catch (RuntimeException e) {
                    log.warn("Cart merge failed for cartId={}", cartId, e);
                }
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
