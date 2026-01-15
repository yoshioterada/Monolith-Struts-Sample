package com.skishop.dao.order;

import com.skishop.domain.order.Order;
import com.skishop.domain.order.OrderItem;
import java.util.List;

public interface OrderDao {
  Order findById(String id);

  List<Order> listByUserId(String userId);

  void insertOrder(Order order);

  void insertOrderItem(OrderItem item);

  List<OrderItem> listItemsByOrderId(String orderId);

  void updateStatus(String orderId, String status);

  void updatePaymentStatus(String orderId, String paymentStatus);
}
