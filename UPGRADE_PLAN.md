# Spring Boot Migration Plan

## Apache Struts 1.x から Spring Boot 3.x + Thymeleaf + Spring Data JPA への移行計画

## 概要

このプロジェクトは Apache Struts 1.3.10 を使用した古いJavaアプリケーションで、Java 1.5 をターゲットにしています。このドキュメントは、**Java 21 + Spring Boot 3.2.x + Thymeleaf + Spring Data JPA** への完全移行計画を示します。

## 現在の状態

### Javaバージョン

- **現在**: Java 1.5 (2004年リリース、サポート終了)
- **移行先**: Java 21 LTS (2023年9月リリース、2031年9月までのLTSサポート)

### フレームワーク

- **現在**: Apache Struts 1.3.10 (2008年リリース、EOL、多数の既知の脆弱性)
- **移行先**: Spring Boot 3.2.x (最新安定版、長期サポート)

### 依存関係の現在のバージョン

| カテゴリ | ライブラリ | 現在のバージョン | 最新LTSバージョン | 備考 |
| --- | --- | --- | --- | --- |
| **フレームワーク** | | | | |
| | Apache Struts Core | 1.3.10 | N/A | EOL、移行推奨 |
| | Apache Struts Taglib | 1.3.10 | N/A | EOL、移行推奨 |
| | Apache Struts Tiles | 1.3.10 | N/A | EOL、移行推奨 |
| | Apache Struts Extras | 1.3.10 | N/A | EOL、移行推奨 |
| **データベース接続** | | | | |
| | commons-dbcp | 1.2.2 | 2.12.0 | DBCP2への移行推奨 |
| | commons-pool | 1.2 | 2.12.0 | Pool2への移行推奨 |
| | commons-dbutils | 1.1 | 1.8.1 | アップグレード可能 |
| | PostgreSQL JDBC | 9.2-1004-jdbc3 | 42.7.4 | 大幅なアップグレード |
| **ファイルアップロード** | | | | |
| | commons-fileupload | 1.3.3 | 1.5 | セキュリティ修正あり |
| **ロギング** | | | | |
| | log4j | 1.2.17 | N/A | Log4j2 2.23.1へ移行推奨 |
| **メール** | | | | |
| | javax.mail | 1.4.7 | Jakarta Mail 2.1.3 | Jakarta EEへ移行 |
| **ビューテンプレート/Web** | | | | |
| | jsp-api | 2.1 | Thymeleaf 3.1.x | JSPからThymeleafへ移行 |
| | servlet-api | 2.5 | Spring Boot組み込み（Tomcat 10.1.x） | Spring Boot Starterに含まれる |
| | - | - | Spring Web MVC 6.1.x | RESTful Web Service対応 |
| **テスト** | | | | |
| | JUnit | 4.12 | JUnit 5.10.2 | JUnit Jupiterへ移行 |
| | H2 Database | 1.3.176 | 2.2.224 | テスト用 |
| | StrutsTestCase | 2.1.4-1.2-2.4 | N/A | Struts依存、削除検討 |

## 移行戦略

### Spring Bootを選択する理由

**Apache Struts 1.x は2013年にEOLとなり、多数の既知の脆弱性があります。** 依存関係の部分的なアップグレードでは根本的な問題は解決しません。

#### Spring Boot 3.2.x への完全移行を推奨する理由

1. **セキュリティ**: 継続的なセキュリティアップデートとサポート
2. **コミュニティ**: 最大のJavaコミュニティと豊富なドキュメント
3. **最新技術**: Java 21の全機能を活用可能
4. **生産性**: 自動設定、組み込みサーバー、開発ツールによる高速開発
5. **将来性**: マイクロサービス、クラウドネイティブへの移行パスが明確
6. **エコシステム**: 豊富なSpring Bootスターター、統合サポート

### 移行先の技術スタック

| コンポーネント | Struts 1.x | Spring Boot 3.2.x |
| --- | --- | --- |
| **フレームワーク** | Apache Struts 1.3.10 | Spring Boot 3.2.x + Spring MVC 6.1.x |
| **Javaバージョン** | Java 1.5 | Java 21 LTS |
| **ビューテンプレート** | JSP + Struts Taglib | Thymeleaf 3.1.x |
| **データアクセス** | JDBC + Commons DBUtils | Spring Data JPA 3.2.x + Hibernate 6.4.x |
| **接続プール** | Commons DBCP 1.x | HikariCP (Spring Boot標準) |
| **バリデーション** | Commons Validator | Bean Validation 3.0 (Hibernate Validator) |
| **ロギング** | Log4j 1.2.17 | Logback (Spring Boot標準) + SLF4J |
| **依存性注入** | なし | Spring IoC Container |
| **テスト** | JUnit 4 + StrutsTestCase | JUnit 5 + Spring Boot Test |
| **ビルドツール** | Maven 2.x系 | Maven 3.9.x |
| **アプリケーションサーバー** | Tomcat 6/7 (外部) | 組み込みTomcat 10.1.x |

## Struts 1.x と Spring Boot の対応関係

### アーキテクチャの対応

| Struts 1.x コンポーネント | Spring Boot 対応 | 説明 |
| --- | --- | --- |
| **Action** | `@Controller` + `@RequestMapping` | リクエスト処理 |
| **ActionForm** | `@ModelAttribute` + Bean Validation | フォームデータバインディング |
| **struts-config.xml** | Java Config (`@Configuration`) | アプリケーション設定 |
| **ActionForward** | `ModelAndView` / `return "viewName"` | ビュー遷移 |
| **ActionMapping** | `@RequestMapping` / `@GetMapping` / `@PostMapping` | URLマッピング |
| **ActionServlet** | `DispatcherServlet` (自動設定) | フロントコントローラー |
| **JSP + Struts Tags** | Thymeleaf テンプレート | ビューレンダリング |
| **Validator Framework** | Bean Validation + `@Valid` | 入力検証 |
| **MessageResources** | `MessageSource` + `messages.properties` | 国際化 |
| **DAO (手動JDBC)** | Spring Data JPA Repository | データアクセス |
| **DataSource (DBCP)** | HikariCP (自動設定) | コネクションプール |

## 段階的移行計画

### フェーズ0: 準備フェーズ（1週間）

#### タスク内容

1. **現状分析**
   - 全Actionクラスのリスト作成
   - 全JSPページのリスト作成
   - データベースアクセスパターンの調査
   - 外部ライブラリの依存関係確認

2. **環境構築**
   - JDK 21のインストール
   - IDE (IntelliJ IDEA / Eclipse) の準備
   - Gitブランチ戦略の決定（例: `feature/spring-boot-migration`）

3. **Spring Boot プロジェクトの作成**
   - Spring Initializr で基本プロジェクト生成
   - 必要な依存関係の追加

#### Spring Initializr 設定

```text
Project: Maven
Language: Java
Spring Boot: 3.2.x (最新安定版)
Java: 21
Packaging: War (既存のWARデプロイとの互換性のため、後でJarに変更可能)

Dependencies:
- Spring Web
- Thymeleaf
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Spring Boot DevTools
- Lombok (オプション、ボイラープレートコード削減)
- Spring Boot Actuator (オプション、監視用)
```

#### 生成される pom.xml の例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>
    
    <groupId>com.skishop</groupId>
    <artifactId>skishop-app</artifactId>
    <version>2.0.0</version>
    <packaging>war</packaging>
    <name>SkiShop Application</name>
    
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web (Spring MVC含む) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Thymeleaf テンプレートエンジン -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        
        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Bean Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- PostgreSQL ドライバ -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- 開発ツール (ホットリロード等) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Lombok (オプション) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- メール送信 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        
        <!-- テスト -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- H2 Database (テスト用) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### フェーズ1: プロジェクト構造とアプリケーションエントリポイントの作成（1週間）

#### Spring Boot メインクラスの作成

```java
package com.skishop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SkiShopApplication extends SpringBootServletInitializer {
    
    public static void main(String[] args) {
        SpringApplication.run(SkiShopApplication.class, args);
    }
}
```

#### application.yml の設定

```yaml
spring:
  application:
    name: skishop-app
  
  # データソース設定
  datasource:
    url: jdbc:postgresql://localhost:5432/skishop
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  # Thymeleaf設定
  thymeleaf:
    cache: false  # 開発時はfalse
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
  
  # メール設定
  mail:
    host: ${MAIL_HOST:smtp.example.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# ログ設定
logging:
  level:
    com.skishop: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: DEBUG
```

#### パッケージ構造の作成

```text
src/main/java/com/skishop/
├── SkiShopApplication.java
├── config/              # 設定クラス
│   ├── WebConfig.java
│   └── SecurityConfig.java (必要に応じて)
├── controller/          # Struts Action → Controller
├── model/              # エンティティクラス
│   └── entity/
├── repository/         # Spring Data JPA リポジトリ
├── service/            # ビジネスロジック
│   └── impl/
├── dto/                # データ転送オブジェクト
└── exception/          # 例外処理

src/main/resources/
├── application.yml
├── messages.properties
├── templates/          # Thymeleaf テンプレート
│   ├── fragments/      # 共通部品
│   ├── layout/         # レイアウト
│   └── pages/          # ページ
└── static/
    ├── css/
    ├── js/
    └── images/
```

### フェーズ2: データアクセス層の移行（2週間）

#### JPA エンティティクラスの作成例

```java
package com.skishop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "商品名は必須です")
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @NotNull(message = "価格は必須です")
    @DecimalMin(value = "0.0", inclusive = false, message = "価格は0より大きい値を入力してください")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Min(value = 0, message = "在庫数は0以上である必要があります")
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### Spring Data JPA リポジトリの作成

```java
package com.skishop.repository;

import com.skishop.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // メソッド名からクエリ自動生成
    List<Product> findByNameContaining(String keyword);
    
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    Optional<Product> findByName(String name);
    
    // カスタムクエリ
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 ORDER BY p.createdAt DESC")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Product> searchProducts(@Param("keyword") String keyword);
}
```

#### サービス層の作成

```java
package com.skishop.service.impl;

import com.skishop.model.entity.Product;
import com.skishop.repository.ProductRepository;
import com.skishop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    
    @Override
    public List<Product> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll();
    }
    
    @Override
    public Optional<Product> getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);
        return productRepository.findById(id);
    }
    
    @Override
    @Transactional
    public Product saveProduct(Product product) {
        log.info("Saving product: {}", product.getName());
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        productRepository.deleteById(id);
    }
}
```

### フェーズ3: コントローラー層の移行（3週間）

#### Struts Action から Spring MVC Controller への移行

**Spring Boot の Controller例:**

```java
package com.skishop.controller;

import com.skishop.dto.ProductFormDTO;
import com.skishop.model.entity.Product;
import com.skishop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public String listProducts(Model model) {
        log.debug("Displaying product list");
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "products/list";
    }
    
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("productForm", new ProductFormDTO());
        return "products/form";
    }
    
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("productForm") ProductFormDTO form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "products/form";
        }
        
        Product product = new Product();
        product.setName(form.getName());
        product.setDescription(form.getDescription());
        product.setPrice(form.getPrice());
        product.setStockQuantity(form.getStockQuantity());
        
        productService.saveProduct(product);
        
        redirectAttributes.addFlashAttribute("message", "商品を登録しました");
        return "redirect:/products";
    }
}
```

#### DTO の作成

```java
package com.skishop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductFormDTO {
    
    private Long id;
    
    @NotBlank(message = "商品名は必須です")
    @Size(max = 100, message = "商品名は100文字以内で入力してください")
    private String name;
    
    @Size(max = 500, message = "説明は500文字以内で入力してください")
    private String description;
    
    @NotNull(message = "価格は必須です")
    @DecimalMin(value = "0.01", message = "価格は0より大きい値を入力してください")
    private BigDecimal price;
    
    @NotNull(message = "在庫数は必須です")
    @Min(value = 0, message = "在庫数は0以上である必要があります")
    private Integer stockQuantity;
}
```

### フェーズ4: ビュー層の移行（JSP → Thymeleaf）（3週間）

#### Thymeleaf テンプレート例（商品一覧）

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:replace="~{layout/main :: layout(~{::title}, ~{::content})}">
<head>
    <title th:text="#{products.list.title}">商品一覧</title>
</head>
<body>
    <div th:fragment="content">
        <h1 th:text="#{products.list.header}">商品一覧</h1>
        
        <div class="alert alert-success" th:if="${message}" th:text="${message}"></div>
        
        <table class="table">
            <thead>
                <tr>
                    <th th:text="#{product.name}">商品名</th>
                    <th th:text="#{product.price}">価格</th>
                    <th th:text="#{product.stock}">在庫</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="product : ${products}">
                    <td th:text="${product.name}">商品名</td>
                    <td th:text="${#numbers.formatCurrency(product.price)}">¥1,000</td>
                    <td th:text="${product.stockQuantity}">10</td>
                    <td>
                        <a th:href="@{/products/{id}/edit(id=${product.id})}" class="btn btn-sm btn-primary">編集</a>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <a th:href="@{/products/new}" class="btn btn-primary">新規登録</a>
    </div>
</body>
</html>
```

#### Struts Taglib と Thymeleaf の対応表

| Struts 1.x Tag | Thymeleaf 相当 | 説明 |
| --- | --- | --- |
| `<bean:write name="var"/>` | `th:text="${var}"` | 変数出力 |
| `<bean:message key="key"/>` | `th:text="#{key}"` | メッセージリソース |
| `<html:link action="/path">` | `th:href="@{/path}"` | リンク |
| `<html:form action="/submit">` | `th:action="@{/submit}" method="post"` | フォーム |
| `<html:text property="name"/>` | `th:field="*{name}"` | テキスト入力 |
| `<html:errors property="name"/>` | `th:errors="*{name}"` | バリデーションエラー |
| `<logic:iterate id="item" name="list">` | `th:each="item : ${list}"` | ループ |
| `<logic:present name="var">` | `th:if="${var != null}"` | 存在チェック |
| `<logic:notPresent name="var">` | `th:if="${var == null}"` | 非存在チェック |
| `<logic:equal name="var" value="val">` | `th:if="${var == 'val'}"` | 値比較 |

### フェーズ5: 設定とその他の移行（1週間）

#### 例外処理

```java
package com.skishop.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralError(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("error", "予期しないエラーが発生しました");
        return "error/500";
    }
}
```

### フェーズ6: テストの作成（2週間）

#### リポジトリテスト

```java
package com.skishop.repository;

import com.skishop.model.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void testFindByNameContaining() {
        // Given
        Product product = new Product();
        product.setName("Ski Boots");
        product.setPrice(BigDecimal.valueOf(200.00));
        product.setStockQuantity(10);
        productRepository.save(product);
        
        // When
        List<Product> found = productRepository.findByNameContaining("Ski");
        
        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Ski Boots");
    }
}
```

#### サービステスト

```java
package com.skishop.service;

import com.skishop.model.entity.Product;
import com.skishop.repository.ProductRepository;
import com.skishop.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    private Product testProduct;
    
    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.valueOf(100.00));
        testProduct.setStockQuantity(5);
    }
    
    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));
        
        // When
        List<Product> products = productService.getAllProducts();
        
        // Then
        assertThat(products).hasSize(1);
        verify(productRepository, times(1)).findAll();
    }
    
    @Test
    void saveProduct_ShouldSaveAndReturnProduct() {
        // Given
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        
        // When
        Product saved = productService.saveProduct(testProduct);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Product");
        verify(productRepository, times(1)).save(testProduct);
    }
}
```

#### コントローラーテスト

```java
package com.skishop.controller;

import com.skishop.model.entity.Product;
import com.skishop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    void listProducts_ShouldReturnProductListView() throws Exception {
        // Given
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStockQuantity(10);
        
        when(productService.getAllProducts()).thenReturn(Arrays.asList(product));
        
        // When & Then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"));
    }
}
```

#### 統合テスト

```java
package com.skishop;

import com.skishop.model.entity.Product;
import com.skishop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void testProductCreationAndRetrieval() {
        // Given
        Product product = new Product();
        product.setName("Integration Test Product");
        product.setPrice(BigDecimal.valueOf(150.00));
        product.setStockQuantity(20);
        productRepository.save(product);
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/products",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Integration Test Product");
    }
}
```

### フェーズ7: パフォーマンステストとチューニング（1週間）

#### パフォーマンステストの実施

##### JMeterを使用した負荷テスト

```xml
<!-- pom.xmlに追加 -->
<dependency>
    <groupId>org.apache.jmeter</groupId>
    <artifactId>ApacheJMeter_core</artifactId>
    <version>5.6.3</version>
    <scope>test</scope>
</dependency>
```

##### アプリケーションメトリクスの監視

```yaml
# application.yml に追加
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

##### パフォーマンステストシナリオ

1. **同時接続テスト**
   - 100ユーザーの同時アクセス
   - レスポンスタイム < 500ms
   - エラー率 < 1%

2. **データベースクエリ最適化**
   - N+1問題の検出と修正
   - インデックスの最適化
   - クエリプランの分析

3. **キャッシュ戦略**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("products"),
            new ConcurrentMapCache("categories")
        ));
        return cacheManager;
    }
}
```

```java
@Service
public class ProductServiceImpl implements ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}
```

#### パフォーマンスチューニングのチェックリスト

- [ ] データベース接続プールの設定最適化（HikariCP）
- [ ] JPA/Hibernateのクエリ最適化（Lazy Loading、Eager Loading）
- [ ] 適切なインデックスの作成
- [ ] キャッシュ戦略の実装
- [ ] 不要なログ出力の削減
- [ ] 静的リソースの圧縮とキャッシュ
- [ ] JVMヒープサイズの調整
- [ ] ガベージコレクションの最適化

### フェーズ8: ドキュメント化と運用準備（1週間）

#### 作成すべきドキュメント

##### 1. アーキテクチャドキュメント

**内容:**

- システム全体のアーキテクチャ図
- コンポーネント間の依存関係
- データフロー図
- デプロイメントアーキテクチャ

##### 2. API仕様書

```java
// SpringDocを使用したAPI自動ドキュメント化
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SkiShop API")
                .version("2.0.0")
                .description("Spring Boot移行後のSkiShopアプリケーションAPI"));
    }
}
```

```xml
<!-- pom.xmlに追加 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

##### 3. 運用手順書

**内容:**

- アプリケーションの起動/停止手順
- ログの確認方法
- トラブルシューティングガイド
- バックアップ/リストア手順
- デプロイ手順

**起動コマンド例:**

```bash
# 開発環境
mvn spring-boot:run

# 本番環境（JARファイル）
java -jar -Xmx2g -Xms1g \
  -Dspring.profiles.active=production \
  skishop-app-2.0.0.jar

# 本番環境（Dockerコンテナ）
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DB_USERNAME=prod_user \
  -e DB_PASSWORD=prod_pass \
  skishop-app:2.0.0
```

##### 4. 開発者ガイド

**内容:**

- プロジェクト構成の説明
- コーディング規約
- テストの書き方
- ローカル開発環境のセットアップ
- よくある問題と解決方法

##### 5. 移行レポート

**内容:**

- 移行前後の比較
- 遭遇した問題と解決策
- 残存する技術的負債
- 今後の改善提案

#### 運用監視の設定

```yaml
# application-production.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      show-details: always
  health:
    db:
      enabled: true
    diskspace:
      enabled: true

logging:
  level:
    root: WARN
    com.skishop: INFO
  file:
    name: /var/log/skishop/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### デプロイメント用Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/skishop-app-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### CI/CDパイプライン（GitHub Actions例）

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Run tests
      run: mvn test
    
    - name: Build Docker image
      run: docker build -t skishop-app:${{ github.sha }} .
    
    - name: Push to registry
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker push skishop-app:${{ github.sha }}
```

### フェーズ9: レガシーコードのクリーンアップと最終検証（1週間）

#### クリーンアップの目的

フェーズ8までの移行が完了し、Spring Bootアプリケーションが正常に動作することを確認した後、使用されなくなった古いStruts関連のコード、設定ファイル、JSP関連のコードを削除します。これにより、コードベースを整理し、保守性を向上させます。

#### 削除対象のファイルとコード

##### 1. Struts関連の設定ファイル

```text
削除対象:
- WEB-INF/struts-config.xml
- WEB-INF/validation.xml
- WEB-INF/validator-rules.xml
- WEB-INF/tiles-defs.xml
- src/main/resources/struts.properties
- src/main/resources/validation.properties
```

##### 2. Struts Actionクラス

```text
削除対象ディレクトリ:
- src/main/java/com/*/action/
- src/main/java/com/*/struts/

確認事項:
- 全てのActionクラスがSpring MVCのControllerに移行済みであること
- ビジネスロジックがServiceレイヤーに抽出されていること
```

##### 3. ActionFormクラス

```text
削除対象:
- src/main/java/com/*/form/
- *ActionForm.java

確認事項:
- 全てのフォームクラスがDTOに移行済みであること
- Bean Validationアノテーションが適用されていること
```

##### 4. JSPファイルとStruts Taglib

```text
削除対象:
- src/main/webapp/**/*.jsp
- src/main/webapp/WEB-INF/tags/
- WEB-INF/tld/*.tld (Struts Tag Library定義)

確認事項:
- 全てのJSPがThymeleafテンプレートに移行済みであること
- 画面表示の動作確認が完了していること
```

##### 5. Struts関連の依存関係（pom.xml）

```xml
削除対象の依存関係:
<dependencies>
    <!-- 削除: Apache Struts -->
    <dependency>
        <groupId>struts</groupId>
        <artifactId>struts</artifactId>
        <version>1.3.10</version>
    </dependency>
    
    <!-- 削除: Commons Validator -->
    <dependency>
        <groupId>commons-validator</groupId>
        <artifactId>commons-validator</artifactId>
        <version>1.3.1</version>
    </dependency>
    
    <!-- 削除: Commons Digester -->
    <dependency>
        <groupId>commons-digester</groupId>
        <artifactId>commons-digester</artifactId>
        <version>1.8</version>
    </dependency>
    
    <!-- 削除: Commons BeanUtils -->
    <dependency>
        <groupId>commons-beanutils</groupId>
        <artifactId>commons-beanutils</artifactId>
        <version>1.8.0</version>
    </dependency>
    
    <!-- 削除: Commons Chain -->
    <dependency>
        <groupId>commons-chain</groupId>
        <artifactId>commons-chain</artifactId>
        <version>1.2</version>
    </dependency>
    
    <!-- 削除: Servlet API (Spring Bootに含まれる) -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>servlet-api</artifactId>
        <version>2.5</version>
    </dependency>
    
    <!-- 削除: JSP API (Thymeleafに移行) -->
    <dependency>
        <groupId>javax.servlet.jsp</groupId>
        <artifactId>jsp-api</artifactId>
        <version>2.1</version>
    </dependency>
    
    <!-- 削除: JSTL (Thymeleafに移行) -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>jstl</artifactId>
        <version>1.2</version>
    </dependency>
    
    <!-- 削除: StrutsTestCase -->
    <dependency>
        <groupId>strutstestcase</groupId>
        <artifactId>strutstestcase</artifactId>
        <version>2.1.4-1.2-2.4</version>
    </dependency>
</dependencies>
```

##### 6. web.xmlの更新

```xml
削除対象のweb.xml内容:
<!-- 削除: Struts ActionServlet設定 -->
<servlet>
    <servlet-name>action</servlet-name>
    <servlet-class>org.apache.struts.action.ActionServlet</servlet-class>
    <init-param>
        <param-name>config</param-name>
        <param-value>/WEB-INF/struts-config.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>action</servlet-name>
    <url-pattern>*.do</url-pattern>
</servlet-mapping>

<!-- 削除: Struts TagLibの設定 -->
<jsp-config>
    <taglib>
        <taglib-uri>/tags/struts-bean</taglib-uri>
        <taglib-location>/WEB-INF/struts-bean.tld</taglib-location>
    </taglib>
    <!-- その他のStrutsタグライブラリ定義 -->
</jsp-config>

注意: Spring Bootでは通常web.xmlは不要ですが、
      既存の設定を残す必要がある場合は、
      Struts関連の設定のみを削除してください。
```

##### 7. その他の設定ファイル

```text
削除対象:
- src/main/resources/ApplicationResources.properties (messages.propertiesに移行済み)
- src/main/webapp/WEB-INF/classes/ (不要なクラスファイル)
```

#### クリーンアップの手順

##### ステップ1: 事前準備（1日）

```bash
# 1. 完全なバックアップの作成
git checkout -b backup/before-cleanup
git add .
git commit -m "Backup before legacy code cleanup"
git push origin backup/before-cleanup

# 2. クリーンアップ用ブランチの作成
git checkout main
git checkout -b feature/cleanup-legacy-code

# 3. 現在の動作確認
mvn clean test
mvn spring-boot:run
# 全機能の動作確認を実施
```

##### ステップ2: Struts関連ファイルの削除（2日）

```bash
# Struts設定ファイルの削除
rm -f src/main/webapp/WEB-INF/struts-config.xml
rm -f src/main/webapp/WEB-INF/validation.xml
rm -f src/main/webapp/WEB-INF/validator-rules.xml
rm -f src/main/webapp/WEB-INF/tiles-defs.xml
rm -rf src/main/webapp/WEB-INF/tld/

# Struts Javaコードの削除
find src/main/java -type d -name "action" -exec rm -rf {} +
find src/main/java -type d -name "form" -exec rm -rf {} +
find src/main/java -name "*Action.java" -delete
find src/main/java -name "*ActionForm.java" -delete

# 各削除後に必ずビルドとテストを実行
mvn clean compile
mvn test
```

##### ステップ3: JSP関連ファイルの削除（2日）

```bash
# 全JSPファイルの削除前にThymeleafテンプレートの存在確認
find src/main/resources/templates -name "*.html" | wc -l

# JSPファイルの削除
rm -rf src/main/webapp/*.jsp
rm -rf src/main/webapp/WEB-INF/jsp/
rm -rf src/main/webapp/WEB-INF/pages/

# タグファイルの削除
rm -rf src/main/webapp/WEB-INF/tags/

# 各削除後に動作確認
mvn spring-boot:run
# ブラウザで全画面を確認
```

##### ステップ4: pom.xmlのクリーンアップ（1日）

```bash
# pom.xmlから不要な依存関係を削除
# 手動で編集、または以下のコマンドで確認

# 未使用の依存関係を検出
mvn dependency:analyze

# ビルドとテストで問題がないことを確認
mvn clean install
mvn test

# 依存関係ツリーの確認
mvn dependency:tree
```

##### ステップ5: web.xmlの更新または削除（1日）

```bash
# Spring Bootではweb.xmlは基本的に不要
# Struts設定を削除後、web.xmlが空になった場合は削除
rm -f src/main/webapp/WEB-INF/web.xml

# または必要な設定のみを残して更新
# (Filter設定など、Spring Boot移行後も必要な設定がある場合)
```

#### クリーンアップ後の検証チェックリスト

##### 1. ビルドとテストの検証

- [ ] `mvn clean compile` が成功すること
- [ ] `mvn test` で全テストがパスすること
- [ ] `mvn package` でWAR/JARファイルが正常に生成されること
- [ ] コンパイルエラーが一切ないこと
- [ ] 警告メッセージの確認と対処

##### 2. アプリケーション起動の検証

- [ ] `mvn spring-boot:run` でアプリケーションが正常に起動すること
- [ ] 起動ログにエラーがないこと
- [ ] Spring Bootのバナーが表示されること
- [ ] 全てのBeanが正常にロードされること
- [ ] データベース接続が確立されること

##### 3. 機能テストの検証

- [ ] 全画面が正常に表示されること（Thymeleafテンプレート）
- [ ] 全フォームの送信が正常に動作すること
- [ ] データベースの登録・更新・削除が正常に動作すること
- [ ] ファイルアップロード機能が動作すること（該当する場合）
- [ ] メール送信機能が動作すること（該当する場合）
- [ ] セッション管理が正常に機能すること
- [ ] エラーハンドリングが正常に動作すること

##### 4. パフォーマンステストの検証

- [ ] レスポンスタイムが劣化していないこと
- [ ] メモリ使用量が適切であること
- [ ] CPU使用率が正常範囲内であること
- [ ] データベース接続プールが正常に動作すること

##### 5. セキュリティテストの検証

- [ ] 認証・認可が正常に動作すること
- [ ] XSS対策が機能していること（Thymeleafの自動エスケープ）
- [ ] CSRF対策が機能していること（必要な場合）
- [ ] SQLインジェクション対策が機能していること（JPA使用）

##### 6. コードベースの検証

- [ ] Struts関連のimport文が残っていないこと
- [ ] 使用されていないクラスがないこと
- [ ] TODO/FIXMEコメントの確認と対処
- [ ] コードの静的解析（SonarQubeなど）

```bash
# Strutsへの参照がないことを確認
grep -r "import org.apache.struts" src/
grep -r "struts" pom.xml

# JSP関連の参照がないことを確認
grep -r "import javax.servlet.jsp" src/
grep -r "jsp-api" pom.xml

# 検索結果が空であることを確認
```

#### クリーンアップ後の最終処理

##### 1. ドキュメントの更新

```markdown
更新対象ドキュメント:
- README.md（起動方法、技術スタックの更新）
- CHANGELOG.md（移行履歴の記録）
- API仕様書（OpenAPI/Swagger）
- 運用手順書（デプロイ手順の更新）
```

##### 2. 変更のコミットとプルリクエスト

```bash
# 変更をステージング
git add .

# コミット
git commit -m "chore: Remove legacy Struts and JSP code after Spring Boot migration

- Remove all Struts Action classes and ActionForm classes
- Remove all JSP files and Struts TagLib configurations
- Remove Struts dependencies from pom.xml
- Clean up web.xml (remove Struts servlet configuration)
- Verify all functionality works with Spring Boot and Thymeleaf

Closes #XXX"

# リモートにプッシュ
git push origin feature/cleanup-legacy-code

# プルリクエストを作成してレビュー依頼
```

##### 3. 本番デプロイ前の最終確認

- [ ] ステージング環境でのフルテスト実施
- [ ] 負荷テストの実施
- [ ] セキュリティスキャンの実施
- [ ] ステークホルダーへのデモ実施
- [ ] デプロイ計画の最終確認
- [ ] ロールバック手順の準備

##### 4. 本番環境デプロイ

```bash
# タグの作成
git tag -a v2.0.0 -m "Spring Boot migration completed - Legacy code removed"
git push origin v2.0.0

# 本番デプロイ（CI/CDパイプライン経由）
# または手動デプロイ
```

#### クリーンアップによる効果

##### 定量的効果

| 項目 | 削減量（推定） | 備考 |
| --- | --- | --- |
| コード行数 | 30-50%削減 | Action、ActionForm、JSPの削除 |
| 依存ライブラリ数 | 10-15個削減 | Struts関連ライブラリの削除 |
| WARファイルサイズ | 20-30%削減 | 不要なライブラリとJSPの削除 |
| ビルド時間 | 10-20%短縮 | 依存関係の削減 |
| 起動時間 | 改善 | Spring Bootの最適化 |

##### 定性的効果

1. **保守性の向上**
   - 二重管理の解消
   - コードベースの一貫性
   - 新規開発者の理解容易化

2. **セキュリティの向上**
   - 脆弱性のあるライブラリの削除
   - 攻撃対象の削減

3. **開発効率の向上**
   - 明確なアーキテクチャ
   - モダンな開発環境
   - テスト容易性の向上

4. **技術的負債の解消**
   - EOLフレームワークの削除
   - レガシーコードの削除
   - 将来への投資

#### トラブルシューティング

##### 問題1: 削除後にビルドエラーが発生

**原因**: 一部のコードが削除したクラスに依存している

**対処**:

```bash
# エラーログから依存しているクラスを特定
mvn clean compile 2>&1 | grep "cannot find symbol"

# 該当箇所を修正
# - Spring Bootの同等機能に置き換え
# - 不要なコードの場合は削除
```

##### 問題2: テストが失敗する

**原因**: テストコードがStrutsTestCaseに依存している

**対処**:

```java
// 削除: StrutsTestCaseベースのテスト
// 追加: Spring Boot Testベースのテスト
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testSomething() throws Exception {
        mockMvc.perform(get("/path"))
            .andExpect(status().isOk());
    }
}
```

##### 問題3: 画面が表示されない

**原因**: JSP削除後、Thymeleafテンプレートのパスが正しくない

**対処**:

```yaml
# application.yml で確認
spring:
  thymeleaf:
    prefix: classpath:/templates/  # 正しいパス
    suffix: .html
```

```java
// Controllerで正しいビュー名を返す
@GetMapping("/products")
public String listProducts() {
    return "products/list";  // templates/products/list.html
}
```

## 主要な移行課題と対策

### 1. ビジネスロジックの抽出

**課題**: Struts Actionにビジネスロジックが直接記述されている場合が多い

**対策**:

- Actionから段階的にServiceレイヤーへロジックを抽出
- トランザクション境界を適切に設定（`@Transactional`）
- 依存性注入を活用してテスタビリティを向上

### 2. セッション管理

**課題**: Struts 1.xでは直接HttpSessionを操作

**対策**:

- Spring Sessionを利用（オプション）
- セッションスコープBeanの活用
- ステートレスな設計を推奨（RESTful）

### 3. データベーススキーマ

**課題**: 既存のデータベーススキーマとの整合性

**対策**:

- JPA エンティティを既存テーブル構造に合わせる
- `@Table(name="existing_table")` で既存テーブル名を指定
- 必要に応じてFlywayやLiquibaseでマイグレーション管理

## リスク評価と緩和策

| リスク | 深刻度 | 確率 | 影響 | 緩和策 |
| --- | --- | --- | --- | --- |
| ビジネスロジックの理解不足 | 高 | 中 | 機能の誤実装 | ドキュメント化、元の開発者へのヒアリング |
| データベーススキーマの不整合 | 高 | 低 | データ破損 | 移行前の完全バックアップ、段階的リリース |
| パフォーマンス劣化 | 中 | 低 | ユーザー体験低下 | 性能テスト実施、プロファイリング |
| 未検出のバグ | 中 | 中 | 本番障害 | 十分なテストカバレッジ、段階的リリース |
| 学習コスト | 中 | 高 | スケジュール遅延 | トレーニング実施、ペアプログラミング |
| 外部ライブラリの互換性 | 低 | 低 | ビルドエラー | 事前調査、代替ライブラリ検討 |

## タイムラインと工数見積もり

| フェーズ | 期間 | 必要リソース | 成果物 |
| --- | --- | --- | --- |
| フェーズ0: 準備 | 1週間 | 1-2名 | 環境構築、現状分析ドキュメント |
| フェーズ1: プロジェクト構造 | 1週間 | 2名 | Spring Bootプロジェクト、基本設定 |
| フェーズ2: データアクセス層 | 2週間 | 2-3名 | エンティティ、リポジトリ、サービス |
| フェーズ3: コントローラー層 | 3週間 | 3-4名 | 全Controller、DTO、バリデーション |
| フェーズ4: ビュー層 | 3週間 | 2-3名 | 全Thymeleafテンプレート |
| フェーズ5: 設定・その他 | 1週間 | 2名 | 例外処理、ファイルアップロード等 |
| フェーズ6: テスト | 2週間 | 3-4名 | 単体・統合テスト |
| フェーズ7: パフォーマンステスト | 1週間 | 2名 | 性能測定、チューニング |
| フェーズ8: ドキュメント化 | 1週間 | 1-2名 | 技術ドキュメント、運用手順書 |
| フェーズ9: レガシーコードクリーンアップ | 1週間 | 2-3名 | クリーンなコードベース、最終検証 |
| **合計** | **約16週間（4ヶ月）** | **2-4名** | |

### 並行作業の可能性

- フェーズ3とフェーズ4は一部並行実施可能
- テストは各フェーズで並行して作成
- フェーズ9は全機能移行完了後に実施（並行作業不可）

## 段階的リリース戦略

### ストラングラーパターン（推奨）

既存のStruts 1.xアプリケーションとSpring Bootアプリケーションを並行稼働:

1. **フェーズ1**: 新機能はSpring Bootで開発
2. **フェーズ2**: 使用頻度の低い画面から移行
3. **フェーズ3**: 主要機能の移行
4. **フェーズ4**: 全機能移行完了後、Struts 1.x版を廃止

**メリット**:

- リスク分散
- 段階的な検証
- ロールバックが容易

**実装方法**:

- リバースプロキシ（Nginx等）でURLパスベースでルーティング
- `/api/*` → Spring Boot
- その他 → Struts 1.x

### ビッグバン移行

全機能を一度に移行:

**メリット**:

- 二重管理不要
- 移行期間が短い

**デメリット**:

- リスクが高い
- ロールバックが困難

**推奨**: 小規模アプリケーションの場合のみ

## 移行チェックリスト

### 準備フェーズ

- [ ] プロジェクトチームの編成
- [ ] ステークホルダーの承認取得
- [ ] JDK 21のインストールと環境設定
- [ ] Spring Initializrでプロジェクト生成
- [ ] Git リポジトリのブランチ戦略決定
- [ ] CI/CD パイプラインの準備

### データアクセス層

- [ ] データベーススキーマの分析
- [ ] JPA エンティティクラスの作成
- [ ] Spring Data JPA リポジトリの作成
- [ ] サービスレイヤーの作成
- [ ] トランザクション境界の設定
- [ ] リポジトリとサービスの単体テスト

### コントローラー層

- [ ] Struts Actionの棚卸し
- [ ] Spring MVC Controllerへの変換
- [ ] DTOクラスの作成
- [ ] Bean Validationの実装
- [ ] 例外ハンドリングの実装
- [ ] Controller の単体テスト

### ビュー層

- [ ] JSPページの棚卸し
- [ ] Thymeleafテンプレートへの変換
- [ ] レイアウトテンプレートの作成
- [ ] CSS/JavaScriptの移行
- [ ] メッセージリソースの確認
- [ ] 画面表示の動作確認

### その他機能

- [ ] ファイルアップロード機能の移行
- [ ] メール送信機能の移行
- [ ] セッション管理の実装
- [ ] セキュリティ設定（必要に応じて）
- [ ] ログ設定の確認

### テストとデプロイ

- [ ] 単体テストの完了
- [ ] 統合テストの完了
- [ ] パフォーマンステスト
- [ ] セキュリティテスト
- [ ] ステージング環境へのデプロイ
- [ ] 本番環境へのデプロイ

### レガシーコードのクリーンアップ

- [ ] 全機能のSpring Boot移行完了確認
- [ ] Struts関連設定ファイルの削除
- [ ] Struts ActionクラスとActionFormクラスの削除
- [ ] 全JSPファイルの削除
- [ ] pom.xmlから不要な依存関係の削除
- [ ] web.xmlのクリーンアップ
- [ ] クリーンアップ後のビルド確認
- [ ] クリーンアップ後の全機能テスト
- [ ] クリーンアップ後のパフォーマンステスト
- [ ] 最終的なコードレビュー

## 次のステップ

### 即座に実施すべきこと

1. **ステークホルダーミーティング**
   - 移行計画の説明と承認
   - リソース配分の決定
   - リリーススケジュールの合意

2. **技術評価**
   - POC（概念実証）の実施
   - 主要機能の1つをSpring Bootで実装してみる
   - パフォーマンスとの検証

3. **チーム準備**
   - Spring Boot研修の実施
   - Thymeleaf、Spring Data JPAの学習
   - ペアプログラミング体制の構築

### 1ヶ月以内

1. **環境構築**
   - 開発環境の整備
   - CI/CDパイプラインの構築
   - テスト環境の準備

2. **フェーズ0-1の完了**
   - 現状分析ドキュメント作成
   - Spring Bootプロジェクトの作成
   - 基本設定の完了

### 3ヶ月以内

1. **コア機能の移行**
   - データアクセス層の完全移行
   - 主要画面のコントローラーとビューの移行
   - 基本テストの完了

2. **ステージング環境デプロイ**
   - 移行済み機能のステージング環境テスト
   - フィードバックの収集と改善

### 6ヶ月以内

1. **全機能の移行完了**
   - すべてのStruts機能のSpring Boot化
   - 総合テスト完了
   - ドキュメント整備

2. **本番環境デプロイ**
   - 段階的リリースまたは一括切り替え
   - 監視体制の確立
   - 旧システムの段階的廃止

## 成功の鍵

### 技術面

1. **段階的な移行**: 一度にすべてを変更しない
2. **十分なテスト**: 各フェーズで徹底的にテスト
3. **継続的インテグレーション**: 自動テストとビルド
4. **パフォーマンス監視**: 移行前後での性能測定

### 組織面

1. **経営陣のコミットメント**: リソースとスケジュールの確保
2. **チームのスキルアップ**: 継続的な学習と研修
3. **明確なコミュニケーション**: 進捗の透明性
4. **適切なリスク管理**: 問題の早期発見と対応

## 期待される効果

### 短期的効果（6ヶ月以内）

- セキュリティリスクの大幅な低減
- 開発生産性の向上（自動設定、ホットリロード等）
- メンテナンス性の向上

### 中長期的効果（6ヶ月以降）

- Java 21の最新機能活用によるコード品質向上
- マイクロサービス化への道筋
- クラウドネイティブアーキテクチャへの移行可能性
- 新規開発者のオンボーディング容易化
- コミュニティサポートの充実

## まとめ

このプロジェクトはApache Struts 1.3.10という2008年のフレームワークを使用しており、セキュリティリスクが極めて高い状態です。本移行計画では、**Java 21 + Spring Boot 3.2.x + Thymeleaf + Spring Data JPA** への完全移行を提案します。

### 移行のメリット

1. **セキュリティ**: EOLのStruts 1.xから、継続的にサポートされるSpring Bootへ
2. **生産性**: 自動設定、開発ツール、豊富なエコシステム
3. **保守性**: モダンなアーキテクチャ、明確な責務分離
4. **将来性**: マイクロサービス、クラウドネイティブへの移行パス
5. **人材**: Spring開発者の豊富さ、学習リソースの充実

### 推奨実装アプローチ

**期間**: 約6ヶ月（16週間の開発 + テスト・デプロイ・クリーンアップ）

**リソース**: 2-4名の開発者

**リリース戦略**: ストラングラーパターンによる段階的移行（推奨）

**最終フェーズ**: レガシーコードの完全削除によるコードベースのクリーンアップ

### 投資対効果

| 項目 | 短期（6ヶ月） | 中期（1-2年） | 長期（2年以上） |
| --- | --- | --- | --- |
| 開発コスト | 高（移行作業） | 低（生産性向上） | 低（保守容易） |
| セキュリティリスク | 大幅低減 | 最小化 | 最小化 |
| 開発速度 | 一時的低下 | 向上 | 大幅向上 |
| システム品質 | 向上 | 大幅向上 | 大幅向上 |

### 最終的な推奨事項

このApache Struts 1.xアプリケーションは**今すぐに移行を開始すべき**状態です。セキュリティリスクと技術的負債を考慮すると、**Spring Boot 3.2.x への完全移行が最適な選択**です。

- ✅ **Java 21**: 2031年までのLTSサポート
- ✅ **Spring Boot 3.2.x**: 業界標準、豊富なエコシステム
- ✅ **Thymeleaf**: モダンで保守しやすいテンプレートエンジン
- ✅ **Spring Data JPA**: 宣言的で生産性の高いデータアクセス

**今すぐ始めるべき3つのアクション:**

1. ステークホルダーへの説明と承認取得
2. 技術POCの実施（1-2週間）
3. 移行チームの編成と研修の開始

### 移行完了後の最終作業

移行が完了し、Spring Bootアプリケーションが正常に動作することを確認した後、**フェーズ9でレガシーコードのクリーンアップ**を実施します。これにより：

- 古いStrutsコードとJSPファイルを完全に削除
- 不要な依存関係を削除してアプリケーションサイズを削減
- コードベースをクリーンに保ち、保守性を大幅に向上
- 技術的負債を完全に解消

この移行により、セキュアで保守しやすく、将来にわたって拡張可能なアプリケーションを構築できます。
