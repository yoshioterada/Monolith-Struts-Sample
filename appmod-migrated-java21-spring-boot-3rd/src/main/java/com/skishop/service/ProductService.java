package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.Product;
import com.skishop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 *   <li>{@link ProductRepository} — 商品エンティティの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.ProductController
 * @see com.skishop.controller.admin.AdminProductController
 * @see ProductRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String STATUS_INACTIVE = AppConstants.STATUS_INACTIVE;

    private final ProductRepository productRepository;

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
            String escapedKeyword = "%" + escapeLikePattern(keyword.toLowerCase()) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), escapedKeyword, '\\'),
                            cb.like(cb.lower(root.get("description")), escapedKeyword, '\\')
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
     * 複数商品 ID で商品を一括取得する。
     *
     * @param productIds 商品 ID のリスト
     * @return 商品 ID をキーとする商品マップ
     */
    @Transactional(readOnly = true)
    public Map<String, Product> findAllByIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    /**
     * LIKE パターン内の特殊文字（%, _, \）をエスケープする。
     */
    private static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
