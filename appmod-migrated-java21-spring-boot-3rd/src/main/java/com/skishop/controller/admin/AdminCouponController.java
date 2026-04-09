package com.skishop.controller.admin;

import com.skishop.dto.request.admin.AdminCouponRequest;
import com.skishop.model.Coupon;
import com.skishop.service.CouponService;
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
 * 管理者向けクーポン管理の HTTP リクエストを処理するコントローラー。
 *
 * <p>クーポンの一覧表示、新規作成、編集画面表示、更新、削除を行う。
 * ベース URL: {@code /admin/coupons}</p>
 *
 * <p>Struts 移行元: {@code AdminCouponAction}, {@code AdminCouponEditAction}</p>
 *
 * <p>認可: 全エンドポイントは {@code ADMIN} ロールのみアクセス可能
 * （クラスレベル {@code @PreAuthorize("hasRole('ADMIN')")}）。</p>
 *
 * @see CouponService
 */
@Controller
@RequestMapping("/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * クーポン一覧画面を表示する。
     *
     * <p>{@code GET /admin/coupons} — 全クーポンの一覧を取得し、管理画面を表示する。
     * 新規作成用の空フォームも同時にモデルに格納する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（{@code coupons}, {@code couponRequest} を格納）
     * @return {@code "admin/coupons/list"} クーポン一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("coupons", couponService.listAll());
        model.addAttribute("couponRequest",
                new AdminCouponRequest(null, null, "", "", null, "", null, null, 0, true, null));
        return "admin/coupons/list";
    }

    /**
     * クーポン新規作成画面を表示する。
     *
     * <p>{@code GET /admin/coupons/new} — 空のクーポン作成フォームを表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（空の {@code couponRequest} を格納）
     * @return {@code "admin/coupons/form"} クーポン作成・編集画面のテンプレート名
     */
    @GetMapping("/new")
    public String newCouponForm(Model model) {
        model.addAttribute("couponRequest",
                new AdminCouponRequest(null, null, "", "", null, "", null, null, 0, true, null));
        return "admin/coupons/form";
    }

    /**
     * クーポンを新規作成する。
     *
     * <p>{@code POST /admin/coupons} — バリデーション済みのクーポン情報を受け取り、新規クーポンを作成する。
     * バリデーションエラー時はクーポン一覧画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param request クーポン作成リクエスト（コード、種別、割引値、割引種別、最低金額、有効期限等）
     * @param result バリデーション結果
     * @param model ビューに渡すモデル（バリデーションエラー時にクーポン一覧を再格納）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（作成完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/coupons"}, バリデーションエラー時: {@code "admin/coupons/list"}
     */
    @PostMapping
    public String createCoupon(@Valid @ModelAttribute("couponRequest") AdminCouponRequest request,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("coupons", couponService.listAll());
            return "admin/coupons/list";
        }
        couponService.createCoupon(request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.coupon.created");
        return "redirect:/admin/coupons";
    }

    /**
     * クーポン編集画面を表示する。
     *
     * <p>{@code GET /admin/coupons/{id}} — 指定されたクーポンの情報を取得し、編集フォームを表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 編集対象のクーポン ID（パスパラメータ、UUID 形式）
     * @param model ビューに渡すモデル（{@code coupon}, {@code couponRequest} を格納）
     * @return {@code "admin/coupons/form"} クーポン編集画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String editCouponForm(@PathVariable String id, Model model) {
        Coupon coupon = couponService.findById(id);
        model.addAttribute("coupon", coupon);
        model.addAttribute("couponRequest",
                new AdminCouponRequest(coupon.getId(), coupon.getCampaignId(), coupon.getCode(),
                        coupon.getCouponType(), coupon.getDiscountValue(), coupon.getDiscountType(),
                        coupon.getMinimumAmount(), coupon.getMaximumDiscount(), coupon.getUsageLimit(),
                        coupon.isActive(), coupon.getExpiresAt()));
        return "admin/coupons/form";
    }

    /**
     * クーポン情報を更新する。
     *
     * <p>{@code PUT /admin/coupons/{id}} — バリデーション済みのクーポン情報で既存クーポンを更新する。
     * バリデーションエラー時はクーポン編集画面を再表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 更新対象のクーポン ID（パスパラメータ、UUID 形式）
     * @param request クーポン更新リクエスト（コード、種別、割引値、割引種別、最低金額、有効期限等）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（更新完了メッセージ）
     * @return 成功時: {@code "redirect:/admin/coupons"}, バリデーションエラー時: {@code "admin/coupons/form"}
     */
    @PutMapping("/{id}")
    public String updateCoupon(@PathVariable String id,
                                @Valid @ModelAttribute("couponRequest") AdminCouponRequest request,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/coupons/form";
        }
        couponService.updateCoupon(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "admin.coupon.updated");
        return "redirect:/admin/coupons";
    }

    /**
     * クーポンを削除する。
     *
     * <p>{@code DELETE /admin/coupons/{id}} — 指定されたクーポンを削除する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 削除対象のクーポン ID（パスパラメータ、UUID 形式）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（削除完了メッセージ）
     * @return {@code "redirect:/admin/coupons"} クーポン一覧へリダイレクト
     */
    @DeleteMapping("/{id}")
    public String deleteCoupon(@PathVariable String id, RedirectAttributes redirectAttributes) {
        couponService.deleteCoupon(id);
        redirectAttributes.addFlashAttribute("successMessage", "admin.coupon.deleted");
        return "redirect:/admin/coupons";
    }
}
