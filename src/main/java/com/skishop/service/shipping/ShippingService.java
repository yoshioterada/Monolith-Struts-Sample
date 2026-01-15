package com.skishop.service.shipping;

import com.skishop.dao.order.OrderShippingDao;
import com.skishop.dao.order.OrderShippingDaoImpl;
import com.skishop.domain.order.OrderShipping;
import java.math.BigDecimal;

public class ShippingService {
  private static final BigDecimal FREE_THRESHOLD = new BigDecimal("10000");
  private static final BigDecimal DEFAULT_FEE = new BigDecimal("800");
  private final OrderShippingDao orderShippingDao = new OrderShippingDaoImpl();

  public BigDecimal calculateShippingFee(BigDecimal amount) {
    if (amount == null) {
      return DEFAULT_FEE;
    }
    if (amount.compareTo(FREE_THRESHOLD) >= 0) {
      return BigDecimal.ZERO;
    }
    return DEFAULT_FEE;
  }

  public void saveOrderShipping(OrderShipping shipping) {
    if (shipping != null) {
      orderShippingDao.insert(shipping);
    }
  }
}
