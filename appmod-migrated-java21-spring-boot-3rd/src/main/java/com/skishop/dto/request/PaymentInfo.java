package com.skishop.dto.request;

/**
 * 決済情報を保持するデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.CheckoutController CheckoutController} で
 * {@link CheckoutRequest} から決済関連フィールドを抽出して生成され、
 * {@link com.skishop.service.CheckoutService CheckoutService} →
 * {@link com.skishop.service.PaymentService PaymentService} へ受け渡される。
 * Struts 移行元: {@code CheckoutForm} の決済関連フィールド部分に相当</p>
 *
 * <p>本レコードにはバリデーションアノテーションは付与されていない。
 * 入力バリデーションは {@link CheckoutRequest} 側で実施される。</p>
 *
 * @param method       支払い方法（例: {@code "credit_card"}, {@code "bank_transfer"}）
 * @param cardNumber   クレジットカード番号（カード決済時のみ使用）
 * @param cardExpMonth クレジットカード有効期限の月（例: {@code "01"}〜{@code "12"}）
 * @param cardExpYear  クレジットカード有効期限の年（例: {@code "2026"}）
 * @param cardCvv      クレジットカードのセキュリティコード（CVV/CVC）
 * @param billingZip   請求先住所の郵便番号
 * @see CheckoutRequest
 * @see com.skishop.service.PaymentService
 */
public record PaymentInfo(
        String method,
        String cardNumber,
        String cardExpMonth,
        String cardExpYear,
        String cardCvv,
        String billingZip
) {}
