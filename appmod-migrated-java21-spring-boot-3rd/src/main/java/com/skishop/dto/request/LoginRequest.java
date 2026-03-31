package com.skishop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ログインリクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>Spring Security のフォームログイン認証で使用されるリクエストボディ。
 * Struts 移行元: {@code LoginForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code email} — 必須、メールアドレス形式、最大 255 文字</li>
 *   <li>{@code password} — 必須、8〜100 文字</li>
 * </ul>
 *
 * @param email    ユーザーのメールアドレス（ログイン ID として使用）
 * @param password ユーザーのパスワード（平文、サーバー側でハッシュ照合）
 */
public record LoginRequest(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    String email,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100)
    String password
) {}
