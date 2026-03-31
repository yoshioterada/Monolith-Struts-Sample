package com.skishop.dto.response;

import com.skishop.model.Cart;
import com.skishop.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * チェックアウト画面に表示するためのサマリー情報。
 *
 * @param cart        カートエンティティ
 * @param items       カート内アイテム一覧
 * @param subtotal    小計金額（税抜き）
 * @param shippingFee 配送料
 * @param tax         消費税額
 */
public record CheckoutSummary(
        Cart cart,
        List<CartItem> items,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax
) {}
