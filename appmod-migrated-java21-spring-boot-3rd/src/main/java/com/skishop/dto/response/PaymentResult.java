package com.skishop.dto.response;

/**
 * 決済処理の結果を表す sealed インターフェース。
 *
 * <p>{@link com.skishop.service.PaymentService PaymentService} の決済認可処理の戻り値として使用される。
 * Java 21 の sealed interface + record パターンを活用し、決済結果を
 * {@link Success}（成功）と {@link Failure}（失敗）の 2 つの型に限定する。</p>
 *
 * <p>Struts 移行元: 移行元には対応するクラスは存在しない（新規設計）。
 * 移行前は決済結果を {@code boolean} や例外で表現していたものを、
 * 型安全な結果型パターンに置き換えたもの。</p>
 *
 * @see com.skishop.service.PaymentService
 * @see com.skishop.service.CheckoutService
 */
public sealed interface PaymentResult permits PaymentResult.Success, PaymentResult.Failure {

    /**
     * 決済が成功したかどうかを返す。
     *
     * @return 成功の場合は {@code true}、失敗の場合は {@code false}
     */
    boolean isSuccess();

    /**
     * 決済ステータス文字列を返す。
     *
     * @return 決済ステータス（例: {@code "approved"}, {@code "declined"}）
     */
    String status();

    /**
     * 決済成功を表すレコード。
     *
     * <p>決済が正常に認可された場合に返される。決済 ID とステータスを保持する。</p>
     *
     * @param paymentId 決済処理で発行された一意の決済 ID
     * @param status    決済ステータス（例: {@code "approved"}）
     */
    record Success(String paymentId, String status) implements PaymentResult {
        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    /**
     * 決済失敗を表すレコード。
     *
     * <p>決済が拒否またはエラーとなった場合に返される。ステータスとエラーメッセージを保持する。</p>
     *
     * @param status  決済ステータス（例: {@code "declined"}, {@code "error"}）
     * @param message エラーメッセージ（失敗理由の説明）
     */
    record Failure(String status, String message) implements PaymentResult {
        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}
