package com.skishop.controller.admin;

import com.skishop.constant.AppConstants;
import com.skishop.dto.request.admin.AdminProductRequest;
import com.skishop.model.Product;
import com.skishop.service.AdminProductService;
import com.skishop.service.CategoryService;
import com.skishop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 管理者向け商品管理の HTTP リクエストを処理するコントローラー。
 *
 * <p>商品の一覧表示、新規作成画面表示、商品作成、編集画面表示、商品更新、商品削除（論理削除）を行う。
 * ベース URL: {@code /admin/products}</p>
 *
 * <p>Struts 移行元: {@code AdminProductAction}, {@code AdminProductEditAction},
 * {@code AdminProductDeleteAction}</p>
 *
 * <p>認可: 全エンドポイントは {@code ADMIN} ロールのみアクセス可能
 * （クラスレベル {@code @PreAuthorize("hasRole('ADMIN')")}）。</p>
 *
 * @see ProductService
 * @see CategoryService
 */
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;
    private final AdminProductService adminProductService;
    private final CategoryService categoryService;

    /**
     * 商品一覧画面を表示する（管理者用）。
     *
     * <p>{@code GET /admin/products} — アクティブな商品の一覧を取得し、管理用商品一覧画面を表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（{@code products} を格納）
     * @return {@code "admin/products/list"} 管理用商品一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findByStatus("ACTIVE"));
        return "admin/products/list";
    }

    /**
     * 商品新規作成画面を表示する。
     *
     * <p>{@code GET /admin/products/new} — 空の商品作成フォームとカテゴリ一覧を表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（空の {@code adminProductRequest}, {@code categories} を格納）
     * @return {@code "admin/products/form"} 商品作成・編集画面のテンプレート名
     */
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("adminProductRequest",
                new AdminProductRequest(null, "", "", "", "", null, null, AppConstants.STATUS_ACTIVE, 0));
        model.addAttribute("categories", categoryService.listAll());
        return "admin/products/form";
    }

    /**
     * 商品を新規作成する。
     *
     * <p>{@code POST /admin/products} — バリデーション済みの商品情報を受け取り、新規商品を作成する。
     * 成功時は作成された商品の編集画面にリダイレクトする。
     * バリデーションエラー時は商品作成画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param request 商品作成リクエスト（商品名、ブランド、説明、カテゴリ ID、価格、在庫数、ステータス等）
     * @param result バリデーション結果
     * @param model ビューに渡すモデル（バリデーションエラー時にカテゴリ一覧を再格納）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（作成完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/products/{id}"}, バリデーションエラー時: {@code "admin/products/form"}
     */
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("adminProductRequest") AdminProductRequest request,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.listAll());
            return "admin/products/form";
        }
        Product product = adminProductService.createProduct(request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.product.created");
        return "redirect:/admin/products/" + product.getId();
    }

    /**
     * 商品編集画面を表示する。
     *
     * <p>{@code GET /admin/products/{id}} — 指定された商品の情報を取得し、編集フォームを表示する。
     * カテゴリ一覧もモデルに格納する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 編集対象の商品 ID（パスパラメータ、UUID 形式）
     * @param model ビューに渡すモデル（{@code product}, {@code adminProductRequest}, {@code categories} を格納）
     * @return {@code "admin/products/form"} 商品作成・編集画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String editProductForm(@PathVariable String id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("adminProductRequest",
                new AdminProductRequest(product.getId(), product.getName(), product.getBrand(),
                        product.getDescription(), product.getCategoryId(), null, null,
                        product.getStatus(), 0));
        model.addAttribute("categories", categoryService.listAll());
        return "admin/products/form";
    }

    /**
     * 商品情報を更新する。
     *
     * <p>{@code PUT /admin/products/{id}} — バリデーション済みの商品情報で既存商品を更新する。
     * バリデーションエラー時は商品編集画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 更新対象の商品 ID（パスパラメータ、UUID 形式）
     * @param request 商品更新リクエスト（商品名、ブランド、説明、カテゴリ ID、価格、在庫数、ステータス等）
     * @param result バリデーション結果
     * @param model ビューに渡すモデル（バリデーションエラー時にカテゴリ一覧を再格納）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（更新完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/products/{id}"}, バリデーションエラー時: {@code "admin/products/form"}
     */
    @PutMapping("/{id}")
    public String updateProduct(@PathVariable String id,
                                 @Valid @ModelAttribute("adminProductRequest") AdminProductRequest request,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.listAll());
            return "admin/products/form";
        }
        adminProductService.updateProduct(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.product.updated");
        return "redirect:/admin/products/" + id;
    }

    /**
     * 商品を削除（論理削除）する。
     *
     * <p>{@code DELETE /admin/products/{id}} — 指定された商品を非アクティブ化（論理削除）する。
     * 物理削除ではなく、ステータスを変更することでカタログから非表示にする。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 削除対象の商品 ID（パスパラメータ、UUID 形式）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（削除完了メッセージ）
     * @return {@code "redirect:/admin/products"} 管理用商品一覧へリダイレクト
     */
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable String id, RedirectAttributes redirectAttributes) {
        adminProductService.deactivateProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "admin.product.deleted");
        return "redirect:/admin/products";
    }
}
