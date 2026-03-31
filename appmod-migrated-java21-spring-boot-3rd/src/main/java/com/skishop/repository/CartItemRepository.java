package com.skishop.repository;

import com.skishop.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link CartItem} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code cart_items} テーブルに対する CRUD 操作および、カート ID による
 * カート内商品の検索・一括削除機能を提供する。主に
 * {@link com.skishop.service.CartService CartService} から利用され、
 * カートへの商品追加・数量変更・削除、およびチェックアウト時のカートクリアを支える。</p>
 *
 * <p>{@link CartItem} は {@link com.skishop.model.Cart Cart} の子エンティティであり、
 * 1 つのカートに複数のカートアイテムが紐づく。</p>
 *
 * @see CartItem
 * @see com.skishop.model.Cart
 * @see com.skishop.service.CartService
 */
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    /**
     * 指定されたカート ID に紐づく全カートアイテムを検索する。
     *
     * <p>カート内容の一覧表示やチェックアウト時の在庫確認・金額計算で使用される。
     * カート内の商品数に応じて 0 件以上のリストを返す。</p>
     *
     * @param cartId 検索対象のカート ID（null 不可）
     * @return 該当カートのカートアイテムリスト。カートが空の場合は空リスト
     */
    List<CartItem> findByCartId(String cartId);

    /**
     * 指定されたカート ID に紐づく全カートアイテムを一括削除する。
     *
     * <p>チェックアウト完了後のカートクリア処理で使用される。
     * {@code @Transactional} コンテキスト内で呼び出す必要がある。</p>
     *
     * @param cartId 削除対象のカート ID（null 不可）
     */
    void deleteByCartId(String cartId);
}
