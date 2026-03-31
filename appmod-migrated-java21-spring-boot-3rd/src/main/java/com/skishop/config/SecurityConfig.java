package com.skishop.config;

import com.skishop.service.CartService;
import com.skishop.service.CustomUserDetailsService;
import com.skishop.util.LegacySha256PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security のセキュリティ設定を定義する構成クラス。
 *
 * <p>SkiShop アプリケーション全体の HTTP セキュリティフィルターチェーンを構成し、
 * 以下のセキュリティ機能を一元的に管理する。</p>
 *
 * <ul>
 *   <li><strong>URL ベースの認可制御</strong>:
 *     <ul>
 *       <li>{@code /admin/**} — ADMIN ロールのみアクセス可能</li>
 *       <li>{@code /account/**, /orders/**, /checkout/**, /cart/coupon, /points/**}
 *           — USER または ADMIN ロールが必要</li>
 *       <li>{@code /actuator/health, /actuator/info} — 全ユーザーに公開</li>
 *       <li>{@code /actuator/**} — ADMIN ロールのみ</li>
 *       <li>その他のリクエスト — 全ユーザーに公開（商品一覧・ログインページ等）</li>
 *     </ul>
 *   </li>
 *   <li><strong>フォームログイン</strong>: ログインページ {@code /auth/login}、
 *       成功時は {@link CartMergeSuccessHandler} による匿名カートのマージ後にリダイレクト</li>
 *   <li><strong>ログアウト</strong>: セッション無効化および {@code JSESSIONID} クッキー削除</li>
 *   <li><strong>セッション管理</strong>: セッション固定攻撃防止（{@code migrateSession}）、
 *       同時セッション数を 1 に制限</li>
 *   <li><strong>CSRF 保護</strong>: Spring Security デフォルトの CSRF 保護を有効化</li>
 *   <li><strong>セキュリティヘッダー</strong>: CSP（Content Security Policy）、
 *       X-Frame-Options（DENY）、X-Content-Type-Options（nosniff）、
 *       XSS Protection、HSTS（1 年間、サブドメイン含む）</li>
 * </ul>
 *
 * <p>パスワードエンコーダーとして {@link DelegatingPasswordEncoder} を使用し、
 * BCrypt（新規ユーザー・パスワード変更後）と SHA-256（レガシー移行用）の
 * 両方のハッシュ形式をサポートする。ユーザーが SHA-256 形式のパスワードでログインすると、
 * {@link CustomUserDetailsService#updatePassword} により自動的に BCrypt へアップグレードされる。</p>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムでは {@code web.xml} のセキュリティ制約と
 * カスタムフィルターで認証・認可を行っていたが、Spring Security の宣言的セキュリティに移行した。</p>
 *
 * @see CartMergeSuccessHandler ログイン成功時の匿名カートマージ処理
 * @see CustomUserDetailsService ユーザー認証情報の取得とパスワード自動アップグレード
 * @see LegacySha256PasswordEncoder レガシー SHA-256 パスワードの検証
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** ユーザー認証情報を提供するカスタム UserDetailsService。 */
    private final CustomUserDetailsService customUserDetailsService;

    /** カートマージ処理で使用するカートサービス。 */
    private final CartService cartService;

    /**
     * パスワードエンコーダーの Bean を生成する。
     *
     * <p>{@link DelegatingPasswordEncoder} を使用し、以下の 2 つのエンコーダーを登録する。</p>
     * <ul>
     *   <li><strong>{@code bcrypt}</strong>（デフォルト）: 新規パスワードのエンコードに使用</li>
     *   <li><strong>{@code sha256}</strong>: Flyway V2 で移行済みのレガシーパスワード
     *       （{@code {sha256}<hash>$<salt>} 形式）の照合に使用</li>
     * </ul>
     *
     * <p>DB 内のパスワードハッシュは以下の形式を取る:</p>
     * <ul>
     *   <li>{@code {sha256}<hash>$<salt>} — V2 マイグレーション適用後のレガシー形式</li>
     *   <li>{@code {bcrypt}<hash>} — ログイン成功時の BCrypt 自動アップグレード後の形式</li>
     * </ul>
     *
     * @return BCrypt と SHA-256 の両方をサポートする {@link DelegatingPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("sha256", new LegacySha256PasswordEncoder());
        return new DelegatingPasswordEncoder("bcrypt", encoders);
    }

    /**
     * DAO ベースの認証プロバイダーを生成する。
     *
     * <p>{@link CustomUserDetailsService} をユーザー情報の取得源として設定し、
     * {@link #passwordEncoder()} で生成したエンコーダーでパスワード照合を行う。
     * また、{@code UserDetailsPasswordService} を設定することで、
     * レガシー SHA-256 パスワードでのログイン成功時に BCrypt へ自動アップグレードする。</p>
     *
     * @return 構成済みの {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsPasswordService(customUserDetailsService);
        return provider;
    }

    /**
     * ログイン成功時のカートマージハンドラーの Bean を生成する。
     *
     * <p>未ログイン状態でカートに追加された商品を、ログイン後にユーザーの
     * 永続カートへマージする {@link CartMergeSuccessHandler} を返す。</p>
     *
     * @return カートマージ機能付きの {@link AuthenticationSuccessHandler}
     * @see CartMergeSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler cartMergeSuccessHandler() {
        return new CartMergeSuccessHandler(cartService);
    }

    /**
     * HTTP セキュリティフィルターチェーンを構築する。
     *
     * <p>以下のセキュリティ機能を順番に構成する:</p>
     * <ol>
     *   <li>認証プロバイダーの設定（DAO 認証 + パスワード自動アップグレード）</li>
     *   <li>URL パターン別の認可ルール設定</li>
     *   <li>フォームログイン設定（カスタムログインページ + カートマージ成功ハンドラー）</li>
     *   <li>ログアウト設定（セッション無効化 + クッキー削除）</li>
     *   <li>セッション管理（セッション固定攻撃防止 + 同時セッション制限）</li>
     *   <li>CSRF 保護（Spring Security デフォルト）</li>
     *   <li>セキュリティヘッダー（CSP, X-Frame-Options, XSS Protection, HSTS）</li>
     * </ol>
     *
     * @param http Spring Security の {@link HttpSecurity} ビルダー
     * @return 構成済みの {@link SecurityFilterChain}
     * @throws Exception セキュリティ構成中にエラーが発生した場合
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(
                    "/account/**", "/orders/**", "/checkout/**",
                    "/cart/coupon", "/points/**"
                ).hasAnyRole("USER", "ADMIN")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(cartMergeSuccessHandler())
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
            )
            .csrf(Customizer.withDefaults())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "img-src 'self' data:"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts ->
                    hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            );
        return http.build();
    }
}
