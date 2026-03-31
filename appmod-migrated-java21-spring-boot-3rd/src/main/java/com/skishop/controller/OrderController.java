package com.skishop.controller;

import com.skishop.model.Order;
import com.skishop.model.OrderItem;
import com.skishop.service.CheckoutService;
import com.skishop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

/**
 * 注文関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>ログイン済みユーザーの注文一覧表示、注文詳細表示、注文キャンセル、返品依頼を行う。
 * IDOR（不正な直接オブジェクト参照）防止のため、注文の参照・操作はログイン中のユーザーに
 * 紐づく注文のみに制限される。ベース URL: {@code /orders}</p>
 *
 * <p>Struts 移行元: {@code OrderAction}, {@code OrderListAction}, {@code OrderDetailAction},
 * {@code OrderCancelAction}</p>
 *
 * <p>認可: 全エンドポイントは認証済みユーザー（{@code USER} または {@code ADMIN}）のみアクセス可能。</p>
 *
 * @see OrderService
 * @see CheckoutService
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    /**
     * 注文一覧画面を表示する。
     *
     * <p>{@code GET /orders} — ログイン中のユーザーに紐づく全注文を取得し、注文一覧画面を表示する。</p>
     *
     * <p>認可: 認証済みユーザーのみ（自分の注文のみ表示）</p>
     *
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param model ビューに渡すモデル（{@code orders} を格納）
     * @return {@code "orders/list"} 注文一覧画面のテンプレート名
     */
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Order> orders = orderService.listByUserId(userDetails.getUsername());
        model.addAttribute("orders", orders);
        return "orders/list";
    }

    /**
     * 注文詳細画面を表示する。
     *
     * <p>{@code GET /orders/{id}} — 指定された注文 ID の注文詳細および注文明細を取得し、
     * 詳細画面を表示する。IDOR 防止のため、ログイン中ユーザーの注文のみアクセス可能。</p>
     *
     * <p>認可: 認証済みユーザーのみ（自分の注文のみ参照可能）</p>
     *
     * @param id 注文 ID（パスパラメータ、UUID 形式）
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param model ビューに渡すモデル（{@code order}, {@code items} を格納）
     * @return {@code "orders/detail"} 注文詳細画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        Order order = orderService.findByIdAndUserId(id, userDetails.getUsername());
        List<OrderItem> items = orderService.listItems(id);
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        return "orders/detail";
    }

    /**
     * 注文をキャンセルする。
     *
     * <p>{@code POST /orders/{id}/cancel} — 指定された注文をキャンセルする。
     * 在庫の復元、ポイントの返還等のキャンセル処理は {@link CheckoutService} に委任される。</p>
     *
     * <p>認可: 認証済みユーザーのみ（自分の注文のみキャンセル可能）</p>
     *
     * @param id キャンセル対象の注文 ID（パスパラメータ、UUID 形式）
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param redirectAttributes リダイレクト時のフラッシュ属性（キャンセル完了メッセージ）
     * @return {@code "redirect:/orders/{id}"} 注文詳細画面へリダイレクト
     */
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        checkoutService.cancelOrder(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "order.cancelled");
        return "redirect:/orders/" + id;
    }

    /**
     * 注文の返品を依頼する。
     *
     * <p>{@code POST /orders/{id}/return} — 指定された注文の返品処理を行う。
     * 返品に伴う在庫復元やポイント調整は {@link CheckoutService} に委任される。</p>
     *
     * <p>認可: 認証済みユーザーのみ（自分の注文のみ返品可能）</p>
     *
     * @param id 返品対象の注文 ID（パスパラメータ、UUID 形式）
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param redirectAttributes リダイレクト時のフラッシュ属性（返品完了メッセージ）
     * @return {@code "redirect:/orders/{id}"} 注文詳細画面へリダイレクト
     */
    @PostMapping("/{id}/return")
    public String returnOrder(@PathVariable String id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        checkoutService.returnOrder(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "order.returned");
        return "redirect:/orders/" + id;
    }
}
