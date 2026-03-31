package com.skishop.controller;

import com.skishop.model.Product;
import com.skishop.service.CategoryService;
import com.skishop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>商品の一覧表示（キーワード検索・カテゴリ絞り込み・ソート・ページネーション対応）
 * および商品詳細表示を行う。ベース URL: {@code /products}</p>
 *
 * <p>Struts 移行元: {@code ProductAction}, {@code ProductListAction}, {@code ProductDetailAction}</p>
 *
 * <p>認可: 全エンドポイントは未認証ユーザーを含む全ユーザーがアクセス可能（{@code permitAll}）。</p>
 *
 * @see ProductService
 * @see CategoryService
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 商品一覧画面を表示する。
     *
     * <p>{@code GET /products} — キーワード検索、カテゴリ絞り込み、ソート、ページネーションに対応した
     * 商品一覧画面を表示する。ソート順は価格昇順・降順、名前昇順、作成日降順（デフォルト）から選択可能。</p>
     *
     * <p>認可: 全ユーザーがアクセス可能</p>
     *
     * @param keyword 検索キーワード（任意、商品名・説明文で部分一致検索）
     * @param categoryId カテゴリ ID による絞り込み（任意）
     * @param page ページ番号（1 始まり、デフォルト: 1）
     * @param size 1 ページあたりの表示件数（デフォルト: 20）
     * @param sort ソート順（{@code "price_asc"}, {@code "price_desc"}, {@code "name_asc"}、未指定時は作成日降順）
     * @param model ビューに渡すモデル（{@code products}, {@code keyword}, {@code categoryId},
     *              {@code sort}, {@code categories} を格納）
     * @return {@code "products/list"} 商品一覧画面のテンプレート名
     */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String categoryId,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String sort,
                        Model model) {
        Sort sortObj = resolveSort(sort);
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100), sortObj);
        Page<Product> products = productService.search(keyword, categoryId, pageable);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryService.listAll());
        return "products/list";
    }

    /**
     * 商品詳細画面を表示する。
     *
     * <p>{@code GET /products/{id}} — 指定された商品 ID の商品情報を取得し、詳細画面を表示する。</p>
     *
     * <p>認可: 全ユーザーがアクセス可能</p>
     *
     * @param id 商品 ID（パスパラメータ、UUID 形式）
     * @param model ビューに渡すモデル（{@code product} を格納）
     * @return {@code "products/detail"} 商品詳細画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "products/detail";
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
