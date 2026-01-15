package com.skishop.dao.payment;

import com.skishop.domain.payment.Payment;

public interface PaymentDao {
  void insert(Payment payment);

  Payment findByOrderId(String orderId);

  void updateStatus(String paymentId, String status);
}
