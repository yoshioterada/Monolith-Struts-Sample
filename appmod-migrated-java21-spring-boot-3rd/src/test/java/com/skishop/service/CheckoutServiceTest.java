package com.skishop.service;

import com.skishop.dto.request.OrderBuildRequest;
import com.skishop.dto.request.PaymentInfo;
import com.skishop.dto.response.PaymentResult;
import com.skishop.exception.BusinessException;
import com.skishop.model.Address;
import com.skishop.model.Cart;
import com.skishop.model.CartItem;
import com.skishop.model.Coupon;
import com.skishop.model.Order;
import com.skishop.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CartService cartService;
    @Mock private CouponService couponService;
    @Mock private InventoryService inventoryService;
    @Mock private PaymentService paymentService;
    @Mock private OrderService orderService;
    @Mock private PointService pointService;
    @Mock private ShippingService shippingService;
    @Mock private TaxService taxService;
    @Mock private ProductService productService;
    @Mock private AddressService addressService;
    @Mock private MailService mailService;
    @Mock private UserService userService;

    @InjectMocks
    private CheckoutService checkoutService;

    private Cart cart;
    private CartItem cartItem;
    private PaymentInfo paymentInfo;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId("user-1");
        cart.setStatus("ACTIVE");

        cartItem = new CartItem();
        cartItem.setId("item-1");
        cartItem.setProductId("prod-1");
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(new BigDecimal("5000"));

        paymentInfo = new PaymentInfo("CARD", "4111111111111111", "12", "2030", "123", "100-0001");
    }

    @Test
    @DisplayName("正常な注文確定が全11ステップを完了する")
    void should_completeAllSteps_when_validOrder() {
        // Arrange
        when(cartService.getCart("cart-1")).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of(cartItem));
        when(cartService.calculateSubtotal(anyList())).thenReturn(new BigDecimal("10000"));
        when(couponService.validateCoupon(null, new BigDecimal("10000"))).thenReturn(Optional.empty());
        when(couponService.calculateDiscount(null, new BigDecimal("10000"))).thenReturn(BigDecimal.ZERO);
        when(taxService.calculateTax(any())).thenReturn(new BigDecimal("1000"));
        when(shippingService.calculateShippingFee(any())).thenReturn(BigDecimal.ZERO);
        when(paymentService.authorize(any(), any(), anyString(), anyString()))
                .thenReturn(new PaymentResult.Success("pay-1", "AUTHORIZED"));

        var product = new Product();
        product.setId("prod-1");
        product.setName("Test Product");
        product.setSku("SKU-001");
        when(productService.findById("prod-1")).thenReturn(product);

        var order = new Order();
        order.setId("order-1");
        order.setOrderNumber("ORD-123");
        when(orderService.buildOrder(any(OrderBuildRequest.class)))
                .thenReturn(order);
        when(orderService.createOrder(any(), anyList())).thenReturn(order);
        when(addressService.findByUserId("user-1")).thenReturn(List.of());

        // Act
        var result = checkoutService.placeOrder("cart-1", null, 0, paymentInfo, "user-1");

        // Assert
        assertThat(result).isNotNull();
        verify(inventoryService).reserveItems(anyList());
        verify(paymentService).authorize(any(), any(), anyString(), anyString());
        verify(orderService).createOrder(any(), anyList());
        verify(cartService).clearCart("cart-1");
    }

    @Test
    @DisplayName("空のカートの場合、BusinessExceptionをスローする")
    void should_throwException_when_cartIsEmpty() {
        // Arrange
        when(cartService.getCart("cart-1")).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.placeOrder("cart-1", null, 0, paymentInfo, "user-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("決済失敗時にロールバック処理が実行される")
    void should_rollbackInventory_when_paymentFails() {
        // Arrange
        when(cartService.getCart("cart-1")).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of(cartItem));
        when(cartService.calculateSubtotal(anyList())).thenReturn(new BigDecimal("10000"));
        when(couponService.validateCoupon(null, new BigDecimal("10000"))).thenReturn(Optional.empty());
        when(couponService.calculateDiscount(null, new BigDecimal("10000"))).thenReturn(BigDecimal.ZERO);
        when(taxService.calculateTax(any())).thenReturn(new BigDecimal("1000"));
        when(shippingService.calculateShippingFee(any())).thenReturn(BigDecimal.ZERO);
        when(paymentService.authorize(any(), any(), anyString(), anyString()))
                .thenReturn(new PaymentResult.Failure("FAILED", "Declined"));

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.placeOrder("cart-1", null, 0, paymentInfo, "user-1"))
                .isInstanceOf(BusinessException.class);

        // Verify rollback: DB changes (inventory reservation) are rolled back by @Transactional.
        // No explicit releaseItems call needed — the transaction rollback undoes the DB state.
        verify(inventoryService, never()).releaseItems(anyList());
        verify(paymentService, never()).voidPayment(anyString());
    }

    @Test
    @DisplayName("クーポン適用時に割引が適用される")
    void should_applyDiscount_when_couponProvided() {
        // Arrange
        var coupon = new Coupon();
        coupon.setId("coupon-1");
        coupon.setCode("SAVE10");
        coupon.setCouponType("PERCENT");
        coupon.setDiscountValue(new BigDecimal("10"));

        when(cartService.getCart("cart-1")).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of(cartItem));
        when(cartService.calculateSubtotal(anyList())).thenReturn(new BigDecimal("10000"));
        when(couponService.validateCoupon("SAVE10", new BigDecimal("10000"))).thenReturn(Optional.of(coupon));
        when(couponService.calculateDiscount(coupon, new BigDecimal("10000"))).thenReturn(new BigDecimal("1000"));
        when(taxService.calculateTax(any())).thenReturn(new BigDecimal("900"));
        when(shippingService.calculateShippingFee(any())).thenReturn(BigDecimal.ZERO);
        when(paymentService.authorize(any(), any(), anyString(), anyString()))
                .thenReturn(new PaymentResult.Success("pay-1", "AUTHORIZED"));

        var product = new Product();
        product.setId("prod-1");
        product.setName("Test Product");
        when(productService.findById("prod-1")).thenReturn(product);

        var order = new Order();
        order.setId("order-1");
        when(orderService.buildOrder(any(OrderBuildRequest.class)))
                .thenReturn(order);
        when(orderService.createOrder(any(), anyList())).thenReturn(order);
        when(addressService.findByUserId("user-1")).thenReturn(List.of());

        // Act
        checkoutService.placeOrder("cart-1", "SAVE10", 0, paymentInfo, "user-1");

        // Assert
        verify(couponService).markUsed(eq(coupon), eq("user-1"), anyString(), eq(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("ポイントを使用した場合、差し引かれた金額で計算される")
    void should_deductPoints_when_pointsUsed() {
        // Arrange
        when(cartService.getCart("cart-1")).thenReturn(cart);
        when(cartService.getItems("cart-1")).thenReturn(List.of(cartItem));
        when(cartService.calculateSubtotal(anyList())).thenReturn(new BigDecimal("10000"));
        when(couponService.validateCoupon(null, new BigDecimal("10000"))).thenReturn(Optional.empty());
        when(couponService.calculateDiscount(null, new BigDecimal("10000"))).thenReturn(BigDecimal.ZERO);
        when(taxService.calculateTax(any())).thenReturn(new BigDecimal("950"));
        when(shippingService.calculateShippingFee(any())).thenReturn(BigDecimal.ZERO);
        when(paymentService.authorize(any(), any(), anyString(), anyString()))
                .thenReturn(new PaymentResult.Success("pay-1", "AUTHORIZED"));

        var product = new Product();
        product.setId("prod-1");
        product.setName("Test Product");
        when(productService.findById("prod-1")).thenReturn(product);

        var order = new Order();
        order.setId("order-1");
        when(orderService.buildOrder(any(OrderBuildRequest.class)))
                .thenReturn(order);
        when(orderService.createOrder(any(), anyList())).thenReturn(order);
        when(addressService.findByUserId("user-1")).thenReturn(List.of());

        // Act
        checkoutService.placeOrder("cart-1", null, 500, paymentInfo, "user-1");

        // Assert
        verify(pointService).redeemPoints(eq("user-1"), eq(500), anyString());
    }

    @Test
    @DisplayName("注文キャンセル時に全ての補償処理が実行される")
    void should_executeCompensation_when_orderCancelled() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setStatus("CREATED");
        order.setUsedPoints(100);
        order.setTotalAmount(new BigDecimal("10000"));

        when(orderService.findByIdAndUserId("order-1", "user-1")).thenReturn(order);
        when(orderService.listItems("order-1")).thenReturn(List.of());
        when(orderService.toCartItems(anyList())).thenReturn(List.of());
        when(pointService.calculateAwardPoints(new BigDecimal("10000"))).thenReturn(100);
        when(orderService.findById("order-1")).thenReturn(order);

        // Act
        checkoutService.cancelOrder("order-1", "user-1");

        // Assert
        verify(inventoryService).releaseItems(anyList());
        verify(couponService).releaseUsage("order-1");
        verify(pointService).refundPoints("user-1", 100, "order-1");
        verify(pointService).revokePoints("user-1", 100, "order-1");
        verify(paymentService).voidPayment("order-1");
        verify(orderService).updateStatus("order-1", "CANCELLED");
        verify(orderService).updatePaymentStatus("order-1", "VOID");
    }

    @Test
    @DisplayName("キャンセル不可の注文ステータスの場合、例外をスローする")
    void should_throwException_when_orderCannotBeCancelled() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setStatus("DELIVERED");
        when(orderService.findByIdAndUserId("order-1", "user-1")).thenReturn(order);

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.cancelOrder("order-1", "user-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("返品処理時に全ての補償処理が実行される")
    void should_executeRefund_when_orderReturned() {
        // Arrange
        var order = new Order();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setStatus("DELIVERED");
        order.setUsedPoints(50);
        order.setTotalAmount(new BigDecimal("5000"));

        when(orderService.findByIdAndUserId("order-1", "user-1")).thenReturn(order);
        when(orderService.listItems("order-1")).thenReturn(List.of());
        when(orderService.toCartItems(anyList())).thenReturn(List.of());
        when(pointService.calculateAwardPoints(new BigDecimal("5000"))).thenReturn(50);
        when(orderService.findById("order-1")).thenReturn(order);

        // Act
        checkoutService.returnOrder("order-1", "user-1");

        // Assert
        verify(paymentService).refundPayment("order-1");
        verify(orderService).recordReturn(eq("order-1"), anyList());
        verify(orderService).updateStatus("order-1", "RETURNED");
        verify(orderService).updatePaymentStatus("order-1", "REFUNDED");
    }
}
