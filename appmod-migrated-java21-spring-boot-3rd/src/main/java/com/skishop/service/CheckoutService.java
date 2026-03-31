package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.dto.request.OrderBuildRequest;
import com.skishop.dto.request.PaymentInfo;
import com.skishop.dto.response.CheckoutSummary;
import com.skishop.dto.response.PaymentResult;
import com.skishop.exception.BusinessException;
import com.skishop.model.Address;
import com.skishop.model.Cart;
import com.skishop.model.CartItem;
import com.skishop.model.Coupon;
import com.skishop.model.Order;
import com.skishop.model.OrderItem;
import com.skishop.model.OrderShipping;
import com.skishop.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 注文確定・キャンセル・返品を統括するチェックアウトサービス。
 *
 * <p>SkiShop EC サイトにおけるチェックアウトフローの中核を担い、
 * 以下の複合的なビジネスオペレーションを単一トランザクションで原子的に実行する。</p>
 *
 * <h3>注文確定フロー（{@link #placeOrder}）</h3>
 * <ol>
 *   <li>カートからアイテムを取得し空チェック</li>
 *   <li>小計金額を計算</li>
 *   <li>クーポンの検証と割引額の計算</li>
 *   <li>ポイント使用（該当する場合）</li>
 *   <li>消費税・配送料の計算</li>
 *   <li>在庫の仮確保（予約）</li>
 *   <li>決済のオーソリ（与信枠確保）</li>
 *   <li>注文レコードおよび注文明細の作成</li>
 *   <li>クーポンの使用済みマーク</li>
 *   <li>ポイント付与（注文金額の 1%）</li>
 *   <li>配送情報の保存、カートのクリア、受注確認メールのキューイング</li>
 * </ol>
 *
 * <p>決済成功後にいずれかのステップで例外が発生した場合は、
 * 決済の取消（void）、在庫の解放、ポイントの返還を補償アクションとして実行する。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link CartService} — カートおよびアイテムの操作</li>
 *   <li>{@link CouponService} — クーポンの検証・割引計算・使用記録</li>
 *   <li>{@link InventoryService} — 在庫の確保・解放</li>
 *   <li>{@link PaymentService} — 決済のオーソリ・取消・返金</li>
 *   <li>{@link OrderService} — 注文レコードの作成・ステータス管理</li>
 *   <li>{@link PointService} — ポイントの使用・付与・返還・取消</li>
 *   <li>{@link ShippingService} — 配送料の計算・配送情報の保存</li>
 *   <li>{@link TaxService} — 消費税の計算</li>
 *   <li>{@link ProductService} — 商品情報の取得</li>
 *   <li>{@link AddressService} — 配送先住所の取得</li>
 *   <li>{@link MailService} — 受注確認メールのキューイング</li>
 *   <li>{@link UserService} — ユーザー情報の取得</li>
 * </ul>
 *
 * @see com.skishop.controller.CheckoutController
 * @see OrderService
 * @see PaymentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartService cartService;
    private final CouponService couponService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PointService pointService;
    private final ShippingService shippingService;
    private final TaxService taxService;
    private final ProductService productService;
    private final AddressService addressService;
    private final MailService mailService;
    private final UserService userService;

    /**
     * チェックアウト画面用のサマリー情報を準備する。
     *
     * @param userId    ログイン中のユーザー ID
     * @param sessionId HTTP セッション ID
     * @return チェックアウトサマリー
     */
    @Transactional(readOnly = true)
    public CheckoutSummary prepareCheckoutSummary(String userId, String sessionId) {
        Cart cart = cartService.getOrCreateCart(userId, sessionId);
        List<CartItem> items = cartService.getItems(cart.getId());
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        BigDecimal shippingFee = shippingService.calculateShippingFee(subtotal);
        BigDecimal tax = taxService.calculateTax(subtotal);
        return new CheckoutSummary(cart, items, subtotal, shippingFee, tax);
    }

    /**
     * 注文確定処理を実行する。
     *
     * <p>以下の 11 ステップを単一トランザクション内で原子的に実行する。
     * いずれかのステップで例外が発生した場合、決済取消・在庫解放・ポイント返還の
     * 補償アクションを実行してからトランザクション全体をロールバックする。</p>
     *
     * <ol>
     *   <li>カートからアイテムを取得（空チェック）</li>
     *   <li>小計金額の計算</li>
     *   <li>クーポン検証と割引額の計算</li>
     *   <li>ポイント使用（使用上限は割引後金額まで）</li>
     *   <li>消費税・配送料の計算</li>
     *   <li>在庫の仮確保（予約）</li>
     *   <li>決済のオーソリ（与信枠確保）</li>
     *   <li>注文レコードおよび注文明細の作成</li>
     *   <li>クーポン使用済みマーク</li>
     *   <li>ポイント付与（注文合計の 1%）</li>
     *   <li>配送情報の保存、カートのクリア、受注確認メールのキューイング</li>
     * </ol>
     *
     * @param cartId      チェックアウト対象のカート ID
     * @param couponCode  適用するクーポンコード（未使用の場合は {@code null} または空文字）
     * @param usePoints   使用するポイント数（0 の場合はポイント使用なし）
     * @param paymentInfo 支払い情報（カード番号、有効期限等）
     * @param userId      ログイン中のユーザー ID（ゲストチェックアウトの場合は {@code null}）
     * @return 作成された注文エンティティ
     * @throws BusinessException カートが空、在庫不足、決済失敗等のビジネスルール違反時
     * @throws ResourceNotFoundException カートまたはユーザーが存在しない場合
     * @see com.skishop.controller.CheckoutController
     */
    @Transactional
    public Order placeOrder(String cartId, String couponCode, int usePoints,
                            PaymentInfo paymentInfo, String userId) {
        // Step 1: Get cart and items (empty check)
        var cart = cartService.getCart(cartId);
        List<CartItem> items = cartService.getItems(cartId);
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty",
                    "redirect:/cart", "error.cart.empty");
        }

        String orderId = UUID.randomUUID().toString();
        String orderNumber = "ORD-" + System.currentTimeMillis();

        // Steps 2-5: Calculate amounts (subtotal, discount, points, tax, shipping)
        var amounts = calculateOrderAmounts(items, couponCode, usePoints, orderId, userId);

        PaymentResult paymentResult = null;

        try {
            // Step 6: Reserve inventory
            inventoryService.reserveItems(items);

            // Step 7: Authorize payment
            paymentResult = paymentService.authorize(paymentInfo, amounts.totalAmount, cartId, orderId);
            if (!paymentResult.isSuccess()) {
                throw new BusinessException("Payment failed",
                        "redirect:/checkout", "error.payment.failed");
            }

            // Step 8: Create order and order items
            var buildRequest = new OrderBuildRequest(orderId, orderNumber, userId,
                    amounts.subtotal, amounts.tax, amounts.shippingFee, amounts.discount, amounts.totalAmount,
                    amounts.coupon != null ? amounts.coupon.getCode() : null, amounts.redeemPoints);
            Order order = orderService.buildOrder(buildRequest);
            order.setPaymentStatus(paymentResult.status());
            List<OrderItem> orderItems = buildOrderItems(orderId, items);
            orderService.createOrder(order, orderItems);

            // Steps 9-11: Post-payment processing
            finalizeOrder(order, amounts, orderId, cartId, userId);

            return order;
        } catch (RuntimeException e) {
            if (paymentResult != null && paymentResult.isSuccess()) {
                paymentService.voidPayment(orderId);
            }
            throw e;
        }
    }

    /**
     * 注文をキャンセルする。
     *
     * <p>ステータスが {@code CREATED} または {@code CONFIRMED} の注文のみキャンセル可能。
     * キャンセル時には以下の補償アクションを実行する:</p>
     * <ul>
     *   <li>在庫の解放</li>
     *   <li>クーポン使用の取消</li>
     *   <li>使用ポイントの返還</li>
     *   <li>付与済みポイントの取消</li>
     *   <li>決済の取消（void）</li>
     * </ul>
     *
     * @param orderId キャンセル対象の注文 ID
     * @param userId  操作ユーザーの ID（オーナーシップ検証に使用）
     * @return キャンセル後の注文エンティティ（ステータス: {@code CANCELLED}）
     * @throws BusinessException          注文ステータスがキャンセル不可の状態の場合
     * @throws ResourceNotFoundException 注文が存在しない、または当該ユーザーの注文でない場合
     * @see com.skishop.controller.OrderController
     */
    @Transactional
    public Order cancelOrder(String orderId, String userId) {
        var order = orderService.findByIdAndUserId(orderId, userId);
        if (!AppConstants.ORDER_STATUS_CREATED.equals(order.getStatus()) && !AppConstants.ORDER_STATUS_CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException("Order cannot be cancelled",
                    "redirect:/orders/" + orderId, "error.order.cancel.invalid");
        }

        List<OrderItem> items = orderService.listItems(orderId);
        reverseOrderEffects(order, items);

        paymentService.voidPayment(orderId);
        orderService.updateStatus(orderId, AppConstants.ORDER_STATUS_CANCELLED);
        orderService.updatePaymentStatus(orderId, AppConstants.PAYMENT_STATUS_VOID);

        return orderService.findById(orderId);
    }

    /**
     * 配達済み注文の返品処理を実行する。
     *
     * <p>ステータスが {@code DELIVERED} の注文のみ返品可能。
     * 返品時には以下の処理を実行する:</p>
     * <ul>
     *   <li>在庫の復帰</li>
     *   <li>クーポン使用の取消</li>
     *   <li>使用ポイントの返還</li>
     *   <li>付与済みポイントの取消</li>
     *   <li>決済の返金（refund）</li>
     *   <li>返品レコードの作成</li>
     * </ul>
     *
     * @param orderId 返品対象の注文 ID
     * @param userId  操作ユーザーの ID（オーナーシップ検証に使用）
     * @return 返品処理後の注文エンティティ（ステータス: {@code RETURNED}）
     * @throws BusinessException          注文ステータスが返品不可の状態の場合
     * @throws ResourceNotFoundException 注文が存在しない、または当該ユーザーの注文でない場合
     * @see com.skishop.controller.OrderController
     */
    @Transactional
    public Order returnOrder(String orderId, String userId) {
        var order = orderService.findByIdAndUserId(orderId, userId);
        if (!AppConstants.ORDER_STATUS_DELIVERED.equals(order.getStatus())) {
            throw new BusinessException("Order cannot be returned",
                    "redirect:/orders/" + orderId, "error.order.return.invalid");
        }

        List<OrderItem> items = orderService.listItems(orderId);
        reverseOrderEffects(order, items);

        paymentService.refundPayment(orderId);
        orderService.recordReturn(orderId, items);
        orderService.updateStatus(orderId, AppConstants.ORDER_STATUS_RETURNED);
        orderService.updatePaymentStatus(orderId, AppConstants.PAYMENT_STATUS_REFUNDED);

        return orderService.findById(orderId);
    }

    /**
     * 管理者による返金処理を実行する（オーナーシップ検証なし）。
     *
     * <p>ステータスが {@code DELIVERED} の注文のみ返金可能。
     * {@link #returnOrder} と同様の補償処理を実行するが、
     * オーナーシップ検証をスキップするため管理者操作用。</p>
     *
     * @param orderId 返金対象の注文 ID
     * @return 返金処理後の注文エンティティ（ステータス: {@code RETURNED}）
     * @throws BusinessException          注文ステータスが返金不可の状態の場合
     * @throws ResourceNotFoundException 注文が存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Order refundOrder(String orderId) {
        var order = orderService.findById(orderId);
        if (!AppConstants.ORDER_STATUS_DELIVERED.equals(order.getStatus())) {
            throw new BusinessException("Order cannot be refunded",
                    "redirect:/admin/orders/" + orderId, "error.order.refund.invalid");
        }

        List<OrderItem> items = orderService.listItems(orderId);
        reverseOrderEffects(order, items);

        paymentService.refundPayment(orderId);
        orderService.recordReturn(orderId, items);
        orderService.updateStatus(orderId, AppConstants.ORDER_STATUS_RETURNED);
        orderService.updatePaymentStatus(orderId, AppConstants.PAYMENT_STATUS_REFUNDED);

        return orderService.findById(orderId);
    }

    private record OrderAmounts(
            BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount,
            BigDecimal tax, BigDecimal shippingFee,
            Coupon coupon, int redeemPoints) {}

    private OrderAmounts calculateOrderAmounts(List<CartItem> items, String couponCode,
                                                int usePoints, String orderId, String userId) {
        BigDecimal subtotal = cartService.calculateSubtotal(items);

        Coupon coupon = couponService.validateCoupon(couponCode, subtotal).orElse(null);
        BigDecimal discount = couponService.calculateDiscount(coupon, subtotal);
        BigDecimal discounted = subtotal.subtract(discount);
        if (discounted.compareTo(BigDecimal.ZERO) < 0) {
            discounted = BigDecimal.ZERO;
        }

        int redeemPoints = 0;
        if (userId != null) {
            redeemPoints = normalizePoints(usePoints, discounted);
            if (redeemPoints > 0) {
                pointService.redeemPoints(userId, redeemPoints, orderId);
            }
        }

        BigDecimal taxable = discounted.subtract(BigDecimal.valueOf(redeemPoints));
        if (taxable.compareTo(BigDecimal.ZERO) < 0) {
            taxable = BigDecimal.ZERO;
        }

        BigDecimal tax = taxService.calculateTax(taxable);
        BigDecimal shippingFee = shippingService.calculateShippingFee(taxable);
        BigDecimal totalAmount = taxable.add(tax).add(shippingFee);

        return new OrderAmounts(subtotal, discount, totalAmount, tax, shippingFee, coupon, redeemPoints);
    }

    private void finalizeOrder(Order order, OrderAmounts amounts,
                                String orderId, String cartId, String userId) {
        couponService.markUsed(amounts.coupon, userId, orderId, amounts.discount);
        pointService.awardPoints(userId, orderId, amounts.totalAmount);
        saveShipping(orderId, amounts.shippingFee, userId);
        cartService.clearCart(cartId);
        sendOrderConfirmation(order, userId);
    }

    /**
     * 注文の補償処理を実行する（在庫解放・クーポン取消・ポイント返還/取消）。
     */
    private void reverseOrderEffects(Order order, List<OrderItem> items) {
        inventoryService.releaseItems(orderService.toCartItems(items));
        couponService.releaseUsage(order.getId());

        if (order.getUsedPoints() > 0) {
            pointService.refundPoints(order.getUserId(), order.getUsedPoints(), order.getId());
        }
        int awarded = pointService.calculateAwardPoints(order.getTotalAmount());
        pointService.revokePoints(order.getUserId(), awarded, order.getId());
    }

    private int normalizePoints(int usePoints, BigDecimal discountedAmount) {
        if (usePoints <= 0) {
            return 0;
        }
        int maxPoints = discountedAmount.setScale(0, RoundingMode.DOWN).intValue();
        return Math.min(usePoints, maxPoints);
    }

    private List<OrderItem> buildOrderItems(String orderId, List<CartItem> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            Product product = productService.findById(cartItem.getProductId());
            var item = new OrderItem();
            item.setId(UUID.randomUUID().toString());
            item.setProductId(cartItem.getProductId());
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPrice(cartItem.getUnitPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(item);
        }
        return orderItems;
    }

    private void saveShipping(String orderId, BigDecimal shippingFee, String userId) {
        if (userId == null) {
            return;
        }
        List<Address> addresses = addressService.findByUserId(userId);
        if (addresses.isEmpty()) {
            return;
        }
        var address = addresses.getFirst();
        var shipping = new OrderShipping();
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
        shippingService.saveOrderShipping(shipping);
    }

    private void sendOrderConfirmation(Order order, String userId) {
        if (userId == null) {
            return;
        }
        try {
            var user = userService.findById(userId);
            mailService.enqueueOrderConfirmation(user.getEmail(), order);
        } catch (Exception e) {
            log.error("Failed to enqueue order confirmation email: {}", e.getMessage(), e);
        }
    }
}
