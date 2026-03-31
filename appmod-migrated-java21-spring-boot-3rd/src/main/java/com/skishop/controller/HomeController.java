package com.skishop.controller;

import com.skishop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * トップページの HTTP リクエストを処理するコントローラー。
 *
 * <p>SkiShop のホームページを表示し、注目商品（アクティブな商品）を一覧表示する。
 * ベース URL: {@code /}</p>
 *
 * <p>Struts 移行元: {@code IndexAction}</p>
 *
 * <p>認可: 全ユーザー（未認証含む）がアクセス可能（{@code permitAll}）。</p>
 *
 * @see ProductService
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final ProductService productService;

    /**
     * トップページ（ホーム画面）を表示する。
     *
     * <p>{@code GET /} — アクティブな商品を「注目商品」として取得し、ホーム画面を表示する。</p>
     *
     * <p>認可: 全ユーザーがアクセス可能</p>
     *
     * @param model ビューに渡すモデル（{@code featuredProducts} を格納）
     * @return {@code "home"} ホーム画面のテンプレート名
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findByStatus("ACTIVE"));
        return "home";
    }
}
