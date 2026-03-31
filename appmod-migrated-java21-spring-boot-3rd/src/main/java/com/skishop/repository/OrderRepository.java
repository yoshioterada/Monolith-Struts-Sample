package com.skishop.repository;

import com.skishop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link Order} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code orders} テーブルに対する CRUD 操作および、ユーザー ID や注文番号による
 * 注文の検索機能を提供する。主に
 * {@link com.skishop.service.OrderService OrderService} から利用され、
 * 注文の登録・一覧表示・詳細参照・ステータス更新を支える。</p>
 *
 * <p>注文は {@link com.skishop.service.CheckoutService CheckoutService} の
 * 注文確定フローで作成され、{@link com.skishop.model.OrderItem OrderItem}、
 * {@link com.skishop.model.Payment Payment}、
 * {@link com.skishop.model.OrderShipping OrderShipping} などの
 * 関連エンティティとともに管理される。</p>
 *
 * @see Order
 * @see com.skishop.model.OrderItem
 * @see com.skishop.service.OrderService
 * @see com.skishop.service.CheckoutService
 */
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * 指定されたユーザー ID に紐づく全注文を検索する。
     *
     * <p>マイページの注文履歴一覧で使用される。ユーザーの全注文（ステータス問わず）を
     * 返すため、結果は 0 件以上のリストとなる。</p>
     *
     * @param userId 検索対象のユーザー ID（null 不可）
     * @return 該当ユーザーの注文リスト。注文がない場合は空リスト
     */
    List<Order> findByUserId(String userId);

    /**
     * 指定された注文番号で注文を検索する。
     *
     * <p>注文確認画面や注文追跡で、ユーザーに公開される注文番号から注文を特定する際に使用される。
     * 注文番号は一意制約があるため、結果は 0 件または 1 件となる。</p>
     *
     * @param orderNumber 検索対象の注文番号（null 不可）
     * @return 該当注文を含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<Order> findByOrderNumber(String orderNumber);
}
