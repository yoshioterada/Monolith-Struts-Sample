package com.skishop.repository;

import com.skishop.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link Cart} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code carts} テーブルに対する CRUD 操作および、ユーザー ID・ステータスや
 * セッション ID によるカート検索機能を提供する。主に
 * {@link com.skishop.service.CartService CartService} から利用され、
 * ショッピングカートのライフサイクル管理（作成・取得・ステータス更新）を支える。</p>
 *
 * <p>未ログインユーザーはセッション ID でカートを識別し、ログイン後は
 * ユーザー ID に紐づけてカートをマージする。</p>
 *
 * @see Cart
 * @see com.skishop.model.CartItem
 * @see com.skishop.service.CartService
 */
public interface CartRepository extends JpaRepository<Cart, String> {

    /**
     * 指定されたユーザー ID とステータスに合致するカートを検索する。
     *
     * <p>ログイン済みユーザーのアクティブなカート（例: ステータス {@code "ACTIVE"}）を
     * 取得する際に使用される。通常はアクティブなカートは 1 件だが、
     * 過去のカートが残存する可能性があるためリストで返す。</p>
     *
     * @param userId 検索対象のユーザー ID（null 不可）
     * @param status 検索対象のカートステータス（例: {@code "ACTIVE"}, {@code "COMPLETED"}）
     * @return 条件に合致するカートのリスト。存在しない場合は空リスト
     */
    List<Cart> findByUserIdAndStatus(String userId, String status);

    /**
     * 指定された HTTP セッション ID に紐づくカートを検索する。
     *
     * <p>未ログインユーザーのカートを識別するために使用される。
     * セッション ID は一意であるため、結果は 0 件または 1 件となる。
     * ログイン成功時のカートマージ処理でも参照される。</p>
     *
     * @param sessionId 検索対象の HTTP セッション ID（null 不可）
     * @return 該当カートを含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<Cart> findBySessionId(String sessionId);
}
