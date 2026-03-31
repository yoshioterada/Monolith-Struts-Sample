package com.skishop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワードリセット要求（パスワードを忘れた場合）のリクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AuthController#forgotPassword AuthController#forgotPassword}
 * エンドポイント（{@code POST /auth/password/forgot}）で受け取るリクエストボディ。
 * 入力されたメールアドレス宛にパスワードリセット用のトークン付きリンクを送信する。
 * Struts 移行元: {@code PasswordResetRequestForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code email} — 必須、メールアドレス形式、最大 255 文字</li>
 * </ul>
 *
 * @param email パスワードリセット対象のユーザーのメールアドレス
 * @see com.skishop.controller.AuthController
 */
public record PasswordForgotRequest(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    String email
) {}
