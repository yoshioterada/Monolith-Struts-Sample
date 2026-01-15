package com.skishop.service.order;

import com.skishop.domain.order.Order;
import com.skishop.service.payment.PaymentInfo;

public interface OrderFacade {
  Order placeOrder(String cartId, String couponCode, int usePoints, PaymentInfo paymentInfo, String userId);

  Order cancelOrder(String orderId, String userId);

  Order returnOrder(String orderId, String userId);
}
