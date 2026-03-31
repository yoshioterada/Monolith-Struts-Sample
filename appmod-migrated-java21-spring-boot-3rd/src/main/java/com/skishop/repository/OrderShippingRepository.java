package com.skishop.repository;

import com.skishop.model.OrderShipping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link OrderShipping} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code order_shipping} テーブルに対する CRUD 操作および、注文 ID による
 * 配送情報の検索機能を提供する。主に
 * {@link com.skishop.service.ShippingService ShippingService} から利用され、
 * 注文の配送先情報・配送方法・追跡番号の管理を支える。</p>
 *
 * <p>1 つの注文に対して配送情報は 1 件であり、注文確定時に作成される。
 * 配送ステータスの更新や追跡番号の付与は管理画面から行われる。</p>
 *
 * @see OrderShipping
 * @see com.skishop.model.Order
 * @see com.skishop.service.ShippingService
 */
public interface OrderShippingRepository extends JpaRepository<OrderShipping, String> {

    /**
     * 指定された注文 ID に紐づく配送情報を検索する。
     *
     * <p>注文詳細画面での配送状況表示や、管理画面での配送ステータス更新時に使用される。
     * 1 つの注文に対して配送情報は 1 件であるため、結果は 0 件または 1 件となる。</p>
     *
     * @param orderId 検索対象の注文 ID（null 不可）
     * @return 該当注文の配送情報を含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<OrderShipping> findByOrderId(String orderId);
}
