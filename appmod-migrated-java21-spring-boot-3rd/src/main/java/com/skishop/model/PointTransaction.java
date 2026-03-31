package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるポイント取引履歴を表す JPA エンティティ。
 *
 * <p>{@code point_transactions} テーブルにマッピングされ、ポイントの獲得・使用・失効
 * などの個々の取引を記録する。取引タイプ（{@code type}）・ポイント数（{@code amount}）・
 * 参照 ID（注文番号等）・説明・有効期限・失効フラグを保持する。</p>
 *
 * <p>取引タイプ: {@code EARN}（獲得）/ {@code REDEEM}（使用）/ {@code EXPIRE}（失効）/
 * {@code ADJUST}（管理者調整）</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link User} — ポイント取引の対象ユーザー（N:1、{@code user_id} で参照）</li>
 *   <li>{@link PointAccount} — ユーザーのポイント口座（残高の増減元）</li>
 *   <li>{@link Order} — ポイント獲得・使用のトリガーとなった注文（{@code reference_id} で間接参照）</li>
 * </ul>
 *
 * @see User
 * @see PointAccount
 * @see Order
 */
@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PointTransaction {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "reference_id", length = 50)
    private String referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_expired", nullable = false)
    private boolean expired;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
