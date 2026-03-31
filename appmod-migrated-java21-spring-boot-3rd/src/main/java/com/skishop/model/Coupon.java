package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるクーポンを表す JPA エンティティ。
 *
 * <p>{@code coupons} テーブルにマッピングされ、割引クーポンのコード・種別・
 * 割引値・割引タイプ（定額/割合）・最低注文金額・最大割引額・使用上限回数・
 * 使用済み回数・有効期限などを保持する。チェックアウト時にクーポンコードを
 * 適用することで注文金額の割引を行う。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Campaign} — このクーポンが属するキャンペーン（N:1、{@code campaign_id} で参照）</li>
 *   <li>{@link CouponUsage} — クーポンの使用履歴（1:N）</li>
 *   <li>{@link Order} — クーポンが適用された注文（{@code coupon_code} で参照）</li>
 * </ul>
 *
 * @see Campaign
 * @see CouponUsage
 * @see Order
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "campaign_id", length = 36)
    private String campaignId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "coupon_type", nullable = false, length = 20)
    private String couponType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "minimum_amount", precision = 10, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
