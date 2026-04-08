package com.skishop.config;

import com.skishop.security.SkiShopUserDetails;
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
 * ログイン成功時に匿名セッションカートをユーザーの永続カートへマージする認証成功ハンドラー。
 *
 * <p>SkiShop では、未ログイン（匿名）ユーザーがカートに商品を追加した場合、
 * セッション上の {@code cartId} 属性に紐づく一時カートとして DB に保存される。
 * ユーザーがログインに成功すると、本ハンドラーが以下の処理を行う:</p>
 *
 * <ol>
 *   <li>HTTP セッションから {@code cartId} 属性を取得する</li>
 *   <li>{@code cartId} が存在する場合、{@link CartService#mergeSessionCart(String, String)} を呼び出し、
 *       匿名カートの商品をログインユーザーのカートにマージする</li>
 *   <li>マージ完了後、セッションから {@code cartId} 属性を削除する</li>
 *   <li>マージ処理中に例外が発生してもログイン自体は成功させる（カートマージの失敗はログ出力のみ）</li>
 *   <li>親クラスの処理を呼び出し、デフォルトターゲット URL（{@code /}）へリダイレクトする</li>
 * </ol>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムでは {@code LoginAction} 内で
 * セッションベースのカート管理を手動実装していたが、Spring Security の
 * {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler} に移行した。</p>
 *
 * <p>設計書参照: {@code docs/migration/DESIGN.md §6.6 カートセッション管理}</p>
 *
 * @see SecurityConfig#cartMergeSuccessHandler() Bean 定義元
 * @see CartService#mergeSessionCart(String, String) カートマージのビジネスロジック
 */
@Slf4j
public class CartMergeSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /** カートのマージ処理を担う {@link CartService}。 */
    private final CartService cartService;

    /**
     * カートマージ成功ハンドラーを生成する。
     *
     * <p>デフォルトのリダイレクト先を {@code /}（トップページ）に設定し、
     * 常にデフォルトターゲット URL を使用するように構成する。</p>
     *
     * @param cartService カートのマージ処理を行うサービス
     */
    public CartMergeSuccessHandler(CartService cartService) {
        super("/");
        this.cartService = cartService;
        setAlwaysUseDefaultTargetUrl(true);
    }

    /**
     * 認証成功時に呼び出されるコールバックメソッド。
     *
     * <p>HTTP セッションに {@code cartId} 属性が存在する場合、その匿名カートを
     * ログインユーザーのカートにマージする。マージに失敗しても認証処理自体は
     * 正常に完了させ、トップページへリダイレクトする。</p>
     *
     * @param request  HTTP リクエスト
     * @param response HTTP レスポンス
     * @param authentication 認証済みのユーザー情報（{@code getName()} でメールアドレスを取得）
     * @throws IOException      リダイレクト処理中に I/O エラーが発生した場合
     * @throws ServletException サーブレット処理中にエラーが発生した場合
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String cartId = (String) session.getAttribute("cartId");
            if (cartId != null) {
                String userId = null;
                if (authentication.getPrincipal() instanceof SkiShopUserDetails skiUser) {
                    userId = skiUser.getUserId();
                }
                if (userId != null) {
                    try {
                        cartService.mergeCartById(cartId, userId);
                        session.removeAttribute("cartId");
                    } catch (RuntimeException e) {
                        log.warn("Cart merge failed for cartId={}", cartId, e);
                    }
                }
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
