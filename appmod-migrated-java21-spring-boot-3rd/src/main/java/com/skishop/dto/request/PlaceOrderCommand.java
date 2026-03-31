package com.skishop.dto.request;

/**
 * 注文確定に必要なパラメータを集約するコマンドオブジェクト。
 *
 * <p>{@link com.skishop.service.CheckoutService#placeOrder} の
 * 引数を 5 つから 1 つに集約し、可読性と保守性を向上させる。</p>
 *
 * @param cartId      チェックアウト対象のカート ID
 * @param couponCode  適用するクーポンコード（未使用の場合は {@code null}）
 * @param usePoints   使用するポイント数（0 の場合はポイント使用なし）
 * @param paymentInfo 支払い情報
 * @param userId      ログイン中のユーザー ID
 */
public record PlaceOrderCommand(
        String cartId,
        String couponCode,
        int usePoints,
        PaymentInfo paymentInfo,
        String userId
) {}
