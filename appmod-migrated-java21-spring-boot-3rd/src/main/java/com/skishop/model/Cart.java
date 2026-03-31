package com.skishop.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SkiShop EC サイトにおけるショッピングカートを表す JPA エンティティ。
 *
 * <p>{@code carts} テーブルにマッピングされ、ユーザー ID またはセッション ID で
 * カートの所有者を識別する。未ログイン状態ではセッション ID で管理し、
 * ログイン成功時に {@code CartMergeSuccessHandler} により
 * セッションカートをユーザーカートにマージする。</p>
 *
 * <p>カートのステータス（{@code status}）により、アクティブ・期限切れ・
 * チェックアウト済みなどの状態を管理する。{@code expires_at} で
 * カートの有効期限を制御する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link CartItem} — カート内の商品明細（1:N、{@code CASCADE ALL} + {@code orphanRemoval}）</li>
 *   <li>{@link User} — カートを所有するユーザー（N:1、{@code user_id} で参照、未ログイン時は null）</li>
 *   <li>{@link Payment} — カートに紐づく仮決済情報（{@code cart_id} で参照）</li>
 * </ul>
 *
 * @see CartItem
 * @see User
 * @see Payment
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<CartItem> items = new ArrayList<>();
}
