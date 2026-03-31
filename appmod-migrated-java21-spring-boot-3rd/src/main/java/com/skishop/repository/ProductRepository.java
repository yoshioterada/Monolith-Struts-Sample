package com.skishop.repository;

import com.skishop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * {@link Product} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code products} テーブルに対する CRUD 操作および、カテゴリやステータスによる
 * 商品検索機能を提供する。{@link JpaSpecificationExecutor} を継承することで、
 * 動的な検索条件の組み立て（Specification パターン）にも対応する。</p>
 *
 * <p>主に {@link com.skishop.service.ProductService ProductService} から利用され、
 * 商品一覧・詳細表示、カテゴリ別商品フィルタリング、管理画面での商品管理を支える。
 * スキー板、ブーツ、ウェアなどのスキー用品が商品として管理される。</p>
 *
 * @see Product
 * @see com.skishop.model.Category
 * @see com.skishop.service.ProductService
 */
public interface ProductRepository extends JpaRepository<Product, String>,
        JpaSpecificationExecutor<Product> {

    /**
     * 指定されたカテゴリ ID に紐づく商品を検索する。
     *
     * <p>カテゴリ別商品一覧ページで使用される。指定カテゴリに直接属する商品のみを返し、
     * サブカテゴリの商品は含まない。サブカテゴリを含めた検索はサービス層で行う。</p>
     *
     * @param categoryId 検索対象のカテゴリ ID（null 不可）
     * @return 該当カテゴリの商品リスト。存在しない場合は空リスト
     */
    List<Product> findByCategoryId(String categoryId);

    /**
     * 指定されたステータスの商品を検索する。
     *
     * <p>公開中の商品一覧（{@code "ACTIVE"}）の取得や、管理画面での
     * 非公開商品（{@code "INACTIVE"}）の確認に使用される。</p>
     *
     * @param status 検索対象の商品ステータス（例: {@code "ACTIVE"}, {@code "INACTIVE"}）
     * @return 該当ステータスの商品リスト。存在しない場合は空リスト
     */
    List<Product> findByStatus(String status);
}
