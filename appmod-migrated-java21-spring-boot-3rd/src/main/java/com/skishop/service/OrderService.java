package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.dto.request.OrderBuildRequest;
import com.skishop.exception.BusinessException;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.CartItem;
import com.skishop.model.Order;
import com.skishop.model.OrderItem;
import com.skishop.model.Return;
import com.skishop.repository.OrderItemRepository;
import com.skishop.repository.OrderRepository;
import com.skishop.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 注文管理サービス。
 *
 * <p>注文の作成、参照、ステータス管理、返品記録を担当する。
 * 注文は {@link CheckoutService} によって作成され、
 * ユーザーごとの注文一覧表示や管理者向けの全件一覧に使用される。</p>
 *
 * <p>注文ステータスの遷移:</p>
 * <pre>
 * CREATED → CONFIRMED → SHIPPED → DELIVERED → RETURNED
 *     ↓
 * CANCELLED
 * </pre>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link OrderRepository} — 注文エンティティの永続化</li>
 *   <li>{@link OrderItemRepository} — 注文明細エンティティの永続化</li>
 *   <li>{@link ReturnRepository} — 返品レコードの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.OrderController
 * @see CheckoutService
 * @see OrderRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRepository returnRepository;

    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
            AppConstants.ORDER_STATUS_CREATED,
            AppConstants.ORDER_STATUS_CONFIRMED,
            AppConstants.ORDER_STATUS_SHIPPED,
            AppConstants.ORDER_STATUS_DELIVERED,
            AppConstants.ORDER_STATUS_CANCELLED,
            AppConstants.ORDER_STATUS_RETURNED
    );

    private static final Set<String> ALLOWED_PAYMENT_STATUSES = Set.of(
            AppConstants.PAYMENT_STATUS_AUTHORIZED,
            AppConstants.PAYMENT_STATUS_FAILED,
            AppConstants.PAYMENT_STATUS_VOID,
            AppConstants.PAYMENT_STATUS_REFUNDED
    );

    /**
     * 注文と注文明細を永続化する。
     *
     * <p>{@link CheckoutService#placeOrder} から呼び出され、
     * 注文ヘッダーと明細行を一括で保存する。</p>
     *
     * @param order      保存する注文エンティティ（ID は事前に設定済み）
     * @param orderItems 保存する注文明細のリスト
     * @return 保存後の注文エンティティ
     */
    @Transactional
    public Order createOrder(Order order, List<OrderItem> orderItems) {
        var savedOrder = orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }
        return savedOrder;
    }

    /**
     * 注文 ID で注文を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param orderId 注文 ID
     * @return 該当する注文エンティティ
     * @throws ResourceNotFoundException 指定 ID の注文が存在しない場合
     */
    @Transactional(readOnly = true)
    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /**
     * 注文 ID とユーザー ID の組み合わせで注文を取得する（IDOR 防止）。
     *
     * <p>注文のオーナーシップを検証し、当該ユーザーの注文のみ返す。
     * 他ユーザーの注文へのアクセスは {@link ResourceNotFoundException} でブロックする。</p>
     *
     * @param orderId 注文 ID
     * @param userId  ログイン中のユーザー ID
     * @return 該当する注文エンティティ
     * @throws ResourceNotFoundException 注文が存在しない、または当該ユーザーの注文でない場合
     */
    @Transactional(readOnly = true)
    public Order findByIdAndUserId(String orderId, String userId) {
        var order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!userId.equals(order.getUserId())) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return order;
    }

    /**
     * 指定ユーザーの注文一覧を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param userId ユーザー ID
     * @return 当該ユーザーの注文リスト（存在しない場合は空リスト）
     */
    @Transactional(readOnly = true)
    public List<Order> listByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 全注文を取得する（管理者向け、件数制限付き）。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param limit 取得件数の上限
     * @return 注文リスト（最大 {@code limit} 件）
     */
    @Transactional(readOnly = true)
    public List<Order> listAll(int limit) {
        return orderRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    /**
     * 指定注文の注文明細を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param orderId 注文 ID
     * @return 注文明細のリスト
     */
    @Transactional(readOnly = true)
    public List<OrderItem> listItems(String orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * 注文のステータスを更新する。
     *
     * @param orderId 更新対象の注文 ID
     * @param status  新しいステータス（例: {@code CONFIRMED}, {@code SHIPPED}, {@code CANCELLED}）
     * @throws ResourceNotFoundException 指定 ID の注文が存在しない場合
     */
    @Transactional
    public void updateStatus(String orderId, String status) {
        if (!ALLOWED_ORDER_STATUSES.contains(status)) {
            throw new BusinessException("Invalid order status: " + status);
        }
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }

    /**
     * 注文の決済ステータスを更新する。
     *
     * @param orderId       更新対象の注文 ID
     * @param paymentStatus 新しい決済ステータス（例: {@code AUTHORIZED}, {@code VOID}, {@code REFUNDED}）
     * @throws ResourceNotFoundException 指定 ID の注文が存在しない場合
     */
    @Transactional
    public void updatePaymentStatus(String orderId, String paymentStatus) {
        if (!ALLOWED_PAYMENT_STATUSES.contains(paymentStatus)) {
            throw new BusinessException("Invalid payment status: " + paymentStatus);
        }
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }

    /**
     * 返品レコードを記録する。
     *
     * <p>各注文明細に対して返品レコードを作成し、ステータスを {@code REQUESTED} に設定する。
     * {@link CheckoutService#returnOrder} から呼び出される。</p>
     *
     * @param orderId 返品対象の注文 ID
     * @param items   返品対象の注文明細リスト
     */
    @Transactional
    public void recordReturn(String orderId, List<OrderItem> items) {
        for (OrderItem item : items) {
            var ret = new Return();
            ret.setId(UUID.randomUUID().toString());
            ret.setOrderId(orderId);
            ret.setOrderItemId(item.getId());
            ret.setReason("return");
            ret.setQuantity(item.getQuantity());
            ret.setRefundAmount(item.getSubtotal());
            ret.setStatus(AppConstants.RETURN_STATUS_REQUESTED);
            returnRepository.save(ret);
        }
    }

    /**
     * 注文エンティティを構築する（永続化はしない）。
     *
     * <p>{@link CheckoutService#placeOrder} から呼び出され、
     * 各金額フィールドを設定した注文オブジェクトを返す。
     * ステータスは {@code CREATED}、決済ステータスは {@code AUTHORIZED} に初期設定される。</p>
     *
     * @param request 注文構築リクエスト（{@link OrderBuildRequest}）
     * @return 構築済みの注文エンティティ（未保存）
     */
    public Order buildOrder(OrderBuildRequest request) {
        var order = new Order();
        order.setId(request.orderId());
        order.setOrderNumber(request.orderNumber());
        order.setUserId(request.userId());
        order.setStatus(AppConstants.ORDER_STATUS_CREATED);
        order.setPaymentStatus(AppConstants.PAYMENT_STATUS_AUTHORIZED);
        order.setSubtotal(request.subtotal());
        order.setTax(request.tax());
        order.setShippingFee(request.shippingFee());
        order.setDiscountAmount(request.discountAmount());
        order.setTotalAmount(request.totalAmount());
        order.setCouponCode(request.couponCode());
        order.setUsedPoints(request.usedPoints());
        return order;
    }

    /**
     * 注文明細リストをカートアイテムリストに変換する。
     *
     * <p>注文キャンセル・返品時の在庫解放処理で、
     * {@link InventoryService#releaseItems} に渡すための変換に使用する。</p>
     *
     * @param items 変換元の注文明細リスト
     * @return カートアイテムリスト（商品 ID と数量のみ設定）
     */
    public List<CartItem> toCartItems(List<OrderItem> items) {
        List<CartItem> cartItems = new ArrayList<>();
        for (OrderItem orderItem : items) {
            var cartItem = new CartItem();
            cartItem.setProductId(orderItem.getProductId());
            cartItem.setQuantity(orderItem.getQuantity());
            cartItems.add(cartItem);
        }
        return cartItems;
    }
}
