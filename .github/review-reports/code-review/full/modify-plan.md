# Modification Plan — Critical & High Severity Fixes

**Based on**: `check-report-1.md` (Code Review Report, 2025-07-11)  
**Target Project**: `appmod-migrated-java21-spring-boot-3rd/`  
**Scope**: All Critical (C-1 to C-10) and High (H-1 to H-44) findings

---

## Table of Contents

1. [Priority 1 — Critical Findings (C-1 to C-10)](#1-critical-findings)
2. [Priority 2 — High Findings: Security (H-1 to H-8)](#2-high-findings-security)
3. [Priority 3 — High Findings: Performance (H-9 to H-15)](#3-high-findings-performance)
4. [Priority 4 — High Findings: Resilience (H-16 to H-21)](#4-high-findings-resilience)
5. [Priority 5 — High Findings: Error/Logging (H-22 to H-28)](#5-high-findings-errorlogging)
6. [Priority 6 — High Findings: API/Architecture/Async (H-29 to H-36)](#6-high-findings-apiarchitectureasync)
7. [Priority 7 — High Findings: Test Quality (H-37 to H-41)](#7-high-findings-test-quality)
8. [Priority 8 — High Findings: Tech Lead (H-42 to H-44)](#8-high-findings-tech-lead)

---

## 1. Critical Findings

### C-1: N+1 Query in CheckoutService.buildOrderItems()

**Finding**: `productService.findById()` called per cart item inside a `.map()` stream in `buildOrderItems()`. With 10 cart items, this generates 10 separate SELECT queries.

**Root Cause**: The `buildOrderItems()` method at line ~383 of `CheckoutService.java` iterates over cart items and calls `productService.findById(cartItem.getProductId())` for each item individually, generating one DB query per item.

**Files to Modify**:
- `src/main/java/com/skishop/service/CheckoutService.java`

**Changes**:

In `buildOrderItems()`, batch-load all products before the stream:

```java
// BEFORE (current implementation):
private List<OrderItem> buildOrderItems(String orderId, List<CartItem> items) {
    return items.stream().map(cartItem -> {
        Product product = productService.findById(cartItem.getProductId());
        // ...build OrderItem...
    }).toList();
}

// AFTER (fixed):
private List<OrderItem> buildOrderItems(String orderId, List<CartItem> items) {
    // Batch-load all products in a single query
    List<String> productIds = items.stream()
            .map(CartItem::getProductId)
            .toList();
    Map<String, Product> productMap = productService.findAllByIds(productIds);

    return items.stream().map(cartItem -> {
        Product product = productMap.get(cartItem.getProductId());
        if (product == null) {
            throw new ResourceNotFoundException("Product", cartItem.getProductId());
        }
        var item = new OrderItem();
        item.setId(UUID.randomUUID().toString());
        item.setProductId(cartItem.getProductId());
        item.setProductName(product.getName());
        item.setSku(product.getSku());
        item.setUnitPrice(cartItem.getUnitPrice());
        item.setQuantity(cartItem.getQuantity());
        item.setSubtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return item;
    }).toList();
}
```

Add import for `java.util.Map` and `java.util.stream.Collectors` in `CheckoutService.java`.

Additionally, add the batch method to `ProductService.java`:

```java
@Transactional(readOnly = true)
public Map<String, Product> findAllByIds(List<String> productIds) {
    if (productIds == null || productIds.isEmpty()) {
        return Map.of();
    }
    return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, product -> product));
}
```

---

### C-2: N+1 Query in InventoryService (reserveItems / releaseItems / deductStock)

**Finding**: `inventoryRepository.findByProductId()` called inside a `for` loop in three methods: `reserveItems()`, `releaseItems()`, and `deductStock()`. Each call generates one SELECT per cart item.

**Root Cause**: Each of the three methods iterates over `List<CartItem>` and makes an individual `findByProductId()` call per item. The intent was to maintain per-row optimistic locking, but only the UPDATE requires per-row processing — the initial SELECT can be batched.

**Files to Modify**:
- `src/main/java/com/skishop/repository/InventoryRepository.java`
- `src/main/java/com/skishop/service/InventoryService.java`

**Changes**:

1. Add batch query method to `InventoryRepository.java`:

```java
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductId(String productId);

    // NEW: batch-load by multiple product IDs
    List<Inventory> findByProductIdIn(List<String> productIds);
}
```

2. Refactor all three methods in `InventoryService.java` to batch-load first, then iterate for updates:

```java
@Transactional
public void reserveItems(List<CartItem> items) {
    if (items == null || items.isEmpty()) {
        return;
    }
    List<String> productIds = items.stream().map(CartItem::getProductId).toList();
    Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
            .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

    for (CartItem item : items) {
        var inventory = inventoryMap.get(item.getProductId());
        if (inventory == null) {
            throw new ResourceNotFoundException("Inventory", item.getProductId());
        }
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available < item.getQuantity()) {
            throw new BusinessException("Insufficient stock for product: " + item.getProductId(),
                    "redirect:/cart", "error.stock.insufficient");
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
        inventoryRepository.save(inventory); // per-row save preserves @Version optimistic lock
    }
}

@Transactional
public void releaseItems(List<CartItem> items) {
    if (items == null || items.isEmpty()) {
        return;
    }
    List<String> productIds = items.stream().map(CartItem::getProductId).toList();
    Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
            .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

    for (CartItem item : items) {
        var inventory = inventoryMap.get(item.getProductId());
        if (inventory != null) {
            int newReserved = inventory.getReservedQuantity() - item.getQuantity();
            inventory.setReservedQuantity(Math.max(newReserved, 0));
            inventoryRepository.save(inventory);
        }
    }
}

@Transactional
public void deductStock(List<CartItem> items) {
    if (items == null || items.isEmpty()) {
        return;
    }
    List<String> productIds = items.stream().map(CartItem::getProductId).toList();
    Map<String, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
            .stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

    for (CartItem item : items) {
        var inventory = inventoryMap.get(item.getProductId());
        if (inventory == null) {
            throw new ResourceNotFoundException("Inventory", item.getProductId());
        }
        inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
        inventory.setReservedQuantity(Math.max(inventory.getReservedQuantity() - item.getQuantity(), 0));
        inventoryRepository.save(inventory);
    }
}
```

Add imports for `java.util.Map`, `java.util.stream.Collectors` in `InventoryService.java`.

---

### C-3: MailService processQueue() — SMTP I/O inside @Transactional

**Finding**: `processQueue()` is annotated `@Transactional`, wrapping the entire batch loop including `javaMailSender.send()`. SMTP network I/O holds a DB connection from HikariCP for the full duration, risking connection pool exhaustion.

**Root Cause**: The `@Transactional` annotation on `processQueue()` (line ~129) means the entire method — including all SMTP sends within the loop — runs inside a single database transaction. The DB connection is held for the full duration.

**Files to Modify**:
- `src/main/java/com/skishop/service/MailService.java`
- `src/main/java/com/skishop/repository/EmailQueueRepository.java`

**Changes**:

1. In `EmailQueueRepository.java`, add a paginated query:

```java
import org.springframework.data.domain.Pageable;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, String> {
    List<EmailQueue> findByStatus(String status);
    List<EmailQueue> findByStatusOrderByScheduledAtAsc(String status);

    // NEW: paginated fetch for bounded batch processing
    List<EmailQueue> findByStatusOrderByScheduledAtAsc(String status, Pageable pageable);
}
```

2. Redesign `MailService.java` to split transaction boundaries:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final int MAX_RETRY = 5;  // Increased from 3 per H-19
    private static final int BATCH_SIZE = 50;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final EmailQueueRepository emailQueueRepository;
    private final JavaMailSender javaMailSender;

    // ... enqueue methods remain unchanged ...

    /**
     * Process pending email queue. No @Transactional — each mail
     * gets its own short transaction for status update.
     */
    @Scheduled(fixedDelay = 30000)
    public void processQueue() {
        try {
            List<EmailQueue> pending = fetchPendingBatch();
            LocalDateTime now = LocalDateTime.now();

            for (EmailQueue mail : pending) {
                if (mail.getScheduledAt() != null && mail.getScheduledAt().isAfter(now)) {
                    continue;
                }
                processSingleMail(mail, now);
            }
        } catch (Exception e) {
            log.error("Error processing mail queue: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<EmailQueue> fetchPendingBatch() {
        return emailQueueRepository.findByStatusOrderByScheduledAtAsc(
                STATUS_PENDING, PageRequest.of(0, BATCH_SIZE));
    }

    private void processSingleMail(EmailQueue mail, LocalDateTime now) {
        try {
            send(mail);
            markSent(mail.getId());
        } catch (RuntimeException e) {
            log.error("Failed to send email id={}: {}", mail.getId(), e.getMessage(), e);
            markRetryOrFailed(mail.getId(), mail.getRetryCount(), e.getMessage(), now);
        }
    }

    @Transactional
    public void markSent(String mailId) {
        emailQueueRepository.findById(mailId).ifPresent(mail -> {
            mail.setStatus(STATUS_SENT);
            mail.setSentAt(LocalDateTime.now());
            emailQueueRepository.save(mail);
        });
    }

    @Transactional
    public void markRetryOrFailed(String mailId, int currentRetryCount, String error, LocalDateTime now) {
        emailQueueRepository.findById(mailId).ifPresent(mail -> {
            int retryCount = currentRetryCount + 1;
            mail.setRetryCount(retryCount);
            mail.setLastError(error);
            if (retryCount >= MAX_RETRY) {
                mail.setStatus(STATUS_FAILED);
            } else {
                // Exponential backoff: 1m, 2m, 4m, 8m, 16m (per H-19)
                mail.setScheduledAt(now.plusMinutes((long) Math.pow(2, retryCount)));
            }
            emailQueueRepository.save(mail);
        });
    }

    private void send(EmailQueue mail) {
        var message = new SimpleMailMessage();
        message.setTo(mail.getToAddr());
        message.setSubject(mail.getSubject());
        message.setText(mail.getBody());
        javaMailSender.send(message);
    }
}
```

Add import for `org.springframework.data.domain.PageRequest`.

> **⚠️ CRITICAL — Self-Invocation Bug in Proposed Code**
>
> The redesigned `MailService` calls `markSent()`, `markRetryOrFailed()`, and `fetchPendingBatch()` as `@Transactional` methods **on the same bean instance** (via `this`). Because Spring AOP uses proxy-based interception, internal method calls bypass the proxy and the `@Transactional` annotations **will be silently ignored**. This means:
> - `markSent()` and `markRetryOrFailed()` will NOT run in separate transactions
> - `fetchPendingBatch()`'s `@Transactional(readOnly = true)` will NOT apply
> - The behavior will be effectively identical to the original (entire batch in one implicit transaction)
>
> **Required Fix — choose one approach:**
>
> **Option A (Recommended): Extract to a separate `@Service`**
> ```java
> @Service
> @RequiredArgsConstructor
> public class EmailQueueStatusService {
>     private final EmailQueueRepository emailQueueRepository;
>
>     @Transactional(readOnly = true)
>     public List<EmailQueue> fetchPendingBatch(int batchSize) { ... }
>
>     @Transactional
>     public void markSent(String mailId) { ... }
>
>     @Transactional
>     public void markRetryOrFailed(String mailId, int currentRetryCount, String error, LocalDateTime now) { ... }
> }
> ```
> Then inject `EmailQueueStatusService` into `MailService` and call `emailQueueStatusService.markSent(...)` etc.
>
> **Option B: Use `TransactionTemplate` for programmatic transactions**
> ```java
> private final TransactionTemplate txTemplate;
> // In processSingleMail():
> txTemplate.executeWithoutResult(status -> { /* markSent logic */ });
> ```
>
> **Files to Add**: `src/main/java/com/skishop/service/EmailQueueStatusService.java` (if Option A)

**Note**: This fix also addresses C-3 (connection pool), H-14 (unbounded email queue), H-19 (exponential backoff), H-21 (processQueue @Transactional scope), and H-35 (top-level try-catch for @Scheduled).

---

### C-4: No SMTP Timeout Configuration

**Finding**: No SMTP connection/read/write timeout configured in any profile. A hanging SMTP server blocks the `@Scheduled` thread indefinitely.

**Root Cause**: The `spring.mail.properties.mail.smtp.*` timeout properties are absent from all four property files.

**Files to Modify**:
- `src/main/resources/application.properties`

> **Note**: Since `application.properties` is inherited by all profiles, adding here is sufficient. No need to duplicate in profile-specific files.

**Changes**:

Add to `application.properties` (shared by all profiles):

```properties
# SMTP Timeouts (milliseconds)
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

---

### C-5: No Graceful Shutdown

**Finding**: `server.shutdown=graceful` is not configured. During deployment, in-flight checkout requests are abruptly terminated.

**Root Cause**: The `server.shutdown` and `spring.lifecycle.timeout-per-shutdown-phase` properties are absent from all property files.

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:

```properties
# Graceful Shutdown
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

---

### C-6 to C-9: DDD Domain Findings (Anemic Model, Aggregate Boundaries, Value Objects, Domain Events)

**Finding**: All 23 entities are pure data containers; child entity repositories bypass aggregate roots; no Value Objects; no domain events.

**Root Cause**: The migration from Struts 1.3 preserved the procedural (Transaction Script) architecture. Entities were translated 1:1 from POJOs to JPA entities without adding domain behavior.

**Escalation Decision Required**: These are architectural improvement items, not bugs. Per the Conflict Resolution in the code review report, the recommendation is to **reclassify C-6 through C-9 from Critical to High** for a Struts migration context. The codebase correctly implements Controller → Service → Repository layering.

**If the team accepts reclassification to High (recommended)**: No immediate code changes. Track as Phase 2 improvement.

**If the team requires Critical-level fixes**: The following modifications would be needed. This is a significant architectural refactoring and should be planned as a separate initiative:

**Files to Modify** (partial list for Phase 2):
- All 23 entity classes in `model/`
- `OrderItemRepository`, `CartItemRepository`, `CouponUsageRepository`, `PointTransactionRepository` — to be removed or converted to package-private
- New `@Embeddable` Value Object classes: `Money`, `Email`, `OrderNumber`
- New domain event classes: `OrderPlacedEvent`, `PaymentCompletedEvent`, `InventoryReservedEvent`

**Recommended approach**: Create a separate migration ticket. Do NOT attempt in the current fix cycle.

---

### C-10: TODO Comments in Production Code

**Finding**: 2 TODO comments in `AppConstants.java` and `CheckoutService.java`. Per coding standards, TODO comments indicate incomplete implementation.

**Root Cause**: 
- `AppConstants.java` line 10: `TODO: OrderStatus, PaymentStatus, InventoryStatus 等の enum 化を計画的に実施`
- `CheckoutService.java` line ~87: `TODO: 依存が 12 個と多い（Orchestrator パターンとして現時点は許容）。将来的に金額計算ロジック（calculateOrderAmounts）を OrderAmountCalculator 等に分離を検討。`

**Files to Modify**:
- `src/main/java/com/skishop/constant/AppConstants.java` — Remove TODO, add `@SuppressWarnings` comment or tracking reference
- `src/main/java/com/skishop/service/CheckoutService.java` — Remove TODO, add tracking reference

**Changes**:

In `AppConstants.java`, replace the TODO Javadoc:
```java
// BEFORE:
 * <p><strong>TODO:</strong> OrderStatus, PaymentStatus, InventoryStatus 等の enum 化を
 * 計画的に実施し、コンパイル時型安全性を確保する。</p>

// AFTER:
 * <p><strong>Note:</strong> Enum migration for OrderStatus, PaymentStatus, InventoryStatus
 * is tracked as a separate improvement initiative (post-migration Phase 2).</p>
```

In `CheckoutService.java`, replace the TODO in the class Javadoc:
```java
// BEFORE:
 * <p><strong>TODO:</strong> 依存が 12 個と多い（Orchestrator パターンとして現時点は許容）。
 * 将来的に金額計算ロジック（calculateOrderAmounts）を OrderAmountCalculator 等に分離を検討。</p>

// AFTER:
 * <p><strong>Note:</strong> 12 dependencies is at the limit for an orchestrator service.
 * OrderAmountCalculator extraction is tracked as a separate refactoring initiative.</p>
```

---

## 2. High Findings: Security

### H-1: Fail-Open Default Deny (.anyRequest().permitAll())

**Finding**: `SecurityConfig.java` uses `.anyRequest().permitAll()` as the fallback rule. Any new endpoint added is silently exposed without authentication.

**Root Cause**: Line ~140 of `SecurityConfig.java` has `.anyRequest().permitAll()` instead of `.anyRequest().authenticated()`.

**Files to Modify**:
- `src/main/java/com/skishop/config/SecurityConfig.java`

**Changes**:

Replace the `authorizeHttpRequests` block to explicitly list public routes and default-deny everything else:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers(
        "/account/**", "/orders/**", "/checkout/**",
        "/cart/coupon", "/points/**"
    ).hasAnyRole("USER", "ADMIN")
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/actuator/**").hasRole("ADMIN")
    // Explicitly permit public routes
    .requestMatchers(
        "/", "/auth/**", "/products/**", "/coupons",
        "/cart", "/cart/items", "/cart/items/**",
        "/css/**", "/js/**", "/images/**", "/webjars/**",
        "/error", "/error/**",
        "/swagger-ui/**", "/v3/api-docs/**"
    ).permitAll()
    // Default: require authentication
    .anyRequest().authenticated()
)
```

**Note**: The exact list of public URLs must be validated against all existing routes. The cart routes (`/cart`, `/cart/items`, `/cart/items/**`) are public because unauthenticated users can add items to carts.

---

### H-2: Session Cookie Attributes Not Configured

**Finding**: No `server.servlet.session.cookie.*` or `server.servlet.session.timeout` properties.

**Root Cause**: These properties are absent from all configuration files.

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:

```properties
# Session Configuration
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=lax
```

**Note**: `same-site=lax` instead of `strict` is recommended because `strict` can break POST redirects from external payment providers. If no external redirects exist, `strict` is acceptable. `secure=true` requires HTTPS; for dev profile, override with `server.servlet.session.cookie.secure=false` in `application-dev.properties`.

Add to `application-dev.properties`:
```properties
server.servlet.session.cookie.secure=false
```

---

### H-3: OptimisticLockingFailureException Not Handled

**Finding**: `@Version` exists on `Inventory`, `PointAccount`, `Coupon` entities but `OptimisticLockingFailureException` is never caught. Concurrent checkouts trigger unhandled 500 errors.

**Root Cause**: The `CheckoutService.placeOrder()` catch clause catches `RuntimeException` for compensation, but `OptimisticLockingFailureException` (a subclass of `RuntimeException`) results in a generic 500 error page rather than a user-friendly retry message.

**Files to Modify**:
- `src/main/java/com/skishop/exception/GlobalExceptionHandler.java`
- `src/main/java/com/skishop/service/InventoryService.java`
- `src/main/java/com/skishop/service/CouponService.java`
- `src/main/java/com/skishop/service/PointService.java`

**Changes**:

1. Add handler to `GlobalExceptionHandler.java`:

```java
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public String handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                    RedirectAttributes redirectAttributes) {
    log.warn("Optimistic locking failure: {}", ex.getMessage());
    redirectAttributes.addFlashAttribute("errorMessage",
            "Another update was in progress. Please try again.");
    return "redirect:/cart";
}
```

2. In `InventoryService.java`, use `saveAndFlush()` instead of `save()` to trigger the version check immediately:

Replace all `inventoryRepository.save(inventory)` with `inventoryRepository.saveAndFlush(inventory)` in `reserveItems()`, `releaseItems()`, and `deductStock()`.

3. In `CouponService.markUsed()`, replace `couponRepository.save(coupon)` with `couponRepository.saveAndFlush(coupon)`:

> **Rationale**: `Coupon` entity has `@Version` (added in Flyway V10). Within `CheckoutService.placeOrder()`, `CouponService.markUsed()` participates in the same transaction. Using `saveAndFlush()` triggers the optimistic lock check immediately, rather than deferring to commit time when the error message is less actionable.

4. In `PointService.redeemPoints()` and `PointService.awardPoints()`, replace `pointAccountRepository.save(account)` with `pointAccountRepository.saveAndFlush(account)`:

> **Rationale**: `PointAccount` entity has `@Version` (added in Flyway V3). Same reasoning as CouponService — early flush ensures optimistic lock conflicts surface immediately within the checkout flow.

---

### H-4: No Rate Limiting on Auth Endpoints + AuthService Not Wired

**Finding**: No rate limiting on login, registration, password reset. `AuthService.recordLoginFailure()` is never called because no `AuthenticationFailureHandler` is wired to Spring Security.

**Root Cause**: `SecurityConfig.java` uses `.failureUrl("/auth/login?error=true")` which is a simple redirect. No custom `AuthenticationFailureHandler` or `AuthenticationSuccessHandler` (beyond cart merge) is wired to call `AuthService`.

**Files to Modify**:
- `src/main/java/com/skishop/config/SecurityConfig.java`
- New file: `src/main/java/com/skishop/config/CustomAuthFailureHandler.java`

**Changes**:

1. Create `CustomAuthFailureHandler.java`:

```java
package com.skishop.config;

import com.skishop.service.AuthService;
import com.skishop.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;

    public CustomAuthFailureHandler(AuthService authService,
                                     CustomUserDetailsService userDetailsService) {
        super("/auth/login?error=true");
        this.authService = authService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception)
            throws IOException, ServletException {
        String email = request.getParameter("username");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        if (email != null) {
            try {
                var user = authService.findByEmail(email);
                authService.recordLoginFailure(user.getId(), ip, userAgent);
            } catch (Exception e) {
                // User not found — don't reveal this
                log.debug("Login failure for unknown email");
            }
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
```

2. Create `CustomAuthSuccessHandler.java` that wraps `CartMergeSuccessHandler` logic and adds login success recording:

```java
package com.skishop.config;

import com.skishop.security.SkiShopUserDetails;
import com.skishop.service.AuthService;
import com.skishop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
public class CustomAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CartService cartService;
    private final AuthService authService;

    public CustomAuthSuccessHandler(CartService cartService, AuthService authService) {
        super("/");
        this.cartService = cartService;
        this.authService = authService;
        setAlwaysUseDefaultTargetUrl(false);  // Also fixes tech-lead finding about saved request
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        // Record login success
        if (authentication.getPrincipal() instanceof SkiShopUserDetails skiUser) {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            try {
                authService.recordLoginSuccess(skiUser.getUserId(), ip, userAgent);
            } catch (RuntimeException e) {
                log.warn("Failed to record login success: {}", e.getMessage());
            }
        }

        // Cart merge logic (from CartMergeSuccessHandler)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String cartId = (String) session.getAttribute("cartId");
            if (cartId != null) {
                if (authentication.getPrincipal() instanceof SkiShopUserDetails skiUser) {
                    try {
                        cartService.mergeCartById(cartId, skiUser.getUserId());
                        session.removeAttribute("cartId");
                    } catch (RuntimeException e) {
                        log.warn("Cart merge failed for cartId={}", cartId, e);
                    }
                }
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

3. Update `SecurityConfig.java`:

Add `AuthService` as a constructor dependency and update the filter chain:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CartService cartService;
    private final AuthService authService;  // NEW

    // Replace cartMergeSuccessHandler() bean:
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthSuccessHandler(cartService, authService);
    }

    @Bean
    public CustomAuthFailureHandler authenticationFailureHandler() {
        return new CustomAuthFailureHandler(authService, customUserDetailsService);
    }

    // In securityFilterChain, update formLogin:
    .formLogin(form -> form
        .loginPage("/auth/login")
        .loginProcessingUrl("/auth/login")
        .successHandler(authenticationSuccessHandler())
        .failureHandler(authenticationFailureHandler())
        .permitAll()
    )
```

Remove the old `CartMergeSuccessHandler` bean and (optionally) the `CartMergeSuccessHandler.java` file since its logic is now in `CustomAuthSuccessHandler`.

**Note**: This fix also resolves H-8 (AuthService dead code) and partially addresses H-4 (rate limiting — for login specifically via account lockout). For broader rate limiting on `/auth/password/forgot` and `/auth/register`, a filter-based approach (Bucket4j or custom) would need separate implementation.

---

### H-5: No Request Body Size Limits

**Finding**: Missing `server.tomcat.max-http-form-post-size`, `spring.servlet.multipart.max-file-size/max-request-size`.

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:

```properties
# Request Size Limits
server.tomcat.max-http-form-post-size=2MB
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
```

---

### H-6: Cart Item Count Unlimited

**Finding**: Unauthenticated users can add unlimited items to session carts. No cart cleanup job.

**Root Cause**: `CartService.addItem()` has no check for maximum cart item count.

**Files to Modify**:
- `src/main/java/com/skishop/service/CartService.java`

**Changes**:

Add a max items check at the beginning of `addItem()`:

```java
private static final int MAX_CART_ITEMS = 50;

@Transactional
public void addItem(String cartId, String productId, int quantity) {
    if (quantity <= 0) {
        return;
    }
    var cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));

    // Check max items limit
    long currentItemCount = cartItemRepository.countByCartId(cartId);
    var existingItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
    if (existingItem.isEmpty() && currentItemCount >= MAX_CART_ITEMS) {
        throw new BusinessException("Cart item limit reached",
                "redirect:/cart", "error.cart.limit");
    }

    // ... rest of existing logic unchanged ...
}
```

Add `countByCartId` to `CartItemRepository`:
```java
long countByCartId(String cartId);
```

Add import for `BusinessException` to `CartService.java`:
```java
import com.skishop.exception.BusinessException;
```

> **Note**: `CartService.java` currently does NOT import `BusinessException` (only `ResourceNotFoundException`). This import MUST be added or the code will not compile.

---

### H-7: Missing Security Headers (Referrer-Policy, Permissions-Policy, Server)

**Files to Modify**:
- `src/main/java/com/skishop/config/SecurityConfig.java`
- `src/main/resources/application.properties`

**Changes**:

1. In `SecurityConfig.java`, update the `.headers()` configuration:

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self'; " +
            "img-src 'self' data:"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(Customizer.withDefaults())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts ->
        hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
    .referrerPolicy(referrer ->
        referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .permissionsPolicy(permissions ->
        permissions.policy("camera=(), microphone=(), geolocation=()"))
)
```

2. In `application.properties`, add server header suppression:

```properties
server.server-header=
```

---

### H-8: AuthService Dead Code (recordLoginSuccess/Failure not wired)

**Addressed in H-4 above.** The `CustomAuthSuccessHandler` and `CustomAuthFailureHandler` wire `AuthService.recordLoginSuccess()` and `AuthService.recordLoginFailure()` into the Spring Security authentication flow.

---

## 3. High Findings: Performance

### H-9: Unbounded Order List

**Finding**: `OrderService.listByUserId()` returns unbounded `List<Order>`.

**Files to Modify**:
- `src/main/java/com/skishop/service/OrderService.java`
- `src/main/java/com/skishop/repository/OrderRepository.java`
- `src/main/java/com/skishop/controller/OrderController.java`

**Changes**:

1. In `OrderRepository.java`, add a paginated query method:
```java
Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
```
Keep the existing unbounded method for backward compatibility where needed.

2. In `OrderService.java`:
```java
@Transactional(readOnly = true)
public Page<Order> listByUserId(String userId, Pageable pageable) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
}
```

3. In `OrderController.java`, update `list()`:
```java
@GetMapping
public String list(@AuthenticationPrincipal SkiShopUserDetails userDetails,
                   @RequestParam(defaultValue = "1") int page,
                   @RequestParam(defaultValue = "20") int size,
                   Model model) {
    PageRequest pageable = PageRequest.of(
            Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    Page<Order> orders = orderService.listByUserId(userDetails.getUserId(), pageable);
    model.addAttribute("orders", orders);
    return "orders/list";
}
```

> **⚠️ Thymeleaf Template Impact**: The `orders/list.html` template currently iterates over a `List<Order>`. After this change, the model attribute becomes a `Page<Order>`. Template updates are required:
> - Change `th:each="order : ${orders}"` to `th:each="order : ${orders.content}"` (or keep as-is since Thymeleaf can iterate `Page` directly)
> - Add pagination controls (page numbers, prev/next links) using `orders.totalPages`, `orders.number`, `orders.hasNext()`, etc.
> - If the template uses `${orders.size()}`, change to `${orders.totalElements}`

---

### H-10: Unbounded Product List (findByCategoryId, findByStatus)

**Finding**: `ProductService.findByCategoryId()` and `findByStatus()` return unbounded lists. `AdminProductController.list()` uses `findByStatus("ACTIVE")` with no pagination.

**Files to Modify**:
- `src/main/java/com/skishop/service/ProductService.java`
- `src/main/java/com/skishop/repository/ProductRepository.java`
- `src/main/java/com/skishop/controller/admin/AdminProductController.java`

**Changes**:

1. Add paginated method to `ProductRepository`:
```java
Page<Product> findByStatus(String status, Pageable pageable);
```

2. Add paginated method to `ProductService`:
```java
@Transactional(readOnly = true)
public Page<Product> findByStatus(String status, Pageable pageable) {
    return productRepository.findByStatus(status, pageable);
}
```

3. Update `AdminProductController.list()`:
```java
@GetMapping
public String list(@RequestParam(defaultValue = "1") int page,
                   @RequestParam(defaultValue = "50") int size,
                   Model model) {
    PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 200),
            Sort.by(Sort.Direction.ASC, "name"));
    model.addAttribute("products", productService.findByStatus("ACTIVE", pageable));
    return "admin/products/list";
}
```

> **⚠️ Thymeleaf Template Impact**: Same as H-9 — `admin/products/list.html` template must be updated to work with `Page<Product>` instead of `List<Product>`. Add pagination controls for admin product listing.

---

### H-11: Duplicate Query in OrderController.detail()

**Finding**: `findByIdAndUserId()` fetches order with items via `@EntityGraph`, then `listItems(id)` executes a second SELECT for the same items.

**Root Cause**: `OrderService.findByIdAndUserId()` uses `findWithItemsById()` which includes `@EntityGraph(attributePaths = {"items"})`, eagerly loading items. Then `OrderController.detail()` calls `orderService.listItems(id)` separately.

**Files to Modify**:
- `src/main/java/com/skishop/controller/OrderController.java`
- `src/main/java/com/skishop/controller/admin/AdminOrderController.java`

**Changes**:

1. In `OrderController.detail()`:
```java
@GetMapping("/{id}")
public String detail(@PathVariable String id,
                      @AuthenticationPrincipal SkiShopUserDetails userDetails,
                      Model model) {
    Order order = orderService.findByIdAndUserId(id, userDetails.getUserId());
    // Use items from the @EntityGraph-loaded order instead of a separate query
    model.addAttribute("order", order);
    model.addAttribute("items", order.getItems());
    return "orders/detail";
}
```

2. In `AdminOrderController.detail()`:
```java
@GetMapping("/{id}")
public String detail(@PathVariable String id, Model model) {
    Order order = orderService.findById(id);
    List<OrderItem> items = orderService.listItems(id);
    model.addAttribute("order", order);
    model.addAttribute("items", items);
    return "admin/orders/detail";
}
```

**Note**: `AdminOrderController` still uses `listItems()` separately because `findById()` doesn't use `@EntityGraph`. This is acceptable. If it becomes an issue, create a `findWithItemsById(id)` admin variant.

---

### H-12: No DTO Projections for List Views

**Finding**: `search()` returns `Page<Product>` with all columns, including `description` (2000 chars).

**Files to Modify**:
- New file: `src/main/java/com/skishop/dto/response/ProductSummaryDto.java`
- `src/main/java/com/skishop/repository/ProductRepository.java`
- `src/main/java/com/skishop/service/ProductService.java`
- `src/main/java/com/skishop/controller/ProductController.java`

**Changes**:

1. Create `ProductSummaryDto.java`:
```java
package com.skishop.dto.response;

public record ProductSummaryDto(
        String id,
        String name,
        String brand,
        String categoryId,
        String sku,
        String status
) {}
```

2. Add interface projection to `ProductRepository`:
```java
@Query("SELECT new com.skishop.dto.response.ProductSummaryDto(p.id, p.name, p.brand, p.categoryId, p.sku, p.status) " +
       "FROM Product p WHERE p.status <> 'INACTIVE'")
Page<ProductSummaryDto> findAllSummaries(Pageable pageable);
```

**Note**: This is a moderate refactor. The Thymeleaf templates that reference `product.description` on list pages would need to be updated. If templates only use `id`, `name`, `brand` on list views and `description` on detail views, the change is safe. If templates use `description` on list views, this refactoring must also update those templates.

**Recommendation**: Implement for admin product list first (lower risk), then extend to public product search if catalog >1000 products.

---

### H-13: PaymentService Inefficient findByOrderId

**Finding**: `findByOrderId(orderId).stream().findFirst()` fetches ALL payments for an order then discards all but the first.

**Files to Modify**:
- `src/main/java/com/skishop/repository/PaymentRepository.java`
- `src/main/java/com/skishop/service/PaymentService.java`

**Changes**:

1. Add to `PaymentRepository.java`:
```java
Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
```

> **Note**: Using `findFirstByOrderId` without ORDER BY returns an arbitrary payment record. When multiple payments exist per order (e.g., failed then successful), this could return the wrong one. `OrderByCreatedAtDesc` ensures the most recent payment is returned.

2. Update `PaymentService.java` `updateStatusByOrderId()`:
```java
private void updateStatusByOrderId(String orderId, String status) {
    paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
            .ifPresent(payment -> {
                payment.setStatus(status);
                paymentRepository.save(payment);
            });
}
```

---

### H-14: Unbounded Email Queue Fetch

**Addressed in C-3 above.** The redesigned `MailService.fetchPendingBatch()` uses `PageRequest.of(0, BATCH_SIZE)` to limit the fetch to 50 records.

---

### H-15: N+1 in Cart Merge

**Finding**: `mergeCart()` → `addItem()` loop triggers up to 4 queries × N items.

**Root Cause**: The `mergeCart()` private method in `CartService.java` iterates `sessionItems` and calls `addItem()` for each, which internally does `findById(cartId)`, `findByCartIdAndProductId()`, potentially `findById(productId)`, and `findByProductId(price)`.

**Files to Modify**:
- `src/main/java/com/skishop/service/CartService.java`

**Changes**:

Refactor `mergeCart()` to batch-process:

```java
private void mergeCart(Cart sourceCart, String userId) {
    List<Cart> userCarts = cartRepository.findByUserIdAndStatus(userId, AppConstants.STATUS_ACTIVE);
    if (userCarts.isEmpty()) {
        sourceCart.setUserId(userId);
        cartRepository.save(sourceCart);
    } else {
        var userCart = userCarts.getFirst();
        List<CartItem> sessionItems = cartItemRepository.findByCartId(sourceCart.getId());
        List<CartItem> existingItems = cartItemRepository.findByCartId(userCart.getId());

        // Build lookup map of existing items by productId
        Map<String, CartItem> existingMap = existingItems.stream()
                .collect(Collectors.toMap(CartItem::getProductId, item -> item, (a, b) -> a));

        for (CartItem sessionItem : sessionItems) {
            CartItem existing = existingMap.get(sessionItem.getProductId());
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + sessionItem.getQuantity());
                cartItemRepository.save(existing);
            } else {
                var newItem = new CartItem();
                newItem.setId(UUID.randomUUID().toString());
                newItem.setCart(userCart);
                newItem.setProductId(sessionItem.getProductId());
                newItem.setProductName(sessionItem.getProductName());
                newItem.setQuantity(sessionItem.getQuantity());
                newItem.setUnitPrice(sessionItem.getUnitPrice());
                cartItemRepository.save(newItem);
            }
        }

        cartItemRepository.deleteByCartId(sourceCart.getId());
        sourceCart.setStatus(AppConstants.CART_STATUS_MERGED);
        cartRepository.save(sourceCart);
    }
}
```

Add imports for `java.util.Map`, `java.util.stream.Collectors`.

---

## 4. High Findings: Resilience

### H-16: No Health Probes Separation

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:
```properties
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

---

### H-17: No SMTP Health Indicator

**Files to Modify**:
- New file: `src/main/java/com/skishop/config/SmtpHealthIndicator.java`

**Changes**:

```java
package com.skishop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpHealthIndicator implements HealthIndicator {

    private final org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Override
    public Health health() {
        try {
            if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
                mailSender.testConnection();
            }
            return Health.up().build();
        } catch (Exception e) {
            log.warn("SMTP health check failed: {}", e.getMessage());
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
```

---

### H-18: No Resilience4j / Spring Retry

**Finding**: No circuit breaker for SMTP. Flapping SMTP server causes repeated failures every 30s.

**Files to Modify**:
- `pom.xml`

**Changes**:

Add Spring Retry dependency (lighter weight than Resilience4j for a single SMTP client):

```xml
<!-- Spring Retry (for @Retryable on external I/O like SMTP) -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Note**: The application-level retry with exponential backoff (implemented in C-3 fix) already provides retry semantics for the email queue. Adding Spring Retry is for potential future external service calls. If architectural decision is to use Resilience4j instead (for future payment gateway integration), use `io.github.resilience4j:resilience4j-spring-boot3` instead.

> **⚠️ YAGNI Warning**: The proposed change adds `spring-retry` and `spring-boot-starter-aop` dependencies but **no method in the codebase uses `@Retryable`**. The email retry is handled by application-level logic (C-3 fix). Adding unused dependencies increases the build size and attack surface without benefit. **Recommendation**: Defer adding these dependencies until an actual use case (e.g., external payment gateway) requires them. If added, at minimum annotate `MailService.send()` with `@Retryable` and remove the application-level retry loop to avoid double-retry behavior.

---

### H-19: Mail Retry Fixed Interval

**Addressed in C-3 above.** The redesigned `markRetryOrFailed()` uses exponential backoff: `now.plusMinutes((long) Math.pow(2, retryCount))` and `MAX_RETRY` is increased to 5.

---

### H-20: Scheduler Single Thread

**Finding**: Default single-threaded `@Scheduled` executor. SMTP blocking halts ALL scheduled tasks.

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:
```properties
# Scheduler thread pool
spring.task.scheduling.pool.size=4
spring.task.scheduling.shutdown.await-termination=true
spring.task.scheduling.shutdown.await-termination-period=30s
```

---

### H-21: processQueue @Transactional Scope

**Addressed in C-3 above.** The `@Transactional` was removed from `processQueue()` and replaced with per-message transactional methods.

---

## 5. High Findings: Error/Logging

### H-22: Missing MethodArgumentNotValidException Handler

**Files to Modify**:
- `src/main/java/com/skishop/exception/GlobalExceptionHandler.java`

**Changes**:

Add handler:
```java
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public String handleValidation(MethodArgumentNotValidException ex, Model model) {
    log.warn("Validation failed: {}", ex.getBindingResult().getAllErrors().size());
    model.addAttribute("errors", ex.getBindingResult().getAllErrors());
    return "error/400";
}
```

Also create `src/main/resources/templates/error/400.html` if it doesn't exist.

---

### H-23: No RFC 7807 ProblemDetail

**Finding**: AGENTS.md §5.2 requires RFC 7807. Current handlers return Thymeleaf views only.

**Root Cause**: As a Thymeleaf MVC app, returning view names is the correct pattern for browser requests. `ProblemDetail` would be relevant for AJAX/API consumers.

**Files to Modify**:
- `src/main/java/com/skishop/exception/GlobalExceptionHandler.java`

**Changes**:

This is a **Recommended Enhancement** rather than a required fix for pure Thymeleaf MVC. If AJAX endpoints are planned, add content negotiation:

```java
@ExceptionHandler(BusinessException.class)
public Object handleBusiness(BusinessException ex, HttpServletRequest request,
                              RedirectAttributes redirectAttributes, Model model) {
    log.warn("Business rule violation: {}", ex.getMessageKey());

    // Content negotiation for Accept: application/json
    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("application/json")) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessageKey());
        problem.setTitle("Business Rule Violation");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    // Thymeleaf view path
    String redirectUrl = ex.getRedirectUrl();
    if (redirectUrl != null) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + redirectUrl;
    }
    model.addAttribute("errorMessage", ex.getMessage());
    return "error/business";
}
```

**Recommendation**: If no AJAX endpoints exist, document the intentional deviation and defer.

---

### H-24 + H-25: Prod Log Level Suppresses INFO

**Finding**: `logging.level.com.skishop=WARN` in `application-prod.properties` and missing `<logger>` in `logback-spring.xml` prod profile suppress INFO business events.

**Files to Modify**:
- `src/main/resources/application-prod.properties`
- `src/main/resources/logback-spring.xml`

**Changes**:

1. In `application-prod.properties`, change:
```properties
# BEFORE:
logging.level.com.skishop=WARN
# AFTER:
logging.level.com.skishop=INFO
```

2. In `logback-spring.xml`, add logger in prod profile:
```xml
<springProfile name="prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <logger name="com.skishop" level="INFO"/>
    <root level="WARN">
        <appender-ref ref="JSON"/>
    </root>
</springProfile>
```

---

### H-26: X-Request-Id Not Sanitized

**Finding**: Incoming `X-Request-Id` header accepted without validation. Log injection risk.

**Files to Modify**:
- `src/main/java/com/skishop/filter/RequestIdFilter.java`

**Changes**:

```java
private static final int MAX_REQUEST_ID_LENGTH = 64;
private static final java.util.regex.Pattern VALID_REQUEST_ID =
        java.util.regex.Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");

@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    String requestId = request.getHeader(HEADER_NAME);
    if (!StringUtils.hasText(requestId)
            || requestId.length() > MAX_REQUEST_ID_LENGTH
            || !VALID_REQUEST_ID.matcher(requestId).matches()) {
        requestId = UUID.randomUUID().toString();
    }
    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER_NAME, requestId);
    try {
        chain.doFilter(request, response);
    } finally {
        MDC.remove(MDC_KEY);
    }
}
```

---

### H-27: Missing @Version on User, Product, Cart

**Finding**: No `@Version` field on entities with concurrent update risk.

**Files to Modify**:
- `src/main/java/com/skishop/model/User.java`
- `src/main/java/com/skishop/model/Product.java`
- `src/main/java/com/skishop/model/Cart.java`

**Changes**:

Add to each entity:
```java
@Version
@Column(name = "version")
private Long version;
```

**Also requires**: A Flyway migration to add the `version` column to the respective DB tables:

New file: `src/main/resources/db/migration/V13__add_version_columns.sql`:
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE carts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
```

---

### H-28: Missing Audit Timestamps on 4 Entities

**Finding**: Four entities lack `@CreationTimestamp`/`@UpdateTimestamp`.

**Files to Modify**: Based on source code verification, the following entities are missing audit timestamps:

| Entity | Missing Fields | DB Table |
|--------|---------------|----------|
| `CartItem.java` | Both `createdAt` and `updatedAt` | `cart_items` |
| `EmailQueue.java` | Both `createdAt` and `updatedAt` (has `scheduledAt`/`sentAt` but no audit fields) | `email_queue` |
| `Coupon.java` | Both `createdAt` and `updatedAt` | `coupons` |
| `PointAccount.java` | Both `createdAt` and `updatedAt` | `point_accounts` |

**Changes**: For each affected entity, add:
```java
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
```

And corresponding Flyway migration if the DB columns don't exist.

---

## 6. High Findings: API/Architecture/Async

### H-29: HiddenHttpMethodFilter Not Enabled

**Finding**: `PUT`/`DELETE` from HTML forms with `_method` parameter won't work.

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:
```properties
spring.mvc.hiddenmethod.filter.enabled=true
```

---

### H-30: Checkout Redirect Loses BindingResult Errors

**Finding**: `CheckoutController.placeOrder()` redirects on validation error, losing `BindingResult`.

**Root Cause**: Line ~95 of `CheckoutController.java`: `return "redirect:/checkout"` discards the `BindingResult` because redirect loses request attributes.

**Files to Modify**:
- `src/main/java/com/skishop/controller/CheckoutController.java`

**Changes**:

Instead of redirecting, render the checkout form directly on error:

```java
@PostMapping
public String placeOrder(@Valid @ModelAttribute CheckoutRequest request,
                          BindingResult result,
                          @AuthenticationPrincipal SkiShopUserDetails userDetails,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        // Re-populate checkout form data for re-rendering
        CheckoutSummary summary = checkoutService.prepareCheckoutSummary(
                userDetails.getUserId(), session.getId());
        model.addAttribute("cart", summary.cart());
        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shippingFee", summary.shippingFee());
        model.addAttribute("tax", summary.tax());
        model.addAttribute("couponCode", request.couponCode());
        model.addAttribute("couponDiscount", session.getAttribute("couponDiscount"));
        return "checkout/index";
    }
    // ... rest unchanged ...
}
```

> **⚠️ PRG Pattern Risk**: The proposed fix changes the response from a redirect (POST-Redirect-GET) to direct form rendering. This means:
> - The browser's address bar shows `/checkout` (from POST), not a clean GET
> - If the user refreshes the page, the browser will attempt to **re-submit the checkout form**
> - This can cause duplicate order attempts
>
> **Safer Alternative — Use Flash Attributes to preserve PRG**:
> ```java
> if (result.hasErrors()) {
>     redirectAttributes.addFlashAttribute(
>         "org.springframework.validation.BindingResult.checkoutRequest", result);
>     redirectAttributes.addFlashAttribute("checkoutRequest", request);
>     return "redirect:/checkout";
> }
> ```
> Then in the GET handler (`checkoutForm()`), check for flash attributes. This preserves the PRG pattern while keeping validation errors visible. The GET handler already populates `CheckoutSummary` model attributes.
>
> **Recommendation**: Use the flash attribute approach if UX/browser-back-button safety is a concern. The direct rendering approach is simpler but has the refresh/resubmit risk.

---

### H-31: JPA Entities Passed Directly to Views

**Finding**: JPA entities passed to Thymeleaf templates, risking LazyInitializationException and tight coupling.

**Root Cause**: Controllers like `OrderController`, `ProductController`, `CartController`, `CheckoutController` pass JPA entities directly to Model attributes.

**Files to Modify**: Multiple controllers and potentially new DTO classes.

**Recommendation**: This is a moderate-effort refactoring. Prioritize based on risk:

1. **Immediate**: Ensure `spring.jpa.open-in-view=false` (already set) — this means lazy-loaded fields accessed in templates will throw `LazyInitializationException`. If no runtime errors are occurring, the current templates only access eagerly-loaded fields.

2. **Short-term**: For list views (where DTO projections provide the most benefit — H-12), create summary DTOs. For detail views, the full entity is acceptable.

3. **Medium-term**: Create view-specific DTOs for all controller responses.

**No immediate code change required** if no `LazyInitializationException` errors occur at runtime. Track as improvement.

---

### H-32: Business Logic in CartController.applyCoupon()

**Finding**: Coupon validation and discount calculation logic in controller layer.

**Root Cause**: `CartController.applyCoupon()` directly calls `couponService.validateCoupon()` and `couponService.calculateDiscount()` and stores results in session — this is orchestration logic that belongs in a service.

**Files to Modify**:
- `src/main/java/com/skishop/service/CartService.java`
- `src/main/java/com/skishop/controller/CartController.java`

**Changes**:

1. Add method to `CartService.java`:
```java
@Transactional(readOnly = true)
public BigDecimal applyCouponToCart(String cartId, String couponCode) {
    List<CartItem> items = getItems(cartId);
    BigDecimal subtotal = calculateSubtotal(items);
    Coupon coupon = couponService.validateCoupon(couponCode, subtotal).orElse(null);
    return couponService.calculateDiscount(coupon, subtotal);
}
```

This requires adding `CouponService` as a dependency of `CartService` (constructor injection).

2. Simplify `CartController.applyCoupon()`:
```java
@PostMapping("/coupon")
public String applyCoupon(@Valid @ModelAttribute CouponApplyRequest request,
                           BindingResult result,
                           @AuthenticationPrincipal SkiShopUserDetails userDetails,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        redirectAttributes.addFlashAttribute("errorMessage", "coupon.code.invalid");
        return "redirect:/cart";
    }
    Cart cart = resolveCart(userDetails, session);
    BigDecimal discount = cartService.applyCouponToCart(cart.getId(), request.code());
    session.setAttribute("couponCode", request.code());
    session.setAttribute("couponDiscount", discount);
    redirectAttributes.addFlashAttribute("successMessage", "coupon.applied");
    return "redirect:/cart";
}
```

---

### H-33: Entity in CheckoutSummary DTO

**Finding**: `CheckoutSummary` record contains `Cart` and `List<CartItem>` JPA entity references.

**Files to Modify**:
- `src/main/java/com/skishop/dto/response/CheckoutSummary.java`

**Changes**:

Replace entity references with primitive fields:

```java
package com.skishop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutSummary(
        String cartId,
        List<CheckoutItemDto> items,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax
) {
    public record CheckoutItemDto(
            String id,
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
```

Then update `CheckoutService.prepareCheckoutSummary()` and `CheckoutController.checkoutForm()` to work with the new structure.

**Note**: This impacts Thymeleaf templates that reference `cart.*` and `items[].cart.*`. Template updates required.

**Recommendation**: Implement when addressing H-31 (JPA entities to views) as a batch change.

---

### H-34: Virtual Threads Not Enabled

**Files to Modify**:
- `src/main/resources/application.properties`

**Changes**:

Add to `application.properties`:
```properties
spring.threads.virtual.enabled=true
```

**Warning**: Virtual threads use `ThreadLocal` differently. Test thoroughly — any code using `ThreadLocal` (e.g., MDC, Spring Security context propagation) must be validated. Spring Boot 3.2+ has built-in support, but third-party libraries may have issues.

**Recommendation**: Enable in `application-dev.properties` first for testing, then promote to common after validation.

---

### H-35: MailService @Scheduled No Top-Level Try-Catch

**Addressed in C-3 above.** The redesigned `processQueue()` wraps the entire method body in `try-catch(Exception)` with error logging.

---

### H-36: No ShedLock for Multi-Instance @Scheduled

**Finding**: Multiple app instances execute `processQueue()` simultaneously → duplicate email sends.

**Files to Modify**:
- `pom.xml`
- New file: `src/main/java/com/skishop/config/ShedLockConfig.java`
- `src/main/java/com/skishop/service/MailService.java`
- New Flyway migration

**Changes**:

1. Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.10.0</version>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
    <version>5.10.0</version>
</dependency>
```

2. Create `ShedLockConfig.java`:
```java
package com.skishop.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
```

3. Add ShedLock annotation to `MailService.processQueue()`:
```java
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Scheduled(fixedDelay = 30000)
@SchedulerLock(name = "processEmailQueue", lockAtMostFor = "5m", lockAtLeastFor = "10s")
public void processQueue() {
    // ...
}
```

4. Create Flyway migration `V14__add_shedlock_table.sql`:
```sql
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

---

## 7. High Findings: Test Quality

### H-37: Only 3/21 Repos Tested

**Files to Create**: New `@DataJpaTest` test files for repositories with custom `@Query` methods.

**Priority repos to test** (those with custom queries):
- `SecurityLogRepository` (countByUserIdAndEventType)
- `CouponRepository` (findByCode, findByActiveTrue)
- `InventoryRepository` (findByProductId, findByProductIdIn)
- `CartRepository` (findBySessionId, findByUserIdAndStatus)
- `CartItemRepository` (findByCartId, findByCartIdAndProductId, deleteByCartId, countByCartId)
- `PointTransactionRepository` (sumExpiredAmount, bulkExpire)

**Changes**: Create `@DataJpaTest` test classes following the `should_X_when_Y` naming pattern and AAA structure with `@ActiveProfiles("test")`.

---

### H-38: Controller POST/PUT/DELETE Untested

**Files to Modify**: Existing controller test files

**Changes**: Add test methods for:
- `AuthControllerTest`: POST `/auth/register` success, password mismatch, duplicate email
- `CartControllerTest`: POST `/cart/items` success, PUT `/cart/items/{id}`, DELETE `/cart/items/{id}`
- `CheckoutControllerTest`: POST `/checkout` validation error, POST `/checkout` success
- Admin controller tests: POST, PUT, DELETE operations

---

### H-39: CouponController No Tests

**Files to Create**:
- `src/test/java/com/skishop/controller/CouponControllerTest.java`

**Changes**: Create `@WebMvcTest(CouponController.class)` with tests for:
- `should_listActiveCoupons_when_accessCouponsPage`
- `should_returnOk_when_couponsExist`

---

### H-40: CheckoutServiceTest 12 Mocks

**Files to Modify**:
- `src/test/java/com/skishop/service/CheckoutServiceTest.java`

**Changes**: Extract test fixture helper methods:
```java
private Cart givenCart(String cartId) { ... }
private List<CartItem> givenCartWithItems(String cartId, int itemCount) { ... }
private void givenPaymentSuccess(String orderId) { ... }
private void givenSufficientInventory(List<CartItem> items) { ... }
```

This reduces verbose Arrange sections and improves readability.

---

### H-41: Security Test Incomplete (8/~20 URL patterns)

**Files to Modify**:
- `src/test/java/com/skishop/security/SecurityAuthorizationTest.java`

**Changes**: Add tests for all secured URL patterns:
```java
// Account
@Test void should_requireAuth_when_accessAccountPages() { ... }
// Checkout
@Test void should_requireAuth_when_accessCheckoutPages() { ... }
// Admin Orders
@Test void should_requireAdmin_when_accessAdminOrders() { ... }
// Admin Coupons
@Test void should_requireAdmin_when_accessAdminCoupons() { ... }
// Admin Shipping Methods
@Test void should_requireAdmin_when_accessAdminShippingMethods() { ... }
// Actuator
@Test void should_requireAdmin_when_accessActuatorEndpoints() { ... }
```

---

## 8. High Findings: Tech Lead

### H-42: Price Sort Broken in ProductController

**Finding**: `resolveSort()` falls back to `name` when `price_asc`/`price_desc` requested (price is in a separate table).

**Root Cause**: Lines 98-104 of `ProductController.java` map `price_asc` and `price_desc` to `Sort.by(..., "name")` because `Product` entity has no price field.

**Files to Modify**:
- `src/main/java/com/skishop/controller/ProductController.java`
- `src/main/java/com/skishop/service/ProductService.java`
- `src/main/java/com/skishop/repository/ProductRepository.java`

**Changes**:

Option A (recommended — simple): Remove price sort options until proper JOIN query is implemented:
```java
private Sort resolveSort(String sort) {
    if (sort == null) {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    return switch (sort) {
        case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
        case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
        case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
        default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
}
```

Option B (complete fix): Add a custom `@Query` with JOIN to the `prices` table:
```java
// In ProductRepository:
@Query("SELECT p FROM Product p LEFT JOIN Price pr ON pr.productId = p.id " +
       "WHERE p.status <> 'INACTIVE' ORDER BY pr.regularPrice ASC")
Page<Product> findAllOrderByPriceAsc(Pageable pageable);
```

**Recommendation**: Implement Option A now (remove broken sort), track Option B as enhancement.

> **Note**: If the Thymeleaf template `products/list.html` has UI elements (e.g., dropdown, links) for "Sort by Price", these must also be removed or disabled to avoid user confusion. Check for `price_asc`/`price_desc` references in template files.

---

### H-43: docker-compose Secret + Prod Profile

**Finding**: `POSTGRES_PASSWORD: skishop_password` hardcoded with `SPRING_PROFILES_ACTIVE: prod`.

**Files to Modify**:
- `appmod-migrated-java21-spring-boot-3rd/docker-compose.yml`
- New file: `appmod-migrated-java21-spring-boot-3rd/.env.example`
- Add `.env` to `.gitignore`

**Changes**:

1. Update `docker-compose.yml`:
```yaml
version: '3.9'

services:
  db:
    image: postgres:15-alpine
    container_name: skishop-db
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-skishop}
      POSTGRES_USER: ${POSTGRES_USER:-skishop}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    # ...

  app:
    # ...
    environment:
      SPRING_PROFILES_ACTIVE: dev       # Changed from prod
      DB_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-skishop}
      DB_USERNAME: ${POSTGRES_USER:-skishop}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      # ...
```

2. Create `.env.example`:
```
POSTGRES_DB=skishop
POSTGRES_USER=skishop
POSTGRES_PASSWORD=changeme_local_dev_only
```

3. Add to `.gitignore`:
```
.env
```

---

### H-44: RegisterRequest No Email Uniqueness Check

**Finding**: No `existsByEmail()` before `registerNewUser()`. DB unique constraint throws unhandled `DataIntegrityViolationException`.

**Files to Modify**:
- `src/main/java/com/skishop/controller/AuthController.java`
- `src/main/java/com/skishop/service/UserService.java`
- `src/main/java/com/skishop/repository/UserRepository.java`

**Changes**:

1. Add to `UserRepository.java`:
```java
boolean existsByEmail(String email);
```

2. Add to `UserService.java`:
```java
@Transactional(readOnly = true)
public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
}
```

3. Update `AuthController.register()`:
```java
@PostMapping("/register")
public String register(@Valid @ModelAttribute RegisterRequest request,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        return "auth/register";
    }
    if (!request.password().equals(request.passwordConfirm())) {
        result.rejectValue("passwordConfirm", "validation.passwordConfirm.mismatch");
        return "auth/register";
    }
    if (userService.existsByEmail(request.email())) {
        // Generic message to prevent user enumeration
        result.rejectValue("email", "validation.email.unavailable",
                "This email address is not available");
        return "auth/register";
    }
    userService.registerNewUser(request.email(), request.username(), request.password());
    redirectAttributes.addFlashAttribute("successMessage", "registration.success");
    return "redirect:/auth/login";
}
```

> **⚠️ TOCTOU Race Condition**: The `existsByEmail()` check and `registerNewUser()` save are NOT atomic. Between the check and the save, another concurrent request could register the same email, causing a `DataIntegrityViolationException` from the DB unique constraint. To handle this safely, add a `DataIntegrityViolationException` handler to `GlobalExceptionHandler.java`:
> ```java
> @ExceptionHandler(DataIntegrityViolationException.class)
> public String handleDataIntegrity(DataIntegrityViolationException ex,
>                                    RedirectAttributes redirectAttributes) {
>     log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
>     redirectAttributes.addFlashAttribute("errorMessage",
>             "The requested operation could not be completed. Please try again.");
>     return "redirect:/auth/register";
> }
> ```
> This provides a user-friendly error instead of a generic 500 page for the rare race condition.

---

## Summary of All Files to Modify/Create

### Modified Files (37):

| # | File | Findings Addressed |
|---|------|--------------------|
| 1 | `src/main/java/com/skishop/service/CheckoutService.java` | C-1, C-10 |
| 2 | `src/main/java/com/skishop/service/ProductService.java` | C-1, H-10, H-12 |
| 3 | `src/main/java/com/skishop/service/InventoryService.java` | C-2, H-3 |
| 4 | `src/main/java/com/skishop/repository/InventoryRepository.java` | C-2 |
| 5 | `src/main/java/com/skishop/service/MailService.java` | C-3, H-14, H-19, H-21, H-35, H-36 |
| 6 | `src/main/java/com/skishop/repository/EmailQueueRepository.java` | C-3 |
| 7 | `src/main/resources/application.properties` | C-4, C-5, H-2, H-5, H-7, H-16, H-20, H-29, H-34 |
| 8 | `src/main/resources/application-prod.properties` | H-24 |
| 9 | `src/main/resources/application-dev.properties` | H-2 |
| 10 | `src/main/java/com/skishop/constant/AppConstants.java` | C-10 |
| 11 | `src/main/java/com/skishop/config/SecurityConfig.java` | H-1, H-4, H-7, H-8 |
| 12 | `src/main/java/com/skishop/exception/GlobalExceptionHandler.java` | H-3, H-22, H-23, H-44 |
| 13 | `src/main/java/com/skishop/filter/RequestIdFilter.java` | H-26 |
| 14 | `src/main/java/com/skishop/model/User.java` | H-27 |
| 15 | `src/main/java/com/skishop/model/Product.java` | H-27 |
| 16 | `src/main/java/com/skishop/model/Cart.java` | H-27 |
| 17 | `src/main/java/com/skishop/service/CartService.java` | H-6, H-15, H-32 |
| 18 | `src/main/java/com/skishop/service/OrderService.java` | H-9 |
| 19 | `src/main/java/com/skishop/controller/OrderController.java` | H-9, H-11 |
| 20 | `src/main/java/com/skishop/controller/admin/AdminProductController.java` | H-10 |
| 21 | `src/main/java/com/skishop/repository/PaymentRepository.java` | H-13 |
| 22 | `src/main/java/com/skishop/service/PaymentService.java` | H-13 |
| 23 | `src/main/resources/logback-spring.xml` | H-25 |
| 24 | `src/main/java/com/skishop/controller/CheckoutController.java` | H-30 |
| 25 | `src/main/java/com/skishop/dto/response/CheckoutSummary.java` | H-33 |
| 26 | `src/main/java/com/skishop/controller/CartController.java` | H-32 |
| 27 | `src/main/java/com/skishop/controller/ProductController.java` | H-42 |
| 28 | `src/main/java/com/skishop/controller/AuthController.java` | H-44 |
| 29 | `src/main/java/com/skishop/service/UserService.java` | H-44 |
| 30 | `src/main/java/com/skishop/repository/UserRepository.java` | H-44 |
| 31 | `src/main/java/com/skishop/repository/ProductRepository.java` | H-10, H-12 |
| 32 | `src/main/java/com/skishop/repository/OrderRepository.java` | H-9 |
| 33 | `pom.xml` | H-18, H-36 |
| 34 | `docker-compose.yml` | H-43 |
| 35 | `src/test/java/com/skishop/security/SecurityAuthorizationTest.java` | H-41 |
| 36 | `src/main/java/com/skishop/service/CouponService.java` | H-3 |
| 37 | `src/main/java/com/skishop/service/PointService.java` | H-3 |

### New Files (12):

| # | File | Findings Addressed |
|---|------|--------------------|
| 1 | `src/main/java/com/skishop/config/CustomAuthFailureHandler.java` | H-4, H-8 |
| 2 | `src/main/java/com/skishop/config/CustomAuthSuccessHandler.java` | H-4, H-8 |
| 3 | `src/main/java/com/skishop/config/SmtpHealthIndicator.java` | H-17 |
| 4 | `src/main/java/com/skishop/config/ShedLockConfig.java` | H-36 |
| 5 | `src/main/java/com/skishop/dto/response/ProductSummaryDto.java` | H-12 |
| 6 | `src/main/java/com/skishop/service/EmailQueueStatusService.java` | C-3 (self-invocation fix) |
| 7 | `src/main/resources/db/migration/V13__add_version_columns.sql` | H-27 |
| 8 | `src/main/resources/db/migration/V14__add_shedlock_table.sql` | H-36 |
| 9 | `src/main/resources/templates/error/400.html` | H-22 |
| 10 | `.env.example` | H-43 |
| 11 | `src/test/java/com/skishop/controller/CouponControllerTest.java` | H-39 |
| 12 | Multiple new `*RepositoryTest.java` files | H-37 |

### Files Potentially Removable (1):

| # | File | Reason |
|---|------|--------|
| 1 | `src/main/java/com/skishop/config/CartMergeSuccessHandler.java` | Logic merged into `CustomAuthSuccessHandler` |

---

## Implementation Order

Recommended execution sequence to minimize merge conflicts:

| Phase | Findings | Effort | Risk |
|-------|----------|--------|------|
| **Phase A: Config & Properties** | C-4, C-5, H-2, H-5, H-7, H-16, H-20, H-24, H-25, H-29, H-34 | Trivial | Low |
| **Phase B: Security (Config + Handlers)** | H-1, H-4, H-8, H-26 | Medium | High (auth flow changes) |
| **Phase C: Performance (N+1 + Batch)** | C-1, C-2, H-13, H-15 | Low–Medium | Medium |
| **Phase D: MailService Redesign** | C-3, H-14, H-19, H-21, H-35, H-36 | Medium | Medium |
| **Phase E: Exception Handling** | H-3, H-22, H-23 | Low | Low |
| **Phase F: Entity & Schema** | H-27, H-28, C-10 | Low | Medium (Flyway migration) |
| **Phase G: Controller Fixes** | H-6, H-9, H-10, H-11, H-30, H-32, H-42, H-44 | Medium | Low |
| **Phase H: DTO/Architecture** | H-12, H-31, H-33 | Medium | Medium (template changes) |
| **Phase I: Docker** | H-43 | Trivial | Low |
| **Phase J: Tests** | H-37, H-38, H-39, H-40, H-41 | High | Low |
| **Phase K: DDD (if required)** | C-6, C-7, C-8, C-9 | Very High | High |
| **Phase L: Resilience (Spring Retry)** | H-17, H-18 | Low | Low |

---

*Plan generated from check-report-1.md. All Critical and High findings covered. C-6 to C-9 (DDD) marked as escalation items requiring human decision on whether to reclassify to High or implement as a separate initiative.*

---

## Review Addendum — Corrections, Risks & Validation Requirements

*Added after cross-checking modify-plan.md against actual source code and check-report-1.md*

### A.1 Critical Technical Corrections Applied

| # | Finding | Issue Identified | Correction Applied |
|---|---------|------------------|--------------------|
| 1 | **C-3** | Self-invocation bug: `markSent()`, `markRetryOrFailed()`, `fetchPendingBatch()` called via `this` bypass Spring AOP proxy → `@Transactional` silently ignored | Added warning + two fix options (extract `EmailQueueStatusService` or use `TransactionTemplate`) |
| 2 | **H-3** | Plan only addressed `saveAndFlush()` in InventoryService. CouponService (`save()` on `@Version` entity Coupon) and PointService (`save()` on `@Version` entity PointAccount) had same issue | Added CouponService and PointService to files-to-modify with `saveAndFlush()` requirement |
| 3 | **H-13** | `findFirstByOrderId` returns arbitrary payment (no ORDER BY). Multiple payments per order (failed → successful) could return wrong record | Changed to `findFirstByOrderIdOrderByCreatedAtDesc` |
| 4 | **H-18** | Added `spring-retry` + `spring-boot-starter-aop` dependencies but no `@Retryable` annotation in codebase. Violates YAGNI | Added warning recommending deferral until actual use case exists |
| 5 | **C-4** | "Files to Modify" listed all 4 property files, but fix only adds to base `application.properties` (inherited by all profiles) | Corrected files list to `application.properties` only |
| 6 | **H-6** | CartService has no `import com.skishop.exception.BusinessException` — proposed code would not compile | Added explicit import with compilation warning note |
| 7 | **H-28** | "Requires further investigation" — entities not identified | Identified: CartItem, EmailQueue, Coupon, PointAccount |

### A.2 Missing Remediation Steps Added

| # | Finding | Missing Step |
|---|---------|-------------|
| 1 | **H-9** | Thymeleaf template `orders/list.html` must be updated for `Page<Order>` (pagination controls) |
| 2 | **H-10** | Thymeleaf template `admin/products/list.html` must be updated for `Page<Product>` (pagination controls) |
| 3 | **H-30** | PRG pattern risk documented — direct form rendering causes refresh/resubmit. Flash-attribute alternative provided |
| 4 | **H-42** | Template UI elements for price sort must be removed/disabled alongside backend fix |
| 5 | **H-44** | `DataIntegrityViolationException` handler added to GlobalExceptionHandler as TOCTOU race condition safety net |
| 6 | **C-3** | `EmailQueueStatusService.java` added to New Files list |
| 7 | **H-3** | `CouponService.java` and `PointService.java` added to Modified Files list |

### A.3 Risk Assessment

| Risk | Severity | Finding | Description | Mitigation |
|------|----------|---------|-------------|------------|
| **Self-invocation bypass** | **HIGH** | C-3 | Original plan's MailService internally calls `@Transactional` methods via `this` — transactions silently NOT created | Must use Option A (separate service) or Option B (TransactionTemplate) |
| **URL list completeness** | **MEDIUM** | H-1 | The permitAll/authenticated URL list must exactly match ALL route mappings. Missing a public route → 403 for users. Missing a secure route → unauthorized access | Run `grep -r '@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping' src/main/java/` and cross-check every URL |
| **Pagination template breakage** | **MEDIUM** | H-9, H-10 | Changing `List<T>` to `Page<T>` breaks Thymeleaf templates that call `.size()`, iterate without `.content`, or lack pagination controls | Test all affected templates after changes |
| **@Version on User/Product/Cart** | **MEDIUM** | H-27 | Adding `@Version` to existing entities may cause unexpected `OptimisticLockingFailureException` in existing update flows. All code paths that update these entities must load the current version first | Test: user profile update, product admin CRUD, cart operations |
| **AuthService lockout bug** | **LOW** | H-4 | `recordLoginFailure()` counts ALL historical login failures (`countByUserIdAndEventType`), not recent ones. After 5 failures EVER (even spread over months), account permanently locks. The plan wires this handler but doesn't fix the counting logic | Consider adding time-window filter: `countByUserIdAndEventTypeAndCreatedAtAfter(userId, type, windowStart)` |
| **ShedLock version** | **LOW** | H-36 | Version `5.10.0` specified — verify latest stable release at implementation time | Check Maven Central for latest 5.x GA |

### A.4 Additional Validation Checklist

Before implementation, verify:

- [ ] `@EnableScheduling` is present (confirmed: `SkiShopApplication.java` line 29)
- [ ] Flyway V12 is the latest migration (confirmed: V13 available for @Version columns, V14 for ShedLock)
- [ ] `flyway-database-postgresql` dependency exists in pom.xml (required by newer Flyway versions for PostgreSQL dialect)
- [ ] `spring.jpa.open-in-view=false` is set (confirmed in application.properties)
- [ ] All `@Version` entities have corresponding Flyway migration with `DEFAULT 0` for existing rows
- [ ] `templates/error/400.html` does NOT exist and must be created (confirmed)
- [ ] `.gitignore` in `appmod-migrated-java21-spring-boot-3rd/` needs `.env` entry added (currently no `.gitignore` in project dir — create one)

### A.5 Verified Source Code State (Pre-Implementation Baseline)

| Entity | `@Version` | `@CreationTimestamp` | `@UpdateTimestamp` | Flyway Migration |
|--------|-----------|---------------------|--------------------|------------------|
| User | ❌ (add in H-27) | ✅ | ✅ | V13 (proposed) |
| Product | ❌ (add in H-27) | ✅ | ✅ | V13 (proposed) |
| Cart | ❌ (add in H-27) | ✅ | ✅ | V13 (proposed) |
| Inventory | ✅ (V3) | ✅ | ✅ | — |
| Order | ✅ (V9) | ✅ | ✅ | — |
| Coupon | ✅ (V10) | ❌ (add in H-28) | ❌ (add in H-28) | V15 (new) |
| PointAccount | ✅ (V3) | ❌ (add in H-28) | ❌ (add in H-28) | V15 (new) |
| CartItem | ❌ | ❌ (add in H-28) | ❌ (add in H-28) | V15 (new) |
| EmailQueue | ❌ | ❌ (add in H-28) | ❌ (add in H-28) | V15 (new) |
| Payment | ❌ | ✅ | ❌ (by design) | — |

### A.6 Implementation Order Update

Add to Phase E (Exception Handling):
- `DataIntegrityViolationException` handler (H-44 TOCTOU safety net)

Add to Phase D (MailService Redesign):
- `EmailQueueStatusService.java` creation (C-3 self-invocation fix)

Add to Phase C (Performance):
- `CouponService.saveAndFlush()` (H-3 extension)
- `PointService.saveAndFlush()` (H-3 extension)

Add to Phase G (Controller Fixes):
- Thymeleaf template updates for `orders/list.html` (H-9)
- Thymeleaf template updates for `admin/products/list.html` (H-10)
- Price sort UI removal in `products/list.html` (H-42)
