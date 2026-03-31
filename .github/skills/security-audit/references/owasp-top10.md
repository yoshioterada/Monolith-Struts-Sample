# OWASP Top 10 チェックリスト（Java / Spring Boot 向け）

> **バージョン**: 2.0  
> **更新日**: 2026-03-18  
> **参照**: OWASP Top 10 (2021)  
> **技術スタック**: Java 25 / Spring Boot 4.1 / Spring AI 2.0

---

## A01: アクセス制御の不備（Broken Access Control）

### チェック項目
- [ ] 全エンドポイントに認可アノテーション（`@PreAuthorize`, `@Secured`）が設定されている
- [ ] 認可チェックなしの公開エンドポイントは**意図的であることがコメントで明記**されている
- [ ] 管理者向け API に `ROLE_ADMIN` 等の適切なロールが要求されている
- [ ] **IDOR（Insecure Direct Object Reference）防止**: パスパラメータの ID でリソースにアクセスする際、そのリソースがリクエスト者に属するかの検証（オーナーシップチェック）が実装されている
- [ ] CORS 設定で `*` ワイルドカードが使用されていない。許可オリジンが明示的に設定されている
- [ ] ディレクトリトラバーサル防止: ファイルパスにユーザー入力が使用されていない

### 検出パターン
```java
// ❌ IDOR 脆弱性: 任意のユーザーのデータにアクセス可能
@GetMapping("/users/{id}/orders")
public List<Order> getUserOrders(@PathVariable Long id) { ... }

// ✅ オーナーシップチェック
@GetMapping("/users/{id}/orders")
@PreAuthorize("@authService.isOwner(#id, authentication)")
public List<Order> getUserOrders(@PathVariable Long id) { ... }
```

---

## A02: 暗号化の失敗（Cryptographic Failures）

### チェック項目
- [ ] パスワードはハッシュ化（**bcrypt / scrypt / Argon2**）して保存されている（MD5 / SHA-1 / SHA-256 単独は禁止）
- [ ] 通信は **TLS 1.2 以上**で暗号化されている
- [ ] 秘密情報（API キー、トークン、パスワード）がソースコードにハードコードされていない
- [ ] 秘密情報が application.properties / application.yml に平文で記述されていない
- [ ] 保存データの暗号化に **AES-256-GCM** が使用されている（ECB モードは禁止）
- [ ] 暗号鍵がソースコードにハードコードされていない
- [ ] **`SecureRandom`** が使用されている（`Random` / `Math.random()` はセキュリティ用途で禁止）
- [ ] Git 履歴に過去コミットで秘密情報が含まれていた痕跡がないか

### 禁止アルゴリズム一覧

| 用途 | 禁止 | 推奨 |
|------|------|------|
| パスワードハッシュ | MD5, SHA-1, SHA-256 単独 | bcrypt, Argon2 |
| 対称暗号 | DES, 3DES, RC4, AES-ECB | AES-256-GCM |
| ハッシュ | MD5, SHA-1 | SHA-256 以上 |
| 乱数生成 | `Random`, `Math.random()` | `SecureRandom` |

---

## A03: インジェクション（Injection）

### チェック項目
- [ ] **SQL インジェクション防止**: SQL クエリに文字列結合が使用されていない。パラメータバインド（`@Query` + `@Param`, `JdbcTemplate` の `?`）が使用されている
- [ ] **XSS 防止**: ユーザー入力がレスポンスに含まれる場合、エスケープ処理が行われている
- [ ] **コマンドインジェクション防止**: `Runtime.exec()` / `ProcessBuilder` の引数にユーザー入力が直接使用されていない
- [ ] **LDAP インジェクション防止**: LDAP クエリにユーザー入力が直接使用されていない
- [ ] `@Valid` / `@Validated` が**全てのコントローラーメソッドの外部入力**に適用されている
- [ ] 入力バリデーションが**ホワイトリスト方式**で実装されている
- [ ] **Spring AI プロンプトインジェクション防止**: ユーザー入力がプロンプトに含まれる場合、システムプロンプトとユーザープロンプトが分離されている

### 検出パターン
```java
// ❌ Critical: SQL インジェクション
String sql = "SELECT * FROM users WHERE name = '" + name + "'";

// ❌ Critical: String.format による SQL
String sql = String.format("SELECT * FROM users WHERE name = '%s'", name);

// ✅ パラメータバインド
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> findByName(@Param("name") String name);
```

---

## A04: 安全でない設計（Insecure Design）

### チェック項目
- [ ] 脅威モデリング（STRIDE）が設計段階で実施されている
- [ ] **レート制限**が認証エンドポイント（ログイン等）に実装されている
- [ ] **ブルートフォース保護**（アカウントロックアウト等）が設計されている
- [ ] 入力サイズの上限がリクエストボディ・ファイルアップロードに設定されている（DoS 防止）
- [ ] **多要素認証**が高リスク操作（管理者ログイン等）に検討されている

---

## A05: セキュリティの設定ミス（Security Misconfiguration）

### チェック項目
- [ ] Spring Boot Actuator の公開エンドポイントが最小限（`health`, `info`）に制限されている
- [ ] `/actuator/env`, `/actuator/configprops`, `/actuator/heapdump` が**外部に公開されていない**
- [ ] `server.error.include-stacktrace=never` が設定されている
- [ ] `server.error.include-message=never` が設定されている（本番）
- [ ] デフォルトのパスワード・設定が変更されている
- [ ] `spring.jpa.show-sql=false` が本番プロファイルで設定されている
- [ ] `spring.jpa.hibernate.ddl-auto=none` が本番プロファイルで設定されている
- [ ] HTTP セキュリティヘッダー（CSP, HSTS, X-Content-Type-Options, X-Frame-Options）が設定されている
- [ ] デバッグモードが本番で無効化されている

---

## A06: 脆弱で古いコンポーネント（Vulnerable and Outdated Components）

### チェック項目
- [ ] 全依存ライブラリに既知の CVE（**CVSS 7.0 以上**）がない
- [ ] SNAPSHOT バージョンが本番ブランチに含まれていない
- [ ] EOL（End of Life）のライブラリが使用されていない
- [ ] `mvn dependency:tree` で推移的依存関係も含めて確認されている
- [ ] バージョンレンジ指定（`[1.0,2.0)`）が使用されていない

---

## A07: 識別と認証の失敗（Identification and Authentication Failures）

### チェック項目
- [ ] 認証メカニズム（JWT / Session / OAuth 2.0）が適切に実装されている
- [ ] JWT 使用時: 署名検証、有効期限チェック、`alg: none` 攻撃への耐性が確保されている
- [ ] セッション管理: 認証後のセッション ID 再生成（セッション固定攻撃対策）が実装されている
- [ ] セッションタイムアウトが設定されている
- [ ] パスワードポリシー（最小長、複雑性）が実装されている
- [ ] Cookie に `Secure`, `HttpOnly`, `SameSite=Strict` が設定されている

---

## A08: ソフトウェアとデータの整合性の不備（Software and Data Integrity Failures）

### チェック項目
- [ ] Java のデシリアライゼーション脆弱性への対策が実装されている
- [ ] CI/CD パイプラインでの整合性チェックが行われている
- [ ] 依存ライブラリの改竄検知（チェックサム検証）が行われている

---

## A09: セキュリティログとモニタリングの不備（Security Logging and Monitoring Failures）

### チェック項目
- [ ] **認証成功**がログに記録されている（ユーザー ID、IP アドレス）
- [ ] **認証失敗**がログに記録されている（試行ユーザー名、IP アドレス。**パスワードは絶対に記録しない**）
- [ ] **認可失敗**がログに記録されている（ユーザー ID、アクセス先リソース、必要な権限）
- [ ] **入力バリデーション失敗**がログに記録されている（リクエスト元 IP。入力値自体は記録しない）
- [ ] ログに個人情報・秘密情報が**マスキングなしで出力されていない**
- [ ] ログの改竄防止策が設計されている

---

## A10: サーバーサイドリクエストフォージェリ（SSRF）

### チェック項目
- [ ] ユーザー入力の URL に対してサーバーサイドから HTTP リクエストを送信するパターンが存在しないか確認されている
- [ ] 存在する場合、**URL のホワイトリスト検証**が実装されている
- [ ] **内部ネットワークへのアクセス**（`localhost`, `127.0.0.1`, `10.x.x.x`, `192.168.x.x`, `169.254.x.x`）が禁止されている
- [ ] Spring AI 2.0 で使用する外部 AI API のエンドポイントが固定化されている（ユーザー入力で変更不可）
