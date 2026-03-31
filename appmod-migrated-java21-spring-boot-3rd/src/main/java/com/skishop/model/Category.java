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

/**
 * SkiShop EC サイトにおける商品カテゴリを表す JPA エンティティ。
 *
 * <p>{@code categories} テーブルにマッピングされ、スキー用品の分類（スキー板・
 * ブーツ・ウェア・アクセサリー等）を階層構造で管理する。
 * {@code parent_id} による自己参照（N:1）で親子カテゴリのツリー構造を構築する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Category} — 親カテゴリ（N:1、自己参照、{@code parent_id} で結合）</li>
 *   <li>{@link Product} — このカテゴリに属する商品（1:N、{@code category_id} で参照）</li>
 * </ul>
 *
 * @see Product
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
}
