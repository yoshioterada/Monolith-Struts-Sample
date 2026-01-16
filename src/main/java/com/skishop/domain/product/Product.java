package com.skishop.domain.product;

import java.util.Date;
import java.math.BigDecimal;
import java.util.Locale;
import java.text.NumberFormat;

public class Product {
  private String id;
  private String name;
  private String brand;
  private String description;
  private String categoryId;
  private String sku;
  private String status;
  private Date createdAt;
  private Date updatedAt;
  private BigDecimal regularPrice;
  private BigDecimal salePrice;
  private String currencyCode;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
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

  public BigDecimal getEffectivePrice() {
    return salePrice != null ? salePrice : regularPrice;
  }

  public String getPriceDisplay() {
    BigDecimal price = getEffectivePrice();
    if (price == null) {
      return "-";
    }
    try {
      NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("ja", "JP"));
      return nf.format(price);
    } catch (Exception e) {
      return price.toPlainString();
    }
  }
}
