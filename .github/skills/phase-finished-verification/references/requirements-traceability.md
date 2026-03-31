# 要件トレーサビリティマトリクス

Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x 移行における、全移行コンポーネントが
どのフェーズで実装され、どのファイルに対応するかを定義する。
検証時はこのマトリクスを参照し、移行漏れがないことを確認する。

> **詳細**: 全 29 Action の URL・ロール・移行先 Controller の完全一覧は `docs/migration/DESIGN.md §2.3` を参照。
> 全 20 Repository の追加メソッド定義は `docs/migration/PLAN.md §5` を参照。

---

## 1. フェーズ別 移行スコープ一覧

| Phase | 主要移行対象 | 移行数 | 検証コマンド |
|-------|-----------|--------|------------|
| Phase 0 | 事前準備・ベースライン記録 | — | 手動確認 |
| Phase 1 | pom.xml・設定ファイル・パッケージ構成 | — | `mvn clean compile` |
| Phase 2 | Domain POJO → JPA @Entity | 22 エンティティ | `@DataJpaTest` |
| Phase 3 | DAO → Spring Data JPA Repository | 20 リポジトリ | `mvn test -Dtest="*RepositoryTest"` |
| Phase 4 | Service 移行・CheckoutService 新設 | 14 サービス | `mvn test -Dtest="*ServiceTest"` |
| Phase 5 | Action → Controller、ActionForm → record DTO | 29 Action → 8 Controller、12 DTO | `mvn test -Dtest="*ControllerTest"` |
| Phase 6 | JSP + Tiles → Thymeleaf + Layout Dialect | 30+ テンプレート | HTTP 200 全画面確認 |
| Phase 7 | Spring Security 統合・パスワード移行 | 認証/認可設定 | Spring Security Test |
| Phase 8 | テスト実装・カバレッジ確認 | — | `mvn clean verify -Djacoco.skip=false` |
| Phase 9 | Docker・本番設定・OWASP チェック | — | `mvn clean verify` |

---

## 2. Struts Action → Spring Controller 移行マトリクス（Phase 5）

**移行ルール**: 29 Action を 8 Controller に集約する。URL から `*.do` を排除し、RESTful URL に変換する。

> **完全一覧**: `docs/migration/DESIGN.md §2.3` に全 29 Action の詳細が記載されている。以下は主要なマッピングを抜粋。

### 2.1 AuthController（`/auth/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `LoginAction` | `/login.do` | `AuthController#showLogin` | `/auth/login` | GET |
| `LoginAction` (POST) | `/login.do` | `AuthController#login` | `/auth/login` | POST |
| `LogoutAction` | `/logout.do` | Spring Security | `/auth/logout` | POST |
| `PasswordResetAction` | `/password/reset.do` | `AuthController#showPasswordReset` | `/auth/password/reset` | GET |
| `PasswordResetAction` (POST) | `/password/reset.do` | `AuthController#requestPasswordReset` | `/auth/password/reset` | POST |
| `PasswordChangeAction` | `/password/change.do` | `AuthController#changePassword` | `/auth/password/change` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AuthController.java`

```bash
grep -n "@GetMapping\|@PostMapping\|@RequestMapping" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AuthController.java
```

### 2.2 ProductController（`/products/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `ProductListAction` | `/products.do` | `ProductController#list` | `/products` | GET |
| `ProductDetailAction` | `/product.do?id=xxx` | `ProductController#detail` | `/products/{id}` | GET |
| `ProductSearchAction` | `/search.do` | `ProductController#search` | `/products/search` | GET |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/ProductController.java`

### 2.3 CartController（`/cart/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `CartViewAction` | `/cart.do` | `CartController#view` | `/cart` | GET |
| `CartAddAction` | `/cart/add.do` | `CartController#addItem` | `/cart/items` | POST |
| `CartUpdateAction` | `/cart/update.do` | `CartController#updateItem` | `/cart/items/{itemId}` | POST |
| `CartRemoveAction` | `/cart/remove.do` | `CartController#removeItem` | `/cart/items/{itemId}/remove` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/CartController.java`

### 2.4 CheckoutController（`/checkout/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `CheckoutViewAction` | `/checkout.do` | `CheckoutController#view` | `/checkout` | GET |
| `CheckoutConfirmAction` | `/checkout/confirm.do` | `CheckoutController#confirm` | `/checkout/confirm` | POST |
| `CheckoutCompleteAction` | `/checkout/complete.do` | `CheckoutController#complete` | `/checkout/complete` | GET |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/CheckoutController.java`

### 2.5 OrderController（`/orders/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `OrderListAction` | `/orders.do` | `OrderController#list` | `/orders` | GET |
| `OrderDetailAction` | `/orders/detail.do?id=xxx` | `OrderController#detail` | `/orders/{id}` | GET |
| `OrderCancelAction` | `/orders/cancel.do` | `OrderController#cancel` | `/orders/{id}/cancel` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/OrderController.java`

**IDOR 防止確認**:
```bash
grep -n "findByIdAndUserId\|@AuthenticationPrincipal" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/OrderController.java
```

### 2.6 AccountController（`/account/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `AccountViewAction` | `/account.do` | `AccountController#view` | `/account` | GET |
| `AccountEditAction` | `/account/edit.do` | `AccountController#update` | `/account` | POST |
| `AddressListAction` | `/account/addresses.do` | `AccountController#addresses` | `/account/addresses` | GET |
| `AddressEditAction` | `/account/address/edit.do` | `AccountController#updateAddress` | `/account/addresses/{id}` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AccountController.java`

### 2.7 AdminProductController（`/admin/products/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `AdminProductListAction` | `/admin/products.do` | `AdminProductController#list` | `/admin/products` | GET |
| `AdminProductCreateAction` | `/admin/product/create.do` | `AdminProductController#showCreate` | `/admin/products/new` | GET |
| `AdminProductCreateAction` (POST) | `/admin/product/create.do` | `AdminProductController#create` | `/admin/products` | POST |
| `AdminProductEditAction` | `/admin/product/edit.do` | `AdminProductController#showEdit` | `/admin/products/{id}` | GET |
| `AdminProductEditAction` (POST) | `/admin/product/edit.do` | `AdminProductController#update` | `/admin/products/{id}` | POST |
| `AdminProductDeleteAction` | `/admin/product/delete.do` | `AdminProductController#delete` | `/admin/products/{id}` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AdminProductController.java`

**@PreAuthorize 確認**:
```bash
grep -n "@PreAuthorize" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AdminProductController.java
```

### 2.8 AdminOrderController（`/admin/orders/`）

| 移行元 Struts Action | 移行元 URL | 移行後 Controller#メソッド | 移行後 URL | HTTP |
|---------------------|-----------|--------------------------|-----------|------|
| `AdminOrderListAction` | `/admin/orders.do` | `AdminOrderController#list` | `/admin/orders` | GET |
| `AdminOrderDetailAction` | `/admin/orders/detail.do` | `AdminOrderController#detail` | `/admin/orders/{id}` | GET |
| `AdminOrderStatusAction` | `/admin/orders/status.do` | `AdminOrderController#updateStatus` | `/admin/orders/{id}/status` | POST |

**確認ファイル**: `appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/AdminOrderController.java`

---

## 3. ActionForm → DTO (record) 移行マトリクス（Phase 5）

**移行ルール**: 全 ActionForm を Bean Validation アノテーション付き Java `record` に変換する。
`validate()` メソッドのロジックは `@NotBlank` / `@Email` / `@Size` 等に置換する。

| 移行元 ActionForm | 移行後 record DTO | バリデーション | 確認ファイルパス |
|-----------------|-----------------|-------------|--------------|
| `LoginForm` | `LoginRequest` | `@NotBlank @Email @Size(max=255)` (email), `@NotBlank @Size(min=8,max=100)` (password) | `dto/request/LoginRequest.java` |
| `RegisterForm` | `RegisterRequest` | email + password + `@NotBlank` (name) | `dto/request/RegisterRequest.java` |
| `ProductSearchForm` | `ProductSearchRequest` | `@Size(max=200)` (keyword), category | `dto/request/ProductSearchRequest.java` |
| `CartAddForm` | `CartAddRequest` | `@NotBlank` (productId), `@Min(1) @Max(99)` (quantity) | `dto/request/CartAddRequest.java` |
| `CartUpdateForm` | `CartUpdateRequest` | `@Min(1) @Max(99)` (quantity) | `dto/request/CartUpdateRequest.java` |
| `CheckoutForm` | `CheckoutRequest` | address fields, couponCode | `dto/request/CheckoutRequest.java` |
| `AccountEditForm` | `AccountUpdateRequest` | `@NotBlank @Size(max=100)` (name) | `dto/request/AccountUpdateRequest.java` |
| `PasswordChangeForm` | `PasswordChangeRequest` | `@NotBlank @Size(min=8,max=100)` | `dto/request/PasswordChangeRequest.java` |
| `AddressForm` | `AddressRequest` | address fields, `@NotBlank` | `dto/request/AddressRequest.java` |
| `AdminProductForm` | `AdminProductRequest` | `@NotBlank @Size(max=200)` (name), `@Positive` (price) | `dto/request/AdminProductRequest.java` |
| `AdminOrderStatusForm` | `AdminOrderStatusRequest` | `@NotBlank` (status) | `dto/request/AdminOrderStatusRequest.java` |
| `CouponApplyForm` | `CouponApplyRequest` | `@NotBlank @Size(max=50)` (couponCode) | `dto/request/CouponApplyRequest.java` |

**一括確認コマンド**:
```bash
# record クラスの数を確認（12 件あるか）
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/dto/request/ \
  -name "*.java" | xargs grep -l "^public record" | wc -l

# @Valid が Controller に漏れなく付与されているか
grep -rn "@Valid" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/controller/
```

---

## 4. DAO → Repository 移行マトリクス（Phase 3）

**移行ルール**: 全 DAO 実装クラスを `JpaRepository<Entity, String>` を extends する interface に置換する。
実装クラス（`*DaoImpl.java`）は不要。Spring Data JPA が自動生成する。

| 移行元 DAO | 移行後 Repository | 主要追加メソッド | 確認ファイルパス |
|-----------|-----------------|---------------|--------------|
| `UserDao` / `UserDaoImpl` | `UserRepository` | `findByEmail`, `findByStatus`, `findByEmailAndStatus` | `repository/UserRepository.java` |
| `ProductDao` / `ProductDaoImpl` | `ProductRepository` | `findByCategory`, `findByStatus`, `JpaSpecificationExecutor` (動的検索) | `repository/ProductRepository.java` |
| `CategoryDao` / `CategoryDaoImpl` | `CategoryRepository` | `findByParentIsNull`, `findByStatus` | `repository/CategoryRepository.java` |
| `CartDao` / `CartDaoImpl` | `CartRepository` | `findByUserId`, `findBySessionId` | `repository/CartRepository.java` |
| `CartItemDao` / `CartItemDaoImpl` | `CartItemRepository` | `findByCartId`, `deleteByCartId` | `repository/CartItemRepository.java` |
| `OrderDao` / `OrderDaoImpl` | `OrderRepository` | `findByUserId`, `findByIdAndUserId`, `findByStatus` | `repository/OrderRepository.java` |
| `OrderItemDao` / `OrderItemDaoImpl` | `OrderItemRepository` | `findByOrderId` | `repository/OrderItemRepository.java` |
| `PaymentDao` / `PaymentDaoImpl` | `PaymentRepository` | `findByOrderId`, `findByStatus` | `repository/PaymentRepository.java` |
| `ShipmentDao` / `ShipmentDaoImpl` | `ShipmentRepository` | `findByOrderId`, `findByStatus` | `repository/ShipmentRepository.java` |
| `AddressDao` / `AddressDaoImpl` | `AddressRepository` | `findByUserId`, `findByIdAndUserId` | `repository/AddressRepository.java` |
| `PointDao` / `PointDaoImpl` | `PointRepository` | `findByUserId` (残高), `findExpiringBefore` | `repository/PointRepository.java` |
| `PointHistoryDao` / `PointHistoryDaoImpl` | `PointHistoryRepository` | `findByUserId`, `findByUserIdAndCreatedAtBetween` | `repository/PointHistoryRepository.java` |
| `CouponDao` / `CouponDaoImpl` | `CouponRepository` | `findByCode`, `findByStatus`, `findAvailableByUserId` | `repository/CouponRepository.java` |
| `CouponUsageDao` / `CouponUsageDaoImpl` | `CouponUsageRepository` | `findByCouponIdAndUserId`, `countByCouponId` | `repository/CouponUsageRepository.java` |
| `SecurityLogDao` / `SecurityLogDaoImpl` | `SecurityLogRepository` | `countByUserIdAndEventTypeAndCreatedAtAfter`, `findByUserId` | `repository/SecurityLogRepository.java` |
| `StockDao` / `StockDaoImpl` | `StockRepository` | `findByProductId`, `findLowStock` | `repository/StockRepository.java` |
| `TaxDao` / `TaxDaoImpl` | `TaxRepository` | `findByActiveTrue` | `repository/TaxRepository.java` |
| `EmailQueueDao` / `EmailQueueDaoImpl` | `EmailQueueRepository` | `findByStatus`, `findPendingByCreatedAtBefore` | `repository/EmailQueueRepository.java` |
| `ReturnDao` / `ReturnDaoImpl` | `ReturnRepository` | `findByOrderId`, `findByUserId`, `findByStatus` | `repository/ReturnRepository.java` |
| `AuditLogDao` / `AuditLogDaoImpl` | `AuditLogRepository` | `findByEntityTypeAndEntityId` | `repository/AuditLogRepository.java` |

**一括確認コマンド**:
```bash
# Repository インターフェースの数を確認（20 件あるか）
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/ \
  -name "*.java" | wc -l

# 全 Repository が JpaRepository を extends しているか
grep -rn "extends JpaRepository" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/

# UserRepository に SecurityLog のクエリが混在していないか（禁止）
grep -n "SecurityLog\|security_log" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/UserRepository.java

# 文字列結合 SQL が存在しないか
grep -rn '"SELECT\|"UPDATE\|"INSERT\|"DELETE' \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/repository/
```

---

## 5. Service 移行マトリクス（Phase 4）

**移行ルール**: 全 Service を `@Service` + コンストラクタインジェクションで再実装する。
`new` による Repository 生成は禁止。`CheckoutService` は新設。

| 移行元 Service | 移行後 @Service | 主要変更点 | 確認ファイルパス |
|--------------|----------------|---------|--------------|
| `AuthService` | `AuthService` | Spring Security + `UserDetailsService` 実装 | `service/AuthService.java` |
| `UserService` | `UserService` | `@Transactional` メソッド分離 | `service/UserService.java` |
| `ProductService` | `ProductService` | `@Transactional(readOnly = true)` | `service/ProductService.java` |
| `CategoryService` | `CategoryService` | `@Transactional(readOnly = true)` | `service/CategoryService.java` |
| `CartService` | `CartService` | `getOrCreateCart()`, `getActiveCart()`, `mergeSessionCart()` | `service/CartService.java` |
| `OrderService` | `OrderService` | `@Transactional` / IDOR 検証 | `service/OrderService.java` |
| `PaymentService` | `PaymentService` | `@Transactional` | `service/PaymentService.java` |
| `ShipmentService` | `ShipmentService` | `@Transactional` | `service/ShipmentService.java` |
| `PointService` | `PointService` | `reservePoints()`, `awardPoints()` を分離 | `service/PointService.java` |
| `CouponService` | `CouponService` | `validateCoupon()` + `discountAmount()` | `service/CouponService.java` |
| `EmailService` | `EmailService` | `EmailQueue` を同一 TX でキューイング | `service/EmailService.java` |
| `StockService` | `StockService` | `checkStock()`, `deductStock()` を分離 | `service/StockService.java` |
| `AdminService` | `AdminProductService` | `@PreAuthorize("hasRole('ADMIN')")` | `service/AdminProductService.java` |
| *(新設)* | `CheckoutService` | 注文確定 11 ステップを単一 `@Transactional` で原子化 | `service/CheckoutService.java` |

**CheckoutService の 11 ステップ確認**:
```bash
grep -n "// Step\|1\.\|2\.\|3\.\|4\.\|5\.\|6\.\|7\.\|8\.\|9\.\|10\.\|11\." \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/service/CheckoutService.java
```

**一括確認コマンド**:
```bash
# Service クラスの数を確認（14 件あるか）
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/service/ \
  -name "*Service.java" | wc -l

# 全 Service に @Service が付与されているか
grep -rn "@Service" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/service/

# new による Repository 生成がないか
grep -rn "= new.*Repository\|= new.*Service" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/service/
```

---

## 6. Domain POJO → JPA Entity 移行マトリクス（Phase 2）

**移行ルール**: 全 POJO に `@Entity` + `@Table(name="...")` を付与。
`java.util.Date` を `java.time.LocalDateTime` / `LocalDate` に変換。
UUID PK に `@GeneratedValue` は付与しない（Service 層で生成）。

| 移行元 POJO | 移行後 @Entity | DB テーブル名 | PK 型 | 関連（LAZY） | 確認ファイルパス |
|-----------|--------------|-----------|-------|------------|--------------|
| `User` | `User` | `users` | `String` (UUID) | orders, addresses, cart | `model/User.java` |
| `Product` | `Product` | `products` | `String` (UUID) | category, stockItems | `model/Product.java` |
| `Category` | `Category` | `categories` | `String` (UUID) | parent, children, products | `model/Category.java` |
| `Cart` | `Cart` | `carts` | `String` (UUID) | user, items | `model/Cart.java` |
| `CartItem` | `CartItem` | `cart_items` | `String` (UUID) | cart, product | `model/CartItem.java` |
| `Order` | `Order` | `orders` | `String` (UUID) | user, items, payment, shipment | `model/Order.java` |
| `OrderItem` | `OrderItem` | `order_items` | `String` (UUID) | order, product | `model/OrderItem.java` |
| `Payment` | `Payment` | `payments` | `String` (UUID) | order | `model/Payment.java` |
| `Shipment` | `Shipment` | `shipments` | `String` (UUID) | order | `model/Shipment.java` |
| `Address` | `Address` | `addresses` | `String` (UUID) | user | `model/Address.java` |
| `Point` | `Point` | `points` | `String` (UUID) | user | `model/Point.java` |
| `PointHistory` | `PointHistory` | `point_histories` | `String` (UUID) | user | `model/PointHistory.java` |
| `Coupon` | `Coupon` | `coupons` | `String` (UUID) | campaign, usages | `model/Coupon.java` |
| `CouponUsage` | `CouponUsage` | `coupon_usages` | `String` (UUID) | coupon, user | `model/CouponUsage.java` |
| `SecurityLog` | `SecurityLog` | `security_logs` | `String` (UUID) | — | `model/SecurityLog.java` |
| `Stock` | `Stock` | `stocks` | `String` (UUID) | product | `model/Stock.java` |
| `Tax` | `Tax` | `taxes` | `String` (UUID) | — | `model/Tax.java` |
| `EmailQueue` | `EmailQueue` | `email_queues` | `String` (UUID) | — | `model/EmailQueue.java` |
| `Return` | `Return` | `returns` | `String` (UUID) | order, user | `model/Return.java` |
| `AuditLog` | `AuditLog` | `audit_logs` | `String` (UUID) | — | `model/AuditLog.java` |
| `Campaign` | `Campaign` | `campaigns` | `String` (UUID) | coupons | `model/Campaign.java` |
| `Tier` | `Tier` | `tiers` | `String` (UUID) | — | `model/Tier.java` |

**一括確認コマンド**:
```bash
# Entity クラスの数を確認（22 件あるか）
find appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/model/ \
  -name "*.java" | wc -l

# 全 Entity に @Entity と @Table が付与されているか
grep -rL "@Entity" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/model/

# java.util.Date が残存していないか
grep -rn "java\.util\.Date\|import java\.util\.Date" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/model/

# @GeneratedValue が UUID PK に付与されていないか（禁止）
grep -rn "@GeneratedValue" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/model/

# @Column(name="...") が全フィールドに付与されているか（一部をサンプル確認）
grep -c "@Column" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/model/User.java
```

---

## 7. JSP → Thymeleaf テンプレート移行マトリクス（Phase 6）

**移行ルール**: 全 JSP を Thymeleaf テンプレートに変換。Tiles レイアウトは `thymeleaf-layout-dialect` の `layout:decorate` に置換。
`th:text` を使用し `th:utext`（XSS リスク）は禁止。

> **全ファイルマッピング**: `docs/migration/DESIGN.md §10.3` に全 30+ ファイルの JSP→Thymeleaf 対応表が記載されている。

### 7.1 レイアウト・共通テンプレート

| 移行元 Tiles 定義 / JSP | 移行後 Thymeleaf テンプレート | 確認ポイント |
|----------------------|---------------------------|-----------|
| `tiles-defs.xml` `base.layout` | `templates/fragments/layout.html` | `layout:fragment="content"` が存在するか |
| `header.jsp` | `templates/fragments/header.html` | `th:replace="~{fragments/header :: header}"` |
| `footer.jsp` | `templates/fragments/footer.html` | `th:replace="~{fragments/footer :: footer}"` |
| `navigation.jsp` | `templates/fragments/header.html`（統合） | `th:sec:authorize` でロールベース表示 |

### 7.2 EC サイトページ

| 移行元 JSP | 移行後テンプレート | 確認ポイント |
|-----------|----------------|-----------|
| `index.jsp` | `templates/index.html` | `layout:decorate="~{fragments/layout}"` |
| `products/list.jsp` | `templates/products/list.html` | `th:each="product : ${products}"` |
| `products/detail.jsp` | `templates/products/detail.html` | `th:text="${product.name}"` |
| `products/search.jsp` | `templates/products/search.html` | フォームの `th:action="@{/products/search}"` |
| `cart/view.jsp` | `templates/cart/view.html` | CSRF `th:action` |
| `checkout/view.jsp` | `templates/checkout/view.html` | 配送先フォーム |
| `checkout/confirm.jsp` | `templates/checkout/confirm.html` | 注文内容確認 |
| `checkout/complete.jsp` | `templates/checkout/complete.html` | 注文番号表示 |
| `orders/list.jsp` | `templates/orders/list.html` | `@{/orders/{id}(id=${order.id})}` |
| `orders/detail.jsp` | `templates/orders/detail.html` | `th:if` でキャンセルボタン表示制御 |
| `account/view.jsp` | `templates/account/index.html` | `#authentication` |
| `account/addresses.jsp` | `templates/account/addresses.html` | 住所一覧 |
| `auth/login.jsp` | `templates/auth/login.html` | `th:action="@{/auth/login}"` + CSRF |
| `auth/register.jsp` | `templates/auth/register.html` | `th:errors="*{email}"` |

### 7.3 管理画面ページ

| 移行元 JSP | 移行後テンプレート | 確認ポイント |
|-----------|----------------|-----------|
| `admin/products/list.jsp` | `templates/admin/products/list.html` | `th:sec:authorize="hasRole('ADMIN')"` |
| `admin/products/edit.jsp` | `templates/admin/products/edit.html` | PUT フォームの hidden `_method` |
| `admin/orders/list.jsp` | `templates/admin/orders/list.html` | ステータスフィルター |
| `admin/orders/detail.jsp` | `templates/admin/orders/detail.html` | ステータス更新フォーム |

**一括確認コマンド**:
```bash
# テンプレートファイルの数を確認（30件以上あるか）
find appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/ \
  -name "*.html" | wc -l

# layout:decorate を使用しているか（全ページテンプレートで使用必須）
grep -rn "layout:decorate" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/ | wc -l

# th:utext が残存していないか（XSS 脆弱性）
grep -rn "th:utext" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/

# *.do URL が残存していないか
grep -rn '\.do"' \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/

# POST フォームで th:action を使っているか（CSRF 自動挿入のため必須）
grep -rL "th:action" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/templates/ \
  | xargs grep -l 'method="post"'
```

---

## 8. 設定ファイル移行マトリクス（Phase 1）

| 移行元ファイル | 移行後ファイル | 用途 |
|-------------|-------------|------|
| `config.properties` / `AppConfig.java` | `application.properties` + プロファイル別 | 共通設定 |
| `struts-config.xml` | Controller の `@RequestMapping` アノテーション | Action マッピング |
| `tiles-defs.xml` | `thymeleaf-layout-dialect` (`layout:decorate`) | レイアウト定義 |
| `web.xml` (FilterDispatcher) | Spring Boot Embedded Tomcat | サーブレット設定 |
| `log4j.properties` | `src/main/resources/logback-spring.xml` | ロギング設定 |
| `db/schema.sql` | `db/migration/V1__initial_schema.sql` (Flyway) | DB スキーマ |
| *(なし)* | `db/migration/V2__add_password_prefix.sql` (Flyway) | パスワード移行 |
| `messages.properties` | `messages.properties` (同名・継続) | メッセージ国際化 |
| `mail/*.xml` | Thymeleaf メールテンプレート (`mail/*.html`) | メールテンプレート |

**確認コマンド**:
```bash
# Flyway マイグレーションファイルの確認
ls appmod-migrated-java21-spring-boot-3rd/src/main/resources/db/migration/

# V2 SQL でパスワードプレフィックス付与が正しいか
grep -n "sha256\|CONCAT\|UPDATE users" \
  appmod-migrated-java21-spring-boot-3rd/src/main/resources/db/migration/V2__*.sql

# logback-spring.xml の存在確認
ls appmod-migrated-java21-spring-boot-3rd/src/main/resources/logback-spring.xml
```

---

## 9. セキュリティ移行マトリクス（Phase 7）

| 移行元（Struts） | 移行後（Spring Security） | 確認ポイント |
|----------------|------------------------|-----------|
| `session.setAttribute("loginUser", user)` | `SecurityContextHolder`（Spring Security 管理） | `SecurityContextHolder.getContext().getAuthentication()` |
| `session.getAttribute("loginUser")` | `@AuthenticationPrincipal UserDetails user` | Controller メソッド引数に付与 |
| `session.invalidate()` | Spring Security ログアウト | `invalidateHttpSession(true)` |
| Filter ベース認可 | `SecurityConfig.authorizeHttpRequests()` | 全 URL のカバレッジ確認 |
| 手動ロールチェック | `@PreAuthorize("hasRole('ADMIN')")` | Service メソッドに付与 |
| `PasswordHasher.encode()` SHA-256 | `DelegatingPasswordEncoder` (BCrypt + sha256) | `LegacySha256PasswordEncoder` |
| *(なし)* | `CartMergeSuccessHandler` | ログイン成功時カートマージ |

**確認コマンド**:
```bash
# SecurityConfig 必須設定の確認
grep -n "csrf\|sessionFixation\|maximumSessions\|httpStrictTransportSecurity\|xssProtection\|frameOptions" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/config/SecurityConfig.java

# CartMergeSuccessHandler の存在確認
find appmod-migrated-java21-spring-boot-3rd/src/main/java/ \
  -name "CartMergeSuccessHandler.java"

# CustomUserDetailsService が UserDetailsPasswordService を implements しているか
grep -n "implements.*UserDetailsPasswordService\|implements.*UserDetailsService.*UserDetailsPasswordService" \
  appmod-migrated-java21-spring-boot-3rd/src/main/java/com/skishop/security/CustomUserDetailsService.java
```

---

## EC サイト — 機能要件 (FR-)

### EC-HOME（Phase 2）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-HOME-01 | ヒーローバナー表示 | `src/app/(ec)/page.tsx`, `src/components/ec/hero-banner.tsx` | — |
| FR-HOME-02 | 人気カテゴリ表示 | `src/components/ec/category-card.tsx` | `GET /categories` |
| FR-HOME-03 | トレンド商品セクション | `src/components/ec/trending-products.tsx` | `GET /recommendations/trending` |
| FR-HOME-04 | パーソナライズドレコメンド | `src/components/ec/personalized-recommendations.tsx` | `GET /recommendations/:userId` |
| FR-HOME-05 | 検索バー（オートコンプリート） | `src/components/ec/search-bar.tsx` | `GET /search/autocomplete` |
| FR-HOME-06 | AI 相談 CTA ボタン | `src/app/(ec)/page.tsx` | — |
| FR-HOME-07 | シーズン別バナー切替 | `src/components/ec/hero-banner.tsx` | `GET /campaigns/active` |
| FR-HOME-08 | オフシーズン訴求セクション | `src/components/ec/off-season-section.tsx` | — |

### EC-CATALOG（Phase 2）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-CAT-01 | 商品一覧カード表示 | `src/app/(ec)/products/page.tsx`, `src/components/ec/product-card.tsx` | `GET /products` |
| FR-CAT-02 | カテゴリフィルター | `src/components/ec/category-filter.tsx` | `GET /products/category/:id` |
| FR-CAT-03 | ソート機能 | `src/components/ec/sort-selector.tsx` | — |
| FR-CAT-04 | ページネーション（無限スクロール） | `src/hooks/use-infinite-scroll.ts` | — |
| FR-CAT-05 | AI セマンティック検索 | `src/app/(ec)/search/page.tsx` | `POST /search/semantic` |

### EC-DETAIL（Phase 2）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-DET-01 | 商品画像ギャラリー | `src/components/ec/product-gallery.tsx` | `GET /products/:id` |
| FR-DET-02 | 商品情報表示 | `src/app/(ec)/products/[id]/page.tsx` | — |
| FR-DET-03 | 在庫状況表示 | `src/components/ec/stock-status.tsx` | — |
| FR-DET-04 | 数量選択 + カート追加 | `src/components/ec/add-to-cart.tsx` | `POST /cart/items` |
| FR-DET-05 | 類似商品レコメンド | `src/components/ec/similar-products.tsx` | `GET /recommendations/similar/:id` |
| FR-DET-06 | パンくずリスト | `src/components/layout/breadcrumb.tsx` | — |

### EC-SEARCH（Phase 2）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-SRCH-01 | キーワード + AI 検索統合 | `src/app/(ec)/search/page.tsx` | `GET /search` |
| FR-SRCH-02 | オートコンプリート | `src/components/ec/search-bar.tsx` | `GET /search/autocomplete` |
| FR-SRCH-03 | カテゴリ別フィルター | `src/app/(ec)/search/page.tsx` | — |
| FR-SRCH-04 | 検索フィードバック | `src/components/ec/search-feedback.tsx` | `POST /search/feedback` |
| FR-SRCH-05 | 0 件時代替提案 | `src/app/(ec)/search/page.tsx` | `GET /recommendations/trending` |

### EC-AUTH（Phase 3）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-AUTH-01 | ログイン | `src/app/(ec)/login/page.tsx` | `POST /auth/login` |
| FR-AUTH-02 | 新規登録 | `src/app/(ec)/register/page.tsx` | `POST /auth/register` |
| FR-AUTH-03 | パスワードリセット要求 | `src/app/(ec)/password/reset/page.tsx` | `POST /auth/password/reset` |
| FR-AUTH-04 | パスワードリセット確認 | `src/app/(ec)/password/reset/page.tsx` | `POST /auth/password/confirm` |
| FR-AUTH-05 | メール認証 | `src/app/(ec)/verify-email/page.tsx` | `POST /users/verify-email` |
| FR-AUTH-06 | トークンリフレッシュ | `src/bff/auth/`, `src/app/api/auth/refresh/route.ts` | `POST /auth/refresh` |
| FR-AUTH-07 | ログアウト | `src/app/api/auth/logout/route.ts` | `POST /auth/logout` |

### EC-CART（Phase 4）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-CART-01 | カート内商品一覧 | `src/app/(ec)/cart/page.tsx` | `GET /cart?userId=` |
| FR-CART-02 | 数量変更 | `src/app/(ec)/cart/page.tsx` | `PUT /cart/items/:itemId` |
| FR-CART-03 | 商品削除 | `src/app/(ec)/cart/page.tsx` | `DELETE /cart/items/:itemId` |
| FR-CART-04 | カートクリア | `src/app/(ec)/cart/page.tsx` | `DELETE /cart` |
| FR-CART-05 | 合計金額表示 | `src/components/ec/cart-summary.tsx` | — |
| FR-CART-06 | クーポン適用 | `src/components/ec/coupon-input.tsx` | `POST /coupons/validate` |
| FR-CART-07 | ポイント残高表示 | `src/components/ec/points-display.tsx` | `GET /points/balance/:userId` |
| FR-CART-08 | CTA ボタン | `src/app/(ec)/cart/page.tsx` | — |

### EC-CHECKOUT（Phase 4）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-CHK-01 | 注文内容確認 | `src/app/(ec)/checkout/page.tsx` | — |
| FR-CHK-02 | 配送先住所入力 | `src/components/ec/shipping-form.tsx` | — |
| FR-CHK-03 | 決済手段選択 | `src/components/ec/payment-method.tsx` | — |
| FR-CHK-04 | 決済インテント作成 | `src/app/api/payments/intent/route.ts` | `POST /payments/intent` |
| FR-CHK-05 | 決済処理実行 | `src/app/api/payments/[id]/process/route.ts` | `POST /payments/:id/process` |
| FR-CHK-06 | 注文作成 | `src/app/api/orders/route.ts` | `POST /orders` |
| FR-CHK-07 | ポイント利用 | `src/app/api/points/redeem/route.ts` | `POST /points/redeem` |
| FR-CHK-08 | 注文完了画面 | `src/app/(ec)/checkout/result/page.tsx` | — |
| FR-CHK-09 | 冪等性・二重送信防止 | チェックアウト処理全体 | — |
| FR-CHK-10 | 決済タイムアウト・リカバリ | チェックアウト処理全体 | — |

### EC-ORDERS / EC-ORDER-DETAIL（Phase 5）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-ORD-01 | 注文一覧表示 | `src/app/(ec)/mypage/orders/page.tsx` | `GET /orders/customer/:id` |
| FR-ORD-02 | ステータスバッジ | `src/components/ec/order-status-badge.tsx` | — |
| FR-ORD-03 | 注文番号検索 | `src/app/(ec)/mypage/orders/page.tsx` | `GET /orders/number/:num` |
| FR-ORD-04 | ページネーション | `src/components/common/pagination.tsx` | — |
| FR-ORDD-01 | 注文詳細表示 | `src/app/(ec)/mypage/orders/[id]/page.tsx` | `GET /orders/:orderId` |
| FR-ORDD-02 | アイテム一覧 | `src/app/(ec)/mypage/orders/[id]/page.tsx` | — |
| FR-ORDD-03 | 配送情報 | `src/components/ec/shipment-info.tsx` | `GET /shipments/order/:orderId` |
| FR-ORDD-04 | キャンセルボタン | `src/app/(ec)/mypage/orders/[id]/page.tsx` | `PUT /orders/:orderId/cancel` |
| FR-ORDD-05 | 返品申請ボタン | `src/app/(ec)/mypage/orders/[id]/page.tsx` | `POST /returns` |

### EC-PROFILE（Phase 5）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-PROF-01 | ユーザー情報表示・編集 | `src/app/(ec)/mypage/profile/page.tsx` | `GET/PUT /users/:id` |
| FR-PROF-02 | パスワード変更 | `src/components/ec/password-change-form.tsx` | `PUT /users/:id/password` |
| FR-PROF-03 | ユーザー設定 | `src/components/ec/user-preferences.tsx` | `GET/PUT /users/:id/preferences/:key` |
| FR-PROF-04 | アカウント削除 | `src/app/(ec)/mypage/profile/page.tsx` | `DELETE /users/:id` |
| FR-PROF-05 | レコメンド履歴 | `src/components/ec/recommendation-history.tsx` | `GET /recommendations/history/:userId` |

### EC-POINTS / EC-COUPONS / EC-RETURNS / EC-AI-CHAT（Phase 7）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | API |
|---------|---------|----------------------|-----|
| FR-PNT-01 | ポイント残高表示 | `src/app/(ec)/mypage/points/page.tsx` | `GET /points/balance/:userId` |
| FR-PNT-02 | ポイント履歴 | `src/app/(ec)/mypage/points/page.tsx` | `GET /points/history/:userId` |
| FR-PNT-03 | 失効予定アラート | `src/components/ec/expiring-points-alert.tsx` | `GET /points/expiring/:userId` |
| FR-PNT-04 | ティア情報 | `src/components/ec/tier-progress.tsx` | `GET /tiers/user/:userId` |
| FR-PNT-05 | ポイント移行 | `src/components/ec/point-transfer-form.tsx` | `POST /points/transfer` |
| FR-PNT-06 | 期間指定フィルター | `src/app/(ec)/mypage/points/page.tsx` | `GET /points/history/:userId/range` |
| FR-CPN-01 | クーポン一覧 | `src/app/(ec)/mypage/coupons/page.tsx` | `GET /coupons/user/available` |
| FR-CPN-02 | クーポン詳細 | `src/components/ec/coupon-detail.tsx` | `GET /coupons/:code` |
| FR-CPN-03 | クーポンバリデーション | `src/components/ec/coupon-input.tsx` | `POST /coupons/validate` |
| FR-RET-01 | 返品申請フォーム | `src/app/(ec)/mypage/returns/page.tsx` | `POST /returns` |
| FR-RET-02 | 返品履歴一覧 | `src/app/(ec)/mypage/returns/page.tsx` | `GET /returns` |
| FR-RET-03 | 返品ステータス表示 | `src/components/ec/return-status-badge.tsx` | — |
| FR-CHAT-01 | チャットセッション作成 | `src/components/ec/chat-widget.tsx` | `POST /chat/session` |
| FR-CHAT-02 | メッセージ送受信 | `src/components/ec/chat-widget.tsx` | `POST /chat/message` |
| FR-CHAT-03 | チャット履歴 | `src/components/ec/chat-history.tsx` | `GET /chat/sessions/:userId` |
| FR-CHAT-04 | セッション内履歴 | `src/components/ec/chat-widget.tsx` | `GET /chat/history/:sessionId` |
| FR-CHAT-05 | フィードバック | `src/components/ec/chat-feedback.tsx` | `POST /chat/feedback` |
| FR-CHAT-06 | 有人エスカレーション | `src/components/ec/chat-widget.tsx` | `POST /chat/escalate` |
| FR-CHAT-07 | インテント表示 | `src/components/ec/chat-intents.tsx` | `GET /chat/intents` |

### トランザクションメール（Phase 3 + Phase 5）

| 要件 ID | 要件概要 | 確認ファイルパス（想定） | Phase |
|---------|---------|----------------------|-------|
| FR-MAIL-01 | メール認証トークン処理 | `src/app/(ec)/verify-email/page.tsx` | 3 |
| FR-MAIL-02 | パスワードリセットトークン処理 | `src/app/(ec)/password/reset/page.tsx` | 3 |
| FR-MAIL-03 | ディープリンク対応（未認証→ログイン→リダイレクト） | `src/middleware.ts` | 3, 5 |
| FR-MAIL-04 | 注文完了画面の「メール送信済み」表示 | `src/app/(ec)/checkout/result/page.tsx` | 4 |

---

## 管理画面 — 機能要件 (FR-)

### ADM-DASH（Phase 6）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-ADASH-01 | KPI カード | `GET /analytics/dashboard?dashboardType=overview` |
| FR-ADASH-02 | 売上推移グラフ | `GET /analytics/trends` |
| FR-ADASH-03 | 在庫アラート | `GET /inventory/low-stock?threshold=10` |
| FR-ADASH-04 | 最新注文一覧 | `GET /admin/orders?sort=createdAt,desc&size=10` |
| FR-ADASH-05 | 顧客セグメント | `GET /analytics/customer-segments` |
| FR-ADASH-06 | エスカレーション件数 | — |

### ADM-PRODUCTS（Phase 6）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-APRD-01 | 商品一覧テーブル | `GET /products` |
| FR-APRD-02 | 検索・フィルター | `GET /products/search?q=` |
| FR-APRD-03 | 商品新規登録 | `POST /products` |
| FR-APRD-04 | 商品編集 + 価格更新 | `PUT /products/:id`, `PUT /prices/:productId` |
| FR-APRD-05 | 商品削除 | — |
| FR-APRD-06 | バッチ取得 | `POST /products/batch` |

### ADM-INVENTORY（Phase 8）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-AINV-01 | 在庫一覧 | `GET /inventory/:productId` |
| FR-AINV-02 | 入庫処理 | `POST /inventory/stock-in` |
| FR-AINV-03 | 出庫処理 | `POST /inventory/stock-out` |
| FR-AINV-04 | 在庫予約 | `POST /inventory/reserve` |
| FR-AINV-05 | 予約解放 | `POST /inventory/release` |
| FR-AINV-06 | 低在庫アラート | `GET /inventory/low-stock?threshold=` |
| FR-AINV-07 | 発注推奨 | `GET /analytics/sales-forecast` |
| FR-AINV-08 | バッチ在庫取得 | `POST /inventory/batch` |
| FR-AINV-09 | 価格一括更新 | `PUT /prices/:productId` |

### ADM-ORDERS（Phase 6）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-AORD-01 | 注文一覧（ステータスフィルター） | `GET /admin/orders` |
| FR-AORD-02 | ステータス更新 | `PUT /orders/:orderId/status` |
| FR-AORD-03 | 配送作成・管理 | `POST /shipments`, `PUT /shipments/:id/status` |
| FR-AORD-04 | 返品処理 | `PUT /returns/:id/status` |
| FR-AORD-05 | 返金処理 | `POST /payments/:id/refund` |
| FR-AORD-06 | 決済履歴表示 | `GET /payments/history` |

### ADM-USERS（Phase 6）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-AUSR-01 | ユーザー一覧 | `GET /admin/users` |
| FR-AUSR-02 | ステータス変更 | `PUT /admin/users/:id/status` |
| FR-AUSR-03 | ロール付与 | `POST /admin/users/:id/roles` |
| FR-AUSR-04 | ユーザー詳細表示 | — |

### ADM-POINTS / ADM-CAMPAIGNS / ADM-COUPONS / ADM-MAIL-LOGS（Phase 8）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-APNT-01 | ポイント手動付与 | `POST /points/award` |
| FR-APNT-02 | 期限切れ一括処理 | `POST /points/process-expired` |
| FR-APNT-03 | ティア定義一覧 | `GET /tiers` |
| FR-APNT-04 | ポイント詳細検索 | — |
| FR-ACMP-01 | キャンペーン一覧 | `GET /campaigns` |
| FR-ACMP-02 | キャンペーン作成 | `POST /campaigns` |
| FR-ACMP-03 | キャンペーン編集 | `PUT /campaigns/:id` |
| FR-ACMP-04 | キャンペーン有効化 | `POST /campaigns/:id/activate` |
| FR-ACMP-05 | アクティブ一覧 | `GET /campaigns/active` |
| FR-ACPN-01 | クーポン作成 | `POST /coupons` |
| FR-ACPN-02 | 一括生成 | `POST /coupons/bulk-generate` |
| FR-ACPN-03 | 利用状況確認 | `GET /coupons/usage/:id` |
| FR-ACPN-04 | キャンペーン別一覧 | `GET /coupons?campaignId=` |
| FR-AMAIL-01 | メール履歴テーブル | `GET /mail/logs` |
| FR-AMAIL-02 | ステータスフィルター | — |
| FR-AMAIL-03 | 手動リトライ | `POST /mail/logs/:id/retry` |
| FR-AMAIL-04 | 統計 | `GET /mail/stats` |
| FR-AMAIL-05 | テストメール | `POST /mail/test` |

### ADM-ANALYTICS / ADM-AI-MODELS（Phase 9）

| 要件 ID | 要件概要 | API |
|---------|---------|-----|
| FR-AANA-01 | ユーザー行動分析 | `GET /analytics/user-behavior` |
| FR-AANA-02 | 売上予測 | `GET /analytics/sales-forecast` |
| FR-AANA-03 | 商品パフォーマンス | `GET /analytics/product-performance` |
| FR-AANA-04 | トレンド分析 | `GET /analytics/trends` |
| FR-AANA-05 | 顧客セグメント | `GET /analytics/customer-segments` |
| FR-AANA-06 | カスタムレポート | `POST /analytics/custom-report` |
| FR-AANA-07 | 検索分析 | `GET /search/analytics` |
| FR-AMDL-01 | モデルバージョン一覧 | `GET /models/versions` |
| FR-AMDL-02 | トレーニング実行 | `POST /models/train` |
| FR-AMDL-03 | 進捗モニタリング | `GET /models/status/:trainingId` |
| FR-AMDL-04 | デプロイ | `POST /models/deploy` |
| FR-AMDL-05 | パフォーマンス評価 | `GET /models/performance` |

---

## 受入基準 (AC-) マッピング

| 受入基準 ID | Phase | 検証項目 |
|------------|-------|---------|
| AC-HOME-01 | 2 | 未ログイン時トレンド商品最大 10 件表示 |
| AC-HOME-02 | 2 | ログイン時パーソナライズドレコメンドがトレンドより上位 |
| AC-HOME-03 | 2 | カテゴリ 4 件以上表示 |
| AC-HOME-04 | 2 | オートコンプリート 500ms 以内 |
| AC-HOME-05 | 2 | シーズン別バナー切替 |
| AC-HOME-06 | 2 | オフシーズン早期予約セクション |
| AC-CAT-01 | 2 | 1 ページ最大 20 件 |
| AC-CAT-02 | 2 | カテゴリ切替で URL 更新 |
| AC-CAT-03 | 2 | 在庫切れラベル + カート追加不可 |
| AC-DET-01 | 2 | 在庫 ≤5「残りわずか」、=0「在庫切れ」 |
| AC-DET-02 | 2 | 在庫超過数量選択不可 |
| AC-DET-03 | 2 | 類似商品 4 件以上 |
| AC-DET-04 | 2 | 未ログインでカート追加→ログインリダイレクト |
| AC-SRCH-01 | 2 | 検索結果 2 秒以内 |
| AC-SRCH-02 | 2 | オートコンプリート 500ms 以内 |
| AC-SRCH-03 | 2 | 自然言語クエリで関連商品ヒット |
| AC-AUTH-01 | 3 | トークンの安全な保存 |
| AC-AUTH-02 | 3 | 5 分前自動リフレッシュ |
| AC-AUTH-03 | 3 | リアルタイム入力バリデーション |
| AC-CART-01 | 4 | アイテム変更時リアルタイム金額再計算 |
| AC-CART-02 | 4 | 在庫超過バリデーション |
| AC-CART-03 | 4 | 無効クーポンエラーメッセージ |
| AC-CART-04 | 4 | ヘッダーカートバッジ |
| AC-CHK-01 | 4 | Idempotency-Key + 二重送信防止 |
| AC-CHK-02 | 4 | 決済失敗時エラー + 再試行 |
| AC-CHK-03 | 4 | 注文完了後リンク |
| AC-CHK-04 | 4 | ブラウザバック防止 |
| AC-CHK-05 | 4 | 503 時ポーリング |
| AC-CHK-06 | 4 | 冪等性キーでリトライ |
| AC-ORD-01 | 5 | 新しい順ソート |
| AC-ORD-02 | 5 | ステータスバッジ色分け |
| AC-PNT-01 | 7 | 失効予定 30 日以内警告 |
| AC-PNT-02 | 7 | ティアプログレスバー |
| AC-CHAT-01 | 7 | AI 応答 5 秒以内 |
| AC-CHAT-02 | 7 | フローティングウィジェット |
| AC-CHAT-03 | 7 | 商品名→詳細リンク |
| AC-AINV-01 | 8 | 在庫操作に確認ダイアログ |
| AC-AINV-02 | 8 | 在庫 ≤10 自動アラート |
| AC-AMAIL-01 | 8 | 失敗メール赤バッジ |
| AC-AMAIL-02 | 8 | リトライ確認ダイアログ |
| AC-AMAIL-03 | 8 | 7 日間成功率グラフ |
| AC-MAIL-01 | 3 | 登録後メール認証リンク送信 |
| AC-MAIL-02 | 3 | リセットメールから新パスワード設定 |
| AC-MAIL-03 | 5 | 注文確認メールから詳細遷移 |
| AC-MAIL-04 | 5 | 発送通知に追跡情報 |
| AC-MAIL-05 | 3 | リンク期限切れエラー |

---

## 共通要件セクション — フェーズマッピング

| セクション | 内容 | 実装フェーズ |
|-----------|------|------------|
| §4.1 | レイアウト・ナビゲーション | Phase 1 |
| §4.2 | 共通 UI パターン | Phase 1 |
| §4.2.1 | RFC 7807 エラーハンドリング | Phase 1 |
| §4.2.2 | ページネーション契約 | Phase 1 |
| §4.2.3 | 縮退 UI | Phase 1 + 各画面 |
| §4.2.4 | API 呼び出し戦略 | 各画面 |
| §4.3 | 認証・セッション管理 | Phase 1 + Phase 3 |
| §4.3.1 | JWT トークンライフサイクル | Phase 1 + Phase 3 |
| §4.4 | トランザクションメール | Phase 3 + Phase 5 |
| §4.5 | 状態管理方針 | Phase 1 |
| §4.6 | i18n アーキテクチャ | Phase 1 + Phase 10 |
| §5 | 非機能要件 | Phase 1 + Phase 10 |
| §5.1 | 観測可能性 | Phase 1 |
| §5.2 | レート制限対応 | Phase 10 |
