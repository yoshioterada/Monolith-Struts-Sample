package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるメール送信キューを表す JPA エンティティ。
 *
 * <p>{@code email_queue} テーブルにマッピングされ、送信先アドレス・件名・本文・
 * ステータス・リトライ回数・最終エラーメッセージ・送信予定日時・送信完了日時を保持する。
 * 注文確定時にチェックアウトトランザクション内でキューに追加され（同一 TX）、
 * 非同期バッチ処理により実際のメール送信が行われる。</p>
 *
 * <p>ステータス遷移: {@code PENDING} → {@code SENDING} → {@code SENT} / {@code FAILED}</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — 受注確認メールなど、注文に関連するメール</li>
 *   <li>{@link User} — メールの送信先ユーザー</li>
 * </ul>
 *
 * @see Order
 * @see User
 */
@Entity
@Table(name = "email_queue")
@Getter
@Setter
@NoArgsConstructor
public class EmailQueue {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "to_addr", nullable = false, length = 255)
    private String toAddr;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
