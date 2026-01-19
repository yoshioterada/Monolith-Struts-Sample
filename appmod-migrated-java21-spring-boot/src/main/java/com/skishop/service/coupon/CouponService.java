package com.skishop.service.coupon;

import com.skishop.dao.coupon.CouponDao;
import com.skishop.dao.coupon.CouponUsageDao;
import com.skishop.domain.coupon.Coupon;
import com.skishop.domain.coupon.CouponUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CouponService {
  private final CouponDao couponDao;
  private final CouponUsageDao couponUsageDao;

  @Autowired
  public CouponService(CouponDao couponDao, CouponUsageDao couponUsageDao) {
    this.couponDao = couponDao;
    this.couponUsageDao = couponUsageDao;
  }

  public Coupon validateCoupon(String code, BigDecimal subtotal) {
    if (code == null || code.isBlank()) {
      return null;
    }
    Coupon coupon = couponDao.findByCode(code);
    if (coupon == null) {
      throw new IllegalArgumentException("Coupon not found");
    }
    if (!coupon.isActive()) {
      throw new IllegalArgumentException("Coupon inactive");
    }
    if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
      throw new IllegalArgumentException("Coupon usage limit reached");
    }
    if (coupon.getMinimumAmount() != null && subtotal.compareTo(coupon.getMinimumAmount()) < 0) {
      throw new IllegalArgumentException("Subtotal below minimum");
    }
    Date now = new Date();
    if (coupon.getExpiresAt() != null && coupon.getExpiresAt().before(now)) {
      throw new IllegalArgumentException("Coupon expired");
    }
    return coupon;
  }

  public List<Coupon> listActiveCoupons() {
    return couponDao.listActive();
  }

  public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
    if (coupon == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal discount;
    if ("PERCENT".equalsIgnoreCase(coupon.getCouponType())) {
      discount = amount.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    } else {
      discount = coupon.getDiscountValue();
    }
    if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
      discount = coupon.getMaximumDiscount();
    }
    if (discount.compareTo(amount) > 0) {
      discount = amount;
    }
    return discount;
  }

  public void markUsed(Coupon coupon, String userId, String orderId, BigDecimal discountAmount) {
    if (coupon == null) {
      return;
    }
    couponDao.incrementUsedCount(coupon.getId());
    CouponUsage usage = new CouponUsage();
    usage.setId(UUID.randomUUID().toString());
    usage.setCouponId(coupon.getId());
    usage.setUserId(userId);
    usage.setOrderId(orderId);
    usage.setDiscountApplied(discountAmount);
    usage.setUsedAt(new Date());
    couponUsageDao.insert(usage);
  }

  public void releaseUsage(String orderId) {
    CouponUsage usage = couponUsageDao.findByOrderId(orderId);
    if (usage != null) {
      couponDao.decrementUsedCount(usage.getCouponId());
      couponUsageDao.deleteByOrderId(orderId);
    }
  }
}
