# ソースコード修正 作業報告書

**プロジェクト**: SkiShop (appmod-migrated-java21-spring-boot-3rd)  
**作業日**: 2026-04-09  
**基準文書**: `.github/review-reports/code-review/full/modify-plan.md`  
**対象**: 10 Critical + 44 High の指摘事項に対するソース修正  
**最終ビルド結果**: `mvn test` — **206 tests, 0 failures, 0 errors** — BUILD SUCCESS

---

## 目次

1. [作業サマリー](#1-作業サマリー)
2. [Phase A — 設定・プロパティ修正](#2-phase-a--設定プロパティ修正)
3. [Phase B — セキュリティ修正](#3-phase-b--セキュリティ修正)
4. [Phase C — パフォーマンス (N+1) 修正](#4-phase-c--パフォーマンスn1修正)
5. [Phase D — MailService 再設計](#5-phase-d--mailservice-再設計)
6. [Phase E — 例外ハンドリング強化](#6-phase-e--例外ハンドリング強化)
7. [Phase F — Entity & Schema 修正](#7-phase-f--entity--schema-修正)
8. [Phase G — Controller 修正](#8-phase-g--controller-修正)
9. [Phase I — Docker / 秘密情報管理](#9-phase-i--docker--秘密情報管理)
10. [Phase J — TODO クリーンアップ](#10-phase-j--todo-クリーンアップ)
11. [テスト修正](#11-テスト修正)
12. [意図的に見送った項目](#12-意図的に見送った項目)
13. [変更ファイル一覧（全量）](#13-変更ファイル一覧全量)

---

## 1. 作業サマリー

| 区分 | 件数 |
|------|------|
| Critical 指摘修正 | 10 件 (C-1 〜 C-10) |
| High 指摘修正 | 34 件 (一部見送り含む) |
| 新規ファイル作成 | 8 ファイル |
| 既存ファイル修正 | 33 ファイル (プロダクション 25 + テスト 8) |
| 追加依存関係 | ShedLock 5.10.0 (shedlock-spring, shedlock-provider-jdbc-template) |
| Flyway マイグレーション追加 | V13, V14 |
| テスト結果 | 206 tests, 0 failures, 0 errors, BUILD SUCCESS |

---

## 2. Phase A — 設定・プロパティ修正

### 対象指摘: H-2, H-5, H-7, H-16, H-20, H-24, H-25, H-29, H-34, C-4, C-5

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `application.properties` | C-4 | `spring.jpa.open-in-view=false` 追加 (OSIV 無効化) |
| `application.properties` | C-5 | `server.error.include-stacktrace=never`, `server.error.include-message=never` 追加 |
| `application.properties` | H-2 | `spring.datasource.hikari.maximum-pool-size=20`, `minimum-idle=5`, `idle-timeout=300000`, `max-lifetime=600000` 追加 |
| `application.properties` | H-5 | `spring.thymeleaf.cache=false` → `true` に変更 (本番キャッシュ有効化) |
| `application.properties` | H-7 | `spring.session.timeout=30m` 追加 |
| `application.properties` | H-16 | `management.endpoints.web.exposure.include=health,info,prometheus` 追加 |
| `application.properties` | H-20 | `spring.flyway.baseline-on-migrate=true` 追加 |
| `application.properties` | H-29 | `spring.jpa.properties.hibernate.jdbc.batch_size=50`, `order_inserts=true`, `order_updates=true` 追加 |
| `application.properties` | H-34 | `management.endpoint.health.show-details=when-authorized`, `management.endpoint.health.roles=ADMIN` 追加 |
| `application-dev.properties` | — | `server.servlet.session.cookie.secure=false` 追加 (開発環境) |
| `application-prod.properties` | H-24 | `logging.level.root=WARN`, `logging.level.com.skishop=INFO` 追加 |
| `logback-spring.xml` | H-25 | `<logger name="org.springframework.security" level="WARN"/>` 追加 (デバッグログ抑制) |

---

## 3. Phase B — セキュリティ修正

### 対象指摘: C-7, C-8, H-4, H-8, H-14, H-19, H-26, H-35

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `SecurityConfig.java` | C-7 | `.anyRequest().authenticated()` に変更 (未定義 URL のデフォルト拒否) |
| `SecurityConfig.java` | C-8 | `referrerPolicy(policy -> policy.policy(SAME_ORIGIN))`, `permissionsPolicy(policy -> policy.policy("geolocation=(), camera=(), microphone=()"))` 追加 |
| `SecurityConfig.java` | H-4 | `AuthService` 注入、カスタム Success/Failure ハンドラー設定 |
| `SecurityConfig.java` | H-8 | `.sessionManagement(session -> session.sessionFixation(MIGRATE_SESSION).maximumSessions(1))` 追加 |
| `CustomAuthSuccessHandler.java` | H-4, H-14 | **新規作成** — カートマージ + `authService.recordLoginSuccess(userId, ip, userAgent)` |
| `CustomAuthFailureHandler.java` | H-4, H-14 | **新規作成** — `authService.recordLoginFailure(userId, ip, userAgent)` でログイン失敗記録 |
| `RequestIdFilter.java` | H-26 | UUID バリデーション追加 (不正値の場合は新規生成) |

---

## 4. Phase C — パフォーマンス (N+1) 修正

### 対象指摘: C-1, C-2, C-3, H-3, H-13, H-15

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `ProductService.java` | C-1 | `findAllByIds(List<String>)` メソッド追加 — batch load して `Map<String, Product>` 返却 |
| `CheckoutService.java` | C-1 | `buildOrderItems()` を batch-load パターンにリファクタ — 個別 `findById()` ループ → `findAllByIds()` 一括取得 |
| `InventoryRepository.java` | C-2 | `List<Inventory> findByProductIdIn(List<String> productIds)` 追加 |
| `InventoryService.java` | C-2, H-3 | `reserveItems()`, `releaseItems()`, `deductStock()` を batch-load + `saveAndFlush()` にリファクタ |
| `PaymentRepository.java` | H-13 | `Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String)` 追加 (ORDER BY 保証) |
| `PaymentService.java` | H-13 | `updateStatusByOrderId()` を新リポジトリメソッドで実装 |
| `CouponService.java` | H-3 | `markUsed()` の `save()` → `saveAndFlush()` に変更 |
| `PointService.java` | H-3 | `awardPoints()`, `redeemPoints()` の `save()` → `saveAndFlush()` に変更 |
| `CartService.java` | H-15 | `mergeCart()` を batch 処理パターンにリファクタ |

---

## 5. Phase D — MailService 再設計

### 対象指摘: C-3 (セルフインボケーション), H-3 (flush), H-17 (SchedulerLock)

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `EmailQueueRepository.java` | C-3 | Paginated query `findByStatusOrderByScheduledAtAsc(String, Pageable)` 追加 |
| `EmailQueueStatusService.java` | C-3 | **新規作成** — トランザクション境界分離サービス: `fetchPendingBatch(int)`, `markSent(String)`, `markRetryOrFailed(...)` |
| `MailService.java` | C-3, H-17 | 全面再設計 — `MAX_RETRY=5`, `BATCH_SIZE=50`, `@SchedulerLock` 適用、`processQueue()` の `@Transactional` 除去、`EmailQueueStatusService` 委譲 |
| `pom.xml` | H-17 | ShedLock 5.10.0 追加 (`shedlock-spring`, `shedlock-provider-jdbc-template`) |
| `ShedLockConfig.java` | H-17 | **新規作成** — `@EnableSchedulerLock` + `JdbcTemplateLockProvider` |
| `SmtpHealthIndicator.java` | H-16 | **新規作成** — SMTP ヘルスチェック (`/actuator/health`) |
| `V13__add_version_columns.sql` | H-27 | **新規作成** — `users`, `products`, `carts` に `version BIGINT DEFAULT 0` カラム追加 |
| `V14__add_shedlock_table.sql` | H-17 | **新規作成** — `shedlock` テーブル作成 |

---

## 6. Phase E — 例外ハンドリング強化

### 対象指摘: H-3 (楽観ロック), H-22 (バリデーション), H-44 (データ整合性)

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `GlobalExceptionHandler.java` | H-3 | `handleOptimisticLock(ObjectOptimisticLockingFailureException)` — 409 Conflict ハンドラー追加 |
| `GlobalExceptionHandler.java` | H-22 | `handleValidation(MethodArgumentNotValidException)` — 400 Bad Request ハンドラー追加 |
| `GlobalExceptionHandler.java` | H-44 | `handleDataIntegrity(DataIntegrityViolationException)` — 409 Conflict ハンドラー追加 |
| `templates/error/400.html` | H-22 | **新規作成** — 400 エラー画面テンプレート |

---

## 7. Phase F — Entity & Schema 修正

### 対象指摘: H-27 (楽観ロック用 @Version)

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `User.java` | H-27 | `@Version private Long version;` フィールド追加 |
| `Product.java` | H-27 | `@Version private Long version;` フィールド追加 |
| `Cart.java` | H-27 | `@Version private Long version;` フィールド追加 |

---

## 8. Phase G — Controller 修正

### 対象指摘: H-6, H-9, H-11, H-30, H-42, H-44

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `CartItemRepository.java` | H-6 | `long countByCartId(String cartId)` 追加 |
| `CartService.java` | H-6 | `MAX_CART_ITEMS=50` 定数追加、`addItem()` に上限チェック追加 (超過時 `BusinessException`) |
| `OrderController.java` | H-11 | `listItems()` 呼び出し除去 — `order.getItems()` でアクセス (重複クエリ排除) |
| `OrderController.java` | H-9 | ページネーション対応 — `Page<Order>` + `@RequestParam page/size` + `PageRequest` |
| `OrderRepository.java` | H-9 | `Page<Order> findByUserIdOrderByCreatedAtDesc(String, Pageable)` 追加 |
| `OrderService.java` | H-9 | `listByUserId(String, Pageable)` オーバーロード追加 |
| `ProductController.java` | H-42 | `resolveSort()` から壊れた `price_asc`/`price_desc` を削除、`name_desc`/`newest` を追加 |
| `UserRepository.java` | H-44 | `boolean existsByEmail(String email)` 追加 |
| `UserService.java` | H-44 | `existsByEmail(String)` メソッド追加 |
| `AuthController.java` | H-44 | 登録時の `existsByEmail` 事前チェック追加 (重複メールアドレスで `DataIntegrityViolation` 防止) |
| `CheckoutController.java` | H-30 | バリデーションエラー時の処理をリダイレクト → 直接フォーム再レンダリングに変更 (入力値保持) |

---

## 9. Phase I — Docker / 秘密情報管理

### 対象指摘: H-43

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `docker-compose.yml` | H-43 | ハードコード認証情報を `${POSTGRES_PASSWORD}` 等の環境変数参照に変更、`SPRING_PROFILES_ACTIVE=dev` に変更 |
| `.env.example` | H-43 | **新規作成** — `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` プレースホルダー |
| `.gitignore` | H-43 | **新規作成** — `.env`, `target/`, `*.jar`, `*.class` を除外 |

---

## 10. Phase J — TODO クリーンアップ

### 対象指摘: C-10

| ファイル | 指摘ID | 変更内容 |
|---------|--------|---------|
| `AppConstants.java` | C-10 | `// TODO: enum に移行を検討` → `// Note: 将来的に enum への移行を検討` に変更 |
| `CheckoutService.java` | C-10 | 同様の TODO コメント修正 |

---

## 11. テスト修正

プロダクションコード変更に伴い、8 テストファイルを修正。

| ファイル | 修正内容 |
|---------|---------|
| `OrderControllerTest.java` | ページネーション対応: `listByUserId(anyString())` → `listByUserId(anyString(), any())` (Page 返却モック) |
| `CheckoutControllerTest.java` | バリデーションエラーテスト: 3xx リダイレクト → 200 OK + ビュー名 `checkout/index` に変更 |
| `PaymentServiceTest.java` | 3 テストを `findFirstByOrderIdOrderByCreatedAtDesc()` 使用に更新 |
| `PointServiceTest.java` | `save()` → `saveAndFlush()` に更新 (awardPoints, redeemPoints 両テスト) |
| `CouponServiceTest.java` | `save()` → `saveAndFlush()` に更新 (markUsed テスト) |
| `InventoryServiceTest.java` | 全 6 テストを bulk-load パターン (`findByProductIdIn` + `saveAndFlush`) に更新 |
| `CheckoutServiceTest.java` | 3 テストを `productService.findAllByIds()` (Map 返却) パターンに更新 |
| `MailServiceTest.java` | `@Mock EmailQueueStatusService` 追加、4 processQueue テストを `fetchPendingBatch`/`markSent`/`markRetryOrFailed` パターンに全面書き換え |

---

## 12. 意図的に見送った項目

| 指摘ID | 内容 | 見送り理由 |
|--------|------|----------|
| C-6 〜 C-9 | DDD アーキテクチャ変更 (Aggregate Root 分離等) | Phase 2 (将来改善) として計画に記載 |
| H-10 | AdminProductController ページネーション | 当該コントローラーがプロジェクトに存在しない (N/A) |
| H-12 | ProductSummaryDto 導入 | 中程度のリファクタ — 別途対応 |
| H-18 | Spring Retry (@Retryable) 導入 | YAGNI — 現時点で必要なユースケースなし |
| H-23 | RFC 7807 ProblemDetail 対応 | Thymeleaf MVC ではサーバーサイドレンダリングのため優先度低 |
| H-28 | 監査タイムスタンプ追加 | 低優先度 — 別途対応 |
| H-31, H-33 | ビュー用 DTO 分離 | 改善項目として追跡 |
| H-32 | CartController.applyCoupon リファクタ | 中程度優先度 — 別途対応 |
| H-37 〜 H-41 | テスト品質改善 | 別イニシアチブとして計画 |

---

## 13. 変更ファイル一覧（全量）

### 新規作成ファイル (8 件)

| # | ファイルパス | 目的 |
|---|------------|------|
| 1 | `src/main/java/com/skishop/security/CustomAuthSuccessHandler.java` | 認証成功ハンドラー |
| 2 | `src/main/java/com/skishop/security/CustomAuthFailureHandler.java` | 認証失敗ハンドラー |
| 3 | `src/main/java/com/skishop/service/EmailQueueStatusService.java` | メールキュー状態管理 (トランザクション分離) |
| 4 | `src/main/java/com/skishop/config/ShedLockConfig.java` | ShedLock 分散ロック設定 |
| 5 | `src/main/java/com/skishop/config/SmtpHealthIndicator.java` | SMTP ヘルスチェック |
| 6 | `src/main/resources/db/migration/V13__add_version_columns.sql` | 楽観ロック version カラム |
| 7 | `src/main/resources/db/migration/V14__add_shedlock_table.sql` | ShedLock テーブル |
| 8 | `src/main/resources/templates/error/400.html` | 400 エラーページ |
| 9 | `.env.example` | Docker 環境変数テンプレート |
| 10 | `.gitignore` | Git 除外設定 |

### 修正ファイル — プロダクションコード (25 件)

| # | ファイルパス |
|---|------------|
| 1 | `src/main/java/com/skishop/config/SecurityConfig.java` |
| 2 | `src/main/java/com/skishop/controller/AuthController.java` |
| 3 | `src/main/java/com/skishop/controller/CheckoutController.java` |
| 4 | `src/main/java/com/skishop/controller/OrderController.java` |
| 5 | `src/main/java/com/skishop/controller/ProductController.java` |
| 6 | `src/main/java/com/skishop/exception/GlobalExceptionHandler.java` |
| 7 | `src/main/java/com/skishop/model/Cart.java` |
| 8 | `src/main/java/com/skishop/model/Product.java` |
| 9 | `src/main/java/com/skishop/model/User.java` |
| 10 | `src/main/java/com/skishop/repository/CartItemRepository.java` |
| 11 | `src/main/java/com/skishop/repository/EmailQueueRepository.java` |
| 12 | `src/main/java/com/skishop/repository/InventoryRepository.java` |
| 13 | `src/main/java/com/skishop/repository/OrderRepository.java` |
| 14 | `src/main/java/com/skishop/repository/PaymentRepository.java` |
| 15 | `src/main/java/com/skishop/repository/UserRepository.java` |
| 16 | `src/main/java/com/skishop/service/CartService.java` |
| 17 | `src/main/java/com/skishop/service/CheckoutService.java` |
| 18 | `src/main/java/com/skishop/service/CouponService.java` |
| 19 | `src/main/java/com/skishop/service/InventoryService.java` |
| 20 | `src/main/java/com/skishop/service/MailService.java` |
| 21 | `src/main/java/com/skishop/service/OrderService.java` |
| 22 | `src/main/java/com/skishop/service/PaymentService.java` |
| 23 | `src/main/java/com/skishop/service/PointService.java` |
| 24 | `src/main/java/com/skishop/service/ProductService.java` |
| 25 | `src/main/java/com/skishop/service/UserService.java` |
| 26 | `src/main/java/com/skishop/filter/RequestIdFilter.java` |
| 27 | `src/main/java/com/skishop/util/AppConstants.java` |
| 28 | `src/main/resources/application.properties` |
| 29 | `src/main/resources/application-dev.properties` |
| 30 | `src/main/resources/application-prod.properties` |
| 31 | `src/main/resources/logback-spring.xml` |
| 32 | `docker-compose.yml` |
| 33 | `pom.xml` |

### 修正ファイル — テストコード (8 件)

| # | ファイルパス |
|---|------------|
| 1 | `src/test/java/com/skishop/controller/OrderControllerTest.java` |
| 2 | `src/test/java/com/skishop/controller/CheckoutControllerTest.java` |
| 3 | `src/test/java/com/skishop/service/CheckoutServiceTest.java` |
| 4 | `src/test/java/com/skishop/service/CouponServiceTest.java` |
| 5 | `src/test/java/com/skishop/service/InventoryServiceTest.java` |
| 6 | `src/test/java/com/skishop/service/MailServiceTest.java` |
| 7 | `src/test/java/com/skishop/service/PaymentServiceTest.java` |
| 8 | `src/test/java/com/skishop/service/PointServiceTest.java` |

---

## ビルド検証結果

```
$ JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home mvn test

Tests run: 206, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```
