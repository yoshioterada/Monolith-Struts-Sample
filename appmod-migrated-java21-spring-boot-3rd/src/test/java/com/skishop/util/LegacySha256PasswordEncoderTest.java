package com.skishop.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for LegacySha256PasswordEncoder.
 * Verifies hash$salt format parsing and correct matching logic.
 */
class LegacySha256PasswordEncoderTest {

    private final LegacySha256PasswordEncoder encoder = new LegacySha256PasswordEncoder();

    @Test
    @DisplayName("正しいパスワードとソルトでmatchesがtrueを返す")
    void should_returnTrue_when_passwordAndSaltMatch() {
        String salt = "testsalt123";
        String password = "mypassword";
        String hash = PasswordHasher.hash(password, salt);
        String storedHashWithSalt = hash + "$" + salt;

        assertThat(encoder.matches(password, storedHashWithSalt)).isTrue();
    }

    @Test
    @DisplayName("間違ったパスワードでmatchesがfalseを返す")
    void should_returnFalse_when_passwordDoesNotMatch() {
        String salt = "testsalt123";
        String hash = PasswordHasher.hash("correctpassword", salt);
        String storedHashWithSalt = hash + "$" + salt;

        assertThat(encoder.matches("wrongpassword", storedHashWithSalt)).isFalse();
    }

    @Test
    @DisplayName("$区切りがない文字列でmatchesがfalseを返す")
    void should_returnFalse_when_noSeparator() {
        assertThat(encoder.matches("password", "hashwithoutdollar")).isFalse();
    }

    @Test
    @DisplayName("storageがnullでmatchesがfalseを返す")
    void should_returnFalse_when_storedIsNull() {
        assertThat(encoder.matches("password", null)).isFalse();
    }

    @Test
    @DisplayName("rawPasswordがnullでmatchesがfalseを返す")
    void should_returnFalse_when_rawPasswordIsNull() {
        assertThat(encoder.matches(null, "hash$salt")).isFalse();
    }

    @Test
    @DisplayName("encode()はUnsupportedOperationExceptionをスローする")
    void should_throwUnsupportedOperation_when_encodeIsCalled() {
        assertThatThrownBy(() -> encoder.encode("anypassword"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
