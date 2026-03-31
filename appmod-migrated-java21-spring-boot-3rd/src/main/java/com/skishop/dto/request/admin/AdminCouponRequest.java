package com.skishop.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理者によるクーポン作成・更新リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AdminCouponController AdminCouponController} の
 * 以下のエンドポイントで使用される:</p>
 * <ul>
 *   <li>{@code POST /admin/coupons} — クーポン新規作成</li>
 *   <li>{@code PUT /admin/coupons/{id}} — クーポン更新</li>
 * </ul>
 *
 * <p>Struts 移行元: 管理画面用の ActionForm に相当（新規追加機能を含む）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code id} — 任意（新規作成時は {@code null}）</li>
 *   <li>{@code campaignId} — 任意（キャンペーンとの紐付け用）</li>
 *   <li>{@code code} — 必須、最大 50 文字</li>
 *   <li>{@code couponType} — 必須（空白不可）</li>
 *   <li>{@code discountValue} — 必須、正の数値</li>
 *   <li>{@code discountType} — 必須（空白不可）</li>
 *   <li>{@code minimumAmount} — 任意（最低注文金額の条件）</li>
 *   <li>{@code maximumDiscount} — 任意（割引上限額）</li>
 *   <li>{@code usageLimit} — 0 以上の整数</li>
 *   <li>{@code active} — クーポン有効フラグ</li>
 *   <li>{@code expiresAt} — 任意（有効期限）</li>
 * </ul>
 *
 * @param id              クーポン ID（UUID 形式、新規作成時は {@code null}）
 * @param campaignId      紐付けるキャンペーン ID（任意）
 * @param code            クーポンコード（例: {@code "WINTER2024"}、ユーザーが入力する文字列）
 * @param couponType      クーポン種別（例: {@code "public"}, {@code "private"}）
 * @param discountValue   割引値（{@code discountType} に応じて金額または割合を表す）
 * @param discountType    割引種別（例: {@code "percentage"}, {@code "fixed_amount"}）
 * @param minimumAmount   最低注文金額（この金額以上の注文にのみ適用可能、任意）
 * @param maximumDiscount 割引上限額（パーセンテージ割引時の上限、任意）
 * @param usageLimit      利用回数上限（0 の場合は無制限）
 * @param active          クーポンの有効・無効フラグ
 * @param expiresAt       クーポンの有効期限（{@code null} の場合は無期限）
 * @see com.skishop.controller.AdminCouponController
 */
public record AdminCouponRequest(
    String id,
    String campaignId,

    @NotBlank(message = "{validation.couponCode.required}")
    @Size(max = 50)
    String code,

    @NotBlank(message = "{validation.couponType.required}")
    String couponType,

    @NotNull(message = "{validation.discountValue.required}")
    @Positive(message = "{validation.discountValue.positive}")
    BigDecimal discountValue,

    @NotBlank(message = "{validation.discountType.required}")
    String discountType,

    BigDecimal minimumAmount,
    BigDecimal maximumDiscount,

    @Min(value = 0, message = "{validation.usageLimit.min}")
    int usageLimit,

    boolean active,
    LocalDateTime expiresAt
) {}
