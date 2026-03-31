package com.skishop.service;

import com.skishop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 用ユーザー認証・パスワードアップグレードサービス。
 *
 * <p>Spring Security の {@link UserDetailsService} と
 * {@link org.springframework.security.core.userdetails.UserDetailsPasswordService} を実装し、
 * ユーザーテーブルをバックエンドとした認証機能を提供する。</p>
 *
 * <h3>パスワードハッシュ移行（SHA-256 → BCrypt）</h3>
 * <p>移行元システム（Struts 1.x）では SHA-256 + Salt でパスワードをハッシュしていた。
 * Spring Boot 移行後は {@code DelegatingPasswordEncoder} により以下の形式を自動判別する:</p>
 * <ul>
 *   <li>{@code {sha256}hash$salt} — 移行直後の旧形式（Flyway V2 で付与）</li>
 *   <li>{@code {bcrypt}hash} — ログイン成功後に自動アップグレードされた新形式</li>
 * </ul>
 *
 * <p>{@link #updatePassword} は Spring Security が認証成功時に自動的に呼び出し、
 * SHA-256 形式のパスワードを BCrypt に透過的にアップグレードする。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link UserRepository} — ユーザーエンティティの参照・パスワード更新</li>
 * </ul>
 *
 * @see com.skishop.config.SecurityConfig
 * @see AuthService
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService,
        org.springframework.security.core.userdetails.UserDetailsPasswordService {

    private final UserRepository userRepository;

    /**
     * メールアドレスでユーザーを検索し、Spring Security の {@link UserDetails} に変換する。
     *
     * <p>Spring Security の認証フローから自動的に呼び出される。
     * ユーザーのメールアドレス、パスワードハッシュ、ロール、ロック状態を
     * {@link UserDetails} オブジェクトに変換して返す。</p>
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param email ログインフォームで入力されたメールアドレス
     * @return Spring Security 認証用の {@link UserDetails}
     * @throws UsernameNotFoundException 指定メールアドレスのユーザーが存在しない場合
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())  // {sha256}hash$salt or {bcrypt}hash
                .roles(user.getRole())
                .accountLocked("LOCKED".equals(user.getStatus()))
                .build();
    }

    /**
     * 認証成功時にパスワードハッシュを自動アップグレードする。
     *
     * <p>Spring Security が認証成功後に自動的に呼び出す。
     * SHA-256 形式（{@code {sha256}hash$salt}）で保存されているパスワードを
     * BCrypt 形式（{@code {bcrypt}hash}）に透過的にアップグレードする。</p>
     *
     * <p>この仕組みにより、ユーザーが次回ログインするたびに
     * パスワードハッシュが段階的に BCrypt に移行される。</p>
     *
     * @param userDetails     認証済みのユーザー情報
     * @param newEncodedPassword Spring Security が生成した新しい BCrypt ハッシュ
     * @return 新しいパスワードハッシュを持つ {@link UserDetails}
     */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails userDetails, String newEncodedPassword) {
        // Called by Spring Security after BCrypt upgrade on successful login
        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            user.setPasswordHash(newEncodedPassword);
            userRepository.save(user);
        });
        return User.withUserDetails(userDetails).password(newEncodedPassword).build();
    }
}
