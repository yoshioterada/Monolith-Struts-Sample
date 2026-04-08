package com.skishop.repository;

import com.skishop.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link Payment} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code payments} テーブルに対する CRUD 操作および、注文 ID による
 * 支払い情報の検索機能を提供する。主に
 * {@link com.skishop.service.PaymentService PaymentService} から利用され、
 * 支払いレコードの作成・参照・ステータス管理を支える。</p>
 *
 * <p>支払いレコードは {@link com.skishop.service.CheckoutService CheckoutService} の
 * 注文確定トランザクション内で作成される。1 つの注文に対して複数の支払い
 * （例: 部分返金）が発生する可能性があるため、リストで返す。</p>
 *
 * @see Payment
 * @see com.skishop.model.Order
 * @see com.skishop.service.PaymentService
 */
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * 指定された注文 ID に紐づく全支払い情報を検索する。
     *
     * <p>注文詳細画面での支払い情報表示や、返品・返金処理時の支払い確認で使用される。
     * 通常は 1 件だが、返金等により複数の支払いレコードが存在する可能性がある。</p>
     *
     * @param orderId 検索対象の注文 ID（null 不可）
     * @return 該当注文の支払い情報リスト。存在しない場合は空リスト
     */
    List<Payment> findByOrderId(String orderId);

    /**
     * 指定注文 ID に紐づく最新の支払い情報を取得する。
     *
     * @param orderId 注文 ID
     * @return 最新の支払い情報
     */
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
}
