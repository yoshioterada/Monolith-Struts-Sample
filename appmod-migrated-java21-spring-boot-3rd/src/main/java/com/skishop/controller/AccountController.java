package com.skishop.controller;

import com.skishop.dto.request.AddressRequest;
import com.skishop.model.Address;
import com.skishop.model.PointAccount;
import com.skishop.service.AddressService;
import com.skishop.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.UUID;

/**
 * アカウント関連の HTTP リクエストを処理するコントローラー。
 *
 * <p>ログイン済みユーザーのポイント残高照会および配送先住所の管理（一覧表示・追加・削除）を行う。
 * ベース URL: {@code /account}</p>
 *
 * <p>Struts 移行元: {@code AccountAction}</p>
 *
 * <p>認可: 全エンドポイントは認証済みユーザー（{@code USER} または {@code ADMIN}）のみアクセス可能。</p>
 *
 * @see AddressService
 * @see PointService
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AddressService addressService;
    private final PointService pointService;

    /**
     * ポイント残高照会画面を表示する。
     *
     * <p>{@code GET /account/points} — ログイン中のユーザーのポイントアカウント情報を取得し、
     * ポイント残高画面を表示する。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param model ビューに渡すモデル（{@code pointAccount} を格納）
     * @return {@code "account/points"} ポイント残高画面のテンプレート名
     */
    @GetMapping("/points")
    public String points(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        PointAccount account = pointService.getAccount(userDetails.getUsername());
        model.addAttribute("pointAccount", account);
        return "account/points";
    }

    /**
     * 配送先住所一覧画面を表示する。
     *
     * <p>{@code GET /account/addresses} — ログイン中のユーザーに紐づく全配送先住所を取得し、
     * 住所一覧画面を表示する。新規追加用の空フォームも同時にモデルに格納する。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param model ビューに渡すモデル（{@code addresses}, {@code addressRequest} を格納）
     * @return {@code "account/addresses"} 住所一覧画面のテンプレート名
     */
    @GetMapping("/addresses")
    public String addresses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Address> addresses = addressService.findByUserId(userDetails.getUsername());
        model.addAttribute("addresses", addresses);
        model.addAttribute("addressRequest", new AddressRequest(
                null, "", "", "", "", "", "", "", false));
        return "account/addresses";
    }

    /**
     * 配送先住所を新規追加する。
     *
     * <p>{@code POST /account/addresses} — バリデーション済みの住所情報を受け取り、
     * 新規配送先住所を保存する。バリデーションエラー時は住所一覧画面を再表示する。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param request 住所リクエスト（ラベル、受取人名、郵便番号、都道府県、住所等）
     * @param result バリデーション結果
     * @param userDetails Spring Security が注入する認証済みユーザー情報
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功メッセージ）
     * @return 成功時: {@code "redirect:/account/addresses"}, バリデーションエラー時: {@code "account/addresses"}
     */
    @PostMapping("/addresses")
    public String addAddress(@Valid @ModelAttribute AddressRequest request,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "account/addresses";
        }
        Address address = new Address();
        address.setId(request.id() != null ? request.id() : UUID.randomUUID().toString());
        address.setUserId(userDetails.getUsername());
        address.setLabel(request.label());
        address.setRecipientName(request.recipientName());
        address.setPostalCode(request.postalCode());
        address.setPrefecture(request.prefecture());
        address.setAddress1(request.address1());
        address.setAddress2(request.address2());
        address.setPhone(request.phone());
        address.setDefault(request.isDefault());
        addressService.save(address);
        redirectAttributes.addFlashAttribute("successMessage", "address.saved");
        return "redirect:/account/addresses";
    }

    /**
     * 配送先住所を削除する。
     *
     * <p>{@code DELETE /account/addresses/{id}} — 指定された ID の配送先住所を削除する。</p>
     *
     * <p>認可: 認証済みユーザーのみ</p>
     *
     * @param id 削除対象の住所 ID（UUID 形式）
     * @param redirectAttributes リダイレクト時のフラッシュ属性（成功メッセージ）
     * @return {@code "redirect:/account/addresses"} 住所一覧へリダイレクト
     */
    @DeleteMapping("/addresses/{id}")
    public String deleteAddress(@PathVariable String id,
                                 RedirectAttributes redirectAttributes) {
        addressService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "address.deleted");
        return "redirect:/account/addresses";
    }
}
