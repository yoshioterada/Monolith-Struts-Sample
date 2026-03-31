package com.skishop.service;

import com.skishop.model.OrderShipping;
import com.skishop.repository.OrderShippingRepository;
import com.skishop.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 配送料計算・配送情報管理サービス。
 *
 * <p>注文金額に基づく配送料の計算と、注文に紐づく配送情報の永続化を担当する。
 * 金額が {@value #FREE_THRESHOLD} 円以上の場合は送料無料となり、
 * それ未満の場合は一律 {@value #DEFAULT_FEE} 円の配送料が適用される。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link ShippingMethodRepository} — 配送方法マスターの参照</li>
 *   <li>{@link OrderShippingRepository} — 注文配送情報の永続化</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see OrderShippingRepository
 */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final BigDecimal FREE_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal DEFAULT_FEE = new BigDecimal("800");

    private final ShippingMethodRepository shippingMethodRepository;
    private final OrderShippingRepository orderShippingRepository;

    /**
     * 注文金額に基づいて配送料を計算する。
     *
     * <p>金額が 10,000 円以上の場合は送料無料（{@link BigDecimal#ZERO}）。
     * それ未満の場合は一律 800 円を返す。金額が {@code null} の場合もデフォルト料金を返す。</p>
     *
     * @param amount 注文金額（税抜き・割引後）
     * @return 配送料（送料無料の場合は {@link BigDecimal#ZERO}）
     */
    public BigDecimal calculateShippingFee(BigDecimal amount) {
        if (amount == null) {
            return DEFAULT_FEE;
        }
        if (amount.compareTo(FREE_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return DEFAULT_FEE;
    }

    /**
     * 注文配送情報を保存する。
     *
     * <p>チェックアウト処理の一環として、受取人名・住所・配送方法等を永続化する。
     * 引数が {@code null} の場合は何もしない。</p>
     *
     * @param shipping 保存対象の注文配送情報（{@code null} の場合は無視）
     */
    @Transactional
    public void saveOrderShipping(OrderShipping shipping) {
        if (shipping != null) {
            orderShippingRepository.save(shipping);
        }
    }
}
