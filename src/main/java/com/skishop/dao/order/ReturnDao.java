package com.skishop.dao.order;

import com.skishop.domain.order.Return;
import java.util.List;

public interface ReturnDao {
  void insert(Return returnItem);

  List<Return> listByOrderId(String orderId);
}
