package com.skishop.service.order;

import com.skishop.dao.order.OrderDao;
import com.skishop.dao.order.OrderDaoImpl;
import com.skishop.dao.order.ReturnDao;
import com.skishop.dao.order.ReturnDaoImpl;
import com.skishop.domain.order.Order;
import com.skishop.domain.order.OrderItem;
import com.skishop.domain.order.Return;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OrderService {
  private final OrderDao orderDao = new OrderDaoImpl();
  private final ReturnDao returnDao = new ReturnDaoImpl();

  public Order createOrder(Order order, List<OrderItem> items) {
    orderDao.insertOrder(order);
    for (OrderItem item : items) {
      orderDao.insertOrderItem(item);
    }
    return order;
  }

  public Order findById(String orderId) {
    return orderDao.findById(orderId);
  }

  public List<OrderItem> listItems(String orderId) {
    return orderDao.listItemsByOrderId(orderId);
  }

  public void updateStatus(String orderId, String status) {
    orderDao.updateStatus(orderId, status);
  }

  public void updatePaymentStatus(String orderId, String paymentStatus) {
    orderDao.updatePaymentStatus(orderId, paymentStatus);
  }

  public void recordReturn(String orderId, List<OrderItem> items) {
    for (OrderItem item : items) {
      Return ret = new Return();
      ret.setId(UUID.randomUUID().toString());
      ret.setOrderId(orderId);
      ret.setOrderItemId(item.getId());
      ret.setReason("return");
      ret.setQuantity(item.getQuantity());
      ret.setRefundAmount(item.getSubtotal());
      ret.setStatus("REQUESTED");
      returnDao.insert(ret);
    }
  }

  public Order buildOrder(String orderId, String orderNumber, String userId, BigDecimal subtotal, BigDecimal tax, BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal totalAmount, String couponCode, int usedPoints) {
    Order order = new Order();
    order.setId(orderId);
    order.setOrderNumber(orderNumber);
    order.setUserId(userId);
    order.setStatus("CREATED");
    order.setPaymentStatus("AUTHORIZED");
    order.setSubtotal(subtotal);
    order.setTax(tax);
    order.setShippingFee(shippingFee);
    order.setDiscountAmount(discountAmount);
    order.setTotalAmount(totalAmount);
    order.setCouponCode(couponCode);
    order.setUsedPoints(usedPoints);
    Date now = new Date();
    order.setCreatedAt(now);
    order.setUpdatedAt(now);
    return order;
  }
}
