package com.skishop.controller.admin;

import com.skishop.model.Order;
import com.skishop.model.OrderItem;
import com.skishop.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

/**
 * 管理者向け注文管理の HTTP リクエストを処理するコントローラー。
 *
 * <p>全ユーザーの注文一覧表示、注文詳細表示、注文ステータス更新を行う。
 * ベース URL: {@code /admin/orders}</p>
 *
 * <p>Struts 移行元: {@code AdminOrderAction}, {@code AdminOrderDetailAction}</p>
 *
 * <p>認可: 全エンドポイントは {@code ADMIN} ロールのみアクセス可能
 * （クラスレベル {@code @PreAuthorize("hasRole('ADMIN')")}）。</p>
 *
 * @see OrderService
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 全注文の一覧画面を表示する。
     *
     * <p>{@code GET /admin/orders} — 全ユーザーの注文を最大 200 件取得し、管理用注文一覧画面を表示する。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param model ビューに渡すモデル（{@code orders} を格納）
     * @return {@code "admin/orders/list"} 管理用注文一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        List<Order> orders = orderService.listAll(200);
        model.addAttribute("orders", orders);
        return "admin/orders/list";
    }

    /**
     * 注文詳細画面を表示する（管理者用）。
     *
     * <p>{@code GET /admin/orders/{id}} — 指定された注文 ID の注文詳細および注文明細を取得し、
     * 管理用注文詳細画面を表示する。ユーザーに関係なく全注文にアクセス可能。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 注文 ID（パスパラメータ、UUID 形式）
     * @param model ビューに渡すモデル（{@code order}, {@code items} を格納）
     * @return {@code "admin/orders/detail"} 管理用注文詳細画面のテンプレート名
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Order order = orderService.findById(id);
        List<OrderItem> items = orderService.listItems(id);
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        return "admin/orders/detail";
    }

    /**
     * 注文ステータスを更新する。
     *
     * <p>{@code PUT /admin/orders/{id}/status} — 指定された注文のステータスを変更する
     * （例: PENDING → PROCESSING → SHIPPED → DELIVERED）。</p>
     *
     * <p>認可: {@code ADMIN} ロールのみ</p>
     *
     * @param id 更新対象の注文 ID（パスパラメータ、UUID 形式）
     * @param status 新しいステータス値（クエリパラメータ）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（更新完了メッセージ）
     * @return {@code "redirect:/admin/orders/{id}"} 管理用注文詳細画面へリダイレクト
     */
    @PutMapping("/{id}/status")
    public String updateStatus(@PathVariable String id,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        orderService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "admin.order.statusUpdated");
        return "redirect:/admin/orders/" + id;
    }
}
