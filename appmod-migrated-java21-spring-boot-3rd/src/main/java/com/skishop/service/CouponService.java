package com.skishop.service;

import com.skishop.exception.BusinessException;
import com.skishop.model.Coupon;
import com.skishop.model.CouponUsage;
import com.skishop.repository.CouponRepository;
import com.skishop.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skishop.dto.request.admin.AdminCouponRequest;
import com.skishop.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * クーポン管理サービス。
 *
 * <p>クーポンの検証・割引計算・使用記録・管理者向け CRUD 操作を提供する。
 * クーポンには定率割引（PERCENT）と定額割引の 2 種類があり、
 * それぞれ最大割引額と最低注文金額の制約を設定可能。</p>
 *
 * <p>クーポンの検証では以下をチェックする:</p>
 * <ul>
 *   <li>有効フラグ（{@code active}）</li>
 *   <li>使用回数上限（{@code usageLimit} vs {@code usedCount}）</li>
 *   <li>最低注文金額（{@code minimumAmount}）</li>
 *   <li>有効期限（{@code expiresAt}）</li>
 * </ul>
 *
 * <p>管理者向け操作（作成・更新・削除）は {@code ADMIN} ロールが必要。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link CouponRepository} — クーポンエンティティの永続化</li>
 *   <li>{@link CouponUsageRepository} — クーポン使用履歴の永続化</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see com.skishop.controller.AdminController
 * @see CouponRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * クーポンコードの有効性を検証する。
     *
     * <p>以下の順序でバリデーションを実施する:</p>
     * <ol>
     *   <li>クーポンコードが null/空白の場合は {@code null} を返す（クーポン未使用）</li>
     *   <li>クーポンの存在確認</li>
     *   <li>有効フラグの確認</li>
     *   <li>使用回数上限の確認</li>
     *   <li>最低注文金額の確認</li>
     *   <li>有効期限の確認</li>
     * </ol>
     *
     * @param code     クーポンコード（{@code null} または空白の場合はクーポン未使用とみなす）
     * @param subtotal 注文小計金額（最低注文金額の判定に使用）
     * @return 有効なクーポンエンティティ、またはクーポン未使用の場合は {@code null}
     * @throws BusinessException クーポンが無効、使用上限到達、最低金額未満、期限切れの場合
     */
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        var coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Coupon not found",
                        "redirect:/cart", "error.coupon.notfound"));

        if (!coupon.isActive()) {
            throw new BusinessException("Coupon inactive",
                    "redirect:/cart", "error.coupon.inactive");
        }
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException("Coupon usage limit reached",
                    "redirect:/cart", "error.coupon.limit");
        }
        if (coupon.getMinimumAmount() != null && subtotal.compareTo(coupon.getMinimumAmount()) < 0) {
            throw new BusinessException("Subtotal below minimum",
                    "redirect:/cart", "error.coupon.minimum");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Coupon expired",
                    "redirect:/cart", "error.coupon.expired");
        }
        return coupon;
    }

    /**
     * 有効なクーポンの一覧を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。{@code active = true} のクーポンのみ返す。</p>
     *
     * @return 有効なクーポンのリスト
     */
    @Transactional(readOnly = true)
    public List<Coupon> listActiveCoupons() {
        return couponRepository.findByActiveTrue();
    }

    /**
     * クーポンによる割引額を計算する。
     *
     * <p>割引タイプに応じた計算:</p>
     * <ul>
     *   <li>{@code PERCENT}: {@code amount × discountValue / 100}（小数第 2 位で四捨五入）</li>
     *   <li>その他（定額）: {@code discountValue} をそのまま適用</li>
     * </ul>
     *
     * <p>割引額が {@code maximumDiscount}（最大割引額）を超える場合は上限に制限する。
     * また、割引額が注文金額を超える場合は注文金額に制限する。</p>
     *
     * @param coupon クーポンエンティティ（{@code null} の場合は割引なし）
     * @param amount 割引適用前の注文金額
     * @return 割引額（クーポンが {@code null} の場合は {@link BigDecimal#ZERO}）
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(coupon.getCouponType())) {
            discount = amount.multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }
        if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
            discount = coupon.getMaximumDiscount();
        }
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount;
    }

    /**
     * クーポンを使用済みとしてマークし、使用履歴を記録する。
     *
     * <p>クーポンの使用回数（{@code usedCount}）をインクリメントし、
     * {@link CouponUsage} に使用履歴（ユーザー・注文・割引額・日時）を永続化する。
     * クーポンが {@code null} の場合は何もしない。</p>
     *
     * @param coupon         使用済みにするクーポン（{@code null} の場合は無視）
     * @param userId         使用したユーザーの ID
     * @param orderId        使用対象の注文 ID
     * @param discountAmount 実際に適用された割引額
     */
    @Transactional
    public void markUsed(Coupon coupon, String userId, String orderId, BigDecimal discountAmount) {
        if (coupon == null) {
            return;
        }
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        var usage = new CouponUsage();
        usage.setId(UUID.randomUUID().toString());
        usage.setCouponId(coupon.getId());
        usage.setUserId(userId);
        usage.setOrderId(orderId);
        usage.setDiscountApplied(discountAmount);
        usage.setUsedAt(LocalDateTime.now());
        couponUsageRepository.save(usage);
    }

    /**
     * 注文キャンセル時にクーポンの使用記録を取り消す。
     *
     * <p>当該注文に紐づくクーポン使用履歴を削除し、
     * クーポンの使用回数（{@code usedCount}）をデクリメントする。
     * 使用履歴が存在しない場合は何もしない。</p>
     *
     * @param orderId キャンセルされた注文 ID
     * @see CheckoutService#cancelOrder
     */
    @Transactional
    public void releaseUsage(String orderId) {
        couponUsageRepository.findByOrderId(orderId).ifPresent(usage -> {
            couponRepository.findById(usage.getCouponId()).ifPresent(coupon -> {
                coupon.setUsedCount(Math.max(coupon.getUsedCount() - 1, 0));
                couponRepository.save(coupon);
            });
            couponUsageRepository.delete(usage);
        });
    }

    /**
     * 全クーポンを取得する（管理者向け）。
     *
     * <p>読み取り専用トランザクションで実行される。有効・無効を問わず全件返す。</p>
     *
     * @return 全クーポンのリスト
     */
    @Transactional(readOnly = true)
    public List<Coupon> listAll() {
        return couponRepository.findAll();
    }

    /**
     * クーポン ID でクーポンを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param couponId クーポン ID
     * @return 該当するクーポンエンティティ
     * @throws ResourceNotFoundException 指定 ID のクーポンが存在しない場合
     */
    @Transactional(readOnly = true)
    public Coupon findById(String couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));
    }

    /**
     * 新しいクーポンを作成する（管理者専用）。
     *
     * <p>{@code ADMIN} ロールが必要。UUID を自動生成して新規レコードを作成する。</p>
     *
     * @param request クーポン作成リクエスト（コード、割引タイプ、割引値、有効期限等）
     * @return 作成されたクーポンエンティティ
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Coupon createCoupon(AdminCouponRequest request) {
        var coupon = new Coupon();
        coupon.setId(UUID.randomUUID().toString());
        populateCoupon(coupon, request);
        return couponRepository.save(coupon);
    }

    /**
     * 既存のクーポンを更新する（管理者専用）。
     *
     * <p>{@code ADMIN} ロールが必要。</p>
     *
     * @param couponId 更新対象のクーポン ID
     * @param request  更新内容を含むリクエスト
     * @return 更新後のクーポンエンティティ
     * @throws ResourceNotFoundException 指定 ID のクーポンが存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Coupon updateCoupon(String couponId, AdminCouponRequest request) {
        var coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));
        populateCoupon(coupon, request);
        return couponRepository.save(coupon);
    }

    /**
     * クーポンを削除する（管理者専用）。
     *
     * <p>{@code ADMIN} ロールが必要。</p>
     *
     * @param couponId 削除対象のクーポン ID
     * @throws ResourceNotFoundException 指定 ID のクーポンが存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteCoupon(String couponId) {
        var coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));
        couponRepository.delete(coupon);
    }

    private void populateCoupon(Coupon coupon, AdminCouponRequest request) {
        coupon.setCampaignId(request.campaignId());
        coupon.setCode(request.code());
        coupon.setCouponType(request.couponType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setDiscountType(request.discountType());
        coupon.setMinimumAmount(request.minimumAmount());
        coupon.setMaximumDiscount(request.maximumDiscount());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setActive(request.active());
        coupon.setExpiresAt(request.expiresAt());
    }
}
