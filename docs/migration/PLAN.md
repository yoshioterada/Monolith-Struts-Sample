# SkiShop 移行計画書（Migration Plan）
## Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x

**文書バージョン**: 1.0  
**作成日**: 2026-03-31  
**移行先ディレクトリ**: `appmod-migrated-java21-spring-boot-3rd/`  
**関連設計書**: [DESIGN.md](./DESIGN.md)

---

## 目次

1. [移行フェーズ全体図](#1-移行フェーズ全体図)
2. [フェーズ 0: 事前準備](#2-フェーズ-0-事前準備)
3. [フェーズ 1: プロジェクト基盤構築](#3-フェーズ-1-プロジェクト基盤構築)
4. [フェーズ 2: ドメインモデル移行](#4-フェーズ-2-ドメインモデル移行)
5. [フェーズ 3: リポジトリ層移行](#5-フェーズ-3-リポジトリ層移行)
6. [フェーズ 4: サービス層移行](#6-フェーズ-4-サービス層移行)
7. [フェーズ 5: Web 層移行（Controller + DTO）](#7-フェーズ-5-web-層移行controller--dto)
8. [フェーズ 6: ビュー層移行（Thymeleaf）](#8-フェーズ-6-ビュー層移行thymeleaf)
9. [フェーズ 7: セキュリティ統合](#9-フェーズ-7-セキュリティ統合)
10. [フェーズ 8: テスト実装・品質確認](#10-フェーズ-8-テスト実装品質確認)
11. [フェーズ 9: 最終検証・リリース準備](#11-フェーズ-9-最終検証リリース準備)
12. [リスクと対策](#12-リスクと対策)
13. [品質チェックリスト](#13-品質チェックリスト)
14. [完了判定基準](#14-完了判定基準)
15. [ロールバック計画](#15-ロールバック計画)

---

## 1. 移行フェーズ全体図

```
Phase 0: 事前準備
  └─ 環境確認・ベースライン確定

Phase 1: プロジェクト基盤構築
  └─ Spring Boot プロジェクト作成・pom.xml・設定ファイル
  
Phase 2: ドメインモデル移行
  └─ POJO → JPA @Entity 変換（全 22 エンティティ）
  
Phase 3: リポジトリ層移行
  └─ JDBC DAO → Spring Data JPA Repository（全 20 リポジトリ）
  
Phase 4: サービス層移行
  └─ Service クラスの DI 化・@Transactional 付与（全 13 サービス）
  
Phase 5: Web 層移行（Controller + DTO）
  └─ Struts Action → Spring MVC Controller（全 29 Action）
  └─ ActionForm → Bean Validation DTO

Phase 6: ビュー層移行（Thymeleaf）
  └─ JSP + Tiles → Thymeleaf テンプレート（全 30+ 画面）
  
Phase 7: セキュリティ統合
  └─ Spring Security 設定・認証/認可・パスワード移行
  
Phase 8: テスト実装・品質確認
  └─ JUnit 5 テスト・カバレッジ確認・E2E テスト
  
Phase 9: 最終検証・リリース準備
  └─ 本番環境想定テスト・Docker 構築・ドキュメント最終化
```

**各フェーズ完了判定**: コンパイル成功 + 各フェーズのチェックリスト全 ✅

---

## 2. フェーズ 0: 事前準備

### 目的
移行作業の基準点を確立し、チーム全員が共通認識を持つ。

### 作業項目

| # | 作業 | 担当 | 確認方法 |
|---|------|------|---------|
| 0-1 | 現行アプリの全機能動作確認（ベースライン） | 移行担当 | 手動テスト・スクリーンショット |
| 0-2 | 現行テスト実行・パス率記録 | 移行担当 | `mvn test` 結果を記録 |
| 0-3 | JDK 21 インストール確認 | 移行担当 | `java -version` |
| 0-4 | Maven 3.9.x 確認 | 移行担当 | `mvn -version` |
| 0-5 | PostgreSQL 接続確認（ローカル or Docker） | 移行担当 | `psql -U skishop -d skishop` |
| 0-6 | Git ブランチ作成 | 移行担当 | `git checkout -b migration/spring-boot-v3` |
| 0-7 | 設計書（DESIGN.md）のレビュー完了 | tech-lead/architect | 全セクション確認済み |
| 0-8 | 移行計画書（PLAN.md）のレビュー完了 | tech-lead/architect | 全フェーズ確認済み |

### 完了条件
- [ ] 現行アプリのベースライン動作確認完了
- [ ] 開発環境に JDK 21 + Maven 3.9 インストール済み
- [ ] 設計書・計画書レビュー完了

---

## 3. フェーズ 1: プロジェクト基盤構築

### 目的
`appmod-migrated-java21-spring-boot-3rd/` ディレクトリに Spring Boot プロジェクトを作成し、コンパイル可能な状態を確立する。

### 作業項目

#### 1-1: `pom.xml` 作成

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>  <!-- 最新の 3.2.x を使用 -->
</parent>

<dependencies>
    spring-boot-starter-web
    spring-boot-starter-thymeleaf
    thymeleaf-extras-springsecurity6        <!-- Thymeleaf で ${#authentication.*} を使用するため必須 -->
    thymeleaf-layout-dialect               <!-- Tiles 置換: layout:decorate を有効にするため必須 -->
    spring-boot-starter-data-jpa
    spring-boot-starter-security
    spring-boot-starter-validation
    spring-boot-starter-mail
    spring-boot-starter-actuator
    flyway-core                            <!-- DB スキーマバージョン管理 -->
    flyway-database-postgresql             <!-- Flyway PostgreSQL 方言 -->
    micrometer-registry-prometheus
    springdoc-openapi-starter-webmvc-ui (2.3.x)
    lombok (provided/optional)              <!-- @RequiredArgsConstructor 等のコード例で使用。ビルド時のみ必要: annotationProcessorPaths に追加 -->
    logstash-logback-encoder (net.logstash.logback, 8.x)  <!-- 本番 JSON ログ用。Spring BOM 外のためバージョン明示指定必須 -->
    spring-security-test (test)
    postgresql (runtime)
    h2 (test)
    spring-boot-starter-test (test)
</dependencies>
```

#### 1-2: メインクラス作成

```java
@SpringBootApplication
public class SkiShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkiShopApplication.class, args);
    }
}
```

#### 1-3: 設定ファイル作成

`application.properties`（共通）:
```properties
spring.application.name=skishop-app
server.error.include-stacktrace=never
server.error.include-message=never
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
logging.level.root=WARN
logging.level.com.skishop=INFO
spring.jpa.open-in-view=false
spring.thymeleaf.cache=false
```

`application-dev.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/skishop
spring.datasource.username=skishop
spring.datasource.password=${DB_PASSWORD:localdev}
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
logging.level.com.skishop=DEBUG
management.endpoints.web.exposure.include=*
```

`application-test.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false  # テストでは Flyway を無効化し JPA DDL を使用
```

#### 1-4: ディレクトリ構成作成

設計書 §5.1 に従いパッケージディレクトリを作成。（空の `.gitkeep` ファイルで管理）

#### 1-5: messages.properties 移植

現行の `src/main/resources/messages.properties` をそのままコピー（文字列リソースは再利用可能）。

#### 1-6: DB スクリプト・ Flyway 設定

現行の `db/schema.sql` と `db/data.sql` を Flyway マイグレーション形式に変換する。

```
src/main/resources/db/migration/
├── V1__initial_schema.sql      # 現行 schema.sql を転記
├── V2__add_password_prefix.sql # パスワードハッシュプレフィックス付与（Phase 7 で実行）
└── R__seed_data.sql            # 開発用初期データ（Repeatable）
```

### 検証コマンド
```bash
cd appmod-migrated-java21-spring-boot-3rd
mvn clean compile  # コンパイル成功を確認
```

### 完了条件
- [ ] `mvn clean compile` 成功
- [ ] `SkiShopApplication` 起動確認（DB なしでも起動確認できる最低限の設定）
- [ ] `application.properties` に機密情報が直接記述されていない

---

## 4. フェーズ 2: ドメインモデル移行

### 目的
現行の POJO ドメインクラスを JPA エンティティに変換する。

### 対象エンティティ（22 クラス）

| ドメイン | 現行クラス | 移行後 | 主な変換内容 |
|---------|----------|--------|------------|
| user | `User.java` | `User.java` | `@Entity`, `java.util.Date` → `LocalDateTime` |
| user | `SecurityLog.java` | `SecurityLog.java` | `@Entity` |
| user | `PasswordResetToken.java` | `PasswordResetToken.java` | `@Entity`, 有効期限管理 |
| address | `Address.java` | `Address.java` | `@Entity`, `@ManyToOne(User)` |
| cart | `Cart.java` | `Cart.java` | `@Entity`, `@OneToMany(CartItem)` |
| cart | `CartItem.java` | `CartItem.java` | `@Entity`, `@ManyToOne(Cart)` |
| product | `Category.java` | `Category.java` | `@Entity`, 自己参照（parent） |
| product | `Product.java` | `Product.java` | `@Entity` |
| product | `Price.java` | `Price.java` | `@Entity`, `@ManyToOne(Product)` |
| inventory | `Inventory.java` | `Inventory.java` | `@Entity` |
| coupon | `Coupon.java` | `Coupon.java` | `@Entity` |
| coupon | `CouponUsage.java` | `CouponUsage.java` | `@Entity` |
| coupon | `Campaign.java` | `Campaign.java` | `@Entity` |
| order | `Order.java` | `Order.java` | `@Entity`, `@OneToMany(OrderItem)` |
| order | `OrderItem.java` | `OrderItem.java` | `@Entity` |
| order | `OrderShipping.java` | `OrderShipping.java` | `@Entity` |
| payment | `Payment.java` | `Payment.java` | `@Entity` |
| shipping | `Shipment.java` | `Shipment.java` | `@Entity` |
| shipping | `Return.java` | `Return.java` | `@Entity` |
| shipping | `ShippingMethod.java` | `ShippingMethod.java` | `@Entity` |
| point | `PointAccount.java` | `PointAccount.java` | `@Entity` |
| point | `PointTransaction.java` | `PointTransaction.java` | `@Entity` |
| mail | `EmailQueue.java` | `EmailQueue.java` | `@Entity` |

### 変換ルール（詳細）

1. クラスに `@Entity`, `@Table(name = "テーブル名")` を付与
2. `@Id` を String 型 id に付与（UUID は自動生成しない: `@GeneratedValue` なし、サービス層で UUID.randomUUID() を使用）
3. `java.util.Date` フィールドは `java.time.LocalDateTime` / `LocalDate` に変換
4. `@Column(name = "カラム名")` でスキーマのスネークケース名を明示
5. NOT NULL 制約は `@Column(nullable = false)` で表現
6. `created_at` / `updated_at` は `@CreationTimestamp` / `@UpdateTimestamp` を付与
7. リレーションシップ（`@OneToMany`, `@ManyToOne`）は LAZY フェッチを明示

### 検証コマンド
```bash
mvn clean compile -pl appmod-migrated-java21-spring-boot-3rd
```

### 完了条件
- [ ] 全 22 エンティティのコンパイル成功
- [ ] `@DataJpaTest` でエンティティのスキーマ検証が通る（H2 で DDL 生成確認）
- [ ] `java.util.Date` は使用されていない
- [ ] `@Column` でカラム名が明示されている（全フィールド）

---

## 5. フェーズ 3: リポジトリ層移行

### 目的
JDBC DAO を Spring Data JPA Repository に置き換える。

### 作業項目

| # | リポジトリ | ベース | 追加メソッド |
|---|-----------|--------|------------|
| 3-1 | `UserRepository` | `JpaRepository<User, String>` | `findByEmail`, `findByStatus` |
| 3-2 | `SecurityLogRepository` | `JpaRepository<SecurityLog, String>` | `countByUserIdAndEventType` |
| 3-3 | `PasswordResetTokenRepository` | `JpaRepository<PasswordResetToken, String>` | `findByToken`, `deleteByToken` |
| 3-4 | `AddressRepository` | `JpaRepository<Address, String>` | `findByUserId` |
| 3-5 | `CartRepository` | `JpaRepository<Cart, String>` | `findByUserIdAndStatus`, `findBySessionId` |
| 3-6 | `CartItemRepository` | `JpaRepository<CartItem, String>` | `findByCartId`, `deleteByCartId` |
| 3-7 | `CategoryRepository` | `JpaRepository<Category, String>` | `findByParentIdIsNull`, `findByParentId` |
| 3-8 | `ProductRepository` | `JpaRepository<Product, String>` | `findByCategoryId`, `findByStatus` + 動的検索用 `JpaSpecificationExecutor` |
| 3-9 | `PriceRepository` | `JpaRepository<Price, String>` | `findByProductId` |
| 3-10 | `InventoryRepository` | `JpaRepository<Inventory, String>` | `findByProductId` |
| 3-11 | `CouponRepository` | `JpaRepository<Coupon, String>` | `findByCode`, `findByStatus` |
| 3-12 | `CouponUsageRepository` | `JpaRepository<CouponUsage, String>` | `findByUserIdAndCouponId`, `countByUserIdAndCouponId` |
| 3-13 | `OrderRepository` | `JpaRepository<Order, String>` | `findByUserId`, `findByOrderNumber` |
| 3-14 | `OrderItemRepository` | `JpaRepository<OrderItem, String>` | `findByOrderId` |
| 3-15 | `ReturnRepository` | `JpaRepository<Return, String>` | `findByOrderId`, `findByOrderItemId` |
| 3-16 | `PaymentRepository` | `JpaRepository<Payment, String>` | `findByOrderId` |
| 3-17 | `ShippingMethodRepository` | `JpaRepository<ShippingMethod, String>` | `findByStatus` |
| 3-18 | `PointAccountRepository` | `JpaRepository<PointAccount, String>` | `findByUserId` |
| 3-19 | `PointTransactionRepository` | `JpaRepository<PointTransaction, String>` | `findByAccountId` |
| 3-20 | `EmailQueueRepository` | `JpaRepository<EmailQueue, String>` | `findByStatus`, `findByStatusOrderByCreatedAt` |

### SQL インジェクション対策

- 全てのクエリパラメータは Spring Data のバインドパラメータ（`@Param`）または JPA メソッド名クエリを使用
- 文字列結合による動的 SQL は**絶対に禁止**
- 動的条件は `Specification<T>` で実装

### 検証コマンド
```bash
mvn test -Dtest="*RepositoryTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### 完了条件
- [ ] 全 20 リポジトリのコンパイル成功
- [ ] `@DataJpaTest` での CRUD 操作テスト通過
- [ ] 文字列結合 SQL が存在しない（grep 確認）

---

## 6. フェーズ 4: サービス層移行

### 目的
サービスクラスを Spring Bean 化し、`@Transactional` で一貫性を確保する。

### 作業項目

| # | サービスクラス | 移行メモ |
|---|--------------|---------|
| 4-1 | `AuthService` | `UserRepository` + `SecurityLogRepository` を DI。BCrypt 対応 |
| 4-2 | `UserService` | `UserRepository` + `PasswordResetTokenRepository`。メール連動 |
| 4-3 | `ProductService` | `ProductRepository` + `PriceRepository` + `CategoryRepository`。動的検索は Specification |
| 4-4 | `CartService` | `CartRepository` + `CartItemRepository`。セッション連動 |
| 4-5 | `InventoryService` | `InventoryRepository`。在庫減算は `@Transactional` で保護 |
| 4-6 | `CouponService` | `CouponRepository` + `CouponUsageRepository`。検証ロジック移植 |
| 4-7 | `OrderService` | `OrderRepository` + `OrderItemRepository`。`@Transactional` で注文確定を原子化 |
| 4-8 | `PaymentService` | `PaymentRepository`。ステータス管理 |
| 4-9 | `ShippingService` | `ShippingMethodRepository` |
| 4-10 | `TaxService` | `@ConfigurationProperties` で税率設定 |
| 4-11 | `PointService` | `PointAccountRepository` + `PointTransactionRepository` |
| 4-12 | `MailService` | `JavaMailSender`（Spring Boot Mail）。`EmailQueueRepository` |
| 4-13 | `AddressService` | `AddressRepository` |

### 重要事項

1. **コンストラクタインジェクション必須**: `@Autowired` フィールドインジェクション禁止
2. **`@Transactional(readOnly = true)`**: 読み取り専用メソッドは必ず付与
3. **`new` による依存生成の禁止**: 現行の `private final AuthService authService = new AuthService()` パターンを全廃
4. **例外変換**: DAO 例外（DataAccessException）をビジネス例外（ResourceNotFoundException 等）に変換

### 検証コマンド
```bash
mvn test -Dtest="*ServiceTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### 完了条件
- [ ] `new` による依存性生成が存在しない（grep 確認）
- [ ] 全サービスに `@Service` アノテーション付与
- [ ] DB を更新する全メソッドに `@Transactional` 付与
- [ ] 読み取りメソッドに `@Transactional(readOnly = true)` 付与
- [ ] サービスユニットテスト（Mockito を使用）が全て通過

---

## 7. フェーズ 5: Web 層移行（Controller + DTO）

### 目的
Struts Action を Spring MVC Controller に変換し、ActionForm を Bean Validation 付き DTO に置き換える。

### 5-A: DTO 作成（先行）

ActionForm から Bean Validation 付き DTO レコードクラスに変換する（先に DTO を作成し、後から Controller で使用）。

| 現行 ActionForm | 移行後 DTO |
|----------------|-----------|
| `LoginForm` | `LoginRequest` |
| `RegisterForm` | `RegisterRequest` |
| `PasswordResetRequestForm` | `PasswordForgotRequest` |
| `PasswordResetForm` | `PasswordResetRequest` |
| `ProductSearchForm` | `ProductSearchRequest` |
| `AddCartForm` | `CartItemRequest` |
| `CheckoutForm` | `CheckoutRequest` |
| `CouponForm` | `CouponApplyRequest` |
| `AddressForm` | `AddressRequest` |
| `AdminProductForm` | `AdminProductRequest` |
| `AdminCouponForm` | `AdminCouponRequest` |
| `AdminShippingMethodForm` | `AdminShippingMethodRequest` |

**変換ルール**:
- ActionForm の `validate()` メソッドロジック → Bean Validation アノテーション
- 文字列フィールドには `@NotBlank` / `@Size` を付与
- メールフィールドには `@Email` を付与
- 数値フィールドには `@Min` / `@Max` / `@Positive` を付与

### 5-B: Controller 作成

設計書 §6 の変換仕様に従い Controller を実装する。

#### 各 Controller の実装チェックリスト

**AuthController**:
- [ ] GET /auth/login → `auth/login` テンプレート
- [ ] POST /auth/login → 認証 → `redirect:/` or `auth/login`（エラー）
- [ ] GET /auth/register → `auth/register` テンプレート
- [ ] POST /auth/register → 会員登録 → `redirect:/auth/login`
- [ ] GET /auth/password/forgot → パスワード再発行画面
- [ ] POST /auth/password/forgot → メール送信
- [ ] GET /auth/password/reset → リセット画面
- [ ] POST /auth/password/reset → パスワード変更
- [ ] POST /auth/logout → ログアウト → `redirect:/`

**ProductController**:
- [ ] GET /products → 商品一覧（検索・ページング）
- [ ] GET /products/{id} → 商品詳細

**CartController**:
- [ ] GET /cart → カート表示
- [ ] POST /cart/items → カートに商品追加
- [ ] PUT /cart/items/{id} → 数量変更
- [ ] DELETE /cart/items/{id} → 商品削除

**CheckoutController**:
- [ ] GET /checkout → チェックアウト画面（要認証）
- [ ] POST /checkout → 注文確定（要認証）

**CouponController**:
- [ ] POST /cart/coupon → クーポン適用
- [ ] GET /coupons → 利用可能クーポン一覧

**OrderController**:
- [ ] GET /orders → 注文履歴（要認証）
- [ ] GET /orders/{id} → 注文詳細（要認証）
- [ ] POST /orders/{id}/cancel → 注文キャンセル（要認証）
- [ ] POST /orders/{orderId}/return → 返品申請（要認証）

**PointController**:
- [ ] GET /account/points → ポイント残高（要認証）

**AddressController**:
- [ ] GET /account/addresses → 住所一覧（要認証）
- [ ] POST /account/addresses → 住所追加/更新（要認証）

**AdminProductController**:
- [ ] GET /admin/products → 管理商品一覧（ADMIN 専用）
- [ ] GET /admin/products/{id}/edit → 商品編集画面
- [ ] PUT /admin/products/{id} → 商品更新
- [ ] POST /admin/products → 商品新規作成
- [ ] DELETE /admin/products/{id} → 商品削除

**AdminOrderController**:
- [ ] GET /admin/orders → 管理注文一覧
- [ ] GET /admin/orders/{id} → 注文詳細
- [ ] PUT /admin/orders/{id}/status → 注文ステータス更新
- [ ] POST /admin/orders/{id}/refund → 返金処理

**AdminCouponController**:
- [ ] GET /admin/coupons → クーポン一覧
- [ ] PUT /admin/coupons/{id} → クーポン更新
- [ ] POST /admin/coupons → クーポン新規作成

**AdminShippingController**:
- [ ] GET /admin/shipping → 配送方法一覧
- [ ] PUT /admin/shipping/{id} → 配送方法更新

### 検証コマンド
```bash
mvn test -Dtest="*ControllerTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### 完了条件
- [ ] 全 Controller のコンパイル成功
- [ ] `@Valid` が全リクエスト DTO 引数に付与されている
- [ ] URL パターンから `*.do` が排除されている
- [ ] `@WebMvcTest` でコントローラーの HTTP リクエスト/レスポンステスト通過
- [ ] フラッシュメッセージ（`RedirectAttributes`）が正しく実装されている

---

## 8. フェーズ 6: ビュー層移行（Thymeleaf）

### 目的
JSP + Tiles を Thymeleaf テンプレートに変換する。

### 8-A: レイアウト作成（先行）

1. `templates/fragments/layout.html` を作成（Tiles の `baseLayout` 相当）
2. `templates/fragments/header.html` を作成
3. `templates/fragments/footer.html` を作成
4. `templates/fragments/messages.html` を作成（Struts `ActionMessages` 相当）
5. `templates/error/` 配下にエラーページを作成（400, 403, 404, 500）

### 8-B: 画面テンプレート変換

設計書 §10.3 のマッピングに従い変換する。

| 優先度 | テンプレート | 現行 JSP |
|--------|-----------|---------|
| 高 | `templates/auth/login.html` | `auth/login.jsp` |
| 高 | `templates/auth/register.html` | `auth/register.jsp` |
| 高 | `templates/products/list.html` | `products/list.jsp` |
| 高 | `templates/products/detail.html` | `products/detail.jsp` |
| 高 | `templates/cart/view.html` | `cart/view.jsp` |
| 高 | `templates/checkout/index.html` | `cart/checkout.jsp` |
| 中 | `templates/orders/list.html` | `orders/history.jsp` |
| 中 | `templates/orders/detail.html` | `orders/detail.jsp` |
| 中 | `templates/home.html` | `home.jsp` |
| 中 | `templates/account/addresses.html` | `account/addresses.jsp` |
| 中 | `templates/account/points.html` | `points/balance.jsp` |
| 低 | `templates/admin/**` | `admin/**/*.jsp` |
| 低 | `templates/coupons/available.html` | `coupons/available.jsp` |

### 8-C: 静的リソース移行

| 現行パス | 移行後パス |
|---------|-----------|
| `src/main/webapp/assets/` | `src/main/resources/static/` |

Content-Type ヘッダーを確認し、CSS/JS/画像ファイルを適切に配置。

### Thymeleaf 変換確認事項

- [ ] `th:action="@{/...}"` で URL を動的生成（ハードコード URL 禁止）
- [ ] `th:text="${...}"` でデフォルト XSS エスケープが有効
- [ ] `th:href="@{/products/{id}(id=${p.id})}"` でパスパラメータを安全に展開
- [ ] `th:each` で繰り返し処理
- [ ] `th:errors` でフォームバリデーションエラー表示
- [ ] `th:with` で Thymeleaf Security Integration（`${#authentication.name}`）
- [ ] CSRF トークンが `<form>` に自動挿入されることを確認
- [ ] `#{...}` で messages.properties から文字列取得

### 完了条件
- [ ] 全テンプレートのコンパイル（Thymeleaf パース）成功
- [ ] 統合テストで全画面の HTTP 200 レスポンス確認
- [ ] XSS テスト：ユーザー入力値が正しくエスケープされる
- [ ] CSRF テスト：POST フォームにトークンが含まれる
- [ ] 静的リソース（CSS/JS）が正しく配信される

---

## 9. フェーズ 7: セキュリティ統合

### 目的
Spring Security を完全統合し、認証・認可・セッション管理・CSRF 保護を確立する。

### 作業項目

#### 7-1: SecurityConfig 実装

設計書 §11.1 の設定を実装する。

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // 設計書に記載の設定を実装
}
```

#### 7-2: UserDetailsService 実装

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())  // DelegatingPasswordEncoder 形式
            .roles(user.getRole())
            .accountLocked("LOCKED".equals(user.getStatus()))
            .build();
    }
}
```

#### 7-3: パスワードエンコーダー設定

設計書 §11.2 の `DelegatingPasswordEncoder` 設定を実装する。

以下の 3 点をこのステップで実装する:

**① `LegacySha256PasswordEncoder` クラスの作成** (`util/LegacySha256PasswordEncoder.java`):

設計書 §11.2 のコード例をそのまま実装する。`PasswordHasher.hash(password, salt)` と比較する `matches()` メソッドを実装し、`encode()` は `UnsupportedOperationException` をスローする。

**② `CustomUserDetailsService` に `UserDetailsPasswordService` を実装**:

`implements UserDetailsService, UserDetailsPasswordService` とし、`updatePassword()` メソッドを実装する。Spring Security が BCrypt アップグレード後に自動コールする。

**③ 既存 DB データへのプレフィックス + ソルト付与（Flyway V2 実行）**:
```sql
-- Flyway: V2__add_password_prefix.sql
-- ハッシュとソルトを {sha256}<hash>$<salt> 形式に変換
UPDATE users SET password_hash = CONCAT('{sha256}', password_hash, '$', salt)
WHERE password_hash NOT LIKE '{%}%';
```

#### 7-4: セキュリティヘッダー設定

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.enable())
);
```

#### 7-5: RequestId フィルター移植

現行の `RequestIdFilter` を `OncePerRequestFilter` として実装する。

```java
@Component
public class RequestIdFilter extends OncePerRequestFilter {
    private static final String HEADER_NAME = "X-Request-Id";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("reqId", requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("reqId");
        }
    }
}
```

#### 7-6: `@EnableMethodSecurity` + `@PreAuthorize` の実装

`SecurityConfig` に `@EnableMethodSecurity` を付加し、管理者封の Service メソッドに `@PreAuthorize("hasRole('ADMIN')")` を付与する。URL ベースの認可と メソッドベースの認可の両層で多層防御を実現する。

```java
// 例: AdminProductService
@Service
public class AdminProductService {
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product updateProduct(String id, AdminProductRequest request) { ... }
}
```

### 完了条件
- [ ] 未認証ユーザーが認証必須 URL にアクセスした場合、ログインページにリダイレクト
- [ ] USER ロールが ADMIN 専用 URL にアクセスした場合、403 を返す
- [ ] POST フォームに CSRF トークンが含まれ、不正なトークンで 403 を返す
- [ ] セッション固定攻撃テスト：ログイン前後でセッション ID が変わる
- [ ] ブルートフォーステスト：5 回失敗でアカウントがロックされる
- [ ] パスワード移行後の認証テスト（SHA-256 存在ユーザーが`{sha256}`プレフィックスでログインできる）
- [ ] パスワードテスト：ログイン成功後 BCrypt へ自動アップグレードされることを確認
- [ ] Spring Security Test で認証・認可のユニットテスト通過

---

## 10. フェーズ 8: テスト実装・品質確認

### 目的
各レイヤーのテストを実装し、カバレッジ目標を達成する。

### 作業項目

#### 8-1: テスト基盤構築

```java
// テスト共通設定
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationTest {
    // 共通セットアップ
}
```

#### 8-2: サービステスト（JUnit 5 + Mockito）

重要サービスのユニットテスト:
- [ ] `AuthServiceTest`: 認証成功・失敗・アカウントロック
- [ ] `CartServiceTest`: カート操作 CRUD
- [ ] `CheckoutServiceTest`: 注文確定トランザクション（11 ステップ全てのロールバック検証を含む）
- [ ] `OrderServiceTest`: 注文確定トランザクション
- [ ] `CouponServiceTest`: クーポン検証ロジック
- [ ] `PointServiceTest`: ポイント加算・使用
- [ ] `LegacySha256PasswordEncoderTest`: 知った SHA-256 ハッシュ+ソルトで `matches()` の成否を検証

#### 8-3: リポジトリテスト（@DataJpaTest）

- [ ] `UserRepositoryTest`: findByEmail, countByStatus
- [ ] `ProductRepositoryTest`: 動的検索（Specification）
- [ ] `OrderRepositoryTest`: 注文履歴取得

#### 8-4: コントローラーテスト（@WebMvcTest）

- [ ] `AuthControllerTest`: ログイン/ログアウト/会員登録/バリデーション
- [ ] `ProductControllerTest`: 商品一覧・詳細
- [ ] `CartControllerTest`: カート操作（認証が必要なエンドポイント含む）
- [ ] `CheckoutControllerTest`: チェックアウト（認証必須）

#### 8-5: 統合テスト（@SpringBootTest）

主要シナリオの E2E テスト:
- [ ] 新規会員登録 → ログイン → 商品選択 → カート追加 → 注文確定
- [ ] 未ログインカート → ログイン → カートマージ（セッションカートがユーザーカートに統合されることを確認）
- [ ] 管理者ログイン → 商品管理 → クーポン管理

#### 8-6: セキュリティテスト

- [ ] 未認証アクセスのリダイレクト確認
- [ ] CSRF 保護動作確認
- [ ] XSS ペイロードがエスケープされることを確認

#### 8-7: カバレッジ計測

```bash
mvn clean verify -Djacoco.skip=false -pl appmod-migrated-java21-spring-boot-3rd
```

目標:
- Service: 80% 以上
- Controller: 70% 以上
- 全体: 70% 以上

### 完了条件
- [ ] `mvn clean test` が 100% 通過
- [ ] カバレッジ目標達成（全体 70% 以上）
- [ ] セキュリティテスト全件通過

---

## 11. フェーズ 9: 最終検証・リリース準備

### 目的
本番環境想定での動作確認と、リリースに必要な成果物を準備する。

### 作業項目

#### 9-1: Docker 環境確認

```bash
docker compose up -d  # PostgreSQL 起動
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Xmx512m"
```

#### 9-2: DB 連携統合テスト

Spring Boot Test + PostgreSQL（TestContainers または ローカル DB）での統合テスト。

#### 9-3: 機能チェックリスト確認

DESIGN.md §2.3 の全 Action に対してシナリオテストを実施（[品質チェックリスト](#13-品質チェックリスト) 参照）。

#### 9-4: パフォーマンステスト

- 商品一覧 API：同時 10 ユーザー × 30 秒のロードテスト
- 注文確定 API：同時 5 ユーザー × 10 秒のロードテスト
- 目標：P95 レスポンス = DESIGN.md §14.1 の基準値以内

#### 9-5: セキュリティスキャン

OWASP Dependency Check:
```bash
mvn org.owasp:dependency-check-maven:check -pl appmod-migrated-java21-spring-boot-3rd
```

#### 9-6: Dockerfile 確認

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# 非ルートユーザーで実行（コンテナセキュリティ対策必須）
RUN addgroup -S skishop && adduser -S skishop -G skishop

WORKDIR /app
COPY target/skishop-app-*.jar app.jar
RUN chown skishop:skishop app.jar
USER skishop

# Java 21 コンテナ対応 JVM フラグ（-Xmx ハードコードよりも %指定準拠）
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
```

#### 9-7: README 更新

移行後プロジェクトの `README.md` に以下を記載:
- 起動手順（環境変数設定含む）
- テスト実行方法
- Docker Compose での起動方法
- 環境変数一覧（`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, etc.）

### 完了条件
- [ ] `mvn clean verify` が成功（全テスト通過・カバレッジ閾値クリア）
- [ ] Docker イメージのビルドと起動成功
- [ ] 本番プロファイルでの起動成功（`spring.profiles.active=prod` + 環境変数設定）
- [ ] OWASP Dependency Check で Critical CVE なし
- [ ] README に必要な起動情報が記載されている

---

## 12. リスクと対策

| リスク番号 | リスク | 影響度 | 発生確率 | 対策 |
|-----------|------|--------|---------|------|
| R-01 | パスワードハッシュ非互換（SHA-256 → BCrypt） | 高 | 確定 | `DelegatingPasswordEncoder` で段階移行 |
| R-02 | セッション属性の設計変更（`loginUser` → `UserDetails`） | 中 | 高 | 全テンプレートで `#authentication.principal` を使用するよう統一 |
| R-03 | URL 変更によるリンク切れ（`*.do` → RESTful） | 中 | 高 | H2 DB + Nginx リダイレクト / Spring の RedirectView で旧 URL を転送 |
| R-04 | JPA N+1 問題（LAZY フェッチの過剰発動） | 中 | 中 | `@EntityGraph` / `JOIN FETCH` で対応。スロークエリログで検出 |
| R-05 | Thymeleaf + CSRF：非 HTML フォームでの CSRF エラー | 中 | 中 | AJAX POST には `X-CSRF-TOKEN` ヘッダーを付与する JS コードを実装 |
| R-06 | メール送信の設定変更（javax.mail → jakarta.mail） | 低 | 確定 | Spring Boot 自動設定で透過的に対応済み |
| R-07 | `commons-fileupload` → Spring Multipart の動作差異 | 低 | 低 | ファイルアップロード機能があれば Spring `MultipartFile` に変換 |
| R-08 | H2 と PostgreSQL の方言差異によるテスト失敗 | 中 | 中 | `@DataJpaTest` では `MODE=PostgreSQL` を設定。重要な SQL は PostgreSQL でも確認 |
| R-09 | Log4j MDC → SLF4J MDC の API 差異 | 低 | 低 | API 名はほぼ同じ（`MDC.put()` / `MDC.remove()`）。Logback 設定で `%X{reqId}` を参照 |
| R-10 | 移行後の機能漏れ | 高 | 中 | 品質チェックリスト §13 を全 Action 対して実施 |

---

## 13. 品質チェックリスト

### 13-A: コード品質チェック

#### セキュリティ（Critical）
- [ ] 設定ファイルにパスワード/API キーが直接記述されていない
- [ ] `@Valid` が全 Controller の入力 DTO に付与されている
- [ ] SQL 文字列結合が存在しない（`grep -r "\"SELECT.*+\|\"UPDATE.*+"` で確認）
- [ ] XSS：Thymeleaf の `th:text` を使用（`th:utext` は必要最小限）
- [ ] CSRF：全 POST フォームにトークンが含まれる
- [ ] Spring Security の認可設定が全 URL をカバーしている

#### アーキテクチャ（High）
- [ ] Controller → Repository の直接参照がない（Controller は Service のみ呼び出す）
- [ ] `new` による Service/Repository 生成が存在しない
- [ ] 循環依存が存在しない（`mvn dependency:tree` で確認）
- [ ] `@Transactional` が Service 層のみに付与されている（Controller に付与禁止）

#### コーディング規約（Medium）
- [ ] 命名規則：PascalCase（クラス）、camelCase（メソッド・変数）、UPPER_SNAKE_CASE（定数）
- [ ] `java.util.Date` が使用されていない（`java.time.*` を使用）
- [ ] `@SuppressWarnings("unchecked")` が不適切に使用されていない
- [ ] `public static void main(void)` パターンを除き、Singleton パターンが Spring DI で代替されている

### 13-B: 機能等価性チェック（全 Action 対応）

#### 認証系
- [ ] ログイン正常系（email/パスワード一致、ダッシュボードへリダイレクト）
- [ ] ログイン異常系（パスワード不一致、エラーメッセージ表示）
- [ ] アカウントロック（5 回失敗後、ログイン不可）
- [ ] ログアウト（セッション無効化、ホームへリダイレクト）
- [ ] 会員登録（新規ユーザー作成、DB 登録確認）
- [ ] 重複メール登録（エラーメッセージ表示）
- [ ] パスワード再発行（メール送信確認）
- [ ] パスワードリセット（有効トークン、パスワード変更確認）

#### 商品系
- [ ] 商品一覧（全商品表示、カテゴリフィルタ、キーワード検索）
- [ ] 商品詳細（商品情報・価格・在庫表示）
- [ ] 存在しない商品 ID（404 または適切なエラー画面）

#### カート系
- [ ] カート追加（商品数量入力、DB 登録確認）
- [ ] カート表示（合計金額計算正確性）
- [ ] カート数量変更
- [ ] カートアイテム削除
- [ ] クーポン適用（有効/無効/使用済みクーポン）
- [ ] チェックアウト画面（配送先・支払い方法選択）
- [ ] 注文確定（DB に注文レコード作成、在庫減算確認）

#### 注文系
- [ ] 注文履歴（ログインユーザーの注文一覧）
- [ ] 注文詳細（注文明細・ステータス表示）
- [ ] 注文キャンセル（キャンセル可否条件確認）
- [ ] 返品申請

#### アカウント系
- [ ] ポイント残高表示
- [ ] 住所一覧表示
- [ ] 住所追加・更新

#### 管理者系
- [ ] 管理商品一覧・編集・削除
- [ ] 管理注文一覧・詳細・ステータス更新・返金
- [ ] 管理クーポン一覧・編集
- [ ] 管理配送方法一覧・編集
- [ ] 管理ユーザー一覧（ロック・アンロック操作サポートがあれば含む）

#### 認可制御
- [ ] 未ログインユーザーが `/orders` にアクセス → ログインページへリダイレクト
- [ ] USER ロールが `/admin/**` にアクセス → 403
- [ ] ADMIN ロールが全 URL にアクセス可能
- [ ] `@PreAuthorize` によるメソッドレベル認可が URL 変更後も有効か
- [ ] Actuator エンドポイント（`/health` 以外）が未認証でアクセス不可能か

### 13-C: 非機能チェック

- [ ] H2（テスト）での `mvn test` 全件通過
- [ ] PostgreSQL（開発）での正常動作確認
- [ ] カバレッジ: Service 80%+, Controller 70%+, 全体 70%+
- [ ] OWASP Dependency Check で Critical CVE なし
- [ ] Logback で `%X{reqId}` がログに出力される
- [ ] `/actuator/health` が `UP` を返す
- [ ] `/actuator/**`（health 以外）が未認証アクセスで 401/403 を返す
- [ ] Docker イメージビルド成功
- [ ] Flyway マイグレーションが正常に適用される（`flyway_schema_history` 確認）
- [ ] PII（メールアドレス・住所・パスワード）がログに出力されていない

---

## 14. 完了判定基準

以下の**全条件**を満たした時点で移行完了とする。

| 番号 | 判定条件 | 検証方法 |
|------|---------|---------|
| C-1 | `mvn clean test` が 100% 通過 | CI コンソール確認 |
| C-2 | 全体テストカバレッジ 70% 以上 | JaCoCo レポート |
| C-3 | §13-B の全シナリオが手動テストで確認済み | チェックリスト完了 |
| C-4 | OWASP Dependency Check で Critical CVE なし | レポート確認 |
| C-5 | Docker イメージのビルド・起動成功 | `docker run` 確認 |
| C-6 | Spring Security 認証・認可が全 URL に適用 | セキュリティテスト通過 |
| C-7 | 設定ファイルに機密情報が存在しない | コードレビュー確認 |
| C-8 | `new` による Service/Repository 生成が存在しない | `grep -r "= new"` 確認 |
| C-9 | Architect レビュー完了（設計原則遵守） | レビューコメント解決済み |
| C-10 | QA マネージャーによるシナリオテスト完了 | 品質レポート |

---

## 15. ロールバック計画

### ロールバックトリガー条件

以下のいずれかが発生した場合、移行を中止してロールバックを検討する:
- フェーズ 1〜7 完了後もコンパイルエラーが解消されない
- セキュリティ上の重大問題（認証バイパス等）が発見された
- DB スキーマの非互換性により既存データが破損する

### ロールバック手順

1. **新規ディレクトリのため元プロジェクトへの影響なし**: `appmod-migrated-java21-spring-boot-3rd/` は別ディレクトリで作成するため、元の `skishop-monolith` への影響はない
2. **移行先ディレクトリを削除**: `rm -rf appmod-migrated-java21-spring-boot-3rd/`
3. **Git でのブランチ廃棄**: `git branch -D migration/spring-boot-v3`
4. **根本原因を分析し、設計書を更新**してから再挑戦

### DB ロールバック

本移行ではスキーマ変更を最小化しているが、`DelegatingPasswordEncoder` のプレフィックス追加 SQL を実行した場合:
```sql
-- ロールバック SQL（パスワードハッシュプレフィックスとソルト埋め込みを除去してハッシュのみ復元）
UPDATE users
SET password_hash = split_part(substring(password_hash from length('{sha256}') + 1), '$', 1)
WHERE password_hash LIKE '{sha256}%';
```

---

## 付録: 作業チェックシート

### フェーズ進捗トラッカー

| フェーズ | 開始日 | 完了日 | ステータス | 担当者 |
|---------|--------|--------|-----------|--------|
| Phase 0: 事前準備 | | | ⬜️ 未開始 | |
| Phase 1: 基盤構築 | | | ⬜️ 未開始 | |
| Phase 2: ドメインモデル | | | ⬜️ 未開始 | |
| Phase 3: リポジトリ層 | | | ⬜️ 未開始 | |
| Phase 4: サービス層 | | | ⬜️ 未開始 | |
| Phase 5: Web 層 | | | ⬜️ 未開始 | |
| Phase 6: ビュー層 | | | ⬜️ 未開始 | |
| Phase 7: セキュリティ | | | ⬜️ 未開始 | |
| Phase 8: テスト | | | ⬜️ 未開始 | |
| Phase 9: 最終検証 | | | ⬜️ 未開始 | |

**ステータス凡例**: ⬜️ 未開始 | 🔄 進行中 | ✅ 完了 | ❌ 問題あり
