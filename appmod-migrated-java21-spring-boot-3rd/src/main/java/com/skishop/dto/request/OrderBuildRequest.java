package com.skishop.dto.request;

import java.math.BigDecimal;

/**
 * 注文エンティティ構築用のリクエスト DTO。
 *
 * <p>{@link com.skishop.service.OrderService#buildOrder} に渡すパラメータを
 * 1 つのオブジェクトにまとめることで、メソッドシグネチャの可読性を向上させる。</p>
 *
 * @param orderId        注文 ID（UUID）
 * @param orderNumber    注文番号（{@code ORD-} プレフィックス + タイムスタンプ）
 * @param userId         ユーザー ID
 * @param subtotal       小計金額
 * @param tax            消費税額
 * @param shippingFee    配送料
 * @param discountAmount 割引額（クーポン適用分）
 * @param totalAmount    合計金額（税込み・送料込み・割引後）
 * @param couponCode     使用されたクーポンコード（未使用の場合は {@code null}）
 * @param usedPoints     使用されたポイント数
 */
public record OrderBuildRequest(
        String orderId,
        String orderNumber,
        String userId,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String couponCode,
        int usedPoints
) {}
