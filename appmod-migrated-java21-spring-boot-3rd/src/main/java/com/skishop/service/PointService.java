package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.exception.BusinessException;
import com.skishop.model.PointAccount;
import com.skishop.model.PointTransaction;
import com.skishop.repository.PointAccountRepository;
import com.skishop.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ポイント管理サービス。
 *
 * <p>SkiShop のポイントプログラムを管理する。ユーザーごとのポイント残高を
 * {@link PointAccount} で管理し、ポイントの付与・使用・返還・取消の
 * 各トランザクションを {@link PointTransaction} に記録する。</p>
 *
 * <h3>ポイント付与ルール</h3>
 * <ul>
 *   <li>注文合計金額の 1%（端数切り捨て）</li>
 *   <li>有効期限: 付与から 365 日</li>
 * </ul>
 *
 * <h3>トランザクションタイプ</h3>
 * <ul>
 *   <li>{@code EARN} — 注文確定時のポイント付与</li>
 *   <li>{@code REDEEM} — チェックアウト時のポイント使用</li>
 *   <li>{@code REFUND} — キャンセル時の使用ポイント返還</li>
 *   <li>{@code REVOKE} — キャンセル時の付与ポイント取消</li>
 * </ul>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link PointAccountRepository} — ポイント口座の永続化</li>
 *   <li>{@link PointTransactionRepository} — ポイント取引履歴の永続化</li>
 * </ul>
 *
 * @see CheckoutService#placeOrder
 * @see CheckoutService#cancelOrder
 * @see com.skishop.controller.AccountController
 * @see PointAccountRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 注文確定時にポイントを付与する。
     *
     * <p>注文合計金額の 1%（端数切り捨て）をポイントとして付与し、
     * ポイント口座の残高と累計獲得ポイントを加算する。
     * 付与されたポイントの有効期限は 365 日間。</p>
     *
     * <p>ユーザー ID または合計金額が {@code null} の場合、
     * または計算結果が 0 以下の場合はポイント付与を行わない。</p>
     *
     * {@code 例: totalAmount=15000 → 付与ポイント=150}
     *
     * @param userId      ポイント付与対象のユーザー ID
     * @param referenceId 紐付け元の注文 ID
     * @param totalAmount 注文合計金額（ポイント計算の基準額）
     * @return 付与されたポイント数（付与しなかった場合は 0）
     * @see CheckoutService#placeOrder
     */
    @Transactional
    public int awardPoints(String userId, String referenceId, BigDecimal totalAmount) {
        if (userId == null || totalAmount == null) {
            return 0;
        }
        int points = calculateAwardPoints(totalAmount);
        if (points <= 0) {
            return 0;
        }
        var account = ensureAccount(userId);
        account.setBalance(account.getBalance() + points);
        account.setLifetimeEarned(account.getLifetimeEarned() + points);
        pointAccountRepository.saveAndFlush(account);

        var transaction = buildTransaction(userId, AppConstants.POINT_TX_EARN, points, referenceId, "Order points");
        transaction.setExpiresAt(LocalDateTime.now().plusDays(365));
        pointTransactionRepository.save(transaction);
        return points;
    }

    /**
     * チェックアウト時にポイントを使用する。
     *
     * <p>指定ポイント数をユーザーの口座残高から減算する。
     * 残高不足の場合は {@link BusinessException} をスローする。
     * ポイント使用前に期限切れポイントの自動失効処理を実行する。</p>
     *
     * @param userId      ポイント使用者のユーザー ID
     * @param points      使用するポイント数（1 以上）
     * @param referenceId 紐付け元の注文 ID
     * @throws BusinessException ポイント残高が不足している場合
     * @see CheckoutService#placeOrder
     */
    @Transactional
    public void redeemPoints(String userId, int points, String referenceId) {
        if (points <= 0) {
            return;
        }
        expirePoints(userId);
        var account = ensureAccount(userId);
        if (account.getBalance() < points) {
            throw new BusinessException("Insufficient points",
                    "redirect:/checkout", "error.points.insufficient");
        }
        account.setBalance(account.getBalance() - points);
        account.setLifetimeRedeemed(account.getLifetimeRedeemed() + points);
        pointAccountRepository.saveAndFlush(account);

        var transaction = buildTransaction(userId, AppConstants.POINT_TX_REDEEM, -points, referenceId, "Redeem points");
        pointTransactionRepository.save(transaction);
    }

    /**
     * 注文キャンセル時に使用されたポイントを返還する。
     *
     * <p>キャンセルされた注文で使用されていたポイントをユーザーの口座残高に加算する。
     * ポイント数が 0 以下の場合は何もしない。</p>
     *
     * @param userId      ポイント返還先のユーザー ID
     * @param points      返還するポイント数
     * @param referenceId 紐付け元の注文 ID
     * @see CheckoutService#cancelOrder
     */
    @Transactional
    public void refundPoints(String userId, int points, String referenceId) {
        if (points <= 0) {
            return;
        }
        var account = ensureAccount(userId);
        account.setBalance(account.getBalance() + points);
        pointAccountRepository.save(account);

        var transaction = buildTransaction(userId, AppConstants.POINT_TX_REFUND, points, referenceId, "Refund points");
        pointTransactionRepository.save(transaction);
    }

    /**
     * 注文キャンセル時に付与済みのポイントを取消する。
     *
     * <p>キャンセルされた注文で付与されていたポイントをユーザーの口座残高から減算する。
     * 残高が 0 未満にならないよう保護される。ポイント数が 0 以下の場合は何もしない。</p>
     *
     * @param userId      ポイント取消対象のユーザー ID
     * @param points      取消するポイント数
     * @param referenceId 紐付け元の注文 ID
     * @see CheckoutService#cancelOrder
     */
    @Transactional
    public void revokePoints(String userId, int points, String referenceId) {
        if (points <= 0) {
            return;
        }
        var account = ensureAccount(userId);
        account.setBalance(Math.max(account.getBalance() - points, 0));
        pointAccountRepository.save(account);

        var transaction = buildTransaction(userId, AppConstants.POINT_TX_REVOKE, -points, referenceId, "Revoke points");
        pointTransactionRepository.save(transaction);
    }

    /**
     * 注文合計金額からポイント付与数を計算する。
     *
     * <p>合計金額の 1% を端数切り捨てで計算する。</p>
     *
     * {@code 例: totalAmount=12345 → 戻り値=123}
     *
     * @param totalAmount 注文合計金額
     * @return 付与ポイント数（{@code null} の場合は 0）
     */
    public int calculateAwardPoints(BigDecimal totalAmount) {
        if (totalAmount == null) {
            return 0;
        }
        return totalAmount.multiply(new BigDecimal("0.01"))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    /**
     * ユーザーのポイント口座情報を取得する。
     *
     * <p>取得前に期限切れポイントの自動失効処理を実行する。
     * ポイント口座が存在しない場合は残高 0 の新規口座を自動作成する。</p>
     *
     * @param userId ユーザー ID
     * @return ポイント口座エンティティ（残高・累計獲得・累計使用ポイント）
     * @see com.skishop.controller.AccountController
     */
    @Transactional
    public PointAccount getAccount(String userId) {
        expirePoints(userId);
        return ensureAccount(userId);
    }

    /**
     * 指定ユーザーの期限切れポイントを失効処理する。
     *
     * <p>有効期限（{@code expiresAt}）を過ぎたポイントトランザクションを
     * 期限切れ（{@code expired = true}）としてマークし、
     * 口座残高から失効分を減算する。</p>
     *
     * @param userId 失効処理対象のユーザー ID（{@code null} の場合は何もしない）
     */
    @Transactional
    public void expirePoints(String userId) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int expiredAmount = pointTransactionRepository.sumExpiredAmount(userId, now);
        pointTransactionRepository.bulkExpire(userId, now);
        if (expiredAmount > 0) {
            pointAccountRepository.findByUserId(userId).ifPresent(account -> {
                account.setBalance(Math.max(account.getBalance() - expiredAmount, 0));
                pointAccountRepository.save(account);
            });
        }
    }

    private PointAccount ensureAccount(String userId) {
        return pointAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    var account = new PointAccount();
                    account.setId(UUID.randomUUID().toString());
                    account.setUserId(userId);
                    account.setBalance(0);
                    account.setLifetimeEarned(0);
                    account.setLifetimeRedeemed(0);
                    return pointAccountRepository.save(account);
                });
    }

    private PointTransaction buildTransaction(String userId, String type, int amount,
                                               String referenceId, String description) {
        var transaction = new PointTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);
        transaction.setExpired(false);
        return transaction;
    }
}
