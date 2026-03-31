package com.skishop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注文確定（チェックアウト）リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.CheckoutController#placeOrder CheckoutController#placeOrder}
 * エンドポイント（{@code POST /checkout}）で受け取るリクエストボディ。
 * カート情報・クーポンコード・決済情報・ポイント利用を一括で送信する。
 * Struts 移行元: {@code CheckoutForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code cartId} — 任意（セッションから取得される場合あり）</li>
 *   <li>{@code couponCode} — 任意（クーポン未適用時は {@code null}）</li>
 *   <li>{@code paymentMethod} — 必須（空白不可）</li>
 *   <li>{@code cardNumber} — 任意（クレジットカード決済時のみ使用）</li>
 *   <li>{@code cardExpMonth} — 任意（クレジットカード有効期限・月）</li>
 *   <li>{@code cardExpYear} — 任意（クレジットカード有効期限・年）</li>
 *   <li>{@code cardCvv} — 任意（クレジットカードセキュリティコード）</li>
 *   <li>{@code billingZip} — 任意（請求先郵便番号）</li>
 *   <li>{@code usePoints} — 0 以上の整数</li>
 * </ul>
 *
 * @param cartId       カート ID（セッション管理用）
 * @param couponCode   適用するクーポンコード（任意）
 * @param paymentMethod 支払い方法（例: {@code "credit_card"}, {@code "bank_transfer"}）
 * @param cardNumber   クレジットカード番号（カード決済時のみ）
 * @param cardExpMonth クレジットカード有効期限の月（例: {@code "01"}〜{@code "12"}）
 * @param cardExpYear  クレジットカード有効期限の年（例: {@code "2026"}）
 * @param cardCvv      クレジットカードのセキュリティコード（CVV/CVC）
 * @param billingZip   請求先住所の郵便番号
 * @param usePoints    利用するポイント数（0 以上）
 * @see com.skishop.controller.CheckoutController
 * @see PaymentInfo
 */
public record CheckoutRequest(
    String cartId,
    String couponCode,

    @NotBlank(message = "{validation.paymentMethod.required}")
    String paymentMethod,

    @Size(max = 19)
    String cardNumber,
    @Size(max = 2)
    String cardExpMonth,
    @Size(max = 4)
    String cardExpYear,
    @Size(max = 4)
    String cardCvv,
    @Size(max = 10)
    String billingZip,

    @Min(value = 0, message = "{validation.usePoints.min}")
    int usePoints
) {}
