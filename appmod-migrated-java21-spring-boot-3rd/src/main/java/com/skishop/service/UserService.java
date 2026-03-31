package com.skishop.service;

import com.skishop.constant.AppConstants;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.PasswordResetToken;
import com.skishop.model.User;
import com.skishop.repository.PasswordResetTokenRepository;
import com.skishop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザー管理サービス。
 *
 * <p>ユーザーの登録、参照、パスワード更新、パスワードリセット機能を提供する。
 * 移行元の Struts アプリケーションから Spring Boot への移行に伴い、
 * ユーザー管理ロジックを Controller から分離した Service 層。</p>
 *
 * <h3>パスワードリセットフロー</h3>
 * <ol>
 *   <li>{@link #createPasswordResetToken} でリセットトークンを生成（有効期限: 24 時間）</li>
 *   <li>トークンをメールで送信（{@link MailService#enqueuePasswordReset}）</li>
 *   <li>{@link #validateResetToken} でトークンの有効性を検証</li>
 *   <li>{@link #updatePassword} でパスワードを更新</li>
 *   <li>{@link #consumeResetToken} でトークンを使用済みにする</li>
 * </ol>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link UserRepository} — ユーザーエンティティの永続化</li>
 *   <li>{@link PasswordResetTokenRepository} — パスワードリセットトークンの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.AuthController
 * @see com.skishop.controller.AccountController
 * @see AuthService
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ユーザー ID でユーザーを取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param userId ユーザー ID
     * @return 該当するユーザーエンティティ
     * @throws ResourceNotFoundException 指定 ID のユーザーが存在しない場合
     */
    @Transactional(readOnly = true)
    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * メールアドレスでユーザーを取得する。
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
     * 新規ユーザーを登録する。
     *
     * <p>ID、ステータス、ロールが未設定の場合はデフォルト値を自動設定する:</p>
     * <ul>
     *   <li>ID: UUID を自動生成</li>
     *   <li>ステータス: {@code ACTIVE}</li>
     *   <li>ロール: {@code USER}</li>
     * </ul>
     *
     * @param user 登録するユーザーエンティティ（パスワードハッシュは事前に設定済みであること）
     * @return 保存後のユーザーエンティティ
     * @see com.skishop.controller.AuthController
     */
    @Transactional
    public User register(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        if (user.getStatus() == null) {
            user.setStatus(AppConstants.STATUS_ACTIVE);
        }
        if (user.getRole() == null) {
            user.setRole(AppConstants.ROLE_USER);
        }
        return userRepository.save(user);
    }

    /**
     * メールアドレス・ユーザー名・パスワードから新規ユーザーを作成・登録する。
     *
     * <p>パスワードは BCrypt でエンコードしてから保存する。
     * ステータスは {@code ACTIVE}、ロールは {@code USER} に設定される。</p>
     *
     * @param email       メールアドレス
     * @param username    ユーザー名
     * @param rawPassword 生パスワード（BCrypt エンコード前）
     * @return 保存後のユーザーエンティティ
     */
    @Transactional
    public User registerNewUser(String email, String username, String rawPassword) {
        var user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setSalt("");
        user.setStatus(AppConstants.STATUS_ACTIVE);
        user.setRole(AppConstants.ROLE_USER);
        return userRepository.save(user);
    }

    /**
     * ユーザーのパスワードハッシュを更新する。
     *
     * <p>パスワードリセットフローの最終ステップとして呼び出される。</p>
     *
     * @param userId       更新対象のユーザー ID
     * @param passwordHash 新しいパスワードハッシュ（BCrypt エンコード済み）
     * @param salt         ソルト（BCrypt の場合は {@code null} 可）
     * @throws ResourceNotFoundException 指定 ID のユーザーが存在しない場合
     */
    @Transactional
    public void updatePassword(String userId, String passwordHash, String salt) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        userRepository.save(user);
    }

    /**
     * 生パスワードを BCrypt エンコードしてユーザーのパスワードを更新する。
     *
     * @param userId      更新対象のユーザー ID
     * @param rawPassword 生パスワード
     * @throws ResourceNotFoundException 指定 ID のユーザーが存在しない場合
     */
    @Transactional
    public void updatePassword(String userId, String rawPassword) {
        updatePassword(userId, passwordEncoder.encode(rawPassword), "");
    }

    /**
     * パスワードリセットトークンを生成する。
     *
     * <p>指定メールアドレスのユーザーに対してリセットトークンを生成し、
     * DB に保存する。トークンの有効期限は生成から 24 時間。
     * 生成されたトークンは {@link MailService#enqueuePasswordReset} で
     * ユーザーにメール送信される。</p>
     *
     * @param email リセット対象ユーザーのメールアドレス
     * @return 生成されたリセットトークン文字列
     * @throws ResourceNotFoundException 指定メールアドレスのユーザーが存在しない場合
     * @see MailService#enqueuePasswordReset
     */
    @Transactional
    public String createPasswordResetToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        var token = new PasswordResetToken();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(user.getId());
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        passwordResetTokenRepository.save(token);
        return token.getToken();
    }

    /**
     * パスワードリセットトークンの有効性を検証する。
     *
     * <p>以下の条件を全て満たすトークンのみ有効と判定する:</p>
     * <ul>
     *   <li>トークン値が DB に存在する</li>
     *   <li>未使用（{@code usedAt} が {@code null}）</li>
     *   <li>有効期限（{@code expiresAt}）が現在時刻より後</li>
     * </ul>
     *
     * @param tokenValue 検証対象のトークン文字列
     * @return 有効なパスワードリセットトークンエンティティ
     * @throws ResourceNotFoundException トークンが無効（存在しない・使用済み・期限切れ）の場合
     */
    @Transactional
    public PasswordResetToken validateResetToken(String tokenValue) {
        return passwordResetTokenRepository.findByToken(tokenValue)
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException("PasswordResetToken", tokenValue));
    }

    /**
     * パスワードリセットトークンを使用済みにする。
     *
     * <p>トークンの有効性を検証した後、使用日時（{@code usedAt}）を現在時刻に設定する。
     * これにより同一トークンの再利用を防止する。</p>
     *
     * @param tokenValue 使用済みにするトークン文字列
     * @throws ResourceNotFoundException トークンが無効（存在しない・使用済み・期限切れ）の場合
     */
    @Transactional
    public void consumeResetToken(String tokenValue) {
        var token = validateResetToken(tokenValue);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }
}
