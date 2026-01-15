package com.skishop.domain.order;

import java.util.Date;

public class Shipment {
  private String id;
  private String orderId;
  private String carrier;
  private String trackingNumber;
  private String status;
  private Date shippedAt;
  private Date deliveredAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getCarrier() {
    return carrier;
  }

  public void setCarrier(String carrier) {
    this.carrier = carrier;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getShippedAt() {
    return shippedAt;
  }

  public void setShippedAt(Date shippedAt) {
    this.shippedAt = shippedAt;
  }

  public Date getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Date deliveredAt) {
    this.deliveredAt = deliveredAt;
  }
}
