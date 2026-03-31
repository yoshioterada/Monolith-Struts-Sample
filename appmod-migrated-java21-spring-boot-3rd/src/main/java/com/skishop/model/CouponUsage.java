package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるクーポン使用履歴を表す JPA エンティティ。
 *
 * <p>{@code coupon_usage} テーブルにマッピングされ、どのユーザーがどの注文で
 * どのクーポンを使用し、いくらの割引が適用されたかを記録する。
 * クーポンの使用回数上限チェックや、ユーザーごとの重複使用防止に利用される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Coupon} — 使用されたクーポン（N:1、{@code coupon_id} で参照）</li>
 *   <li>{@link User} — クーポンを使用したユーザー（N:1、{@code user_id} で参照）</li>
 *   <li>{@link Order} — クーポンが適用された注文（N:1、{@code order_id} で参照）</li>
 * </ul>
 *
 * @see Coupon
 * @see User
 * @see Order
 */
@Entity
@Table(name = "coupon_usage")
@Getter
@Setter
@NoArgsConstructor
public class CouponUsage {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "coupon_id", nullable = false, length = 36)
    private String couponId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;
}
