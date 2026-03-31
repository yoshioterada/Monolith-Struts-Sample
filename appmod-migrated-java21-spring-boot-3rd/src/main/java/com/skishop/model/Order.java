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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SkiShop EC サイトにおける注文を表す JPA エンティティ。
 *
 * <p>{@code orders} テーブルにマッピングされ、注文番号・ユーザー ID・注文ステータス・
 * 決済ステータス・小計・税額・送料・割引額・合計金額・使用クーポンコード・
 * 使用ポイント数を保持する。{@code CheckoutService} の 11 ステップの注文確定フローにより、
 * 単一の {@code @Transactional} 内で原子的に作成される。</p>
 *
 * <p>注文ステータス遷移: {@code PENDING} → {@code CONFIRMED} → {@code SHIPPED} →
 * {@code DELIVERED} / {@code CANCELLED} / {@code RETURNED}</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link OrderItem} — 注文明細（1:N、{@code CASCADE ALL} + {@code orphanRemoval}）</li>
 *   <li>{@link User} — 注文したユーザー（N:1、{@code user_id} で参照）</li>
 *   <li>{@link Payment} — この注文に対する支払い情報（{@code order_id} で参照）</li>
 *   <li>{@link OrderShipping} — 配送先情報（{@code order_id} で参照）</li>
 *   <li>{@link Shipment} — 出荷情報（{@code order_id} で参照）</li>
 *   <li>{@link Return} — 返品情報（{@code order_id} で参照）</li>
 *   <li>{@link CouponUsage} — クーポン使用記録（{@code order_id} で参照）</li>
 * </ul>
 *
 * @see OrderItem
 * @see User
 * @see Payment
 * @see OrderShipping
 * @see Shipment
 * @see Return
 * @see CouponUsage
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "used_points", nullable = false)
    private int usedPoints;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();
}
