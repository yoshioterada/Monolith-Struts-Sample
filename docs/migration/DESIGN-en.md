# SkiShop Migration Detailed Design Document
## Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x

**Document Version**: 1.0  
**Created**: 2026-03-31  
**Target Project**: `appmod-migrated-java21-spring-boot-3rd`  
**Source**: `skishop-monolith` (Java 5 / Struts 1.3.10 / Tomcat 6)  
**Target**: Java 21 / Spring Boot 3.2.x / Embedded Tomcat (JAR)

---

## Table of Contents

1. [Migration Overview](#1-migration-overview)
2. [Current System Analysis](#2-current-system-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Technology Stack Conversion Mapping](#4-technology-stack-conversion-mapping)
5. [Package Structure Design](#5-package-structure-design)
6. [Web Layer Migration Design](#6-web-layer-migration-design)
7. [Service Layer Migration Design](#7-service-layer-migration-design)
8. [Data Access Layer Migration Design](#8-data-access-layer-migration-design)
9. [Domain Model Migration Design](#9-domain-model-migration-design)
10. [View Layer Migration Design (JSP → Thymeleaf)](#10-view-layer-migration-design-jsp--thymeleaf)
11. [Security Migration Design](#11-security-migration-design)
12. [Configuration File Migration Design](#12-configuration-file-migration-design)
13. [Test Strategy](#13-test-strategy)
14. [Non-Functional Requirements](#14-non-functional-requirements)
15. [Known Issues and Limitations](#15-known-issues-and-limitations)

---

## 1. Migration Overview

### 1.1 Purpose of the Migration

This migration aims to modernize the EOL (End of Life) Java 5 / Struts 1.3 monolith into Java 21 / Spring Boot 3.2.x, which offers superior maintainability, security, and extensibility.

### 1.2 Migration Policy

| Policy | Details |
|--------|---------|
| **Maintain Functional Equivalence** | All existing business features must be available in the target system |
| **Incremental Migration** | Proceed in phases, verifying compilation and tests at each stage |
| **Preserve Existing DB Schema** | The DB schema will not be modified in principle (additions only) |
| **New Project in Separate Directory** | Create `appmod-migrated-java21-spring-boot-3rd/` without modifying the original project |
| **Spring BOM First** | Manage dependency versions via the Spring Boot BOM, minimizing individual version specifications |
| **Schema Version Control** | Track and manage schema changes using Flyway |

### 1.3 Migration Scope

**In Scope (Required)**:
- All Struts Actions → Spring MVC Controllers
- All ActionForms → DTOs with Bean Validation
- JSP + Tiles → Thymeleaf Templates
- JDBC DAOs → Spring Data JPA Repositories
- Log4j 1.x → SLF4J + Logback (managed by Spring Boot)
- Custom Authentication → Spring Security
- app.properties → application.properties (profile-based separation)
- Custom Connection Pool (Commons DBCP) → HikariCP (managed by Spring Boot)
- WAR → JAR (embedded Tomcat)

**Out of Scope (Tracked as Separate Tasks)**:
- Microservices decomposition
- Frontend SPA migration
- API Gateway introduction
- Asynchronous messaging migration

---

## 2. Current System Analysis

### 2.1 Current Technology Stack

| Layer | Current Technology | Version | EOL Status |
|-------|-------------------|---------|------------|
| Language | Java | 1.5 | EOL (2009) |
| Framework | Apache Struts | 1.3.10 | EOL (2013) |
| Build | Maven | 2.x | EOL |
| Servlet Container | Tomcat | 6.x | EOL (2016) |
| DB Access | Commons DBCP + commons-dbutils | 1.2.2 / 1.1 | Legacy |
| DB Driver | PostgreSQL JDBC | 9.2 JDBC3 | EOL |
| Logging | Log4j | 1.2.17 | EOL (2015) + Log4Shell vulnerability |
| Mail | javax.mail | 1.4.7 | EOL (migrated to jakarta.mail) |
| Testing | JUnit | 4.12 | Legacy |

### 2.2 Current Directory Structure

```
src/main/java/com/skishop/
├── common/
│   ├── config/         # AppConfig (Singleton configuration loader)
│   ├── dao/            # AbstractDao, DataSourceLocator, DaoException
│   ├── service/        # BaseService (if present)
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
├── domain/             # POJO entities (getters/setters only)
│   ├── address/, cart/, coupon/, inventory/, mail/
│   ├── order/, payment/, point/, product/, shipping/, user/
├── service/            # Business logic (dependencies created via `new`)
│   ├── auth/, cart/, catalog/, coupon/, inventory/
│   ├── mail/, order/, payment/, point/, shipping/, tax/, user/
└── web/
    ├── action/         # Struts Action classes (+ admin/)
    ├── filter/         # RequestIdFilter, CharacterEncodingFilter
    ├── form/           # ActionForm subclasses (+ admin/)
    ├── processor/      # Custom RequestProcessor (if present)
    └── tag/            # Custom tags (if present)
```

### 2.3 Current Action List (Migration Targets)

| Action Class | URL Path | Role | Target Controller |
|-------------|----------|------|-------------------|
| `LoginAction` | /login | All | `AuthController` |
| `LogoutAction` | /logout | USER,ADMIN | `AuthController` |
| `RegisterAction` | /register | All | `AuthController` |
| `PasswordForgotAction` | /password/forgot | All | `AuthController` |
| `PasswordResetAction` | /password/reset | All | `AuthController` |
| `ProductListAction` | /products | All | `ProductController` |
| `ProductDetailAction` | /product | All | `ProductController` |
| `CartAction` | /cart | All | `CartController` |
| `CheckoutAction` | /checkout | USER,ADMIN | `CheckoutController` |
| `CouponApplyAction` | /coupon/apply | USER,ADMIN | `CouponController` |
| `CouponAvailableAction` | /coupons/available | All | `CouponController` |
| `OrderHistoryAction` | /orders | USER,ADMIN | `OrderController` |
| `OrderDetailAction` | /orders/detail | USER,ADMIN | `OrderController` |
| `OrderCancelAction` | /orders/cancel | USER,ADMIN | `OrderController` |
| `OrderReturnAction` | /orders/return | USER,ADMIN | `OrderController` |
| `PointBalanceAction` | /points | USER,ADMIN | `AccountController` (consolidated under /account/points) |
| `AddressListAction` | /addresses | USER,ADMIN | `AccountController` (consolidated under /account/addresses) |
| `AddressSaveAction` | /addresses/save | USER,ADMIN | `AccountController` (consolidated under /account/addresses) |
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

### 2.4 Current DAO List

| DAO Interface | Domain | Primary Methods |
|--------------|--------|-----------------|
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

### 2.5 Current Service List

| Service Class | Dependent DAOs |
|--------------|---------------|
| `AuthService` | UserDao, SecurityLogDao |
| `UserService` | UserDao, PasswordResetTokenDao |
| `CartService` | CartDao, CartItemDao, ProductDao, PriceDao |
| `ProductService` / `CatalogService` | ProductDao, PriceDao, CategoryDao, InventoryDao |
| `InventoryService` | InventoryDao |
| `CouponService` | CouponDao, CouponUsageDao |
| `OrderService` | OrderDao, OrderItemDao, CartDao, PaymentDao |
| `PaymentService` | PaymentDao, OrderDao |
| `ShippingService` | ShippingMethodDao |
| `TaxService` | (configuration values only) |
| `PointService` | PointAccountDao, PointTransactionDao |
| `MailService` | EmailQueueDao |
| `AddressService` | UserAddressDao |

### 2.6 DB Schema

Primary tables (from schema.sql):
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

### 2.7 Current Security Issues

| Issue | Severity | Migration Countermeasure |
|-------|----------|------------------------|
| SHA-256 + custom salt (BCrypt not used) | High | Migrate to Spring Security BCrypt (existing passwords via re-registration flow) |
| No session fixation attack prevention | High | Addressed by Spring Security's default protection |
| No CSRF protection | High | Addressed by Spring Security CSRF tokens |
| Log4Shell (Log4j 1.x) | Critical | Replace with SLF4J + Logback (managed by Spring Boot) |
| Outdated version of `commons-dbutils` | Medium | Replace with Spring Data JPA |

---

## 3. Target Architecture

### 3.1 Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.2.x |
| Web | Spring MVC | 6.1.x (managed by Boot) |
| View | Thymeleaf | 3.1.x (managed by Boot) |
| View Thymeleaf Extension | thymeleaf-extras-springsecurity6 | Managed by Boot |
| View Tiles Replacement | thymeleaf-layout-dialect | 3.x |
| Security | Spring Security | 6.2.x (managed by Boot) |
| DB Access | Spring Data JPA + Hibernate | 3.2.x (managed by Boot) |
| DB Schema Management | Flyway | 10.x (managed by Boot) |
| DB Driver | postgresql | 42.7.x (managed by Boot) |
| Connection Pool | HikariCP | 5.x (managed by Boot) |
| Validation | Hibernate Validator (Bean Validation 3.0) | Managed by Boot |
| Logging | SLF4J + Logback | Managed by Boot |
| Mail | Spring Boot Mail (jakarta.mail) | Managed by Boot |
| Testing | JUnit 5 + Spring Boot Test | Managed by Boot |
| Metrics | Spring Boot Actuator + Micrometer | Managed by Boot |
| API Documentation | springdoc-openapi | 2.3.x |
| Build | Maven | 3.9.x |
| Packaging | JAR (embedded Tomcat) | — |

### 3.2 Architecture Overview

```
Browser / Client
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

### 3.3 Layer Responsibilities

| Layer | Package | Responsibility |
|-------|---------|---------------|
| Web (Controller) | `controller/` | Accept HTTP requests, input validation, response construction |
| Web (DTO) | `dto/` | Request/response type definitions and validation annotations |
| Service | `service/` | Business logic, transaction management |
| Repository | `repository/` | Data access (Spring Data JPA interfaces) |
| Model (Entity) | `model/` | JPA entities |
| Config | `config/` | Spring configuration classes |
| Exception | `exception/` | Custom exceptions, global exception handler |

---

## 4. Technology Stack Conversion Mapping

### 4.1 Framework Mapping

| Current (Struts) | Target (Spring Boot) | Conversion Strategy |
|-----------------|---------------------|---------------------|
| `Action` extends `org.apache.struts.action.Action` | `@Controller` + `@RequestMapping` | 1 Action → 1 Controller method (consolidated by URL group) |
| `ActionForm` extends `org.apache.struts.action.ActionForm` | `record` / POJO + Bean Validation annotations | Forms expressed as record classes |
| `ActionForward` | `String` (template name) / `redirect:` prefix | Return template name directly |
| `ActionMessages` / `ActionMessage` | `BindingResult`, `RedirectAttributes.addFlashAttribute` | Spring MVC binding results |
| `ActionServlet` | Not needed (managed by Spring DispatcherServlet) | — |
| `struts-config.xml` | `@RequestMapping` annotations + `application.properties` | XML eliminated |
| `validation.xml` / `validator-rules.xml` | Bean Validation annotations + `@Valid` | XML eliminated |
| Struts Tiles | Thymeleaf Layout Dialect | Tile definitions → layout templates |
| `*.do` URL pattern | Unified to `/` (RESTful) | Remove `.do` suffix from URLs |
| `javax.servlet.*` | `jakarta.servlet.*` | Namespace change only |

### 4.2 Layer-by-Layer Conversion Details

#### Web Layer

```
[Before]
public class LoginAction extends Action {
    private final AuthService authService = new AuthService(); // created via new
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest req, HttpServletResponse res) {
        LoginForm loginForm = (LoginForm) form;
        // ...
        return mapping.findForward("success");
    }
}

[After]
@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;          // injected via DI

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

#### DAO Layer

```
[Before]
public class UserDaoImpl extends AbstractDao implements UserDao {
    public User findByEmail(String email) {
        Connection con = getConnection(); // manual connection management
        PreparedStatement ps = con.prepareStatement("SELECT ... WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        // ...
        closeQuietly(rs, ps, con);
    }
}

[After]
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
}
```

### 4.3 Dependency Injection (DI) Conversion

| Current Pattern | Migrated Pattern |
|----------------|-----------------|
| `private final AuthService authService = new AuthService();` | `@Autowired` / Constructor injection |
| `AppConfig.getInstance().getString(key)` | `@Value("${key}")` / `@ConfigurationProperties` |
| `DataSourceLocator.getInstance().getDataSource()` | Spring Boot AutoConfiguration (HikariCP auto-configured) |

---

## 5. Package Structure Design

### 5.1 Target Package Structure

```
com.skishop/
├── SkiShopApplication.java              # @SpringBootApplication
├── config/
│   ├── SecurityConfig.java              # Spring Security configuration
│   ├── JpaConfig.java                   # JPA configuration (if needed)
│   ├── MailConfig.java                  # Mail configuration
│   ├── WebMvcConfig.java                # MVC configuration (common filters, etc.)
│   ├── ThymeleafConfig.java             # Thymeleaf configuration (if needed)
│   └── CartMergeSuccessHandler.java     # Cart merge on login success (AuthenticationSuccessHandler)
├── controller/
│   ├── AuthController.java              # Login/Logout/Registration/Password Reset
│   ├── ProductController.java           # Product list/detail
│   ├── CartController.java              # Cart
│   ├── CheckoutController.java          # Checkout
│   ├── CouponController.java            # Coupon
│   ├── OrderController.java             # Order history/detail/cancel/return
│   ├── AccountController.java           # Points + Address management (consolidated from PointController/AddressController)
│   └── admin/
│       ├── AdminProductController.java  # Admin: Product management
│       ├── AdminOrderController.java    # Admin: Order management
│       ├── AdminCouponController.java   # Admin: Coupon management
│       └── AdminShippingController.java # Admin: Shipping method management
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
│   └── CustomUserDetailsService.java    # UserDetailsService + UserDetailsPasswordService implementation
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
│   └── RequestIdFilter.java             # Request ID filter (MDC + X-Request-Id header propagation)
└── util/
    ├── PasswordHasher.java              # Legacy password compatibility hash (for gradual deprecation)
    └── LegacySha256PasswordEncoder.java # Registered with DelegatingPasswordEncoder (for gradual deprecation)
```

---

## 6. Web Layer Migration Design

### 6.1 Controller Design Principles

- **1 functional group = 1 Controller**: Consolidate by functional area (authentication, products, cart, etc.)
- **Constructor injection required**: `@Autowired` field injection is prohibited
- **`@Valid` must always be applied**: All request DTOs must have validation
- **Unified response types**: Page navigation returns `String` (template name), APIs return `ResponseEntity<T>`

### 6.2 ActionForm → DTO Conversion Rules

ActionForm validation was previously defined via XML in `validation.xml`. After migration, Bean Validation annotations are applied directly to fields.

```java
// Conversion example: LoginForm → LoginRequest
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

### 6.3 ActionForward → Template Name / Redirect Conversion

| Struts ActionForward | Spring MVC Return Value |
|--------------------|------------------------|
| `mapping.findForward("success")` → path=`.home` | `"redirect:/"` |
| `mapping.findForward("failure")` → path=`auth.login` | `"auth/login"` |
| `mapping.getInputForward()` | Return the same template on validation error |
| `forward name="success" path="/home.do" redirect="true"` | `"redirect:/"` |

### 6.4 Session Management Conversion

| Current (Struts) | Migrated (Spring Security) |
|-----------------|---------------------------|
| `session.setAttribute("loginUser", user)` | Managed by Spring Security's `SecurityContextHolder` |
| `session.getAttribute("loginUser")` | `@AuthenticationPrincipal UserDetails user` |
| `session.invalidate()` | Delegated to Spring Security's logout handling |

### 6.5 URL Design

The `*.do` URL pattern is eliminated and replaced with RESTful URLs.

| Current URL | Migrated URL | HTTP Method |
|------------|-------------|-------------|
| `/login.do` (GET/POST) | `/auth/login` | GET (display) / POST (process) |
| `/logout.do` | `/auth/logout` | POST |
| `/register.do` | `/auth/register` | GET / POST |
| `/products.do` | `/products` | GET |
| `/product.do?id=xxx` | `/products/{id}` | GET |
| `/cart.do` | `/cart` | GET |
| `/cart.do` (add to cart) | `/cart/items` | POST |
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

### 6.6 Cart Session Management Design

In the current Struts application, the entire cart is stored in the session via `session.setAttribute("cart", cartPojo)`. After migration, carts are managed using a **DB-based + session ID** approach.

| State | Cart Management Method |
|-------|----------------------|
| Unauthenticated user | Store `cartId` (UUID) in HTTP session. Save in `carts` table linked by `session_id` |
| Authenticated user | Manage cart via `carts.user_id` (session ID not used) |
| Cart merge on login | Merge session cart (linked by `session_id`) into user cart, then delete session cart |

```java
// CartService: Get or create active cart
@Transactional
public Cart getOrCreateCart(HttpSession session, String userId) {
    if (userId != null) {
        // Authenticated: search by user_id
        return cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
            .orElseGet(() -> createCartForUser(userId));
    } else {
        // Unauthenticated: search by session ID
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

// Cart merge after successful login (called from AuthenticationSuccessHandler)
@Transactional
public void mergeSessionCart(String sessionCartId, String userId) {
    cartRepository.findBySessionId(sessionCartId).ifPresent(sessionCart -> {
        Cart userCart = getOrCreateCart(null, userId); // retrieve by user_id
        sessionCart.getItems().forEach(item -> addItemToCart(userCart, item));
        sessionCart.setStatus("MERGED");
        cartRepository.save(sessionCart);
    });
}

// Custom method called from checkout downstream (does not create a new cart)
@Transactional(readOnly = true)
public Cart getActiveCart(String userId) {
    return cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
        .orElseThrow(() -> new BusinessException("cart.not.found", "/cart"));
}
```

### 6.7 Global Exception Handler Design

A centralized exception handler is implemented using `@ControllerAdvice` to return appropriate error pages to users.

```java
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // 404: Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    // 422: Business rule violation (insufficient stock, invalid coupon, etc.)
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String handleBusiness(BusinessException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + ex.getRedirectUrl(); // redirect to the calling page
    }

    // 403: Access denied
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied() {
        return "error/403";
    }

    // 500: Unexpected server error
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unexpected error", ex); // log the stack trace
        model.addAttribute("errorId", UUID.randomUUID()); // error ID for user reference
        return "error/500";
    }
}
```

**Custom Exception Classes**:

| Class | HTTP Status | Usage |
|-------|------------|-------|
| `ResourceNotFoundException` | 404 | Product, order, or user not found in DB |
| `BusinessException` | 422 | Business rule violations such as insufficient stock, expired coupon, non-cancellable order, etc. |
| `AuthenticationException` | 401 | Custom authentication errors (separate from Spring Security's exception) |

`BusinessException` class design (`redirectUrl` field is required):

```java
public class BusinessException extends RuntimeException {
    private final String redirectUrl;
    private final String messageKey;

    public BusinessException(String messageKey, String redirectUrl) {
        super(messageKey);
        this.messageKey = messageKey;
        this.redirectUrl = redirectUrl;
    }

    // Constructor without redirect destination
    public BusinessException(String messageKey) {
        this(messageKey, "/");
    }

    public String getRedirectUrl() { return redirectUrl; }
    public String getMessageKey()  { return messageKey; }
}
```

---

## 7. Service Layer Migration Design

### 7.1 Service Design Principles

- **`@Service` annotation**: Applied to all service classes
- **`@Transactional` for consistency**: Applied to all DB-modifying methods (read-only uses `readOnly = true`)
- **Constructor injection**: Receives repositories via constructor
- **Exception translation**: DAO layer exceptions are converted to business exceptions at the service layer

```java
// Conversion example: AuthService
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SecurityLogRepository securityLogRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Transactional
    public AuthResult authenticate(String email, String password, String ip, String ua) {
        // ... validate with BCryptPasswordEncoder
    }
}
```

### 7.2 Transaction Boundaries

| Operation | Annotation |
|-----------|-----------|
| Read-only | `@Transactional(readOnly = true)` |
| Single table update | `@Transactional` |
| Multi-table update (e.g., order confirmation) | `@Transactional` (applied at the service method level) |
| Batch processing | `@Transactional(propagation = Propagation.REQUIRES_NEW)` |

### 7.3 CheckoutService Implementation Design (Most Complex Service)

Order confirmation is the service method that spans the most repositories. All steps are **atomized within a single `@Transactional`**, and if an exception occurs at any point, all steps are rolled back.

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
        // 1. Retrieve items from cart
        Cart cart = cartService.getActiveCart(userId);
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) throw new BusinessException("cart.empty");

        // 2. Check stock (throws BusinessException if insufficient → rollback)
        inventoryService.checkStock(items);

        // 3. Apply coupon (calculate discount amount)
        BigDecimal discount = couponService.apply(request.getCouponCode(), userId, cart.getSubtotal());

        // 4. Verify and tentatively consume points
        pointService.reservePoints(userId, request.getUsePoints());

        // 5. Create order record
        Order order = buildOrder(request, userId, cart, discount);
        orderRepository.save(order);

        // 6. Create order item records
        List<OrderItem> orderItems = buildOrderItems(order, items);
        orderItemRepository.saveAll(orderItems);

        // 7. Deduct stock
        inventoryService.deductStock(items);

        // 8. Create payment record
        Payment payment = buildPayment(order, request.getPaymentMethod());
        paymentRepository.save(payment);

        // 9. Award confirmed points (point amount based on totalAmount is calculated internally by PointService; orders schema has no point_award column)
        pointService.awardPoints(userId, order.getId(), order.getTotalAmount());

        // 10. Clear cart
        cartService.clearCart(cart.getId());

        // 11. Enqueue order confirmation email (within the same transaction)
        mailService.enqueueOrderConfirmation(order, userId);

        return order;
    }
}
```

**Important Notes on Step Ordering**:
- Stock check (step 2) must always precede stock deduction (step 7)
- Tentative point consumption (step 4) and confirmed point award (step 9) are distinguished by the `type` field in `PointTransaction`
- Email queue insertion (step 11) is performed within the same transaction, committed simultaneously with the order confirmation

---

## 8. Data Access Layer Migration Design

### 8.1 Repository Design

JPA Repositories are used, implemented with standard methods and JPQL/Criteria API.

```java
// Conversion example: UserDao → UserRepository
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(String status);
}

// ⚠️ Important: Do not include SecurityLog queries in UserRepository.
// Each Repository should only handle its own Aggregate Root (repository-per-entity principle).
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {
    // Spring Data auto-generates SQL from method name queries (cross-entity JPQL is prohibited)
    long countByUserIdAndEventType(String userId, String eventType);
    List<SecurityLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
```

### 8.2 Handling Complex Queries

| Current SQL | Migration Strategy |
|------------|-------------------|
| Simple WHERE conditions | Spring Data method name queries |
| Queries with JOINs | `@Query` JPQL or `@Query(nativeQuery=true)` |
| Dynamic search queries (e.g., product search) | `JpaSpecificationExecutor` + `Specification` |

### 8.3 DataSource Configuration

The current `DataSourceLocator` (Commons DBCP + app.properties) is replaced with Spring Boot auto-configuration (HikariCP).

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

### 8.4 JPA Entity Design Principles

- Map to the schema using `@Entity` + `@Table(name = "...")`
- The existing DB schema is not modified (explicitly specify column and table names with `@Column(name = "...")`)
- UUID primary keys are managed as `String` type (`@Id` + no `@GeneratedValue`)
- Use `@CreationTimestamp` / `@UpdateTimestamp` for `created_at` / `updated_at`
- Set appropriate `FetchType` for `@OneToMany` and `@ManyToOne` (LAZY recommended as default)
- **N+1 problem countermeasure**: When related entities are needed in list queries, use `@EntityGraph` or `JOIN FETCH`. Apply `@BatchSize(size = 50)` to collection relationships to mitigate N+1 issues.
- **Cascade configuration**: Set `cascade = CascadeType.ALL, orphanRemoval = true` for parent-child `@OneToMany` relationships. However, do not set `cascade` for independent entities (e.g., `Order` and `User`).

```java
// Example: Order → OrderItem cascade configuration
@Entity
@Table(name = "orders")
public class Order {
    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
}

// Example: Cart → CartItem cascade configuration
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

> **Note**: `CascadeType.ALL` means "saving the parent also saves children" and "deleting the parent also deletes children." To prevent unintended cascade deletions, do not set cascade on independent aggregates (e.g., `User` → `Order`).

```java
// Conversion example: User POJO → User JPA Entity
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

## 9. Domain Model Migration Design

### 9.1 POJO → JPA Entity Conversion Rules

| Current | Migrated |
|---------|----------|
| `java.util.Date` fields | `java.time.LocalDateTime` / `LocalDate` |
| `String id` (UUID) | `String id` + `@Id` (no auto-generation) |
| `String status` (string) | `@Enumerated(EnumType.STRING)` Enum recommended |
| getters/setters only | getters/setters + `@Entity`, `@Table`, `@Column` |

### 9.2 Java 21 Feature Utilization

- **Record classes**: Used for DTOs (immutable data)
- **Pattern matching**: Simplify `instanceof` checks
- **Text blocks**: SQL queries and JSON templates
- **Sealed classes**: Limited type hierarchies such as payment results

---

## 10. View Layer Migration Design (JSP → Thymeleaf)

### 10.1 Tiles → Thymeleaf Layout Dialect

| Tiles Concept | Thymeleaf Equivalent |
|--------------|---------------------|
| `tiles-defs.xml` `baseLayout` | `fragments/layout.html` |
| `<tiles:insertDefinition>` | `layout:decorate="~{fragments/layout}"` |
| `<tiles:getAsString name="body">` | `th:fragment="content"` |
| `<put name="title" value="...">` | `th:block layout:fragment="title"` |
| `<put name="header">` | `layout:fragment="header"` |

### 10.2 Struts Tags → Thymeleaf Conversion

| Struts Tag | Thymeleaf Equivalent |
|-----------|---------------------|
| `<html:form action="/login.do">` | `<form th:action="@{/auth/login}" method="post">` |
| `<html:text property="email">` | `<input th:field="*{email}">` |
| `<html:errors property="email">` | `<span th:errors="*{email}">` |
| `<logic:present name="loginUser">` | `<div th:if="${#authentication.authenticated}">` |
| `<bean:write name="user" property="username">` | `<span th:text="${user.username}">` |
| `<logic:iterate collection="products">` | `<tr th:each="product : ${products}">` |
| `<html:link page="/product.do?id=...">` | `<a th:href="@{/products/{id}(id=${product.id})}">` |
| `<bean:message key="label.xxx">` | `<span th:text="#{label.xxx}">` |

### 10.3 JSP File → Thymeleaf File Mapping

| Current JSP Path | Migrated Thymeleaf Path |
|-----------------|------------------------|
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

### 10.4 CSRF Token Thymeleaf Integration

With Spring Security + Thymeleaf, CSRF tokens are automatically inserted into `<form>` tags (using the Thymeleaf Security extension).

```html
<!-- Automatically inserted (no additional work required) -->
<form th:action="@{/auth/login}" method="post">
  <!-- <input type="hidden" name="_csrf" th:value="${_csrf.token}"> is auto-inserted -->
</form>
```

### 10.5 Email Template Migration

Email templates (text format) located under `src/main/resources/mail/` in the current system are migrated to Thymeleaf HTML templates.

| Current Template | Migrated Template | Purpose |
|-----------------|------------------|---------|
| `mail/order_confirmation.txt` | `templates/mail/order-confirmation.html` | Order confirmation email |
| `mail/password_reset.txt` | `templates/mail/password-reset.html` | Password reset email |
| `mail/register.txt` | `templates/mail/register.html` | Registration completion email |

`MailService` renders templates using `TemplateEngine` (Thymeleaf) and sends them as HTML emails.

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
        // Register in EmailQueue (within the same transaction)
        EmailQueue eq = new EmailQueue();
        eq.setToEmail(/* user email */);
        eq.setSubject("Order Confirmation #" + order.getOrderNumber());
        eq.setBody(html);
        eq.setStatus("PENDING");
        emailQueueRepository.save(eq);
    }
}
```

---

## 11. Security Migration Design

### 11.1 Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enable @PreAuthorize in the service layer
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/account/**", "/orders/**", "/checkout/**",
                                 "/cart/coupon", "/points/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")  // Restrict Actuator to admin only
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
                .sessionFixation().migrateSession()  // Session fixation attack prevention
                .maximumSessions(1)
            )
            .csrf(Customizer.withDefaults())  // Enable CSRF protection
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())  // In Spring Security 6.x, enable() is deprecated. XSS is prevented via Thymeleaf default escaping + CSP
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            );
        return http.build();
    }
}
```

> **Policy**: In addition to URL-level authorization (`SecurityFilterChain`), apply `@PreAuthorize("hasRole('ADMIN')")` to service methods for defense in depth. Authorization remains effective even if URL mappings change.

### 11.2 Password Hash Migration Strategy

The current system uses a custom SHA-256 + salt scheme (1000 iterations). Migration to Spring Security BCrypt is performed incrementally.

**Approach**: Use `DelegatingPasswordEncoder` to automatically upgrade existing users to BCrypt upon login.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // Custom encoder to recognize existing hash format
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());         // For new registrations and upgraded passwords
    encoders.put("sha256", new LegacySha256PasswordEncoder());  // Compatibility for existing users
    return new DelegatingPasswordEncoder("bcrypt", encoders);
}
```

**Adding {prefix} + salt to DB (Flyway migration SQL)**:

> **Important**: The current DB stores `users.password_hash` (hash value) and `users.salt` (salt) in separate columns. Since `DelegatingPasswordEncoder`'s `matches(rawPassword, encodedPassword)` has no way to pass the salt, the V2 migration concatenates the hash and salt with `$` as a delimiter, making it parseable by `LegacySha256PasswordEncoder`.

```sql
-- Flyway: V2__add_password_prefix.sql
-- Add {sha256} prefix and embed salt into existing password hashes
-- Format: {sha256}<hash>$<salt>
UPDATE users SET password_hash = CONCAT('{sha256}', password_hash, '$', salt)
WHERE password_hash NOT LIKE '{%}%';
```

**Rollback SQL**:
```sql
-- Rollback: Remove prefix and embedded salt, restoring hash only
UPDATE users
SET password_hash = split_part(substring(password_hash from length('{sha256}') + 1), '$', 1)
WHERE password_hash LIKE '{sha256}%';
```

**`LegacySha256PasswordEncoder` Implementation**:

```java
/**
 * Encoder that verifies passwords in the legacy SHA-256+salt format.
 * Registered with DelegatingPasswordEncoder for incremental migration.
 * Will no longer be used after new registration / BCrypt upgrade (gradual deprecation).
 */
public class LegacySha256PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        // Not called because DelegatingPasswordEncoder selects bcrypt for new registrations
        throw new UnsupportedOperationException(
                "LegacySha256PasswordEncoder does not support encoding. " +
                "New passwords must be encoded with BCrypt.");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedHashWithSalt) {
        // storedHashWithSalt format: "<hash>$<salt>" ({sha256} prefix already removed by DelegatingPasswordEncoder)
        int sep = storedHashWithSalt.lastIndexOf('$');
        if (sep < 0) {
            return false; // unexpected format
        }
        String hash = storedHashWithSalt.substring(0, sep);
        String salt = storedHashWithSalt.substring(sep + 1);
        return PasswordHasher.hash(rawPassword.toString(), salt).equals(hash);
    }
}
```

**Automatic Upgrade on Successful Login (`UserDetailsPasswordService` Implementation)**:

Spring Security automatically calls `UserDetailsPasswordService.updatePassword()` when `DelegatingPasswordEncoder` detects a legacy format. By implementing this in `CustomUserDetailsService`, passwords are automatically upgraded to BCrypt upon successful login.

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
            .password(user.getPasswordHash())  // {sha256}hash$salt or {bcrypt}hash
            .roles(user.getRole())
            .accountLocked("LOCKED".equals(user.getStatus()))
            .build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails userDetails, String newEncodedPassword) {
        // Called by Spring Security after BCrypt upgrade
        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            user.setPasswordHash(newEncodedPassword);
            userRepository.save(user);
        });
        return org.springframework.security.core.userdetails.User.withUserDetails(userDetails)
            .password(newEncodedPassword).build();
    }
}
```

### 11.3 Authorization Model

| Current Struts Role | Spring Security Role |
|--------------------|---------------------|
| `roles="USER,ADMIN"` | `.hasAnyRole("USER", "ADMIN")` |
| `roles="ADMIN"` | `.hasRole("ADMIN")` |
| (none) | `.permitAll()` |

---

## 12. Configuration File Migration Design

### 12.1 Configuration File Mapping

| Current File | Migrated |
|-------------|----------|
| `app.properties` | `application.properties` + profile-specific files |
| `log4j.properties` | `application.properties` (Spring Boot logging configuration) |
| `struts-config.xml` | `@RequestMapping` annotations (eliminated) |
| `validation.xml` / `validator-rules.xml` | Bean Validation annotations (eliminated) |
| `tiles-defs.xml` | Thymeleaf Layout Dialect (eliminated) |
| `web.xml` | Spring Boot AutoConfiguration (eliminated) |
| `messages.properties` | `src/main/resources/messages.properties` (continued) |

### 12.2 application.properties Profile Design

```
src/main/resources/
├── application.properties          # Common settings (safe default values only)
├── application-dev.properties      # Development environment (local PostgreSQL / DEBUG logging)
├── application-test.properties     # Test environment (H2 in-memory / DDL=create-drop)
├── application-staging.properties  # Staging environment (PostgreSQL / INFO logging)
└── application-prod.properties     # Production environment (environment variable references / WARN logging)
```

**Sensitive information is referenced via environment variables** (`${DB_PASSWORD}`, etc.). Direct specification in configuration files is prohibited.

### 12.3 Flyway Schema Management

DB schema change history is managed with Flyway.

```
src/main/resources/db/migration/
├── V1__initial_schema.sql      # Transcribed from the current schema.sql
├── V2__add_password_prefix.sql # Add password hash prefix (see §11.2)
└── R__seed_data.sql            # Test/development seed data (Repeatable)
```

```properties
# application.properties (Flyway configuration)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true  # Baseline application for existing DB
```

### 12.4 Logback Configuration

`X-Request-Id` tracing is embedded in the Logback pattern.

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

> **Dependency**: Add `net.logstash.logback:logstash-logback-encoder` (e.g., `8.0`) to `pom.xml` for production JSON logging (`LogstashEncoder`). An explicit version specification is required as it is outside the Spring Boot BOM.

> **Important**: Do not output PII (Personally Identifiable Information) such as email addresses, physical addresses, or credit card numbers in logs. `SecurityLog` should only record IP addresses and event types. Never log password values.

---

## 13. Test Strategy

### 13.1 Test Types and Technologies

| Test Type | Technology | Target |
|-----------|-----------|--------|
| Unit Test | JUnit 5 + Mockito | Service, Util |
| Slice Test (Web) | `@WebMvcTest` | Controller |
| Slice Test (DB) | `@DataJpaTest` + H2 | Repository |
| Integration Test | `@SpringBootTest` | All layers |
| Security Test | Spring Security Test | Authentication/Authorization |

### 13.2 Test Coverage Targets

| Layer | Target Coverage |
|-------|----------------|
| Service | 80% or higher |
| Controller | 70% or higher (`@WebMvcTest`) |
| Repository | 70% or higher (`@DataJpaTest`) |
| Overall | 70% or higher |

### 13.3 Pre/Post-Migration Behavioral Equivalence Verification

For each feature, verify the following before and after migration:
1. Normal scenarios (input values → expected page transitions / DB changes)
2. Error scenarios (validation errors, business errors)
3. Authorization control (role-based access control)
4. Session management (login/logout)

---

## 14. Non-Functional Requirements

### 14.1 Performance

| Item | Target Value |
|------|-------------|
| Product list API response | Within 300ms (P95) |
| Order confirmation processing | Within 1000ms (P95) |
| Connection pool | HikariCP max 20 connections |
| JVM heap | 512MB (startup) / 1GB (maximum) |

### 14.2 Observability

| Item | Implementation |
|------|---------------|
| Health check | `/actuator/health` |
| Metrics | `/actuator/prometheus` (Micrometer) |
| Request tracing | `X-Request-Id` header (continue the existing RequestIdFilter as `OncePerRequestFilter`) |
| Access logs | Logback access logs / Tomcat access logs |

### 14.3 Security Non-Functional Requirements

| Requirement | Implementation |
|------------|---------------|
| HTTPS enforcement | Reverse Proxy (Nginx/ALB) or HSTS |
| XSS prevention | Thymeleaf default escaping |
| CSRF protection | Spring Security CSRF filter |
| Session fixation attack prevention | `sessionFixation().migrateSession()` |
| Brute-force prevention | Continue existing 5-failure account lock |
| Content Security Policy | Header configuration in `SecurityConfig` |

---

## 15. Known Issues and Limitations

### 15.1 Password Compatibility

Existing user password hashes use the SHA-256 format. Migration to BCrypt has the following challenges:
- Existing hashes cannot be converted to BCrypt (one-way hash)
- **Countermeasure**: Upgrade on login via `DelegatingPasswordEncoder`, or require all users to reset their passwords

### 15.2 Session Attribute Migration

The current system stores the `User` POJO in the session via `session.setAttribute("loginUser", User)`. In Spring Security, this is managed as `UserDetails`, so a design to convert the `User` entity to a `UserDetails` implementation is required.

### 15.3 File Upload

If `commons-fileupload` is used, migrate to Spring Boot's `MultipartFile`.

### 15.4 Asynchronous Email Sending

The current `MailService` sends emails synchronously. Asynchronous processing via `@Async` or Spring Batch/Scheduler is recommended, but this migration prioritizes functional equivalence with synchronous sending.

### 15.5 Impact of URL Changes

The change from `*.do` to RESTful URLs may affect email links, bookmarks, etc. Consider URL redirects via Nginx or Spring to address this.
