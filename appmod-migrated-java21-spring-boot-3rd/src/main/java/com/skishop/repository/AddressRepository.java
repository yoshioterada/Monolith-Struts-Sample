package com.skishop.repository;

import com.skishop.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link Address} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code addresses} テーブルに対する CRUD 操作および、ユーザー ID による
 * 配送先住所の検索機能を提供する。主に {@link com.skishop.service.AddressService AddressService}
 * から利用され、ユーザーの住所管理（登録・一覧表示・削除）を支える。</p>
 *
 * <p>1 人のユーザーが複数の配送先住所を持つことができるため、ユーザー ID での検索は
 * 複数件を返す可能性がある。</p>
 *
 * @see Address
 * @see com.skishop.service.AddressService
 */
public interface AddressRepository extends JpaRepository<Address, String> {

    /**
     * 指定されたユーザー ID に紐づく全配送先住所を検索する。
     *
     * <p>マイページの住所一覧表示やチェックアウト時の配送先選択で使用される。
     * ユーザーは複数の配送先住所を登録できるため、結果は 0 件以上のリストとなる。</p>
     *
     * @param userId 検索対象のユーザー ID（null 不可）
     * @return 該当ユーザーの配送先住所リスト。存在しない場合は空リスト
     */
    List<Address> findByUserId(String userId);
}
