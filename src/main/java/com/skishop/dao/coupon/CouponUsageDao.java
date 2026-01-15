package com.skishop.dao.coupon;

import com.skishop.domain.coupon.CouponUsage;

public interface CouponUsageDao {
  void insert(CouponUsage usage);

  CouponUsage findByOrderId(String orderId);

  void deleteByOrderId(String orderId);
}
