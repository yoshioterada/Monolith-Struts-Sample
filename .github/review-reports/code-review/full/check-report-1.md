# Source Code Review — Integrated Report

## Verdict

- **Target**: `appmod-migrated-java21-spring-boot-3rd/` (full application — 102 Java source files, 35 test files, 12 Flyway migrations, 5 property files, pom.xml, Dockerfile, docker-compose.yml)
- **Verdict**: ❌ **Rejected** — Critical deficiencies detected (11 Critical findings from 4 agents)
- **Review Date**: 2025-07-11
- **Project**: SkiShop (Java 21 / Spring Boot 3.2.12 Monolith EC Site)

> 🚨 **CRITICAL FINDING DETECTED** — 11 Critical findings across Performance (3), Resilience (2), DDD Domain (4), and Java Standards (2) agents. Per verdict matrix, final verdict is automatically **❌ Rejected**. Critical fixes required before merge.

---

## Findings Summary

| Agent | Verdict | Critical | High | Medium | Low | Score |
|-------|---------|----------|------|--------|-----|-------|
| tech-lead | ⚠️ Warning | 0 | 3 | 6 | 4 | 19/20 |
| architecture-reviewer | ⚠️ Warning | 0 | 2 | 5 | 2 | 21/25 |
| ddd-domain-reviewer | ⚠️ Warning | 4 | 9 | 5 | 3 | 9/30 |
| api-endpoint-reviewer | ⚠️ Warning | 0 | 3 | 5 | 2 | 21/25 |
| java-standards-reviewer | ⚠️ Warning | 2 | 3 | 3 | 2 | 21/25 |
| async-concurrency-reviewer | ⚠️ Warning | 0 | 3 | 3 | 2 | 18/25 |
| error-logging-reviewer | ⚠️ Warning | 0 | 5 | 5 | 3 | 20/25 |
| data-access-reviewer | ⚠️ Warning | 0 | 5 | 6 | 3 | 21/25 |
| config-di-reviewer | ⚠️ Warning | 0 | 2 | 3 | 2 | 23/25 |
| security-reviewer | ⚠️ Warning | 0 | 7 | 6 | 3 | 49/65 |
| dependency-reviewer | ⚠️ Warning | 0 | 0 | 2 | 3 | — |
| test-quality-reviewer | ⚠️ Warning | 0 | 6 | 8 | 3 | 19/25 |
| performance-reviewer | ❌ Fail | 3 | 8 | 4 | 1 | 11/25 |
| resilience-reviewer | ❌ Fail | 2 | 7 | 4 | 1 | 11/25 |
| **Total (pre-dedup)** | | **11** | **63** | **65** | **34** | |

After deduplication across agents, the unique finding count is approximately: **Critical: 10, High: 42, Medium: 48, Low: 28**.

---

## Verdict Rationale

- **Rule Applied**: "1 or more Critical findings → ❌ Rejected"
- **Most critical findings**: N+1 queries in checkout hot path (Performance), SMTP timeout absent causing potential indefinite thread blocking (Resilience), no graceful shutdown risking order data corruption during deployments (Resilience)
- **Positive signals**: Zero prohibited pattern violations (tech-lead: all 11 checks clean), zero SQL injection vectors, zero hardcoded secrets, zero Struts remnants, zero `@Autowired` field injection, zero `System.out.println`. Full design document alignment for 29 Struts Action migration, SecurityConfig, DelegatingPasswordEncoder, and CheckoutService 11-step flow.
- **Context note**: DDD domain Critical findings (anemic domain model, child entity repositories) reflect architectural pattern adherence rather than production incident risk. In the context of a Struts 1.3 → Spring Boot migration, these are improvement opportunities rather than blockers. If the project accepts anemic domain model as a deliberate architectural choice, these can be reclassified to High. This reclassification is an escalation item requiring human judgment.

---

## Critical/High Findings List (Fix Required)

### Critical Findings

| # | Severity | Source Agent(s) | Category | Target File | Finding | Suggested Fix |
|---|----------|----------------|----------|-------------|---------|---------------|
| C-1 | **Critical** | performance | N+1 Query | CheckoutService.java L383 | `productService.findById()` called per cart item inside `buildOrderItems()` stream. 10 items → 10 SELECTs during checkout. | Batch-load: `productRepository.findAllById(productIds)` into a `Map<String, Product>` before the stream. |
| C-2 | **Critical** | performance | N+1 Query | InventoryService.java L66 | `findByProductId()` inside `for` loop in `reserveItems()`. Same pattern in `releaseItems()` (L91) and `deductStock()` (L111). Three methods with identical N+1 pattern. | Add `List<Inventory> findByProductIdIn(List<String>)` to repository; batch-load, iterate in-memory. Keep per-row UPDATE for optimistic lock semantics. |
| C-3 | **Critical** | performance, resilience, async-concurrency | Connection Pool / SMTP I/O | MailService.java L127-129 | `processQueue()` is `@Transactional`, wrapping entire batch loop including `javaMailSender.send()`. SMTP network I/O holds a DB connection for full duration. Under high email volume or SMTP delay, this exhausts the HikariCP connection pool, blocking all DB operations application-wide. | Split: (1) `@Transactional(readOnly=true)` to fetch pending batch with LIMIT, (2) iterate outside transaction, (3) per-message `@Transactional` for status update after each SMTP send. |
| C-4 | **Critical** | resilience | SMTP Timeout | application.properties | No SMTP connection/read/write timeout configured in any profile. If SMTP server hangs, `JavaMailSender.send()` blocks the `@Scheduled` thread indefinitely, halting all future mail processing. | Add: `spring.mail.properties.mail.smtp.connectiontimeout=5000`, `spring.mail.properties.mail.smtp.timeout=5000`, `spring.mail.properties.mail.smtp.writetimeout=5000` |
| C-5 | **Critical** | resilience | Graceful Shutdown | application.properties | `server.shutdown=graceful` not configured. During deployment, in-flight checkout requests are abruptly terminated, potentially leaving orders in inconsistent state (payment authorized but order not saved). | Add: `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s` |
| C-6 | **Critical** | ddd-domain | Anemic Domain Model | All 23 entities in model/ | All entities are pure data containers (getters/setters only). Zero business methods on any entity. All business logic resides in services, preventing domain invariant enforcement at the entity level. | **Escalation**: Evaluate if anemic model is an acceptable architectural choice for this migration. If not, add domain methods (e.g., `Order.cancel()`, `Cart.addItem()`, `Inventory.reserve()`). |
| C-7 | **Critical** | ddd-domain | Aggregate Boundary Violation | OrderItemRepository, CartItemRepository, CouponUsageRepository, PointTransactionRepository | Child entities have their own repositories, bypassing aggregate root access control. External code can modify `OrderItem` without going through `Order` aggregate. | **Escalation**: Access child entities only through aggregate root repositories. Use `cascade` operations and `@EntityGraph` for loading children. |
| C-8 | **Critical** | ddd-domain | No Value Objects | All model/ classes | No Value Objects exist. `Money` (amount + currency), `Address` (street + city + state + zip), `Email`, `OrderNumber` are all raw primitives. No type safety for domain concepts. | **Escalation**: Introduce `@Embeddable` Value Objects for key domain concepts. Priority: `Money`, `Address`, `Email`. |
| C-9 | **Critical** | ddd-domain | No Domain Events | Entire codebase | No domain event mechanism (Spring `ApplicationEvent` or custom). State changes are communicated only through service method calls, creating tight coupling between services. | **Escalation**: Introduce Spring `ApplicationEventPublisher` for key domain events: `OrderPlaced`, `PaymentCompleted`, `InventoryReserved`. |
| C-10 | **Critical** | java-standards | TODO in Production Code | AppConstants.java, CheckoutService.java | 2 TODO comments in production code. Per coding standards, TODO comments indicate incomplete implementation. | Resolve or remove: (1) Convert status `String` constants to enums in `AppConstants`, (2) Extract `OrderAmountCalculator` from `CheckoutService` as noted in the TODO. |

### High Findings

| # | Severity | Source Agent(s) | Category | Target File | Finding | Suggested Fix |
|---|----------|----------------|----------|-------------|---------|---------------|
| H-1 | High | security | Fail-Open Default Deny | SecurityConfig.java L140 | `.anyRequest().permitAll()` instead of `.anyRequest().authenticated()`. New endpoints are silently open. | Change to `.anyRequest().authenticated()`, explicitly `permitAll()` for public routes. |
| H-2 | High | security | Session Cookie Attributes | application.properties | No `server.servlet.session.cookie.http-only`, `secure`, `same-site`, or `timeout` configured. | Add: `http-only=true`, `secure=true`, `same-site=strict`, `timeout=30m`. |
| H-3 | High | security, data-access, resilience | Race Condition (Optimistic Lock) | InventoryService, PointService, CouponService | `@Version` exists on entities but `OptimisticLockingFailureException` is never caught or retried. Concurrent checkouts trigger unhandled 500 errors. | Use `saveAndFlush()` after mutations; catch `OptimisticLockingFailureException` in `CheckoutService.placeOrder`; throw `BusinessException` prompting retry. |
| H-4 | High | security | No Rate Limiting | AuthController (all /auth/** endpoints) | No rate limiting on login, registration, password reset. `AuthService.recordLoginFailure` is never called (no `AuthenticationFailureHandler` wired). Unlimited brute-force attempts possible. | Implement `CustomAuthFailureHandler` calling `authService.recordLoginFailure()`; wire via `.failureHandler()` in SecurityConfig; add rate limiting filter for `/auth/**`. |
| H-5 | High | security | No Request Body Size Limit | All application*.properties | Missing `server.tomcat.max-http-form-post-size`, `spring.servlet.multipart.max-file-size/max-request-size`. Attacker can send multi-MB form submissions. | Add: `max-http-form-post-size=2MB`, `max-file-size=5MB`, `max-request-size=10MB`. |
| H-6 | High | security | Cart Item Count Unlimited | CartController, CartService | Unauthenticated users can add unlimited items to session carts, consuming DB capacity. No cart cleanup job. | Add max items check (e.g., 50 per cart) in `CartService.addItem()`. Add scheduled cleanup for expired carts. |
| H-7 | High | security | Missing Security Headers | SecurityConfig.java L150 | Missing `Referrer-Policy`, `Permissions-Policy` headers. Missing `server.server-header=` to suppress Tomcat version. | Add headers in SecurityFilterChain: `referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)`, `permissionsPolicy("camera=(), microphone=(), geolocation=()")`. |
| H-8 | High | security | AuthService Dead Code | SecurityConfig.java | `AuthService.recordLoginSuccess/Failure` is never called. No success/failure handler wired. `SecurityLog` table remains empty. Account lockout is non-functional. | Wire `CustomAuthSuccessHandler` → `authService.recordLoginSuccess()` and `CustomAuthFailureHandler` → `authService.recordLoginFailure()`. |
| H-9 | High | performance | Unbounded Order List | OrderService.java L140 | `listByUserId()` returns unbounded `List<Order>`. Power user with hundreds of orders loads all into memory. | Change to `Page<Order>` with `Pageable` parameter. |
| H-10 | High | performance | Unbounded Product List | ProductService.java L112 | `findByCategoryId()` and `findByStatus()` return unbounded `List<Product>`. Used in `AdminProductController.list()` with no pagination. | Add `Pageable` overloads to repository and service. |
| H-11 | High | performance | Duplicate Query | OrderController.java L81-83 | `findByIdAndUserId()` fetches order with items via `@EntityGraph`, then `listItems(id)` executes a second SELECT for the same items. Same in `AdminOrderController.detail()`. | Use `order.getItems()` from the entity-graph-loaded order. |
| H-12 | High | performance | No DTO Projections | ProductService L82, OrderService L140 | `search()` returns `Page<Product>` with all columns. List pages load full `description` (2000 chars) per row. No DTO projections anywhere. | Create `ProductSummaryDto` and `OrderSummaryDto` for list views. |
| H-13 | High | performance | PaymentService Inefficient | PaymentService.java L121 | `findByOrderId(orderId).stream().findFirst()` fetches ALL payments then discards all but first. | Change to `Optional<Payment> findFirstByOrderId(String)`. |
| H-14 | High | performance, resilience | Unbounded Email Queue | MailService.java L128-131 | `findByStatusOrderByScheduledAtAsc(PENDING)` loads ALL pending emails with no LIMIT. After SMTP outage, thousands of emails loaded at once → OOM risk. | Use `findTop100ByStatusOrderByScheduledAtAsc()` or `Pageable`. |
| H-15 | High | performance | N+1 in Cart Merge | CartService.java L344-353 | `mergeCart()` → `addItem()` loop triggers up to 4 queries × N items. | Batch-load existing items and prices; batch insert/update. |
| H-16 | High | resilience | No Health Probes | application.properties L14 | Liveness/readiness probes not separated. No `management.endpoint.health.probes.enabled=true`. | Enable probe separation for Kubernetes readiness. |
| H-17 | High | resilience | No SMTP Health Indicator | — | No custom `HealthIndicator` for SMTP. `/actuator/health` remains UP when mail server is down. | Create `SmtpHealthIndicator` calling `javaMailSender.testConnection()`. |
| H-18 | High | resilience | No Resilience4j / Spring Retry | pom.xml | No circuit breaker for SMTP. Flapping SMTP server causes repeated failures every 30s with no backoff. | Add `resilience4j-spring-boot3`; apply `@CircuitBreaker` to `MailService.handleSend()`. |
| H-19 | High | resilience | Mail Retry Fixed Interval | MailService.java L158 | Fixed 1-minute delay for all retries. No exponential backoff. 3-minute SMTP outage → permanent FAILED. | Exponential backoff: `now.plusMinutes((long) Math.pow(2, retryCount))`. Increase MAX_RETRY to 5. |
| H-20 | High | resilience | Scheduler Single Thread | MailService.java L128 | Default single-threaded `@Scheduled` executor. SMTP blocking halts ALL scheduled tasks. | Configure `spring.task.scheduling.pool.size=4` or define `TaskScheduler` bean. |
| H-21 | High | resilience | processQueue @Transactional scope | MailService.java L129 | Single `@Transactional` wraps all SMTP sends. DB connection held for entire batch. | (Merged with C-3) Remove `@Transactional` from `processQueue()`; per-message transaction. |
| H-22 | High | error-logging | Missing Exception Handlers | exception/ | No handler for `MethodArgumentNotValidException` or `BindException`. Spring Boot default handling bypasses error view templates and error ID pattern. | Add `@ExceptionHandler(MethodArgumentNotValidException.class)` returning `error/400` view. |
| H-23 | High | error-logging | RFC 7807 ProblemDetail | GlobalExceptionHandler.java | No `ProblemDetail` usage. AGENTS.md §5.2 requires RFC 7807 for error responses. Acceptable for pure Thymeleaf MVC, but no content negotiation for AJAX/JSON. | Add content-negotiation-aware handler for `Accept: application/json` or document deviation. |
| H-24 | High | error-logging | Prod Log Level Suppresses INFO | application-prod.properties L20 | `logging.level.com.skishop=WARN` suppresses INFO-level business events (order processing, mail failures). | Change to `logging.level.com.skishop=INFO` in production. |
| H-25 | High | error-logging | Prod logback-spring logger | logback-spring.xml L16-20 | `com.skishop` logger not configured in prod profile → inherits WARN from root, suppressing INFO business events. | Add `<logger name="com.skishop" level="INFO"/>` in prod springProfile. |
| H-26 | High | error-logging, security | X-Request-Id Not Sanitized | RequestIdFilter.java L66 | Incoming `X-Request-Id` header not validated. Malicious client can inject long strings or log-forging content into MDC. | Add length check (≤64 chars), alphanumeric + hyphens whitelist. Reject or regenerate on failure. |
| H-27 | High | data-access | Missing @Version | User, Product, Cart entities | No `@Version` field. Concurrent updates produce last-write-wins with no conflict detection. | Add `@Version private Long version;` to entities with concurrent update risk. |
| H-28 | High | data-access | Missing Audit Timestamps | 4 entities | Four entities lack `@CreationTimestamp`/`@UpdateTimestamp` audit fields. | Add audit fields per entity design requirements. |
| H-29 | High | api-endpoint | HiddenHttpMethodFilter | All controllers | Not enabled. `PUT`/`DELETE` from HTML forms with `_method` parameter won't work. Admin CRUD operations that rely on PUT/DELETE semantics affected. | Add `spring.mvc.hiddenmethod.filter.enabled=true` in `application.properties`. |
| H-30 | High | api-endpoint | Checkout Redirect Loses Errors | CheckoutController | Redirect on validation error loses `BindingResult`. User sees checkout page with no error messages. | Use flash attributes or render the form view directly on error instead of redirecting. |
| H-31 | High | api-endpoint | JPA Entities to Views | Multiple controllers | JPA entities passed directly to Thymeleaf templates. Risk of lazy-loading exceptions and tight view-model coupling. | Create view DTOs or use `@EntityGraph` to ensure all needed fields are loaded. |
| H-32 | High | architecture | Business Logic in Controller | CartController.applyCoupon() | Coupon application logic in controller layer violates layer separation. | Move coupon application logic to `CartService` or `CouponService`. |
| H-33 | High | architecture | Entity in DTO | CheckoutSummary | `CheckoutSummary` DTO contains JPA entity references. DTO layer should not leak persistence concerns. | Replace entity references with flat value fields or nested response DTOs. |
| H-34 | High | async-concurrency | Virtual Threads Not Enabled | application.properties | `spring.threads.virtual.enabled=true` not configured. Java 21 virtual threads would improve throughput for SMTP I/O and DB calls. | Add `spring.threads.virtual.enabled=true` (requires testing for thread-local compatibility). |
| H-35 | High | async-concurrency | MailService @Scheduled No Top-Level Try-Catch | MailService processQueue() | Unhandled exception in `@Scheduled` method silently stops future executions. | Wrap entire method body in `try-catch(Exception)` with error logging. |
| H-36 | High | async-concurrency | No ShedLock | MailService processQueue() | Multiple app instances execute `processQueue()` simultaneously → duplicate email sends. | Add ShedLock or database-based distributed lock for scheduled tasks. |
| H-37 | High | test-quality | Only 3/21 Repos Tested | repository/ | Only `OrderRepository`, `ProductRepository`, `UserRepository` have `@DataJpaTest`. 18 repositories untested. | Add `@DataJpaTest` for repos with custom `@Query` methods at minimum. |
| H-38 | High | test-quality | Controller POST/PUT/DELETE Untested | controller/ | Controller tests mostly cover GET happy paths. Mutation endpoints (POST, PUT, DELETE) and error paths under-tested. | Add error-path and mutation tests, especially form submissions. |
| H-39 | High | test-quality | CouponController No Tests | CouponController.java | Zero test coverage — no `CouponControllerTest.java` exists. | Create `CouponControllerTest` with `@WebMvcTest`. |
| H-40 | High | test-quality | CheckoutServiceTest 12 Mocks | CheckoutServiceTest.java L46-57 | 12 `@Mock` fields. Excessive mocking makes test fragile. | Extract test fixture helpers (e.g., `givenCartWithItems()`). |
| H-41 | High | test-quality | Security Test Incomplete | SecurityAuthorizationTest.java | Only 8 URL patterns tested. Missing: `/account/**`, `/checkout/**`, `/admin/orders/**`, `/admin/coupons/**`, `/admin/shipping-methods/**`. | Add comprehensive URL authorization matrix tests. |
| H-42 | High | tech-lead | Price Sort Broken | ProductController.java L98-104 | `resolveSort()` falls back to `name` when `price_asc`/`price_desc` requested (price in separate table). Users get name-sorted results silently — functional regression from Struts. | Implement JOIN query on `prices` table for price sort, or surface UI message. |
| H-43 | High | tech-lead | docker-compose Secret | docker-compose.yml L10, 33 | `POSTGRES_PASSWORD: skishop_password` hardcoded with `SPRING_PROFILES_ACTIVE: prod`. | Use `.env` file (gitignored) or Docker secrets. Change profile to `dev`. |
| H-44 | High | tech-lead | RegisterRequest No Email Uniqueness Check | AuthController.java L96-102 | No `existsByEmail()` before `registerNewUser()`. DB unique constraint throws unhandled `DataIntegrityViolationException` → generic 500. | Add pre-check or catch `DataIntegrityViolationException` with friendly error. |

---

## Escalation Items (Human Judgment Required)

| # | Priority | Source Agent(s) | Description | Recommended Decision Maker |
|---|----------|----------------|-------------|---------------------------|
| 1 | **Highest** | security | **Fail-open to fail-closed (H-1)**: Must decide exact list of public URLs before changing `.anyRequest().authenticated()`. Incorrect configuration locks out legitimate users. | Security Architect + Product Owner |
| 2 | **Highest** | security | **Rate limiting approach (H-4)**: Bucket4j, WAF-level, or custom filter? Affects infrastructure decisions. | Tech Lead + Infrastructure |
| 3 | **Highest** | security | **AuthService wiring (H-8)**: Full lockout implementation exists but is dead code. Must confirm lockout policy (auto-unlock timer, admin manual unlock) with stakeholders. | Product Owner + Security |
| 4 | **High** | ddd-domain | **Anemic domain model acceptance (C-6 to C-9)**: Should the project accept anemic domain model as an architectural choice for this migration? If yes, reclassify C-6 through C-9 from Critical to High. Recommended for migration context. | Tech Lead + Architect |
| 5 | **High** | resilience | **Resilience4j adoption scope**: Decide if circuit breaker is needed only for SMTP (Spring Retry sufficient) or for future external payment gateways (full Resilience4j). | Architect + Product |
| 6 | **High** | resilience | **Mail FAILED recovery**: No recovery path for permanently failed emails. Options: (a) admin retry UI, (b) automated re-queue, (c) alerts-only with support fallback. | Product Owner + Ops |
| 7 | **High** | resilience | **Kubernetes vs Docker Compose target**: Health probe separation is essential for K8s but less critical for Docker Compose. Confirm deployment platform. | Infrastructure + Ops |
| 8 | **High** | performance | **MailService transaction redesign**: Moving from single `@Transactional` to per-message transactions is a behavioral change. Requires load testing with SMTP stub. | Tech Lead + QA |
| 9 | **High** | performance | **Product search DTO projection**: Introducing `ProductSummaryDto` with price (from `prices` table) requires JOIN + constructor expression. Moderate refactor affecting views. Prioritize based on catalog size. | Tech Lead |
| 10 | **Normal** | tech-lead | **Price sort fallback (H-42)**: Is name-sort fallback acceptable for MVP, or must price sort work with JOIN? Feature parity with Struts version affected. | Product Owner |
| 11 | **Normal** | tech-lead | **Email uniqueness on registration (H-44)**: Should form show "email exists" (UX) or generic "registration failed" (prevents enumeration)? Security vs UX tradeoff. | Product Owner + Security |
| 12 | **Normal** | tech-lead | **Post-login redirect (CartMergeSuccessHandler.setAlwaysUseDefaultTargetUrl)**: Always redirect to home, or return to originally requested page? | Product Owner |
| 13 | **Normal** | error-logging | **Prod logging level**: `WARN`-only is unusual for EC. If intentional (reduce cost), document. Otherwise, change to `INFO`. | Ops + Tech Lead |
| 14 | **Normal** | test-quality | **Repository test scope**: Many repos use only derived queries (framework-tested). Decide which custom `@Query` repos need explicit `@DataJpaTest`. | Tech Lead + QA |

---

## Conflict Resolution Record

| # | Agent A | Agent B | Conflict Description | Resolution | Rationale |
|---|---------|---------|---------------------|------------|-----------|
| 1 | ddd-domain-reviewer | data-access-reviewer, performance-reviewer | DDD reviewer flagged child entity repositories (OrderItemRepository, CartItemRepository, etc.) as Critical aggregate boundary violations. Data-access and performance reviewers implicitly accept these repositories for query efficiency and direct data manipulation needs. | **Apply predefined rule: Prioritize DDD boundaries.** However, in the context of Struts migration, full DDD enforcement is a Phase 2 improvement. Reclassification to High is recommended (see Escalation #4). Tech-lead initial review did not flag these, supporting the pragmatic approach. | For a migration project from procedural Struts 1.3, demanding full DDD tactical patterns (aggregate-only repos, value objects, domain events) in the first migration phase is impractical. The codebase correctly implements the target architecture (Controller → Service → Repository layering). DDD enrichment should be a separate improvement initiative after functional equivalence is verified. |

---

## Design Document Cross-Reference Results

### Deviations from Design Documents

| # | Design Spec | Deviation | Impact |
|---|-------------|-----------|--------|
| 1 | AGENTS.md §5.2: RFC 7807 `ProblemDetail` for error responses | Not implemented — handlers return Thymeleaf views only | Low for MVC app; Medium if AJAX endpoints planned |
| 2 | AGENTS.md §4.3: `anyRequest` should be `authenticated()` | Implementation uses `.anyRequest().permitAll()` (fail-open) | High — new endpoints silently open |
| 3 | AGENTS.md §7.1: `flyway-database-postgresql` required dependency | Intentionally omitted (Flyway 9.x bundled with Spring Boot 3.2) | Low — must add when upgrading to Spring Boot 3.3+/Flyway 10+ |
| 4 | AGENTS.md §10.5: `.dockerignore` required | Missing from project | Low — multi-stage build mitigates |
| 5 | AGENTS.md §4.4: Login success/failure handlers for security logging | `AuthService.recordLoginSuccess/Failure` not wired to Spring Security | High — account lockout and security audit are non-functional |

### Unimplemented Design Elements

| # | Design Element | Status |
|---|---------------|--------|
| 1 | `AuthenticationFailureHandler` → `AuthService.recordLoginFailure()` | Not wired |
| 2 | `AuthenticationSuccessHandler` (beyond cart merge) → `AuthService.recordLoginSuccess()` | Not wired |
| 3 | Email uniqueness validation on registration | Missing pre-check |
| 4 | Idempotency key for checkout | Not implemented |
| 5 | Cart expiration cleanup job | Not implemented |
| 6 | Micrometer business metrics | Dependencies present, zero metrics registered |
| 7 | Prometheus endpoint exposure | Not in `management.endpoints.web.exposure.include` |

---

## Strengths (What the Codebase Does Well)

The following areas demonstrate high-quality implementation:

1. **Zero Prohibited Pattern Violations**: All 11 prohibited patterns checked by tech-lead are completely clean across 102 source files. No `System.out.println`, no `@Autowired` field injection, no `catch(Exception){}` swallowing, no hardcoded secrets, no string-concatenation SQL, no `new Service()`, no `Optional.get()`, no PII in logs, no `java.util.Date`, no Controller→Repository, no `@Transactional` on Controller.

2. **Complete Struts Migration**: All 29 Struts Actions successfully migrated. Zero `*.do` URLs, zero `javax.*` imports, zero Struts dependencies. URL mapping table fully aligned with AGENTS.md spec.

3. **Security Foundation**: Spring Security configuration covers all 7 required areas (URL auth, form login, logout, session fixation protection, max sessions, CSRF enabled, security headers including CSP/HSTS/X-Frame-Options/X-Content-Type-Options). `DelegatingPasswordEncoder` with BCrypt + legacy SHA-256 auto-upgrade correctly implemented. `CustomUserDetailsService` implements both `UserDetailsService` and `UserDetailsPasswordService`.

4. **Clean DI**: 100% constructor injection via `@RequiredArgsConstructor`. Zero `@Autowired`. Clean dependency direction (Controller → Service → Repository).

5. **DTO Layer**: All request inputs use record DTOs with Bean Validation annotations. No JPA entity used as direct `@RequestBody`/`@ModelAttribute` binding target. Mass assignment protection effectively implemented.

6. **Exception Handling**: Well-structured `GlobalExceptionHandler` with error IDs for tracking. No empty catch blocks. All catch blocks either log with stack trace or re-throw appropriately. `ResourceNotFoundException` → 404, `BusinessException` → redirect/view.

7. **Correlation ID**: `RequestIdFilter` with proper MDC lifecycle, response header `X-Request-Id`, and `finally` cleanup. `logback-spring.xml` pattern includes `%X{reqId}`.

8. **Structured Logging**: Zero string concatenation in log statements. All SLF4J `{}` placeholders. JSON logging with `LogstashEncoder` for production.

9. **CheckoutService Atomicity**: Full 11-step checkout flow in single `@Transactional` with compensating actions on failure. Design document alignment verified.

10. **Test Framework**: Consistent `should_X_when_Y` naming, AAA pattern adherence, `@WebMvcTest`/`@DataJpaTest`/`@SpringBootTest` proper usage, custom `@WithSkiShopUser` annotation for security tests.

---

## Individual Agent Detail Reports

<details>
<summary>tech-lead Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 3 / Medium: 6 / Low: 4
- Score: 19/20

### Prohibited Pattern Check: ALL 11 CLEAN ✅

### Design Document Alignment: FULLY ALIGNED ✅
- All 29 Actions migrated
- URL mapping table complete
- SecurityConfig complete (7/7 requirements)
- DelegatingPasswordEncoder correct
- CheckoutService 11-step flow verified
- Entity design conventions followed

### Findings
1. **High** — Price sort broken: `resolveSort()` silently falls back to `name` when price sort requested
2. **High** — docker-compose hardcoded `POSTGRES_PASSWORD` with `prod` profile
3. **High** — RegisterRequest missing email uniqueness check → 500 on duplicate
4. **Medium** — Missing `@Slf4j` on CouponController
5. **Medium** — Status values as String constants (TODO for enum migration)
6. **Medium** — `CartMergeSuccessHandler.setAlwaysUseDefaultTargetUrl(true)` overrides saved request
7. **Medium** — Admin refund endpoint IDOR documentation (acceptable but needs comment)
8. **Medium** — Flyway `flyway-database-postgresql` intentionally omitted (needs upgrade note)
9. **Medium** — CheckoutService 12 dependencies at maintainability limit
10. **Low** — `CartItem.cart` `@ManyToOne(LAZY)` without `@ToString.Exclude` (not active)
11. **Low** — `version: '3.9'` deprecated in docker-compose.yml
12. **Low** — No `.dockerignore` file
13. **Low** — Dockerfile uses `wget` instead of `curl` (valid Alpine adaptation)

</details>

<details>
<summary>architecture-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 2 / Medium: 5 / Low: 2
- Score: 21/25

### Key Results
- Zero layer violations detected
- All controllers depend only on services
- Clean package structure matching AGENTS.md spec

### Findings
1. **High** — Business logic in `CartController.applyCoupon()` violates layer separation
2. **High** — JPA entities referenced in `CheckoutSummary` DTO
3. **Medium** — Entities passed directly to Thymeleaf views (5+ controllers)
4. **Medium** — Missing `WebMvcConfig` class
5. **Medium** — `CartMergeSuccessHandler` placed in `config/` instead of dedicated `security/` package
6. **Medium** — `AppConstants` mixes concerns (order status + payment status + general)
7. **Medium** — No interface segregation for services (acceptable for monolith)
8. **Low** — `SkiShopUserDetails` in `security/` package naming (minor)
9. **Low** — Unused `@EnableScheduling` could be in dedicated config class

</details>

<details>
<summary>ddd-domain-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 4 / High: 9 / Medium: 5 / Low: 3
- Score: 9/30

### Note
DDD reviewer applies strict tactical DDD pattern evaluation. In the context of this Struts migration, the anemic domain model is an expected characteristic. See Escalation #4 and Conflict Resolution #1. Recommended reclassification: C-6 to C-9 → High for migration context.

### Critical
1. All 23 entities are anemic data containers
2. Child entity repositories bypass aggregate root access control
3. No Value Objects for domain concepts
4. No domain event mechanism

### High (9 items)
- Mixed concerns in entities (JPA + Lombok + validation)
- No aggregate root enforcement pattern
- Business rules scattered across 18 services
- No bounded context separation
- Entity state transitions unprotected
- Order item price not captured at order time (relies on current price)
- No invariant validation in entities
- Domain language not enforced (generic names)
- Bidirectional relationships without safety methods

</details>

<details>
<summary>api-endpoint-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 3 / Medium: 5 / Low: 2
- Score: 21/25

### Key Results
- All controllers use `@Controller` correctly (not `@RestController`)
- Zero `*.do` URLs
- Proper `@Valid` usage on form bindings

### High Findings
1. `HiddenHttpMethodFilter` not enabled — PUT/DELETE from forms won't work
2. `ConstraintViolationException` handler missing in GlobalExceptionHandler
3. Checkout redirect loses `BindingResult` errors

### Medium Findings
1. JPA entities passed to views (5 controllers)
2. No `@ResponseStatus` on several error handlers
3. Cart operations use GET for state changes (should be POST)
4. Admin controllers lack standardized error response
5. No OpenAPI `@Operation` annotations

</details>

<details>
<summary>java-standards-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 2 / High: 3 / Medium: 3 / Low: 2
- Score: 21/25

### Positive Signals
- Excellent naming convention adherence
- Strong Java 21 feature adoption (records, sealed classes, pattern matching, text blocks)
- Zero prohibited patterns

### Critical Findings
1. TODO comment in `AppConstants.java` (incomplete enum migration)
2. TODO comment in `CheckoutService.java` (pending `OrderAmountCalculator` extraction)

### High Findings
1. 13 `LocalDateTime.now()` calls without `Clock` injection (untestable time dependencies)
2. Magic string `"ACTIVE"` used in multiple locations
3. `AppConstants` status strings need enum migration

### Medium Findings
1. `BigDecimal` comparisons may not use `compareTo`
2. Some methods exceed 20 lines
3. Limited use of `switch` expressions for status conversions

</details>

<details>
<summary>async-concurrency-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 3 / Medium: 3 / Low: 2
- Score: 18/25

### Key Results
- No blocking calls in controller/service layer
- No async processing patterns (no `@Async`, no `CompletableFuture`)
- Only external I/O is SMTP via `@Scheduled`

### High Findings
1. `spring.threads.virtual.enabled=true` not configured (Java 21 benefit unused)
2. `MailService.processQueue()` missing top-level try-catch for `@Scheduled`
3. No ShedLock for multi-instance `@Scheduled` (duplicate email sends)

### Medium Findings
1. Long-held DB connection during SMTP I/O
2. No graceful shutdown for scheduled tasks
3. Single-threaded scheduler pool

</details>

<details>
<summary>error-logging-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 5 / Medium: 5 / Low: 3
- Score: 20/25

### Positive Signals
- Zero PII in log statements
- Zero `System.out.println`
- All SLF4J `{}` placeholder usage
- Good `RequestIdFilter` MDC implementation

### High Findings
1. Missing `MethodArgumentNotValidException` handler
2. No RFC 7807 `ProblemDetail` usage
3. Prod logback-spring.xml missing `com.skishop` logger at INFO level
4. `logging.level.com.skishop=WARN` in prod suppresses business events
5. `X-Request-Id` header not sanitized (log injection risk)

### Medium Findings
1. Missing `@Slf4j` on 4 services and CouponController
2. No custom Micrometer metrics registered
3. `BusinessException` missing `@ResponseStatus`
4. No rolling file appender in prod (stdout only)
5. Error ID not correlated with request ID in MDC

</details>

<details>
<summary>data-access-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 5 / Medium: 6 / Low: 3
- Score: 21/25

### Positive Signals
- Excellent entity design: proper `java.time`, explicit `@Column(name=...)`, UUID PK without `@GeneratedValue`
- Clean Flyway migrations V1-V12
- `@Transactional(readOnly=true)` correctly applied on read methods
- `@EntityGraph` used for eager loading where needed

### High Findings
1. Missing `@Version` on User, Product, Cart entities
2. No `OptimisticLockException` handler in GlobalExceptionHandler
3. Unbounded list queries for orders by user ID
4. Unbounded list queries for products by category
5. Missing audit timestamps on 4 entities

### Medium Findings
1. N+1 risk in `CheckoutService.buildOrderItems`
2. No DTO projections for list views
3. `MailService` wraps SMTP I/O in `@Transactional`
4. `findByOrderId().stream().findFirst()` inefficiency
5. Entity bidirectional relationships missing cascade consistency
6. No database-level check constraints for business rules

</details>

<details>
<summary>config-di-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 2 / Medium: 3 / Low: 2
- Score: 23/25

### Positive Signals
- SecurityFilterChain fully compliant with all 7 AGENTS.md requirements
- 100% constructor injection via `@RequiredArgsConstructor`
- Zero `@Autowired`
- Profile-based configuration properly separated
- All secrets use `${ENV_VAR}`

### High Findings
1. `anyRequest().permitAll()` fail-open default (also flagged by security-reviewer)
2. Missing `server.server-header=` to suppress Tomcat version

### Medium Findings
1. Hardcoded dev DB URL in `application-dev.properties` (should use env var)
2. `TaxService` uses `@Value` instead of `@ConfigurationProperties`
3. No `@ConfigurationPropertiesScan` annotation

</details>

<details>
<summary>security-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 7 / Medium: 6 / Low: 3
- Score: 49/65

### OWASP Top 10 Results
- A01 Broken Access Control: ⚠️ (fail-open default)
- A02 Cryptographic Failures: ✅
- A03 Injection: ✅
- A07 XSS: ✅ (no `th:utext`)
- A08 Mass Assignment: ✅ (all record DTOs)
- A10 SSRF: ✅ (no external HTTP calls)

### Struts Legacy Pattern Check: ALL CLEAN ✅

### STRIDE Analysis Summary
- Spoofing: 3 sufficient, 2 insufficient
- Tampering: 5 sufficient, 2 insufficient
- Repudiation: 2 sufficient, 2 insufficient
- Information Disclosure: 5 sufficient, 2 insufficient
- Denial of Service: 2 sufficient, 3 insufficient, 1 unmitigated
- Elevation of Privilege: 6 sufficient, 0 insufficient

### High Findings
1. `.anyRequest().permitAll()` instead of `.authenticated()`
2. Session cookie attributes not configured
3. Race conditions — `@Version` without exception handling
4. No request body size limits
5. No rate limiting on auth endpoints
6. No cart item count limits
7. Missing `Referrer-Policy` and `Permissions-Policy` headers

### Medium Findings
1. `AuthService` security logging not wired (dead code)
2. No admin operation audit logging
3. `BusinessException.getMessage()` leaks internal details to view
4. No idempotency key for checkout
5. `CheckoutRequest` accepts `cartId` from client (server overrides, but confusing)
6. Page size clamped correctly (noted as positive)

</details>

<details>
<summary>dependency-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 0 / Medium: 2 / Low: 3

### Positive Signals
- All prohibited dependencies absent (struts, log4j 1.x, commons-dbcp, etc.)
- No `-SNAPSHOT` versions
- Parent POM correct: `spring-boot-starter-parent:3.2.12`
- `java.version=21`
- All required dependencies present

### Medium Findings
1. `thymeleaf-layout-dialect` explicit version override (should use BOM-managed version)
2. Deprecated `maven-compiler-plugin` `source`/`target` settings (use `release` instead)

### Low Findings
1. `flyway-database-postgresql` intentionally absent (needs upgrade note for Spring Boot 3.3+)
2. `springdoc-openapi` present but no `@Operation` annotations in controllers
3. Missing `spring-boot-starter-cache` for future `@Cacheable` adoption

</details>

<details>
<summary>test-quality-reviewer Review Report</summary>

### Summary
- Verdict: ⚠️ Warning
- Findings: Critical: 0 / High: 6 / Medium: 8 / Low: 3
- Score: 19/25

### Positive Signals
- Excellent `should_X_when_Y` naming convention
- Strong AAA pattern adherence in service tests
- Custom `@WithSkiShopUser` annotation for security tests
- H2 `MODE=PostgreSQL` correctly configured

### High Findings
1. Only 3/21 Repository interfaces have `@DataJpaTest` tests
2. Controller POST/PUT/DELETE actions under-tested, error paths minimal
3. `CouponController` has zero test coverage
4. Error case tests < happy case tests (should be ≥ equal)
5. 12 `@Mock` fields in `CheckoutServiceTest` (inherent complexity)
6. Security test covers only 8 URL patterns, many secured endpoints uncovered

### Medium Findings
1. Zero `@ParameterizedTest` usage
2. Limited boundary value testing
3. No test data builders / fixture factories
4. `TestSecurityConfig` disables CSRF for `@WebMvcTest` (acceptable but document)
5. No Testcontainers for PostgreSQL-fidelity testing
6. `EntitySchemaValidationTest` lacks AAA comments
7. JaCoCo has no enforcement rules (no minimum coverage threshold)
8. `CartServiceAdditionalTest` duplicates tests from `CartServiceTest`

</details>

<details>
<summary>performance-reviewer Review Report</summary>

### Summary
- Verdict: ❌ Fail
- Findings: Critical: 3 / High: 8 / Medium: 4 / Low: 1
- Score: 11/25

### N+1 Query Detections
1. **Critical** — `CheckoutService.buildOrderItems()`: `productService.findById()` per cart item
2. **Critical** — `InventoryService.reserveItems/releaseItems/deductStock()`: per-item DB lookup
3. **High** — `CartService.mergeCart()`: up to 4 queries × N items in loop

### Pagination Gaps
- `GET /orders` — unbounded
- `GET /admin/products` — unbounded
- `ProductService.findByCategoryId()` — unbounded
- `MailService.processQueue()` — unbounded email batch

### Cache Strategy: ABSENT
- Zero `@Cacheable` usage. Categories, active coupons, shipping methods are classic cache candidates.

### Other Findings
- `PaymentService`: `findByOrderId().stream().findFirst()` loads all payments
- `OrderController`: duplicate query (EntityGraph + separate listItems)
- No DTO projections for list views (full entities with description column)

</details>

<details>
<summary>resilience-reviewer Review Report</summary>

### Summary
- Verdict: ❌ Fail
- Findings: Critical: 2 / High: 7 / Medium: 4 / Low: 1
- Score: 11/25

### Key Architecture Observations
- Only external I/O: SMTP via JavaMailSender
- No RestTemplate/WebClient usage (no external API calls)
- Payment is local-only (Luhn validation, no gateway)

### Critical Findings
1. No SMTP timeout configuration → indefinite thread blocking
2. No `server.shutdown=graceful` → data corruption risk during deployment

### High Findings
1. No liveness/readiness probe separation
2. No SMTP `HealthIndicator`
3. No Resilience4j or Spring Retry dependency
4. Mail retry uses fixed 1-minute interval (no exponential backoff)
5. Unbounded email queue fetch
6. `processQueue()` wraps all sends in single `@Transactional`
7. Single-threaded `@Scheduled` executor

### Medium Findings
1. No dead letter / alerting for failed emails
2. No graceful mail task shutdown (`@PreDestroy`)
3. Compensation action failure in `placeOrder` → lost exceptions
4. No HikariCP statement timeout

</details>

---

## Recommendations for Fix Prioritization

### Priority 1 — Must Fix Before Merge (Critical + High Security)

| Fix | Effort | Impact |
|-----|--------|--------|
| N+1 queries in CheckoutService.buildOrderItems (C-1) | Low | High (checkout performance) |
| N+1 queries in InventoryService reserve/release/deduct (C-2) | Low | High (checkout performance) |
| MailService transaction redesign (C-3) | Medium | High (connection pool protection) |
| SMTP timeout configuration (C-4) | Trivial | High (prevents indefinite blocking) |
| Graceful shutdown (C-5) | Trivial | High (prevents data corruption) |
| `.anyRequest().authenticated()` (H-1) | Low | High (security posture) |
| Session cookie hardening (H-2) | Trivial | High (session security) |
| Rate limiting on auth endpoints (H-4) | Medium | High (brute force protection) |
| Wire AuthService to Spring Security handlers (H-8) | Medium | High (enables account lockout + audit) |

### Priority 2 — Should Fix Before Release

| Fix | Effort | Impact |
|-----|--------|--------|
| OptimisticLockingFailureException handling (H-3) | Low | Medium (concurrency safety) |
| Request body size limits (H-5) | Trivial | Medium (DoS prevention) |
| Pagination for orders/products (H-9, H-10) | Low | Medium (memory safety) |
| HiddenHttpMethodFilter (H-29) | Trivial | Medium (admin CRUD) |
| Email queue pagination (H-14) | Low | Medium (OOM prevention) |
| Prod log level → INFO (H-24, H-25) | Trivial | Medium (operational visibility) |
| X-Request-Id sanitization (H-26) | Low | Medium (log injection) |

### Priority 3 — Improvement Cycle

- DDD enrichment (Value Objects, domain methods, aggregate boundaries)
- DTO projections for list views
- Caching strategy (`@Cacheable` for categories, coupons, shipping methods)
- Micrometer business metrics
- Resilience4j for SMTP
- Test coverage expansion (repository slice tests, controller mutation tests)
- TODO resolution (enum migration, OrderAmountCalculator extraction)

---

*Report generated by orchest-code-review orchestrator. 14/14 agents completed successfully. 1 conflict identified and resolved (DDD vs Data-Access aggregate boundary). No agent timeouts or errors.*
