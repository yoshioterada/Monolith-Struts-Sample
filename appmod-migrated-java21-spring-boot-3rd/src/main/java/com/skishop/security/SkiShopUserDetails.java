package com.skishop.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * SkiShop 固有の {@link org.springframework.security.core.userdetails.UserDetails} 実装。
 *
 * <p>Spring Security 標準の {@link User} を拡張し、DB 上の {@code users.id}（UUID）を
 * 追加フィールドとして保持する。Controller や Service で
 * {@code @AuthenticationPrincipal SkiShopUserDetails user} として注入し、
 * {@code user.getUserId()} で DB の主キーを取得できる。</p>
 *
 * <p>{@code getUsername()} はログイン識別子であるメールアドレスを返す（Spring Security 標準動作）。
 * DB の FK（{@code orders.user_id}, {@code carts.user_id} 等）には {@code getUserId()} を使用すること。</p>
 */
public class SkiShopUserDetails extends User {

    private final String userId;

    public SkiShopUserDetails(String userId, String email, String password,
                               boolean accountNonLocked,
                               Collection<? extends GrantedAuthority> authorities) {
        super(email, password, true, true, true, accountNonLocked, authorities);
        this.userId = userId;
    }

    /**
     * DB 上の {@code users.id}（UUID）を返す。
     *
     * @return ユーザーの主キー ID
     */
    public String getUserId() {
        return userId;
    }
}
