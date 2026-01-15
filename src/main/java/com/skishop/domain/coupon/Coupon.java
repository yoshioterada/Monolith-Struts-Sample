package com.skishop.domain.coupon;

import java.math.BigDecimal;
import java.util.Date;

public class Coupon {
  private String id;
  private String campaignId;
  private String code;
  private String couponType;
  private BigDecimal discountValue;
  private String discountType;
  private BigDecimal minimumAmount;
  private BigDecimal maximumDiscount;
  private int usageLimit;
  private int usedCount;
  private boolean active;
  private Date expiresAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCampaignId() {
    return campaignId;
  }

  public void setCampaignId(String campaignId) {
    this.campaignId = campaignId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getCouponType() {
    return couponType;
  }

  public void setCouponType(String couponType) {
    this.couponType = couponType;
  }

  public BigDecimal getDiscountValue() {
    return discountValue;
  }

  public void setDiscountValue(BigDecimal discountValue) {
    this.discountValue = discountValue;
  }

  public String getDiscountType() {
    return discountType;
  }

  public void setDiscountType(String discountType) {
    this.discountType = discountType;
  }

  public BigDecimal getMinimumAmount() {
    return minimumAmount;
  }

  public void setMinimumAmount(BigDecimal minimumAmount) {
    this.minimumAmount = minimumAmount;
  }

  public BigDecimal getMaximumDiscount() {
    return maximumDiscount;
  }

  public void setMaximumDiscount(BigDecimal maximumDiscount) {
    this.maximumDiscount = maximumDiscount;
  }

  public int getUsageLimit() {
    return usageLimit;
  }

  public void setUsageLimit(int usageLimit) {
    this.usageLimit = usageLimit;
  }

  public int getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(int usedCount) {
    this.usedCount = usedCount;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }
}
