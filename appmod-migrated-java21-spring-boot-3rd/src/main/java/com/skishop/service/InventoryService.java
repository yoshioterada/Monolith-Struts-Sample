package com.skishop.service;

import com.skishop.exception.BusinessException;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.CartItem;
import com.skishop.model.Inventory;
import com.skishop.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在庫管理サービス。
 *
 * <p>スキー用品の在庫数と予約数を管理し、チェックアウト時の在庫確保・解放・減算を担当する。
 * 在庫は {@code quantity}（総在庫数）と {@code reservedQuantity}（予約済み数）で管理され、
 * 利用可能在庫は {@code quantity - reservedQuantity} で計算される。</p>
 *
 * <p>チェックアウトフローにおける在庫操作の流れ:</p>
 * <ol>
 *   <li>{@link #reserveItems} — 注文確定時に在庫を仮確保（予約数を加算）</li>
 *   <li>{@link #deductStock} — 出荷確定時に在庫を実減算（総在庫数を減算）</li>
 *   <li>{@link #releaseItems} — キャンセル・返品時に予約を解放（予約数を減算）</li>
 * </ol>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link InventoryRepository} — 在庫エンティティの永続化</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see CheckoutService#cancelOrder
 * @see InventoryRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * カートアイテムに対応する在庫を仮確保（予約）する。
     *
     * <p>各アイテムの利用可能在庫（{@code quantity - reservedQuantity}）が
     * 必要数量を満たすか検証し、十分であれば予約数を加算する。
     * 在庫不足の商品がある場合は即座に例外をスローする。</p>
     *
     * <p><strong>パフォーマンス注記:</strong> 個別 SELECT + UPDATE（N アイテム × 2 クエリ）だが、
     * 各在庫レコードに {@code @Version} 楽観ロックが設定されているため、
     * バルク UPDATE に変更すると競合検出が困難になる。現行方式を維持する。</p>
     *
     * @param items 在庫を確保するカートアイテムのリスト
     * @throws BusinessException          在庫が不足している場合
     * @throws ResourceNotFoundException 商品に対応する在庫レコードが存在しない場合
     */
    @Transactional
    public void reserveItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

        for (CartItem item : items) {
            var inventory = inventoryMap.get(item.getProductId());
            if (inventory == null) {
                throw new ResourceNotFoundException("Inventory", item.getProductId());
            }
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + item.getProductId(),
                        "redirect:/cart", "error.stock.insufficient");
            }
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            inventoryRepository.saveAndFlush(inventory);
        }
    }

    /**
     * 仮確保（予約）されていた在庫を解放する。
     *
     * <p>注文キャンセルや返品時に呼び出され、予約数量を減算する。
     * 予約数が 0 未満にならないよう保護される。
     * 在庫レコードが存在しない場合はスキップする。</p>
     *
     * @param items 在庫を解放するカートアイテムのリスト
     * @see CheckoutService#cancelOrder
     * @see CheckoutService#returnOrder
     */
    @Transactional
    public void releaseItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

        for (CartItem item : items) {
            var inventory = inventoryMap.get(item.getProductId());
            if (inventory != null) {
                int newReserved = inventory.getReservedQuantity() - item.getQuantity();
                inventory.setReservedQuantity(Math.max(newReserved, 0));
                inventoryRepository.saveAndFlush(inventory);
            }
        }
    }

    /**
     * 在庫を実際に減算する（出荷確定時）。
     *
     * <p>総在庫数（{@code quantity}）と予約数（{@code reservedQuantity}）の両方を減算する。
     * 予約数が 0 未満にならないよう保護される。</p>
     *
     * @param items 在庫を減算するカートアイテムのリスト
     * @throws ResourceNotFoundException 商品に対応する在庫レコードが存在しない場合
     */
    @Transactional
    public void deductStock(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

        for (CartItem item : items) {
            var inventory = inventoryMap.get(item.getProductId());
            if (inventory == null) {
                throw new ResourceNotFoundException("Inventory", item.getProductId());
            }
            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventory.setReservedQuantity(Math.max(inventory.getReservedQuantity() - item.getQuantity(), 0));
            inventoryRepository.saveAndFlush(inventory);
        }
    }

    /**
     * 指定商品の在庫が十分かを確認する。
     *
     * <p>利用可能在庫（{@code quantity - reservedQuantity}）が要求数量以上であるかを判定する。
     * 在庫レコードが存在しない場合は {@code false} を返す。</p>
     *
     * @param productId 確認対象の商品 ID
     * @param quantity  必要な数量
     * @return 在庫が十分な場合 {@code true}、不足または在庫レコード未登録の場合 {@code false}
     */
    @Transactional(readOnly = true)
    public boolean checkStock(String productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> (inv.getQuantity() - inv.getReservedQuantity()) >= quantity)
                .orElse(false);
    }
}
