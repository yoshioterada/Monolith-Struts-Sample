# フェーズ別検証ガイド

Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x 移行における、各フェーズ固有の
検証ポイントと重点確認事項を定義する。検証実施時はこのガイドとともに SKILL.md の
`フェーズ別重点確認事項` セクションを参照し、フェーズごとに特に注意すべき点を確認すること。

---

## Phase 0: 事前準備

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | JDK 21 がインストールされているか | `java -version` で `21.x.x` を確認 |
| 2 | Maven 3.9.x がインストールされているか | `mvn -version` で `3.9.x` を確認 |
| 3 | 現行アプリのベースライン動作記録が取得されているか | 全機能の動作確認メモが存在するか確認 |
| 4 | `docs/migration/DESIGN.md` と `docs/migration/PLAN.md` の読み込みが完了しているか | 設計書レビュー記録があるか確認 |
| 5 | `appmod-migrated-java21-spring-boot-3rd/` ディレクトリが作成されているか | `list_dir` で確認 |
| 6 | 現行 DB スキーマが `src/main/resources/db/schema.sql` に存在するか | `file_search` で確認 |

### 手抜き検出ポイント

- ベースライン記録が「動く」という記述のみで、具体的な機能一覧・エラーログがない
- JDK のインストール確認を省略して作業を開始している
- PLAN.md / DESIGN.md を読まずに Phase 1 に着手している

---

## Phase 1: プロジェクト基盤構築

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `pom.xml` に `spring-boot-starter-parent` バージョン `3.2.x` が設定されているか | `read_file` で pom.xml を確認 |
| 2 | `thymeleaf-extras-springsecurity6` が依存関係に含まれているか（`th:sec` 使用に必須） | `grep "springsecurity6" pom.xml` |
| 3 | `thymeleaf-layout-dialect` が含まれているか（Tiles 置換に必須） | `grep "layout-dialect" pom.xml` |
| 4 | `flyway-core` と `flyway-database-postgresql` が含まれているか | `grep "flyway" pom.xml` |
| 5 | `lombok` が `provided`/`optional` スコープで、かつ `annotationProcessorPaths` にも追加されているか | pom.xml の `<annotationProcessorPaths>` セクションを確認 |
| 6 | `net.logstash.logback:logstash-logback-encoder` がバージョン明示で追加されているか（Spring BOM 外のため） | `grep "logstash" pom.xml` |
| 7 | 禁止依存関係（`struts`, `log4j:1.x`, `commons-dbcp`, `javax.servlet.*`）が含まれていないか | pom.xml 全体を確認 |
| 8 | `application.properties` に `server.error.include-stacktrace=never` が設定されているか | `read_file` で確認 |
| 9 | `application.properties` に `spring.jpa.open-in-view=false` が設定されているか | `read_file` で確認 |
| 10 | `application-test.properties` に `spring.flyway.enabled=false` と H2 `MODE=PostgreSQL` が設定されているか | `read_file` で確認 |
| 11 | 秘密情報（`DB_PASSWORD` 等）が `application.properties` に直接記述されていないか | `grep -E "password\s*=\s*[^$\{]" src/main/resources/` |
| 12 | パッケージ構成が `com.skishop.{controller,service,repository,model,dto,config,security,util,exception}` になっているか | `list_dir` で確認 |

### 手抜き検出ポイント

- `spring-boot-starter-parent` のバージョンが 3.2.x ではなく古い 2.x
- `lombok` が `<scope>compile</scope>` のみで `annotationProcessorPaths` に追加されていない → コンパイル時に Lombok が機能しない
- `application-prod.properties` に本番 DB の実際のパスワードがハードコードされている
- `spring.jpa.open-in-view=true`（デフォルト値のまま）— View 層での N+1 を隠蔽する危険設定

---

## Phase 2: ドメインモデル移行

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 全 22 エンティティが存在するか | `find model/ -name "*.java" \| wc -l` |
| 2 | 全エンティティに `@Entity` と `@Table(name = "...")` が付与されているか | `grep -rL "@Entity" model/` |
| 3 | UUID PK に `@GeneratedValue` が **付与されていない**か（Service 層で生成） | `grep -rn "@GeneratedValue" model/` |
| 4 | `java.util.Date` が使われていないか（`java.time.*` を使用） | `grep -rn "java\.util\.Date" model/` |
| 5 | 全フィールドに `@Column(name = "...")` でカラム名が明示されているか | 各 Entity ファイルをサンプル確認 |
| 6 | `@OneToMany` に `cascade = ALL, orphanRemoval = true, fetch = LAZY` が設定されているか | `grep -rn "OneToMany" model/` |
| 7 | LAZY コレクション関連に `@BatchSize(size = 50)` が付与されているか（N+1 対策） | `grep -rn "@BatchSize" model/` |
| 8 | `@CreationTimestamp` と `@UpdateTimestamp` が `createdAt` / `updatedAt` に付与されているか | 各 Entity を確認 |
| 9 | `@DataJpaTest` でスキーマが H2 上で正常に作成されるか | `mvn test -Dtest="*EntityTest"` |

### 手抜き検出ポイント

- Entity の PK に `@GeneratedValue(strategy = GenerationType.UUID)` が付与されている → Service 層での UUID 生成という設計原則に違反
- `@Column` のカラム名指定がない → DB カラム名と Java フィールド名の命名規則が異なる場合にマッピング失敗
- `@ManyToOne` に `fetch = FetchType.EAGER` が設定されている → 全クエリで関連を JOIN FETCH する性能劣化
- `@BatchSize` が付与されていない → 一覧表示で N+1 クエリが発生する
- `LocalDateTime` ではなく `Date` 型でタイムスタンプを保持 → タイムゾーン問題

---

## Phase 3: リポジトリ層移行

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 全 20 リポジトリが `JpaRepository<Entity, String>` を extends しているか | `grep -rn "extends JpaRepository" repository/` |
| 2 | `ProductRepository` に `JpaSpecificationExecutor` が追加されているか（動的検索用） | `grep "JpaSpecificationExecutor" repository/ProductRepository.java` |
| 3 | `UserRepository` に `SecurityLog` のクエリが混在していないか（1 Repo = 1 Aggregate Root） | `grep -n "SecurityLog" repository/UserRepository.java` |
| 4 | `SecurityLogRepository` が独立して定義されているか | `find repository/ -name "SecurityLogRepository.java"` |
| 5 | 文字列結合 SQL が 1 件も存在しないか | `grep -rE '"(SELECT|UPDATE|INSERT|DELETE).*\+' repository/` |
| 6 | `@DataJpaTest` でのCRUD テストが全 Repository で通過するか | `mvn test -Dtest="*RepositoryTest"` |
| 7 | H2 テストに `MODE=PostgreSQL;NON_KEYWORDS=VALUE` が設定されているか | `application-test.properties` を確認 |
| 8 | PLAN.md §5 に定義された追加メソッドが全て実装されているか | PLAN.md §5 の表と実装を突合 |

### 手抜き検出ポイント

- Repository インターフェースではなく実装クラス（`UserRepositoryImpl`）を作成している → Spring Data JPA の設計に反する
- `@Query` アノテーションで JPQL ではなくネイティブ SQL を使用している → JPA の移植性が低下
- `@Query` の `:param` バインドを使わず文字列結合 SQL を記述 → SQLi 脆弱性
- `UserRepository` に `countByUserIdAndEventType`（SecurityLog 集計）を定義している → Aggregate Root 原則違反

---

## Phase 4: サービス層移行

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 全 14 Service に `@Service` アノテーションが付与されているか | `grep -rn "@Service" service/` |
| 2 | `new` による Service/Repository の生成が一切ないか | `grep -rE "= new.*Service\|= new.*Repository" service/` |
| 3 | DB 更新メソッドに `@Transactional` が付与されているか | 各 Service の更新メソッドを確認 |
| 4 | 読み取り専用メソッドに `@Transactional(readOnly = true)` が付与されているか | `grep -rn "readOnly = true" service/` |
| 5 | `CheckoutService.confirmOrder()` が 11 ステップすべてを単一 `@Transactional` で実行しているか | `service/CheckoutService.java` を精読 |
| 6 | `CartService` に `getOrCreateCart(HttpSession, String userId)` と `mergeSessionCart(String cartId, String userId)` が実装されているか | `grep -n "getOrCreateCart\|mergeSessionCart" service/CartService.java` |
| 7 | `BusinessException` が `redirectUrl` と `messageKey` フィールドを持つか | `exception/BusinessException.java` を確認 |
| 8 | `TaxService` が `AppConfig.getInstance()` ではなく `@ConfigurationProperties` または `@Value` で税率を取得しているか | `service/TaxService.java` を確認 |
| 9 | サービスユニットテスト（Mockito）が全件通過するか | `mvn test -Dtest="*ServiceTest"` |

### CheckoutService 11 ステップ確認

```bash
grep -n "checkStock\|reservePoints\|createOrder\|createOrderItems\|deductStock\|createPayment\|awardPoints\|clearCart\|emailQueue\|coupon\|discount" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/service/CheckoutService.java
```

11 ステップの原子化確認: ステップ 5 で例外が発生した場合、ステップ 1〜4 がロールバックされることを `CheckoutServiceTest` で検証すること。

### 手抜き検出ポイント

- `CheckoutService` が `@Transactional` なしに 11 ステップのメソッド呼び出しを行っている → 部分コミットで DB 不整合
- `CartService.mergeSessionCart()` が未実装 → ゲストカートとログイン後カートが統合されない
- `BusinessException` に `redirectUrl` がなく、Controller 側でハードコードしている
- `PointService.reservePoints()` と `awardPoints()` が同一メソッドに統合されている → トランザクション設計の欠陥

---

## Phase 5: Web 層移行（Controller + DTO）

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 29 Action が 8 Controller に集約されているか（DESIGN.md §2.3 参照） | `find controller/ -name "*Controller.java" \| wc -l` |
| 2 | 全 Controller に `@Controller` + `@RequestMapping` が付与されているか | `grep -rn "@Controller\|@RequestMapping" controller/` |
| 3 | 全リクエスト DTO 引数に `@Valid` が付与されているか | `grep -rn "@ModelAttribute\|@RequestBody" controller/ \| grep -v "@Valid"` |
| 4 | URL パターンから `*.do` が完全に排除されているか | `grep -rn '\.do"' src/` |
| 5 | 12 ActionForm が全て Bean Validation 付き `record` クラスに変換されているか | `find dto/request/ -name "*.java" \| xargs grep -l "^public record" \| wc -l` |
| 6 | `OrderController` と `AccountController` に IDOR 防止（`@AuthenticationPrincipal` + オーナーシップ検証）が実装されているか | コントローラー内の `findByIdAndUserId` 等を確認 |
| 7 | `RedirectAttributes` が `redirect:` の後の遷移に使われているか（PRG パターン） | `grep -rn "RedirectAttributes\|redirect:" controller/` |
| 8 | Admin Controller メソッドに `@PreAuthorize("hasRole('ADMIN')")` が付与されているか | `grep -rn "@PreAuthorize" controller/Admin*` |
| 9 | `@WebMvcTest` でのテストが全件通過するか | `mvn test -Dtest="*ControllerTest"` |

### 手抜き検出ポイント

- `@RestController` を誤って使用している → 画面遷移ではなく JSON レスポンスを返す
- POST のフォーム送信後に `return "checkout/confirm"` と直接 View 名を返している → ブラウザバックで二重送信
- `@Valid` がなく、バリデーション未実行のまま Service を呼んでいる
- `OrderController` が `orderService.findById(orderId)` でユーザー ID チェックなしに取得 → IDOR 脆弱性

---

## Phase 6: ビュー層移行（Thymeleaf）

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `templates/fragments/layout.html` が存在し `layout:fragment="content"` を持つか | `file_search` + `read_file` で確認 |
| 2 | 全ページテンプレートで `layout:decorate="~{fragments/layout}"` を使用しているか | `grep -rn "layout:decorate" templates/ \| wc -l` と比較 |
| 3 | `th:utext` の使用箇所がゼロか（XSS 対策） | `grep -rn "th:utext" templates/` |
| 4 | POST フォームが全て `th:action="@{/...}"` を使用しているか（CSRF 自動挿入） | `grep -rn 'method="post"' templates/ \| grep -v "th:action"` |
| 5 | `th:href="@{...}"` を使用し URL をハードコードしていないか | `grep -rn 'href="/' templates/ \| grep -v "th:href"` |
| 6 | ロールベース表示が `th:if="${#authorization.expression('hasRole(...)') }"` or `th:sec:authorize` で実装されているか | `grep -rn "th:sec\|hasRole\|#authorization" templates/admin/` |
| 7 | 統合テストで全画面 HTTP 200 が確認できるか | Spring Boot Test でのエンドポイントテスト |
| 8 | 静的リソース（CSS/JS/画像）が `src/main/resources/static/` に配置されているか | `list_dir` で確認 |
| 9 | JSP タグ（`<html:form>`, `<logic:iterate>` 等）が全テンプレートから排除されているか | `grep -rn "<html:\|<logic:\|<bean:\|<%@" templates/` |

### 重点確認: Tiles → Layout Dialect 変換

| 確認ポイント | 期待される実装 | 確認コマンド |
|------------|-------------|-----------|
| Tiles `base.layout` 相当 | `fragments/layout.html` の `layout:fragment="content"` | `grep -n "layout:fragment" templates/fragments/layout.html` |
| 各ページの extends | `layout:decorate="~{fragments/layout}"` | `grep -rn "layout:decorate" templates/` |
| ヘッダー・フッターの include | `th:replace="~{fragments/header :: header}"` | `grep -rn "th:replace" templates/fragments/layout.html` |
| セキュリティ統合 | `th:sec:authorize="hasRole('USER')"` でメニュー制御 | `grep -rn "th:sec" templates/` |

### 手抜き検出ポイント

- 一部ページが `layout:decorate` を使わず独立した HTML ファイルとして実装されている → ヘッダー/フッターの二重実装
- `th:text` の代わりに直接テキストをハードコードし Thymeleaf が機能していない箇所がある
- `<form action="/checkout/confirm" method="post">` のように `th:action` を使わず URL をハードコード → CSRF トークンが挿入されない
- エラーメッセージを `th:text="${errorMessage}"` で表示しているが、`th:errors="*{fieldName}"` を使っていない → フォームバリデーションエラーが表示されない

---

## Phase 7: セキュリティ統合

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `SecurityConfig` に `@EnableWebSecurity` と `@EnableMethodSecurity` が付与されているか | `grep -n "@EnableWebSecurity\|@EnableMethodSecurity" config/SecurityConfig.java` |
| 2 | URL 認可設定が `/admin/**` → ADMIN のみ、`/account/**` → USER 以上、`/actuator/**` → ADMIN のみ（`/health`, `/info` 除く）になっているか | `read_file` で SecurityConfig の `authorizeHttpRequests()` を確認 |
| 3 | `sessionFixation().migrateSession()` が設定されているか（セッション固定攻撃対策） | `grep -n "sessionFixation" config/SecurityConfig.java` |
| 4 | CSRF 保護が `csrf(Customizer.withDefaults())` で有効になっているか | `grep -n "csrf" config/SecurityConfig.java` |
| 5 | `xssProtection` が `Customizer.withDefaults()` を使用しているか（`xss.enable()` 非推奨） | `grep -n "xssProtection\|xss\." config/SecurityConfig.java` |
| 6 | HSTS が `includeSubDomains(true).maxAgeInSeconds(31536000)` で設定されているか | `grep -n "httpStrict\|includeSubDomains" config/SecurityConfig.java` |
| 7 | `LegacySha256PasswordEncoder.matches()` が `{sha256}<hash>$<salt>` 形式をパースしているか | `read_file` で `util/LegacySha256PasswordEncoder.java` を確認 |
| 8 | `CustomUserDetailsService` が `UserDetailsService` **と** `UserDetailsPasswordService` の両方を implements しているか | `grep -n "implements" security/CustomUserDetailsService.java` |
| 9 | `CartMergeSuccessHandler` が `AuthenticationSuccessHandler` を implements し、ログイン後カートマージを行うか | `find security/ -name "CartMergeSuccessHandler.java"` |
| 10 | Flyway V2 SQL で `CONCAT('{sha256}', password_hash, '$', salt)` によるプレフィックス付与が行われているか | `read_file` で `db/migration/V2__*.sql` を確認 |
| 11 | 未認証ユーザーが `/account/**` にアクセスした場合に `/auth/login` にリダイレクトされるか | Spring Security Test で確認 |
| 12 | SHA-256 ハッシュのユーザーがログインでき、BCrypt に自動アップグレードされるか | `LegacySha256PasswordEncoderTest` で検証 |

### 重点確認: DelegatingPasswordEncoder 構成

```java
// 以下の構成になっているか確認
@Bean
public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("sha256", new LegacySha256PasswordEncoder());
    return new DelegatingPasswordEncoder("bcrypt", encoders);
}
```

```bash
grep -n "DelegatingPasswordEncoder\|LegacySha256\|bcrypt\|sha256" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/config/SecurityConfig.java
```

### 手抜き検出ポイント

- `xssProtection` で非推奨の `.enable()` を使用 → Spring Security 非推奨 API 警告
- `UserDetailsPasswordService` を implements していない → BCrypt 自動アップグレードが機能しない
- Flyway V2 SQL が未作成または不正確 → SHA-256 ユーザーがログイン不可
- `CartMergeSuccessHandler` が未定義で、ログイン後もゲストカートが失われる
- `formLogin().loginProcessingUrl("/login")` が `th:action="@{/auth/login}"` と不一致 → ログイン POST が 404

---

## Phase 8: テスト実装・品質確認

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 全テストが `mvn clean test` で 100% 通過するか | `mvn clean test` の出力を確認 |
| 2 | `CheckoutServiceTest` が 11 ステップ全てのロールバック検証を含むか | テストファイルのメソッド一覧を確認 |
| 3 | `LegacySha256PasswordEncoderTest` が既知の SHA-256 ハッシュ + ソルトで `matches()` を検証しているか | テストの `@Test` メソッドを確認 |
| 4 | カートマージ統合テスト（未ログインカート → ログイン → カートマージ）が存在するか | `grep -rn "mergeSessionCart\|CartMerge" test/` |
| 5 | JaCoCo で Service 80%+・全体 80%+ が達成されているか | `mvn clean verify -Djacoco.skip=false` 後 `target/site/jacoco/index.html` を確認 |
| 6 | 全テストメソッドが `should_期待結果_when_条件` 命名規約に従っているか | `grep -rn "void test\|void check\|void verify" test/ \| grep -v "should_"` |
| 7 | スタブテスト（`assertTrue(true)` のみ等）が存在しないか | `grep -rn "assertTrue(true)\|assume\|todo" test/` |
| 8 | `@DataJpaTest` テストが H2 `MODE=PostgreSQL` で実行されているか | `application-test.properties` の DB URL を確認 |
| 9 | セキュリティテスト（`@WithMockUser`）が認証・認可シナリオをカバーしているか | `grep -rn "@WithMockUser\|@WithAnonymousUser" test/` |

### CheckoutService ロールバック検証のチェック

```bash
# 11 ステップ分のロールバックテストが存在するか（最低でも ステップ 1, 5, 8, 11 で例外発生テスト）
grep -n "should.*when.*throws\|should.*when.*fail\|rollback" \
  appmod-migrated-java21-spring-boot-3rd/src/test/java/com/skishop/service/CheckoutServiceTest.java
```

### 手抜き検出ポイント

- `CheckoutServiceTest` が「基本的な注文確定フロー」のみ検証し、ロールバックテストがない
- テストで `@TestConfiguration` を使わずに実際の DB（PostgreSQL）に接続している
- `@WebMvcTest` を使わず `@SpringBootTest` を Controller テストに使用している → テストが重くなる
- カバレッジレポートを生成していない（`-Djacoco.skip=false` を実行していない）

---

## Phase 9: 最終検証・リリース準備

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `mvn clean verify` が全テストを含めて成功するか | `mvn clean verify` の出力を確認 |
| 2 | Dockerfile がマルチステージビルド（JDK ビルド → JRE 実行）になっているか | `read_file` で Dockerfile を確認 |
| 3 | Dockerfile の実行ユーザーが非 root か | `grep -n "USER\|useradd\|groupadd" Dockerfile` |
| 4 | Dockerfile に `HEALTHCHECK` が設定されているか | `grep -n "HEALTHCHECK" Dockerfile` |
| 5 | JVM メモリが `-Xmx` 固定値ではなく `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` か | `grep -n "UseContainerSupport\|MaxRAMPercentage" Dockerfile` |
| 6 | `.dockerignore` で `target/`, `.git/`, `*.md` が除外されているか | `read_file` で `.dockerignore` を確認 |
| 7 | `application-prod.properties` の全秘密情報が `${ENV_VAR}` 形式か | `grep -E "password\s*=\s*[^$\{]" src/main/resources/application-prod.properties` |
| 8 | OWASP Dependency Check で Critical CVE がゼロか | `mvn dependency-check:check` |
| 9 | Docker イメージがビルドできるか | `docker build -t skishop-app .` |
| 10 | `spring.profiles.active=prod` でアプリが起動し `/actuator/health` が `UP` を返すか | `curl http://localhost:8080/actuator/health` |
| 11 | `README.md` に起動方法・必須環境変数一覧（`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST` 等）が記載されているか | `read_file` で README.md を確認 |

### Dockerfile チェックリスト

```dockerfile
# ✅ 期待される構成（下記を確認）
FROM eclipse-temurin:21-jdk AS build    # マルチステージ: JDK でビルド
FROM eclipse-temurin:21-jre             # JRE のみの実行イメージ
RUN groupadd -r skishop && useradd -r -g skishop skishop  # 非 root ユーザー
USER skishop                            # 非 root で実行
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/actuator/health || exit 1
```

```bash
# ベースイメージが latest タグでないか確認（固定バージョン必須）
grep -n "FROM" appmod-migrated-java21-spring-boot-3rd/Dockerfile | grep "latest"
```

### 手抜き検出ポイント

- Dockerfile がシングルステージビルド（JDK イメージがそのままランタイムに使用されている）→ イメージサイズ膨大
- `USER root` のまま（非 root ユーザーへの切り替えなし）→ セキュリティリスク
- `-Xmx512m` のようなメモリ固定値 → コンテナのメモリリミットを活用できない
- `.dockerignore` が未作成 → `target/` が全てイメージに含まれ巨大なイメージが生成される
- `application-prod.properties` に `spring.datasource.password=password` のような直書き → Critical セキュリティ違反

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `frontend/package.json` に `front-end-need.md` §1.5 の全パッケージが記載されているか | `read_file` で package.json を確認し、§1.5 のバージョン表と照合 |
| 2 | TypeScript `strict` モードが有効か | `tsconfig.json` の `"strict": true` を確認 |
| 3 | ディレクトリ構成が `front-end-impl-plan.md` P0-4 に一致するか | `list_dir` で各ディレクトリの存在を確認 |
| 4 | `.env.example` に全必要環境変数が記載されているか | P0-5 の変数リストと照合 |
| 5 | `.gitignore` に `.env.local` が含まれているか | `grep_search` で確認 |
| 6 | shadcn/ui の初期コンポーネントが追加されているか | `src/components/ui/` 配下のファイル数を確認 |
| 7 | `orval.config.ts` が存在し、9 サービスの設定があるか | ファイル内容を確認 |
| 8 | `vitest.config.ts` が存在し、React Testing Library が統合されているか | 設定ファイルを確認 |
| 9 | ESLint + Prettier の設定がプロジェクトルールに沿っているか | 設定ファイルを確認 |

### 手抜き検出ポイント

- `package.json` にパッケージが記載されているが、実際に `node_modules` にインストールされていない（`package-lock.json` に反映されているか確認）
- ディレクトリは存在するが中身が完全に空で `.gitkeep` すらない
- `lib/env.ts` が存在するが、zod バリデーションが実装されておらず単なる `process.env` の再 export

---

## Phase 1: 共通インフラストラクチャ

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | `src/lib/http.ts` がタイムアウト・X-Request-Id を実装しているか | fetch ラッパの実装コードを確認 |
| 2 | `src/lib/api-client.ts` が `API_BASE_URL` 環境変数を使用しているか | ハードコード URL がないことを grep で確認 |
| 3 | `src/types/api/` に §6.1 の全 DTO 型が定義されているか | ファイル一覧と型名を確認 |
| 4 | 認証フロー（ログイン→Cookie→リフレッシュ→ログアウト）が実装されているか | BFF Route Handler と zustand ストアを確認 |
| 5 | RFC 7807 `ProblemDetail` 型とステータス別分岐が実装されているか | `error-handler.ts` の分岐ロジックを確認 |
| 6 | EC レイアウト（ヘッダー・フッター）がski-shop.pngデザインに準拠しているか | コンポーネントの JSX 構造を確認 |
| 7 | Admin サイドバーにナビゲーション全項目が含まれているか | ナビ項目リストを §3.1 画面一覧と照合 |
| 8 | ページネーションコンポーネントが `PagedModel` 形式（§4.2.2）に対応しているか | `page.number`, `page.totalPages` 等のフィールド名を確認 |
| 9 | 縮退 UI コンポーネントが §4.2.3 の仕様通りに実装されているか | セクション非表示・バックグラウンドリトライのロジックを確認 |
| 10 | React Query Provider が正しいキャッシュ戦略（§4.5）を設定しているか | `staleTime`, `gcTime` の設定値を確認 |
| 11 | i18n ヘルパー `t()` が翻訳キー方式で動作するか | `src/lib/i18n.ts` と `locales/ja.json` を確認 |
| 12 | 通貨フォーマットが `Intl.NumberFormat` を使用しているか | `src/lib/format.ts` を確認 |
| 13 | Sentry / OpenTelemetry の初期化が正しいか | 設定ファイルを確認 |
| 14 | ミドルウェアが保護パスを正しくガードしているか | `middleware.ts` のパスマッチングを確認 |

### 手抜き検出ポイント

- `error-handler.ts` が全ステータスコード（400/401/403/404/422/429/500/503）を処理していない（一部のみ実装）
- 型定義が存在するが、フィールドが `front-end-need.md` §6.1 と一致しない（フィールド名の欠落や型の不一致）
- トークンリフレッシュのタイマー（`expiresAt - 5分前`）が実装されていない
- 401 受信時のリフレッシュ→リトライ→キューイングが省略されている
- ページネーションコンポーネントが EC 用（無限スクロール）と Admin 用（テーブル）の両方を提供していない
- カートの楽観的更新が実装されていない（§4.5 の楽観的更新テーブル参照）

### 型定義の完全性チェック

以下の型ファイルが `src/types/api/` に存在し、§6.1 の全フィールドを含むこと:

| ファイル | 必須型名 | 参照セクション |
|---------|---------|-------------|
| `auth.ts` | `LoginRequest`, `RegisterRequest`, `AuthResponse` | §6.1.1 |
| `user.ts` | `UserResponse` | §6.1.2 |
| `product.ts` | `ProductResponse`, `CategoryResponse` | §6.1.3 |
| `order.ts` | `CreateOrderRequest`, `OrderItemRequest`, `OrderResponse`, `OrderItemResponse` | §6.1.4 |
| `cart.ts` | `CartResponse`, `CartItemResponse`, `CreatePaymentIntentRequest`, `PaymentResponse` | §6.1.5 |
| `point.ts` | `UserTierResponse`, `PointTransactionResponse` | §6.1.6 |
| `coupon.ts` | `CampaignResponse`, `CouponResponse` | §6.1.7 |
| `ai.ts` | `ChatMessageResponse`, `RecommendationResponse`, `ProductRecommendation`, `SearchResponse`, `SearchResult` | §6.1.8 |
| `common.ts` | `PaginatedResponse<T>`, `ProblemDetail` | §4.2.1, §4.2.2 |

---

## Phase 2: EC サイト — 商品閲覧

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | EC-HOME のヒーローバナーがグラデーション背景 + CTA 2 つで構成されているか | JSX を確認 |
| 2 | EC-HOME のカテゴリセクションが 4 列グリッドで表示されるか | Tailwind クラスを確認（`grid-cols-4` 等） |
| 3 | EC-HOME のシーズン切替（10月〜3月/4月〜9月）が実装されているか | 月判定ロジックを確認 |
| 4 | EC-HOME の API 呼び出しが `Promise.allSettled` で並列化されているか | BFF Route Handler を確認 |
| 5 | EC-HOME の縮退対応（各 API 障害時にセクション非表示）が実装されているか | エラーハンドリング分岐を確認 |
| 6 | EC-CATALOG の無限スクロールが Intersection Observer で実装されているか | hook / コンポーネントを確認 |
| 7 | EC-CATALOG の URL パラメータ連動（カテゴリ・ソート）が実装されているか | `useSearchParams` の使用を確認 |
| 8 | EC-DETAIL の在庫表示（在庫あり/残りわずか≤5/在庫切れ=0）が正しいか | 条件分岐を確認 |
| 9 | EC-DETAIL のカート追加で未ログイン時にリダイレクトされるか | 認証チェックを確認 |
| 10 | EC-SEARCH のオートコンプリートが debounce 300ms で実装されているか | debounce 設定を確認 |
| 11 | 商品カード（`product-card.tsx`）がセール価格対応しているか | `salePrice` 表示ロジックを確認 |
| 12 | レスポンシブデザイン（375px/768px/1280px）が実装されているか | Tailwind のレスポンシブクラスを確認 |

### 手抜き検出ポイント

- EC-HOME のトレンド商品がハードコードされたダミーデータで表示されている
- カテゴリカードの画像が静的な placeholder.png のまま
- 無限スクロールが実装されておらず「もっと見る」ボタンのみ
- EC-SEARCH のフィードバック機能（FR-SRCH-04）が省略されている
- EC-SEARCH の 0 件時代替表示（FR-SRCH-05）が未実装
- EC-HOME のオフシーズン訴求セクション（FR-HOME-08）が省略されている

---

## Phase 3: EC サイト — 認証フロー

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | ログインフォームが react-hook-form + zod で実装されているか | import と useForm 設定を確認 |
| 2 | リアルタイムバリデーション（メール形式、パスワード強度）が実装されているか | zod スキーマとフォームの連携を確認 |
| 3 | ログイン成功後に `?redirect` パラメータの遷移先にリダイレクトされるか | リダイレクトロジックを確認 |
| 4 | 登録フォームのパスワード強度インジケーターが実装されているか | コンポーネントを確認 |
| 5 | `EMAIL_ALREADY_EXISTS` エラーの専用メッセージが実装されているか | errorCode 分岐を確認 |
| 6 | EC-VERIFY でトークン処理（`POST /users/verify-email` + `{ "token" }` ボディ）が正しいか | BFF Route Handler を確認 |
| 7 | EC-PW-RESET がリセット要求と新パスワード設定の 2 画面を `?token=` で切り替えるか | URL パラメータ判定を確認 |
| 8 | ログアウト時に Cookie セッション削除 + ストアクリアが行われるか | ログアウト処理を確認 |
| 9 | トークンが localStorage/sessionStorage に保存されていないか | `grep_search` で確認 |

### 手抜き検出ポイント

- バリデーションが zod ではなく HTML の `required` 属性のみ
- パスワード強度インジケーターが見た目だけで、実際の強度判定ロジックがない
- EC-VERIFY がトークン検証 API を呼ばず、常に「成功」を表示
- EC-PW-RESET の新パスワード設定画面で確認入力（パスワード再入力）がない

---

## Phase 4: EC サイト — 購入フロー

### 重点確認事項（最重要フェーズ）

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | カートの楽観的更新（数量変更・削除 → ロールバック）が実装されているか | React Query useMutation の `onMutate`/`onError` を確認 |
| 2 | クーポン適用は楽観的更新ではなく、バリデーション結果待ちか | クーポン適用のミューテーション処理を確認 |
| 3 | `Idempotency-Key` ヘッダーが `crypto.randomUUID()` で生成されているか | 決済 BFF Handler を確認 |
| 4 | 各決済 API ごとに **独立した** Idempotency-Key が生成されているか | intent/process/orders の各呼び出しを確認 |
| 5 | 「注文確定」ボタン押下で即座に disabled + ローディングが設定されるか | ボタンの状態管理を確認 |
| 6 | `beforeunload` イベントで画面離脱警告が設定されているか | イベントリスナーを確認 |
| 7 | 決済フロー Step 3→4→5 が **直列実行** されているか（並列にしていないか） | async/await チェーンを確認 |
| 8 | タイムアウト 30 秒 + ポーリング（3 秒×10 回）が実装されているか | タイマー設定を確認 |
| 9 | `history.replaceState()` による履歴置換が注文完了画面で実行されるか | ブラウザバック防止ロジックを確認 |
| 10 | ネットワークエラー時に同一 Idempotency-Key でリトライされるか | リトライロジックを確認 |
| 11 | ヘッダーカートバッジがアイテム数を正しく反映するか | React Query キャッシュ連携を確認 |
| 12 | `userId` が JWT の sub claim から取得され、ハードコードされていないか | BFF での userId 解決を確認 |

### 手抜き検出ポイント（セキュリティ最重要）

- Idempotency-Key が固定値やタイムスタンプで生成されている（UUID v4 でないと冪等性が保証されない）
- 決済フロー（intent→process→orders）が並列実行されている
- タイムアウト・ポーリングが未実装で、エラー時に即座に失敗画面を表示するだけ
- `beforeunload` 警告が未設定
- `history.replaceState` が未実装でブラウザバックでチェックアウトに戻れる
- 空カート時の導線（「カートが空です」+ 商品一覧リンク）が未実装

---

## Phase 5: EC サイト — マイページ

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 注文一覧のステータスバッジが色分けされているか（PENDING:黄, CONFIRMED:青, SHIPPED:紫, DELIVERED:緑, CANCELLED:赤） | バッジコンポーネントの色設定を確認 |
| 2 | 注文詳細のキャンセルボタンが PENDING/CONFIRMED のみ表示されるか | 条件分岐を確認 |
| 3 | キャンセル・アカウント削除に確認ダイアログが表示されるか | ダイアログ呼び出しを確認 |
| 4 | メール内リンク（`/mypage/orders/{orderId}`）からの遷移で未認証時にリダイレクトされるか | ミドルウェアの動作を確認 |
| 5 | プロフィールのレコメンド履歴セクションにプレースホルダが配置されているか（Phase 7 で接続） | コンポーネント構造を確認 |

---

## Phase 6: 管理画面 — コア機能

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | ADM-DASH の API 呼び出しが `Promise.allSettled` で並列化されているか | BFF Route Handler を確認 |
| 2 | ADM-DASH で部分障害時に該当カードのみ「データ取得不可」を表示するか | 縮退ロジックを確認 |
| 3 | テーブルページネーションが §4.2.2 準拠（件数表示 + ページサイズ切替 20/50/100）か | コンポーネントを確認 |
| 4 | 破壊的操作（削除、キャンセル、返金、ステータス変更）に確認ダイアログがあるか | 全操作ボタンを確認 |
| 5 | MANAGER ロールで ADMIN 専用機能が非表示になるか | ロールチェックを確認 |
| 6 | 商品管理の価格更新で通常価格 + セール価格が設定可能か | フォームフィールドを確認 |
| 7 | カテゴリ削除で `CAT_HAS_CHILDREN` エラーが処理されるか | errorCode 分岐を確認 |

---

## Phase 7: EC サイト — ロイヤルティ & AI

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | ティアプログレスバーが次ティアまでの進捗を視覚化しているか | コンポーネントを確認 |
| 2 | 失効予定ポイント警告が 30 日以内に限定されているか | 条件判定を確認 |
| 3 | AI チャットウィジェットが全 EC 画面からフローティングで起動可能か | レイアウトへの配置を確認 |
| 4 | AI レスポンス内の商品名→商品詳細リンクの自動生成が実装されているか | パーサーロジックを確認 |
| 5 | ai-support-service 障害時のフォールバック UI が実装されているか | 縮退動作を確認 |
| 6 | セマンティック検索のフォールバック（通常検索）が実装されているか | フォールバックロジックを確認 |
| 7 | ポイント移行で `PNT-4224`（自己送金）エラーが処理されるか | errorCode 分岐を確認 |

---

## Phase 8: 管理画面 — 拡張機能

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 在庫操作（入庫/出庫/予約/解放）の全操作に確認ダイアログがあるか | 各操作のイベントハンドラを確認 |
| 2 | 低在庫アラートの閾値がカスタマイズ可能か | パラメータ設定を確認 |
| 3 | キャンペーンエラー（`CMP-4221`, `CMP-4222`）が適切に処理されるか | errorCode 分岐を確認 |
| 4 | メール送信履歴の失敗メールに赤バッジが表示されるか | ステータス別スタイルを確認 |
| 5 | リトライ前に確認ダイアログが表示されるか | ダイアログ呼び出しを確認 |
| 6 | メール統計で過去 7 日間の成功率グラフが表示されるか | グラフコンポーネントを確認 |

---

## Phase 9: 管理画面 — 分析 & AI

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | 分析画面の全 7 項目（FR-AANA-01〜07）が実装されているか | コンポーネント一覧を確認 |
| 2 | AI モデルデプロイに確認ダイアログがあるか | 操作フローを確認 |
| 3 | 全分析/AI 画面が ADMIN/MANAGER ロール限定か | アクセス制御を確認 |

---

## Phase 10: 品質向上 & 仕上げ

### 重点確認事項

| # | 確認項目 | 確認方法 |
|---|---------|---------|
| 1 | ダークモードが管理画面の全コンポーネントで正常表示されるか | `dark:` プレフィックスの使用を確認 |
| 2 | 画像に `next/image` + `priority`/`sizes` が設定されているか | ヒーローバナー等の画像コンポーネントを確認 |
| 3 | アクセシビリティ（`aria-label`, `role`, フォーカスリング）が設定されているか | インタラクティブ要素を確認 |
| 4 | SSR が EC 公開画面で使用されているか | サーバーコンポーネントの使用を確認 |
| 5 | JSON-LD 構造化データが商品詳細ページに含まれているか | `generateMetadata` / `<script type="application/ld+json">` を確認 |
| 6 | `sitemap.xml` / `robots.txt` が `public/` に存在するか | ファイル存在を確認 |
| 7 | 429 レート制限ハンドリングが API クライアントに実装されているか | interceptor を確認 |
| 8 | E2E テストが Playwright で主要フロー（購入/認証/管理）をカバーしているか | テストファイルを確認 |
| 9 | ハードコード日本語文字列が全画面で翻訳キー方式に置き換えられているか | grep で日本語リテラルを検索 |
