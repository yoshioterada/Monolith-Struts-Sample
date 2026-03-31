package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるパスワードリセットトークンを表す JPA エンティティ。
 *
 * <p>{@code password_reset_tokens} テーブルにマッピングされ、パスワードリセット要求時に
 * 生成される一意のトークン・対象ユーザー ID・有効期限・使用日時を保持する。
 * トークンは一度使用されると {@code used_at} が設定され、再利用は不可。
 * 有効期限（{@code expires_at}）を超過したトークンも無効として扱う。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link User} — パスワードリセット対象のユーザー（N:1、{@code user_id} で参照）</li>
 * </ul>
 *
 * @see User
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
