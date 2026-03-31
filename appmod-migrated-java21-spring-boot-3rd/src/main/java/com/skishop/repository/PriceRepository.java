package com.skishop.repository;

import com.skishop.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link Price} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code prices} テーブルに対する CRUD 操作および、商品 ID による
 * 価格情報の検索機能を提供する。主に
 * {@link com.skishop.service.ProductService ProductService} および
 * {@link com.skishop.service.CartService CartService} から利用され、
 * 商品価格の参照や金額計算を支える。</p>
 *
 * <p>1 つの商品に対して複数の価格レコード（通常価格、セール価格、会員価格など）が
 * 存在する可能性がある。有効期間や価格種別による絞り込みはサービス層で行われる。</p>
 *
 * @see Price
 * @see com.skishop.model.Product
 * @see com.skishop.service.ProductService
 */
public interface PriceRepository extends JpaRepository<Price, String> {

    /**
     * 指定された商品 ID に紐づく全価格情報を検索する。
     *
     * <p>商品詳細画面での価格表示やカート内の金額計算、チェックアウト時の
     * 合計金額算出で使用される。通常価格・セール価格など複数の価格が
     * 存在する可能性があるため、リストで返す。</p>
     *
     * @param productId 検索対象の商品 ID（null 不可）
     * @return 該当商品の価格情報リスト。存在しない場合は空リスト
     */
    List<Price> findByProductId(String productId);
}
