package com.skishop.repository;

import com.skishop.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User createUser(String id, String email, String status) {
        var user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("user_" + id);
        user.setPasswordHash("{bcrypt}$2a$10$dummy");
        user.setSalt("salt_" + id);
        user.setStatus(status);
        user.setRole("USER");
        return user;
    }

    @Test
    @DisplayName("メールアドレスでユーザーを検索した場合、対応するユーザーを返す")
    void should_returnUser_when_emailExists() {
        // Arrange
        var user = createUser("u-1", "alice@example.com", "ACTIVE");
        entityManager.persistAndFlush(user);

        // Act
        var result = userRepository.findByEmail("alice@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo("u-1");
        assertThat(result.orElseThrow().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索した場合、空のOptionalを返す")
    void should_returnEmpty_when_emailNotFound() {
        // Arrange (nothing persisted)

        // Act
        var result = userRepository.findByEmail("notexist@example.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ステータスでユーザーを検索した場合、一致するユーザーリストを返す")
    void should_returnUsers_when_statusMatches() {
        // Arrange
        var active = createUser("u-2", "bob@example.com", "ACTIVE");
        var locked = createUser("u-3", "carol@example.com", "LOCKED");
        entityManager.persistAndFlush(active);
        entityManager.persistAndFlush(locked);

        // Act
        List<User> activeUsers = userRepository.findByStatus("ACTIVE");
        List<User> lockedUsers = userRepository.findByStatus("LOCKED");

        // Assert
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("bob@example.com");
        assertThat(lockedUsers).hasSize(1);
        assertThat(lockedUsers.get(0).getEmail()).isEqualTo("carol@example.com");
    }

    @Test
    @DisplayName("該当ステータスのユーザーが存在しない場合、空のリストを返す")
    void should_returnEmptyList_when_noUserWithStatus() {
        // Arrange
        var user = createUser("u-4", "dave@example.com", "ACTIVE");
        entityManager.persistAndFlush(user);

        // Act
        List<User> suspended = userRepository.findByStatus("SUSPENDED");

        // Assert
        assertThat(suspended).isEmpty();
    }

    @Test
    @DisplayName("ユーザーを保存および取得できる")
    void should_saveAndFind_when_userPersisted() {
        // Arrange
        var user = createUser("u-5", "eve@example.com", "ACTIVE");

        // Act
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();
        var found = userRepository.findById("u-5");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getEmail()).isEqualTo("eve@example.com");
        assertThat(found.orElseThrow().getCreatedAt()).isNotNull();
        assertThat(found.orElseThrow().getUpdatedAt()).isNotNull();
    }
}
