package com.skishop.repository;

import com.skishop.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link Inventory} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code inventory} テーブルに対する CRUD 操作および、商品 ID による在庫情報の
 * 検索機能を提供する。主に {@link com.skishop.service.InventoryService InventoryService}
 * および {@link com.skishop.service.ProductService ProductService} から利用され、
 * 在庫確認・在庫減算・在庫補充などの在庫管理機能を支える。</p>
 *
 * <p>チェックアウト時の在庫確認（{@code checkStock}）や在庫減算（{@code deductStock}）は
 * {@link com.skishop.service.CheckoutService CheckoutService} の注文確定トランザクション内で
 * 呼び出される。</p>
 *
 * @see Inventory
 * @see com.skishop.model.Product
 * @see com.skishop.service.InventoryService
 */
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    /**
     * 指定された商品 ID に紐づく在庫情報を検索する。
     *
     * <p>商品詳細画面での在庫表示、カート追加時の在庫チェック、
     * およびチェックアウト時の在庫確認・減算で使用される。
     * 1 つの商品に対して在庫レコードは 1 件であるため、結果は 0 件または 1 件となる。</p>
     *
     * @param productId 検索対象の商品 ID（null 不可）
     * @return 該当商品の在庫情報を含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<Inventory> findByProductId(String productId);
}
