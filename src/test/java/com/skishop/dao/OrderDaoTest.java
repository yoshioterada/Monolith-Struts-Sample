package com.skishop.dao;

import com.skishop.dao.order.OrderDao;
import com.skishop.dao.order.OrderDaoImpl;
import com.skishop.domain.order.Order;
import com.skishop.domain.order.OrderItem;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OrderDaoTest extends DaoTestBase {
  private OrderDao orderDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    orderDao = new OrderDaoImpl();
  }

  @Test
  public void testFindByIdAndList() {
    Order order = orderDao.findById("order-1");
    Assert.assertNotNull(order);
    List orders = orderDao.listByUserId("u-1");
    Assert.assertFalse(orders.isEmpty());
  }

  @Test
  public void testInsertOrderAndItem() {
    Order order = new Order();
    order.setId("order-2");
    order.setOrderNumber("ORD-0002");
    order.setUserId("u-1");
    order.setStatus("CREATED");
    order.setPaymentStatus("AUTHORIZED");
    order.setSubtotal(new BigDecimal("1000"));
    order.setTax(new BigDecimal("100"));
    order.setShippingFee(new BigDecimal("800"));
    order.setDiscountAmount(new BigDecimal("0"));
    order.setTotalAmount(new BigDecimal("1900"));
    order.setCouponCode(null);
    order.setUsedPoints(0);
    order.setCreatedAt(new Date());
    order.setUpdatedAt(new Date());
    orderDao.insertOrder(order);

    OrderItem item = new OrderItem();
    item.setId("order-item-2");
    item.setOrderId("order-2");
    item.setProductId("P001");
    item.setProductName("Ski A");
    item.setSku("SKU-001");
    item.setUnitPrice(new BigDecimal("1000"));
    item.setQuantity(1);
    item.setSubtotal(new BigDecimal("1000"));
    orderDao.insertOrderItem(item);

    Order loaded = orderDao.findById("order-2");
    Assert.assertNotNull(loaded);
    Assert.assertEquals("ORD-0002", loaded.getOrderNumber());
  }
}
