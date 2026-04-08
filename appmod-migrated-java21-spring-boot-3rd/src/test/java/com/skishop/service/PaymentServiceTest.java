package com.skishop.service;

import com.skishop.dto.request.PaymentInfo;
import com.skishop.dto.response.PaymentResult;
import com.skishop.model.Payment;
import com.skishop.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    /** Luhn-valid test card number (4111111111111111) */
    private static final String VALID_CARD = "4111111111111111";

    private PaymentInfo validPaymentInfo() {
        return new PaymentInfo("CREDIT_CARD", VALID_CARD, "12", "2030", "123", "1234567");
    }

    @Test
    @DisplayName("有効なカード情報の場合、支払い承認に成功する")
    void should_returnSuccess_when_validCardInfo() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var info = validPaymentInfo();

        // Act
        var result = paymentService.authorize(info, BigDecimal.valueOf(10000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Success.class);
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("AUTHORIZED");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("PaymentInfoがnullの場合、支払い失敗を返す")
    void should_returnFailure_when_paymentInfoIsNull() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = paymentService.authorize(null, BigDecimal.valueOf(5000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Failure.class);
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("無効なカード番号の場合、支払い失敗を返す")
    void should_returnFailure_when_invalidCardNumber() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var info = new PaymentInfo("CREDIT_CARD", "1234567890123456", "12", "2030", "123", "123");

        // Act
        var result = paymentService.authorize(info, BigDecimal.valueOf(5000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Failure.class);
    }

    @Test
    @DisplayName("カード番号がnullの場合、支払い失敗を返す")
    void should_returnFailure_when_cardNumberIsNull() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var info = new PaymentInfo("CREDIT_CARD", null, "12", "2030", "123", "123");

        // Act
        var result = paymentService.authorize(info, BigDecimal.valueOf(5000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Failure.class);
    }

    @Test
    @DisplayName("有効期限切れカードの場合、支払い失敗を返す")
    void should_returnFailure_when_cardExpired() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var info = new PaymentInfo("CREDIT_CARD", VALID_CARD, "01", "2020", "123", "123");

        // Act
        var result = paymentService.authorize(info, BigDecimal.valueOf(5000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Failure.class);
    }

    @Test
    @DisplayName("カード番号が短すぎる場合、支払い失敗を返す")
    void should_returnFailure_when_cardNumberTooShort() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var info = new PaymentInfo("CREDIT_CARD", "12345", "12", "2030", "123", "123");

        // Act
        var result = paymentService.authorize(info, BigDecimal.valueOf(5000), "cart-1", "order-1");

        // Assert
        assertThat(result).isInstanceOf(PaymentResult.Failure.class);
    }

    @Test
    @DisplayName("注文の支払いを無効化した場合、ステータスがVOIDになる")
    void should_setStatusVoid_when_voidPayment() {
        // Arrange
        var payment = new Payment();
        payment.setId("pay-1");
        payment.setStatus("AUTHORIZED");
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        paymentService.voidPayment("order-1");

        // Assert
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("VOID");
    }

    @Test
    @DisplayName("注文の支払いを払い戻した場合、ステータスがREFUNDEDになる")
    void should_setStatusRefunded_when_refundPayment() {
        // Arrange
        var payment = new Payment();
        payment.setId("pay-2");
        payment.setStatus("AUTHORIZED");
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("order-2")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        paymentService.refundPayment("order-2");

        // Assert
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("支払いが存在しない注文を無効化しても例外が発生しない")
    void should_doNothing_when_voidPaymentWithNoPayment() {
        // Arrange
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("order-3")).thenReturn(Optional.empty());

        // Act
        paymentService.voidPayment("order-3");

        // Assert
        verify(paymentRepository, never()).save(any());
    }
}
