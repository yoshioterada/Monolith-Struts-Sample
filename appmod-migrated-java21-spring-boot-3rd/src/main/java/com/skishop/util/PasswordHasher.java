package com.skishop.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * SHA-256 + ソルトによるパスワードハッシュユーティリティ（レガシー互換）。
 *
 * <p>Struts 1.x 時代の {@code com.skishop.common.util.PasswordHasher} をそのまま
 * 移行したクラスである。ハッシュアルゴリズムは SHA-256 を 1000 回反復適用（ストレッチング）し、
 * ソルトを各反復でプレフィックスとして結合する。</p>
 *
 * <h3>ハッシュ計算の流れ</h3>
 * <ol>
 *   <li>ソルト（UTF-8 バイト列）とパスワード（UTF-8 バイト列）を結合する</li>
 *   <li>結合結果に SHA-256 ダイジェストを適用する</li>
 *   <li>ダイジェスト結果の先頭にソルトを再度結合し、手順 2 を繰り返す（合計 1000 回）</li>
 *   <li>最終的なバイト列を 16 進数文字列に変換して返す</li>
 * </ol>
 *
 * <p>本クラスは直接使用されず、{@link LegacySha256PasswordEncoder#matches(CharSequence, String)}
 * 経由でのみ呼び出される。新規パスワードのハッシュには BCrypt を使用するため、
 * 本クラスの {@link #hash(String, String)} を新規エンコードに使用してはならない。</p>
 *
 * <p>このクラスはインスタンス化できない（ユーティリティクラス）。</p>
 *
 * @see LegacySha256PasswordEncoder パスワード照合で本クラスを使用するエンコーダー
 */
public final class PasswordHasher {

    /** パスワードハッシュのストレッチング回数。セキュリティ強度を高めるための反復数。 */
    private static final int HASH_ITERATIONS = 1000;

    /**
     * ユーティリティクラスのため、インスタンス化を禁止する。
     */
    private PasswordHasher() {
    }

    /**
     * ランダムなソルト文字列を生成する。
     *
     * <p>UUID v4 からハイフンを除去した 32 文字の 16 進数文字列を返す。
     * 生成されたソルトは、パスワードハッシュとともに DB に保存される。</p>
     *
     * @return 32 文字のランダムなソルト文字列
     */
    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * パスワードとソルトから SHA-256 ハッシュを生成する。
     *
     * <p>ソルトとパスワードを結合し、SHA-256 ダイジェストを {@value #HASH_ITERATIONS} 回
     * 反復適用する。各反復ではダイジェスト結果の先頭にソルトを再度結合してから
     * 次のダイジェストを計算する。最終結果を 16 進数文字列として返す。</p>
     *
     * @param passwordRaw ハッシュ対象の生パスワード
     * @param salt        ソルト文字列
     * @return SHA-256 ハッシュの 16 進数文字列表現
     * @throws IllegalStateException SHA-256 アルゴリズムが利用できない場合、
     *                               または UTF-8 エンコーディングがサポートされていない場合
     */
    public static String hash(String passwordRaw, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            byte[] input = concat(saltBytes, passwordRaw.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                input = digest.digest(input);
                digest.reset();
                input = concat(saltBytes, input);
            }
            return toHex(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing unavailable", e);
        }
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private static String toHex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }
}
