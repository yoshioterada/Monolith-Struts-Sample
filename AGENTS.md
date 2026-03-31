# AGENTS.md — SkiShop 移行エージェント指示書
## Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x

本ファイルはエージェントが移行作業を実施する際に**必ず最初に読み込むドキュメント**である。
全セクションを熟読した上で作業を開始すること。

---

## 1. 本プロジェクトの概要

| 項目 | 内容 |
|------|------|
| プロジェクト名 | SkiShop（スキー用品 EC サイト） |
| 移行元 | Java 5 / Struts 1.3.10 / Tomcat 6 / WAR パッケージ |
| 移行先 | Java 21 / Spring Boot 3.2.x / 組み込み Tomcat / JAR パッケージ |
| 移行先ディレクトリ | `appmod-migrated-java21-spring-boot-3rd/` |
| ベースパッケージ | `com.skishop` |
| DB | PostgreSQL（スキーマは変更しない／追加のみ可） |
| 詳細設計書 | `docs/migration/DESIGN.md` |
| 移行計画書 | `docs/migration/PLAN.md` |

**作業前に必ず参照するファイル一覧**:
1. `docs/migration/DESIGN.md` — アーキテクチャ・変換仕様・コード例
2. `docs/migration/PLAN.md` — フェーズ計画・チェックリスト・完了条件
3. `.github/instructions/java-coding-standards.instructions.md` — Java コーディング規約
4. `.github/instructions/security-coding.instructions.md` — セキュリティ規約
5. `.github/instructions/api-design.instructions.md` — Controller/API 設計規約
6. `.github/instructions/spring-config.instructions.md` — Spring 設定規約
7. `.github/instructions/pom-dependency.instructions.md` — 依存関係管理規約
8. `.github/instructions/test-standards.instructions.md` — テスト規約
9. `.github/instructions/dockerfile-infra.instructions.md` — Dockerfile / コンテナ設定規約
10. `.github/instructions/sql-schema-review.instructions.md` — SQL スキーマ・Flyway 規約

---

## 2. アーキテクチャの基本理解

### 2.1 レイヤー依存方向（絶対に逆転させない）

```
Controller → Service → Repository
                         ↓
                     JPA Entity (model/)
```

- **Controller は Repository を直接呼び出さない**
- **Service は Controller に依存しない**
- **Repository は Service/Controller に依存しない**

### 2.2 パッケージマッピング

| 移行元（Struts） | 移行先（Spring Boot） |
|----------------|---------------------|
| `web/action/*.java` | `controller/*.java` |
| `web/form/*.java` | `dto/request/*.java` (record クラス) |
| `service/**/*.java` | `service/*.java` |
| `dao/**/*.java` | `repository/*.java` (Spring Data JPA interface) |
| `domain/**/*.java` | `model/*.java` (JPA @Entity) |
| `common/util/PasswordHasher.java` | `util/PasswordHasher.java` (継続使用) |

### 2.3 移行数サマリー

| レイヤー | 数 | 移行先 |
|---------|-----|--------|
| Struts Action | 29 | Spring MVC Controller（8 クラスに集約） |
| ActionForm | 12 | Bean Validation 付き record DTO |
| DAO | 20 | Spring Data JPA Repository（interface） |
| Service | 13 | @Service クラス（CheckoutService 追加で 14） |
| Domain POJO | 22 | JPA @Entity |
| JSP テンプレート | 30+ | Thymeleaf テンプレート |

---

## 3. コーディング規約（必須遵守）

以下は `.github/instructions/java-coding-standards.instructions.md` の要点。
**詳細はインストラクションファイル本体を参照すること。**

### 3.1 命名規則

| 対象 | 規約 | 正しい例 | 誤った例 |
|------|------|---------|---------|
| クラス名 | PascalCase | `UserService`, `OrderController` | `user_service` |
| インターフェース | PascalCase（`I` プレフィックス不要） | `UserRepository` | `IUserRepository` |
| メソッド | camelCase + 動詞始まり | `findByEmail`, `createOrder` | `userSearch`, `data` |
| 変数 | camelCase + 意味のある名前 | `userName`, `orderItems` | `x`, `tmp`, `data` |
| 定数 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` | `maxRetryCount` |
| boolean | `is/has/can/should` プレフィックス | `isActive`, `hasPermission` | `active`, `checkPerm` |
| コレクション変数 | 複数形 | `users`, `orderItems` | `userList`, `items_arr` |

### 3.2 禁止事項チェックリスト（Critical / High）

| 優先度 | 禁止事項 | 代替手段 |
|--------|---------|---------|
| **Critical** | `System.out.println` / `System.err.println` | SLF4J `@Slf4j` + `log.info()` |
| **Critical** | `catch (Exception e) {}` 例外の握りつぶし | 必ずログ出力または再スロー |
| **Critical** | 秘密情報のハードコード（`password = "..."` 等） | 環境変数 `${DB_PASSWORD}` |
| **Critical** | 文字列結合による SQL 構築 | Spring Data JPA / `@Query` パラメータバインド |
| **Critical** | `new` による Service/Repository の生成 | Spring DI（コンストラクタインジェクション） |
| **High** | `@Autowired` フィールドインジェクション | コンストラクタインジェクション |
| **High** | `Optional.get()` の使用 | `orElseThrow()` / `orElse()` / `map()` |
| **High** | ログへの個人情報出力 | マスキングまたは出力しない |
| **High** | Controller が Repository を直接参照 | Service 経由 |
| **High** | `@Transactional` を Controller に付与 | Service 層のみに付与 |

### 3.3 Spring Boot DI 規約

```java
// ✅ 正しい: コンストラクタインジェクション (@RequiredArgsConstructor が推奨)
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;
}

// ❌ 禁止: フィールドインジェクション
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;  // 禁止
}
```

### 3.4 Java 21 の積極的活用

| 機能 | 適用場面 | コード例 |
|------|---------|---------|
| **record クラス** | DTO（リクエスト/レスポンス）の不変型定義 | `public record LoginRequest(String email, String password) {}` |
| **sealed クラス** | 決済結果などの限定型階層 | `sealed interface PaymentResult permits Success, Failure` |
| **switch 式** | status の文字列 → Enum 変換等 | `var label = switch (s) { case "A" -> ...; }` |
| **パターンマッチング** | instanceof チェック | `if (obj instanceof String s) { process(s); }` |
| **テキストブロック** | JPQL / HTML テンプレート文字列 | `""" SELECT u FROM User u WHERE ... """` |

### 3.5 @Transactional の使い方

| ケース | アノテーション |
|--------|-------------|
| 読み取り専用クエリ | `@Transactional(readOnly = true)` |
| 単一テーブル更新 | `@Transactional` |
| 複数テーブル更新（注文確定等） | `@Transactional`（Service メソッドで一括） |
| バッチ処理（新規トランザクション） | `@Transactional(propagation = Propagation.REQUIRES_NEW)` |

### 3.6 例外処理規約

```java
// ✅ 正しい例外クラス階層
ResourceNotFoundException  → HTTP 404
BusinessException          → HTTP 422（ビジネスルール違反）
AuthenticationException    → HTTP 401

// ✅ グローバル例外ハンドラー
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(...) { return "error/404"; }
}
```

- `catch (Exception e)` で全例外をまとめてキャッチしない
- `catch` ブロック内は必ず `log.error("message: {}", e.getMessage(), e)` でスタックトレースを含めてログ出力する

### 3.7 Null Safety

```java
// ❌ 禁止
return null;  // コレクション型の場合
Optional.get();

// ✅ 正しい
return List.of();  // コレクションが空の場合
return Collections.emptyList();
userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
```

---

## 4. セキュリティ規約（最優先）

以下は `.github/instructions/security-coding.instructions.md` の要点。
セキュリティ規約の違反は他の全規約より優先的に修正する。

### 4.1 入力バリデーション

```java
// ✅ すべてのリクエスト DTO に Bean Validation アノテーション
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 100) String password
) {}

// ✅ Controller で @Valid を必ず付与
@PostMapping("/auth/login")
public String login(@Valid @ModelAttribute LoginRequest request, BindingResult result) { ... }
```

**NG パターン一覧**:
- バリデーションなしで Service を直接呼ぶ
- ブラックリスト方式の検証（ホワイトリスト方式に変更）
- コレクションパラメータに上限なし

### 4.2 SQL インジェクション防止（絶対禁止）

```java
// ❌ Critical 違反: 絶対禁止
String sql = "SELECT * FROM users WHERE email = '" + email + "'";

// ✅ 安全: Spring Data メソッド名クエリ
Optional<User> findByEmail(String email);

// ✅ 安全: @Query + バインドパラメータ
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
```

### 4.3 Spring Security 設定の必須項目

`SecurityConfig.java` に以下を全て含めること:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize を Service 層で使用可能にする
public class SecurityConfig {
    // 1. 認可設定（全 URL をカバー）
    //    /admin/** → ADMIN のみ
    //    /account/**, /orders/**, /checkout/** → USER または ADMIN
    //    /actuator/** → ADMIN のみ（/health, /info は除く）
    //    その他 → permitAll

    // 2. フォームログイン設定
    //    ログインページ、処理 URL、成功/失敗 URL

    // 3. ログアウト設定
    //    セッション無効化、JSESSIONID クッキー削除

    // 4. セッション管理
    //    .sessionFixation().migrateSession()  // セッション固定攻撃防止
    //    .maximumSessions(1)

    // 5. CSRF 保護（有効化必須）
    //    .csrf(Customizer.withDefaults())

    // 6. セキュリティヘッダー
    //    CSP, X-Frame-Options(DENY), X-Content-Type-Options, HSTS
    //    xssProtection は Customizer.withDefaults() を使用（enable() は非推奨）

    // 7. HSTS
    //    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
}
```

### 4.4 パスワードハッシュ移行

現行 DB の `users.password_hash` と `users.salt` は以下の Flyway V2 で変換済み前提:

```
password_hash カラムの形式: {sha256}<hash>$<salt>  (V2 適用後)
                        または {bcrypt}<hash>        (ログイン後 BCrypt 昇格済み)
```

`DelegatingPasswordEncoder` の設定:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("sha256", new LegacySha256PasswordEncoder());  // util/ 配下
    return new DelegatingPasswordEncoder("bcrypt", encoders);
}
```

`CustomUserDetailsService` は `UserDetailsService` と `UserDetailsPasswordService` の両方を implements すること。`updatePassword()` でログイン成功時の BCrypt 自動アップグレードを実装する。

### 4.5 管理者操作への多層防御

URL レベル認可（SecurityConfig）に加え、管理者専用 Service メソッドには `@PreAuthorize` を付与:

```java
@Service
public class AdminProductService {
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product updateProduct(String id, AdminProductRequest request) { ... }
}
```

### 4.6 IDOR（オブジェクトレベル認可）防止

注文・住所等のユーザー固有リソースは、ログイン済みユーザーの ID と照合してオーナーシップを検証する:

```java
// ✅ ログインユーザーの注文のみ参照可能
@GetMapping("/orders/{id}")
public String orderDetail(@PathVariable String id,
                          @AuthenticationPrincipal UserDetails user,
                          Model model) {
    Order order = orderService.findByIdAndUserId(id, user.getUsername());
    // ...
}
```

### 4.7 PII ログ禁止

ログに出力してはいけない情報:
- `users.email`（メールアドレス全文）
- `users.password_hash` / `users.salt`（パスワード関連）
- `addresses.*`（住所情報）
- クレジットカード番号、決済情報

`SecurityLog` には IP アドレスとイベント種別のみ記録する。

---

## 5. Controller / API 設計規約

以下は `.github/instructions/api-design.instructions.md` の要点。

### 5.1 URL 設計

| 現行（Struts） | 移行後（Spring Boot） | HTTP メソッド |
|--------------|---------------------|-------------|
| `/login.do` | `/auth/login` | GET / POST |
| `/products.do` | `/products` | GET |
| `/product.do?id=xxx` | `/products/{id}` | GET |
| `/cart.do` (追加) | `/cart/items` | POST |
| `/orders/cancel.do` | `/orders/{id}/cancel` | POST |
| `/admin/products.do` | `/admin/products` | GET |
| `/admin/product/edit.do` | `/admin/products/{id}` | GET(表示) / PUT(更新) |
| `/admin/product/delete.do` | `/admin/products/{id}` | DELETE |

- **`*.do` URL は完全廃止**。移行後のコードに `*.do` パターンを含めない
- URI は名詞（複数形）で構成し、動詞を含めない
- 画面遷移は `String`（テンプレート名）、リダイレクトは `"redirect:/path"` を返す

### 5.2 エラーレスポンス

Spring Boot 3.2 の `ProblemDetail` を使用して RFC 7807 準拠のエラーレスポンスを返す。
スタックトレースをクライアントに返さない（`server.error.include-stacktrace=never`）。

---

## 6. Spring 設定ファイル規約

以下は `.github/instructions/spring-config.instructions.md` の要点。

### 6.1 プロファイル別ファイル構成

```
src/main/resources/
├── application.properties           # 共通（安全なデフォルト値のみ）
├── application-dev.properties       # 開発（ローカル PostgreSQL / DEBUG ログ）
├── application-test.properties      # テスト（H2 in-memory / Flyway 無効）
├── application-staging.properties   # ステージング（PostgreSQL / INFO ログ）
└── application-prod.properties      # 本番（環境変数参照 / WARN ログ）
```

### 6.2 必須設定項目（application.properties 共通）

```properties
spring.application.name=skishop-app
server.error.include-stacktrace=never
server.error.include-message=never
spring.jpa.open-in-view=false    # LazyInitializationException を View 層まで隠蔽しない
spring.thymeleaf.cache=false     # dev のみキャッシュ無効（prod は true）
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
logging.level.root=WARN
logging.level.com.skishop=INFO
```

### 6.3 秘密情報管理

```properties
# ✅ 環境変数で参照
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${MAIL_PASSWORD}

# ❌ Critical 違反: 直接記述
spring.datasource.password=mysecretpassword
```

---

## 7. 依存関係管理規約

以下は `.github/instructions/pom-dependency.instructions.md` の要点。

### 7.1 必須依存関係（pom.xml）

```xml
<!-- Spring Boot BOM 管理 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>
</parent>

<!-- 主要依存関係 -->
spring-boot-starter-web
spring-boot-starter-thymeleaf
thymeleaf-extras-springsecurity6       <!-- th:sec, #authentication を使うために必須 -->
thymeleaf-layout-dialect               <!-- layout:decorate で Tiles を置換するために必須 -->
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-mail
spring-boot-starter-actuator
flyway-core
flyway-database-postgresql             <!-- Flyway の PostgreSQL 方言サポート -->
micrometer-registry-prometheus
springdoc-openapi-starter-webmvc-ui    <!-- version: 2.3.x (BOM 外のため明示指定) -->
lombok                                 <!-- provided スコープ / annotationProcessorPaths に追加 -->
net.logstash.logback:logstash-logback-encoder  <!-- prod JSON ログ用（BOM 外のため明示指定, 8.x） -->
postgresql                             <!-- runtime スコープ -->
h2                                     <!-- test スコープ -->
spring-boot-starter-test               <!-- test スコープ -->
spring-security-test                   <!-- test スコープ -->
```

### 7.2 禁止依存関係（移行元からの持ち越し禁止）

| 禁止ライブラリ | 理由 | 代替 |
|-------------|------|------|
| `struts`, `struts-core` | 移行元フレームワーク | Spring MVC |
| `log4j:log4j:1.x` | EOL + Log4Shell 脆弱性 | SLF4J + Logback（Boot 管理） |
| `commons-dbcp` | 旧世代 DBCP | HikariCP（Boot 管理） |
| `commons-dbutils` | 旧世代 DAO ユーティリティ | Spring Data JPA |
| `javax.servlet.*` | Jakarta EE 前の旧パッケージ | `jakarta.servlet.*` |
| `javax.mail.*` | Jakarta EE 前の旧パッケージ | `jakarta.mail.*` |
| `junit:junit:4.x` | 旧世代テスト | JUnit 5（Boot 管理） |
| `-SNAPSHOT` バージョン | 不安定 | GA（正式リリース）を使用 |

---

## 8. テスト規約

以下は `.github/instructions/test-standards.instructions.md` の要点。

### 8.1 テスト種別と対応アノテーション

| テスト種別 | アノテーション | 対象 |
|---------|-------------|------|
| Unit Test | JUnit 5 + Mockito（アノテーションなし） | Service, Util |
| Web スライステスト | `@WebMvcTest` | Controller（セキュリティ含む） |
| DB スライステスト | `@DataJpaTest` | Repository（H2 + `MODE=PostgreSQL`） |
| 統合テスト | `@SpringBootTest` + `@ActiveProfiles("test")` | 全レイヤー |
| セキュリティテスト | Spring Security Test + `@WithMockUser` | 認証/認可 |

### 8.2 テストメソッド命名（必須）

```java
// ✅ should_期待結果_when_条件 パターン
@Test
@DisplayName("有効な認証情報でログインした場合、ホームにリダイレクトされる")
void should_redirectToHome_when_validCredentialsProvided() { ... }

@Test
@DisplayName("5回失敗した場合、アカウントがロックされる")
void should_lockAccount_when_loginFailsFiveTimes() { ... }
```

### 8.3 AAA パターン（Arrange-Act-Assert）

```java
@Test
void should_findUser_when_emailExists() {
    // Arrange
    var user = new User();
    user.setEmail("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    // Act
    var result = authService.findByEmail("test@example.com");

    // Assert
    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getEmail()).isEqualTo("test@example.com");
}
```

### 8.4 カバレッジ目標

`test-standards.instructions.md` が定める必須基準: **分岐カバレッジ 80% 以上**

| レイヤー | 目標 | 備考 |
|---------|------|------|
| Service | 80% 以上 | 必須（ビジネスロジック中心） |
| Controller | 80% 以上（@WebMvcTest） | セキュリティ含む画面フロー |
| Repository | 70% 以上（@DataJpaTest） | スライステスト（H2 + MODE=PostgreSQL） |
| 全体 | 80% 以上 | `mvn clean verify -Djacoco.skip=false` で確認 |

---

## 9. 移行実装上の重要知識

### 9.1 Struts → Spring の変換パターン

#### Action → Controller

```java
// ❌ 移行元 (Struts)
public class LoginAction extends Action {
    private final AuthService authService = new AuthService();  // new で生成

    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest req, HttpServletResponse res) {
        LoginForm f = (LoginForm) form;
        return mapping.findForward("success");
    }
}

// ✅ 移行後 (Spring Boot)
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;  // DI

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest request,
                        BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return "auth/login";
        // ...
        return "redirect:/";
    }
}
```

#### DAO → Repository

```java
// ❌ 移行元 (Struts / JDBC)
public class UserDaoImpl extends AbstractDao implements UserDao {
    public User findByEmail(String email) throws DaoException {
        Connection con = getConnection();
        // 手動 JDBC 処理...
    }
}

// ✅ 移行後 (Spring Data JPA)
// interface のみ。実装クラスは不要
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(String status);
}
```

#### Repository ごとに1 Aggregate Root の原則

```java
// ❌ 禁止: UserRepository に SecurityLog のクエリを含める
public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT COUNT(sl) FROM SecurityLog sl WHERE sl.userId = :userId...")
    long countSecurityLogs(...);  // cross-entity クエリは禁止
}

// ✅ 正しい: SecurityLogRepository に分離
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {
    long countByUserIdAndEventType(String userId, String eventType);
}
```

#### ActionForm → DTO (record)

```java
// ❌ 移行元 (Struts ActionForm)
public class LoginForm extends ActionForm {
    private String email;
    private String password;
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest req) { ... }
}

// ✅ 移行後 (Java record + Bean Validation)
public record LoginRequest(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    String email,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100)
    String password
) {}
```

#### Singleton AppConfig → @Value / @ConfigurationProperties

```java
// ❌ 移行元
String host = AppConfig.getInstance().getString("mail.host");

// ✅ 移行後
@Value("${spring.mail.host}")
private String mailHost;

// または (推奨)
@ConfigurationProperties(prefix = "spring.mail")
public record MailProperties(String host, int port) {}
```

### 9.2 セッション管理の変換

| 移行元（Struts） | 移行後（Spring Security） |
|----------------|-------------------------|
| `session.setAttribute("loginUser", user)` | `SecurityContextHolder`（Spring Security が管理） |
| `session.getAttribute("loginUser")` | `@AuthenticationPrincipal UserDetails user` |
| `session.invalidate()` | Spring Security のログアウト処理に委任 |

カート ID 管理（未ログイン時）:
```java
// HTTP セッションに cartId を格納（Cart エンティティは DB に保存）
String cartId = (String) session.getAttribute("cartId");
if (cartId == null) {
    cartId = UUID.randomUUID().toString();
    session.setAttribute("cartId", cartId);
}
```

ログイン成功時は `CartMergeSuccessHandler`（`AuthenticationSuccessHandler` 実装）で
セッションカートをユーザーカートにマージする。

### 9.3 JPA エンティティ作成の必須ルール

```java
@Entity
@Table(name = "users")  // スネークケース DB テーブル名を明示
public class User {

    @Id
    @Column(name = "id", length = 36)
    private String id;  // UUID は自動生成しない。service 層で UUID.randomUUID().toString()

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;  // camelCase フィールド名 → @Column でスネークケース指定

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // java.util.Date は使用禁止

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 親子関係: LAZY + CASCADE ALL + orphanRemoval
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)  // N+1 対策
    private List<OrderItem> items = new ArrayList<>();
}
```

**必須チェック項目**:
- `java.util.Date` は使用禁止 → `java.time.LocalDateTime` / `LocalDate` を使用
- 全フィールドに `@Column(name = "...")` でカラム名を明示
- UUID PK は `@GeneratedValue` なし
- コレクション関連は `LAZY` フェッチ + `@BatchSize`

### 9.4 CheckoutService の注文確定フロー

注文確定は単一の `@Transactional` で以下の 11 ステップを原子化する（途中で例外が発生したら全ロールバック）:

```
1. カートからアイテム取得（空チェック）
2. 在庫確認（checkStock）
3. クーポン適用（discountAmount 計算）
4. ポイント仮消費（reservePoints）
5. 注文レコード作成
6. 注文明細作成
7. 在庫減算（deductStock）
8. 支払いレコード作成
9. ポイント確定付与（awardPoints, totalAmount ベース）
10. カートクリア
11. 受注確認メールをキューに追加（EmailQueue, 同一 TX）
```

### 9.5 Thymeleaf テンプレートへの変換

| Struts タグ | Thymeleaf 相当 |
|------------|--------------|
| `<html:form action="/login.do">` | `<form th:action="@{/auth/login}" method="post">` |
| `<html:text property="email">` | `<input th:field="*{email}">` |
| `<html:errors property="email">` | `<span th:errors="*{email}">` |
| `<logic:present name="loginUser">` | `<div th:if="${#authentication.authenticated}">` |
| `<logic:iterate collection="products">` | `<tr th:each="product : ${products}">` |
| `<bean:write name="user" property="username">` | `<span th:text="${user.username}">` |
| `<bean:message key="label.xxx">` | `<span th:text="#{label.xxx}">` |
| `<html:link page="/product.do?id=...">` | `<a th:href="@{/products/{id}(id=${product.id})}">` |

XSS 対策: `th:text` を使用（`th:utext` は原則禁止）。
CSRF: `<form th:action="@{/...}" method="post">` を使えば Spring Security が自動挿入。

### 9.6 Tiles → Thymeleaf Layout Dialect

```html
<!-- 移行元: Tiles定義 -->
tiles-defs.xml の <definition name="base.layout" ...>

<!-- 移行後: layout.html (fragments/layout.html) -->
<html xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <title layout:title-pattern="$DECORATOR_TITLE - SkiShop">SkiShop</title>
</head>
<body>
    <div th:replace="~{fragments/header :: header}"></div>
    <main layout:fragment="content"><!-- ページ固有コンテンツ --></main>
    <div th:replace="~{fragments/footer :: footer}"></div>
</body>
</html>

<!-- 移行後: 各ページ (例: auth/login.html) -->
<html layout:decorate="~{fragments/layout}">
<body>
    <div layout:fragment="content">
        <!-- ページ固有コンテンツ -->
    </div>
</body>
</html>
```

---

## 10. フェーズ実行の注意事項

### 10.1 各フェーズの完了条件

各フェーズ終了時に **必ず** 以下を確認すること:

| フェーズ | 最低限の確認コマンド |
|---------|-------------------|
| Phase 1（基盤構築） | `mvn clean compile` |
| Phase 2（エンティティ） | `mvn clean compile` + `@DataJpaTest` で H2 スキーマ確認 |
| Phase 3（Repository） | `mvn test -Dtest="*RepositoryTest"` |
| Phase 4（Service） | `mvn test -Dtest="*ServiceTest"` |
| Phase 5（Controller） | `mvn test -Dtest="*ControllerTest"` |
| Phase 6（View） | `mvn spring-boot:run` + 全画面 HTTP 200 確認 |
| Phase 7（Security） | Spring Security Test 全件通過 |
| Phase 8（Test） | `mvn clean verify -Djacoco.skip=false` |
| Phase 9（最終） | `mvn clean verify` + Docker イメージビルド |

### 10.2 自動生成・ツール使用時の確認事項

自動生成や OpenRewrite 等のツールを使用した場合は必ず以下を確認する:
1. `@Autowired` フィールドインジェクションが生成されていないか
2. `javax.*` パッケージが残っていないか（`jakarta.*` に変換されているか）
3. `Optional.get()` が使用されていないか
4. ログに `System.out.println` が残っていないか
5. SQL を文字列結合で構築するコードが生成されていないか

### 10.3 DB 操作上の注意

- **スキーマは変更しない**。追加のみ可。既存カラムの定義変更・削除は禁止
- Flyway V1 は現行 `schema.sql` をそのまま転記
- Flyway V2 はパスワードハッシュプレフィックス付与（`docs/migration/DESIGN.md §11.2` 参照）
- H2 テスト環境では `MODE=PostgreSQL` を設定してクエリ方言を合わせる
- Flyway SQL ファイルの命名は `V<N>__<snake_case_description>.sql` 形式で統一（例: `V1__initial_schema.sql`）
- SQL 内の命名規則: テーブル名は `snake_case` 複数形、カラム名は `snake_case`（`sql-schema-review.instructions.md` 準拠）

### 10.4 セキュリティ上の絶対禁止事項

事前確認コマンド（コードレビュー前に実施）:
```bash
# 秘密情報のハードコードチェック
grep -r "password\s*=\s*\"" src/main/

# 文字列結合 SQL チェック
grep -r '"SELECT.*+\|"UPDATE.*+\|"INSERT.*+' src/main/java/

# System.out のチェック
grep -r "System\.out\." src/main/java/

# new によるサービス生成チェック
grep -r "= new.*Service\|= new.*Repository" src/main/java/

# @Autowired フィールドインジェクションチェック
grep -rA1 "@Autowired" src/main/java/ | grep -v "@Bean\|@Qualifier"
```

### 10.5 Dockerfile 規約

以下は `.github/instructions/dockerfile-infra.instructions.md` の要点。Phase 9（最終）の Docker イメージビルド時に遵守すること。

```dockerfile
# ✅ 正しい: マルチステージビルド + 非 root + バージョン固定
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
# 非 root ユーザーの作成（必須）
RUN groupadd -r skishop && useradd -r -g skishop -d /app skishop
COPY --from=build --chown=skishop:skishop /app/target/skishop-app.jar app.jar
USER skishop

# コンテナ対応 JVM フラグ（-Xmx 固定値禁止）
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**必須チェック項目**:
- ベースイメージのタグは `latest` 禁止 → `21-jre` 等の固定バージョンを指定
- ランタイムイメージは JRE を使用し JDK は含めない
- `USER` 命令で非 root ユーザーに切り替え（root 実行禁止）
- `-Xmx512m` などのメモリ固定値を使わず `UseContainerSupport` + `MaxRAMPercentage` を使用
- `HEALTHCHECK` を必ず設定（`/actuator/health` エンドポイントを利用）
- `.dockerignore` で `target/`, `.git/`, `*.md` を除外し、イメージサイズを最小化

---

## 11. よくあるミスと対策

| ミス | 原因 | 対策 |
|------|------|------|
| `LazyInitializationException` | View 層でセッション外の LAZY 関連をアクセス | `spring.jpa.open-in-view=false` + `@EntityGraph` か `JOIN FETCH` で明示的 FETCH |
| BCrypt アップグレードが走らない | `UserDetailsPasswordService` を実装していない | `CustomUserDetailsService` に `UserDetailsPasswordService` を implements |
| sha256 ユーザーがログイン不可 | V2 Flyway 未適用、または `$` 区切りの salt パースミス | `LegacySha256PasswordEncoder.matches()` のデバッグ + V2 SQL 確認 |
| CSRF エラー（AJAX POST） | AJAX リクエストに CSRF トークンを付与していない | `X-CSRF-TOKEN` ヘッダーを JS で付与 |
| N+1 クエリ | LAZY 関連を一覧ループ内でアクセス | `@EntityGraph` / `JOIN FETCH` / `@BatchSize` で対策 |
| H2 テストのみ通過（PostgreSQL で失敗） | H2/PostgreSQL の SQL 方言差異 | `@DataJpaTest` で `spring.datasource.url=jdbc:h2:mem:...;MODE=PostgreSQL` 設定 |
| 循環依存 | Service が互いに注入し合っている | 依存関係を見直す。`@Lazy` は一時回避のみ |
| テスト用プロパティに本番値 | 設定ファイルの混用 | `application-test.properties` はすべてダミー値 |

---

## 12. ファイル探索ガイド

移行作業中に特定の情報を見つける際の参照先:

| 知りたいこと | 参照先 |
|------------|--------|
| 全 29 Action の URL / ロール / 移行先 Controller | `docs/migration/DESIGN.md` §2.3 |
| 全 20 Repository の追加メソッド一覧 | `docs/migration/PLAN.md` §5（フェーズ 3 テーブル） |
| JPA エンティティの設計原則・コード例 | `docs/migration/DESIGN.md` §8.4 |
| SecurityConfig のコード例（完全版） | `docs/migration/DESIGN.md` §11.1 |
| パスワード移行の SQL / LegacySha256PasswordEncoder 実装 | `docs/migration/DESIGN.md` §11.2 |
| CheckoutService の 11 ステップ詳細 | `docs/migration/DESIGN.md` §7.3 |
| カートセッション管理の実装パターン | `docs/migration/DESIGN.md` §6.6 |
| GlobalExceptionHandler のコード例 | `docs/migration/DESIGN.md` §6.7 |
| メールテンプレート移行 | `docs/migration/DESIGN.md` §10.5 |
| Logback 設定（logstash-logback-encoder 含む） | `docs/migration/DESIGN.md` §12.4 |
| Thymeleaf → JSP マッピング全ファイル | `docs/migration/DESIGN.md` §10.3 |
| 品質チェックリスト（全 Action 機能等価性確認） | `docs/migration/PLAN.md` §13-B |
| フェーズ別完了条件 | `docs/migration/PLAN.md` 各フェーズの「完了条件」セクション |
| リスクと対策 | `docs/migration/PLAN.md` §12 |
| ロールバック手順 | `docs/migration/PLAN.md` §15 |
| 現行ソースコード | `src/main/java/com/skishop/` |
| 現行 DB スキーマ | `src/main/resources/db/schema.sql` |
| 現行 Struts 設定 | `src/main/webapp/WEB-INF/struts-config.xml` |
