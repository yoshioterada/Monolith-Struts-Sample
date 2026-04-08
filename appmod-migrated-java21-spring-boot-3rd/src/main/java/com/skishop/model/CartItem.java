package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SkiShop EC サイトにおけるカート内商品明細を表す JPA エンティティ。
 *
 * <p>{@code cart_items} テーブルにマッピングされ、カートに追加された個々の商品の
 * 商品 ID・数量・単価を保持する。{@link Cart} との親子関係（N:1）により、
 * カート削除時には明細も連鎖的に削除される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Cart} — この明細が属するカート（N:1、{@code cart_id} で結合）</li>
 *   <li>{@link Product} — カートに追加された商品（{@code product_id} で参照）</li>
 * </ul>
 *
 * @see Cart
 * @see Product
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false, length = 20)
    private String productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
