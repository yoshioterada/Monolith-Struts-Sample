package com.skishop.repository;

import com.skishop.model.EmailQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * {@link EmailQueue} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code email_queue} テーブルに対する CRUD 操作および、ステータスによるメールキューの
 * 検索機能を提供する。主に {@link com.skishop.service.MailService MailService} から利用され、
 * 受注確認メールやパスワードリセットメールなどの非同期メール送信を支える。</p>
 *
 * <p>メールは注文確定トランザクションと同一トランザクション内でキューに追加され、
 * バックグラウンドプロセスによって送信予定時刻順に処理される。</p>
 *
 * @see EmailQueue
 * @see com.skishop.service.MailService
 */
public interface EmailQueueRepository extends JpaRepository<EmailQueue, String> {

    /**
     * 指定されたステータスのメールキューを検索する。
     *
     * <p>特定ステータス（例: {@code "PENDING"}, {@code "SENT"}, {@code "FAILED"}）の
     * メールを一覧取得する際に使用される。管理画面でのメール送信状況の確認にも利用できる。</p>
     *
     * @param status 検索対象のステータス（例: {@code "PENDING"}, {@code "SENT"}, {@code "FAILED"}）
     * @return 該当ステータスのメールキューリスト。存在しない場合は空リスト
     */
    List<EmailQueue> findByStatus(String status);

    /**
     * 指定されたステータスのメールキューを送信予定時刻の昇順で検索する。
     *
     * <p>メール送信バッチ処理で、未送信（{@code "PENDING"}）のメールを
     * 送信予定時刻が早い順に取得するために使用される。
     * FIFO（先入れ先出し）方式でメールを処理することを保証する。</p>
     *
     * @param status 検索対象のステータス（例: {@code "PENDING"}）
     * @return 送信予定時刻の昇順でソートされたメールキューリスト。存在しない場合は空リスト
     */
    List<EmailQueue> findByStatusOrderByScheduledAtAsc(String status);

    /**
     * 指定ステータスのメールキューをページネーション付きで取得する。
     *
     * @param status   検索対象のステータス
     * @param pageable ページネーション情報
     * @return 送信予定時刻の昇順でソートされたメールキューリスト
     */
    List<EmailQueue> findByStatusOrderByScheduledAtAsc(String status, Pageable pageable);
}
