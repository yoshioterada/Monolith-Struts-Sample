package com.skishop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 消費税計算サービス。
 *
 * <p>注文金額に対する消費税を計算する。税率は設定ファイル
 * {@code skishop.tax.rate} から取得され、デフォルトは 10%（0.10）。</p>
 *
 * <p>チェックアウト処理（{@link CheckoutService}）から呼び出され、
 * クーポン適用・ポイント使用後の課税対象金額に対して税額を算出する。</p>
 *
 * @see CheckoutService#placeOrder
 */
@Service
public class TaxService {

    private final BigDecimal taxRate;

    /**
     * TaxService を構築する。
     *
     * @param taxRate 消費税率（設定ファイルの {@code skishop.tax.rate} から注入。デフォルト: {@code 0.10}）
     */
    public TaxService(@Value("${skishop.tax.rate:0.10}") String taxRate) {
        this.taxRate = new BigDecimal(taxRate);
    }

    /**
     * 指定金額に対する消費税を計算する。
     *
     * <p>計算結果は小数点第 2 位で四捨五入（{@link RoundingMode#HALF_UP}）される。
     * 金額が {@code null} の場合は {@link BigDecimal#ZERO} を返す。</p>
     *
     * {@code 例: amount=1000, taxRate=0.10 → 戻り値=100.00}
     *
     * @param amount 課税対象金額（税抜き金額）
     * @return 消費税額（{@code null} の場合は {@link BigDecimal#ZERO}）
     */
    public BigDecimal calculateTax(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }
}
