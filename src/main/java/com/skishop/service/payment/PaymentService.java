package com.skishop.service.payment;

import com.skishop.dao.payment.PaymentDao;
import com.skishop.dao.payment.PaymentDaoImpl;
import com.skishop.domain.payment.Payment;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class PaymentService {
  private final PaymentDao paymentDao = new PaymentDaoImpl();

  public PaymentResult authorize(PaymentInfo info, BigDecimal amount, String cartId, String orderId) {
    boolean success = isPaymentAllowed(info);
    String status = success ? "AUTHORIZED" : "FAILED";
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID().toString());
    payment.setOrderId(orderId);
    payment.setCartId(cartId);
    payment.setAmount(amount);
    payment.setCurrency("JPY");
    payment.setStatus(status);
    payment.setPaymentIntentId("PAY-" + System.currentTimeMillis());
    payment.setCreatedAt(new Date());
    paymentDao.insert(payment);
    if (success) {
      return PaymentResult.success(payment.getId(), status);
    }
    return PaymentResult.failure(status, "Payment declined");
  }

  public void voidPayment(String orderId) {
    updateStatusByOrderId(orderId, "VOID");
  }

  public void refundPayment(String orderId) {
    updateStatusByOrderId(orderId, "REFUNDED");
  }

  private void updateStatusByOrderId(String orderId, String status) {
    Payment payment = paymentDao.findByOrderId(orderId);
    if (payment != null) {
      paymentDao.updateStatus(payment.getId(), status);
    }
  }

  private boolean isPaymentAllowed(PaymentInfo info) {
    if (info == null) {
      return false;
    }
    if (!isValidCardNumber(info.getCardNumber())) {
      return false;
    }
    return isExpiryValid(info.getCardExpYear(), info.getCardExpMonth());
  }

  private boolean isValidCardNumber(String cardNumber) {
    if (cardNumber == null) {
      return false;
    }
    String number = cardNumber.trim();
    if (number.length() < 12 || number.length() > 19) {
      return false;
    }
    int sum = 0;
    boolean alternate = false;
    for (int i = number.length() - 1; i >= 0; i--) {
      char ch = number.charAt(i);
      if (ch < '0' || ch > '9') {
        return false;
      }
      int digit = ch - '0';
      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }
      sum += digit;
      alternate = !alternate;
    }
    return sum % 10 == 0;
  }

  private boolean isExpiryValid(String expYear, String expMonth) {
    if (expYear == null || expMonth == null) {
      return false;
    }
    int year;
    int month;
    try {
      year = Integer.parseInt(expYear);
      month = Integer.parseInt(expMonth);
    } catch (NumberFormatException e) {
      return false;
    }
    if (month < 1 || month > 12) {
      return false;
    }
    if (year < 100) {
      year += 2000;
    }
    Calendar cal = Calendar.getInstance();
    int currentYear = cal.get(Calendar.YEAR);
    int currentMonth = cal.get(Calendar.MONTH) + 1;
    if (year < currentYear) {
      return false;
    }
    return year > currentYear || month >= currentMonth;
  }
}
