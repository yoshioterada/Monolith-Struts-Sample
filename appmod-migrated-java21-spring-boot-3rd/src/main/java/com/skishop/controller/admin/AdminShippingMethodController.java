package com.skishop.controller.admin;

import com.skishop.dto.request.admin.AdminShippingMethodRequest;
import com.skishop.model.ShippingMethod;
import com.skishop.service.AdminShippingMethodService;
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
 * 管理者向け配送方法管理の HTTP リクエストを処理するコントローラー。
 *
 * <p>配送方法の一覧表示、新規作成、編集画面表示、更新、削除を行う。
 * ベース URL: {@code /admin/shipping-methods}</p>
 *
 * <p>Struts 移行元: {@code AdminShippingAction}, {@code AdminShippingEditAction}</p>
 *
 * <p>認可: 全エンドポイントは {@code ADMIN} ロールのみアクセス可能
 * （クラスレベル {@code @PreAuthorize("hasRole('ADMIN')")}）。</p>
 *
 * @see AdminShippingMethodService
 */
@Controller
@RequestMapping("/admin/shipping-methods")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminShippingMethodController {

    private final AdminShippingMethodService adminShippingMethodService;

    /**
     * 配送方法一覧画面を表示する。
     *
     * <p>{@code GET /admin/shipping-methods} — 全配送方法の一覧を取得し、管理画面を表示する。
     * 新規作成用の空フォームも同時にモデルに格納する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（{@code shippingMethods}, {@code shippingMethodRequest} を格納）
     * @return {@code "admin/shipping-methods/list"} 配送方法一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("shippingMethods", adminShippingMethodService.listAll());
        model.addAttribute("shippingMethodRequest",
                new AdminShippingMethodRequest(null, "", "", null, true, 0));
        return "admin/shipping-methods/list";
    }

    /**
     * 配送方法を新規作成する。
     *
     * <p>{@code POST /admin/shipping-methods} — バリデーション済みの配送方法情報を受け取り、
     * 新規配送方法を作成する。バリデーションエラー時は配送方法一覧画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param request 配送方法作成リクエスト（コード、名称、配送料、有効フラグ、表示順）
     * @param result バリデーション結果
     * @param model ビューに渡すモデル（バリデーションエラー時に配送方法一覧を再格納）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（作成完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/shipping-methods"}, バリデーションエラー時: {@code "admin/shipping-methods/list"}
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("shippingMethodRequest") AdminShippingMethodRequest request,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("shippingMethods", adminShippingMethodService.listAll());
            return "admin/shipping-methods/list";
        }
        adminShippingMethodService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.shippingMethod.created");
        return "redirect:/admin/shipping-methods";
    }

    /**
     * 配送方法編集画面を表示する。
     *
     * <p>{@code GET /admin/shipping-methods/{id}} — 指定された配送方法の情報を取得し、
     * 編集フォームを表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 編集対象の配送方法 ID（パスパラメータ、UUID 形式）
     * @param model ビューに渡すモデル（{@code shippingMethod}, {@code shippingMethodRequest} を格納）
     * @return {@code "admin/shipping-methods/form"} 配送方法編集画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String editForm(@PathVariable String id, Model model) {
        ShippingMethod method = adminShippingMethodService.findById(id);
        model.addAttribute("shippingMethod", method);
        model.addAttribute("shippingMethodRequest",
                new AdminShippingMethodRequest(method.getId(), method.getCode(), method.getName(),
                        method.getFee(), method.isActive(), method.getSortOrder()));
        return "admin/shipping-methods/form";
    }

    /**
     * 配送方法情報を更新する。
     *
     * <p>{@code PUT /admin/shipping-methods/{id}} — バリデーション済みの配送方法情報で既存の配送方法を更新する。
     * バリデーションエラー時は配送方法編集画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 更新対象の配送方法 ID（パスパラメータ、UUID 形式）
     * @param request 配送方法更新リクエスト（コード、名称、配送料、有効フラグ、表示順）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（更新完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/shipping-methods"}, バリデーションエラー時: {@code "admin/shipping-methods/form"}
     */
    @PutMapping("/{id}")
    public String update(@PathVariable String id,
                          @Valid @ModelAttribute("shippingMethodRequest") AdminShippingMethodRequest request,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/shipping-methods/form";
        }
        adminShippingMethodService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.shippingMethod.updated");
        return "redirect:/admin/shipping-methods";
    }

    /**
     * 配送方法を削除する。
     *
     * <p>{@code DELETE /admin/shipping-methods/{id}} — 指定された配送方法を削除する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 削除対象の配送方法 ID（パスパラメータ、UUID 形式）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（削除完了メッセージ）
     * @return {@code "redirect:/admin/shipping-methods"} 配送方法一覧へリダイレクト
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        adminShippingMethodService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "admin.shippingMethod.deleted");
        return "redirect:/admin/shipping-methods";
    }
}
