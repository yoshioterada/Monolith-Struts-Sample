package com.skishop.service.order;

import com.skishop.dao.address.UserAddressDao;
import com.skishop.dao.address.UserAddressDaoImpl;
import com.skishop.domain.address.Address;
import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import com.skishop.domain.coupon.Coupon;
import com.skishop.domain.order.Order;
import com.skishop.domain.order.OrderItem;
import com.skishop.domain.order.OrderShipping;
import com.skishop.domain.product.Product;
import com.skishop.service.cart.CartService;
import com.skishop.service.catalog.ProductService;
import com.skishop.service.coupon.CouponService;
import com.skishop.service.inventory.InventoryService;
import com.skishop.service.payment.PaymentInfo;
import com.skishop.service.payment.PaymentResult;
import com.skishop.service.payment.PaymentService;
import com.skishop.service.point.PointService;
import com.skishop.service.shipping.ShippingService;
import com.skishop.service.tax.TaxService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderFacadeImpl implements OrderFacade {
  private final CartService cartService = new CartService();
  private final CouponService couponService = new CouponService();
  private final InventoryService inventoryService = new InventoryService();
  private final PaymentService paymentService = new PaymentService();
  private final OrderService orderService = new OrderService();
  private final PointService pointService = new PointService();
  private final ShippingService shippingService = new ShippingService();
  private final TaxService taxService = new TaxService();
  private final ProductService productService = new ProductService();
  private final UserAddressDao userAddressDao = new UserAddressDaoImpl();

  public Order placeOrder(String cartId, String couponCode, int usePoints, PaymentInfo paymentInfo, String userId) {
    Cart cart = cartService.getCart(cartId);
    if (cart == null) {
      throw new IllegalArgumentException("Cart not found");
    }
    List<CartItem> items = cartService.getItems(cartId);
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("Cart is empty");
    }

    BigDecimal subtotal = cartService.calculateSubtotal(items);
    Coupon coupon = couponService.validateCoupon(couponCode, subtotal);
    BigDecimal discount = couponService.calculateDiscount(coupon, subtotal);
    BigDecimal discounted = subtotal.subtract(discount);
    if (discounted.compareTo(BigDecimal.ZERO) < 0) {
      discounted = BigDecimal.ZERO;
    }

    String orderId = UUID.randomUUID().toString();
    String orderNumber = "ORD-" + System.currentTimeMillis();

    int redeemPoints = 0;
    if (userId != null) {
      redeemPoints = normalizePoints(usePoints, discounted);
      if (redeemPoints > 0) {
        pointService.redeemPoints(userId, redeemPoints, orderId);
      }
    }

    BigDecimal taxable = discounted.subtract(new BigDecimal(redeemPoints));
    if (taxable.compareTo(BigDecimal.ZERO) < 0) {
      taxable = BigDecimal.ZERO;
    }

    BigDecimal tax = taxService.calculateTax(taxable);
    BigDecimal shippingFee = shippingService.calculateShippingFee(taxable);
    BigDecimal totalAmount = taxable.add(tax).add(shippingFee);

    boolean inventoryReserved = false;
    PaymentResult paymentResult = null;
    try {
      inventoryService.reserveItems(items);
      inventoryReserved = true;
      paymentResult = paymentService.authorize(paymentInfo, totalAmount, cartId, orderId);
      if (!paymentResult.isSuccess()) {
        throw new IllegalStateException("Payment failed");
      }

      Order order = orderService.buildOrder(orderId, orderNumber, userId, subtotal, tax, shippingFee, discount, totalAmount, coupon != null ? coupon.getCode() : null, redeemPoints);
      order.setPaymentStatus(paymentResult.getStatus());
      List<OrderItem> orderItems = buildOrderItems(orderId, items);
      orderService.createOrder(order, orderItems);
      couponService.markUsed(coupon, userId, orderId, discount);
      pointService.awardPoints(userId, orderId, totalAmount);
      saveShipping(orderId, shippingFee, userId);
      cartService.clearCart(cartId);
      return order;
    } catch (RuntimeException e) {
      if (paymentResult != null && paymentResult.isSuccess()) {
        paymentService.voidPayment(orderId);
      }
      if (inventoryReserved) {
        inventoryService.releaseItems(items);
      }
      if (redeemPoints > 0) {
        pointService.refundPoints(userId, redeemPoints, orderId);
      }
      throw e;
    }
  }

  public Order cancelOrder(String orderId, String userId) {
    Order order = orderService.findById(orderId);
    if (order == null) {
      throw new IllegalArgumentException("Order not found");
    }
    if (!"CREATED".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
      throw new IllegalStateException("Order cannot be cancelled");
    }
    List<OrderItem> items = orderService.listItems(orderId);
    inventoryService.releaseItems(toCartItems(items));
    couponService.releaseUsage(orderId);
    if (order.getUsedPoints() > 0) {
      pointService.refundPoints(order.getUserId(), order.getUsedPoints(), orderId);
    }
    int awarded = pointService.calculateAwardPoints(order.getTotalAmount());
    pointService.revokePoints(order.getUserId(), awarded, orderId);
    paymentService.voidPayment(orderId);
    orderService.updateStatus(orderId, "CANCELLED");
    orderService.updatePaymentStatus(orderId, "VOID");
    return orderService.findById(orderId);
  }

  public Order returnOrder(String orderId, String userId) {
    Order order = orderService.findById(orderId);
    if (order == null) {
      throw new IllegalArgumentException("Order not found");
    }
    if (!"DELIVERED".equals(order.getStatus())) {
      throw new IllegalStateException("Order cannot be returned");
    }
    List<OrderItem> items = orderService.listItems(orderId);
    inventoryService.releaseItems(toCartItems(items));
    couponService.releaseUsage(orderId);
    if (order.getUsedPoints() > 0) {
      pointService.refundPoints(order.getUserId(), order.getUsedPoints(), orderId);
    }
    int awarded = pointService.calculateAwardPoints(order.getTotalAmount());
    pointService.revokePoints(order.getUserId(), awarded, orderId);
    paymentService.refundPayment(orderId);
    orderService.recordReturn(orderId, items);
    orderService.updateStatus(orderId, "RETURNED");
    orderService.updatePaymentStatus(orderId, "REFUNDED");
    return orderService.findById(orderId);
  }

  private int normalizePoints(int usePoints, BigDecimal discountedAmount) {
    if (usePoints <= 0) {
      return 0;
    }
    int maxPoints = discountedAmount.setScale(0, RoundingMode.DOWN).intValue();
    if (usePoints > maxPoints) {
      return maxPoints;
    }
    return usePoints;
  }

  private List<OrderItem> buildOrderItems(String orderId, List<CartItem> items) {
    List<OrderItem> orderItems = new ArrayList<OrderItem>();
    for (CartItem cartItem : items) {
      Product product = productService.findById(cartItem.getProductId());
      OrderItem item = new OrderItem();
      item.setId(UUID.randomUUID().toString());
      item.setOrderId(orderId);
      item.setProductId(cartItem.getProductId());
      item.setProductName(product != null ? product.getName() : "");
      item.setSku(product != null ? product.getSku() : null);
      item.setUnitPrice(cartItem.getUnitPrice());
      item.setQuantity(cartItem.getQuantity());
      item.setSubtotal(cartItem.getUnitPrice().multiply(new BigDecimal(cartItem.getQuantity())));
      orderItems.add(item);
    }
    return orderItems;
  }

  private List<CartItem> toCartItems(List<OrderItem> items) {
    List<CartItem> cartItems = new ArrayList<CartItem>();
    for (OrderItem orderItem : items) {
      CartItem cartItem = new CartItem();
      cartItem.setProductId(orderItem.getProductId());
      cartItem.setQuantity(orderItem.getQuantity());
      cartItems.add(cartItem);
    }
    return cartItems;
  }

  private void saveShipping(String orderId, BigDecimal shippingFee, String userId) {
    if (userId == null) {
      return;
    }
    List<Address> addresses = userAddressDao.listByUserId(userId);
    if (addresses == null || addresses.isEmpty()) {
      return;
    }
    Address address = addresses.get(0);
    OrderShipping shipping = new OrderShipping();
    shipping.setId(UUID.randomUUID().toString());
    shipping.setOrderId(orderId);
    shipping.setRecipientName(address.getRecipientName());
    shipping.setPostalCode(address.getPostalCode());
    shipping.setPrefecture(address.getPrefecture());
    shipping.setAddress1(address.getAddress1());
    shipping.setAddress2(address.getAddress2());
    shipping.setPhone(address.getPhone());
    shipping.setShippingMethodCode("STANDARD");
    shipping.setShippingFee(shippingFee);
    shipping.setRequestedDeliveryDate(null);
    shippingService.saveOrderShipping(shipping);
  }
}
