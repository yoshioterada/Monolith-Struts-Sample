package com.skishop.domain.product;

import java.math.BigDecimal;
import java.util.Date;

public class Price {
  private String id;
  private String productId;
  private BigDecimal regularPrice;
  private BigDecimal salePrice;
  private String currencyCode;
  private Date saleStartDate;
  private Date saleEndDate;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public BigDecimal getRegularPrice() {
    return regularPrice;
  }

  public void setRegularPrice(BigDecimal regularPrice) {
    this.regularPrice = regularPrice;
  }

  public BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(BigDecimal salePrice) {
    this.salePrice = salePrice;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public Date getSaleStartDate() {
    return saleStartDate;
  }

  public void setSaleStartDate(Date saleStartDate) {
    this.saleStartDate = saleStartDate;
  }

  public Date getSaleEndDate() {
    return saleEndDate;
  }

  public void setSaleEndDate(Date saleEndDate) {
    this.saleEndDate = saleEndDate;
  }
}
