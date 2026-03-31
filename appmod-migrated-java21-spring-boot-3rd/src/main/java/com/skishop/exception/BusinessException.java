package com.skishop.exception;

/**
 * ビジネスルール違反を表すランタイム例外。
 *
 * <p>SkiShop のビジネスロジック層（Service 層）で、業務ルールに違反する操作が
 * 検出された場合にスローされる。例えば、以下のようなケースで使用する:</p>
 * <ul>
 *   <li>在庫不足（注文数量が在庫を超過）</li>
 *   <li>無効なクーポンコードの適用</li>
 *   <li>ポイント残高不足</li>
 *   <li>注文のキャンセル期限超過</li>
 * </ul>
 *
 * <p>{@link GlobalExceptionHandler#handleBusiness(BusinessException,
 * org.springframework.web.servlet.mvc.support.RedirectAttributes, org.springframework.ui.Model)}
 * で捕捉され、{@link #getRedirectUrl()} が設定されている場合はリダイレクト先に
 * フラッシュメッセージとして表示され、未設定の場合はエラーページ
 * {@code error/business} を返す。</p>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムでは {@code ActionErrors} と
 * {@code ActionMessages} で業務エラーを管理していたが、Spring の例外ベースの
 * エラーハンドリング（{@code @ControllerAdvice}）に移行した。</p>
 *
 * @see GlobalExceptionHandler#handleBusiness(BusinessException,
 *      org.springframework.web.servlet.mvc.support.RedirectAttributes, org.springframework.ui.Model)
 */
public class BusinessException extends RuntimeException {

    /** エラー発生時のリダイレクト先 URL。{@code null} の場合はエラーページを表示する。 */
    private final String redirectUrl;

    /** 国際化メッセージキー。メッセージリソースからエラーメッセージを解決するために使用する。 */
    private final String messageKey;

    /**
     * リダイレクト先とメッセージキーを指定してビジネス例外を生成する。
     *
     * @param message     例外メッセージ（ログ出力およびデフォルトの表示メッセージ）
     * @param redirectUrl エラー発生時のリダイレクト先 URL（{@code null} の場合はエラーページを表示）
     * @param messageKey  国際化メッセージキー（{@code null} の場合は {@code message} をそのまま使用）
     */
    public BusinessException(String message, String redirectUrl, String messageKey) {
        super(message);
        this.redirectUrl = redirectUrl;
        this.messageKey = messageKey;
    }

    /**
     * メッセージのみを指定してビジネス例外を生成する。
     *
     * <p>リダイレクト先とメッセージキーは {@code null} に設定されるため、
     * {@link GlobalExceptionHandler} ではエラーページ {@code error/business} が表示される。</p>
     *
     * @param message 例外メッセージ
     */
    public BusinessException(String message) {
        this(message, null, null);
    }

    /**
     * エラー発生時のリダイレクト先 URL を返す。
     *
     * @return リダイレクト先 URL。設定されていない場合は {@code null}
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * 国際化メッセージキーを返す。
     *
     * @return メッセージキー。設定されていない場合は {@code null}
     */
    public String getMessageKey() {
        return messageKey;
    }
}
