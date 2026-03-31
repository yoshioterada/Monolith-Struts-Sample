package com.skishop.controller;

import com.skishop.dto.request.PasswordForgotRequest;
import com.skishop.dto.request.PasswordResetRequest;
import com.skishop.dto.request.RegisterRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.PasswordResetToken;
import com.skishop.service.MailService;
import com.skishop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 認証関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>ログイン画面表示、ユーザー新規登録、パスワード忘れ・リセット機能の画面表示および
 * フォーム送信を処理する。ベース URL: {@code /auth}</p>
 *
 * <p>Struts 移行元: {@code LoginAction}, {@code RegisterAction}, {@code PasswordResetAction}</p>
 *
 * <p>認可: 全エンドポイントは未認証ユーザーがアクセス可能（{@code permitAll}）。
 * ログイン処理自体は Spring Security の {@code formLogin} 設定に委任される。</p>
 *
 * @see UserService
 * @see MailService
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final MailService mailService;

    /**
     * ログイン画面を表示する。
     *
     * <p>{@code GET /auth/login} — ログインフォームを表示する。
     * 実際の認証処理は Spring Security の {@code formLogin} に委任される。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能</p>
     *
     * @return {@code "auth/login"} ログイン画面のテンプレート名
     */
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    /**
     * ユーザー登録画面を表示する。
     *
     * <p>{@code GET /auth/register} — 空の登録フォームを表示する。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能</p>
     *
     * @param model ビューに渡すモデル（空の {@code registerRequest} を格納）
     * @return {@code "auth/register"} ユーザー登録画面のテンプレート名
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", "", ""));
        return "auth/register";
    }

    /**
     * ユーザー登録フォームの送信を処理する。
     *
     * <p>{@code POST /auth/register} — メールアドレス、ユーザー名、パスワードでユーザーを新規登録する。
     * パスワード確認の一致チェックを行い、成功時はログイン画面にリダイレクトする。
     * バリデーションエラーまたはパスワード不一致の場合は登録画面を再表示する。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能</p>
     *
     * @param request 登録リクエスト（メールアドレス、ユーザー名、パスワード、パスワード確認）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功メッセージ）
     * @return 成功時: {@code "redirect:/auth/login"}, 失敗時: {@code "auth/register"}
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        if (!request.password().equals(request.passwordConfirm())) {
            result.rejectValue("passwordConfirm", "validation.passwordConfirm.mismatch");
            return "auth/register";
        }
        userService.registerNewUser(request.email(), request.username(), request.password());
        redirectAttributes.addFlashAttribute("successMessage", "registration.success");
        return "redirect:/auth/login";
    }

    /**
     * パスワード忘れ画面を表示する。
     *
     * <p>{@code GET /auth/password/forgot} — メールアドレス入力用のパスワードリセット依頼フォームを表示する。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能</p>
     *
     * @param model ビューに渡すモデル（空の {@code forgotRequest} を格納）
     * @return {@code "auth/password-forgot"} パスワード忘れ画面のテンプレート名
     */
    @GetMapping("/password/forgot")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("forgotRequest", new PasswordForgotRequest(""));
        return "auth/password-forgot";
    }

    /**
     * パスワードリセット依頼フォームの送信を処理する。
     *
     * <p>{@code POST /auth/password/forgot} — 入力されたメールアドレスに対してパスワードリセットトークンを
     * 生成し、リセット用メールを送信する。存在しないメールアドレスでもセキュリティ上同一のレスポンスを返す。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能</p>
     *
     * @param request パスワード忘れリクエスト（メールアドレス）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（送信完了メッセージ）
     * @return 成功時: {@code "redirect:/auth/password/forgot"}, バリデーションエラー時: {@code "auth/password-forgot"}
     */
    @PostMapping("/password/forgot")
    public String forgotPassword(@Valid @ModelAttribute PasswordForgotRequest request,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/password-forgot";
        }
        try {
            String token = userService.createPasswordResetToken(request.email());
            mailService.enqueuePasswordReset(request.email(), token);
        } catch (ResourceNotFoundException e) {
            log.info("Password reset requested for non-existent email");
        }
        redirectAttributes.addFlashAttribute("successMessage", "passwordReset.emailSent");
        return "redirect:/auth/password/forgot";
    }

    /**
     * パスワードリセット画面を表示する。
     *
     * <p>{@code GET /auth/password/reset?token=xxx} — リセットメールに含まれるトークンを受け取り、
     * 新しいパスワード入力フォームを表示する。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能（有効なトークンが必要）</p>
     *
     * @param token パスワードリセットトークン（クエリパラメータ）
     * @param model ビューに渡すモデル（{@code token}, {@code resetRequest} を格納）
     * @return {@code "auth/password-reset"} パスワードリセット画面のテンプレート名
     */
    @GetMapping("/password/reset")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("resetRequest", new PasswordResetRequest(token, "", ""));
        return "auth/password-reset";
    }

    /**
     * パスワードリセットフォームの送信を処理する。
     *
     * <p>{@code POST /auth/password/reset} — トークンを検証し、新しいパスワードでユーザーのパスワードを更新する。
     * パスワード確認の一致チェックを行い、成功時はログイン画面にリダイレクトする。
     * 使用済みのトークンは無効化される。</p>
     *
     * <p>認可: 未認証ユーザーがアクセス可能（有効なトークンが必要）</p>
     *
     * @param request パスワードリセットリクエスト（トークン、新パスワード、パスワード確認）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功メッセージ）
     * @return 成功時: {@code "redirect:/auth/login"}, 失敗時: {@code "auth/password-reset"}
     */
    @PostMapping("/password/reset")
    public String resetPassword(@Valid @ModelAttribute PasswordResetRequest request,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/password-reset";
        }
        if (!request.password().equals(request.passwordConfirm())) {
            result.rejectValue("passwordConfirm", "validation.passwordConfirm.mismatch");
            return "auth/password-reset";
        }
        PasswordResetToken resetToken = userService.validateResetToken(request.token());
        userService.updatePassword(resetToken.getUserId(), request.password());
        userService.consumeResetToken(request.token());
        redirectAttributes.addFlashAttribute("successMessage", "passwordReset.success");
        return "redirect:/auth/login";
    }
}
