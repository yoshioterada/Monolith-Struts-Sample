package com.skishop.domain.point;

public class PointAccount {
  private String id;
  private String userId;
  private int balance;
  private int lifetimeEarned;
  private int lifetimeRedeemed;

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

  public int getBalance() {
    return balance;
  }

  public void setBalance(int balance) {
    this.balance = balance;
  }

  public int getLifetimeEarned() {
    return lifetimeEarned;
  }

  public void setLifetimeEarned(int lifetimeEarned) {
    this.lifetimeEarned = lifetimeEarned;
  }

  public int getLifetimeRedeemed() {
    return lifetimeRedeemed;
  }

  public void setLifetimeRedeemed(int lifetimeRedeemed) {
    this.lifetimeRedeemed = lifetimeRedeemed;
  }
}
