---
applyTo: "**/controller/**/*.java"
---

# API 設計 Instructions

本 Instructions は `**/controller/**/*.java` に自動適用される。Controller クラスの作成・編集時に以下のチェック観点を遵守すること。

---

## 1. REST 設計原則

### URI 設計
- リソース指向の URI 設計（**名詞を使用、動詞を避ける**）
- コレクションリソースには**複数形**を使用（`/users`, `/orders`）
- ネストは 2 階層までに制限（`/users/{userId}/orders` は可、`/users/{userId}/orders/{orderId}/items/{itemId}` は避ける）
- URI に API バージョンを含める（`/api/v1/users`）
- URI にファイル拡張子を含めない（`.json`, `.xml`）

```java
// ✅ 良い例
@RequestMapping("/api/v1/users")
@RequestMapping("/api/v1/users/{userId}/orders")

// ❌ 悪い例
@RequestMapping("/api/v1/getUsers")        // 動詞を使用
@RequestMapping("/api/v1/user")            // 単数形
@RequestMapping("/api/v1/user_list")       // スネークケース
```

### HTTP メソッド
- 適切な HTTP メソッドを使用し、意味論を厳守する

| メソッド | 用途 | べき等性 | 安全性 | 成功時ステータス |
|---------|------|---------|--------|----------------|
| **GET** | リソースの取得 | ✅ | ✅ | 200（データあり）/ 204（データなし） |
| **POST** | リソースの作成 | ❌ | ❌ | 201 + `Location` ヘッダー |
| **PUT** | リソースの全置換 | ✅ | ❌ | 200 または 204 |
| **PATCH** | リソースの部分更新 | ❌ | ❌ | 200 |
| **DELETE** | リソースの削除 | ✅ | ❌ | 204（ボディなし） |

- **GET リクエストでリソースの状態を変更しない**（安全性の保証）
- **PUT / DELETE はべき等でなければならない**（同一リクエストの複数回実行で結果が同一）

### HTTP ステータスコード
- 意味的に正しいステータスコードを返す。全て 200 で返すことは禁止

| コード | 用途 | 使用場面 |
|--------|------|---------|
| **200** | 成功（ボディあり） | GET / PUT / PATCH の正常応答 |
| **201** | 作成成功 | POST でリソース作成成功時。`Location` ヘッダーに新リソースの URI を含める |
| **204** | 成功（ボディなし） | DELETE 成功時、PUT でボディ不要時 |
| **400** | 不正なリクエスト | バリデーションエラー、不正なリクエストボディ |
| **401** | 未認証 | 認証情報なし、または認証失敗 |
| **403** | 権限不足 | 認証済みだがリソースへのアクセス権限がない |
| **404** | リソース未検出 | 指定された ID のリソースが存在しない |
| **409** | 競合 | 楽観的ロックの競合、一意制約違反 |
| **422** | 処理不能 | ビジネスルール違反（バリデーションは通過するが業務的に処理不能） |
| **429** | レート制限超過 | 一定期間内のリクエスト数が上限を超過 |
| **500** | サーバーエラー | 予期しない例外。**スタックトレースをクライアントに返さない** |

---

## 2. エラーレスポンス設計

### RFC 7807 Problem Details 形式の必須化
- 全てのエラーレスポンスは **RFC 7807 Problem Details** 形式で統一する
- Spring Boot 3.2 の `ProblemDetail` クラスを活用する

```java
// ✅ RFC 7807 準拠のエラーレスポンス
{
  "type": "https://example.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "User with ID 123 was not found",
  "instance": "/api/v1/users/123"
}
```

### バリデーションエラーのレスポンス
- フィールド単位のエラー詳細を `errors` 配列に含める
- 全フィールドのエラーを**一括で返す**（1 つずつ返す逐次方式は禁止）

```java
// ✅ バリデーションエラーレスポンス
{
  "type": "https://example.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "入力内容に誤りがあります（2件）",
  "instance": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "有効なメールアドレスを入力してください",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "name",
      "message": "名前は1〜50文字で入力してください",
      "rejectedValue": ""
    }
  ]
}
```

### エラーレスポンスの禁止事項
- **スタックトレースをクライアントに返さない**（`server.error.include-stacktrace=never`）
- **内部実装の詳細を漏洩しない**（SQL エラーメッセージ、クラス名、内部パス等）
- **Whitelabel Error Page を無効化**し、`@ControllerAdvice` で統一的に処理する

```java
// ✅ @ControllerAdvice による統一例外処理
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "入力内容に誤りがあります");
        problem.setType(URI.create("https://example.com/errors/validation-failed"));
        problem.setTitle("Validation Failed");
        // フィールドエラーの詳細を追加
        return problem;
    }
}
```

---

## 3. 入力検証

### バリデーション必須化
- **全ての外部入力に対してバリデーションを実施する**
- `@Valid` / `@Validated` を Controller メソッドの引数に付与
- Bean Validation アノテーション（`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, `@Email`）を DTO / リクエストクラスに付与

```java
// ✅ 入力検証の実装例
@PostMapping
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    var user = userService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + user.id()))
                         .body(user);
}

// ✅ リクエスト DTO（レコードクラス + Bean Validation）
public record CreateUserRequest(
    @NotBlank(message = "名前は必須です")
    @Size(min = 1, max = 50, message = "名前は1〜50文字で入力してください")
    String name,

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    String email
) {}
```

### パスパラメータ・クエリパラメータの検証
- `@PathVariable` の値に対して型安全性と範囲チェックを実施する
- `@RequestParam` にデフォルト値とバリデーションを設定する

```java
// ✅ パスパラメータの検証
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(
        @PathVariable @Positive(message = "IDは正の整数を指定してください") Long id) {
    // ...
}
```

### サイズ制限
- リクエストボディのサイズ上限を設定する（`server.tomcat.max-http-form-post-size`, `spring.servlet.multipart.max-file-size`）
- コレクションパラメータに上限を設定する

---

## 4. レスポンス設計

### レスポンス DTO の分離
- エンティティクラスを直接レスポンスとして返さない。**レスポンス専用の DTO（レコードクラス推奨）を使用する**

```java
// ❌ エンティティを直接返却
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}

// ✅ レスポンス DTO を使用
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    var user = userService.findById(id);
    return ResponseEntity.ok(UserResponse.from(user));
}
```

### ページネーション
- コレクションリソースには**ページネーションを必須化**する
- 上限なしの全件取得エンドポイントは禁止
- Spring Data の `Pageable` を活用する

```java
// ✅ ページネーション対応
@GetMapping
public ResponseEntity<Page<UserResponse>> listUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    return ResponseEntity.ok(userService.findAll(pageable));
}
```

### 日時フォーマット
- 日時は **ISO 8601 形式**（`2026-03-18T14:30:00Z`）で統一する
- タイムゾーン情報を必ず含める（UTC 推奨）
- `@JsonFormat` で明示的にフォーマットを指定する

---

## 5. バージョニング

- **URI パスベース**（`/api/v1/users`）を標準方式とする
- プロジェクト内で統一した方式を使用する（混在禁止）
- バージョンアップ時は後方互換性を維持する

### 後方互換性のルール
- **フィールド追加**は許容（既存クライアントは新フィールドを無視可能）
- **フィールド削除・名前変更・型変更**は Breaking Change。新バージョン（v2）で対応する
- **必須フィールドの追加**は Breaking Change

---

## 6. べき等性とリトライ安全性

- **PUT / DELETE はべき等に設計する**（同一リクエストの複数回実行で同一結果）
- POST のリトライ安全性が必要な場合、**べき等性キー**（`Idempotency-Key` ヘッダー）の設計を検討する

```java
// ✅ べき等な PUT の例
@PutMapping("/{id}")
public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request) {
    var user = userService.update(id, request);
    return ResponseEntity.ok(UserResponse.from(user));
}
```

---

## 7. Controller の責務

### Controller にビジネスロジックを書かない
- Controller の役割は**リクエストの受付・バリデーション・レスポンスの構築のみ**
- ビジネスロジックは Service 層に委譲する

```java
// ❌ Controller にビジネスロジック
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    var totalPrice = request.items().stream()
        .mapToInt(item -> item.price() * item.quantity())
        .sum();
    if (totalPrice > 1000000) {
        throw new BusinessException("上限金額を超過しています");
    }
    // ...
}

// ✅ Service に委譲
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    var order = orderService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id()))
                         .body(OrderResponse.from(order));
}
```

### ResponseEntity の使用
- **`ResponseEntity` を使用して明示的にステータスコードを返す**。メソッドの戻り値からステータスコードが読み取れるようにする

---

## 8. セキュリティ関連（Controller 層のみ）

- 全エンドポイントに**認可アノテーション**（`@PreAuthorize` / `@Secured`）を設定する。認可チェックなしのエンドポイントは意図的であることをコメントで明記する
- **CORS 設定**で `*` ワイルドカードを使用しない。許可オリジンを明示する
- **レート制限**が必要なエンドポイント（認証、検索等）を識別し、対策を設計する

```java
// ✅ 認可アノテーションの設定
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}

// ✅ 認可不要のエンドポイントは意図を明記
@GetMapping("/health")
// 認可不要: ヘルスチェック用の公開エンドポイント
public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
}
```

---

## 9. API ドキュメント

- OpenAPI / Swagger のアノテーション（`@Operation`, `@ApiResponse`, `@Schema`）で API 仕様を記述する
- 正常系と主要な異常系の両方のレスポンス例を記載する
