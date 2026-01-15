package com.skishop.dao.coupon;

import com.skishop.domain.coupon.Coupon;

public interface CouponDao {
  Coupon findByCode(String code);

  void incrementUsedCount(String couponId);

  void decrementUsedCount(String couponId);
}
