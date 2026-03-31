# セキュアコーディングガイド（Java / Spring Boot）

> **バージョン**: 2.0  
> **更新日**: 2026-03-18  
> **技術スタック**: Java 25 / Spring Boot 4.1 / Spring AI 2.0

---

## 1. 入力検証

### 必須ルール
- **全ての外部入力**（`@RequestBody`, `@RequestParam`, `@PathVariable`, `@RequestHeader`）に Bean Validation を適用する
- **ホワイトリスト方式**のバリデーションを優先する（ブラックリスト禁止）
- カスタムバリデーターは `ConstraintValidator` を実装する

### バリデーションアノテーションの使用

| アノテーション | 用途 | 例 |
|---|---|---|
| `@NotNull` | null 禁止 | 必須フィールド |
| `@NotBlank` | null, 空文字, 空白のみ禁止 | 文字列の必須フィールド |
| `@Size(min, max)` | 文字列長・コレクションサイズの制限 | `@Size(min = 1, max = 50)` |
| `@Pattern` | 正規表現パターン | `@Pattern(regexp = "^[a-zA-Z0-9_-]+$")` |
| `@Email` | メールアドレス形式 | メールフィールド |
| `@Min`, `@Max` | 数値の範囲 | `@Min(0)`, `@Max(1000000)` |
| `@Positive` | 正の数値のみ | ID, 数量 |

### サイズ制限（DoS 防止）
- リクエストボディサイズ: `server.tomcat.max-http-form-post-size=2MB`
- ファイルアップロード: `spring.servlet.multipart.max-file-size=10MB`
- コレクションパラメータ: `@Size(max = 100)` で上限設定

```java
// ✅ 良い例: 完全なバリデーション付き DTO
public record CreateUserRequest(
    @NotBlank(message = "名前は必須です")
    @Size(min = 1, max = 50, message = "名前は1〜50文字で入力してください")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s-]+$", message = "使用できない文字が含まれています")
    String name,

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 255)
    String email
) {}
```

---

## 2. 認証・認可

### 認証
- Spring Security を使用し、**エンドポイントごとにアクセス制御**を設定する
- JWT トークンの検証には**有効期限・署名の両方**をチェックする
- `alg: none` 攻撃への耐性を確保する
- セッション使用時: 認証後に**セッション ID を再生成**する（セッション固定攻撃対策）

### 認可
- `@PreAuthorize` でメソッドレベルの認可を実装する
- **IDOR 防止**: パスパラメータの ID に対するオーナーシップチェックを実装する

```java
// ✅ 良い例: IDOR 防止のオーナーシップチェック
@GetMapping("/users/{id}/profile")
@PreAuthorize("@authService.isOwner(#id, authentication) or hasRole('ADMIN')")
public ResponseEntity<UserProfile> getProfile(@PathVariable Long id) { ... }
```

### パスワード管理

| ハッシュ方式 | 判定 |
|---|---|
| bcrypt / scrypt / Argon2 | ✅ 推奨 |
| PBKDF2（十分なイテレーション） | ⚠️ 許容 |
| SHA-256 + ソルト | ❌ 不十分 |
| SHA-1 / MD5 | ❌ 使用禁止 |
| 平文保存 | ❌ **Critical 違反** |

---

## 3. 暗号化

### 推奨 / 禁止アルゴリズム

| 用途 | 推奨 | 禁止 |
|------|------|------|
| 対称暗号 | AES-256-GCM | DES, 3DES, RC4, AES-ECB |
| ハッシュ | SHA-256 以上 | MD5, SHA-1 |
| パスワードハッシュ | bcrypt, Argon2 | SHA 系単独 |
| 公開鍵暗号 | RSA-2048 以上, ECDSA | RSA-1024 |
| 乱数生成 | `SecureRandom` | `Random`, `Math.random()` |

### 鍵管理
- 暗号鍵をソースコードにハードコードしない
- 鍵のローテーション計画を策定する
- 環境変数または外部シークレット管理サービス（Vault 等）で管理する

---

## 4. 秘密情報管理

### ハードコード禁止パターン

| 検出パターン | 重要度 |
|---|---|
| `password = "..."`, `passwd = "..."` | Critical |
| `apiKey = "..."`, `api_key = "..."` | Critical |
| `token = "..."`, `secret = "..."` | Critical |
| `jdbc:...password=...` | Critical |
| `-----BEGIN PRIVATE KEY-----` | Critical |

### 安全な管理方法
- `application.properties` には `${ENV_VAR}` 形式で環境変数を参照する
- テスト用の秘密情報は `application-test.properties` にのみ記載し、本番値と異なるダミー値を使用する
- `.gitignore` に秘密情報を含む可能性のあるファイルを追加する

### ログへの秘密情報出力禁止
- パスワード、トークン、API キー、クレジットカード番号をログに出力しない
- リクエスト全体をログに記録する場合、秘密情報フィールドをマスキングする

---

## 5. エラーハンドリング

### 内部情報の漏洩防止

| 漏洩してはならない情報 | 例 |
|---------------------|-----|
| スタックトレース | `java.lang.NullPointerException at ...` |
| SQL エラー | `ORA-00942: table or view does not exist` |
| 内部パス | `/opt/app/src/main/java/...` |
| クラス名 / メソッド名 | `UserServiceImpl.findById()` |
| DB カラム名 | `column 'user_password' cannot be null` |

### 安全なエラーハンドリング
- `@ControllerAdvice` でグローバルな例外ハンドリングを実装する
- エラーレスポンスは **RFC 7807 Problem Details** 形式を使用する
- クライアントには汎用的なエラーメッセージを返し、詳細はサーバーログに記録する

```java
// ✅ 良い例: 安全なエラーハンドリング
@ExceptionHandler(Exception.class)
public ProblemDetail handleError(Exception e) {
    log.error("予期しないエラー", e);  // ログには詳細を記録
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "サーバーでエラーが発生しました");
}
```

### 本番環境の設定
```properties
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-exception=false
```

---

## 6. ログ出力

### SLF4J の使用（System.out.println 禁止）
- `System.out.println` / `System.err.println` は **Critical 違反**
- SLF4J (`@Slf4j`) を使用する
- パラメータはプレースホルダー `{}` を使用する（文字列結合禁止）

### セキュリティイベントのログ記録

| イベント | ログレベル | 記録内容 |
|---------|----------|---------|
| 認証成功 | INFO | ユーザー ID、IP アドレス |
| 認証失敗 | WARN | 試行ユーザー名（**パスワード記録禁止**）、IP |
| 認可失敗 | WARN | ユーザー ID、アクセス先、必要な権限 |
| 入力バリデーション失敗 | WARN | リクエスト元 IP（**入力値記録禁止**） |
| データ削除 / 重要な変更 | INFO | 操作者、対象リソース |

### ログインジェクション対策
- ユーザー入力をログに出力する際は改行文字（`\n`, `\r`）をサニタイズする

---

## 7. Spring AI 2.0 セキュリティ

### プロンプトインジェクション対策
- ユーザー入力とシステムプロンプトを**明確に分離**する
- ユーザー入力をプロンプトに含める前に**サニタイズ**する

### 個人情報の保護
- AI サービスに送信するデータに個人情報が含まれていないか確認する
- 含まれる場合、送信前に**マスキング / 除去**を実施する

### API キー管理
- AI サービスの API キーを環境変数で管理する
- ログやエラーレスポンスに API キーが出力されないようにする

---

## 8. 禁止事項チェックリスト

| # | 禁止事項 | OWASP | 重要度 |
|---|---------|-------|--------|
| 1 | 文字列結合による SQL 構築 | A03 | Critical |
| 2 | 秘密情報のハードコード | A02 | Critical |
| 3 | `@Valid` なしの外部入力受付 | A03 | High |
| 4 | 認可アノテーションなしのエンドポイント | A01 | High |
| 5 | スタックトレースのクライアント返却 | A05 | High |
| 6 | パスワードの平文保存 | A02 | Critical |
| 7 | `Random` / `Math.random()` のセキュリティ用途使用 | A02 | Medium |
| 8 | ログへの秘密情報出力 | A09 | High |
| 9 | Actuator の全エンドポイント公開 | A05 | High |
| 10 | CORS の `*` ワイルドカード | A05 | Medium |
