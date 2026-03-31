package com.skishop.repository;

import com.skishop.model.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link SecurityLog} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code security_logs} テーブルに対する CRUD 操作および、ユーザー ID とイベント種別による
 * セキュリティイベントの集計機能を提供する。主に
 * {@link com.skishop.service.AuthService AuthService} から利用され、
 * ログイン試行の記録・アカウントロック判定を支える。</p>
 *
 * <p>セキュリティログには認証成功・失敗、パスワード変更、アカウントロックなどの
 * イベントが記録される。PII（個人情報）はログに含めず、IP アドレスと
 * イベント種別のみを記録する。</p>
 *
 * @see SecurityLog
 * @see com.skishop.service.AuthService
 */
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {

    /**
     * 指定されたユーザー ID とイベント種別の組み合わせでセキュリティイベント件数を集計する。
     *
     * <p>ログイン失敗回数によるアカウントロック判定で主に使用される。
     * 例えば、ユーザーの {@code "LOGIN_FAILURE"} イベント数をカウントし、
     * 閾値（5 回など）を超えた場合にアカウントをロックする判定に活用される。</p>
     *
     * @param userId    集計対象のユーザー ID（null 不可）
     * @param eventType 集計対象のイベント種別（例: {@code "LOGIN_FAILURE"}, {@code "LOGIN_SUCCESS"}）
     * @return 該当するセキュリティイベントの件数（イベントがない場合は 0）
     */
    long countByUserIdAndEventType(String userId, String eventType);
}
