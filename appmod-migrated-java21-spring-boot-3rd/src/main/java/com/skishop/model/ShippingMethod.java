package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SkiShop EC サイトにおける配送方法マスタを表す JPA エンティティ。
 *
 * <p>{@code shipping_methods} テーブルにマッピングされ、配送方法のコード（UNIQUE）・
 * 名称・送料・有効フラグ・表示順序を保持する。チェックアウト画面での配送方法選択や
 * 送料計算に利用される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link OrderShipping} — 注文時に選択された配送方法（{@code shipping_method_code} で参照）</li>
 * </ul>
 *
 * @see OrderShipping
 */
@Entity
@Table(name = "shipping_methods")
@Getter
@Setter
@NoArgsConstructor
public class ShippingMethod {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
