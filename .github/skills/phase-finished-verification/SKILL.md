---
name: phase-finished-verification
description: "Java 移行フェーズの完了検証を実行。各フェーズの修了条件チェックリストに基づき、間違い・抜け漏れ・手抜きがないかを網羅的に検証する。Use when: 各フェーズが終了後、実装品質チェック、抜け漏れ検証、移行実装の検収"
argument-hint: "Phase 番号（0〜9）を指定。例: Phase 0, Phase 3"
---

# phase-finished-verification — Java 移行フェーズ完了検証 Skill

## 目的

`docs/migration/PLAN.md` に定義された各フェーズ（Phase 0〜9）の移行実装が完了した際に、以下の 3 観点で **網羅的かつ厳格に** 検証を実施する:

1. **修了条件チェックリスト完全充足**: 各フェーズの修了条件チェックリストの全項目が対応済みか
2. **設計書準拠（抜け漏れ検出）**: `docs/migration/DESIGN.md` の該当セクションに記載された設計・変換仕様が全て実装に反映されているか
3. **実装品質（手抜き検出）**: コードが TODO 放置・スタブ実装・禁止パターン使用等の手抜きなく、本番品質で実装されているか

**Fail-Safe 原則**: 判定に迷う場合は「不合格（要修正）」に倒す。最終判断は人間が行う。

## 前提条件

- 検証対象の Phase 番号（0〜9）が指定されていること
- `docs/migration/PLAN.md` が存在し、対象フェーズのセクションが読み取り可能であること
- `docs/migration/DESIGN.md`（詳細設計書）が存在すること
- 移行先ディレクトリ `appmod-migrated-java21-spring-boot-3rd/` に実装コードが存在すること
- 前提フェーズが完了済みであること（フェーズ依存関係は PLAN.md §1 参照）

> **参照ファイル（同ディレクトリ内）**: 検証実施時は以下の補足資料を活用すること
> - `references/phase-verification-guide.md` — フェーズ別の詳細確認手順・重点チェックリスト
> - `references/quality-check-patterns.md` — 品質チェックパターン集・スタブ実装検出方法
> - `references/requirements-traceability.md` — 移行要件トレーサビリティマトリクス（Struts → Spring Boot）

## 手順

以下の 6 ステップで検証を実施する。**各ステップで問題を発見した場合も、全ステップを完了してから結果をまとめて報告すること**（途中で打ち切らない）。

---

### Step 1: フェーズ情報の読み取り

1. `docs/migration/PLAN.md` から対象フェーズのセクション全体を読み取る
2. 以下の情報を抽出する:
   - フェーズの **目的**
   - **作業項目**（1-1, 2-A, 3-1 等の全タスク）
   - **検証コマンド**（`mvn clean compile` / `mvn test` 等）
   - **修了条件チェックリスト**（全 [ ] 項目）
3. `docs/migration/DESIGN.md` から対象フェーズに関連するセクションを特定・読み取る

---

### Step 2: 修了条件チェックリストの全項目検証

対象フェーズの修了条件チェックリストの **全項目** について以下を実施する:

1. **ファイル存在確認**: チェック項目が言及するファイル・クラスが `appmod-migrated-java21-spring-boot-3rd/src/` 配下に存在するか（`file_search` / `grep_search` で確認）
2. **実装内容確認**: ファイルの内容がチェック項目の要求を満たしているか（`read_file` で確認）
3. **コンパイル・テスト結果確認**: 検証コマンド（`mvn clean compile`, `mvn test` 等）の結果がパスしているか確認
4. 各項目の結果を ✅ PASS / ❌ FAIL / ⚠️ PARTIAL で記録する

> **重要**: 1 項目でも FAIL がある場合、フェーズは **不合格** とする。PARTIAL が 3 件以上ある場合も不合格。

#### フェーズ別 修了条件一覧（PLAN.md より）

| Phase | 主要修了条件 |
|-------|------------|
| **Phase 0** | 現行アプリのベースライン動作確認完了 / JDK 21 + Maven 3.9 インストール済み / 設計書・計画書レビュー完了 |
| **Phase 1** | `mvn clean compile` 成功 / `SkiShopApplication` 起動確認 / `application.properties` に機密情報が直接記述されていない |
| **Phase 2** | 全 22 エンティティのコンパイル成功 / `@DataJpaTest` でスキーマ検証通過 / `java.util.Date` 不使用 / 全フィールドに `@Column` でカラム名明示 |
| **Phase 3** | 全 20 リポジトリのコンパイル成功 / `@DataJpaTest` での CRUD テスト通過 / 文字列結合 SQL が存在しない |
| **Phase 4** | `new` による依存性生成が存在しない / 全サービスに `@Service` 付与 / DB 更新全メソッドに `@Transactional` 付与 / 読み取りメソッドに `@Transactional(readOnly = true)` 付与 / サービスユニットテスト全通過 |
| **Phase 5** | 全 Controller コンパイル成功 / `@Valid` が全リクエスト DTO 引数に付与 / URL パターンから `*.do` が排除 / `@WebMvcTest` でのテスト通過 / `RedirectAttributes` が正しく実装 |
| **Phase 6** | 全テンプレートの Thymeleaf パース成功 / 統合テストで全画面 HTTP 200 確認 / XSS テスト通過 / CSRF テスト通過 / 静的リソース配信確認 |
| **Phase 7** | 未認証ユーザーのリダイレクト確認 / USER ロールが ADMIN URL に 403 / CSRF トークン動作確認 / セッション固定攻撃テスト / ブルートフォーステスト / SHA-256 ユーザーがログイン可能 / BCrypt 自動アップグレード確認 / Spring Security Test 全通過 |
| **Phase 8** | `mvn clean test` が 100% 通過 / カバレッジ目標達成（Service 80%+・全体 80%+） / セキュリティテスト全件通過 |
| **Phase 9** | `mvn clean verify` 成功 / Docker イメージのビルドと起動成功 / 本番プロファイルでの起動成功 / OWASP Dependency Check で Critical CVE なし / README に起動情報が記載 |

---

### Step 3: 設計書要件との突合せ（抜け漏れ検出）

`docs/migration/DESIGN.md` から対象フェーズに関連するセクションを読み取り、設計どおりに実装されているかを確認する。

#### フェーズ別 DESIGN.md 参照セクション

| Phase | 確認すべき DESIGN.md セクション |
|-------|-------------------------------|
| Phase 1 | §3.1 技術スタック / §5.1 パッケージ構成 / §12 設定ファイル移行設計 |
| Phase 2 | §8.4 JPA エンティティ設計原則 / §9 ドメインモデル移行設計 |
| Phase 3 | §8.1〜8.3 Repository 設計 / §8.5 N+1 対策 |
| Phase 4 | §7 サービス層移行設計 / §7.3 CheckoutService 11 ステップ / §6.6 カートセッション管理 |
| Phase 5 | §6 Web 層移行設計 / §6.2 DTO 変換ルール / §6.5 URL 設計 |
| Phase 6 | §10 ビュー層移行設計 / §10.3 JSP→Thymeleaf マッピング / §10.5 メールテンプレート |
| Phase 7 | §11.1 SecurityConfig / §11.2 パスワードハッシュ移行 / §6.6 CartMergeSuccessHandler |
| Phase 8 | §13 テスト戦略 |
| Phase 9 | §14 非機能要件 / §12.4 Logback 設定 |

各セクションの要件が実装に反映されているか確認し、実装されていない要件を **「未実装要件」** として記録する。

---

### Step 4: 実装品質チェック（手抜き検出）

以下のパターンを `appmod-migrated-java21-spring-boot-3rd/src/` から検索し、禁止パターンや手抜きがないか確認する。

#### 4.1 致命的禁止パターン（Critical — 1 件でも即不合格）

```bash
# System.out.println チェック（SLF4J @Slf4j を使用すること）
grep -r "System\.out\." appmod-migrated-java21-spring-boot-3rd/src/main/java/

# 文字列結合による SQL 構築（SQLi 脆弱性）
grep -rE '"(SELECT|UPDATE|INSERT|DELETE).*\+' appmod-migrated-java21-spring-boot-3rd/src/main/java/

# 秘密情報のハードコード
grep -rE '(password|secret|apiKey|api_key)\s*=\s*"[^$\{]' appmod-migrated-java21-spring-boot-3rd/src/

# @Autowired フィールドインジェクション（コンストラクタインジェクション必須）
grep -rn "@Autowired" appmod-migrated-java21-spring-boot-3rd/src/main/java/ | grep -v "@Bean\|@Qualifier\|@Primary"

# new による Service/Repository 生成
grep -rE "= new (.*Service|.*Repository)\(" appmod-migrated-java21-spring-boot-3rd/src/main/java/

# Optional.get() 使用（orElseThrow / orElse を使うこと）
# ※ findBy*.get() のように Optional チェーンの直呼び出しを検出する
grep -rn "\.get()" appmod-migrated-java21-spring-boot-3rd/src/main/java/ | grep -v "//(」\|\*\|test"
grep -rn "Optional\.of\|findBy\|findAll" appmod-migrated-java21-spring-boot-3rd/src/main/java/ | grep "\.get()" | grep -v "getClass\|getters\|getMessage\|getName\|getValue\|getType\|getId\|getUser"

# javax.* パッケージ残存（jakarta.* に変換されているか）
grep -rn "import javax\." appmod-migrated-java21-spring-boot-3rd/src/main/java/

# *.do URL パターンが残存
grep -rn '\.do"' appmod-migrated-java21-spring-boot-3rd/src/
```

#### 4.2 高優先度チェック（High — FAIL は不合格）

```bash
# Controller が Repository を直接参照（Service 経由必須）
grep -rn "Repository" appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/

# @Transactional を Controller に付与
grep -rn "@Transactional" appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/

# ログへの個人情報出力
grep -rn "log\.(info|debug|warn|error)" appmod-migrated-java21-spring-boot-3rd/src/main/java/ | grep -iE "(email|password|address|credit)"

# java.util.Date 使用（java.time.* を使用すること）
grep -rn "java\.util\.Date\|new Date()" appmod-migrated-java21-spring-boot-3rd/src/main/java/

# TODO / FIXME / HACK の残存（未完了作業の痕跡）
grep -rn "TODO\|FIXME\|HACK\|XXX\|TEMP" appmod-migrated-java21-spring-boot-3rd/src/main/java/

# catch ブロックでの例外握りつぶし
grep -rn -A2 "catch\s*(Exception\|Throwable" appmod-migrated-java21-spring-boot-3rd/src/main/java/ | grep -v "log\.\|throw\s"
```

#### 4.3 アーキテクチャ準拠チェック

- **レイヤー依存方向**: `controller → service → repository` の方向を守っているか。逆方向参照（`repository → service` 等）がないか
- **1 Repository = 1 Aggregate Root の原則**: `UserRepository` に `SecurityLog` のクエリが混在していないか（`SecurityLogRepository` に分離すること）
- **DTO はレコードクラス**: リクエスト DTO が `record` クラスとして定義されているか（旧来の POJO + getter/setter ではないか）
- **例外クラス階層**: カスタム例外が `ResourceNotFoundException`, `BusinessException`, `AuthenticationException` の階層で定義され、`@ControllerAdvice` でハンドリングされているか
- **`th:utext` 使用制限**: Thymeleaf テンプレートで `th:utext`（エスケープなし出力）が使われていないか（`th:text` を使うこと）
- **Spring Security `xssProtection`**: `xss.enable()` の非推奨 API ではなく `Customizer.withDefaults()` を使用しているか

#### 4.4 セキュリティチェック

- **`@Valid` の付与漏れ**: 全 Controller の `@ModelAttribute` / `@RequestBody` 引数に `@Valid` が付与されているか
- **IDOR 防止**: 注文詳細・住所等のユーザー固有リソースに、ログインユーザーとのオーナーシップ検証が実装されているか（`findByIdAndUserId` 等）
- **Spring Security 設定の必須項目**: `SecurityConfig` に CSRF 保護・セッション固定攻撃対策・XSS ヘッダー・HSTS が設定されているか
- **パスワードエンコーダー**: `DelegatingPasswordEncoder` + `LegacySha256PasswordEncoder` + `BCryptPasswordEncoder` の構成になっているか
- **`UserDetailsPasswordService`**: `CustomUserDetailsService` が `UserDetailsPasswordService` を implements し、`updatePassword()` で BCrypt 自動アップグレードを実装しているか

### Step 5: テスト存在確認

対象フェーズで実装されたクラスに対して:

1. **テストファイルの存在**: 重要クラスに対応するテストファイルが `src/test/java/` 配下に存在するか
2. **テストの充実度**: テストがスタブ（`@Test void test() {}`）ではなく、実際のシナリオ・アサーションを含むか
3. **テスト命名規約**: `should_期待結果_when_条件` パターン + `@DisplayName` 日本語説明が付与されているか
4. **AAA パターン**: `// Arrange`, `// Act`, `// Assert` コメントで 3 セクションに分割されているか

#### フェーズ別 必須テストクラス（PLAN.md §8〜9 より）

| Phase | 必須テストクラス |
|-------|---------------|
| Phase 2 | `@DataJpaTest` でエンティティのスキーマ検証 |
| Phase 3 | `UserRepositoryTest`, `ProductRepositoryTest`, `OrderRepositoryTest` |
| Phase 4 | `AuthServiceTest`, `CartServiceTest`, `CheckoutServiceTest`（11 ステップ全ロールバック検証含む）, `OrderServiceTest`, `CouponServiceTest`, `PointServiceTest`, `LegacySha256PasswordEncoderTest` |
| Phase 5 | `AuthControllerTest`, `ProductControllerTest`, `CartControllerTest`, `CheckoutControllerTest` |
| Phase 7 | Spring Security Test（認証・認可のユニットテスト） |
| Phase 8 | 全サービステスト 80%+ カバレッジ / 統合テスト（新規登録→ログイン→注文確定シナリオ / カートマージシナリオ）/ セキュリティテスト |

#### テスト実行確認コマンド

```bash
# 各フェーズの検証コマンド（PLAN.md 各フェーズの「検証コマンド」セクション参照）
cd appmod-migrated-java21-spring-boot-3rd

# Phase 3
mvn test -Dtest="*RepositoryTest"

# Phase 4
mvn test -Dtest="*ServiceTest"

# Phase 5
mvn test -Dtest="*ControllerTest"

# Phase 8 (カバレッジ計測)
mvn clean verify -Djacoco.skip=false
```

### Step 6: 検証レポートの生成

全ステップの結果を以下のフォーマットで統合レポートとして出力する:

```markdown
# Phase {N} 完了検証レポート

## 実施日時
YYYY-MM-DD

## 対象フェーズ
Phase {N}: {フェーズ名}（例: Phase 3: リポジトリ層移行）

## 総合判定
✅ 合格 / ⚠️ 条件付き合格 / ❌ 不合格

---

## 1. 修了条件チェックリスト結果

| # | チェック項目 | 結果 | 備考 |
|---|------------|------|------|
| 1 | {PLAN.md の修了条件項目} | ✅/❌/⚠️ | {詳細・確認ファイル} |
| ... | ... | ... | ... |

**合計**: ✅ {n}件 / ❌ {n}件 / ⚠️ {n}件

---

## 2. 設計書（DESIGN.md）要件 突合せ結果

| セクション | 要件概要 | 実装状況 | 備考 |
|-----------|---------|---------|------|
| §{x.x} | {概要} | ✅ 実装済 / ❌ 未実装 / ⚠️ 部分実装 | {詳細} |
| ... | ... | ... | ... |

**未実装要件**: {件数}件

---

## 3. 実装品質チェック結果

### 3-1. 致命的禁止パターン（Critical）
| チェック項目 | 結果 | 検出箇所 |
|------------|------|---------|
| System.out.println | ✅/❌ | {ファイル:行番号} |
| 文字列結合 SQL | ✅/❌ | {ファイル:行番号} |
| 秘密情報ハードコード | ✅/❌ | {ファイル:行番号} |
| @Autowired フィールドインジェクション | ✅/❌ | {ファイル:行番号} |
| new による Service/Repository 生成 | ✅/❌ | {ファイル:行番号} |
| Optional.get() 使用 | ✅/❌ | {ファイル:行番号} |
| javax.* パッケージ残存 | ✅/❌ | {ファイル:行番号} |
| *.do URL パターン残存 | ✅/❌ | {ファイル:行番号} |

### 3-2. 高優先度チェック（High）
| チェック項目 | 結果 | 検出箇所 |
|------------|------|---------|
| Controller → Repository 直接参照 | ✅/❌ | ... |
| @Transactional を Controller に付与 | ✅/❌ | ... |
| ログへの個人情報出力 | ✅/❌ | ... |
| java.util.Date 使用 | ✅/❌ | ... |
| TODO/FIXME 残存 | ✅/❌ | ... |
| 例外握りつぶし | ✅/❌ | ... |

### 3-3. アーキテクチャ準拠
| チェック項目 | 結果 | 備考 |
|------------|------|------|
| レイヤー依存方向（controller→service→repository） | ✅/❌ | ... |
| 1 Repository = 1 Aggregate Root | ✅/❌ | ... |
| DTO はレコードクラス | ✅/❌ | ... |
| GlobalExceptionHandler 実装 | ✅/❌ | ... |
| th:utext 不使用 | ✅/❌ | ... |
| xssProtection(Customizer.withDefaults()) | ✅/❌ | ... |

### 3-4. セキュリティ
| チェック項目 | 結果 | 備考 |
|------------|------|------|
| @Valid 付与漏れなし | ✅/❌ | ... |
| IDOR 防止（オーナーシップ検証） | ✅/❌ | ... |
| CSRF 保護設定 | ✅/❌ | ... |
| DelegatingPasswordEncoder 構成 | ✅/❌ | ... |
| UserDetailsPasswordService 実装 | ✅/❌ | ... |

---

## 4. テスト確認結果

| 対象クラス | テストファイル | 命名規約（should_） | AAA パターン | 充実度 |
|-----------|-------------|---------------------|------------|--------|
| {クラス名} | ✅/❌ 存在 | ✅/❌ | ✅/❌ | ✅/⚠️/❌ |
| ... | ... | ... | ... | ... |

**カバレッジ（Phase 8 のみ）**:
- Service: {%} / 目標 80%+
- Controller: {%} / 目標 80%+（@WebMvcTest）
- Repository: {%} / 目標 70%+（@DataJpaTest）
- 全体: {%} / 目標 80%+

---

## 5. 是正が必要な項目

| # | カテゴリ | 重要度 | 内容 | 対応方針 |
|---|---------|--------|------|---------|
| 1 | Critical / High / Medium | {高/中/低} | {具体的な問題内容} | {修正方針} |
| ... | ... | ... | ... | ... |
```

---

## 判定基準

| 判定 | 条件 |
|------|------|
| ✅ **合格** | 修了条件チェックリスト全項目 PASS / 未実装要件 0 件 / Critical・High チェック全 PASS |
| ⚠️ **条件付き合格** | PARTIAL が 1〜2 件のみ、かつ Medium 以下の指摘のみ（次フェーズ開始前に修正必須） |
| ❌ **不合格** | FAIL が 1 件以上 / 未実装要件あり / Critical または High の指摘あり / セキュリティ違反あり |

---

## 注意事項

- **全項目を必ず検証すること**: チェックリストの項目を飛ばしたり、「おそらく問題ない」と推定で PASS にしない。ファイルを実際に読んで `grep_search` / `read_file` で確認する
- **DESIGN.md を正として判断する**: 実装コードと設計書に矛盾がある場合、`docs/migration/DESIGN.md` を正とし、実装側を「不適合」と判断する
- **手抜きパターンを厳格にチェックする**: スタブ実装（空の関数・ダミーデータ返却・TODO コメントのみ）は FAIL とする
- **前フェーズのリグレッション確認**: 前フェーズの主要チェック項目が新しい実装で壊れていないか抜き打ちで確認する
- **AGENTS.md の遵守**: `AGENTS.md` §3〜§4 のコーディング規約・セキュリティ規約に従っているか確認する

---

## フェーズ別 重点確認事項

### Phase 0: 事前準備
- `java -version` で Java 21 であること
- `mvn -version` で Maven 3.9.x であること
- 現行アプリ（Struts）のベースライン動作記録が取得されていること

### Phase 1: プロジェクト基盤構築
- `pom.xml` に `thymeleaf-extras-springsecurity6` と `thymeleaf-layout-dialect` が含まれているか（Thymeleaf Security Integration と Tiles 置換に必須）
- `lombok` の scope が `provided` または `optional` になっているか、かつ `annotationProcessorPaths` にも追加されているか
- `net.logstash.logback:logstash-logback-encoder` がバージョン明示で追加されているか（Spring BOM 外のため）
- `application-test.properties` に `spring.flyway.enabled=false` と `MODE=PostgreSQL` が設定されているか
- 禁止依存関係（`struts`, `log4j:1.x`, `commons-dbcp`, `javax.servlet.*`）が含まれていないか

### Phase 2: ドメインモデル移行
- 全 22 エンティティが `@Entity` + `@Table(name = "...")` を持つか
- UUID PK に `@GeneratedValue` が **付いていない**か（Service 層で `UUID.randomUUID()` を使用）
- `@OneToMany` 関連に `cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY` が設定されているか
- `@BatchSize(size = 50)` が LAZY コレクション関連に付与されているか（N+1 対策）

### Phase 3: リポジトリ層移行
- `UserRepository` に `SecurityLog` のクエリが含まれていないか（`SecurityLogRepository` に分離）
- 20 リポジトリすべてが `JpaRepository<Entity, String>` を extends しているか
- PLAN.md §5 の追加メソッド定義表に従い、全メソッドが定義されているか
- `ProductRepository` に `JpaSpecificationExecutor` が追加されているか（動的検索用）

### Phase 4: サービス層移行
- `CheckoutService` の注文確定メソッドが 11 ステップすべてを単一の `@Transactional` で実行しているか（DESIGN.md §7.3 参照）
- `CartService` に `getActiveCart(String userId)` と `getOrCreateCart(HttpSession, String)` が定義されているか
- `BusinessException` が `redirectUrl` と `messageKey` フィールドを持つか（DESIGN.md §6.7 参照）
- `TaxService` が `@ConfigurationProperties` で税率を設定しているか（`AppConfig.getInstance()` 禁止）

### Phase 5: Web 層移行（Controller + DTO）
- 29 Action が 8 Controller に集約されているか（DESIGN.md §2.3 参照）
- 12 ActionForm が Bean Validation 付き `record` クラスに変換されているか
- 全 DTO の文字列フィールドに `@NotBlank` / `@Size` / `@Email` が付与されているか
- IDOR 対策として `OrderController` / `AddressController` がオーナーシップ検証を行っているか

### Phase 6: ビュー層移行（Thymeleaf）
- `fragments/layout.html` が `layout:decorate` を使う Thymeleaf Layout Dialect 形式か（Tiles の `baseLayout` 相当）
- 全テンプレートで `th:action="@{/...}"` を使用し URL をハードコードしていないか
- `th:utext` の使用箇所がないか（XSS 対策: `th:text` を使うこと）
- POST フォームに CSRF トークンが `<form th:action="@{/...}" method="post">` 形式で自動挿入されているか
- `th:sec:authorize` を使用したロールベース表示制御が実装されているか

### Phase 7: セキュリティ統合
- `SecurityConfig` に `@EnableMethodSecurity` が付与されているか
- `LegacySha256PasswordEncoder.matches()` が `{sha256}<hash>$<salt>` 形式（V2 Flyway 適用後）をパースしているか
- `CustomUserDetailsService` が `UserDetailsService` **と** `UserDetailsPasswordService` の両方を implements しているか
- `CartMergeSuccessHandler`（`AuthenticationSuccessHandler` 実装）が定義されているか
- Flyway V2 SQL で `CONCAT('{sha256}', password_hash, '$', salt)` によるプレフィックス付与が行われているか
- `xssProtection` の設定が `Customizer.withDefaults()` を使用しているか（`xss.enable()` 非推奨）

### Phase 8: テスト実装・品質確認
- `CheckoutServiceTest` が 11 ステップすべてのロールバック検証ケース（各ステップで例外発生した場合の全ロールバック）を含むか
- `LegacySha256PasswordEncoderTest` が既知の SHA-256 ハッシュ + ソルトで `matches()` の成否を検証しているか
- カートマージ統合テスト（未ログインカート → ログイン → カートマージ）が存在するか
- JaCoCo カバレッジレポートで Service 80%+・全体 80%+ が達成されているか

### Phase 9: 最終検証・リリース準備
- Dockerfile がマルチステージビルド + JRE ベースイメージ + 非 root ユーザー + `HEALTHCHECK` + `-XX:+UseContainerSupport` で構成されているか
- `.dockerignore` で `target/`, `.git/`, `*.md` が除外されているか
- `application-prod.properties` の全秘密情報が `${ENV_VAR}` 形式になっているか
- OWASP Dependency Check で Critical CVE がゼロか
- `README.md` に環境変数一覧（`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST` 等）が記載されているか

---

## 参照ドキュメント

- `docs/migration/PLAN.md` — 移行計画書（フェーズ定義・修了条件チェックリスト）
- `docs/migration/DESIGN.md` — 詳細設計書（アーキテクチャ・変換仕様・コード例）
- `AGENTS.md` — エージェント必読ガイド（コーディング規約・禁止事項・移行パターン要点）
- `.github/instructions/java-coding-standards.instructions.md` — Java コーディング規約（詳細）
- `.github/instructions/security-coding.instructions.md` — セキュリティ規約（詳細）
- `.github/instructions/api-design.instructions.md` — Controller/API 設計規約
- `.github/instructions/pom-dependency.instructions.md` — 依存関係管理規約
- `.github/instructions/spring-config.instructions.md` — Spring 設定ファイル規約（プロファイル別構成・秘密情報管理）
- `.github/instructions/test-standards.instructions.md` — テスト規約（命名・AAA・カバレッジ）
- `.github/instructions/dockerfile-infra.instructions.md` — Dockerfile / コンテナ設定規約
- `.github/instructions/sql-schema-review.instructions.md` — SQL スキーマ・Flyway 規約
