package com.skishop.service;

import com.skishop.model.EmailQueue;
import com.skishop.model.Order;
import com.skishop.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * メール送信キューサービス。
 *
 * <p>メール送信をキュー方式で非同期に処理する。メールは即時送信ではなく、
 * DB のキューテーブル（{@code email_queue}）に登録され、
 * 30 秒間隔のスケジューラによってバッチ送信される。</p>
 *
 * <p>リトライ機能:</p>
 * <ul>
 *   <li>送信失敗時は最大 {@value #MAX_RETRY} 回まで指数バックオフでリトライする</li>
 *   <li>リトライ上限に達した場合はステータスを {@code FAILED} に設定する</li>
 * </ul>
 *
 * <p>ステータス遷移: {@code PENDING} → {@code SENT} / {@code FAILED}</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link EmailQueueRepository} — メールキューの永続化</li>
 *   <li>{@link EmailQueueStatusService} — ステータス更新（トランザクション分離）</li>
 *   <li>{@link JavaMailSender} — SMTP メール送信</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see UserService#createPasswordResetToken
 * @see EmailQueueRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 50;
    private static final String STATUS_PENDING = "PENDING";

    private final EmailQueueRepository emailQueueRepository;
    private final EmailQueueStatusService emailQueueStatusService;
    private final JavaMailSender javaMailSender;

    /**
     * メールをキューに登録する。
     *
     * <p>即時送信ではなく、DB のキューテーブルに {@code PENDING} ステータスで保存する。
     * 実際の送信は {@link #processQueue()} スケジューラが処理する。
     * チェックアウトのトランザクション内で呼び出される場合、
     * 注文確定と同じトランザクションでキューに追加される。</p>
     *
     * @param to      宛先メールアドレス
     * @param subject メール件名
     * @param body    メール本文
     */
    @Transactional
    public void enqueue(String to, String subject, String body) {
        var mail = new EmailQueue();
        mail.setId(UUID.randomUUID().toString());
        mail.setToAddr(to);
        mail.setSubject(subject);
        mail.setBody(body);
        mail.setStatus(STATUS_PENDING);
        mail.setRetryCount(0);
        mail.setScheduledAt(LocalDateTime.now());
        emailQueueRepository.save(mail);
    }

    /**
     * パスワードリセットメールをキューに登録する。
     *
     * @param to    宛先メールアドレス
     * @param token パスワードリセットトークン
     * @see UserService#createPasswordResetToken
     */
    @Transactional
    public void enqueuePasswordReset(String to, String token) {
        String body = "Password reset token: " + token;
        enqueue(to, "Password reset", body);
    }

    /**
     * 注文確認メールをキューに登録する。
     *
     * <p>注文番号と合計金額を含む確認メールをキューに追加する。
     * チェックアウト処理（{@link CheckoutService#placeOrder}）のトランザクション内で呼び出される。</p>
     *
     * @param to    宛先メールアドレス
     * @param order 注文エンティティ（注文番号・合計金額をメール本文に含める）
     * @see CheckoutService#placeOrder
     */
    @Transactional
    public void enqueueOrderConfirmation(String to, Order order) {
        String orderNumber = order != null ? order.getOrderNumber() : "";
        String totalAmount = order != null && order.getTotalAmount() != null
                ? order.getTotalAmount().toString() : "";
        String body = "Thank you for your order.\nOrder: " + orderNumber
                + "\nTotal: " + totalAmount;
        enqueue(to, "Order confirmation", body);
    }

    /**
     * メールキューを処理し、保留中のメールを送信する。
     *
     * <p>30 秒間隔（{@code fixedDelay = 30000}）で自動実行されるスケジュールタスク。
     * {@code PENDING} ステータスかつスケジュール時刻を過ぎたメールを順次送信する。</p>
     *
     * <p>{@code @Transactional} は付与しない — SMTP I/O が DB コネクションを占有しないよう、
     * 個別のステータス更新は {@link EmailQueueStatusService} に委譲する。</p>
     */
    @Scheduled(fixedDelay = 30000)
    @SchedulerLock(name = "processEmailQueue", lockAtMostFor = "5m", lockAtLeastFor = "10s")
    public void processQueue() {
        try {
            List<EmailQueue> pending = emailQueueStatusService.fetchPendingBatch(BATCH_SIZE);
            LocalDateTime now = LocalDateTime.now();

            for (EmailQueue mail : pending) {
                if (mail.getScheduledAt() != null && mail.getScheduledAt().isAfter(now)) {
                    continue;
                }
                processSingleMail(mail, now);
            }
        } catch (Exception e) {
            log.error("Error processing mail queue: {}", e.getMessage(), e);
        }
    }

    private void processSingleMail(EmailQueue mail, LocalDateTime now) {
        try {
            send(mail);
            emailQueueStatusService.markSent(mail.getId());
        } catch (RuntimeException e) {
            log.error("Failed to send email id={}: {}", mail.getId(), e.getMessage(), e);
            emailQueueStatusService.markRetryOrFailed(
                    mail.getId(), mail.getRetryCount(), MAX_RETRY, e.getMessage(), now);
        }
    }

    private void send(EmailQueue mail) {
        var message = new SimpleMailMessage();
        message.setTo(mail.getToAddr());
        message.setSubject(mail.getSubject());
        message.setText(mail.getBody());
        javaMailSender.send(message);
    }
}
