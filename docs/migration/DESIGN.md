# SkiShop 移行詳細設計書
## Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x

**文書バージョン**: 1.0  
**作成日**: 2026-03-31  
**対象プロジェクト**: `appmod-migrated-java21-spring-boot-3rd`  
**移行元**: `skishop-monolith` (Java 5 / Struts 1.3.10 / Tomcat 6)  
**移行先**: Java 21 / Spring Boot 3.2.x / 組み込み Tomcat (JAR)

---

## 目次

1. [移行概要](#1-移行概要)
2. [現行システム分析](#2-現行システム分析)
3. [ターゲットアーキテクチャ](#3-ターゲットアーキテクチャ)
4. [技術スタック変換マッピング](#4-技術スタック変換マッピング)
5. [パッケージ構成設計](#5-パッケージ構成設計)
6. [Web 層移行設計](#6-web-層移行設計)
7. [サービス層移行設計](#7-サービス層移行設計)
8. [データアクセス層移行設計](#8-データアクセス層移行設計)
9. [ドメインモデル移行設計](#9-ドメインモデル移行設計)
10. [ビュー層移行設計（JSP → Thymeleaf）](#10-ビュー層移行設計jsp--thymeleaf)
11. [セキュリティ移行設計](#11-セキュリティ移行設計)
12. [設定ファイル移行設計](#12-設定ファイル移行設計)
13. [テスト戦略](#13-テスト戦略)
14. [非機能要件](#14-非機能要件)
15. [既知の課題と制限事項](#15-既知の課題と制限事項)

---

## 1. 移行概要

### 1.1 移行の目的

本移行は、EOL（End of Life）状態にある Java 5 / Struts 1.3 モノリスを、保守性・セキュリティ・拡張性に優れた Java 21 / Spring Boot 3.2.x へ刷新することを目的とする。

### 1.2 移行方針

| 方針 | 内容 |
|------|------|
| **機能等価性の維持** | 全ての既存業務機能を移行先でも提供する |
| **段階的移行** | フェーズ分割により各段階でコンパイル・テストを確認しながら進める |
| **既存 DB スキーマの継続利用** | DB スキーマは原則として変更しない（追加のみ可）|
| **別ディレクトリでの新規作成** | 元プロジェクトを改変せず、`appmod-migrated-java21-spring-boot-3rd/` を新規作成 |
| **Spring BOM 優先** | 依存バージョンは Spring Boot BOM で一元管理し、個別バージョン指定を最小化 |
| **スキーマバージョン管理** | Flyway でスキーマ変更を追跡・管理する |

### 1.3 移行スコープ

**スコープ内（必須）**:
- 全 Struts Action → Spring MVC Controller
- 全 ActionForm → Bean Validation 付き DTO
- JSP + Tiles → Thymeleaf テンプレート
- JDBC DAO → Spring Data JPA Repository
- Log4j 1.x → SLF4J + Logback（Spring Boot 管理）
- カスタム認証 → Spring Security
- app.properties → application.properties（プロファイル分離）
- カスタム接続プール（Commons DBCP） → HikariCP（Spring Boot 管理）
- WAR → JAR（組み込み Tomcat）

**スコープ外（別課題として管理）**:
- マイクロサービス分割
- フロントエンドの SPA 化
- API ゲートウェイの導入
- 非同期メッセージングへの変更

---

## 2. 現行システム分析

### 2.1 現行技術スタック

| レイヤー | 現行技術 | バージョン | EOL 状態 |
|---------|---------|-----------|---------|
| 言語 | Java | 1.5 | EOL（2009年） |
| フレームワーク | Apache Struts | 1.3.10 | EOL（2013年） |
| ビルド | Maven | 2.x | EOL |
| サーブレットコンテナ | Tomcat | 6.x | EOL（2016年） |
| DB アクセス | Commons DBCP + commons-dbutils | 1.2.2 / 1.1 | 旧世代 |
| DB ドライバ | PostgreSQL JDBC | 9.2 JDBC3 | EOL |
| ログ | Log4j | 1.2.17 | EOL（2015年） + Log4Shell 脆弱性 |
| メール | javax.mail | 1.4.7 | EOL（jakarta.mail へ移行済み） |
| テスト | JUnit | 4.12 | 旧世代 |

### 2.2 現行ディレクトリ構成

```
src/main/java/com/skishop/
├── common/
│   ├── config/         # AppConfig (Singleton 設定読み込み)
│   ├── dao/            # AbstractDao, DataSourceLocator, DaoException
│   ├── service/        # BaseService (存在する場合)
│   └── util/           # PasswordHasher
├── dao/
│   ├── address/        # UserAddressDao/Impl
│   ├── cart/           # CartDao/Impl, CartItemDao/Impl
│   ├── category/       # CategoryDao/Impl
│   ├── coupon/         # CouponDao/Impl, CouponUsageDao/Impl
│   ├── inventory/      # InventoryDao/Impl
│   ├── mail/           # EmailQueueDao/Impl
│   ├── order/          # OrderDao/Impl, OrderItemDao/Impl
│   ├── payment/        # PaymentDao/Impl
│   ├── point/          # PointAccountDao/Impl, PointTransactionDao/Impl
│   ├── product/        # ProductDao/Impl, PriceDao/Impl
│   ├── shipping/       # ShippingMethodDao/Impl
│   └── user/           # UserDao/Impl, SecurityLogDao/Impl, PasswordResetTokenDao/Impl
├── domain/             # POJO エンティティ（getter/setter のみ）
│   ├── address/, cart/, coupon/, inventory/, mail/
│   ├── order/, payment/, point/, product/, shipping/, user/
├── service/            # ビジネスロジック（new による依存生成）
│   ├── auth/, cart/, catalog/, coupon/, inventory/
│   ├── mail/, order/, payment/, point/, shipping/, tax/, user/
└── web/
    ├── action/         # Struts Action クラス（+ admin/）
    ├── filter/         # RequestIdFilter, CharacterEncodingFilter
    ├── form/           # ActionForm サブクラス（+ admin/）
    ├── processor/      # カスタム RequestProcessor（存在する場合）
    └── tag/            # カスタムタグ（存在する場合）
```

### 2.3 現行 Action 一覧（移行対象）

| Action クラス | URL パス | ロール | 移行先 Controller |
|--------------|---------|--------|-----------------|
| `LoginAction` | /login | 全員 | `AuthController` |
| `LogoutAction` | /logout | USER,ADMIN | `AuthController` |
| `RegisterAction` | /register | 全員 | `AuthController` |
| `PasswordForgotAction` | /password/forgot | 全員 | `AuthController` |
| `PasswordResetAction` | /password/reset | 全員 | `AuthController` |
| `ProductListAction` | /products | 全員 | `ProductController` |
| `ProductDetailAction` | /product | 全員 | `ProductController` |
| `CartAction` | /cart | 全員 | `CartController` |
| `CheckoutAction` | /checkout | USER,ADMIN | `CheckoutController` |
| `CouponApplyAction` | /coupon/apply | USER,ADMIN | `CouponController` |
| `CouponAvailableAction` | /coupons/available | 全員 | `CouponController` |
| `OrderHistoryAction` | /orders | USER,ADMIN | `OrderController` |
| `OrderDetailAction` | /orders/detail | USER,ADMIN | `OrderController` |
| `OrderCancelAction` | /orders/cancel | USER,ADMIN | `OrderController` |
| `OrderReturnAction` | /orders/return | USER,ADMIN | `OrderController` |
| `PointBalanceAction` | /points | USER,ADMIN | `AccountController`（/account/points に統合） |
| `AddressListAction` | /addresses | USER,ADMIN | `AccountController`（/account/addresses に統合） |
| `AddressSaveAction` | /addresses/save | USER,ADMIN | `AccountController`（/account/addresses に統合） |
| `AdminProductListAction` | /admin/products | ADMIN | `AdminProductController` |
| `AdminProductEditAction` | /admin/product/edit | ADMIN | `AdminProductController` |
| `AdminProductDeleteAction` | /admin/product/delete | ADMIN | `AdminProductController` |
| `AdminOrderListAction` | /admin/orders | ADMIN | `AdminOrderController` |
| `AdminOrderDetailAction` | /admin/orders/detail | ADMIN | `AdminOrderController` |
| `AdminOrderUpdateAction` | /admin/order/update | ADMIN | `AdminOrderController` |
| `AdminOrderRefundAction` | /admin/order/refund | ADMIN | `AdminOrderController` |
| `AdminCouponListAction` | /admin/coupons | ADMIN | `AdminCouponController` |
| `AdminCouponEditAction` | /admin/coupon/edit | ADMIN | `AdminCouponController` |
| `AdminShippingMethodListAction` | /admin/shipping | ADMIN | `AdminShippingController` |
| `AdminShippingMethodEditAction` | /admin/shipping/edit | ADMIN | `AdminShippingController` |

### 2.4 現行 DAO 一覧

| DAO インターフェース | ドメイン | 主要メソッド |
|--------------------|---------|------------|
| `UserDao` | user | findByEmail, findById, insert, updatePassword, updateStatus |
| `SecurityLogDao` | user | insert, countByUserAndEvent |
| `PasswordResetTokenDao` | user | insert, findByToken, delete |
| `UserAddressDao` | address | findByUserId, findById, insert, update, delete |
| `CartDao` | cart | findBySessionOrUser, insert, update |
| `CartItemDao` | cart | findByCartId, insert, update, delete |
| `CategoryDao` | category | findAll, findById |
| `ProductDao` | product | findAll, findById, findByCategory, insert, update, delete |
| `PriceDao` | product | findByProductId, insert, update |
| `InventoryDao` | inventory | findByProductId, update |
| `CouponDao` | coupon | findByCode, findAll, insert, update |
| `CouponUsageDao` | coupon | findByUserAndCoupon, insert |
| `OrderDao` | order | findByUserId, findById, insert, update |
| `OrderItemDao` | order | findByOrderId, insert |
| `ReturnDao` | order | findByOrderId, findByOrderItemId, insert |
| `PaymentDao` | payment | findByOrderId, insert, update |
| `ShippingMethodDao` | shipping | findAll, findById, insert, update, delete |
| `PointAccountDao` | point | findByUserId, insert, update |
| `PointTransactionDao` | point | findByAccountId, insert |
| `EmailQueueDao` | mail | insert, findPending, updateStatus |

### 2.5 現行サービス一覧

| サービスクラス | 依存 DAO |
|--------------|---------|
| `AuthService` | UserDao, SecurityLogDao |
| `UserService` | UserDao, PasswordResetTokenDao |
| `CartService` | CartDao, CartItemDao, ProductDao, PriceDao |
| `ProductService` / `CatalogService` | ProductDao, PriceDao, CategoryDao, InventoryDao |
| `InventoryService` | InventoryDao |
| `CouponService` | CouponDao, CouponUsageDao |
| `OrderService` | OrderDao, OrderItemDao, CartDao, PaymentDao |
| `PaymentService` | PaymentDao, OrderDao |
| `ShippingService` | ShippingMethodDao |
| `TaxService` | (設定値のみ) |
| `PointService` | PointAccountDao, PointTransactionDao |
| `MailService` | EmailQueueDao |
| `AddressService` | UserAddressDao |

### 2.6 DB スキーマ

主要テーブル（schema.sql より）:
- `users` (id, email, username, password_hash, salt, status, role, created_at, updated_at)
- `security_logs` (id, user_id, event_type, ip_address, user_agent, details_json)
- `password_reset_tokens`
- `categories` (id, name, parent_id)
- `products` (id, name, brand, description, category_id, sku, status, created_at, updated_at)
- `prices` (id, product_id, regular_price, sale_price, currency_code, sale_start_date, sale_end_date)
- `inventory` (id, product_id, quantity, reserved_quantity, status)
- `carts` (id, user_id, session_id, status, expires_at)
- `cart_items` (id, cart_id, product_id, quantity, unit_price)
- `payments` (id, order_id, cart_id, amount, currency, status, payment_intent_id, created_at)
- `orders` (id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at)
- `order_items` (id, order_id, product_id, product_name, sku, unit_price, quantity, subtotal)
- `shipments` (id, order_id, carrier, tracking_number, status, shipped_at, delivered_at)
- `returns` (id, order_id, order_item_id, reason, quantity, ...)
- `coupons`, `coupon_usages`, `campaigns`
- `shipping_methods`
- `point_accounts`, `point_transactions`
- `addresses`
- `email_queue`

### 2.7 現行セキュリティ課題

| 課題 | 重要度 | 移行時の対処 |
|------|--------|------------|
| SHA-256 + カスタムソルト（BCrypt 未使用） | 高 | Spring Security BCrypt へ移行（既存パスワードは再設定フロー） |
| セッション固定攻撃防止なし | 高 | Spring Security のデフォルト保護で対応 |
| CSRF 保護なし | 高 | Spring Security CSRF トークンで対応 |
| Log4Shell（Log4j 1.x） | 緊急 | SLF4J + Logback（Spring Boot 管理）へ置換 |
| `commons-dbutils` のバージョン古い | 中 | Spring Data JPA で置換 |

---

## 3. ターゲットアーキテクチャ

### 3.1 技術スタック

| レイヤー | 採用技術 | バージョン |
|---------|---------|-----------|
| 言語 | Java | 21 (LTS) |
| フレームワーク | Spring Boot | 3.2.x |
| Web | Spring MVC | 6.1.x（Boot 管理） |
| ビュー | Thymeleaf | 3.1.x（Boot 管理） |
| ビュー Thymeleaf 拡張 | thymeleaf-extras-springsecurity6 | Boot 管理 |
| ビュー Tiles 置換 | thymeleaf-layout-dialect | 3.x |
| セキュリティ | Spring Security | 6.2.x（Boot 管理） |
| DB アクセス | Spring Data JPA + Hibernate | 3.2.x（Boot 管理） |
| DB スキーマ管理 | Flyway | 10.x（Boot 管理） |
| DB ドライバ | postgresql | 42.7.x（Boot 管理） |
| 接続プール | HikariCP | 5.x（Boot 管理） |
| バリデーション | Hibernate Validator (Bean Validation 3.0) | Boot 管理 |
| ログ | SLF4J + Logback | Boot 管理 |
| メール | Spring Boot Mail (jakarta.mail) | Boot 管理 |
| テスト | JUnit 5 + Spring Boot Test | Boot 管理 |
| メトリクス | Spring Boot Actuator + Micrometer | Boot 管理 |
| API ドキュメント | springdoc-openapi | 2.3.x |
| ビルド | Maven | 3.9.x |
| パッケージング | JAR（組み込み Tomcat） | — |

### 3.2 アーキテクチャ概略図

```
ブラウザ / クライアント
         │
         ▼
┌─────────────────────────────────────────────┐
│          Spring Boot Application            │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   Web Layer (Spring MVC)             │  │
│  │  ┌─────────────┐  ┌───────────────┐ │  │
│  │  │ Controllers  │  │ Thymeleaf     │ │  │
│  │  │ (MVC/REST)  │  │ Templates     │ │  │
│  │  └──────┬──────┘  └───────────────┘ │  │
│  └─────────┼────────────────────────────┘  │
│            │ Spring Security Filter Chain   │
│  ┌─────────▼────────────────────────────┐  │
│  │   Service Layer                      │  │
│  │   (Business Logic, @Transactional)   │  │
│  └─────────┬────────────────────────────┘  │
│  ┌─────────▼────────────────────────────┐  │
│  │   Repository Layer (Spring Data JPA) │  │
│  └─────────┬────────────────────────────┘  │
│  ┌─────────▼────────────────────────────┐  │
│  │   HikariCP Connection Pool           │  │
│  └─────────┬────────────────────────────┘  │
└────────────┼────────────────────────────────┘
             │
             ▼
     PostgreSQL Database
```

### 3.3 レイヤー責務

| レイヤー | パッケージ | 責務 |
|---------|-----------|------|
| Web（Controller） | `controller/` | HTTP リクエスト受付、入力バリデーション、レスポンス構築 |
| Web（DTO） | `dto/` | リクエスト/レスポンスの型定義・バリデーションアノテーション |
| Service | `service/` | ビジネスロジック、トランザクション管理 |
| Repository | `repository/` | データアクセス（Spring Data JPA インターフェース） |
| Model（Entity） | `model/` | JPA エンティティ |
| Config | `config/` | Spring 設定クラス |
| Exception | `exception/` | カスタム例外、グローバル例外ハンドラ |

---

## 4. 技術スタック変換マッピング

### 4.1 フレームワークマッピング

| 現行（Struts） | 移行先（Spring Boot） | 変換方針 |
|--------------|---------------------|---------|
| `Action` extends `org.apache.struts.action.Action` | `@Controller` + `@RequestMapping` | 1 Action → 1 Controller メソッド（同 URL ごとに集約） |
| `ActionForm` extends `org.apache.struts.action.ActionForm` | `record` / POJO + Bean Validation アノテーション | フォームはレコードクラスで表現 |
| `ActionForward` | `String`（テンプレート名）/ `redirect:` プレフィックス | テンプレート名を直接返す |
| `ActionMessages` / `ActionMessage` | `BindingResult`, `RedirectAttributes.addFlashAttribute` | Spring MVC のバインディング結果 |
| `ActionServlet` | 不要（Spring DispatcherServlet が管理） | — |
| `struts-config.xml` | `@RequestMapping` アノテーション + `application.properties` | XML 廃止 |
| `validation.xml` / `validator-rules.xml` | Bean Validation アノテーション + `@Valid` | XML 廃止 |
| Struts Tiles | Thymeleaf Layout Dialect | タイル定義 → レイアウトテンプレート |
| `*.do` URL パターン | `/` に統一（RESTful） | URL 末尾の `.do` を除去 |
| `javax.servlet.*` | `jakarta.servlet.*` | namespace 変更のみ |

### 4.2 レイヤー別変換詳細

#### Web 層

```
【変換前】
public class LoginAction extends Action {
    private final AuthService authService = new AuthService(); // new による生成
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest req, HttpServletResponse res) {
        LoginForm loginForm = (LoginForm) form;
        // ...
        return mapping.findForward("success");
    }
}

【変換後】
@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;          // DI による注入

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest request,
                        BindingResult result, HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/login";
        }
        // ...
        return "redirect:/";
    }
}
```

#### DAO 層

```
【変換前】
public class UserDaoImpl extends AbstractDao implements UserDao {
    public User findByEmail(String email) {
        Connection con = getConnection(); // 手動接続管理
        PreparedStatement ps = con.prepareStatement("SELECT ... WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        // ...
        closeQuietly(rs, ps, con);
    }
}

【変換後】
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
}
```

### 4.3 依存性注入（DI）の変換

| 現行パターン | 移行後パターン |
|------------|-------------|
| `private final AuthService authService = new AuthService();` | `@Autowired` / コンストラクタインジェクション |
| `AppConfig.getInstance().getString(key)` | `@Value("${key}")` / `@ConfigurationProperties` |
| `DataSourceLocator.getInstance().getDataSource()` | Spring Boot AutoConfiguration（HikariCP を自動設定） |

---

## 5. パッケージ構成設計

### 5.1 ターゲットパッケージ構成

```
com.skishop/
├── SkiShopApplication.java              # @SpringBootApplication
├── config/
│   ├── SecurityConfig.java              # Spring Security 設定
│   ├── JpaConfig.java                   # JPA 設定（必要な場合）
│   ├── MailConfig.java                  # メール設定
│   ├── WebMvcConfig.java                # MVC 設定（共通フィルタ等）
│   ├── ThymeleafConfig.java             # Thymeleaf 設定（必要な場合）
│   └── CartMergeSuccessHandler.java     # ログイン成功後のカートマージ（AuthenticationSuccessHandler）
├── controller/
│   ├── AuthController.java              # ログイン/ログアウト/会員登録/パスワードリセット
│   ├── ProductController.java           # 商品一覧/詳細
│   ├── CartController.java              # カート
│   ├── CheckoutController.java          # チェックアウト
│   ├── CouponController.java            # クーポン
│   ├── OrderController.java             # 注文履歴/詳細/キャンセル/返品
│   ├── AccountController.java            # ポイント + 住所管理（PointController/AddressController を統合）
│   └── admin/
│       ├── AdminProductController.java  # 管理：商品管理
│       ├── AdminOrderController.java    # 管理：注文管理
│       ├── AdminCouponController.java   # 管理：クーポン管理
│       └── AdminShippingController.java # 管理：配送方法管理
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── PasswordForgotRequest.java
│   │   ├── PasswordResetRequest.java
│   │   ├── ProductSearchRequest.java
│   │   ├── CartItemRequest.java
│   │   ├── CheckoutRequest.java
│   │   ├── CouponApplyRequest.java
│   │   ├── AddressRequest.java
│   │   └── admin/
│   │       ├── AdminProductRequest.java
│   │       ├── AdminCouponRequest.java
│   │       └── AdminShippingMethodRequest.java
│   └── response/
│       ├── UserResponse.java
│       ├── ProductResponse.java
│       ├── CartResponse.java
│       ├── OrderResponse.java
│       └── ...
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ProductService.java
│   ├── CartService.java
│   ├── CheckoutService.java
│   ├── CouponService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── PointService.java
│   ├── ShippingService.java
│   ├── AddressService.java
│   ├── MailService.java
│   ├── TaxService.java
│   ├── InventoryService.java
│   └── CustomUserDetailsService.java    # UserDetailsService + UserDetailsPasswordService 実装
├── repository/
│   ├── UserRepository.java
│   ├── SecurityLogRepository.java
│   ├── PasswordResetTokenRepository.java
│   ├── AddressRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   ├── PriceRepository.java
│   ├── InventoryRepository.java
│   ├── CouponRepository.java
│   ├── CouponUsageRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── ReturnRepository.java
│   ├── PaymentRepository.java
│   ├── ShippingMethodRepository.java
│   ├── PointAccountRepository.java
│   ├── PointTransactionRepository.java
│   └── EmailQueueRepository.java
├── model/
│   ├── User.java
│   ├── SecurityLog.java
│   ├── PasswordResetToken.java
│   ├── Address.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Category.java
│   ├── Product.java
│   ├── Price.java
│   ├── Inventory.java
│   ├── Coupon.java
│   ├── CouponUsage.java
│   ├── Campaign.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderShipping.java
│   ├── Payment.java
│   ├── Shipment.java
│   ├── Return.java
│   ├── ShippingMethod.java
│   ├── PointAccount.java
│   ├── PointTransaction.java
│   └── EmailQueue.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   ├── AuthenticationException.java
│   └── GlobalExceptionHandler.java
├── filter/
│   └── RequestIdFilter.java             # リクエスト ID フィルター（MDC + X-Request-Id ヘッダー伝播）
└── util/
    ├── PasswordHasher.java              # 既存パスワード互換ハッシュ（段階的廃止用）
    └── LegacySha256PasswordEncoder.java # DelegatingPasswordEncoder 登録用（段階的廃止）
```

---

## 6. Web 層移行設計

### 6.1 Controller 設計原則

- **1 機能グループ = 1 Controller**: 認証系・商品系・カート系など機能ごとに集約
- **コンストラクタインジェクション必須**: `@Autowired` フィールドインジェクション禁止
- **`@Valid` を必ず付与**: リクエスト DTO にはバリデーションを実施
- **レスポンス型の統一**: 画面遷移は `String` (テンプレート名)、API は `ResponseEntity<T>`

### 6.2 ActionForm → DTO 変換ルール

ActionForm のバリデーションは `validation.xml` で XML 定義されていたが、移行後は Bean Validation アノテーションをフィールドに付与する。

```java
// 変換例: LoginForm → LoginRequest
public record LoginRequest(
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    String email,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    String password
) {}
```

### 6.3 ActionForward → テンプレート名/リダイレクト変換

| Struts ActionForward | Spring MVC 戻り値 |
|--------------------|-----------------|
| `mapping.findForward("success")` → path=`.home` | `"redirect:/"` |
| `mapping.findForward("failure")` → path=`auth.login` | `"auth/login"` |
| `mapping.getInputForward()` | バリデーションエラー時に同テンプレートを返す |
| `forward name="success" path="/home.do" redirect="true"` | `"redirect:/"` |

### 6.4 セッション管理の変換

| 現行（Struts） | 移行後（Spring Security） |
|--------------|-------------------------|
| `session.setAttribute("loginUser", user)` | Spring Security の `SecurityContextHolder` で管理 |
| `session.getAttribute("loginUser")` | `@AuthenticationPrincipal UserDetails user` |
| `session.invalidate()` | Spring Security の logout 処理に委任 |

### 6.5 URL 設計

`*.do` URL パターンを廃止し、RESTful な URL に変更する。

| 現行 URL | 移行後 URL | HTTP メソッド |
|---------|-----------|-------------|
| `/login.do` (GET/POST) | `/auth/login` | GET（表示）/ POST（処理） |
| `/logout.do` | `/auth/logout` | POST |
| `/register.do` | `/auth/register` | GET / POST |
| `/products.do` | `/products` | GET |
| `/product.do?id=xxx` | `/products/{id}` | GET |
| `/cart.do` | `/cart` | GET |
| `/cart.do` (カートに追加) | `/cart/items` | POST |
| `/checkout.do` | `/checkout` | GET / POST |
| `/coupon/apply.do` | `/cart/coupon` | POST |
| `/orders.do` | `/orders` | GET |
| `/orders/detail.do?id=xxx` | `/orders/{id}` | GET |
| `/orders/cancel.do` | `/orders/{id}/cancel` | POST |
| `/orders/return.do` | `/orders/{orderId}/return` | POST |
| `/points.do` | `/account/points` | GET |
| `/addresses.do` | `/account/addresses` | GET |
| `/addresses/save.do` | `/account/addresses` | POST / PUT |
| `/admin/products.do` | `/admin/products` | GET |
| `/admin/product/edit.do` | `/admin/products/{id}` | GET / PUT |
| `/admin/product/delete.do` | `/admin/products/{id}` | DELETE |

### 6.6 カートセッション管理設計

現行の Struts では `session.setAttribute("cart", cartPojo)` でカート全体をセッションに保持している。移行後は **DB ベース + セッション ID** で管理する。

| 状態 | カートの管理方法 |
|------|---------------|
| 未ログインユーザー | HTTP セッションに `cartId`（UUID）を格納。`carts` テーブルに `session_id` 紐付けで保存 |
| ログインユーザー | `carts.user_id` でカートを管理（セッション ID は不使用） |
| ログイン時のカートマージ | セッションカート（`session_id` 紐付け）をユーザーカートにマージ後、セッションカートを削除 |

```java
// CartService: アクティブカートの取得または生成
@Transactional
public Cart getOrCreateCart(HttpSession session, String userId) {
    if (userId != null) {
        // ログイン済み: user_id で検索
        return cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
            .orElseGet(() -> createCartForUser(userId));
    } else {
        // 未ログイン: セッション ID で検索
        String sessionId = (String) session.getAttribute("cartId");
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute("cartId", sessionId);
        }
        final String sid = sessionId;
        return cartRepository.findBySessionId(sid)
            .orElseGet(() -> createCartForSession(sid));
    }
}

// ログイン成功後のカートマージ（AuthenticationSuccessHandler で呼び出す）
@Transactional
public void mergeSessionCart(String sessionCartId, String userId) {
    cartRepository.findBySessionId(sessionCartId).ifPresent(sessionCart -> {
        Cart userCart = getOrCreateCart(null, userId); // user_id で取得
        sessionCart.getItems().forEach(item -> addItemToCart(userCart, item));
        sessionCart.setStatus("MERGED");
        cartRepository.save(sessionCart);
    });
}

// チェックアウトダウンストリームから呼び出す独自メソッド（カートを新規作成しない）
@Transactional(readOnly = true)
public Cart getActiveCart(String userId) {
    return cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
        .orElseThrow(() -> new BusinessException("cart.not.found", "/cart"));
}
```

### 6.7 グローバル例外ハンドラー設計

`@ControllerAdvice` で一元的な例外処理を実装し、ユーザーに適切なエラーページを返す。

```java
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // 404: リソースが見つからない
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    // 422: ビジネスルール違反（在庫不足・クーポン無効等）
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String handleBusiness(BusinessException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + ex.getRedirectUrl(); // 呼び出し元画面へリダイレクト
    }

    // 403: アクセス拒否
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied() {
        return "error/403";
    }

    // 500: 予期しないサーバーエラー
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unexpected error", ex); // スタックトレースをログに記録
        model.addAttribute("errorId", UUID.randomUUID()); // ユーザー提示用エラーID
        return "error/500";
    }
}
```

**カスタム例外クラス**:

| クラス | HTTP ステータス | 用途 |
|-------|--------------|------|
| `ResourceNotFoundException` | 404 | 商品・注文・ユーザーが DB に存在しない |
| `BusinessException` | 422 | 在庫不足・クーポン期限切れ・注文キャンセル不可等のビジネスルール違反 |
| `AuthenticationException` | 401 | カスタム認証エラー（Spring Security の例外とは別） |

`BusinessException` のクラス設計（`redirectUrl` フィールドが必須）:

```java
public class BusinessException extends RuntimeException {
    private final String redirectUrl;
    private final String messageKey;

    public BusinessException(String messageKey, String redirectUrl) {
        super(messageKey);
        this.messageKey = messageKey;
        this.redirectUrl = redirectUrl;
    }

    // リダイレクト先を指定しない場合のコンストラクタ
    public BusinessException(String messageKey) {
        this(messageKey, "/");
    }

    public String getRedirectUrl() { return redirectUrl; }
    public String getMessageKey()  { return messageKey; }
}
```

---

## 7. サービス層移行設計

### 7.1 サービス設計原則

- **`@Service` アノテーション**: 全サービスクラスに付与
- **`@Transactional` で一貫性確保**: DB を更新する全メソッドに付与（読み取りは `readOnly = true`）
- **コンストラクタインジェクション**: Repository を受け取る
- **例外変換**: DAO レイヤーの例外はサービス層でビジネス例外に変換

```java
// 変換例: AuthService
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Transactional
    public AuthResult authenticate(String email, String password, String ip, String ua) {
        // ... BCryptPasswordEncoder で検証
    }
}
```

### 7.2 トランザクション境界

| 操作 | アノテーション |
|------|-------------|
| 読み取り専用 | `@Transactional(readOnly = true)` |
| 単一テーブル更新 | `@Transactional` |
| 複数テーブル更新（注文確定等） | `@Transactional`（Service メソッドレベルで一括） |
| バッチ処理 | `@Transactional(propagation = Propagation.REQUIRES_NEW)` |

### 7.3 CheckoutService 実装設計（最複雑サービス）

注文確定は最も多くのリポジトリにまたがるサービスメソッドである。全ステップを **1 つの `@Transactional` で原子化**し、途中で例外が発生した場合は全ステップをロールバックする。

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final PointService pointService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final MailService mailService;

    @Transactional
    public Order checkout(CheckoutRequest request, String userId) {
        // 1. カートからアイテム取得
        Cart cart = cartService.getActiveCart(userId);
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) throw new BusinessException("cart.empty");

        // 2. 在庫確認（不足したら BusinessException をスロー → ロールバック）
        inventoryService.checkStock(items);

        // 3. クーポン適用（割引額を計算）
        BigDecimal discount = couponService.apply(request.getCouponCode(), userId, cart.getSubtotal());

        // 4. ポイント使用確認・仮消費
        pointService.reservePoints(userId, request.getUsePoints());

        // 5. 注文レコード作成
        Order order = buildOrder(request, userId, cart, discount);
        orderRepository.save(order);

        // 6. 注文明細作成
        List<OrderItem> orderItems = buildOrderItems(order, items);
        orderItemRepository.saveAll(orderItems);

        // 7. 在庫減算
        inventoryService.deductStock(items);

        // 8. 支払いレコード作成
        Payment payment = buildPayment(order, request.getPaymentMethod());
        paymentRepository.save(payment);

        // 9. ポイント確定付与（注文金額に応じたポイント数は PointService が内部計算。orders スキーマには point_award カラムなし）
        pointService.awardPoints(userId, order.getId(), order.getTotalAmount());

        // 10. カートクリア
        cartService.clearCart(cart.getId());

        // 11. 受注確認メールをキューに追加（同一トランザクション内）
        mailService.enqueueOrderConfirmation(order, userId);

        return order;
    }
}
```

**ステップ順序の注意点**:
- 在庫確認（ステップ 2）は在庫減算（ステップ 7）より必ず前に行う
- ポイント仮消費（ステップ 4）と確定付与（ステップ 9）は `PointTransaction` の `type` で区別する
- メールキュー追加（ステップ 11）は同一トランザクション内で行い、注文確定と同時にコミットする

---

## 8. データアクセス層移行設計

### 8.1 Repository 設計

JPA Repository を使用し、標準メソッドと JPQL/Criteria API で実装する。

```java
// 変換例: UserDao → UserRepository
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(String status);
}

// ⚠️ 重要: SecurityLog に関するクエリは UserRepository に含めない。
// 各 Repository は自分の Aggregate Root のみを扱うこと（Repository per entity 原則）。
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {
    // メソッド名クエリで Spring Data が SQL を自動生成する（cross-entity JPQL 禁止）
    long countByUserIdAndEventType(String userId, String eventType);
    List<SecurityLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
```

### 8.2 複雑クエリの扱い

| 現行 SQL | 移行方針 |
|---------|---------|
| 単純な WHERE 条件 | Spring Data メソッド名クエリ |
| JOIN を含むクエリ | `@Query` JPQL または `@Query(nativeQuery=true)` |
| 動的検索クエリ（商品検索等） | `JpaSpecificationExecutor` + `Specification` |

### 8.3 DataSource 設定

現行の `DataSourceLocator`（Commons DBCP + app.properties）を Spring Boot の自動設定（HikariCP）に置き換える。

```properties
# application.properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/skishop}
spring.datasource.username=${DB_USERNAME:skishop}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=10000
```

### 8.4 JPA エンティティ設計原則

- `@Entity` + `@Table(name = "...")` でスキーマに対応
- 既存 DB スキーマは変更しない（カラム名・テーブル名を `@Column(name = "...")` で明示）
- UUID 主キーは `String` 型で管理（`@Id` + `@GeneratedValue` なし）
- `created_at` / `updated_at` は `@CreationTimestamp` / `@UpdateTimestamp` を活用
- `@OneToMany`, `@ManyToOne` は適切な `FetchType` を設定（デフォルト LAZY を推奨）
- **N+1 問題対策**: 一覧系クエリで関連エンティティが必要な場合は `@EntityGraph` または `JOIN FETCH` を使用する。`@BatchSize(size = 50)` をコレクション関連に付与して N+1 を軽減する。
- **カスケード設定**: 親子関係の `@OneToMany` には `cascade = CascadeType.ALL, orphanRemoval = true` を設定する。ただし独立エンティティ（例: `Order` と `User`）には `cascade` を設定しない。

```java
// 例: Order → OrderItem のカスケード設定
@Entity
@Table(name = "orders")
public class Order {
    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}

// 例: Cart → CartItem のカスケード設定
@Entity
@Table(name = "carts")
public class Cart {
    @OneToMany(mappedBy = "cart",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<CartItem> items = new ArrayList<>();
}
```

> **注意**: `CascadeType.ALL` は「親を保存すると子も保存」「親を削除すると子も削除」を意味する。意図しないカスケード削除を防ぐため、独立した集約（`User`→`Order` 等）には cascade を設定しないこと。

```java
// 変換例: User POJO → User JPA Entity
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "salt", nullable = false, length = 255)
    private String salt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    // ... getters/setters
}
```

---

## 9. ドメインモデル移行設計

### 9.1 POJO → JPA Entity 変換ルール

| 現行 | 移行後 |
|------|--------|
| `java.util.Date` フィールド | `java.time.LocalDateTime` / `LocalDate` |
| `String id` (UUID) | `String id` + `@Id`（自動生成なし） |
| `String status` (文字列) | `@Enumerated(EnumType.STRING)` Enum 推奨 |
| getter/setter のみ | getter/setter + `@Entity`, `@Table`, `@Column` |

### 9.2 Java 21 機能活用

- **レコードクラス**: DTO（不変データ）に使用
- **パターンマッチング**: `instanceof` チェックを簡潔に
- **テキストブロック**: SQL クエリや JSON テンプレート
- **sealed クラス**: 決済結果等の限定的な型階層

---

## 10. ビュー層移行設計（JSP → Thymeleaf）

### 10.1 Tiles → Thymeleaf Layout Dialect

| Tiles 概念 | Thymeleaf 相当 |
|-----------|--------------|
| `tiles-defs.xml` の `baseLayout` | `fragments/layout.html` |
| `<tiles:insertDefinition>` | `layout:decorate="~{fragments/layout}"` |
| `<tiles:getAsString name="body">` | `th:fragment="content"` |
| `<put name="title" value="...">` | `th:block layout:fragment="title"` |
| `<put name="header">` | `layout:fragment="header"` |

### 10.2 Struts タグ → Thymeleaf 変換

| Struts タグ | Thymeleaf 相当 |
|------------|--------------|
| `<html:form action="/login.do">` | `<form th:action="@{/auth/login}" method="post">` |
| `<html:text property="email">` | `<input th:field="*{email}">` |
| `<html:errors property="email">` | `<span th:errors="*{email}">` |
| `<logic:present name="loginUser">` | `<div th:if="${#authentication.authenticated}">` |
| `<bean:write name="user" property="username">` | `<span th:text="${user.username}">` |
| `<logic:iterate collection="products">` | `<tr th:each="product : ${products}">` |
| `<html:link page="/product.do?id=...">` | `<a th:href="@{/products/{id}(id=${product.id})}">` |
| `<bean:message key="label.xxx">` | `<span th:text="#{label.xxx}">` |

### 10.3 JSP ファイル → Thymeleaf ファイルマッピング

| 現行 JSP パス | 移行後 Thymeleaf パス |
|-------------|---------------------|
| `/WEB-INF/jsp/home.jsp` | `templates/home.html` |
| `/WEB-INF/jsp/auth/login.jsp` | `templates/auth/login.html` |
| `/WEB-INF/jsp/auth/register.jsp` | `templates/auth/register.html` |
| `/WEB-INF/jsp/products/list.jsp` | `templates/products/list.html` |
| `/WEB-INF/jsp/products/detail.jsp` | `templates/products/detail.html` |
| `/WEB-INF/jsp/cart/view.jsp` | `templates/cart/view.html` |
| `/WEB-INF/jsp/cart/checkout.jsp` | `templates/checkout/index.html` |
| `/WEB-INF/jsp/orders/history.jsp` | `templates/orders/list.html` |
| `/WEB-INF/jsp/orders/detail.jsp` | `templates/orders/detail.html` |
| `/WEB-INF/jsp/common/header.jsp` | `templates/fragments/header.html` |
| `/WEB-INF/jsp/common/footer.jsp` | `templates/fragments/footer.html` |
| `/WEB-INF/jsp/layouts/base.jsp` | `templates/fragments/layout.html` |

### 10.4 CSRF トークンの Thymeleaf 統合

Spring Security + Thymeleaf では、`<form>` タグに CSRF トークンが自動的に挿入される（Thymeleaf Security 拡張使用）。

```html
<!-- 自動挿入される（追加作業不要） -->
<form th:action="@{/auth/login}" method="post">
  <!-- <input type="hidden" name="_csrf" th:value="${_csrf.token}"> が自動挿入 -->
</form>
```

### 10.5 メールテンプレート移行

現行の `src/main/resources/mail/` 配下にあるメールテンプレート（テキスト形式）を Thymeleaf HTML テンプレートに移行する。

| 現行テンプレート | 移行後テンプレート | 用途 |
|----------------|-----------------|------|
| `mail/order_confirmation.txt` | `templates/mail/order-confirmation.html` | 受注確認メール |
| `mail/password_reset.txt` | `templates/mail/password-reset.html` | パスワードリセットメール |
| `mail/register.txt` | `templates/mail/register.html` | 会員登録完了メール |

`MailService` は `TemplateEngine`（Thymeleaf）でテンプレートをレンダリングし、HTML メールとして送信する。

```java
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void enqueueOrderConfirmation(Order order, String userId) {
        Context ctx = new Context();
        ctx.setVariable("order", order);
        String html = templateEngine.process("mail/order-confirmation", ctx);
        // EmailQueue に登録（同一トランザクション内）
        EmailQueue eq = new EmailQueue();
        eq.setToEmail(/* user email */);
        eq.setSubject("ご注文確認 #" + order.getOrderNumber());
        eq.setBody(html);
        eq.setStatus("PENDING");
        emailQueueRepository.save(eq);
    }
}
```

---

## 11. セキュリティ移行設計

### 11.1 Spring Security 設定

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize をサービス層で使用可能にする
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/account/**", "/orders/**", "/checkout/**",
                                 "/cart/coupon", "/points/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")  // Actuator を管理者のみに制限
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()  // セッション固定攻撃防止
                .maximumSessions(1)
            )
            .csrf(Customizer.withDefaults())  // CSRF 保護有効化
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())  // Spring Security 6.x では enable() は非推奨。Thymeleaf デフォルトエスケープ + CSP で XSS を防御する
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            );
        return http.build();
    }
}
```

> **方針**: URL レベルの認可（`SecurityFilterChain`）に加え、サービスメソッドには `@PreAuthorize("hasRole('ADMIN')")` を付与して多層防御を実現する。URL マッピングが変わっても認可が有効に機能する。

### 11.2 パスワードハッシュ移行戦略

現行はカスタム SHA-256 + ソルト方式（1000 イテレーション）。Spring Security BCrypt への移行は段階的に行う。

**方針**: `DelegatingPasswordEncoder` を使用し、既存ユーザーのログイン時に BCrypt へ自動アップグレードする。

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // 既存のカスタムハッシュを認識させるためのカスタムエンコーダー
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());         // 新規登録・更新後
    encoders.put("sha256", new LegacySha256PasswordEncoder());  // 既存ユーザーの互換用
    return new DelegatingPasswordEncoder("bcrypt", encoders);
}
```

**DB への {prefix} + ソルト付与（Flyway マイグレーション SQL）**:

> **重要**: 現行 DB は `users.password_hash`（ハッシュ値）と `users.salt`（ソルト）を別カラムで管理している。`DelegatingPasswordEncoder` の `matches(rawPassword, encodedPassword)` にはソルトを渡す手段がないため、V2 マイグレーションでハッシュ値とソルトを `$` でつなげた形式に変換し、`LegacySha256PasswordEncoder` がパース可能にする。

```sql
-- Flyway: V2__add_password_prefix.sql
-- 既存パスワードハッシュに {sha256} プレフィックスとソルトを埋め込む
-- 形式: {sha256}<hash>$<salt>
UPDATE users SET password_hash = CONCAT('{sha256}', password_hash, '$', salt)
WHERE password_hash NOT LIKE '{%}%';
```

**ロールバック SQL**:
```sql
-- ロールバック時: プレフィックスとソルト埋め込みを除去してハッシュのみ復元
UPDATE users
SET password_hash = split_part(substring(password_hash from length('{sha256}') + 1), '$', 1)
WHERE password_hash LIKE '{sha256}%';
```

**`LegacySha256PasswordEncoder` 実装**:

```java
/**
 * 既存の SHA-256+ソルト形式パスワードを検証するエンコーダー。
 * 段階移行用として DelegatingPasswordEncoder に登録する。
 * 新規登録・BCrypt アップグレード後は使用されなくなる（段階廃止）。
 */
public class LegacySha256PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        // 新規登録時は DelegatingPasswordEncoder が bcrypt を選択するため呼ばれない
        throw new UnsupportedOperationException(
                "LegacySha256PasswordEncoder does not support encoding. " +
                "New passwords must be encoded with BCrypt.");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedHashWithSalt) {
        // storedHashWithSalt 形式: "<hash>$<salt>"（{sha256} プレフィックスは DelegatingPasswordEncoder が除去済み）
        int sep = storedHashWithSalt.lastIndexOf('$');
        if (sep < 0) {
            return false; // 予期しない形式
        }
        String hash = storedHashWithSalt.substring(0, sep);
        String salt = storedHashWithSalt.substring(sep + 1);
        return PasswordHasher.hash(rawPassword.toString(), salt).equals(hash);
    }
}
```

**ログイン成功時の自動アップグレード（`UserDetailsPasswordService` 実装）**:

Spring Security は `DelegatingPasswordEncoder` が古い形式を検知すると、`UserDetailsPasswordService.updatePassword()` を自動的に呼び出す。これを `CustomUserDetailsService` で実装することで、ログイン成功時に BCrypt へ自動アップグレードされる。

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService, UserDetailsPasswordService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())  // {sha256}hash$salt または {bcrypt}hash
            .roles(user.getRole())
            .accountLocked("LOCKED".equals(user.getStatus()))
            .build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails userDetails, String newEncodedPassword) {
        // Spring Security が BCrypt アップグレード後に呼び出す
        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            user.setPasswordHash(newEncodedPassword);
            userRepository.save(user);
        });
        return org.springframework.security.core.userdetails.User.withUserDetails(userDetails)
            .password(newEncodedPassword).build();
    }
}
```

### 11.3 認可モデル

| 現行 Struts ロール | Spring Security ロール |
|------------------|---------------------|
| `roles="USER,ADMIN"` | `.hasAnyRole("USER", "ADMIN")` |
| `roles="ADMIN"` | `.hasRole("ADMIN")` |
| （なし） | `.permitAll()` |

---

## 12. 設定ファイル移行設計

### 12.1 設定ファイル対応

| 現行ファイル | 移行後 |
|------------|--------|
| `app.properties` | `application.properties` + プロファイル別ファイル |
| `log4j.properties` | `application.properties`（Spring Boot logging 設定） |
| `struts-config.xml` | `@RequestMapping` アノテーション（廃止） |
| `validation.xml` / `validator-rules.xml` | Bean Validation アノテーション（廃止） |
| `tiles-defs.xml` | Thymeleaf Layout Dialect（廃止） |
| `web.xml` | Spring Boot AutoConfiguration（廃止） |
| `messages.properties` | `src/main/resources/messages.properties`（継続） |

### 12.2 application.properties プロファイル設計

```
src/main/resources/
├── application.properties          # 共通設定（安全なデフォルト値のみ）
├── application-dev.properties      # 開発環境（ローカル PostgreSQL / DEBUG ログ）
├── application-test.properties     # テスト環境（H2 インメモリ / DDL=create-drop）
├── application-staging.properties  # ステージング環境（PostgreSQL / INFO ログ）
└── application-prod.properties     # 本番環境（環境変数参照 / WARN ログ）
```

**機密情報は環境変数で参照** (`${DB_PASSWORD}` 等)。設定ファイルへの直接記述は禁止。

### 12.3 Flyway スキーマ管理

DB スキーマの変更履歴を Flyway で管理する。

```
src/main/resources/db/migration/
├── V1__initial_schema.sql      # 現行 schema.sql を転記
├── V2__add_password_prefix.sql # パスワードハッシュプレフィックス付与（§11.2 参照）
└── R__seed_data.sql            # テスト/開発用初期データ（Repeatable）
```

```properties
# application.properties（Flyway 設定）
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true  # 既存 DB へのベースライン適用
```

### 12.4 Logback 設定

`X-Request-Id` トレーシングを Logback パターンに組み込む。

```xml
<!-- logback-spring.xml -->
<configuration>
  <springProfile name="!prod">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{reqId}] %logger{36} - %msg%n</pattern>
      </encoder>
    </appender>
  </springProfile>
  <springProfile name="prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
  </springProfile>
</configuration>
```

> **依存関係**: 本番 JSON ログ（`LogstashEncoder`）のために `pom.xml` に `net.logstash.logback:logstash-logback-encoder`（例: `8.0`）を追加すること。Spring Boot BOM 外のため明示バージョン指定が必要。

> **重要**: ログにメールアドレス・住所・クレジットカード番号等の PII (個人識別情報) を出力しない。`SecurityLog` には IP アドレスとイベント種別のみ記録し、パスワード値は絶対にログに出力しない。

---

## 13. テスト戦略

### 13.1 テスト種別と技術

| テスト種別 | 技術 | 対象 |
|----------|------|------|
| ユニットテスト | JUnit 5 + Mockito | Service, Util |
| スライステスト（Web） | `@WebMvcTest` | Controller |
| スライステスト（DB） | `@DataJpaTest` + H2 | Repository |
| 統合テスト | `@SpringBootTest` | 全レイヤー |
| セキュリティテスト | Spring Security Test | 認証/認可 |

### 13.2 テストカバレッジ目標

| レイヤー | 目標カバレッジ |
|---------|-------------|
| Service | 80% 以上 |
| Controller | 70% 以上（`@WebMvcTest`） |
| Repository | 70% 以上（`@DataJpaTest`） |
| 全体 | 70% 以上 |

### 13.3 移行前後の動作等価性確認

各機能について、移行前後で以下を確認する:
1. 正常系シナリオ（入力値 → 期待する画面遷移/DB 変更）
2. 異常系シナリオ（バリデーションエラー、業務エラー）
3. 認可制御（ロール別アクセス制御）
4. セッション管理（ログイン/ログアウト）

---

## 14. 非機能要件

### 14.1 パフォーマンス

| 項目 | 目標値 |
|------|--------|
| 商品一覧 API レスポンス | 300ms 以内（P95） |
| 注文確定処理 | 1000ms 以内（P95） |
| コネクションプール | HikariCP 最大 20 接続 |
| JVM ヒープ | 512MB（起動時）/ 1GB（最大） |

### 14.2 可観測性

| 項目 | 実装方法 |
|------|---------|
| ヘルスチェック | `/actuator/health` |
| メトリクス | `/actuator/prometheus` (Micrometer) |
| リクエスト追跡 | `X-Request-Id` ヘッダー（現行の RequestIdFilter を `OncePerRequestFilter` で継続） |
| アクセスログ | Logback アクセスログ・Tomcat アクセスログ |

### 14.3 セキュリティ非機能要件

| 要件 | 実装 |
|-----|------|
| HTTPS 強制 | Reverse Proxy（Nginx/ALB）または HSTS |
| XSS 対策 | Thymeleaf のデフォルトエスケープ |
| CSRF 保護 | Spring Security CSRF フィルタ |
| セッション固定攻撃防止 | `sessionFixation().migrateSession()` |
| ブルートフォース防止 | 既存の 5 回失敗でアカウントロック継続 |
| Content Security Policy | `SecurityConfig` でヘッダー設定 |

---

## 15. 既知の課題と制限事項

### 15.1 パスワード互換性

既存ユーザーのパスワードハッシュは SHA-256 形式。BCrypt への移行には以下の課題がある：
- 既存ハッシュを BCrypt に変換することはできない（一方向ハッシュ）
- **対策**: `DelegatingPasswordEncoder` でログイン時にアップグレード、または全ユーザーにパスワード再設定を要求

### 15.2 セッション属性の移行

現行では `session.setAttribute("loginUser", User)` で `User` POJO をセッションに格納している。Spring Security では `UserDetails` として管理するため、`User` エンティティを `UserDetails` 実装に変換する設計が必要。

### 15.3 ファイルアップロード

`commons-fileupload` を使用している場合、Spring Boot の `MultipartFile` に移行する。

### 15.4 メール送信の非同期化

現行の `MailService` は同期送信。`@Async` または Spring Batch/Scheduler での非同期化を推奨するが、本移行では同期送信で機能等価性を優先する。

### 15.5 URL 変更による影響

`*.do` から RESTful URL への変更は、メール内のリンク・ブックマーク等に影響する可能性がある。Nginx や Spring の URL リダイレクトでの対応を検討すること。
