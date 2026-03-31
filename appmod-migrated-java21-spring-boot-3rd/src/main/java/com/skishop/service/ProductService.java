package com.skishop.service;

import com.skishop.dto.request.admin.AdminProductRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Inventory;
import com.skishop.model.Price;
import com.skishop.model.Product;
import com.skishop.repository.CategoryRepository;
import com.skishop.repository.InventoryRepository;
import com.skishop.repository.PriceRepository;
import com.skishop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 商品管理サービス。
 *
 * <p>スキー用品の検索・参照・管理を担当する。一般ユーザー向けの商品検索・カテゴリ別一覧と、
 * 管理者向けの商品 CRUD 操作（商品・価格・在庫の一括管理）を提供する。</p>
 *
 * <p>商品検索（{@link #search}）は JPA Specification を使用した動的クエリで、
 * キーワード（商品名・説明文の部分一致）とカテゴリ ID による絞り込みに対応する。
 * ステータスが {@code INACTIVE} の商品は検索結果から除外される。</p>
 *
 * <p>管理者向け操作（作成・更新）では商品エンティティに加え、
 * 価格（{@link Price}）と在庫（{@link Inventory}）も同一トランザクションで管理する。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link ProductRepository} — 商品エンティティの永続化</li>
 *   <li>{@link CategoryRepository} — カテゴリの参照</li>
 *   <li>{@link InventoryRepository} — 在庫エンティティの管理</li>
 *   <li>{@link PriceRepository} — 価格エンティティの管理</li>
 * </ul>
 *
 * @see com.skishop.controller.ProductController
 * @see com.skishop.controller.AdminController
 * @see ProductRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final PriceRepository priceRepository;

    /**
     * 商品 ID で商品を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param productId 商品 ID
     * @return 該当する商品エンティティ
     * @throws ResourceNotFoundException 指定 ID の商品が存在しない場合
     */
    @Transactional(readOnly = true)
    public Product findById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    /**
     * キーワードとカテゴリで商品を検索する。
     *
     * <p>JPA Specification を使用した動的クエリで、以下の条件を組み合わせる:</p>
     * <ul>
     *   <li>キーワード: 商品名または説明文の大文字小文字を区別しない部分一致</li>
     *   <li>カテゴリ ID: 完全一致</li>
     *   <li>ステータスが {@code INACTIVE} の商品を除外（常時適用）</li>
     * </ul>
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param keyword    検索キーワード（{@code null} または空白の場合はキーワード条件なし）
     * @param categoryId カテゴリ ID（{@code null} または空白の場合はカテゴリ条件なし）
     * @param pageable   ページネーション・ソート情報
     * @return 検索結果のページ
     */
    @Transactional(readOnly = true)
    public Page<Product> search(String keyword, String categoryId, Pageable pageable) {
        Specification<Product> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                    )
            );
        }

        if (categoryId != null && !categoryId.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("categoryId"), categoryId)
            );
        }

        spec = spec.and((root, query, cb) ->
                cb.notEqual(root.get("status"), STATUS_INACTIVE)
        );

        return productRepository.findAll(spec, pageable);
    }

    /**
     * 指定カテゴリに属する商品を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param categoryId カテゴリ ID
     * @return 当該カテゴリの商品リスト
     */
    @Transactional(readOnly = true)
    public List<Product> findByCategoryId(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    /**
     * 指定ステータスの商品を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param status 商品ステータス（例: {@code ACTIVE}, {@code INACTIVE}, {@code OUT_OF_STOCK}）
     * @return 当該ステータスの商品リスト
     */
    @Transactional(readOnly = true)
    public List<Product> findByStatus(String status) {
        return productRepository.findByStatus(status);
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

    /**
     * 新しい商品を作成する（管理者専用）。
     *
     * <p>{@code ADMIN} ロールが必要。商品エンティティに加え、
     * 価格（{@link Price}）と在庫（{@link Inventory}）を同一トランザクションで作成する。
     * 通貨コードはデフォルトで {@code JPY} が設定される。</p>
     *
     * @param request 商品作成リクエスト（名前、ブランド、説明、カテゴリ、通常価格、セール価格、在庫数）
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
     * <p>{@code ADMIN} ロールが必要。商品エンティティに加え、
     * 価格（{@link Price}）と在庫（{@link Inventory}）を同一トランザクションで更新する。</p>
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
            var price = prices.get(0);
            price.setRegularPrice(request.regularPrice());
            price.setSalePrice(request.salePrice());
            priceRepository.save(price);
        }
        inventoryRepository.findByProductId(productId).ifPresent(inventory -> {
            inventory.setQuantity(request.inventoryQty());
            inventory.setStatus(request.inventoryQty() > 0 ? "IN_STOCK" : STATUS_OUT_OF_STOCK);
            inventoryRepository.save(inventory);
        });
        return productRepository.save(product);
    }

    private void populateProduct(Product product, AdminProductRequest request) {
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setStatus(request.status());
    }
}
