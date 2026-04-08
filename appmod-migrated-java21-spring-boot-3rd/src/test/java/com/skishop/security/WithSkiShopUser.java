package com.skishop.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * テスト用に {@link SkiShopUserDetails} を SecurityContext に設定するカスタムアノテーション。
 *
 * <p>{@code @WithMockUser} の代わりに使用することで、
 * {@code @AuthenticationPrincipal SkiShopUserDetails} が正しく注入されるようになる。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithSkiShopUser.Factory.class)
public @interface WithSkiShopUser {

    String userId() default "user-id-1";

    String email() default "test@example.com";

    String role() default "USER";

    class Factory implements WithSecurityContextFactory<WithSkiShopUser> {
        @Override
        public SecurityContext createSecurityContext(WithSkiShopUser annotation) {
            var authorities = AuthorityUtils.createAuthorityList("ROLE_" + annotation.role());
            var principal = new SkiShopUserDetails(
                    annotation.userId(),
                    annotation.email(),
                    "password",
                    true,
                    authorities
            );
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, "password", authorities);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            return context;
        }
    }
}
