package com.skishop.repository;

import com.skishop.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link OrderItem} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code order_items} テーブルに対する CRUD 操作および、注文 ID による
 * 注文明細の検索機能を提供する。主に
 * {@link com.skishop.service.OrderService OrderService} から利用され、
 * 注文明細の登録・参照を支える。</p>
 *
 * <p>{@link OrderItem} は {@link com.skishop.model.Order Order} の子エンティティであり、
 * 注文確定時に {@link com.skishop.service.CheckoutService CheckoutService} によって
 * カートアイテムから変換されて作成される。</p>
 *
 * @see OrderItem
 * @see com.skishop.model.Order
 * @see com.skishop.service.OrderService
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    /**
     * 指定された注文 ID に紐づく全注文明細を検索する。
     *
     * <p>注文詳細画面での明細一覧表示や、返品処理時の対象商品確認で使用される。
     * 1 つの注文に複数の注文明細が含まれるため、結果は 0 件以上のリストとなる。</p>
     *
     * @param orderId 検索対象の注文 ID（null 不可）
     * @return 該当注文の注文明細リスト。存在しない場合は空リスト
     */
    List<OrderItem> findByOrderId(String orderId);
}
