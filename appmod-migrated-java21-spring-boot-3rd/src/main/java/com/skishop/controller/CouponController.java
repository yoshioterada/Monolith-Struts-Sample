package com.skishop.controller;

import com.skishop.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ユーザー向けクーポン一覧 Controller。
 *
 * <p>利用可能なクーポン（{@code active=true}）を一覧表示する。
 * Struts 版の {@code CouponAvailableAction} に相当する。</p>
 */
@Controller
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 利用可能なクーポン一覧を表示する。
     *
     * @param model Spring MVC モデル
     * @return coupons/available テンプレート
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("coupons", couponService.listActiveCoupons());
        return "coupons/available";
    }
}
