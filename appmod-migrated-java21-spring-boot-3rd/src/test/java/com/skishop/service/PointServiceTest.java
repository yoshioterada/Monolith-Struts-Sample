package com.skishop.service;

import com.skishop.exception.BusinessException;
import com.skishop.model.PointAccount;
import com.skishop.model.PointTransaction;
import com.skishop.repository.PointAccountRepository;
import com.skishop.repository.PointTransactionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointAccountRepository pointAccountRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("ポイントが正しく計算される (1%)")
    void should_calculateCorrectPoints_when_called() {
        // Act
        int points = pointService.calculateAwardPoints(new BigDecimal("10000"));

        // Assert
        assertThat(points).isEqualTo(100);
    }

    @Test
    @DisplayName("null金額の場合、0ポイントが返される")
    void should_returnZero_when_amountIsNull() {
        // Act
        int points = pointService.calculateAwardPoints(null);

        // Assert
        assertThat(points).isZero();
    }

    @Test
    @DisplayName("ポイントが正しく付与される")
    void should_awardPoints_when_validOrder() {
        // Arrange
        var account = new PointAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBalance(500);
        account.setLifetimeEarned(500);
        account.setLifetimeRedeemed(0);
        when(pointAccountRepository.findByUserId("user-1")).thenReturn(Optional.of(account));
        when(pointAccountRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        int awarded = pointService.awardPoints("user-1", "order-1", new BigDecimal("10000"));

        // Assert
        assertThat(awarded).isEqualTo(100);
        assertThat(account.getBalance()).isEqualTo(600);
        assertThat(account.getLifetimeEarned()).isEqualTo(600);
    }

    @Test
    @DisplayName("ポイントが正しく消費される")
    void should_redeemPoints_when_sufficientBalance() {
        // Arrange
        var account = new PointAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBalance(500);
        account.setLifetimeRedeemed(0);
        when(pointTransactionRepository.sumExpiredAmount(eq("user-1"), any())).thenReturn(0);
        when(pointTransactionRepository.bulkExpire(eq("user-1"), any())).thenReturn(0);
        when(pointAccountRepository.findByUserId("user-1")).thenReturn(Optional.of(account));
        when(pointAccountRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        pointService.redeemPoints("user-1", 200, "order-1");

        // Assert
        assertThat(account.getBalance()).isEqualTo(300);
        assertThat(account.getLifetimeRedeemed()).isEqualTo(200);
    }

    @Test
    @DisplayName("残高不足の場合、例外をスローする")
    void should_throwException_when_insufficientPoints() {
        // Arrange
        var account = new PointAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBalance(100);
        when(pointTransactionRepository.sumExpiredAmount(eq("user-1"), any())).thenReturn(0);
        when(pointTransactionRepository.bulkExpire(eq("user-1"), any())).thenReturn(0);
        when(pointAccountRepository.findByUserId("user-1")).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> pointService.redeemPoints("user-1", 500, "order-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient points");
    }

    @Test
    @DisplayName("ポイント返還が正しく行われる")
    void should_refundPoints_when_called() {
        // Arrange
        var account = new PointAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBalance(300);
        when(pointAccountRepository.findByUserId("user-1")).thenReturn(Optional.of(account));
        when(pointAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        pointService.refundPoints("user-1", 200, "order-1");

        // Assert
        assertThat(account.getBalance()).isEqualTo(500);

        var captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("REFUND");
        assertThat(captor.getValue().getAmount()).isEqualTo(200);
    }

    @Test
    @DisplayName("アカウントが存在しない場合、新規作成される")
    void should_createAccount_when_notExists() {
        // Arrange
        when(pointAccountRepository.findByUserId("user-new")).thenReturn(Optional.empty());
        when(pointAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        int points = pointService.awardPoints("user-new", "order-1", new BigDecimal("5000"));

        // Assert
        assertThat(points).isEqualTo(50);
    }

    @Test
    @DisplayName("ポイント取消で残高が0未満にならない")
    void should_notGoBelowZero_when_revokeExceedsBalance() {
        // Arrange
        var account = new PointAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBalance(50);
        when(pointAccountRepository.findByUserId("user-1")).thenReturn(Optional.of(account));
        when(pointAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pointTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        pointService.revokePoints("user-1", 100, "order-1");

        // Assert
        assertThat(account.getBalance()).isZero();
    }
}
