package com.skishop.service.auth;

import com.skishop.domain.user.User;

public class AuthResult {
  private boolean success;
  private String message;
  private User user;

  public static AuthResult success(User user) {
    AuthResult result = new AuthResult();
    result.success = true;
    result.user = user;
    return result;
  }

  public static AuthResult failure(String message) {
    AuthResult result = new AuthResult();
    result.success = false;
    result.message = message;
    return result;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public User getUser() {
    return user;
  }
}
