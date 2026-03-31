package com.skishop.repository;

import com.skishop.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link Coupon} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code coupons} テーブルに対する CRUD 操作および、クーポンコードによる検索や
 * 有効クーポンの一覧取得機能を提供する。主に
 * {@link com.skishop.service.CouponService CouponService} から利用され、
 * チェックアウト時のクーポン適用・割引計算を支える。</p>
 *
 * <p>クーポンにはコード（一意）、割引率または割引額、有効期間、利用回数制限などの
 * 属性があり、注文確定時に {@link com.skishop.model.CouponUsage CouponUsage} として
 * 使用履歴が記録される。</p>
 *
 * @see Coupon
 * @see com.skishop.model.CouponUsage
 * @see com.skishop.service.CouponService
 */
public interface CouponRepository extends JpaRepository<Coupon, String> {

    /**
     * 指定されたクーポンコードでクーポンを検索する。
     *
     * <p>チェックアウト画面でユーザーが入力したクーポンコードの有効性を検証する際に使用される。
     * クーポンコードは一意制約があるため、結果は 0 件または 1 件となる。</p>
     *
     * @param code 検索対象のクーポンコード（null 不可）
     * @return 該当クーポンを含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 有効状態のクーポンを全件検索する。
     *
     * <p>管理画面での有効クーポン一覧表示や、利用可能なクーポンの
     * ユーザーへの提示に使用される。{@code active} フラグが {@code true} の
     * クーポンのみを返す。</p>
     *
     * @return 有効なクーポンのリスト。存在しない場合は空リスト
     */
    List<Coupon> findByActiveTrue();
}
