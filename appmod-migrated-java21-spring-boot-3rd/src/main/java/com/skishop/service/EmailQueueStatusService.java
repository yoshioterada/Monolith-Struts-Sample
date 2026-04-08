package com.skishop.service;

import com.skishop.model.EmailQueue;
import com.skishop.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * メールキューのステータス管理サービス。
 *
 * <p>{@link MailService} から分離された DB 操作専用サービス。
 * Spring AOP のプロキシベースの {@code @Transactional} が内部メソッド呼び出しでは
 * 機能しない制約を回避するため、トランザクション境界を持つメソッドを別 Bean に配置する。</p>
 *
 * @see MailService#processQueue()
 */
@Service
@RequiredArgsConstructor
public class EmailQueueStatusService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final EmailQueueRepository emailQueueRepository;

    /**
     * 送信待ちメールをバッチサイズ分取得する。
     *
     * @param batchSize 取得件数上限
     * @return 送信予定時刻の昇順でソートされた PENDING メールリスト
     */
    @Transactional(readOnly = true)
    public List<EmailQueue> fetchPendingBatch(int batchSize) {
        return emailQueueRepository.findByStatusOrderByScheduledAtAsc(
                STATUS_PENDING, PageRequest.of(0, batchSize));
    }

    /**
     * メールを送信済みとしてマークする。
     *
     * @param mailId メール ID
     */
    @Transactional
    public void markSent(String mailId) {
        emailQueueRepository.findById(mailId).ifPresent(mail -> {
            mail.setStatus(STATUS_SENT);
            mail.setSentAt(LocalDateTime.now());
            emailQueueRepository.save(mail);
        });
    }

    /**
     * メールをリトライまたは失敗としてマークする。
     *
     * @param mailId            メール ID
     * @param currentRetryCount 現在のリトライ回数
     * @param maxRetry          最大リトライ回数
     * @param error             エラーメッセージ
     * @param now               現在時刻
     */
    @Transactional
    public void markRetryOrFailed(String mailId, int currentRetryCount, int maxRetry,
                                   String error, LocalDateTime now) {
        emailQueueRepository.findById(mailId).ifPresent(mail -> {
            int retryCount = currentRetryCount + 1;
            mail.setRetryCount(retryCount);
            mail.setLastError(error);
            if (retryCount >= maxRetry) {
                mail.setStatus(STATUS_FAILED);
            } else {
                // Exponential backoff: 2m, 4m, 8m, 16m, 32m
                mail.setScheduledAt(now.plusMinutes((long) Math.pow(2, retryCount)));
            }
            emailQueueRepository.save(mail);
        });
    }
}
