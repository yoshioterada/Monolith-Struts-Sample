---
applyTo:
  - "**/service/**/*.java"
  - "**/controller/**/*.java"
---

# セキュリティコーディング規約

本 Instructions は外部入力を受けるレイヤー（`**/controller/**/*.java`, `**/service/**/*.java`）に自動適用される。
controller / service クラスの作成・編集時に以下のセキュリティ規約を遵守すること。

> **重要**: セキュリティコーディング規約の違反は、他の全てのコーディング規約違反より優先的に是正する。

---

## 1. 入力検証（OWASP A03: インジェクション防止の基盤）

### バリデーション必須化
- **全ての外部入力**（リクエストボディ、パスパラメータ、クエリパラメータ、ヘッダー）にバリデーションを実施する
- `@Valid` / `@Validated` を Controller メソッドの引数に付与する
- Bean Validation アノテーション（`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, `@Email`）を DTO に付与する

```java
// ✅ 良い例: 入力検証付きの Controller
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.created(URI.create("/api/v1/users/" + userService.create(request).id()))
                         .body(userService.create(request));
}

// ✅ 良い例: Bean Validation 付きリクエスト DTO
public record CreateUserRequest(
    @NotBlank(message = "名前は必須です")
    @Size(min = 1, max = 50, message = "名前は1〜50文字で入力してください")
    String name,

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 255)
    String email
) {}
```

### パスパラメータ・クエリパラメータの検証
- `@PathVariable` の値に型安全性と範囲チェックを実施する
- `@RequestParam` にデフォルト値と上限を設定する

```java
// ✅ 良い例: パスパラメータの検証
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(
        @PathVariable @Positive(message = "IDは正の整数を指定してください") Long id) {
    return ResponseEntity.ok(userService.findById(id));
}
```

### ホワイトリスト検証
- **ブラックリスト（禁止文字列の除外）ではなくホワイトリスト（許可パターンの限定）**で検証する

```java
// ❌ 悪い例: ブラックリスト（回避可能）
@Pattern(regexp = "^(?!.*<script>).*$")
String comment;

// ✅ 良い例: ホワイトリスト（許可パターンのみ）
@Pattern(regexp = "^[a-zA-Z0-9_-]{3,30}$", message = "英数字・ハイフン・アンダースコアのみ使用可能です")
String username;
```

### サイズ制限（DoS 防止）
- 全ての入力に**サイズ上限**を設定する。無制限の入力は DoS 攻撃のベクトルとなる
- リクエストボディサイズ: `server.tomcat.max-http-form-post-size` で制限
- ファイルアップロード: `spring.servlet.multipart.max-file-size` / `max-request-size` で制限
- コレクションパラメータ: `@Size(max = ...)` で上限を設定

### カスタムバリデーター
- 複雑なビジネスバリデーションは `ConstraintValidator` インターフェースを実装する

---

## 2. SQL インジェクション防止（OWASP A03）

### 絶対禁止事項
- **文字列結合による SQL 構築は絶対に禁止**。違反は Critical 指摘

```java
// ❌ Critical 違反: 文字列結合による SQL
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
jdbcTemplate.query(sql, ...);

// ❌ Critical 違反: String.format による SQL
String sql = String.format("SELECT * FROM users WHERE name = '%s'", name);
```

### 安全な方法

| 方法 | 安全性 | 例 |
|------|--------|-----|
| Spring Data JPA メソッド名クエリ | ✅ 安全 | `findByName(String name)` |
| `@Query` + パラメータバインド | ✅ 安全 | `@Query("... WHERE u.name = :name")` |
| `JdbcTemplate` + `?` プレースホルダー | ✅ 安全 | `jdbcTemplate.query("... WHERE name = ?", name)` |
| `Criteria API` | ✅ 安全 | `cb.equal(root.get("name"), name)` |
| 文字列結合 | ❌ **絶対禁止** | `"... WHERE name = '" + name + "'"` |

```java
// ✅ 良い例: パラメータバインド
@Query("SELECT u FROM User u WHERE u.name = :name AND u.status = :status")
List<User> findByNameAndStatus(@Param("name") String name, @Param("status") String status);

// ✅ 良い例: JdbcTemplate
jdbcTemplate.query(
    "SELECT * FROM users WHERE name = ? AND status = ?",
    new Object[]{name, status},
    userRowMapper
);
```

### ネイティブクエリ
- ネイティブクエリを使用する場合も**必ずパラメータバインドを使用**する
- ネイティブクエリの使用は最小限にする（ポータビリティの観点）

```java
// ✅ 良い例: ネイティブクエリでもパラメータバインド
@Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
List<User> findByNameNative(@Param("name") String name);
```

---

## 3. XSS 防止（OWASP A03）

- ユーザー入力をレスポンスに含める場合は**エスケープ処理**を行う
- JSON レスポンスでは Jackson のデフォルトエスケープに依存可能（ただし HTML コンテキストでは不十分）
- **HTML を返す場合**は Thymeleaf のデフォルトエスケープ機能（`th:text`）を利用する。`th:utext`（エスケープなし出力）は原則禁止
- ユーザー入力をそのまま HTML 属性、JavaScript コンテキスト、URL パラメータに埋め込まない

```java
// ❌ 悪い例: ユーザー入力をそのまま HTML に含める
return "<div>" + userInput + "</div>";

// ✅ 良い例: レスポンスは JSON で返し、エスケープは Jackson に委ねる
return ResponseEntity.ok(new UserResponse(user.getName()));
```

### HTTP セキュリティヘッダー
- 以下のセキュリティヘッダーを Spring Security で設定する

| ヘッダー | 設定値 | 目的 |
|---------|--------|------|
| `Content-Security-Policy` | `default-src 'self'` | XSS・データインジェクション防止 |
| `X-Content-Type-Options` | `nosniff` | MIME スニッフィング防止 |
| `X-Frame-Options` | `DENY` | クリックジャッキング防止 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 強制 |

---

## 4. 認証・認可チェック（OWASP A01 / A07）

### エンドポイントの認可必須化
- **全エンドポイント**に認可アノテーション（`@PreAuthorize`, `@Secured`, `@RolesAllowed`）を設定する
- 認可チェックなしのエンドポイントは意図的であることを**コメントで明記**する

```java
// ✅ 良い例: 認可アノテーション付き
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}

// ✅ 良い例: 意図的な公開エンドポイントの明記
@GetMapping("/health")
// 認可不要: ヘルスチェック用の公開エンドポイント
public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
}
```

### オブジェクトレベルの認可（IDOR 防止）
- パスパラメータの ID でリソースにアクセスする際、**そのリソースがリクエスト者に属するかを検証**する
- 他ユーザーのリソースに ID 推測でアクセスできてはならない

```java
// ❌ 悪い例: IDOR 脆弱性（誰でも任意のユーザーデータにアクセス可能）
@GetMapping("/users/{id}/orders")
public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long id) {
    return ResponseEntity.ok(orderService.findByUserId(id));
}

// ✅ 良い例: オーナーシップチェック
@GetMapping("/users/{id}/orders")
@PreAuthorize("@authService.isOwner(#id, authentication)")
public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long id) {
    return ResponseEntity.ok(orderService.findByUserId(id));
}
```

### ロールベースアクセス制御

| 操作 | 必要なロール | アノテーション例 |
|------|------------|----------------|
| ユーザー情報参照 | 全認証ユーザー | `@PreAuthorize("isAuthenticated()")` |
| ユーザー情報更新 | 本人またはADMIN | `@PreAuthorize("@authService.isOwner(#id, authentication) or hasRole('ADMIN')")` |
| ユーザー削除 | ADMIN のみ | `@PreAuthorize("hasRole('ADMIN')")` |
| 管理機能 | ADMIN のみ | `@PreAuthorize("hasRole('ADMIN')")` |

---

## 5. 秘密情報管理（OWASP A02 / A05）

### ハードコード禁止
- **API キー、パスワード、トークン、接続文字列、秘密鍵をソースコードにハードコードすることは絶対禁止**
- 違反は Critical 指摘

```java
// ❌ Critical 違反: 秘密情報のハードコード
private static final String API_KEY = "sk-1234567890abcdef";
private static final String DB_PASSWORD = "mysecretpassword";

// ✅ 良い例: 設定から注入
@Value("${external.api.key}")
private String apiKey;
```

### 環境変数による外部化
- `application.properties` には `${ENV_VAR}` 形式で環境変数を参照する
- 本番用の秘密情報をリポジトリにコミットしない

```properties
# ✅ 良い例: 環境変数参照
spring.datasource.password=${DB_PASSWORD}
external.api.key=${API_KEY}

# ❌ 悪い例: 秘密情報を直接記述
spring.datasource.password=mysecretpassword
```

### テスト環境の秘密情報
- テスト用の秘密情報は `application-test.properties` にのみ記載する
- テスト用の値は**本番値とは異なるダミー値**を使用する
- テスト用の `.properties` ファイルに本番の秘密情報を記載しない

### ログへの秘密情報出力禁止
- ログに秘密情報（パスワード、トークン、API キー）を出力しない
- リクエスト/レスポンスをログに記録する場合、**秘密情報をマスキング**する

---

## 6. エラーレスポンスのセキュリティ（OWASP A05）

### 内部情報の漏洩防止
- エラーレスポンスに以下の内部情報を含めない

| 漏洩してはならない情報 | 例 | リスク |
|---------------------|-----|------|
| スタックトレース | `java.lang.NullPointerException at ...` | 内部構造の露呈 |
| SQL エラー | `ORA-00942: table or view does not exist` | DB 構造の露呈 |
| 内部パス | `/opt/app/src/main/java/...` | サーバー構成の露呈 |
| クラス名 / メソッド名 | `UserServiceImpl.findById()` | 内部実装の露呈 |
| DB カラム名 | `column 'user_password' cannot be null` | DB スキーマの露呈 |

```java
// ❌ 悪い例: スタックトレースの露出
@ExceptionHandler(Exception.class)
public ResponseEntity<String> handleError(Exception e) {
    return ResponseEntity.status(500).body(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
}

// ✅ 良い例: 安全なエラーレスポンス
@ExceptionHandler(Exception.class)
public ProblemDetail handleError(Exception e) {
    log.error("予期しないエラー", e);  // ログには詳細を記録
    var problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "サーバーでエラーが発生しました");
    problem.setType(URI.create("https://example.com/errors/internal-error"));
    return problem;  // クライアントには最小限の情報のみ
}
```

### Spring Boot の設定
```properties
# ✅ 本番環境の設定
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-exception=false
```

---

## 7. SSRF 防止（OWASP A10）

- ユーザー入力の URL に対してサーバーサイドから HTTP リクエストを送信するパターンを検出する
- URL のホワイトリスト検証を必須化する

```java
// ❌ 悪い例: ユーザー入力の URL にそのままリクエスト
@GetMapping("/proxy")
public ResponseEntity<String> proxy(@RequestParam String url) {
    return restTemplate.getForEntity(url, String.class);  // SSRF 脆弱性
}

// ✅ 良い例: 許可ドメインのホワイトリスト検証
@GetMapping("/proxy")
public ResponseEntity<String> proxy(@RequestParam String url) {
    if (!urlValidator.isAllowedDomain(url)) {
        throw new SecurityException("許可されていないドメインです");
    }
    return restTemplate.getForEntity(url, String.class);
}
```

- **内部ネットワーク（`localhost`, `127.0.0.1`, `10.x.x.x`, `192.168.x.x`）への アクセスを禁止**する

---

## 8. セキュリティログ（OWASP A09）

### 記録すべきセキュリティイベント

| イベント | ログレベル | 記録内容 |
|---------|----------|---------|
| 認証成功 | INFO | ユーザー ID、IP アドレス |
| 認証失敗 | WARN | 試行ユーザー名（**パスワードは絶対に記録しない**）、IP、失敗理由 |
| 認可失敗（権限不足） | WARN | ユーザー ID、アクセス先リソース、必要な権限 |
| 入力バリデーション失敗 | WARN | リクエスト元 IP、失敗フィールド（**入力値自体は記録しない**） |
| データの削除 / 重要な更新 | INFO | 操作者、対象リソース |

### ログに含めてはならない情報
- パスワード、トークン、API キー、クレジットカード番号
- 完全な個人情報（マスキング必須: `user@example.com` → `u***@example.com`）
- セッション ID（セッションハイジャックのリスク）

---

## 9. 禁止事項チェックリスト

| # | 禁止事項 | OWASP | 重要度 | 検出パターン |
|---|---------|-------|--------|------------|
| 1 | 文字列結合による SQL 構築 | A03 | Critical | `"SELECT ... WHERE " + param`, `String.format("SELECT ...")` |
| 2 | 秘密情報のハードコード | A02 | Critical | `password = "..."`, `apiKey = "..."`, `token = "..."` |
| 3 | `@Valid` / `@Validated` なしの外部入力受付 | A03 | High | `@RequestBody` に `@Valid` がない |
| 4 | 認可アノテーションなしのエンドポイント（意図的明記なし） | A01 | High | `@GetMapping` / `@PostMapping` 等に `@PreAuthorize` 等がない |
| 5 | スタックトレースのクライアント返却 | A05 | High | `e.getStackTrace()` をレスポンスに含める |
| 6 | ユーザー入力 URL への無検証サーバーサイドリクエスト | A10 | High | `restTemplate.getForEntity(userInput, ...)` |
| 7 | `th:utext` の使用（エスケープなし HTML 出力） | A03 | High | Thymeleaf テンプレート内の `th:utext` |
| 8 | ログへの秘密情報・個人情報出力 | A09 | High | `log.info("password: {}", password)` |
| 9 | `Math.random()` / `Random` のセキュリティ用途使用 | A02 | Medium | トークン・ID 生成に `Random` を使用（`SecureRandom` を使用すべき） |
| 10 | CORS の `*` ワイルドカード設定 | A05 | Medium | `@CrossOrigin("*")`, `allowedOrigins("*")` |
