package com.skishop.repository;

import com.skishop.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link Category} エンティティのデータアクセスを提供する Spring Data JPA リポジトリ。
 *
 * <p>{@code categories} テーブルに対する CRUD 操作および、階層構造を持つ
 * 商品カテゴリの検索機能を提供する。主に
 * {@link com.skishop.service.CategoryService CategoryService} および
 * {@link com.skishop.service.ProductService ProductService} から利用され、
 * カテゴリツリーの構築やカテゴリ別商品一覧の表示を支える。</p>
 *
 * <p>カテゴリは親子関係を持つ階層構造（例: スキー板 → カービングスキー）を
 * 形成しており、ルートカテゴリは {@code parent} が {@code null} となる。</p>
 *
 * @see Category
 * @see com.skishop.service.CategoryService
 * @see com.skishop.service.ProductService
 */
public interface CategoryRepository extends JpaRepository<Category, String> {

    /**
     * 親カテゴリを持たないルートカテゴリを全件検索する。
     *
     * <p>サイドバーやヘッダーのカテゴリナビゲーション構築時に、最上位カテゴリの一覧を
     * 取得するために使用される。取得したルートカテゴリから子カテゴリを辿ることで
     * カテゴリツリー全体を構築できる。</p>
     *
     * @return ルートカテゴリのリスト。存在しない場合は空リスト
     */
    List<Category> findByParentIsNull();

    /**
     * 指定された親カテゴリ ID に紐づく子カテゴリを検索する。
     *
     * <p>カテゴリツリーの展開やサブカテゴリ一覧の表示で使用される。
     * 指定された親カテゴリ直下の子カテゴリのみを返す（孫カテゴリは含まない）。</p>
     *
     * @param parentId 親カテゴリの ID（null 不可）
     * @return 子カテゴリのリスト。子カテゴリが存在しない場合は空リスト
     */
    List<Category> findByParentId(String parentId);
}
