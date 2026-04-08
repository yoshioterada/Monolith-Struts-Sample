package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.dto.request.PaymentInfo;
import com.skishop.dto.response.PaymentResult;
import com.skishop.model.Payment;
import com.skishop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * 決済処理サービス。
 *
 * <p>クレジットカード決済のオーソリ（与信枠確保）、取消（void）、返金（refund）を担当する。
 * 決済結果は {@link Payment} エンティティとして DB に永続化される。</p>
 *
 * <p>カード番号のバリデーション:</p>
 * <ul>
 *   <li>桁数チェック（12〜19 桁）</li>
 *   <li>Luhn アルゴリズムによるチェックディジット検証</li>
 *   <li>有効期限の検証（現在月以降であること）</li>
 * </ul>
 *
 * <p>決済ステータスの遷移:</p>
 * <pre>
 * AUTHORIZED → VOID（キャンセル時）
 *            → REFUNDED（返品時）
 * FAILED（オーソリ失敗時）
 * </pre>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link PaymentRepository} — 決済レコードの永続化</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see CheckoutService#cancelOrder
 * @see PaymentRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 決済のオーソリ（与信枠確保）を実行する。
     *
     * <p>カード情報のバリデーション（Luhn アルゴリズム + 有効期限チェック）を行い、
     * 結果を {@link Payment} レコードとして DB に保存する。
     * 成功時は {@code AUTHORIZED}、失敗時は {@code FAILED} ステータスとなる。</p>
     *
     * @param info    支払い情報（カード番号、有効期限の年月）
     * @param amount  決済金額
     * @param cartId  カート ID（決済レコードの紐付け用）
     * @param orderId 注文 ID（決済レコードの紐付け用）
     * @return 決済結果（{@link PaymentResult.Success} または {@link PaymentResult.Failure}）
     */
    @Transactional
    public PaymentResult authorize(PaymentInfo info, BigDecimal amount, String cartId, String orderId) {
        boolean success = isPaymentAllowed(info);
        String status = success ? AppConstants.PAYMENT_STATUS_AUTHORIZED : AppConstants.PAYMENT_STATUS_FAILED;

        var payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setOrderId(orderId);
        payment.setCartId(cartId);
        payment.setAmount(amount);
        payment.setCurrency("JPY");
        payment.setStatus(status);
        payment.setPaymentIntentId("PAY-" + System.currentTimeMillis());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (success) {
            return new PaymentResult.Success(payment.getId(), status);
        }
        return new PaymentResult.Failure(status, "Payment declined");
    }

    /**
     * 決済を取消（void）する。
     *
     * <p>注文キャンセル時に呼び出され、オーソリ済みの決済を無効化する。
     * 当該注文に紐づく決済レコードのステータスを {@code VOID} に更新する。</p>
     *
     * @param orderId 取消対象の注文 ID
     * @see CheckoutService#cancelOrder
     */
    @Transactional
    public void voidPayment(String orderId) {
        updateStatusByOrderId(orderId, AppConstants.PAYMENT_STATUS_VOID);
    }

    /**
     * 決済を返金（refund）する。
     *
     * <p>返品処理時に呼び出され、決済済みの支払いを返金する。
     * 当該注文に紐づく決済レコードのステータスを {@code REFUNDED} に更新する。</p>
     *
     * @param orderId 返金対象の注文 ID
     * @see CheckoutService#returnOrder
     */
    @Transactional
    public void refundPayment(String orderId) {
        updateStatusByOrderId(orderId, AppConstants.PAYMENT_STATUS_REFUNDED);
    }

    private void updateStatusByOrderId(String orderId, String status) {
        paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .ifPresent(payment -> {
                    payment.setStatus(status);
                    paymentRepository.save(payment);
                });
    }

    private boolean isPaymentAllowed(PaymentInfo info) {
        if (info == null) {
            return false;
        }
        if (!isValidCardNumber(info.cardNumber())) {
            return false;
        }
        return isExpiryValid(info.cardExpYear(), info.cardExpMonth());
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String number = cardNumber.trim();
        if (number.length() < 12 || number.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char ch = number.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
            int digit = ch - '0';
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean isExpiryValid(String expYear, String expMonth) {
        if (expYear == null || expMonth == null) {
            return false;
        }
        int year;
        int month;
        try {
            year = Integer.parseInt(expYear);
            month = Integer.parseInt(expMonth);
        } catch (NumberFormatException e) {
            return false;
        }
        if (month < 1 || month > 12) {
            return false;
        }
        if (year < 100) {
            year += 2000;
        }
        var now = YearMonth.now();
        var expiry = YearMonth.of(year, month);
        return !expiry.isBefore(now);
    }
}
