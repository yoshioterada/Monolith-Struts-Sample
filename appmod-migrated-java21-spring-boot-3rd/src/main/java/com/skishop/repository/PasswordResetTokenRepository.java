package com.skishop.repository;

import com.skishop.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link PasswordResetToken} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code password_reset_tokens} テーブルに対する CRUD 操作および、
 * トークン文字列によるパスワードリセットトークンの検索・削除機能を提供する。主に
 * {@link com.skishop.service.UserService UserService} から利用され、
 * パスワードリセットフロー（トークン発行・検証・使用後の削除）を支える。</p>
 *
 * <p>トークンには有効期限があり、リセット完了後または期限切れ時に削除される。
 * セキュリティ上、トークンは一度使用されたら無効化（削除）される必要がある。</p>
 *
 * @see PasswordResetToken
 * @see com.skishop.service.UserService
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    /**
     * 指定されたトークン文字列でパスワードリセットトークンを検索する。
     *
     * <p>パスワードリセット画面でユーザーがリセットリンクからアクセスした際に、
     * トークンの有効性（存在確認・有効期限チェック）を検証するために使用される。
     * トークンは一意であるため、結果は 0 件または 1 件となる。</p>
     *
     * @param token 検索対象のトークン文字列（null 不可）
     * @return 該当トークンを含む {@link Optional}。存在しない場合は {@link Optional#empty()}
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 指定されたトークン文字列に合致するパスワードリセットトークンを削除する。
     *
     * <p>パスワードリセット完了後に使用済みトークンを無効化するために使用される。
     * セキュリティ上、トークンの再利用を防止する目的がある。
     * {@code @Transactional} コンテキスト内で呼び出す必要がある。</p>
     *
     * @param token 削除対象のトークン文字列（null 不可）
     */
    void deleteByToken(String token);
}
