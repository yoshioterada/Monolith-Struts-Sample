package com.skishop.constant;

/**
 * アプリケーション全体で使用するステータス文字列定数を一元管理するクラス。
 *
 * <p>ドメイン横断的なステータス値をここに集約し、散在するマジックストリングを排除する。</p>
 */
public final class AppConstants {

    private AppConstants() {}

    /** ユーザー・商品・カートの有効ステータス。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 商品の非公開ステータス。 */
    public static final String STATUS_INACTIVE = "INACTIVE";
    /** アカウントロック済みステータス。 */
    public static final String STATUS_LOCKED = "LOCKED";

    /** 注文: 作成済み。 */
    public static final String ORDER_STATUS_CREATED = "CREATED";
    /** 注文: 確認済み。 */
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    /** 注文: 発送済み。 */
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    /** 注文: 配達済み。 */
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    /** 注文: キャンセル済み。 */
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    /** 注文: 返品済み。 */
    public static final String ORDER_STATUS_RETURNED = "RETURNED";

    /** 決済: オーソリ済み。 */
    public static final String PAYMENT_STATUS_AUTHORIZED = "AUTHORIZED";
    /** 決済: 失敗。 */
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    /** 決済: 取消済み。 */
    public static final String PAYMENT_STATUS_VOID = "VOID";
    /** 決済: 返金済み。 */
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";

    /** 在庫: 在庫あり。 */
    public static final String INVENTORY_STATUS_IN_STOCK = "IN_STOCK";
    /** 在庫: 在庫なし。 */
    public static final String INVENTORY_STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    /** カート: チェックアウト済み。 */
    public static final String CART_STATUS_CHECKED_OUT = "CHECKED_OUT";
    /** カート: マージ済み。 */
    public static final String CART_STATUS_MERGED = "MERGED";

    /** クーポン割引タイプ: パーセント割引。 */
    public static final String COUPON_TYPE_PERCENT = "PERCENT";

    /** ポイント取引: 付与。 */
    public static final String POINT_TX_EARN = "EARN";
    /** ポイント取引: 使用。 */
    public static final String POINT_TX_REDEEM = "REDEEM";
    /** ポイント取引: 返還。 */
    public static final String POINT_TX_REFUND = "REFUND";
    /** ポイント取引: 取消。 */
    public static final String POINT_TX_REVOKE = "REVOKE";

    /** 返品: リクエスト済み。 */
    public static final String RETURN_STATUS_REQUESTED = "REQUESTED";

    /** ユーザーロール。 */
    public static final String ROLE_USER = "USER";
    /** 管理者ロール。 */
    public static final String ROLE_ADMIN = "ADMIN";
}
