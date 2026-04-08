package com.skishop.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * テスト用に {@link SkiShopUserDetails} を生成するユーティリティ。
 */
public final class TestUserFactory {

    private TestUserFactory() {}

    /**
     * 指定された userId とロールで {@link SkiShopUserDetails} を生成し、
     * MockMvc の RequestPostProcessor として返す。
     */
    public static RequestPostProcessor skiShopUser(String userId, String role) {
        var user = new SkiShopUserDetails(
                userId,
                userId + "@example.com",
                "password",
                true,
                AuthorityUtils.createAuthorityList("ROLE_" + role)
        );
        return SecurityMockMvcRequestPostProcessors.user(user);
    }

    /**
     * デフォルトの USER ロールで生成する。
     */
    public static RequestPostProcessor skiShopUser(String userId) {
        return skiShopUser(userId, "USER");
    }
}
