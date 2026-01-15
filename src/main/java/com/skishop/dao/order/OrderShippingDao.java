package com.skishop.dao.order;

import com.skishop.domain.order.OrderShipping;

public interface OrderShippingDao {
  void insert(OrderShipping shipping);

  OrderShipping findByOrderId(String orderId);
}
