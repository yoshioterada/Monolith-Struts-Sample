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

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるスキー用品の商品を表す JPA エンティティ。
 *
 * <p>{@code products} テーブルにマッピングされ、商品名・ブランド・説明・カテゴリ ID・
 * SKU（在庫管理単位）・ステータス（{@code ACTIVE} / {@code INACTIVE} / {@code DELETED}）
 * を保持する。商品 ID は最大 20 文字の文字列型で、既存データとの互換性を維持する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Category} — この商品が属するカテゴリ（N:1、{@code category_id} で参照）</li>
 *   <li>{@link Price} — この商品の価格情報（1:N、{@code product_id} で参照）</li>
 *   <li>{@link Inventory} — この商品の在庫情報（1:1、{@code product_id} で参照）</li>
 *   <li>{@link CartItem} — カート内でのこの商品への参照</li>
 *   <li>{@link OrderItem} — 注文明細でのこの商品への参照</li>
 * </ul>
 *
 * @see Category
 * @see Price
 * @see Inventory
 * @see CartItem
 * @see OrderItem
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category_id", length = 36)
    private String categoryId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
