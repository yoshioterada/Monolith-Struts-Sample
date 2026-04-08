package com.skishop.controller;

import com.skishop.dto.request.CartItemRequest;
import com.skishop.dto.request.CouponApplyRequest;
import com.skishop.model.Cart;
import com.skishop.model.CartItem;
import com.skishop.model.Coupon;
import com.skishop.service.CartService;
import com.skishop.service.CouponService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.skishop.security.SkiShopUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.math.BigDecimal;
import java.util.List;

/**
 * ショッピングカート関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>カートの表示、商品の追加・数量変更・削除、およびクーポン適用を行う。
 * 未ログインユーザーはセッションベースのカート、ログイン済みユーザーはユーザー紐づけカートを使用する。
 * ベース URL: {@code /cart}</p>
 *
 * <p>Struts 移行元: {@code CartAction}, {@code CartAddAction}, {@code CartUpdateAction},
 * {@code CartDeleteAction}, {@code CouponApplyAction}</p>
 *
 * <p>認可: カート表示・商品追加は未認証ユーザーでもアクセス可能。
 * クーポン適用は認証済みユーザー推奨。</p>
 *
 * @see CartService
 * @see CouponService
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final CouponService couponService;

    /**
     * カート内容を表示する。
     *
     * <p>{@code GET /cart} — 現在のカートに含まれる商品一覧と小計金額を取得し、
     * カート画面を表示する。ログイン状態に応じてユーザーカートまたはセッションカートを参照する。</p>
     *
     * <p>認可: 未認証ユーザーを含む全ユーザーがアクセス可能</p>
     *
     * @param userDetails Spring Security が注入する認証済みユーザー情報（未ログイン時は {@code null}）
     * @param session HTTP セッション（セッションベースのカート ID 管理に使用）
     * @param model ビューに渡すモデル（{@code cart}, {@code items}, {@code subtotal} 等を格納）
     * @return {@code "cart/view"} カート画面のテンプレート名
     */
    @GetMapping
    public String view(@AuthenticationPrincipal SkiShopUserDetails userDetails,
                       HttpSession session,
                       Model model) {
        Cart cart = resolveCart(userDetails, session);
        List<CartItem> items = cartService.getItems(cart.getId());
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartItemRequest", new CartItemRequest("", 1));
        return "cart/view";
    }

    /**
     * カートに商品を追加する。
     *
     * <p>{@code POST /cart/items} — 指定された商品 ID と数量でカートに商品を追加する。
     * バリデーションエラー時はエラーメッセージと共にカート画面にリダイレクトする。</p>
     *
     * <p>認可: 未認証ユーザーを含む全ユーザーがアクセス可能</p>
     *
     * @param request カート商品リクエスト（商品 ID、数量）
     * @param result バリデーション結果
     * @param userDetails Spring Security が注入する認証済みユーザー情報（未ログイン時は {@code null}）
     * @param session HTTP セッション（カート ID の管理に使用）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功/エラーメッセージ）
     * @return {@code "redirect:/cart"} カート画面へリダイレクト
     */
    @PostMapping("/items")
    public String addItem(@Valid @ModelAttribute CartItemRequest request,
                          BindingResult result,
                          @AuthenticationPrincipal SkiShopUserDetails userDetails,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "cart.item.invalid");
            return "redirect:/cart";
        }
        Cart cart = resolveCart(userDetails, session);
        cartService.addItem(cart.getId(), request.productId(), request.quantity());
        redirectAttributes.addFlashAttribute("successMessage", "cart.item.added");
        return "redirect:/cart";
    }

    /**
     * カート内商品の数量を変更する。
     *
     * <p>{@code PUT /cart/items/{itemId}} — 指定されたカートアイテムの数量を更新する。
     * バリデーションエラー時はエラーメッセージと共にカート画面にリダイレクトする。</p>
     *
     * <p>認可: 未認証ユーザーを含む全ユーザーがアクセス可能</p>
     *
     * @param itemId 更新対象のカートアイテム ID（パスパラメータ）
     * @param request カート商品リクエスト（新しい数量）
     * @param result バリデーション結果
     * @param redirectAttributes リダイレクト時のフラッシュ属性（エラーメッセージ）
     * @return {@code "redirect:/cart"} カート画面へリダイレクト
     */
    @PutMapping("/items/{itemId}")
    public String updateItem(@PathVariable String itemId,
                              @Valid @ModelAttribute CartItemRequest request,
                              BindingResult result,
                              @AuthenticationPrincipal SkiShopUserDetails userDetails,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "cart.item.invalid");
            return "redirect:/cart";
        }
        Cart cart = resolveCart(userDetails, session);
        cartService.updateItemQuantity(itemId, request.quantity(), cart.getId());
        return "redirect:/cart";
    }

    /**
     * カートから商品を削除する。
     *
     * <p>{@code DELETE /cart/items/{itemId}} — 指定されたカートアイテムを削除する。</p>
     *
     * <p>認可: 未認証ユーザーを含む全ユーザーがアクセス可能</p>
     *
     * @param itemId 削除対象のカートアイテム ID（パスパラメータ）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功メッセージ）
     * @return {@code "redirect:/cart"} カート画面へリダイレクト
     */
    @DeleteMapping("/items/{itemId}")
    public String removeItem(@PathVariable String itemId,
                              @AuthenticationPrincipal SkiShopUserDetails userDetails,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Cart cart = resolveCart(userDetails, session);
        cartService.removeItem(itemId, cart.getId());
        redirectAttributes.addFlashAttribute("successMessage", "cart.item.removed");
        return "redirect:/cart";
    }

    /**
     * クーポンをカートに適用する。
     *
     * <p>{@code POST /cart/coupon} — 入力されたクーポンコードを検証し、カートの小計に対する
     * 割引額を計算してセッションに保存する。無効なクーポンコードの場合はエラーメッセージを返す。</p>
     *
     * <p>認可: 未認証ユーザーを含む全ユーザーがアクセス可能</p>
     *
     * @param request クーポン適用リクエスト（クーポンコード）
     * @param result バリデーション結果
     * @param userDetails Spring Security が注入する認証済みユーザー情報（未ログイン時は {@code null}）
     * @param session HTTP セッション（クーポンコードと割引額を保存）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功/エラーメッセージ）
     * @return {@code "redirect:/cart"} カート画面へリダイレクト
     */
    @PostMapping("/coupon")
    public String applyCoupon(@Valid @ModelAttribute CouponApplyRequest request,
                               BindingResult result,
                               @AuthenticationPrincipal SkiShopUserDetails userDetails,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "coupon.code.invalid");
            return "redirect:/cart";
        }
        Cart cart = resolveCart(userDetails, session);
        List<CartItem> items = cartService.getItems(cart.getId());
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        Coupon coupon = couponService.validateCoupon(request.code(), subtotal).orElse(null);
        BigDecimal discount = couponService.calculateDiscount(coupon, subtotal);
        session.setAttribute("couponCode", request.code());
        session.setAttribute("couponDiscount", discount);
        redirectAttributes.addFlashAttribute("successMessage", "coupon.applied");
        return "redirect:/cart";
    }

    private Cart resolveCart(SkiShopUserDetails userDetails, HttpSession session) {
        String userId = userDetails != null ? userDetails.getUserId() : null;
        String cartId = (String) session.getAttribute("cartId");
        Cart cart = cartService.resolveCart(userId, session.getId(), cartId);
        if (userDetails == null && cartId == null) {
            session.setAttribute("cartId", cart.getId());
        }
        return cart;
    }
}
