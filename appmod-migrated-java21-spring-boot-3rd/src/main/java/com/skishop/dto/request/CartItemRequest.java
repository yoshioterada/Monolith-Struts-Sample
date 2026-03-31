package com.skishop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * カートへの商品追加・数量更新リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.CartController CartController} の以下のエンドポイントで使用される:</p>
 * <ul>
 *   <li>{@code POST /cart/items} — カートに商品を追加</li>
 *   <li>{@code PUT /cart/items/{itemId}} — カート内商品の数量を更新</li>
 * </ul>
 *
 * <p>Struts 移行元: {@code AddCartForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code productId} — 必須（空白不可）</li>
 *   <li>{@code quantity} — 正の整数（1 以上）</li>
 * </ul>
 *
 * @param productId 追加・更新対象の商品 ID（UUID 形式）
 * @param quantity  数量（1 以上の正の整数）
 * @see com.skishop.controller.CartController
 */
public record CartItemRequest(
    @NotBlank(message = "{validation.productId.required}")
    String productId,

    @Positive(message = "{validation.quantity.positive}")
    int quantity
) {}
