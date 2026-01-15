package com.skishop.dao;

import com.skishop.dao.coupon.CouponDao;
import com.skishop.dao.coupon.CouponDaoImpl;
import com.skishop.domain.coupon.Coupon;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CouponDaoTest extends DaoTestBase {
  private CouponDao couponDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    couponDao = new CouponDaoImpl();
  }

  @Test
  public void testFindByCodeAndUpdateUsage() {
    Coupon coupon = couponDao.findByCode("SAVE10");
    Assert.assertNotNull(coupon);
    Assert.assertEquals("coupon-1", coupon.getId());

    couponDao.incrementUsedCount("coupon-1");
    Coupon updated = couponDao.findByCode("SAVE10");
    Assert.assertEquals(1, updated.getUsedCount());
  }
}
