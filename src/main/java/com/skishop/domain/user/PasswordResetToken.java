package com.skishop.domain.user;

import java.util.Date;

public class PasswordResetToken {
  private String id;
  private String userId;
  private String token;
  private Date expiresAt;
  private Date usedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Date getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Date usedAt) {
    this.usedAt = usedAt;
  }
}
