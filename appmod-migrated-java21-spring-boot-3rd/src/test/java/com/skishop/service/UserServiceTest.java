package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.PasswordResetToken;
import com.skishop.model.User;
import com.skishop.repository.PasswordResetTokenRepository;
import com.skishop.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("有効なIDでユーザーを取得した場合、ユーザーを返す")
    void should_returnUser_when_idExists() {
        // Arrange
        var user = new User();
        user.setId("u-1");
        user.setEmail("alice@example.com");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        // Act
        var result = userService.findById("u-1");

        // Assert
        assertThat(result.getId()).isEqualTo("u-1");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("存在しないIDでユーザーを取得した場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_userIdNotFound() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("有効なメールアドレスでユーザーを取得した場合、ユーザーを返す")
    void should_returnUser_when_emailExists() {
        // Arrange
        var user = new User();
        user.setEmail("bob@example.com");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        // Act
        var result = userService.findByEmail("bob@example.com");

        // Assert
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("存在しないメールアドレスの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_emailNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByEmail("notexist@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("IDなしでユーザー登録した場合、UUIDが自動設定される")
    void should_generateId_when_registerWithoutId() {
        // Arrange
        var user = new User();
        user.setEmail("new@example.com");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = userService.register(user);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("ステータスなしで登録した場合、ACTIVEが設定される")
    void should_setDefaultStatus_when_registerWithoutStatus() {
        // Arrange
        var user = new User();
        user.setId("u-existing");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.register(user);

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("パスワードを更新した場合、ハッシュとソルトが更新される")
    void should_updatePasswordHash_when_updatePasswordCalled() {
        // Arrange
        var user = new User();
        user.setId("u-2");
        when(userRepository.findById("u-2")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.updatePassword("u-2", "newHash", "newSalt");

        // Assert
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("newHash");
        assertThat(captor.getValue().getSalt()).isEqualTo("newSalt");
    }

    @Test
    @DisplayName("パスワードリセットトークンを作成した場合、トークンを返す")
    void should_returnToken_when_createPasswordResetToken() {
        // Arrange
        var user = new User();
        user.setId("u-3");
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        var token = userService.createPasswordResetToken("carol@example.com");

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("有効なリセットトークンを検証した場合、トークンを返す")
    void should_returnToken_when_validResetToken() {
        // Arrange
        var token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        // Act
        var result = userService.validateResetToken("valid-token");

        // Assert
        assertThat(result.getToken()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("有効期限切れのリセットトークンの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_tokenExpired() {
        // Arrange
        var token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> userService.validateResetToken("expired-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("使用済みのリセットトークンの場合、ResourceNotFoundExceptionをスローする")
    void should_throwException_when_tokenAlreadyUsed() {
        // Arrange
        var token = new PasswordResetToken();
        token.setToken("used-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsedAt(LocalDateTime.now().minusMinutes(10));
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> userService.validateResetToken("used-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
