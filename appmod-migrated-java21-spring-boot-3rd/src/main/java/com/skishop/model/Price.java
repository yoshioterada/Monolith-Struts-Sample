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
 * SkiShop EC サイトにおける商品価格を表す JPA エンティティ。
 *
 * <p>{@code prices} テーブルにマッピングされ、商品の通常価格（{@code regular_price}）・
 * セール価格（{@code sale_price}）・通貨コード・セール期間（開始日・終了日）を保持する。
 * セール価格はセール期間中のみ有効であり、期間外は通常価格が適用される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Product} — この価格が設定されている商品（N:1、{@code product_id} で参照）</li>
 * </ul>
 *
 * @see Product
 */
@Entity
@Table(name = "prices")
@Getter
@Setter
@NoArgsConstructor
public class Price {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 20)
    private String productId;

    @Column(name = "regular_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal regularPrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "sale_start_date")
    private LocalDateTime saleStartDate;

    @Column(name = "sale_end_date")
    private LocalDateTime saleEndDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
