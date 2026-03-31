package com.skishop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ユーザー新規登録リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>{@link com.skishop.controller.AuthController#register AuthController#register}
 * エンドポイント（{@code POST /auth/register}）で受け取るリクエストボディ。
 * Struts 移行元: {@code RegisterForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code email} — 必須、メールアドレス形式、最大 255 文字</li>
 *   <li>{@code password} — 必須、8〜100 文字</li>
 *   <li>{@code passwordConfirm} — 必須（{@code password} と一致することをサービス層で検証）</li>
 *   <li>{@code username} — 必須、最大 100 文字</li>
 * </ul>
 *
 * @param email           ユーザーのメールアドレス（ログイン ID として使用、一意制約あり）
 * @param password        パスワード（平文、サーバー側で BCrypt ハッシュ化して保存）
 * @param passwordConfirm パスワード確認入力（{@code password} との一致検証用）
 * @param username        ユーザー名（表示名として使用）
 * @see com.skishop.controller.AuthController
 */
public record RegisterRequest(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    String email,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100)
    String password,

    @NotBlank(message = "{validation.passwordConfirm.required}")
    String passwordConfirm,

    @NotBlank(message = "{validation.username.required}")
    @Size(max = 100)
    String username
) {}
