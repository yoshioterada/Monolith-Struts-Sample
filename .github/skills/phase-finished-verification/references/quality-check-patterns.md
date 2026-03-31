# 品質チェックパターン集

Java 5 → Java 21 / Struts 1.3 → Spring Boot 3.2.x 移行の完了検証で使用する
手抜き検出パターン・コード品質基準の詳細を定義する。

---

## 1. コード品質 — grep 検索パターン

検証時に以下のパターンを `grep_search` または `run_in_terminal` で実行し、該当箇所を記録する。

### 1.1 未完了作業の痕跡

| パターン | 対象ファイル | 判定 |
|---------|------------|------|
| `TODO` | `appmod-migrated-java21-spring-boot-3rd/src/main/java/**` | ❌ フェーズ完了時は全て解消済みであること |
| `FIXME` | 同上 | ❌ 同上 |
| `HACK` | 同上 | ❌ 同上 |
| `XXX` | 同上 | ⚠️ コメント内容を確認し、一時的な回避策なら ❌ |
| `TEMP` | 同上 | ❌ 仮実装は許可しない |
| `Not implemented` | 同上 | ❌ `throw new UnsupportedOperationException("Not implemented")` 等 |

```bash
grep -rn "TODO\|FIXME\|HACK\|XXX\|TEMP\|Not implemented" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/
```

### 1.2 デバッグコード・ロギング問題

| パターン | 対象ファイル | 判定 |
|---------|------------|------|
| `System.out.println` | `src/main/java/**` | ❌ SLF4J `@Slf4j` + `log.info()` を使用すること |
| `System.err.println` | 同上 | ❌ 同上 |
| `e.printStackTrace()` | 同上 | ❌ `log.error("msg: {}", e.getMessage(), e)` を使用すること |
| `System.exit(` | 同上 | ❌ Spring Application Context を終了させない |
| PII ログ出力 | 同上 | ❌ `email`, `password`, `address`, `credit` をログ出力しない |

```bash
# System.out / System.err
grep -rn "System\.out\.\|System\.err\." \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# e.printStackTrace
grep -rn "\.printStackTrace()" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# PII をログに出力
grep -rn "log\.\(info\|debug\|warn\|error\)" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/ \
  | grep -iE "(email|password|address|credit|password_hash)"
```

### 1.3 SQL インジェクション（文字列結合 SQL）— Critical

**ルール**: SQL を文字列結合で構築することは絶対禁止。Spring Data JPA のメソッド名クエリまたは `@Query` + パラメータバインドを使用すること。

| パターン | 判定 |
|---------|------|
| `"SELECT ... " + variable` | ❌ 絶対禁止（SQLi 脆弱性） |
| `"UPDATE ... " + variable` | ❌ 絶対禁止 |
| `"INSERT ... " + variable` | ❌ 絶対禁止 |
| `"DELETE ... " + variable` | ❌ 絶対禁止 |
| `jdbcTemplate.query(sql + var, ...)` | ❌ 絶対禁止 |

```bash
grep -rE '"(SELECT|UPDATE|INSERT|DELETE)[^"]*"\s*\+' \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

grep -rE 'query\(\s*sql\s*\+|execute\(\s*sql\s*\+' \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/
```

✅ **正しい実装例**:
```java
// ✅ Spring Data JPA メソッド名クエリ
Optional<User> findByEmail(String email);

// ✅ @Query + パラメータバインド
@Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
List<User> findActiveByEmail(@Param("email") String email, @Param("status") String status);
```

❌ **禁止パターン**:
```java
// ❌ 文字列結合 SQL（SQLi 脆弱性）
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
jdbcTemplate.queryForObject(sql, User.class);
```

### 1.4 Spring DI 違反 — Critical

| パターン | 判定 |
|---------|------|
| `@Autowired` フィールドインジェクション | ❌ コンストラクタインジェクションを使用すること |
| `new UserService()` 等の直接生成 | ❌ Spring DI を使用すること |
| `new UserRepository()` | ❌ 同上 |
| Controller が Repository を直接 import | ❌ Service 経由を必ず通すこと |

```bash
# @Autowired フィールドインジェクション（@Bean, @Qualifier, @Primary 付きは除外）
grep -rn "@Autowired" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/ \
  | grep -v "@Bean\|@Qualifier\|@Primary"

# new による Service/Repository 生成
grep -rE "=\s*new\s+\w*(Service|Repository|Dao|Manager)\(" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# Controller からの Repository 直接参照
grep -rn "Repository" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/

# @Transactional を Controller に付与（Service 層のみに許可）
grep -rn "@Transactional" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/
```

✅ **正しい実装例**:
```java
// ✅ コンストラクタインジェクション（Lombok @RequiredArgsConstructor 推奨）
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;
    private final PasswordEncoder passwordEncoder;
}
```

### 1.5 Java 型安全性

| パターン | 判定 |
|---------|------|
| `Optional.get()` を直接呼び出し | ❌ `orElseThrow()` / `orElse()` / `map()` を使用すること |
| `java.util.Date` の使用 | ❌ `java.time.LocalDateTime` / `LocalDate` を使用すること |
| `new Date()` の使用 | ❌ 同上 |
| コレクション戻り値に `null` | ❌ `List.of()` / `Collections.emptyList()` を返すこと |

```bash
# Optional.get() 直接呼び出し（findBy*.get() パターン等を検出）
grep -rn "\.get()" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/ \
  | grep -v "getClass\|getMessage\|getName\|getValue\|getType\|getId\|getUser\|getOrder\|getProduct\|getCart\|getKey\|getContent\|//\|*"

# java.util.Date
grep -rn "java\.util\.Date\|import java\.util\.Date\|new Date()" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# コレクション戻り値の null
grep -rn "return null;" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/ \
  | grep -v "//\|test\|Test"
```

✅ **正しい実装例**:
```java
// ✅ Optional を安全に使用
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new ResourceNotFoundException("User", email));

// ✅ java.time を使用
private LocalDateTime createdAt;
private LocalDate deliveryDate;
```

### 1.6 禁止パッケージ・URL パターン

| パターン | 判定 |
|---------|------|
| `import javax.servlet.*` | ❌ `import jakarta.servlet.*` に変換すること |
| `import javax.persistence.*` | ❌ `import jakarta.persistence.*` に変換すること |
| `import javax.validation.*` | ❌ `import jakarta.validation.*` に変換すること |
| `import javax.mail.*` | ❌ `import jakarta.mail.*` に変換すること |
| `*.do` URL パターン | ❌ 移行後のコードに `.do` を含めない |
| `org.apache.struts.*` | ❌ Struts パッケージは完全排除 |

```bash
# javax.* パッケージ残存
grep -rn "import javax\." \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# *.do URL パターン残存
grep -rn '\.do"' \
  appmod-migrated-java21-spring-boot-3rd/src/
grep -rn 'action=".*\.do' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/

# Struts パッケージ残存
grep -rn "org\.apache\.struts" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/
```

### 1.7 秘密情報のハードコード — Critical

| パターン | 判定 |
|---------|------|
| `password = "..."` リテラル代入 | ❌ 環境変数 `${DB_PASSWORD}` を使用すること |
| `secret = "..."` リテラル代入 | ❌ 同上 |
| `url = "jdbc:postgresql://..."` リテラル代入 | ⚠️ `${DB_URL}` で外部化されているか確認 |
| API キーのハードコード | ❌ 環境変数を使用すること |

```bash
# パスワード/シークレットのハードコード（プロパティファイル含む）
grep -rE '(password|secret|api[_-]?key|apikey)\s*=\s*"[^$\{]' \
  appmod-migrated-java21-spring-boot-3rd/src/

# application.properties での直接記述（test プロファイルは除外）
grep -rE '^spring\.datasource\.password\s*=\s*[^$\{]' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/ \
  | grep -v "test\|dev"
```

---

## 2. アーキテクチャ準拠チェック

### 2.1 レイヤー依存方向

**ルール**: `Controller → Service → Repository` の依存方向は一方向のみ。逆方向への依存は禁止。

```bash
# Controller から Repository 直接参照チェック
grep -rn "import com\.skishop\.repository\." \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/

# Repository から Service 参照チェック（逆方向依存）
grep -rn "import com\.skishop\.service\." \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/
```

### 2.2 1 Repository = 1 Aggregate Root の原則

**ルール**: 各 Repository は 1 つの Aggregate Root（Entity）のみを管理する。
`UserRepository` に `SecurityLog` のクエリが混在することは禁止。

```bash
# UserRepository に SecurityLog クエリが混在していないか
grep -rn "SecurityLog\|security_log" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/UserRepository.java
```

### 2.3 DTO はレコードクラス

**ルール**: リクエスト DTO は Java `record` クラスとして定義する。旧来の POJO（`get/set` メソッド付きクラス）は使用しない。

```bash
# DTO ディレクトリのクラス種別確認
grep -rn "^public record \|^public class " \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/dto/

# ActionForm を継承するクラスが残存していないか
grep -rn "extends ActionForm\|extends ValidatorForm" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/
```

✅ **正しい実装例**:
```java
// ✅ record クラスとして定義
public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 100) String password
) {}
```

❌ **禁止パターン**:
```java
// ❌ 旧来の POJO（get/set 付き）
public class LoginRequest {
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

### 2.4 例外クラス階層

```bash
# カスタム例外クラスの存在確認
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/ \
  -name "ResourceNotFoundException.java" -o \
  -name "BusinessException.java" -o \
  -name "AuthenticationException.java"

# GlobalExceptionHandler の存在確認
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/ \
  -name "GlobalExceptionHandler.java"

# BusinessException に redirectUrl / messageKey フィールドがあるか
grep -n "redirectUrl\|messageKey" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/exception/BusinessException.java
```

### 2.5 例外握りつぶし検出

```bash
# catch ブロックで log も throw もしていない箇所を検出
grep -rn -A3 "catch\s*(Exception\|RuntimeException\|Throwable" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/ \
  | grep -v "log\.\|throw\|e\.getMessage\|// "
```

✅ **正しいパターン**:
```java
try {
    paymentService.process(paymentId);
} catch (PaymentException e) {
    log.error("決済処理失敗: paymentId={}, msg={}", paymentId, e.getMessage(), e);
    throw new BusinessException("payment.error", e);
}
```

❌ **禁止パターン**:
```java
try {
    sendConfirmationEmail(order);
} catch (Exception e) {
    // 何もしない（握りつぶし）
}
```

---

## 3. セキュリティチェック

### 3.1 Bean Validation（@Valid）付与確認

```bash
# @ModelAttribute に @Valid がない箇所を検出
grep -rn "@ModelAttribute" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/ \
  | grep -v "@Valid"

# @RequestBody に @Valid がない箇所を検出
grep -rn "@RequestBody" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/ \
  | grep -v "@Valid"
```

✅ **正しい実装例**:
```java
@PostMapping("/auth/login")
public String login(@Valid @ModelAttribute LoginRequest request,
                    BindingResult result,
                    RedirectAttributes ra) {
    if (result.hasErrors()) return "auth/login";
    // ...
}
```

### 3.2 IDOR 防止（オーナーシップ検証）

```bash
# OrderController に @AuthenticationPrincipal があるか
grep -rn "@AuthenticationPrincipal" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/OrderController.java

# リソースをユーザー ID で絞り込んでいるか
grep -rn "findByIdAndUserId\|findByIdAndUserEmail\|AndUserId" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/
```

### 3.3 Spring Security 設定の必須項目

```bash
# @EnableWebSecurity, @EnableMethodSecurity の付与確認
grep -n "@EnableWebSecurity\|@EnableMethodSecurity" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/config/SecurityConfig.java

# CSRF 保護・セッション固定攻撃対策・HSTS
grep -n "csrf\|sessionFixation\|httpStrictTransportSecurity\|xssProtection" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/config/SecurityConfig.java

# xssProtection が非推奨 API を使っていないか（xss.enable() は禁止）
grep -n "xss\.enable\b\|\.enable()" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/config/SecurityConfig.java
```

**確認すべき必須設定項目**:

| 設定項目 | 期待値 | Critical か |
|---------|--------|------------|
| CSRF 保護 | `.csrf(Customizer.withDefaults())` | ❌ なければ Critical |
| セッション固定対策 | `.sessionFixation().migrateSession()` | ❌ なければ High |
| 最大セッション数 | `.maximumSessions(1)` | ⚠️ High |
| XSS ヘッダー | `Customizer.withDefaults()`（`xss.enable()` 禁止） | ❌ なければ High |
| HSTS | `includeSubDomains(true).maxAgeInSeconds(31536000)` | ⚠️ Medium |
| X-Frame-Options | `DENY` | ❌ なければ High |

### 3.4 パスワードエンコーダー設定

```bash
# DelegatingPasswordEncoder の設定確認
grep -rn "DelegatingPasswordEncoder\|LegacySha256PasswordEncoder" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# UserDetailsPasswordService を implements しているか
grep -rn "UserDetailsPasswordService\|updatePassword" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# LegacySha256PasswordEncoder の matches() が {sha256}<hash>$<salt> 形式を処理しているか
grep -n "sha256\|split\|\\\$" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/util/LegacySha256PasswordEncoder.java
```

---

## 4. Thymeleaf テンプレートチェック

### 4.1 XSS 対策

```bash
# th:utext 使用箇所（エスケープなし出力 — 原則禁止）
grep -rn "th:utext" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/

# *.do URL が残存していないか
grep -rn '\.do"' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/

# POST フォームで th:action を使っているか（CSRF 自動挿入のため必須）
grep -rn 'method="post"\|method='"'"'post'"'"'' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/ \
  | grep -v "th:action"
```

### 4.2 URL ハードコード禁止

```bash
# href / action に URL をハードコードしていないか（th:href / th:action を使うこと）
grep -rn 'href="/\|action="/' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/ \
  | grep -v "th:href\|th:action"
```

### 4.3 Struts タグ残存確認

```bash
# 旧 Struts/JSTL タグが残存していないか
grep -rn "<%@\|<html:form\|<html:text\|<logic:iterate\|<bean:write\|<bean:message" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/
```

---

## 5. テスト品質チェック

### 5.1 テストファイルの存在

| カテゴリ | テストファイル命名規則 | 必須度 |
|---------|---------------------|--------|
| Service クラス | `{Service}Test.java` | 必須 |
| Controller クラス | `{Controller}Test.java` | 必須（`@WebMvcTest`） |
| Repository インターフェース | `{Repository}Test.java` | 必須（`@DataJpaTest`） |
| Util クラス | `{Util}Test.java` | 必須 |
| Entity スキーマ | スキーマ検証テスト | 必須（`@DataJpaTest`） |

### 5.2 スタブテスト検出

以下のパターンが存在する場合、「スタブテスト」として ❌ とする:

```java
// ❌ 空テスト
@Test
void test() {}

// ❌ アサーションなしテスト
@Test
void should_findUser() {
    userRepository.findByEmail("test@example.com");
    // assert なし
}

// ❌ assertTrue(true) のみ
@Test
void should_doSomething() {
    assertTrue(true);
}
```

```bash
# assertTrue(true) のみのテスト
grep -rn "assertTrue(true)" \
  appmod-migrated-java21-spring-boot-3rd/src/test/java/
```

✅ **正しいテスト例**:
```java
@Test
@DisplayName("有効な認証情報でログインした場合、HOME にリダイレクトされる")
void should_redirectToHome_when_validCredentialsProvided() throws Exception {
    // Arrange
    var user = createTestUser("{bcrypt}$2a$10$...", "ROLE_USER");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    // Act
    var result = mockMvc.perform(post("/auth/login")
        .param("email", "user@example.com")
        .param("password", "password123")
        .with(csrf()));

    // Assert
    result.andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/"));
}
```

### 5.3 テスト命名規約

```bash
# should_ パターンが使われているか
grep -rn "void should_" \
  appmod-migrated-java21-spring-boot-3rd/src/test/java/ | wc -l

# @DisplayName が付与されているか
grep -rn "@DisplayName" \
  appmod-migrated-java21-spring-boot-3rd/src/test/java/ | wc -l
```

### 5.4 テストカバレッジ基準

| カテゴリ | 目標カバレッジ | 測定ツール |
|---------|-------------|---------|
| Service 層 | ≥ 80% | JaCoCo |
| Controller 層 | ≥ 80%（`@WebMvcTest`） | JaCoCo |
| Repository 層 | ≥ 70%（`@DataJpaTest`） | JaCoCo |
| 全体 | ≥ 80% | JaCoCo |

```bash
# カバレッジ計測（Phase 8 のみ必須）
cd appmod-migrated-java21-spring-boot-3rd
mvn clean verify -Djacoco.skip=false
# レポート: target/site/jacoco/index.html
```

---

## 6. スタブ実装検出パターン

以下のパターンが本番コード（`src/main/java/`）に存在する場合、「手抜き実装」として ❌ とする:

| パターン | 説明 | 例 |
|---------|------|-----|
| 空のメソッドボディ | サービスメソッドが空 | `public void processOrder(Order o) {}` |
| `UnsupportedOperationException` | 実装未完了マーカー | `throw new UnsupportedOperationException("Not implemented")` |
| `return null`（コレクション型） | エラー時に `null` を返す | `List<Order> getOrders() { return null; }` |
| TODO のみコメント | ロジック未実装 | `// TODO: implement inventory check` |
| ハードコードされた本番データ | 固定値を業務データとして使用 | `private String adminEmail = "admin@skishop.com";` |

```bash
# throw new UnsupportedOperationException
grep -rn "UnsupportedOperationException" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/

# return null（コレクション型の可能性が高い箇所）
grep -rn "return null;" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/

# ハードコードされたメールアドレス（テスト・@Email アノテーション除外）
grep -rn '"[a-zA-Z0-9._%+-]*@[a-zA-Z0-9.-]*\.[a-zA-Z]{2,}"' \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/ \
  | grep -v "//\|test\|Test\|example\|@Email"
```
