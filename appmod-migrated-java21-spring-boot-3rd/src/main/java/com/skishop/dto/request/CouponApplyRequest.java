package com.skishop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * クーポン適用リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.CartController#applyCoupon CartController#applyCoupon}
 * エンドポイント（{@code POST /cart/coupon}）で受け取るリクエストボディ。
 * カートに対してクーポンコードを適用するために使用する。
 * Struts 移行元: {@code CouponForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code code} — 必須（空白不可）、最大 50 文字</li>
 * </ul>
 *
 * @param code 適用するクーポンコード（例: {@code "WINTER2024"}）
 * @see com.skishop.controller.CartController
 */
public record CouponApplyRequest(
    @NotBlank(message = "{validation.couponCode.required}")
    @Size(max = 50)
    String code
) {}
