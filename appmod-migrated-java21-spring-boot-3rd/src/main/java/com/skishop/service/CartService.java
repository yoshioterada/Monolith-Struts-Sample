package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.exception.BusinessException;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Cart;
import com.skishop.model.CartItem;
import com.skishop.model.Price;
import com.skishop.repository.CartItemRepository;
import com.skishop.repository.CartRepository;
import com.skishop.repository.PriceRepository;
import com.skishop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ショッピングカート管理サービス。
 *
 * <p>スキー用品 EC サイトにおけるカートのライフサイクル全体を管理する。
 * 未ログインユーザーにはセッション ID ベースのカートを、
 * ログイン済みユーザーにはユーザー ID ベースのカートを提供する。</p>
 *
 * <p>主な機能:</p>
 * <ul>
 *   <li>カートの取得・作成（セッション / ユーザー）</li>
 *   <li>カートアイテムの追加・数量変更・削除</li>
 *   <li>小計金額の計算</li>
 *   <li>ログイン時のセッションカートとユーザーカートのマージ</li>
 *   <li>チェックアウト完了後のカートクリア</li>
 * </ul>
 *
 * <p>カートの有効期限は作成から 30 日間。ステータスは以下の遷移を持つ:</p>
 * <ul>
 *   <li>{@code ACTIVE} → {@code CHECKED_OUT}（チェックアウト完了時）</li>
 *   <li>{@code ACTIVE} → {@code MERGED}（ログイン時のセッションカートマージ時）</li>
 * </ul>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link CartRepository} — カートエンティティの永続化</li>
 *   <li>{@link CartItemRepository} — カートアイテムエンティティの永続化</li>
 *   <li>{@link PriceRepository} — 商品の現在価格（通常価格・セール価格）の解決</li>
 * </ul>
 *
 * @see com.skishop.controller.CartController
 * @see CheckoutService
 * @see CartRepository
 * @see CartItemRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;

    private static final int MAX_CART_ITEMS = 50;

    /**
     * カート ID でカートを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param cartId カート ID
     * @return 該当するカートエンティティ
     * @throws ResourceNotFoundException 指定 ID のカートが存在しない場合
     */
    @Transactional(readOnly = true)
    public Cart getCart(String cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
    }

    /**
     * ユーザー情報またはセッション情報からカートを解決する。
     *
     * <p>ログインユーザーの場合はユーザーカートを取得または作成し、
     * 未ログインユーザーの場合は既存カート ID またはセッション ID でカートを解決する。</p>
     *
     * @param userId    ユーザー ID（未ログインの場合は {@code null}）
     * @param sessionId HTTP セッション ID
     * @param cartId    セッションに保存されたカート ID（未ログインで初回訪問の場合は {@code null}）
     * @return 解決されたカート
     */
    @Transactional
    public Cart resolveCart(String userId, String sessionId, String cartId) {
        if (userId != null) {
            return getOrCreateCart(userId, sessionId);
        }
        if (cartId != null) {
            return getCart(cartId);
        }
        return createCart(null, sessionId);
    }

    /**
     * 既存のアクティブカートを取得するか、存在しない場合は新規作成する。
     *
     * <p>以下の優先順位でカートを検索する:</p>
     * <ol>
     *   <li>{@code userId} が指定されている場合、ユーザーに紐づくアクティブカートを検索</li>
     *   <li>{@code sessionId} が指定されている場合、セッション ID に紐づくカートを検索</li>
     *   <li>いずれも見つからない場合、新規カートを作成</li>
     * </ol>
     *
     * @param userId    ユーザー ID（未ログインの場合は {@code null}）
     * @param sessionId HTTP セッション ID（未ログインユーザーのカート識別に使用）
     * @return 既存または新規作成されたカート
     */
    @Transactional
    public Cart getOrCreateCart(String userId, String sessionId) {
        if (userId != null) {
            List<Cart> activeCarts = cartRepository.findByUserIdAndStatus(userId, AppConstants.STATUS_ACTIVE);
            if (!activeCarts.isEmpty()) {
                return activeCarts.getFirst();
            }
        }
        if (sessionId != null) {
            return cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createCart(userId, sessionId));
        }
        return createCart(userId, sessionId);
    }

    /**
     * 新規カートを作成する。
     *
     * <p>UUID を自動生成し、ステータスを {@code ACTIVE}、有効期限を 30 日後に設定する。</p>
     *
     * @param userId    ユーザー ID（未ログインの場合は {@code null}）
     * @param sessionId HTTP セッション ID
     * @return 作成されたカートエンティティ
     */
    @Transactional
    public Cart createCart(String userId, String sessionId) {
        var cart = new Cart();
        cart.setId(UUID.randomUUID().toString());
        cart.setUserId(userId);
        cart.setSessionId(sessionId);
        cart.setStatus(AppConstants.STATUS_ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusDays(30));
        return cartRepository.save(cart);
    }

    /**
     * 指定カートに含まれる全アイテムを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param cartId カート ID
     * @return カートアイテムのリスト（空の場合は空リスト）
     */
    @Transactional(readOnly = true)
    public List<CartItem> getItems(String cartId) {
        return cartItemRepository.findByCartId(cartId);
    }

    /**
     * カートに商品を追加する。
     *
     * <p>同一商品が既にカート内に存在する場合は数量を加算する。
     * 新規商品の場合は価格を自動解決（セール価格優先）してアイテムを作成する。
     * 数量が 0 以下の場合は何もしない。</p>
     *
     * @param cartId    カート ID
     * @param productId 追加する商品の ID
     * @param quantity  追加数量（1 以上）
     * @throws ResourceNotFoundException 指定 ID のカートが存在しない場合
     */
    @Transactional
    public void addItem(String cartId, String productId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        var cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));

        var existingItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (existingItem.isEmpty() && cartItemRepository.countByCartId(cartId) >= MAX_CART_ITEMS) {
            throw new BusinessException("Cart item limit reached",
                    "redirect:/cart", "error.cart.limit");
        }
        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
            cartItemRepository.save(existingItem.get());
            return;
        }

        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        var item = new CartItem();
        item.setId(UUID.randomUUID().toString());
        item.setCart(cart);
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setQuantity(quantity);
        item.setUnitPrice(resolveUnitPrice(productId));
        cartItemRepository.save(item);
    }

    /**
     * カートアイテムの数量を更新する。
     *
     * <p>数量が 0 以下に設定された場合、該当アイテムをカートから削除する。</p>
     *
     * @param itemId   カートアイテム ID
     * @param quantity 新しい数量（0 以下の場合は削除）
     * @throws ResourceNotFoundException 指定 ID のカートアイテムが存在しない場合
     */
    @Transactional
    public void updateItemQuantity(String itemId, int quantity, String cartId) {
        var item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        if (!cartId.equals(item.getCart().getId())) {
            throw new ResourceNotFoundException("CartItem", itemId);
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    /**
     * 指定アイテムをカートから削除する。
     *
     * @param itemId 削除対象のカートアイテム ID
     */
    @Transactional
    public void removeItem(String itemId, String cartId) {
        var item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        if (!cartId.equals(item.getCart().getId())) {
            throw new ResourceNotFoundException("CartItem", itemId);
        }
        cartItemRepository.delete(item);
    }

    /**
     * カートアイテムの小計金額を計算する。
     *
     * <p>各アイテムの {@code 単価 × 数量} を合計して返す。
     * アイテムリストが {@code null} または空の場合は {@link BigDecimal#ZERO} を返す。</p>
     *
     * @param items カートアイテムのリスト
     * @return 小計金額（税抜き）
     */
    public BigDecimal calculateSubtotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * カートをクリアする（チェックアウト完了時）。
     *
     * <p>カート内の全アイテムを削除し、カートのステータスを {@code CHECKED_OUT} に変更する。</p>
     *
     * @param cartId クリア対象のカート ID
     * @throws ResourceNotFoundException 指定 ID のカートが存在しない場合
     */
    @Transactional
    public void clearCart(String cartId) {
        cartItemRepository.deleteByCartId(cartId);
        var cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
        cart.setStatus(AppConstants.CART_STATUS_CHECKED_OUT);
        cartRepository.save(cart);
    }

    /**
     * セッションカートをユーザーカートにマージする。
     *
     * <p>未ログイン時に作成されたセッションカートを、ログイン後のユーザーカートに統合する。
     * {@code CartMergeSuccessHandler} から呼び出される。</p>
     *
     * <p>マージの動作:</p>
     * <ul>
     *   <li>ユーザーにアクティブカートが無い場合: セッションカートの所有者をユーザーに変更</li>
     *   <li>ユーザーにアクティブカートがある場合: セッションカートの全アイテムをユーザーカートに追加し、
     *       セッションカートのステータスを {@code MERGED} に変更</li>
     * </ul>
     *
     * @param sessionId マージ元のセッション ID
     * @param userId    マージ先のユーザー ID
     */
    @Transactional
    public void mergeSessionCart(String sessionId, String userId) {
        var sessionCart = cartRepository.findBySessionId(sessionId);
        if (sessionCart.isEmpty()) {
            return;
        }
        mergeCart(sessionCart.orElseThrow(), userId);
    }

    /**
     * カート ID を指定してカートをユーザーにマージする。
     *
     * <p>{@link com.skishop.config.CartMergeSuccessHandler} から呼び出される。
     * セッション属性に保存されたカート ID で匿名カートを特定し、
     * ログインユーザーのカートにマージする。</p>
     *
     * @param cartId マージ元のカート ID
     * @param userId マージ先のユーザー ID
     */
    @Transactional
    public void mergeCartById(String cartId, String userId) {
        var cartOpt = cartRepository.findById(cartId);
        if (cartOpt.isEmpty()) {
            return;
        }
        mergeCart(cartOpt.orElseThrow(), userId);
    }

    private void mergeCart(Cart sourceCart, String userId) {
        List<Cart> userCarts = cartRepository.findByUserIdAndStatus(userId, AppConstants.STATUS_ACTIVE);
        if (userCarts.isEmpty()) {
            sourceCart.setUserId(userId);
            cartRepository.save(sourceCart);
        } else {
            var userCart = userCarts.getFirst();
            List<CartItem> sessionItems = cartItemRepository.findByCartId(sourceCart.getId());
            List<CartItem> existingItems = cartItemRepository.findByCartId(userCart.getId());

            // Build lookup map of existing items by productId
            Map<String, CartItem> existingMap = existingItems.stream()
                    .collect(Collectors.toMap(CartItem::getProductId, item -> item, (a, b) -> a));

            for (CartItem sessionItem : sessionItems) {
                CartItem existing = existingMap.get(sessionItem.getProductId());
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + sessionItem.getQuantity());
                    cartItemRepository.save(existing);
                } else {
                    var newItem = new CartItem();
                    newItem.setId(UUID.randomUUID().toString());
                    newItem.setCart(userCart);
                    newItem.setProductId(sessionItem.getProductId());
                    newItem.setProductName(sessionItem.getProductName());
                    newItem.setQuantity(sessionItem.getQuantity());
                    newItem.setUnitPrice(sessionItem.getUnitPrice());
                    cartItemRepository.save(newItem);
                }
            }

            cartItemRepository.deleteByCartId(sourceCart.getId());
            sourceCart.setStatus(AppConstants.CART_STATUS_MERGED);
            cartRepository.save(sourceCart);
        }
    }

    private BigDecimal resolveUnitPrice(String productId) {
        List<Price> prices = priceRepository.findByProductId(productId);
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var price = prices.getFirst();
        if (price.getRegularPrice() == null) {
            return BigDecimal.ZERO;
        }
        if (price.getSalePrice() != null && isSaleActive(price)) {
            return price.getSalePrice();
        }
        return price.getRegularPrice();
    }

    private boolean isSaleActive(Price price) {
        LocalDateTime now = LocalDateTime.now();
        if (price.getSaleStartDate() != null && price.getSaleStartDate().isAfter(now)) {
            return false;
        }
        return price.getSaleEndDate() == null || !price.getSaleEndDate().isBefore(now);
    }
}
