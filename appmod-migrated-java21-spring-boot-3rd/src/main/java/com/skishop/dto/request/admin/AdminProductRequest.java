package com.skishop.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 管理者による商品の作成・更新リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AdminProductController AdminProductController} の
 * 以下のエンドポイントで使用される:</p>
 * <ul>
 *   <li>{@code POST /admin/products} — 商品新規作成</li>
 *   <li>{@code PUT /admin/products/{id}} — 商品更新</li>
 * </ul>
 *
 * <p>Struts 移行元: 管理画面用の商品登録 ActionForm に相当</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code id} — 任意（新規作成時は {@code null}）</li>
 *   <li>{@code name} — 必須、最大 200 文字</li>
 *   <li>{@code brand} — 任意、最大 100 文字</li>
 *   <li>{@code description} — 任意（商品の詳細説明）</li>
 *   <li>{@code categoryId} — 必須（空白不可）</li>
 *   <li>{@code regularPrice} — 必須、正の数値</li>
 *   <li>{@code salePrice} — 任意（セール価格）</li>
 *   <li>{@code status} — 必須（空白不可）</li>
 *   <li>{@code inventoryQty} — 在庫数量</li>
 * </ul>
 *
 * @param id           商品 ID（UUID 形式、新規作成時は {@code null}）
 * @param name         商品名
 * @param brand        ブランド名（任意）
 * @param description  商品説明文（任意、HTML は含めないこと）
 * @param categoryId   所属カテゴリ ID
 * @param regularPrice 通常価格（税抜き）
 * @param salePrice    セール価格（任意、{@code null} の場合はセール対象外）
 * @param status       商品ステータス（例: {@code "active"}, {@code "inactive"}, {@code "discontinued"}）
 * @param inventoryQty 在庫数量
 * @see com.skishop.controller.AdminProductController
 */
public record AdminProductRequest(
    String id,

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 200)
    String name,

    @Size(max = 100)
    String brand,

    String description,

    @NotBlank(message = "{validation.categoryId.required}")
    String categoryId,

    @NotNull(message = "{validation.regularPrice.required}")
    @Positive(message = "{validation.regularPrice.positive}")
    BigDecimal regularPrice,

    BigDecimal salePrice,

    @NotBlank(message = "{validation.status.required}")
    String status,

    int inventoryQty
) {}
