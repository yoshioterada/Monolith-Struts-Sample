package com.skishop.repository;

import com.skishop.model.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link PointAccount} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code point_accounts} テーブルに対する CRUD 操作および、ユーザー ID による
 * ポイント口座の検索機能を提供する。主に
 * {@link com.skishop.service.PointService PointService} から利用され、
 * ユーザーのポイント残高管理（残高照会・ポイント付与・ポイント消費）を支える。</p>
 *
 * <p>1 人のユーザーに対してポイント口座は 1 つ存在する。ポイントの増減履歴は
 * {@link com.skishop.model.PointTransaction PointTransaction} として別途記録される。</p>
 *
 * @see PointAccount
 * @see com.skishop.model.PointTransaction
 * @see com.skishop.service.PointService
 */
public interface PointAccountRepository extends JpaRepository<PointAccount, String> {

    /**
     * 指定されたユーザー ID に紐づくポイント口座を検索する。
     *
     * <p>マイページでのポイント残高表示やチェックアウト時のポイント利用可能額の確認、
     * ポイント付与・消費処理時の口座参照で使用される。
     * 1 ユーザーにつきポイント口座は 1 つであるため、結果は 0 件または 1 件となる。</p>
     *
     * @param userId 検索対象のユーザー ID（null 不可）
     * @return 該当ユーザーのポイント口座を含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<PointAccount> findByUserId(String userId);
}
