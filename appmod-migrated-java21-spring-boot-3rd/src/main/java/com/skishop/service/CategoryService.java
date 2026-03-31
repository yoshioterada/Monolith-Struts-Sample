package com.skishop.service;

import com.skishop.model.Category;
import com.skishop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品カテゴリ管理サービス。
 *
 * <p>スキー用品のカテゴリ階層（例: スキー板 → アルペンスキー、クロスカントリー等）の
 * 参照機能を提供する。カテゴリは親子関係を持つツリー構造で管理される。</p>
 *
 * <p>全メソッドが読み取り専用トランザクションで実行される。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link CategoryRepository} — カテゴリエンティティの参照</li>
 * </ul>
 *
 * @see com.skishop.controller.ProductController
 * @see CategoryRepository
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 全カテゴリを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。階層に関わらず全件を返す。</p>
     *
     * @return 全カテゴリのリスト
     */
    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    /**
     * ルートカテゴリ（親カテゴリを持たない最上位カテゴリ）を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。
     * サイドバーやトップページのカテゴリメニュー表示に使用される。</p>
     *
     * @return ルートカテゴリのリスト
     */
    @Transactional(readOnly = true)
    public List<Category> listRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    /**
     * 指定親カテゴリ直下のサブカテゴリを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param parentId 親カテゴリ ID
     * @return 指定親カテゴリに属するサブカテゴリのリスト
     */
    @Transactional(readOnly = true)
    public List<Category> listByParentId(String parentId) {
        return categoryRepository.findByParentId(parentId);
    }
}
