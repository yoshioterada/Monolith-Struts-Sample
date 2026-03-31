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
 * SkiShop EC サイトにおける注文明細を表す JPA エンティティ。
 *
 * <p>{@code order_items} テーブルにマッピングされ、注文に含まれる個々の商品の
 * 商品 ID・商品名・SKU・単価・数量・小計を保持する。注文確定時にカート内の
 * 商品情報がスナップショットとしてコピーされるため、商品マスタの変更に影響されない。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — この明細が属する注文（N:1、{@code order_id} で結合）</li>
 *   <li>{@link Product} — 注文された商品（{@code product_id} で参照）</li>
 *   <li>{@link Return} — この明細に対する返品（{@code order_item_id} で参照）</li>
 * </ul>
 *
 * @see Order
 * @see Product
 * @see Return
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, length = 20)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
