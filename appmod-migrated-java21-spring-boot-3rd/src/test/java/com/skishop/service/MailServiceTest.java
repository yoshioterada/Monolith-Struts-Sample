package com.skishop.service;

import com.skishop.model.EmailQueue;
import com.skishop.model.Order;
import com.skishop.repository.EmailQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private EmailQueueRepository emailQueueRepository;

    @Mock
    private EmailQueueStatusService emailQueueStatusService;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private MailService mailService;

    @Test
    @DisplayName("メールをキューに追加した場合、PENDINGステータスで保存される")
    void should_enqueueMail_when_enqueueCalledWithValidParams() {
        // Arrange
        when(emailQueueRepository.save(any(EmailQueue.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        mailService.enqueue("to@example.com", "Subject", "Body text");

        // Assert
        var captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(emailQueueRepository).save(captor.capture());
        assertThat(captor.getValue().getToAddr()).isEqualTo("to@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Subject");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(0);
        assertThat(captor.getValue().getId()).isNotNull();
    }

    @Test
    @DisplayName("パスワードリセットメールをキューに追加できる")
    void should_enqueuePasswordReset_when_called() {
        // Arrange
        when(emailQueueRepository.save(any(EmailQueue.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        mailService.enqueuePasswordReset("user@example.com", "reset-token-123");

        // Assert
        var captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(emailQueueRepository).save(captor.capture());
        assertThat(captor.getValue().getToAddr()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Password reset");
        assertThat(captor.getValue().getBody()).contains("reset-token-123");
    }

    @Test
    @DisplayName("注文確認メールをキューに追加できる")
    void should_enqueueOrderConfirmation_when_called() {
        // Arrange
        var order = new Order();
        order.setOrderNumber("ORD-001");
        order.setTotalAmount(new BigDecimal("15000"));
        when(emailQueueRepository.save(any(EmailQueue.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        mailService.enqueueOrderConfirmation("buyer@example.com", order);

        // Assert
        var captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(emailQueueRepository).save(captor.capture());
        assertThat(captor.getValue().getToAddr()).isEqualTo("buyer@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Order confirmation");
        assertThat(captor.getValue().getBody()).contains("ORD-001");
    }

    @Test
    @DisplayName("注文がnullの場合でも注文確認メールをキューに追加できる")
    void should_enqueueOrderConfirmation_when_orderIsNull() {
        // Arrange
        when(emailQueueRepository.save(any(EmailQueue.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        mailService.enqueueOrderConfirmation("buyer@example.com", null);

        // Assert
        var captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(emailQueueRepository).save(captor.capture());
        assertThat(captor.getValue().getToAddr()).isEqualTo("buyer@example.com");
    }

    @Test
    @DisplayName("キューにPENDINGメールがある場合、送信処理が実行される")
    void should_sendMail_when_pendingMailInQueue() {
        // Arrange
        var mail = new EmailQueue();
        mail.setId("eq-1");
        mail.setToAddr("recv@example.com");
        mail.setSubject("Test");
        mail.setBody("Test body");
        mail.setStatus("PENDING");
        mail.setRetryCount(0);
        mail.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(emailQueueStatusService.fetchPendingBatch(50))
                .thenReturn(List.of(mail));

        // Act
        mailService.processQueue();

        // Assert
        verify(javaMailSender).send(any(SimpleMailMessage.class));
        verify(emailQueueStatusService).markSent("eq-1");
    }

    @Test
    @DisplayName("送信失敗した場合、リトライまたは失敗処理が呼ばれる")
    void should_incrementRetryCount_when_sendFails() {
        // Arrange
        var mail = new EmailQueue();
        mail.setId("eq-2");
        mail.setToAddr("recv@example.com");
        mail.setSubject("Test");
        mail.setBody("Body");
        mail.setStatus("PENDING");
        mail.setRetryCount(0);
        mail.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(emailQueueStatusService.fetchPendingBatch(50))
                .thenReturn(List.of(mail));
        doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));

        // Act
        mailService.processQueue();

        // Assert
        verify(emailQueueStatusService).markRetryOrFailed(eq("eq-2"), eq(0), eq(5), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("リトライ上限超過時もmarkRetryOrFailedが呼ばれる")
    void should_setFailed_when_retryCountExceedsMax() {
        // Arrange
        var mail = new EmailQueue();
        mail.setId("eq-3");
        mail.setToAddr("recv@example.com");
        mail.setSubject("Test");
        mail.setBody("Body");
        mail.setStatus("PENDING");
        mail.setRetryCount(4);
        mail.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(emailQueueStatusService.fetchPendingBatch(50))
                .thenReturn(List.of(mail));
        doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));

        // Act
        mailService.processQueue();

        // Assert
        verify(emailQueueStatusService).markRetryOrFailed(eq("eq-3"), eq(4), eq(5), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("スケジュール時刻が未来の場合、送信処理をスキップする")
    void should_skipMail_when_scheduledAtIsFuture() {
        // Arrange
        var mail = new EmailQueue();
        mail.setId("eq-4");
        mail.setToAddr("recv@example.com");
        mail.setSubject("Test");
        mail.setBody("Body");
        mail.setStatus("PENDING");
        mail.setRetryCount(0);
        mail.setScheduledAt(LocalDateTime.now().plusHours(1));
        when(emailQueueStatusService.fetchPendingBatch(50))
                .thenReturn(List.of(mail));

        // Act
        mailService.processQueue();

        // Assert
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(emailQueueStatusService, never()).markSent(anyString());
    }
}
