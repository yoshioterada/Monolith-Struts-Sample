package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.SecurityLog;
import com.skishop.model.User;
import com.skishop.repository.SecurityLogRepository;
import com.skishop.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityLogRepository securityLogRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("有効なメールアドレスでユーザーが見つかる場合、ユーザーを返す")
    void should_returnUser_when_emailExists() {
        // Arrange
        var user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        var result = authService.findByEmail("test@example.com");

        // Assert
        assertThat(result.getId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("存在しないメールアドレスの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_emailNotFound() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.findByEmail("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("ログイン成功時にセキュリティログを記録する")
    void should_recordSecurityLog_when_loginSuccess() {
        // Arrange & Act
        authService.recordLoginSuccess("user-1", "127.0.0.1", "TestAgent");

        // Assert
        var captor = ArgumentCaptor.forClass(SecurityLog.class);
        verify(securityLogRepository).save(captor.capture());
        var log = captor.getValue();
        assertThat(log.getUserId()).isEqualTo("user-1");
        assertThat(log.getEventType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("5回失敗した場合、アカウントがロックされる")
    void should_lockAccount_when_loginFailsFiveTimes() {
        // Arrange
        var user = new User();
        user.setId("user-1");
        user.setStatus("ACTIVE");
        when(securityLogRepository.countByUserIdAndEventType("user-1", "LOGIN_FAILURE"))
                .thenReturn(5L);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(securityLogRepository.save(any(SecurityLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        authService.recordLoginFailure("user-1", "127.0.0.1", "TestAgent");

        // Assert
        assertThat(user.getStatus()).isEqualTo("LOCKED");
    }

    @Test
    @DisplayName("アカウントがロックされている場合、trueを返す")
    void should_returnTrue_when_accountIsLocked() {
        // Arrange
        var user = new User();
        user.setId("user-1");
        user.setStatus("LOCKED");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // Act
        boolean result = authService.isAccountLocked("user-1");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("アカウントがアクティブの場合、falseを返す")
    void should_returnFalse_when_accountIsActive() {
        // Arrange
        var user = new User();
        user.setId("user-1");
        user.setStatus("ACTIVE");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // Act
        boolean result = authService.isAccountLocked("user-1");

        // Assert
        assertThat(result).isFalse();
    }
}
