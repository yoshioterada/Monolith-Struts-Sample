package com.skishop.repository;

import com.skishop.model.Return;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link Return} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code returns} テーブルに対する CRUD 操作および、注文 ID や注文明細 ID による
 * 返品情報の検索機能を提供する。主に
 * {@link com.skishop.service.ProductService ProductService} から利用され、
 * 返品リクエストの登録・一覧表示・ステータス管理を支える。</p>
 *
 * <p>返品は注文単位または注文明細（商品）単位で行われ、返品理由や
 * 返品ステータス（申請中・承認・却下・完了）が管理される。</p>
 *
 * @see Return
 * @see com.skishop.model.Order
 * @see com.skishop.model.OrderItem
 * @see com.skishop.service.ProductService
 */
public interface ReturnRepository extends JpaRepository<Return, String> {

    /**
     * 指定された注文 ID に紐づく全返品情報を検索する。
     *
     * <p>注文詳細画面での返品状況の確認や、管理画面での返品処理に使用される。
     * 1 つの注文に対して複数の返品リクエストが存在する可能性がある。</p>
     *
     * @param orderId 検索対象の注文 ID（null 不可）
     * @return 該当注文の返品情報リスト。返品がない場合は空リスト
     */
    List<Return> findByOrderId(String orderId);

    /**
     * 指定された注文明細 ID に紐づく返品情報を検索する。
     *
     * <p>特定の商品（注文明細）に対する返品リクエストの有無を確認する際に使用される。
     * 同一商品に対して複数回の返品リクエスト（例: 部分返品後の追加返品）が
     * 存在する可能性があるため、リストで返す。</p>
     *
     * @param orderItemId 検索対象の注文明細 ID（null 不可）
     * @return 該当注文明細の返品情報リスト。返品がない場合は空リスト
     */
    List<Return> findByOrderItemId(String orderItemId);
}
