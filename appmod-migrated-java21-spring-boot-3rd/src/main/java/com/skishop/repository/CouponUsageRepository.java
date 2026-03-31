package com.skishop.repository;

import com.skishop.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link CouponUsage} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code coupon_usages} テーブルに対する CRUD 操作および、クーポン使用履歴の
 * 検索・集計・削除機能を提供する。主に
 * {@link com.skishop.service.CouponService CouponService} から利用され、
 * クーポンの利用回数チェックや注文キャンセル時のクーポン返却を支える。</p>
 *
 * <p>{@link CouponUsage} はユーザー・クーポン・注文の 3 者を紐づける中間エンティティであり、
 * 同一ユーザーによる同一クーポンの複数回使用を制限するために使用される。</p>
 *
 * @see CouponUsage
 * @see com.skishop.model.Coupon
 * @see com.skishop.service.CouponService
 */
public interface CouponUsageRepository extends JpaRepository<CouponUsage, String> {

    /**
     * 指定されたユーザー ID とクーポン ID の組み合わせでクーポン使用履歴を検索する。
     *
     * <p>特定ユーザーの特定クーポンに対する過去の使用履歴を取得する際に使用される。
     * クーポンの利用回数制限の判定に活用できる。</p>
     *
     * @param userId   検索対象のユーザー ID（null 不可）
     * @param couponId 検索対象のクーポン ID（null 不可）
     * @return 該当する使用履歴のリスト。未使用の場合は空リスト
     */
    List<CouponUsage> findByUserIdAndCouponId(String userId, String couponId);

    /**
     * 指定されたユーザー ID とクーポン ID の組み合わせでクーポン使用回数を集計する。
     *
     * <p>チェックアウト時にユーザーがクーポンの利用上限回数に達しているかを判定する際に使用される。
     * クーポンの {@code maxUsagePerUser} と比較して使用可否を決定する。</p>
     *
     * @param userId   集計対象のユーザー ID（null 不可）
     * @param couponId 集計対象のクーポン ID（null 不可）
     * @return 該当する使用回数（未使用の場合は 0）
     */
    long countByUserIdAndCouponId(String userId, String couponId);

    /**
     * 指定された注文 ID に紐づくクーポン使用履歴を検索する。
     *
     * <p>注文詳細画面でのクーポン適用情報の表示や、注文キャンセル時の
     * クーポン使用履歴の確認に使用される。1 つの注文に適用されるクーポンは
     * 1 つであるため、結果は 0 件または 1 件となる。</p>
     *
     * @param orderId 検索対象の注文 ID（null 不可）
     * @return 該当する使用履歴を含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<CouponUsage> findByOrderId(String orderId);

    /**
     * 指定された注文 ID に紐づくクーポン使用履歴を削除する。
     *
     * <p>注文キャンセル時にクーポンの使用回数を戻すために使用される。
     * {@code @Transactional} コンテキスト内で呼び出す必要がある。</p>
     *
     * @param orderId 削除対象の注文 ID（null 不可）
     */
    void deleteByOrderId(String orderId);
}
