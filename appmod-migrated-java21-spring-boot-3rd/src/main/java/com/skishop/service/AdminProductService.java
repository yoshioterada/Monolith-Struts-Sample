package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.dto.request.admin.AdminProductRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Inventory;
import com.skishop.model.Price;
import com.skishop.model.Product;
import com.skishop.repository.InventoryRepository;
import com.skishop.repository.PriceRepository;
import com.skishop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 管理者向け商品管理サービス。
 *
 * <p>商品の作成・更新・無効化（論理削除）を担当する管理者専用サービス。
 * 商品エンティティに加え、価格（{@link Price}）と在庫（{@link Inventory}）を
 * 同一トランザクションで一括管理する。</p>
 *
 * <p>全メソッドに {@code @PreAuthorize("hasRole('ADMIN')")} が付与されており、
 * {@code ADMIN} ロールのユーザーのみ実行可能。</p>
 *
 * @see com.skishop.controller.admin.AdminProductController
 * @see ProductService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductService {

    private static final String STATUS_INACTIVE = AppConstants.STATUS_INACTIVE;
    private static final String STATUS_OUT_OF_STOCK = AppConstants.INVENTORY_STATUS_OUT_OF_STOCK;

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PriceRepository priceRepository;

    /**
     * 新しい商品を作成する（管理者専用）。
     *
     * <p>商品エンティティに加え、価格と在庫を同一トランザクションで作成する。
     * 通貨コードはデフォルトで {@code JPY} が設定される。</p>
     *
     * @param request 商品作成リクエスト
     * @return 作成された商品エンティティ
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product createProduct(AdminProductRequest request) {
        var product = new Product();
        product.setId(UUID.randomUUID().toString().substring(0, 20));
        populateProduct(product, request);
        var saved = productRepository.save(product);

        var price = new Price();
        price.setId(UUID.randomUUID().toString());
        price.setProductId(saved.getId());
        price.setRegularPrice(request.regularPrice());
        price.setSalePrice(request.salePrice());
        price.setCurrencyCode("JPY");
        priceRepository.save(price);

        var inventory = new Inventory();
        inventory.setId(UUID.randomUUID().toString());
        inventory.setProductId(saved.getId());
        inventory.setQuantity(request.inventoryQty());
        inventory.setStatus(request.inventoryQty() > 0 ? "IN_STOCK" : STATUS_OUT_OF_STOCK);
        inventoryRepository.save(inventory);
        return saved;
    }

    /**
     * 既存の商品を更新する（管理者専用）。
     *
     * <p>商品エンティティに加え、価格と在庫を同一トランザクションで更新する。</p>
     *
     * @param productId 更新対象の商品 ID
     * @param request   更新内容を含むリクエスト
     * @return 更新後の商品エンティティ
     * @throws ResourceNotFoundException 指定 ID の商品が存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product updateProduct(String productId, AdminProductRequest request) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        populateProduct(product, request);
        var prices = priceRepository.findByProductId(productId);
        if (!prices.isEmpty()) {
            var price = prices.getFirst();
            price.setRegularPrice(request.regularPrice());
            price.setSalePrice(request.salePrice());
            priceRepository.save(price);
        }
        inventoryRepository.findByProductId(productId).ifPresent(inventory -> {
            inventory.setQuantity(request.inventoryQty());
            inventory.setStatus(request.inventoryQty() > 0 ? AppConstants.INVENTORY_STATUS_IN_STOCK : STATUS_OUT_OF_STOCK);
            inventoryRepository.save(inventory);
        });
        return productRepository.save(product);
    }

    /**
     * 商品を無効化（非アクティブ）にする。
     *
     * <p>商品のステータスを {@code INACTIVE} に変更し、
     * 在庫の数量を 0、ステータスを {@code OUT_OF_STOCK} に設定する。</p>
     *
     * @param productId 無効化対象の商品 ID
     * @throws ResourceNotFoundException 指定 ID の商品が存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivateProduct(String productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        product.setStatus(STATUS_INACTIVE);
        productRepository.save(product);

        inventoryRepository.findByProductId(productId).ifPresent(inventory -> {
            inventory.setQuantity(0);
            inventory.setStatus(STATUS_OUT_OF_STOCK);
            inventoryRepository.save(inventory);
        });
    }

    private void populateProduct(Product product, AdminProductRequest request) {
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setStatus(request.status());
    }
}
