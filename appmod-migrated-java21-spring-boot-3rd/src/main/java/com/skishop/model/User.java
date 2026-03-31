package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトのユーザーを表す JPA エンティティ。
 *
 * <p>{@code users} テーブルにマッピングされ、認証情報（メール・パスワードハッシュ・ソルト）、
 * プロフィール情報（ユーザー名）、アカウント状態（{@code status}）、
 * およびロール（{@code USER} / {@code ADMIN}）を保持する。</p>
 *
 * <p>パスワードハッシュは {@code DelegatingPasswordEncoder} により管理され、
 * レガシーの SHA-256 形式（{@code {sha256}<hash>$<salt>}）から BCrypt 形式
 * （{@code {bcrypt}<hash>}）へのログイン時自動アップグレードに対応する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Address} — ユーザーの配送先住所（1:N、{@code user_id} で参照）</li>
 *   <li>{@link Order} — ユーザーの注文履歴（1:N、{@code user_id} で参照）</li>
 *   <li>{@link Cart} — ユーザーのショッピングカート（1:1、{@code user_id} で参照）</li>
 *   <li>{@link PointAccount} — ユーザーのポイント口座（1:1、{@code user_id} で参照）</li>
 *   <li>{@link SecurityLog} — セキュリティ監査ログ（1:N、{@code user_id} で参照）</li>
 *   <li>{@link PasswordResetToken} — パスワードリセットトークン（1:N、{@code user_id} で参照）</li>
 * </ul>
 *
 * @see Address
 * @see Order
 * @see Cart
 * @see PointAccount
 * @see SecurityLog
 * @see PasswordResetToken
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "salt", nullable = false, length = 255)
    private String salt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
