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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおける返品を表す JPA エンティティ。
 *
 * <p>{@code returns} テーブルにマッピングされ、返品対象の注文 ID・注文明細 ID・
 * 返品理由・返品数量・返金額・返品ステータスを保持する。
 * 注文明細単位で返品を管理し、部分返品にも対応する。</p>
 *
 * <p>返品ステータス遷移: {@code REQUESTED} → {@code APPROVED} → {@code REFUNDED} /
 * {@code REJECTED}</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — 返品対象の注文（N:1、{@code order_id} で参照）</li>
 *   <li>{@link OrderItem} — 返品対象の注文明細（N:1、{@code order_item_id} で参照）</li>
 * </ul>
 *
 * @see Order
 * @see OrderItem
 */
@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
public class Return {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_item_id", nullable = false, length = 36)
    private String orderItemId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "status", length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
