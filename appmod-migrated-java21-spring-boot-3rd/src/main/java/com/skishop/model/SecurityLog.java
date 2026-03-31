package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるセキュリティ監査ログを表す JPA エンティティ。
 *
 * <p>{@code security_logs} テーブルにマッピングされ、ログイン成功・ログイン失敗・
 * アカウントロック・パスワード変更などのセキュリティイベントを記録する。
 * ユーザー ID・イベント種別・IP アドレス・User-Agent・詳細情報（JSON 形式）を保持する。</p>
 *
 * <p>{@code user_id} と {@code event_type} の複合インデックスが設定されており、
 * ログイン失敗回数の集計によるアカウントロック判定などに利用される。</p>
 *
 * <p><strong>注意:</strong> PII（個人情報）はこのログに記録しない。
 * メールアドレス全文・パスワード関連・住所情報の出力は禁止。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link User} — セキュリティイベントの対象ユーザー（N:1、{@code user_id} で参照）</li>
 * </ul>
 *
 * @see User
 */
@Entity
@Table(name = "security_logs", indexes = {
        @Index(name = "idx_security_logs_user_event", columnList = "user_id, event_type")
})
@Getter
@Setter
@NoArgsConstructor
public class SecurityLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "details_json", length = 2000)
    private String detailsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
