package com.skishop.service.auth;

import com.skishop.dao.user.UserDao;
import com.skishop.dao.user.UserDaoImpl;
import com.skishop.domain.user.User;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {
  private static final int HASH_ITERATIONS = 1000;
  private final UserDao userDao = new UserDaoImpl();

  public AuthResult authenticate(String email, String passwordRaw) {
    if (email == null || passwordRaw == null) {
      return AuthResult.failure("INVALID_INPUT");
    }
    User user = userDao.findByEmail(email);
    if (user == null) {
      return AuthResult.failure("USER_NOT_FOUND");
    }
    if (!matches(user, passwordRaw)) {
      return AuthResult.failure("INVALID_CREDENTIALS");
    }
    return AuthResult.success(user);
  }

  private boolean matches(User user, String passwordRaw) {
    String salt = user.getSalt();
    if (salt == null || salt.length() == 0) {
      return secureEquals(passwordRaw, user.getPasswordHash());
    }
    try {
      String hashed = hashPassword(passwordRaw, salt);
      return secureEquals(hashed, user.getPasswordHash());
    } catch (NoSuchAlgorithmException e) {
      return false;
    } catch (UnsupportedEncodingException e) {
      return false;
    }
  }

  private String hashPassword(String passwordRaw, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] saltBytes = salt.getBytes("UTF-8");
    byte[] input = concat(saltBytes, passwordRaw.getBytes("UTF-8"));
    for (int i = 0; i < HASH_ITERATIONS; i++) {
      input = digest.digest(input);
      digest.reset();
      input = concat(saltBytes, input);
    }
    return toHex(input);
  }

  private byte[] concat(byte[] first, byte[] second) {
    byte[] combined = new byte[first.length + second.length];
    System.arraycopy(first, 0, combined, 0, first.length);
    System.arraycopy(second, 0, combined, first.length, second.length);
    return combined;
  }

  private String toHex(byte[] data) {
    StringBuilder builder = new StringBuilder();
    for (byte value : data) {
      String hex = Integer.toHexString(value & 0xff);
      if (hex.length() == 1) {
        builder.append('0');
      }
      builder.append(hex);
    }
    return builder.toString();
  }

  private boolean secureEquals(String left, String right) {
    if (left == null || right == null || left.length() != right.length()) {
      return false;
    }
    int result = 0;
    char[] leftChars = left.toCharArray();
    char[] rightChars = right.toCharArray();
    int index = 0;
    for (char leftChar : leftChars) {
      result |= leftChar ^ rightChars[index++];
    }
    return result == 0;
  }
}
