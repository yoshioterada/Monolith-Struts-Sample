package com.skishop.util;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * レガシー SHA-256 + ソルト方式のパスワードエンコーダー（移行互換性用）。
 *
 * <p>Struts 1.x 時代に {@link PasswordHasher} で生成されたパスワードハッシュを
 * Spring Security の認証フローで検証するためのアダプタークラスである。
 * {@link org.springframework.security.crypto.password.DelegatingPasswordEncoder} に
 * {@code "sha256"} キーで登録され、DB 内の {@code {sha256}<hash>$<salt>} 形式の
 * パスワードハッシュを照合する。</p>
 *
 * <h3>パスワード形式</h3>
 * <p>DB に格納されるパスワードハッシュの形式は以下の通り:</p>
 * <ul>
 *   <li><strong>Flyway V2 適用後（レガシー）</strong>: {@code {sha256}<hex-hash>$<salt>}</li>
 *   <li><strong>BCrypt アップグレード後</strong>: {@code {bcrypt}<bcrypt-hash>}</li>
 * </ul>
 *
 * <p>{@code DelegatingPasswordEncoder} がプレフィックス {@code {sha256}} を解析し、
 * 本エンコーダーの {@link #matches(CharSequence, String)} に {@code <hex-hash>$<salt>}
 * 部分（プレフィックス除去済み）を渡す。</p>
 *
 * <h3>パスワード照合の流れ</h3>
 * <ol>
 *   <li>{@code DelegatingPasswordEncoder} が {@code {sha256}} プレフィックスを検出</li>
 *   <li>本エンコーダーの {@link #matches} が呼び出される</li>
 *   <li>格納値を {@code $} で分割し、ハッシュ部分とソルト部分を取得</li>
 *   <li>{@link PasswordHasher#hash(String, String)} で入力パスワードをハッシュ化</li>
 *   <li>生成されたハッシュと格納ハッシュを比較</li>
 * </ol>
 *
 * <p>照合成功後、{@code CustomUserDetailsService#updatePassword()} により
 * パスワードは BCrypt 形式に自動アップグレードされる。</p>
 *
 * <p>新規パスワードのエンコードには対応しない（{@link #encode} は
 * {@link UnsupportedOperationException} をスローする）。新規パスワードは
 * {@code DelegatingPasswordEncoder} のデフォルトである BCrypt でエンコードされる。</p>
 *
 * @see PasswordHasher SHA-256 ハッシュの実装
 * @see SecurityConfig#passwordEncoder() DelegatingPasswordEncoder の構成
 */
public class LegacySha256PasswordEncoder implements PasswordEncoder {

    /**
     * 新規パスワードのエンコードを行う（未サポート）。
     *
     * <p>本エンコーダーはレガシーパスワードの照合専用であり、新規パスワードの
     * エンコードには対応しない。新規パスワードは {@code DelegatingPasswordEncoder} の
     * デフォルトエンコーダー（BCrypt）によってエンコードされる。</p>
     *
     * @param rawPassword エンコード対象の生パスワード
     * @return （このメソッドは値を返さない）
     * @throws UnsupportedOperationException 常にスローされる
     */
    @Override
    public String encode(CharSequence rawPassword) {
        // New registrations use BCrypt via DelegatingPasswordEncoder.
        // This encoder is only used for matching legacy passwords.
        throw new UnsupportedOperationException(
                "LegacySha256PasswordEncoder does not support encoding. " +
                "New passwords must be encoded with BCrypt.");
    }

    /**
     * 入力パスワードがレガシー形式の格納ハッシュと一致するか検証する。
     *
     * <p>格納値のフォーマットは {@code <hex-hash>$<salt>} である
     * （{@code {sha256}} プレフィックスは {@code DelegatingPasswordEncoder} により除去済み）。
     * {@code $} を区切り文字としてハッシュ部分とソルト部分に分割し、
     * {@link PasswordHasher#hash(String, String)} で入力パスワードをハッシュ化して比較する。</p>
     *
     * @param rawPassword        ユーザーが入力した生パスワード
     * @param storedHashWithSalt 格納されたハッシュ値（{@code <hex-hash>$<salt>} 形式）
     * @return パスワードが一致する場合は {@code true}、一致しない場合またはフォーマット不正の場合は {@code false}
     */
    @Override
    public boolean matches(CharSequence rawPassword, String storedHashWithSalt) {
        // storedHashWithSalt format: "<hash>$<salt>"
        // The {sha256} prefix has already been stripped by DelegatingPasswordEncoder.
        if (storedHashWithSalt == null || rawPassword == null) {
            return false;
        }
        int sep = storedHashWithSalt.lastIndexOf('$');
        if (sep < 0) {
            return false;
        }
        String hash = storedHashWithSalt.substring(0, sep);
        String salt = storedHashWithSalt.substring(sep + 1);
        return PasswordHasher.hash(rawPassword.toString(), salt).equals(hash);
    }
}
