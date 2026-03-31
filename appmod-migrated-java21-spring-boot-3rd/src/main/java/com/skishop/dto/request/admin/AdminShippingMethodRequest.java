package com.skishop.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 管理者による配送方法の作成・更新リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AdminShippingMethodController AdminShippingMethodController} の
 * 以下のエンドポイントで使用される:</p>
 * <ul>
 *   <li>{@code POST /admin/shipping-methods} — 配送方法新規作成</li>
 *   <li>{@code PUT /admin/shipping-methods/{id}} — 配送方法更新</li>
 * </ul>
 *
 * <p>Struts 移行元: 管理画面用の配送方法設定 ActionForm に相当</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code id} — 任意（新規作成時は {@code null}）</li>
 *   <li>{@code code} — 必須、最大 50 文字</li>
 *   <li>{@code name} — 必須、最大 100 文字</li>
 *   <li>{@code fee} — 必須、正の数値</li>
 *   <li>{@code active} — 有効・無効フラグ</li>
 *   <li>{@code sortOrder} — 0 以上の整数</li>
 * </ul>
 *
 * @param id        配送方法 ID（UUID 形式、新規作成時は {@code null}）
 * @param code      配送方法コード（例: {@code "standard"}, {@code "express"}、一意識別子として使用）
 * @param name      配送方法の表示名（例: {@code "通常配送"}, {@code "速達便"}）
 * @param fee       配送料金
 * @param active    配送方法の有効・無効フラグ
 * @param sortOrder 表示順序（0 以上、昇順で表示）
 * @see com.skishop.controller.AdminShippingMethodController
 */
public record AdminShippingMethodRequest(
    String id,

    @NotBlank(message = "{validation.code.required}")
    @Size(max = 50)
    String code,

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 100)
    String name,

    @NotNull(message = "{validation.fee.required}")
    @Positive(message = "{validation.fee.positive}")
    BigDecimal fee,

    boolean active,

    @Min(value = 0, message = "{validation.sortOrder.min}")
    int sortOrder
) {}
