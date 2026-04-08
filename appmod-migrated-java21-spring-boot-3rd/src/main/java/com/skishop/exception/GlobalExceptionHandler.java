package com.skishop.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

/**
 * アプリケーション全体の例外を統一的にハンドリングする {@code @ControllerAdvice} クラス。
 *
 * <p>SkiShop の全 Controller で発生する例外を捕捉し、適切な HTTP ステータスコードと
 * エラーページを返却する。以下の例外タイプを個別にハンドリングする:</p>
 *
 * <table border="1">
 *   <caption>例外ハンドリング一覧</caption>
 *   <tr><th>例外クラス</th><th>HTTP ステータス</th><th>レスポンス</th></tr>
 *   <tr><td>{@link ResourceNotFoundException}</td><td>404 Not Found</td><td>{@code error/404} ビュー</td></tr>
 *   <tr><td>{@link BusinessException}</td><td>（リダイレクトまたはビュー）</td><td>リダイレクト先または {@code error/business} ビュー</td></tr>
 *   <tr><td>{@link AccessDeniedException}</td><td>403 Forbidden</td><td>{@code error/403} ビュー</td></tr>
 *   <tr><td>{@link Exception}（その他全般）</td><td>500 Internal Server Error</td><td>{@code error/500} ビュー（一意のエラー ID 付き）</td></tr>
 * </table>
 *
 * <p>セキュリティ上の考慮事項: クライアントへのレスポンスにスタックトレースや
 * 内部実装の詳細を含めない。予期しないエラーには UUID ベースのエラー ID を生成し、
 * サーバーログとの照合を可能にする。</p>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムでは {@code struts-config.xml} の
 * {@code <global-exceptions>} と各 Action 内の try-catch で例外処理を行っていたが、
 * Spring の {@code @ControllerAdvice} による宣言的な統一例外ハンドリングに移行した。</p>
 *
 * @see ResourceNotFoundException リソース未検出例外
 * @see BusinessException ビジネスルール違反例外
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * {@link ResourceNotFoundException} をハンドリングし、404 エラーページを返す。
     *
     * <p>指定されたリソースが見つからない場合（例: 存在しない商品 ID、注文 ID）に
     * 呼び出される。リソース名と ID をログに記録し、エラーメッセージをモデルに
     * 追加して {@code error/404} テンプレートを返す。</p>
     *
     * @param ex    発生した {@link ResourceNotFoundException}
     * @param model ビューに渡すモデル（エラーメッセージを格納）
     * @return {@code error/404} テンプレート名
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {} id={}", ex.getResourceName(), ex.getResourceId());
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    /**
     * {@link BusinessException} をハンドリングし、リダイレクトまたはエラーページを返す。
     *
     * <p>ビジネスルール違反（在庫不足、クーポン無効、ポイント不足等）が発生した場合に
     * 呼び出される。{@link BusinessException#getRedirectUrl()} が設定されている場合は
     * その URL へリダイレクトし、フラッシュ属性としてエラーメッセージを渡す。
     * 設定されていない場合は {@code error/business} テンプレートを表示する。</p>
     *
     * @param ex                 発生した {@link BusinessException}
     * @param redirectAttributes リダイレクト時にフラッシュ属性を渡すための {@link RedirectAttributes}
     * @param model              ビューに渡すモデル（リダイレクトしない場合にエラーメッセージを格納）
     * @return リダイレクト先 URL（{@code redirect:xxx}）または {@code error/business} テンプレート名
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, RedirectAttributes redirectAttributes, Model model) {
        log.warn("Business rule violation: {}", ex.getMessageKey());
        String redirectUrl = ex.getRedirectUrl();
        if (redirectUrl != null) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + redirectUrl;
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/business";
    }

    /**
     * {@link AccessDeniedException} をハンドリングし、403 エラーページを返す。
     *
     * <p>認証済みユーザーが権限のないリソースにアクセスした場合
     * （例: 一般ユーザーが管理画面にアクセス）に呼び出される。</p>
     *
     * @param ex    発生した {@link AccessDeniedException}
     * @param model ビューに渡すモデル
     * @return {@code error/403} テンプレート名
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Access denied: {}", ex.getMessage());
        return "error/403";
    }

    /**
     * {@link ObjectOptimisticLockingFailureException} をハンドリングし、カート画面にリダイレクトする。
     *
     * <p>在庫・クーポン・ポイントの並行更新で楽観ロック競合が発生した場合に
     * リトライを促すメッセージとともにリダイレクトする。</p>
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                        RedirectAttributes redirectAttributes) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage",
                "Another update was in progress. Please try again.");
        return "redirect:/cart";
    }

    /**
     * {@link MethodArgumentNotValidException} をハンドリングし、400 エラーページを返す。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex, Model model) {
        log.warn("Validation failed: {} error(s)", ex.getBindingResult().getErrorCount());
        model.addAttribute("errors", ex.getBindingResult().getAllErrors());
        return "error/400";
    }

    /**
     * {@link DataIntegrityViolationException} をハンドリングする。
     *
     * <p>ユニーク制約違反等の DB 整合性エラー（TOCTOU レースコンディション対策）。</p>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                       RedirectAttributes redirectAttributes) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        redirectAttributes.addFlashAttribute("errorMessage",
                "The requested operation could not be completed. Please try again.");
        return "redirect:/auth/register";
    }

    /**
     * その他すべての未捕捉例外をハンドリングし、500 エラーページを返す。
     *
     * <p>上記の個別ハンドラーで捕捉されなかった予期しない例外の最終防壁として機能する。
     * UUID ベースのエラー ID を生成し、以下の処理を行う:</p>
     * <ul>
     *   <li>サーバーログにエラー ID・メッセージ・スタックトレースを {@code ERROR} レベルで記録する</li>
     *   <li>エラー ID をモデルに追加し、{@code error/500} テンプレートで表示する
     *       （ユーザーがサポートに問い合わせる際の参照用）</li>
     * </ul>
     *
     * <p>セキュリティ上、クライアントにはスタックトレースや内部エラー詳細を返さない。</p>
     *
     * @param ex    発生した {@link Exception}
     * @param model ビューに渡すモデル（エラー ID を格納）
     * @return {@code error/500} テンプレート名
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error [errorId={}]: {}", errorId, ex.getMessage(), ex);
        model.addAttribute("errorId", errorId);
        return "error/500";
    }
}
