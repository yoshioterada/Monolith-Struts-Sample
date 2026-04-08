package com.skishop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * SMTP サーバーへの接続ヘルスチェック。
 *
 * <p>{@code /actuator/health} エンドポイントに SMTP 接続状態を含める。
 * メール送信基盤の監視に使用する。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpHealthIndicator implements HealthIndicator {

    private final JavaMailSender javaMailSender;

    @Override
    public Health health() {
        try {
            if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
                mailSender.testConnection();
            }
            return Health.up().build();
        } catch (Exception e) {
            log.warn("SMTP health check failed: {}", e.getMessage());
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
