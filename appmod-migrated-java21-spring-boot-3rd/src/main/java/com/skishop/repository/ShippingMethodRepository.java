package com.skishop.repository;

import com.skishop.model.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link ShippingMethod} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code shipping_methods} テーブルに対する CRUD 操作および、有効な配送方法の
 * 検索機能を提供する。主に
 * {@link com.skishop.service.ShippingService ShippingService} および
 * {@link com.skishop.service.AdminShippingMethodService AdminShippingMethodService}
 * から利用され、チェックアウト時の配送方法選択や管理画面での配送方法管理を支える。</p>
 *
 * <p>配送方法には名称、配送料、配送日数目安、有効/無効フラグなどの属性があり、
 * ユーザーには有効な配送方法のみが選択肢として表示される。</p>
 *
 * @see ShippingMethod
 * @see com.skishop.service.ShippingService
 * @see com.skishop.service.AdminShippingMethodService
 */
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, String> {

    /**
     * 有効状態の配送方法を全件検索する。
     *
     * <p>チェックアウト画面での配送方法選択肢の表示に使用される。
     * {@code active} フラグが {@code true} の配送方法のみを返す。
     * 管理者が無効化した配送方法はユーザーに表示されない。</p>
     *
     * @return 有効な配送方法のリスト。存在しない場合は空リスト
     */
    List<ShippingMethod> findByActiveTrue();
}
