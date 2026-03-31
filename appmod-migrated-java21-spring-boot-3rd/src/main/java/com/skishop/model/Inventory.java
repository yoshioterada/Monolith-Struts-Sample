package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SkiShop EC サイトにおける商品在庫を表す JPA エンティティ。
 *
 * <p>{@code inventory} テーブルにマッピングされ、商品ごとの在庫数量（{@code quantity}）・
 * 予約済み数量（{@code reserved_quantity}）・在庫ステータスを保持する。
 * 商品と 1:1 の関係を持ち、{@code product_id} に UNIQUE 制約が設定されている。</p>
 *
 * <p>チェックアウト処理では在庫確認（{@code checkStock}）→ 在庫減算（{@code deductStock}）
 * の順で在庫操作が行われ、注文確定トランザクション内で原子的に処理される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Product} — この在庫に対応する商品（1:1、{@code product_id} で参照）</li>
 * </ul>
 *
 * @see Product
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, unique = true, length = 20)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
