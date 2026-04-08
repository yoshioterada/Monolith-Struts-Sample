# SkiShop Migration Plan
## Java 5 / Struts 1.3 → Java 21 / Spring Boot 3.2.x

**Document Version**: 1.0  
**Created**: 2026-03-31  
**Target Directory**: `appmod-migrated-java21-spring-boot-3rd/`  
**Related Design Document**: [DESIGN.md](./DESIGN.md)

---

## Table of Contents

1. [Migration Phase Overview](#1-migration-phase-overview)
2. [Phase 0: Pre-Migration Setup](#2-phase-0-pre-migration-setup)
3. [Phase 1: Project Foundation Setup](#3-phase-1-project-foundation-setup)
4. [Phase 2: Domain Model Migration](#4-phase-2-domain-model-migration)
5. [Phase 3: Repository Layer Migration](#5-phase-3-repository-layer-migration)
6. [Phase 4: Service Layer Migration](#6-phase-4-service-layer-migration)
7. [Phase 5: Web Layer Migration (Controller + DTO)](#7-phase-5-web-layer-migration-controller--dto)
8. [Phase 6: View Layer Migration (Thymeleaf)](#8-phase-6-view-layer-migration-thymeleaf)
9. [Phase 7: Security Integration](#9-phase-7-security-integration)
10. [Phase 8: Test Implementation & Quality Assurance](#10-phase-8-test-implementation--quality-assurance)
11. [Phase 9: Final Verification & Release Preparation](#11-phase-9-final-verification--release-preparation)
12. [Risks and Mitigations](#12-risks-and-mitigations)
13. [Quality Checklist](#13-quality-checklist)
14. [Completion Acceptance Criteria](#14-completion-acceptance-criteria)
15. [Rollback Plan](#15-rollback-plan)

---

## 1. Migration Phase Overview

```
Phase 0: Pre-Migration Setup
  └─ Environment verification & baseline establishment

Phase 1: Project Foundation Setup
  └─ Spring Boot project creation, pom.xml, configuration files
  
Phase 2: Domain Model Migration
  └─ POJO → JPA @Entity conversion (all 22 entities)
  
Phase 3: Repository Layer Migration
  └─ JDBC DAO → Spring Data JPA Repository (all 20 repositories)
  
Phase 4: Service Layer Migration
  └─ Service class DI conversion & @Transactional annotation (all 13 services)
  
Phase 5: Web Layer Migration (Controller + DTO)
  └─ Struts Action → Spring MVC Controller (all 29 Actions)
  └─ ActionForm → Bean Validation DTO

Phase 6: View Layer Migration (Thymeleaf)
  └─ JSP + Tiles → Thymeleaf templates (all 30+ screens)
  
Phase 7: Security Integration
  └─ Spring Security configuration, authentication/authorization, password migration
  
Phase 8: Test Implementation & Quality Assurance
  └─ JUnit 5 tests, coverage verification, E2E tests
  
Phase 9: Final Verification & Release Preparation
  └─ Production-like testing, Docker build, documentation finalization
```

**Phase completion gate**: Compilation succeeds + all phase checklist items ✅

---

## 2. Phase 0: Pre-Migration Setup

### Objective
Establish the baseline for the migration and ensure all team members share a common understanding.

### Work Items

| # | Task | Owner | Verification Method |
|---|------|-------|---------------------|
| 0-1 | Verify all functions of the current application (baseline) | Migration lead | Manual testing & screenshots |
| 0-2 | Run current tests & record pass rates | Migration lead | Record `mvn test` results |
| 0-3 | Confirm JDK 21 installation | Migration lead | `java -version` |
| 0-4 | Confirm Maven 3.9.x | Migration lead | `mvn -version` |
| 0-5 | Confirm PostgreSQL connectivity (local or Docker) | Migration lead | `psql -U skishop -d skishop` |
| 0-6 | Create Git branch | Migration lead | `git checkout -b migration/spring-boot-v3` |
| 0-7 | Complete design document (DESIGN.md) review | tech-lead/architect | All sections reviewed |
| 0-8 | Complete migration plan (PLAN.md) review | tech-lead/architect | All phases reviewed |

### Completion Criteria
- [ ] Baseline functional verification of current application completed
- [ ] JDK 21 + Maven 3.9 installed in development environment
- [ ] Design document & migration plan reviews completed

---

## 3. Phase 1: Project Foundation Setup

### Objective
Create a Spring Boot project in the `appmod-migrated-java21-spring-boot-3rd/` directory and establish a compilable state.

### Work Items

#### 1-1: Create `pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>  <!-- Use the latest 3.2.x release -->
</parent>

<dependencies>
    spring-boot-starter-web
    spring-boot-starter-thymeleaf
    thymeleaf-extras-springsecurity6        <!-- Required for using ${#authentication.*} in Thymeleaf -->
    thymeleaf-layout-dialect               <!-- Required for layout:decorate to replace Tiles -->
    spring-boot-starter-data-jpa
    spring-boot-starter-security
    spring-boot-starter-validation
    spring-boot-starter-mail
    spring-boot-starter-actuator
    flyway-core                            <!-- DB schema version management -->
    flyway-database-postgresql             <!-- Flyway PostgreSQL dialect -->
    micrometer-registry-prometheus
    springdoc-openapi-starter-webmvc-ui (2.3.x)
    lombok (provided/optional)              <!-- Used for @RequiredArgsConstructor etc. Build-time only: add to annotationProcessorPaths -->
    logstash-logback-encoder (net.logstash.logback, 8.x)  <!-- For production JSON logging. Version must be explicitly specified as it is outside the Spring BOM -->
    spring-security-test (test)
    postgresql (runtime)
    h2 (test)
    spring-boot-starter-test (test)
</dependencies>
```

#### 1-2: Create Main Class

```java
@SpringBootApplication
public class SkiShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkiShopApplication.class, args);
    }
}
```

#### 1-3: Create Configuration Files

`application.properties` (common):
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
spring.flyway.enabled=false  # Disable Flyway in tests; use JPA DDL instead
```

#### 1-4: Create Directory Structure

Create package directories according to Design Document §5.1. (Managed with empty `.gitkeep` files.)

#### 1-5: Copy messages.properties

Copy the existing `src/main/resources/messages.properties` as-is (string resources can be reused).

#### 1-6: DB Scripts & Flyway Configuration

Convert the existing `db/schema.sql` and `db/data.sql` into Flyway migration format.

```
src/main/resources/db/migration/
├── V1__initial_schema.sql      # Transcribed from current schema.sql
├── V2__add_password_prefix.sql # Add password hash prefix (executed in Phase 7)
└── R__seed_data.sql            # Development seed data (Repeatable)
```

### Verification Commands
```bash
cd appmod-migrated-java21-spring-boot-3rd
mvn clean compile  # Verify compilation succeeds
```

### Completion Criteria
- [ ] `mvn clean compile` succeeds
- [ ] `SkiShopApplication` startup confirmed (minimum configuration that can start without a DB)
- [ ] No sensitive information directly written in `application.properties`

---

## 4. Phase 2: Domain Model Migration

### Objective
Convert existing POJO domain classes to JPA entities.

### Target Entities (22 classes)

| Domain | Current Class | After Migration | Key Conversion Details |
|--------|--------------|-----------------|----------------------|
| user | `User.java` | `User.java` | `@Entity`, `java.util.Date` → `LocalDateTime` |
| user | `SecurityLog.java` | `SecurityLog.java` | `@Entity` |
| user | `PasswordResetToken.java` | `PasswordResetToken.java` | `@Entity`, expiration management |
| address | `Address.java` | `Address.java` | `@Entity`, `@ManyToOne(User)` |
| cart | `Cart.java` | `Cart.java` | `@Entity`, `@OneToMany(CartItem)` |
| cart | `CartItem.java` | `CartItem.java` | `@Entity`, `@ManyToOne(Cart)` |
| product | `Category.java` | `Category.java` | `@Entity`, self-referencing (parent) |
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

### Conversion Rules (Detailed)

1. Add `@Entity` and `@Table(name = "table_name")` to each class
2. Add `@Id` to the String-type id field (UUIDs are not auto-generated: no `@GeneratedValue`, use `UUID.randomUUID()` in the service layer)
3. Convert `java.util.Date` fields to `java.time.LocalDateTime` / `LocalDate`
4. Explicitly specify snake_case schema column names with `@Column(name = "column_name")`
5. Express NOT NULL constraints with `@Column(nullable = false)`
6. Add `@CreationTimestamp` / `@UpdateTimestamp` to `created_at` / `updated_at`
7. Explicitly set LAZY fetch for relationships (`@OneToMany`, `@ManyToOne`)

### Verification Commands
```bash
mvn clean compile -pl appmod-migrated-java21-spring-boot-3rd
```

### Completion Criteria
- [ ] All 22 entities compile successfully
- [ ] Schema validation passes with `@DataJpaTest` (DDL generation confirmed on H2)
- [ ] `java.util.Date` is not used anywhere
- [ ] Column names are explicitly specified with `@Column` (all fields)

---

## 5. Phase 3: Repository Layer Migration

### Objective
Replace JDBC DAOs with Spring Data JPA Repositories.

### Work Items

| # | Repository | Base | Additional Methods |
|---|-----------|------|--------------------|
| 3-1 | `UserRepository` | `JpaRepository<User, String>` | `findByEmail`, `findByStatus` |
| 3-2 | `SecurityLogRepository` | `JpaRepository<SecurityLog, String>` | `countByUserIdAndEventType` |
| 3-3 | `PasswordResetTokenRepository` | `JpaRepository<PasswordResetToken, String>` | `findByToken`, `deleteByToken` |
| 3-4 | `AddressRepository` | `JpaRepository<Address, String>` | `findByUserId` |
| 3-5 | `CartRepository` | `JpaRepository<Cart, String>` | `findByUserIdAndStatus`, `findBySessionId` |
| 3-6 | `CartItemRepository` | `JpaRepository<CartItem, String>` | `findByCartId`, `deleteByCartId` |
| 3-7 | `CategoryRepository` | `JpaRepository<Category, String>` | `findByParentIdIsNull`, `findByParentId` |
| 3-8 | `ProductRepository` | `JpaRepository<Product, String>` | `findByCategoryId`, `findByStatus` + `JpaSpecificationExecutor` for dynamic search |
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

### SQL Injection Prevention

- All query parameters use Spring Data bind parameters (`@Param`) or JPA derived query methods
- Dynamic SQL via string concatenation is **strictly prohibited**
- Dynamic conditions are implemented using `Specification<T>`

### Verification Commands
```bash
mvn test -Dtest="*RepositoryTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### Completion Criteria
- [ ] All 20 repositories compile successfully
- [ ] CRUD operation tests pass with `@DataJpaTest`
- [ ] No string-concatenated SQL exists (verified with grep)

---

## 6. Phase 4: Service Layer Migration

### Objective
Convert service classes to Spring Beans and ensure consistency with `@Transactional`.

### Work Items

| # | Service Class | Migration Notes |
|---|--------------|-----------------|
| 4-1 | `AuthService` | DI for `UserRepository` + `SecurityLogRepository`. BCrypt support |
| 4-2 | `UserService` | `UserRepository` + `PasswordResetTokenRepository`. Email integration |
| 4-3 | `ProductService` | `ProductRepository` + `PriceRepository` + `CategoryRepository`. Dynamic search via Specification |
| 4-4 | `CartService` | `CartRepository` + `CartItemRepository`. Session integration |
| 4-5 | `InventoryService` | `InventoryRepository`. Stock deduction protected by `@Transactional` |
| 4-6 | `CouponService` | `CouponRepository` + `CouponUsageRepository`. Port validation logic |
| 4-7 | `OrderService` | `OrderRepository` + `OrderItemRepository`. Atomize order confirmation with `@Transactional` |
| 4-8 | `PaymentService` | `PaymentRepository`. Status management |
| 4-9 | `ShippingService` | `ShippingMethodRepository` |
| 4-10 | `TaxService` | Tax rate configuration via `@ConfigurationProperties` |
| 4-11 | `PointService` | `PointAccountRepository` + `PointTransactionRepository` |
| 4-12 | `MailService` | `JavaMailSender` (Spring Boot Mail). `EmailQueueRepository` |
| 4-13 | `AddressService` | `AddressRepository` |

### Important Notes

1. **Constructor injection is mandatory**: `@Autowired` field injection is prohibited
2. **`@Transactional(readOnly = true)`**: Must be applied to all read-only methods
3. **Prohibition of dependency creation via `new`**: Completely eliminate patterns like `private final AuthService authService = new AuthService()`
4. **Exception translation**: Convert DAO exceptions (DataAccessException) to business exceptions (ResourceNotFoundException, etc.)

### Verification Commands
```bash
mvn test -Dtest="*ServiceTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### Completion Criteria
- [ ] No dependency creation via `new` exists (verified with grep)
- [ ] All services have `@Service` annotation
- [ ] All methods that update the DB have `@Transactional`
- [ ] Read-only methods have `@Transactional(readOnly = true)`
- [ ] Service unit tests (using Mockito) all pass

---

## 7. Phase 5: Web Layer Migration (Controller + DTO)

### Objective
Convert Struts Actions to Spring MVC Controllers and replace ActionForms with Bean Validation DTOs.

### 5-A: Create DTOs (First)

Convert ActionForms to Bean Validation DTO record classes (create DTOs first, then use them in Controllers).

| Current ActionForm | Migrated DTO |
|-------------------|-------------|
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

**Conversion Rules**:
- ActionForm `validate()` method logic → Bean Validation annotations
- String fields get `@NotBlank` / `@Size`
- Email fields get `@Email`
- Numeric fields get `@Min` / `@Max` / `@Positive`

### 5-B: Create Controllers

Implement Controllers according to the conversion specification in Design Document §6.

#### Implementation Checklist for Each Controller

**AuthController**:
- [ ] GET /auth/login → `auth/login` template
- [ ] POST /auth/login → Authentication → `redirect:/` or `auth/login` (error)
- [ ] GET /auth/register → `auth/register` template
- [ ] POST /auth/register → User registration → `redirect:/auth/login`
- [ ] GET /auth/password/forgot → Password recovery screen
- [ ] POST /auth/password/forgot → Send email
- [ ] GET /auth/password/reset → Reset screen
- [ ] POST /auth/password/reset → Change password
- [ ] POST /auth/logout → Logout → `redirect:/`

**ProductController**:
- [ ] GET /products → Product list (search & pagination)
- [ ] GET /products/{id} → Product detail

**CartController**:
- [ ] GET /cart → Display cart
- [ ] POST /cart/items → Add product to cart
- [ ] PUT /cart/items/{id} → Update quantity
- [ ] DELETE /cart/items/{id} → Remove product

**CheckoutController**:
- [ ] GET /checkout → Checkout screen (authentication required)
- [ ] POST /checkout → Confirm order (authentication required)

**CouponController**:
- [ ] POST /cart/coupon → Apply coupon
- [ ] GET /coupons → List available coupons

**OrderController**:
- [ ] GET /orders → Order history (authentication required)
- [ ] GET /orders/{id} → Order detail (authentication required)
- [ ] POST /orders/{id}/cancel → Cancel order (authentication required)
- [ ] POST /orders/{orderId}/return → Return request (authentication required)

**PointController**:
- [ ] GET /account/points → Point balance (authentication required)

**AddressController**:
- [ ] GET /account/addresses → Address list (authentication required)
- [ ] POST /account/addresses → Add/update address (authentication required)

**AdminProductController**:
- [ ] GET /admin/products → Admin product list (ADMIN only)
- [ ] GET /admin/products/{id}/edit → Product edit screen
- [ ] PUT /admin/products/{id} → Update product
- [ ] POST /admin/products → Create new product
- [ ] DELETE /admin/products/{id} → Delete product

**AdminOrderController**:
- [ ] GET /admin/orders → Admin order list
- [ ] GET /admin/orders/{id} → Order detail
- [ ] PUT /admin/orders/{id}/status → Update order status
- [ ] POST /admin/orders/{id}/refund → Process refund

**AdminCouponController**:
- [ ] GET /admin/coupons → Coupon list
- [ ] PUT /admin/coupons/{id} → Update coupon
- [ ] POST /admin/coupons → Create new coupon

**AdminShippingController**:
- [ ] GET /admin/shipping → Shipping method list
- [ ] PUT /admin/shipping/{id} → Update shipping method

### Verification Commands
```bash
mvn test -Dtest="*ControllerTest" -pl appmod-migrated-java21-spring-boot-3rd
```

### Completion Criteria
- [ ] All Controllers compile successfully
- [ ] `@Valid` is applied to all request DTO parameters
- [ ] `*.do` URL patterns are eliminated
- [ ] Controller HTTP request/response tests pass with `@WebMvcTest`
- [ ] Flash messages (`RedirectAttributes`) are correctly implemented

---

## 8. Phase 6: View Layer Migration (Thymeleaf)

### Objective
Convert JSP + Tiles to Thymeleaf templates.

### 8-A: Create Layout (First)

1. Create `templates/fragments/layout.html` (equivalent to Tiles `baseLayout`)
2. Create `templates/fragments/header.html`
3. Create `templates/fragments/footer.html`
4. Create `templates/fragments/messages.html` (equivalent to Struts `ActionMessages`)
5. Create error pages under `templates/error/` (400, 403, 404, 500)

### 8-B: Convert Screen Templates

Convert according to the mapping in Design Document §10.3.

| Priority | Template | Current JSP |
|----------|----------|-------------|
| High | `templates/auth/login.html` | `auth/login.jsp` |
| High | `templates/auth/register.html` | `auth/register.jsp` |
| High | `templates/products/list.html` | `products/list.jsp` |
| High | `templates/products/detail.html` | `products/detail.jsp` |
| High | `templates/cart/view.html` | `cart/view.jsp` |
| High | `templates/checkout/index.html` | `cart/checkout.jsp` |
| Medium | `templates/orders/list.html` | `orders/history.jsp` |
| Medium | `templates/orders/detail.html` | `orders/detail.jsp` |
| Medium | `templates/home.html` | `home.jsp` |
| Medium | `templates/account/addresses.html` | `account/addresses.jsp` |
| Medium | `templates/account/points.html` | `points/balance.jsp` |
| Low | `templates/admin/**` | `admin/**/*.jsp` |
| Low | `templates/coupons/available.html` | `coupons/available.jsp` |

### 8-C: Static Resource Migration

| Current Path | Migrated Path |
|-------------|---------------|
| `src/main/webapp/assets/` | `src/main/resources/static/` |

Verify Content-Type headers and place CSS/JS/image files appropriately.

### Thymeleaf Conversion Verification Items

- [ ] URLs are dynamically generated with `th:action="@{/...}"` (hardcoded URLs prohibited)
- [ ] Default XSS escaping is enabled with `th:text="${...}"`
- [ ] Path parameters are safely expanded with `th:href="@{/products/{id}(id=${p.id})}"`
- [ ] Iteration is handled with `th:each`
- [ ] Form validation errors are displayed with `th:errors`
- [ ] Thymeleaf Security Integration is used with `th:with` (`${#authentication.name}`)
- [ ] Verify CSRF tokens are auto-injected into `<form>` elements
- [ ] Strings are retrieved from messages.properties with `#{...}`

### Completion Criteria
- [ ] All templates compile (Thymeleaf parsing) successfully
- [ ] Integration tests confirm HTTP 200 responses for all screens
- [ ] XSS test: User input values are properly escaped
- [ ] CSRF test: POST forms contain tokens
- [ ] Static resources (CSS/JS) are correctly served

---

## 9. Phase 7: Security Integration

### Objective
Fully integrate Spring Security to establish authentication, authorization, session management, and CSRF protection.

### Work Items

#### 7-1: Implement SecurityConfig

Implement the configuration from Design Document §11.1.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Implement the settings described in the design document
}
```

#### 7-2: Implement UserDetailsService

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
            .password(user.getPasswordHash())  // DelegatingPasswordEncoder format
            .roles(user.getRole())
            .accountLocked("LOCKED".equals(user.getStatus()))
            .build();
    }
}
```

#### 7-3: Configure Password Encoder

Implement the `DelegatingPasswordEncoder` configuration from Design Document §11.2.

The following 3 items are implemented in this step:

**① Create `LegacySha256PasswordEncoder` class** (`util/LegacySha256PasswordEncoder.java`):

Implement exactly as shown in the Design Document §11.2 code example. Implement a `matches()` method that compares against `PasswordHasher.hash(password, salt)`, and throw `UnsupportedOperationException` from `encode()`.

**② Implement `UserDetailsPasswordService` in `CustomUserDetailsService`**:

Change to `implements UserDetailsService, UserDetailsPasswordService` and implement the `updatePassword()` method. Spring Security will automatically call this after a BCrypt upgrade.

**③ Add prefix + salt embedding to existing DB data (Execute Flyway V2)**:
```sql
-- Flyway: V2__add_password_prefix.sql
-- Convert hash and salt to {sha256}<hash>$<salt> format
UPDATE users SET password_hash = CONCAT('{sha256}', password_hash, '$', salt)
WHERE password_hash NOT LIKE '{%}%';
```

#### 7-4: Configure Security Headers

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.enable())
);
```

#### 7-5: Port RequestId Filter

Implement the existing `RequestIdFilter` as a `OncePerRequestFilter`.

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

#### 7-6: Implement `@EnableMethodSecurity` + `@PreAuthorize`

Add `@EnableMethodSecurity` to `SecurityConfig` and apply `@PreAuthorize("hasRole('ADMIN')")` to admin-only Service methods. This achieves defense-in-depth with both URL-based authorization and method-based authorization.

```java
// Example: AdminProductService
@Service
public class AdminProductService {
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product updateProduct(String id, AdminProductRequest request) { ... }
}
```

### Completion Criteria
- [ ] Unauthenticated users accessing authentication-required URLs are redirected to the login page
- [ ] USER role accessing ADMIN-only URLs receives a 403 response
- [ ] POST forms contain CSRF tokens and invalid tokens return 403
- [ ] Session fixation attack test: Session ID changes before and after login
- [ ] Brute force test: Account is locked after 5 failed attempts
- [ ] Post-password-migration authentication test (SHA-256 users can log in with `{sha256}` prefix)
- [ ] Password test: Automatic upgrade to BCrypt after successful login is confirmed
- [ ] Authentication/authorization unit tests pass with Spring Security Test

---

## 10. Phase 8: Test Implementation & Quality Assurance

### Objective
Implement tests for each layer and achieve coverage targets.

### Work Items

#### 8-1: Test Infrastructure Setup

```java
// Common test configuration
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationTest {
    // Common setup
}
```

#### 8-2: Service Tests (JUnit 5 + Mockito)

Unit tests for critical services:
- [ ] `AuthServiceTest`: Authentication success, failure, account lock
- [ ] `CartServiceTest`: Cart CRUD operations
- [ ] `CheckoutServiceTest`: Order confirmation transaction (including rollback verification for all 11 steps)
- [ ] `OrderServiceTest`: Order confirmation transaction
- [ ] `CouponServiceTest`: Coupon validation logic
- [ ] `PointServiceTest`: Point accrual & redemption
- [ ] `LegacySha256PasswordEncoderTest`: Verify `matches()` success/failure with known SHA-256 hash + salt

#### 8-3: Repository Tests (@DataJpaTest)

- [ ] `UserRepositoryTest`: findByEmail, countByStatus
- [ ] `ProductRepositoryTest`: Dynamic search (Specification)
- [ ] `OrderRepositoryTest`: Order history retrieval

#### 8-4: Controller Tests (@WebMvcTest)

- [ ] `AuthControllerTest`: Login/logout/registration/validation
- [ ] `ProductControllerTest`: Product list & detail
- [ ] `CartControllerTest`: Cart operations (including endpoints requiring authentication)
- [ ] `CheckoutControllerTest`: Checkout (authentication required)

#### 8-5: Integration Tests (@SpringBootTest)

E2E tests for key scenarios:
- [ ] New user registration → Login → Product selection → Add to cart → Confirm order
- [ ] Anonymous cart → Login → Cart merge (verify session cart is merged into user cart)
- [ ] Admin login → Product management → Coupon management

#### 8-6: Security Tests

- [ ] Verify unauthenticated access redirects
- [ ] Verify CSRF protection behavior
- [ ] Verify XSS payloads are escaped

#### 8-7: Coverage Measurement

```bash
mvn clean verify -Djacoco.skip=false -pl appmod-migrated-java21-spring-boot-3rd
```

Targets:
- Service: 80% or higher
- Controller: 70% or higher
- Overall: 70% or higher

### Completion Criteria
- [ ] `mvn clean test` passes 100%
- [ ] Coverage targets met (overall 70% or higher)
- [ ] All security tests pass

---

## 11. Phase 9: Final Verification & Release Preparation

### Objective
Verify behavior in a production-like environment and prepare deliverables needed for release.

### Work Items

#### 9-1: Docker Environment Verification

```bash
docker compose up -d  # Start PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Xmx512m"
```

#### 9-2: DB Integration Tests

Integration tests with Spring Boot Test + PostgreSQL (TestContainers or local DB).

#### 9-3: Functional Checklist Verification

Execute scenario tests against all Actions from DESIGN.md §2.3 (see [Quality Checklist](#13-quality-checklist)).

#### 9-4: Performance Tests

- Product list API: Load test with 10 concurrent users × 30 seconds
- Order confirmation API: Load test with 5 concurrent users × 10 seconds
- Target: P95 response time within DESIGN.md §14.1 baseline values

#### 9-5: Security Scan

OWASP Dependency Check:
```bash
mvn org.owasp:dependency-check-maven:check -pl appmod-migrated-java21-spring-boot-3rd
```

#### 9-6: Dockerfile Verification

```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Run as non-root user (mandatory container security measure)
RUN addgroup -S skishop && adduser -S skishop -G skishop

WORKDIR /app
COPY target/skishop-app-*.jar app.jar
RUN chown skishop:skishop app.jar
USER skishop

# Java 21 container-aware JVM flags (percentage-based preferred over hardcoded -Xmx)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
```

#### 9-7: Update README

Include the following in the migrated project's `README.md`:
- Startup instructions (including environment variable setup)
- Test execution instructions
- Docker Compose startup instructions
- Environment variable list (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, etc.)

### Completion Criteria
- [ ] `mvn clean verify` succeeds (all tests pass, coverage threshold met)
- [ ] Docker image builds and starts successfully
- [ ] Startup succeeds with production profile (`spring.profiles.active=prod` + environment variables)
- [ ] No Critical CVEs from OWASP Dependency Check
- [ ] README contains necessary startup information

---

## 12. Risks and Mitigations

| Risk ID | Risk | Impact | Likelihood | Mitigation |
|---------|------|--------|------------|------------|
| R-01 | Password hash incompatibility (SHA-256 → BCrypt) | High | Certain | Gradual migration using `DelegatingPasswordEncoder` |
| R-02 | Session attribute design change (`loginUser` → `UserDetails`) | Medium | High | Standardize all templates to use `#authentication.principal` |
| R-03 | Broken links due to URL changes (`*.do` → RESTful) | Medium | High | H2 DB + Nginx redirects / Spring RedirectView to forward legacy URLs |
| R-04 | JPA N+1 problem (excessive LAZY fetch triggering) | Medium | Medium | Address with `@EntityGraph` / `JOIN FETCH`. Detect via slow query logging |
| R-05 | Thymeleaf + CSRF: CSRF errors on non-HTML form submissions | Medium | Medium | Implement JS code to add `X-CSRF-TOKEN` header to AJAX POST requests |
| R-06 | Email configuration change (javax.mail → jakarta.mail) | Low | Certain | Transparently handled by Spring Boot auto-configuration |
| R-07 | Behavioral differences between `commons-fileupload` → Spring Multipart | Low | Low | Convert to Spring `MultipartFile` if file upload functionality exists |
| R-08 | Test failures due to H2 vs PostgreSQL dialect differences | Medium | Medium | Set `MODE=PostgreSQL` in `@DataJpaTest`. Verify critical SQL against PostgreSQL as well |
| R-09 | Log4j MDC → SLF4J MDC API differences | Low | Low | API names are nearly identical (`MDC.put()` / `MDC.remove()`). Reference `%X{reqId}` in Logback config |
| R-10 | Missing functionality after migration | High | Medium | Execute Quality Checklist §13 against all Actions |

---

## 13. Quality Checklist

### 13-A: Code Quality Checks

#### Security (Critical)
- [ ] No passwords/API keys directly written in configuration files
- [ ] `@Valid` is applied to all Controller input DTOs
- [ ] No SQL string concatenation exists (verify with `grep -r "\"SELECT.*+\|\"UPDATE.*+"`)
- [ ] XSS: Thymeleaf uses `th:text` (`th:utext` only when absolutely necessary)
- [ ] CSRF: All POST forms contain tokens
- [ ] Spring Security authorization covers all URLs

#### Architecture (High)
- [ ] No direct Controller → Repository references (Controllers only call Services)
- [ ] No Service/Repository creation via `new`
- [ ] No circular dependencies (verify with `mvn dependency:tree`)
- [ ] `@Transactional` is only applied at the Service layer (prohibited on Controllers)

#### Coding Standards (Medium)
- [ ] Naming conventions: PascalCase (classes), camelCase (methods/variables), UPPER_SNAKE_CASE (constants)
- [ ] `java.util.Date` is not used (`java.time.*` is used instead)
- [ ] `@SuppressWarnings("unchecked")` is not used inappropriately
- [ ] Except for `public static void main(void)` patterns, Singleton patterns are replaced with Spring DI

### 13-B: Functional Equivalence Checks (All Actions)

#### Authentication
- [ ] Login happy path (email/password match, redirect to dashboard)
- [ ] Login failure (password mismatch, error message displayed)
- [ ] Account lock (login disabled after 5 failures)
- [ ] Logout (session invalidated, redirect to home)
- [ ] User registration (new user created, DB record confirmed)
- [ ] Duplicate email registration (error message displayed)
- [ ] Password recovery (email sending confirmed)
- [ ] Password reset (valid token, password change confirmed)

#### Products
- [ ] Product list (all products displayed, category filter, keyword search)
- [ ] Product detail (product info, price, stock displayed)
- [ ] Non-existent product ID (404 or appropriate error screen)

#### Cart
- [ ] Add to cart (quantity input, DB record confirmed)
- [ ] Cart display (total amount calculation accuracy)
- [ ] Cart quantity update
- [ ] Cart item removal
- [ ] Coupon application (valid/invalid/used coupons)
- [ ] Checkout screen (delivery address & payment method selection)
- [ ] Order confirmation (order record created in DB, stock deduction confirmed)

#### Orders
- [ ] Order history (order list for logged-in user)
- [ ] Order detail (order items & status displayed)
- [ ] Order cancellation (cancellation eligibility conditions verified)
- [ ] Return request

#### Account
- [ ] Point balance display
- [ ] Address list display
- [ ] Address add/update

#### Admin
- [ ] Admin product list, edit, delete
- [ ] Admin order list, detail, status update, refund
- [ ] Admin coupon list, edit
- [ ] Admin shipping method list, edit
- [ ] Admin user list (include lock/unlock operations if supported)

#### Authorization Control
- [ ] Unauthenticated user accessing `/orders` → Redirect to login page
- [ ] USER role accessing `/admin/**` → 403
- [ ] ADMIN role can access all URLs
- [ ] Method-level authorization via `@PreAuthorize` remains effective after URL changes
- [ ] Actuator endpoints (except `/health`) are inaccessible without authentication

### 13-C: Non-Functional Checks

- [ ] All `mvn test` pass on H2 (test environment)
- [ ] Normal operation confirmed on PostgreSQL (development environment)
- [ ] Coverage: Service 80%+, Controller 70%+, Overall 70%+
- [ ] No Critical CVEs from OWASP Dependency Check
- [ ] `%X{reqId}` is output in logs via Logback
- [ ] `/actuator/health` returns `UP`
- [ ] `/actuator/**` (except health) returns 401/403 for unauthenticated access
- [ ] Docker image builds successfully
- [ ] Flyway migrations apply successfully (`flyway_schema_history` confirmed)
- [ ] PII (email addresses, addresses, passwords) is not output in logs

---

## 14. Completion Acceptance Criteria

The migration is considered complete when **all** of the following conditions are met.

| ID | Acceptance Criterion | Verification Method |
|----|---------------------|---------------------|
| C-1 | `mvn clean test` passes 100% | CI console verification |
| C-2 | Overall test coverage 70% or higher | JaCoCo report |
| C-3 | All scenarios in §13-B verified via manual testing | Checklist completion |
| C-4 | No Critical CVEs from OWASP Dependency Check | Report verification |
| C-5 | Docker image builds and starts successfully | `docker run` verification |
| C-6 | Spring Security authentication/authorization applied to all URLs | Security tests pass |
| C-7 | No sensitive information in configuration files | Code review verification |
| C-8 | No Service/Repository creation via `new` | `grep -r "= new"` verification |
| C-9 | Architect review completed (design principles adherence) | Review comments resolved |
| C-10 | QA manager scenario testing completed | Quality report |

---

## 15. Rollback Plan

### Rollback Trigger Conditions

Consider halting the migration and rolling back if any of the following occur:
- Compilation errors remain unresolved after completing Phases 1–7
- A critical security issue is discovered (e.g., authentication bypass)
- DB schema incompatibility causes existing data corruption

### Rollback Procedure

1. **No impact on the original project since it's a new directory**: `appmod-migrated-java21-spring-boot-3rd/` is created in a separate directory, so there is no impact on the original `skishop-monolith`
2. **Delete the migration target directory**: `rm -rf appmod-migrated-java21-spring-boot-3rd/`
3. **Discard the Git branch**: `git branch -D migration/spring-boot-v3`
4. **Analyze root cause, update the design document**, then reattempt

### DB Rollback

This migration minimizes schema changes, but if the `DelegatingPasswordEncoder` prefix addition SQL has been executed:
```sql
-- Rollback SQL (remove password hash prefix and embedded salt, restoring hash only)
UPDATE users
SET password_hash = split_part(substring(password_hash from length('{sha256}') + 1), '$', 1)
WHERE password_hash LIKE '{sha256}%';
```

---

## Appendix: Work Tracking Sheet

### Phase Progress Tracker

| Phase | Start Date | End Date | Status | Owner |
|-------|-----------|----------|--------|-------|
| Phase 0: Pre-Migration Setup | | | ⬜️ Not Started | |
| Phase 1: Project Foundation Setup | | | ⬜️ Not Started | |
| Phase 2: Domain Model Migration | | | ⬜️ Not Started | |
| Phase 3: Repository Layer Migration | | | ⬜️ Not Started | |
| Phase 4: Service Layer Migration | | | ⬜️ Not Started | |
| Phase 5: Web Layer Migration | | | ⬜️ Not Started | |
| Phase 6: View Layer Migration | | | ⬜️ Not Started | |
| Phase 7: Security Integration | | | ⬜️ Not Started | |
| Phase 8: Test Implementation | | | ⬜️ Not Started | |
| Phase 9: Final Verification | | | ⬜️ Not Started | |

**Status Legend**: ⬜️ Not Started | 🔄 In Progress | ✅ Completed | ❌ Blocked
