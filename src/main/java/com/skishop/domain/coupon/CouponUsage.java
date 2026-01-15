package com.skishop.domain.coupon;

import java.math.BigDecimal;
import java.util.Date;

public class CouponUsage {
  private String id;
  private String couponId;
  private String userId;
  private String orderId;
  private BigDecimal discountApplied;
  private Date usedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCouponId() {
    return couponId;
  }

  public void setCouponId(String couponId) {
    this.couponId = couponId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public BigDecimal getDiscountApplied() {
    return discountApplied;
  }

  public void setDiscountApplied(BigDecimal discountApplied) {
    this.discountApplied = discountApplied;
  }

  public Date getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Date usedAt) {
    this.usedAt = usedAt;
  }
}
