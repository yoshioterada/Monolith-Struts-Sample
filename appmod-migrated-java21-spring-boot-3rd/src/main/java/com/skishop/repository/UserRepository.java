package com.skishop.repository;

import com.skishop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link User} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code users} テーブルに対する CRUD 操作および、メールアドレスやステータスによる
 * ユーザー検索機能を提供する。主に
 * {@link com.skishop.service.AuthService AuthService}、
 * {@link com.skishop.service.UserService UserService}、および
 * {@link com.skishop.service.CustomUserDetailsService CustomUserDetailsService}
 * から利用され、認証・認可・ユーザー管理の中核を担う。</p>
 *
 * <p>{@link User} はシステムのルートエンティティの一つであり、
 * {@link com.skishop.model.Address Address}、{@link com.skishop.model.Order Order}、
 * {@link com.skishop.model.PointAccount PointAccount} など多くのエンティティが
 * ユーザーに紐づいている。</p>
 *
 * @see User
 * @see com.skishop.service.AuthService
 * @see com.skishop.service.UserService
 * @see com.skishop.service.CustomUserDetailsService
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 指定されたメールアドレスでユーザーを検索する。
     *
     * <p>ログイン認証時に {@link com.skishop.service.CustomUserDetailsService CustomUserDetailsService}
     * から呼び出されるほか、ユーザー登録時のメールアドレス重複チェックにも使用される。
     * メールアドレスは一意制約があるため、結果は 0 件または 1 件となる。</p>
     *
     * @param email 検索対象のメールアドレス（null 不可）
     * @return 該当ユーザーを含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<User> findByEmail(String email);

    /**
     * 指定されたステータスのユーザーを検索する。
     *
     * <p>管理画面でのユーザー一覧表示（有効ユーザー・ロック済みユーザーの絞り込み）や、
     * バッチ処理での対象ユーザー抽出に使用される。</p>
     *
     * @param status 検索対象のユーザーステータス（例: {@code "ACTIVE"}, {@code "LOCKED"}, {@code "INACTIVE"}）
     * @return 該当ステータスのユーザーリスト。存在しない場合は空リスト
     */
    List<User> findByStatus(String status);

    /**
     * 指定されたメールアドレスのユーザーが存在するかを確認する。
     *
     * <p>ユーザー登録時のメールアドレス重複チェックに使用される。</p>
     *
     * @param email 検索対象のメールアドレス（null 不可）
     * @return ユーザーが存在する場合 {@code true}
     */
    boolean existsByEmail(String email);
}
