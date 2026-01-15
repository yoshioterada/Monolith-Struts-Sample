package com.skishop.service.payment;

public class PaymentResult {
  private boolean success;
  private String paymentId;
  private String status;
  private String message;

  public static PaymentResult success(String paymentId, String status) {
    PaymentResult result = new PaymentResult();
    result.success = true;
    result.paymentId = paymentId;
    result.status = status;
    return result;
  }

  public static PaymentResult failure(String status, String message) {
    PaymentResult result = new PaymentResult();
    result.success = false;
    result.status = status;
    result.message = message;
    return result;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
