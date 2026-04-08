package com.skishop.controller;

import com.skishop.dto.request.CheckoutRequest;
import com.skishop.dto.request.PaymentInfo;
import com.skishop.dto.request.PlaceOrderCommand;
import com.skishop.dto.response.CheckoutSummary;
import com.skishop.model.Cart;
import com.skishop.model.Order;
import com.skishop.service.CartService;
import com.skishop.service.CheckoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.skishop.security.SkiShopUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

/**
 * チェックアウト（注文確定）関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>注文確認画面の表示および注文確定処理を行う。カート内商品の小計、送料、消費税、
 * クーポン割引を計算し、支払い情報と共に注文を確定する。
 * ベース URL: {@code /checkout}</p>
 *
 * <p>Struts 移行元: {@code CheckoutAction}, {@code CheckoutConfirmAction}</p>
 *
 * <p>認可: 全エンドポイントは認証済みユーザー（{@code USER} または {@code ADMIN}）のみアクセス可能。</p>
 *
 * @see CheckoutService
 * @see CartService
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    /**
     * チェックアウト画面（注文確認画面）を表示する。
     *
     * <p>{@code GET /checkout} — カート内の商品一覧、小計、送料、消費税、クーポン割引を
     * 計算し、注文確認フォームを表示する。セッションに保存されたクーポン情報も反映する。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param session HTTP セッション（クーポンコード・割引額の取得に使用）
     * @param model ビューに渡すモデル（{@code cart}, {@code items}, {@code subtotal},
     *              {@code shippingFee}, {@code tax}, {@code couponDiscount} 等を格納）
     * @return {@code "checkout/index"} チェックアウト画面のテンプレート名
     */
    @GetMapping
    public String checkoutForm(@AuthenticationPrincipal SkiShopUserDetails userDetails,
                                HttpSession session,
                                Model model) {
        CheckoutSummary summary = checkoutService.prepareCheckoutSummary(
                userDetails.getUserId(), session.getId());
        String couponCode = (String) session.getAttribute("couponCode");
        BigDecimal couponDiscount = (BigDecimal) session.getAttribute("couponDiscount");
        if (couponDiscount == null) {
            couponDiscount = BigDecimal.ZERO;
        }
        model.addAttribute("cart", summary.cart());
        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shippingFee", summary.shippingFee());
        model.addAttribute("tax", summary.tax());
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("couponDiscount", couponDiscount);
        model.addAttribute("checkoutRequest", new CheckoutRequest(
                summary.cart().getId(), couponCode, "", "", "", "", "", "", 0));
        return "checkout/index";
    }

    /**
     * 注文を確定する。
     *
     * <p>{@code POST /checkout} — チェックアウトフォームの送信を処理し、注文を確定する。
     * カート内商品の在庫確認、クーポン適用、ポイント消費、支払い処理、在庫減算、
     * ポイント付与、カートクリア、受注確認メール送信を単一トランザクションで実行する。
     * 注文確定後はセッションからクーポン情報を削除し、注文詳細画面にリダイレクトする。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param request チェックアウトリクエスト（カート ID、クーポンコード、支払い方法、カード情報、使用ポイント等）
     * @param result バリデーション結果
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param session HTTP セッション（クーポン情報のクリアに使用）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（注文完了メッセージ）
     * @return 成功時: {@code "redirect:/orders/{orderId}"}, バリデーションエラー時: {@code "redirect:/checkout"}
     */
    @PostMapping
    public String placeOrder(@Valid @ModelAttribute CheckoutRequest request,
                              BindingResult result,
                              @AuthenticationPrincipal SkiShopUserDetails userDetails,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Re-populate checkout form data for re-rendering
            CheckoutSummary summary = checkoutService.prepareCheckoutSummary(
                    userDetails.getUserId(), session.getId());
            model.addAttribute("cart", summary.cart());
            model.addAttribute("items", summary.items());
            model.addAttribute("subtotal", summary.subtotal());
            model.addAttribute("shippingFee", summary.shippingFee());
            model.addAttribute("tax", summary.tax());
            model.addAttribute("couponCode", request.couponCode());
            model.addAttribute("couponDiscount", session.getAttribute("couponDiscount"));
            return "checkout/index";
        }
        PaymentInfo paymentInfo = new PaymentInfo(
                request.paymentMethod(),
                request.cardNumber(),
                request.cardExpMonth(),
                request.cardExpYear(),
                request.cardCvv(),
                request.billingZip()
        );
        Cart cart = cartService.getOrCreateCart(userDetails.getUserId(), session.getId());
        var command = new PlaceOrderCommand(
                cart.getId(), request.couponCode(), request.usePoints(),
                paymentInfo, userDetails.getUserId());
        Order order = checkoutService.placeOrder(command);
        session.removeAttribute("couponCode");
        session.removeAttribute("couponDiscount");
        redirectAttributes.addFlashAttribute("successMessage", "checkout.orderPlaced");
        return "redirect:/orders/" + order.getId();
    }
}
