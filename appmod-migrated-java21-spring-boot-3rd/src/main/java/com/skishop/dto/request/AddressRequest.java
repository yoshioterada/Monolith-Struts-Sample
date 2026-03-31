package com.skishop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 配送先住所の登録・更新リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AccountController#addAddress AccountController#addAddress}
 * エンドポイント（{@code POST /account/addresses}）で受け取るリクエストボディ。
 * Struts 移行元: {@code AddressForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code id} — 任意（新規作成時は {@code null}、更新時は既存 ID を指定）</li>
 *   <li>{@code label} — 必須、最大 50 文字</li>
 *   <li>{@code recipientName} — 必須、最大 100 文字</li>
 *   <li>{@code postalCode} — 必須、最大 20 文字</li>
 *   <li>{@code prefecture} — 必須、最大 50 文字</li>
 *   <li>{@code address1} — 必須、最大 200 文字</li>
 *   <li>{@code address2} — 任意、最大 200 文字</li>
 *   <li>{@code phone} — 任意、最大 20 文字</li>
 *   <li>{@code isDefault} — デフォルト住所フラグ</li>
 * </ul>
 *
 * @param id            住所 ID（UUID 形式、新規作成時は {@code null}）
 * @param label         住所のラベル名（例: 「自宅」「会社」）
 * @param recipientName 受取人氏名
 * @param postalCode    郵便番号
 * @param prefecture    都道府県
 * @param address1      住所 1（市区町村・番地）
 * @param address2      住所 2（建物名・部屋番号など、任意）
 * @param phone         電話番号（任意）
 * @param isDefault     デフォルト配送先として設定するかどうか
 * @see com.skishop.controller.AccountController
 */
public record AddressRequest(
    String id,

    @NotBlank(message = "{validation.label.required}")
    @Size(max = 50)
    String label,

    @NotBlank(message = "{validation.recipientName.required}")
    @Size(max = 100)
    String recipientName,

    @NotBlank(message = "{validation.postalCode.required}")
    @Size(max = 20)
    String postalCode,

    @NotBlank(message = "{validation.prefecture.required}")
    @Size(max = 50)
    String prefecture,

    @NotBlank(message = "{validation.address1.required}")
    @Size(max = 200)
    String address1,

    @Size(max = 200)
    String address2,

    @Size(max = 20)
    String phone,

    boolean isDefault
) {}
