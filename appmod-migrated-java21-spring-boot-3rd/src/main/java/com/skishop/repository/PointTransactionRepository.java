package com.skishop.repository;

import com.skishop.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link PointTransaction} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code point_transactions} テーブルに対する CRUD 操作および、ユーザー ID による
 * ポイント取引履歴の検索機能を提供する。主に
 * {@link com.skishop.service.PointService PointService} から利用され、
 * ポイントの付与・消費・取消などの取引履歴の記録・照会を支える。</p>
 *
 * <p>{@link PointTransaction} は {@link com.skishop.model.PointAccount PointAccount} の
 * 明細レコードであり、ポイント残高の変動理由（注文によるポイント付与、チェックアウト時の
 * ポイント消費など）を追跡するために使用される。</p>
 *
 * @see PointTransaction
 * @see com.skishop.model.PointAccount
 * @see com.skishop.service.PointService
 */
public interface PointTransactionRepository extends JpaRepository<PointTransaction, String> {

    /**
     * 指定されたユーザー ID に紐づく全ポイント取引履歴を検索する。
     *
     * <p>マイページのポイント履歴一覧で使用される。付与・消費・取消を含む
     * 全種類の取引を返すため、結果は 0 件以上のリストとなる。</p>
     *
     * @param userId 検索対象のユーザー ID（null 不可）
     * @return 該当ユーザーのポイント取引履歴リスト。取引がない場合は空リスト
     */
    List<PointTransaction> findByUserId(String userId);
}
