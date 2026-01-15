package com.skishop.service;

import com.skishop.common.dao.DataSourceLocator;
import com.skishop.dao.coupon.CouponDao;
import com.skishop.dao.coupon.CouponDaoImpl;
import com.skishop.dao.coupon.CouponUsageDao;
import com.skishop.dao.coupon.CouponUsageDaoImpl;
import com.skishop.dao.inventory.InventoryDao;
import com.skishop.dao.inventory.InventoryDaoImpl;
import com.skishop.dao.order.OrderDao;
import com.skishop.dao.order.OrderDaoImpl;
import com.skishop.dao.order.ReturnDao;
import com.skishop.dao.order.ReturnDaoImpl;
import com.skishop.dao.point.PointAccountDao;
import com.skishop.dao.point.PointAccountDaoImpl;
import com.skishop.dao.DaoTestBase;
import com.skishop.domain.coupon.Coupon;
import com.skishop.domain.coupon.CouponUsage;
import com.skishop.domain.inventory.Inventory;
import com.skishop.domain.order.Order;
import com.skishop.domain.order.Return;
import com.skishop.domain.point.PointAccount;
import com.skishop.service.order.OrderFacade;
import com.skishop.service.order.OrderFacadeImpl;
import com.skishop.service.payment.PaymentInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OrderFacadeTest extends DaoTestBase {
  private OrderFacade orderFacade;
  private CouponDao couponDao;
  private CouponUsageDao couponUsageDao;
  private PointAccountDao pointAccountDao;
  private InventoryDao inventoryDao;
  private OrderDao orderDao;
  private ReturnDao returnDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    orderFacade = new OrderFacadeImpl();
    couponDao = new CouponDaoImpl();
    couponUsageDao = new CouponUsageDaoImpl();
    pointAccountDao = new PointAccountDaoImpl();
    inventoryDao = new InventoryDaoImpl();
    orderDao = new OrderDaoImpl();
    returnDao = new ReturnDaoImpl();
    updateCouponExpiry("SAVE10");
    updatePointExpiry();
  }

  @Test
  public void testCheckoutWithCouponAndPoints() {
    PaymentInfo paymentInfo = createPaymentInfo();
    Order order = orderFacade.placeOrder("cart-1", "SAVE10", 100, paymentInfo, "u-1");
    Assert.assertNotNull(order);
    BigDecimal subtotal = new BigDecimal("50000");
    BigDecimal discount = new BigDecimal("5000");
    BigDecimal taxable = subtotal.subtract(discount).subtract(new BigDecimal("100"));
    BigDecimal tax = taxable.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
    BigDecimal expectedTotal = taxable.add(tax);
    Assert.assertEquals(0, order.getTotalAmount().compareTo(expectedTotal));

    Coupon coupon = couponDao.findByCode("SAVE10");
    Assert.assertEquals(1, coupon.getUsedCount());
    CouponUsage usage = couponUsageDao.findByOrderId(order.getId());
    Assert.assertNotNull(usage);

    PointAccount account = pointAccountDao.findByUserId("u-1");
    Assert.assertEquals(493, account.getBalance());

    Inventory inventory = inventoryDao.findByProductId("P001");
    Assert.assertEquals(1, inventory.getReservedQuantity());
  }

  @Test
  public void testCancelOrder() {
    PaymentInfo paymentInfo = createPaymentInfo();
    Order order = orderFacade.placeOrder("cart-1", "SAVE10", 100, paymentInfo, "u-1");
    Order cancelled = orderFacade.cancelOrder(order.getId(), "u-1");

    Assert.assertEquals("CANCELLED", cancelled.getStatus());
    Assert.assertEquals("VOID", cancelled.getPaymentStatus());

    Coupon coupon = couponDao.findByCode("SAVE10");
    Assert.assertEquals(0, coupon.getUsedCount());
    Assert.assertNull(couponUsageDao.findByOrderId(order.getId()));

    PointAccount account = pointAccountDao.findByUserId("u-1");
    Assert.assertEquals(100, account.getBalance());

    Inventory inventory = inventoryDao.findByProductId("P001");
    Assert.assertEquals(0, inventory.getReservedQuantity());
  }

  @Test
  public void testReturnOrder() {
    PaymentInfo paymentInfo = createPaymentInfo();
    Order order = orderFacade.placeOrder("cart-1", "SAVE10", 100, paymentInfo, "u-1");
    orderDao.updateStatus(order.getId(), "DELIVERED");

    Order returned = orderFacade.returnOrder(order.getId(), "u-1");
    Assert.assertEquals("RETURNED", returned.getStatus());
    Assert.assertEquals("REFUNDED", returned.getPaymentStatus());

    Coupon coupon = couponDao.findByCode("SAVE10");
    Assert.assertEquals(0, coupon.getUsedCount());

    List<Return> returns = returnDao.listByOrderId(order.getId());
    Assert.assertFalse(returns.isEmpty());

    PointAccount account = pointAccountDao.findByUserId("u-1");
    Assert.assertEquals(100, account.getBalance());

    Inventory inventory = inventoryDao.findByProductId("P001");
    Assert.assertEquals(0, inventory.getReservedQuantity());
  }

  private PaymentInfo createPaymentInfo() {
    PaymentInfo paymentInfo = new PaymentInfo();
    paymentInfo.setMethod("CARD");
    paymentInfo.setCardNumber("4111111111111111");
    paymentInfo.setCardExpMonth("12");
    paymentInfo.setCardExpYear("2030");
    paymentInfo.setCardCvv("123");
    paymentInfo.setBillingZip("160-0022");
    return paymentInfo;
  }

  private void updateCouponExpiry(String code) throws Exception {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DataSourceLocator.getInstance().getDataSource().getConnection();
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, 1);
      ps = con.prepareStatement("UPDATE coupons SET expires_at = ? WHERE code = ?");
      ps.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
      ps.setString(2, code);
      ps.executeUpdate();
    } finally {
      if (ps != null) {
        ps.close();
      }
      if (con != null) {
        con.close();
      }
    }
  }

  private void updatePointExpiry() throws Exception {
    Connection con = null;
    PreparedStatement ps = null;
    try {
      con = DataSourceLocator.getInstance().getDataSource().getConnection();
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, 30);
      ps = con.prepareStatement("UPDATE point_transactions SET expires_at = ?, is_expired = FALSE WHERE user_id = ?");
      ps.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
      ps.setString(2, "u-1");
      ps.executeUpdate();
    } finally {
      if (ps != null) {
        ps.close();
      }
      if (con != null) {
        con.close();
      }
    }
  }
}
