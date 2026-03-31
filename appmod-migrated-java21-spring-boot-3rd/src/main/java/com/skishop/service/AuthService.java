package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.exception.BusinessException;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.SecurityLog;
import com.skishop.model.User;
import com.skishop.repository.SecurityLogRepository;
import com.skishop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 認証・セキュリティサービス。
 *
 * <p>ユーザー認証に関連するビジネスロジックを提供する。
 * ログイン成功・失敗の記録、アカウントロック制御、セキュリティログの管理を担当する。</p>
 *
 * <p>ログイン失敗が {@value #MAX_FAILED_ATTEMPTS} 回に達した場合、
 * 当該アカウントを自動的にロックし、不正アクセスを防止する。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link UserRepository} — ユーザーエンティティの参照・ステータス更新</li>
 *   <li>{@link SecurityLogRepository} — セキュリティイベントログの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.AuthController
 * @see CustomUserDetailsService
 * @see UserRepository
 * @see SecurityLogRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    private static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
    private static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String STATUS_LOCKED = AppConstants.STATUS_LOCKED;
    private static final String STATUS_ACTIVE = AppConstants.STATUS_ACTIVE;

    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;

    /**
     * メールアドレスでユーザーを検索する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param email 検索対象のメールアドレス
     * @return 該当するユーザーエンティティ
     * @throws ResourceNotFoundException 指定メールアドレスのユーザーが存在しない場合
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    /**
     * ログイン成功を記録する。
     *
     * <p>セキュリティログに {@code LOGIN_SUCCESS} イベントを保存する。
     * ログにはユーザー ID、IP アドレス、User-Agent が記録される。</p>
     *
     * @param userId    ログイン成功したユーザーの ID
     * @param ipAddress リクエスト元の IP アドレス
     * @param userAgent リクエスト元の User-Agent 文字列
     */
    @Transactional
    public void recordLoginSuccess(String userId, String ipAddress, String userAgent) {
        var logEntry = buildLog(userId, EVENT_LOGIN_SUCCESS, ipAddress, userAgent, null);
        securityLogRepository.save(logEntry);
    }

    /**
     * ログイン失敗を記録し、必要に応じてアカウントをロックする。
     *
     * <p>セキュリティログに {@code LOGIN_FAILURE} イベントを保存した後、
     * 累計失敗回数が {@value #MAX_FAILED_ATTEMPTS} 回以上に達した場合は
     * {@link #lockAccount(String, String, String)} を呼び出してアカウントをロックする。</p>
     *
     * @param userId    ログイン失敗したユーザーの ID
     * @param ipAddress リクエスト元の IP アドレス
     * @param userAgent リクエスト元の User-Agent 文字列
     */
    @Transactional
    public void recordLoginFailure(String userId, String ipAddress, String userAgent) {
        var logEntry = buildLog(userId, EVENT_LOGIN_FAILURE, ipAddress, userAgent, null);
        securityLogRepository.save(logEntry);

        long failCount = securityLogRepository.countByUserIdAndEventType(userId, EVENT_LOGIN_FAILURE);
        if (failCount >= MAX_FAILED_ATTEMPTS) {
            lockAccount(userId, ipAddress, userAgent);
        }
    }

    /**
     * 指定ユーザーのアカウントをロックする。
     *
     * <p>ユーザーのステータスを {@code LOCKED} に変更し、
     * セキュリティログに {@code ACCOUNT_LOCKED} イベントを記録する。
     * ロックされたアカウントはログイン不可となる。</p>
     *
     * @param userId    ロック対象のユーザー ID
     * @param ipAddress ロック契機となったリクエスト元の IP アドレス
     * @param userAgent ロック契機となったリクエスト元の User-Agent 文字列
     * @throws ResourceNotFoundException 指定 ID のユーザーが存在しない場合
     */
    @Transactional
    public void lockAccount(String userId, String ipAddress, String userAgent) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setStatus(STATUS_LOCKED);
        userRepository.save(user);

        var logEntry = buildLog(userId, EVENT_ACCOUNT_LOCKED, ipAddress, userAgent, null);
        securityLogRepository.save(logEntry);
        log.warn("Account locked for userId: {}", userId);
    }

    /**
     * 指定ユーザーのアカウントがロックされているか判定する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param userId 判定対象のユーザー ID
     * @return ロック済みの場合 {@code true}、未ロックまたはユーザーが存在しない場合 {@code false}
     */
    @Transactional(readOnly = true)
    public boolean isAccountLocked(String userId) {
        return userRepository.findById(userId)
                .map(user -> STATUS_LOCKED.equals(user.getStatus()))
                .orElse(false);
    }

    private SecurityLog buildLog(String userId, String eventType, String ipAddress,
                                  String userAgent, String details) {
        var logEntry = new SecurityLog();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setUserId(userId);
        logEntry.setEventType(eventType);
        logEntry.setIpAddress(ipAddress);
        logEntry.setUserAgent(userAgent);
        logEntry.setDetailsJson(details);
        return logEntry;
    }
}
