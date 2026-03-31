package com.skishop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワードリセット実行リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AuthController#resetPassword AuthController#resetPassword}
 * エンドポイント（{@code POST /auth/password/reset}）で受け取るリクエストボディ。
 * メールで受信したトークンと新しいパスワードを送信し、パスワードを更新する。
 * Struts 移行元: {@code PasswordResetForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code token} — 必須（空白不可）</li>
 *   <li>{@code password} — 必須、8〜100 文字</li>
 *   <li>{@code passwordConfirm} — 必須（{@code password} と一致することをサービス層で検証）</li>
 * </ul>
 *
 * @param token           パスワードリセット用の一時トークン（メールリンクから取得）
 * @param password        新しいパスワード（平文、サーバー側で BCrypt ハッシュ化）
 * @param passwordConfirm パスワード確認入力（{@code password} との一致検証用）
 * @see com.skishop.controller.AuthController
 */
public record PasswordResetRequest(
    @NotBlank(message = "{validation.token.required}")
    String token,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100)
    String password,

    @NotBlank(message = "{validation.passwordConfirm.required}")
    String passwordConfirm
) {}
