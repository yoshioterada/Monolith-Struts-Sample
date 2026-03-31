package com.skishop.service;

import com.skishop.model.User;
import com.skishop.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("有効なメールアドレスでUserDetailsを取得できる")
    void should_returnUserDetails_when_emailExists() {
        // Arrange
        var user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("{bcrypt}$2a$10$hashedpw");
        user.setStatus("ACTIVE");
        user.setRole("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Act
        var result = customUserDetailsService.loadUserByUsername("user@example.com");

        // Assert
        assertThat(result.getUsername()).isEqualTo("user@example.com");
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("LOCKEDステータスのユーザーはアカウントがロックされる")
    void should_lockAccount_when_statusIsLocked() {
        // Arrange
        var user = new User();
        user.setEmail("locked@example.com");
        user.setPasswordHash("{bcrypt}$2a$10$hashedpw");
        user.setStatus("LOCKED");
        user.setRole("USER");
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user));

        // Act
        var result = customUserDetailsService.loadUserByUsername("locked@example.com");

        // Assert
        assertThat(result.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("存在しないメールアドレスの場合、UsernameNotFoundExceptionをスローする")
    void should_throwException_when_userNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("notfound@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("ADMINロールのユーザーはROLE_ADMINが設定される")
    void should_setAdminRole_when_userIsAdmin() {
        // Arrange
        var user = new User();
        user.setEmail("admin@example.com");
        user.setPasswordHash("{bcrypt}$2a$10$hashedpw");
        user.setStatus("ACTIVE");
        user.setRole("ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        // Act
        var result = customUserDetailsService.loadUserByUsername("admin@example.com");

        // Assert
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("パスワードアップグレード時にDBのパスワードハッシュが更新される")
    void should_updatePasswordHash_when_updatePasswordCalled() {
        // Arrange
        var user = new User();
        user.setEmail("upgrade@example.com");
        user.setPasswordHash("{sha256}oldhash$salt");
        when(userRepository.findByEmail("upgrade@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("upgrade@example.com");
        when(userDetails.getPassword()).thenReturn("{sha256}oldhash$salt");

        // Act
        var result = customUserDetailsService.updatePassword(userDetails, "{bcrypt}newhash");

        // Assert
        assertThat(result.getPassword()).isEqualTo("{bcrypt}newhash");
        verify(userRepository).save(any(User.class));
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}newhash");
    }
}
