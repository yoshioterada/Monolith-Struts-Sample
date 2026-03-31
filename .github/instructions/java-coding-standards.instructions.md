---
applyTo: "**/*.java"
---

# Java コーディング規約

本 Instructions は `**/*.java` に自動適用される。全ての Java ファイルの作成・編集時に以下の規約を遵守すること。

---

## 1. 命名規則

### 基本ルール

| 対象 | 規約 | 良い例 | 悪い例 |
|------|------|--------|--------|
| クラス名 | PascalCase | `UserService`, `OrderController` | `userService`, `order_controller` |
| インターフェース名 | PascalCase（`I` プレフィックス不要） | `UserRepository` | `IUserRepository` |
| メソッド名 | camelCase（動詞で始まる） | `findById`, `createOrder` | `user`, `orderProcess` |
| 変数名 | camelCase（意味のある名前） | `userName`, `orderItems` | `x`, `tmp`, `data` |
| 定数 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` | `maxRetryCount` |
| パッケージ名 | 全て小文字 | `com.example.springaisample` | `com.example.SpringAiSample` |
| boolean 変数/メソッド | `is`, `has`, `can`, `should` プレフィックス | `isActive`, `hasPermission` | `active`, `checkPermission` |
| コレクション変数 | 複数形 | `users`, `orderItems` | `userList`, `orderItemArray` |

### 命名のアンチパターン

| アンチパターン | 問題点 | 改善例 |
|-------------|--------|--------|
| `Manager`, `Helper`, `Util` の安易な使用 | 責務が曖昧になる | 具体的な責務を表す名前に変更 |
| 1〜2 文字の変数名（`x`, `s`） | 意味が読み取れない | 意味のある名前に変更（ラムダの引数を除く） |
| 型名の繰り返し（`userList`, `nameString`） | 冗長 | `users`, `name` に簡潔化 |
| 略語の多用（`dept`, `usr`, `mgr`） | 可読性低下 | `department`, `user`, `manager` に展開 |

---

## 2. パッケージ構成

- `com.example.<プロジェクト名>` 配下に以下のパッケージを配置する
- **レイヤードアーキテクチャの依存方向を厳守**: controller → service → repository

```
com.example.springaisample/
├── controller/    # REST コントローラー（外部入力の受付、レスポンス構築）
├── service/       # ビジネスロジック
├── repository/    # データアクセス
├── model/         # エンティティ、ドメインモデル
├── dto/           # リクエスト/レスポンス DTO
├── config/        # 設定クラス
└── exception/     # カスタム例外クラス
```

- **依存方向の違反を禁止**: controller が repository を直接呼び出す、repository が controller に依存する等
- **循環依存の禁止**: パッケージ間の双方向依存は許容しない

---

## 3. Java 21 機能の活用

プロジェクトの技術スタック（Java 21）に基づき、モダン機能を積極的に活用する。

### レコードクラス（推奨）
- 不変 DTO、値オブジェクト、設定パラメータにはレコードクラスを使用する
- 従来の POJO + getter/setter/equals/hashCode をレコードクラスに置換する

```java
// ❌ 悪い例: 従来の POJO
public class UserResponse {
    private final String name;
    private final String email;
    public UserResponse(String name, String email) { ... }
    public String getName() { return name; }
    public String getEmail() { return email; }
    // equals, hashCode, toString ...
}

// ✅ 良い例: レコードクラス
public record UserResponse(String name, String email) {}
```

### パターンマッチング（推奨）
```java
// ❌ 悪い例
if (obj instanceof String) {
    String s = (String) obj;
    process(s);
}

// ✅ 良い例
if (obj instanceof String s) {
    process(s);
}
```

### シールドクラス（適用場面がある場合）
```java
// ✅ 良い例: 限定された型階層
public sealed interface PaymentResult
    permits PaymentSuccess, PaymentFailure, PaymentPending {}

public record PaymentSuccess(String transactionId) implements PaymentResult {}
public record PaymentFailure(String errorCode, String message) implements PaymentResult {}
public record PaymentPending(String referenceId) implements PaymentResult {}
```

### switch 式（推奨）
```java
// ❌ 悪い例: 旧来の switch
String label;
switch (status) {
    case ACTIVE: label = "有効"; break;
    case INACTIVE: label = "無効"; break;
    default: label = "不明"; break;
}

// ✅ 良い例: switch 式
var label = switch (status) {
    case ACTIVE -> "有効";
    case INACTIVE -> "無効";
    default -> "不明";
};
```

### テキストブロック（推奨）
```java
// ❌ 悪い例: 文字列結合
String query = "SELECT u FROM User u " +
               "WHERE u.status = :status " +
               "AND u.department = :dept";

// ✅ 良い例: テキストブロック
String query = """
    SELECT u FROM User u
    WHERE u.status = :status
    AND u.department = :dept
    """;
```

---

## 4. 例外処理

### 基本原則
- **例外の握りつぶしは絶対禁止**: `catch` ブロック内で必ずログ出力または再スローを行う
- 検査例外（Checked Exception）: 回復可能なビジネスエラーに使用
- 非検査例外（Unchecked Exception）: プログラミングエラーやシステムエラーに使用
- カスタム例外は `RuntimeException` を継承し、ドメイン固有のメッセージを含める

```java
// ❌ Critical 違反: 例外の握りつぶし
try {
    service.execute();
} catch (Exception e) {
    // 何もしない
}

// ✅ 良い例: ログ出力 + チェーン付き再スロー
try {
    service.execute();
} catch (ServiceException e) {
    log.error("サービス実行エラー: {}", e.getMessage(), e);
    throw new ApplicationException("処理に失敗しました", e);
}
```

### 例外の粒度
- `catch (Exception e)` で全例外をキャッチしない。**具体的な例外型**でキャッチする
- 再スロー時は**原因例外（cause）を保持**する: `throw new XxxException(message, e)`

### 例外階層の設計

```java
// ✅ 推奨: ドメイン固有の例外階層
public class ApplicationException extends RuntimeException {
    public ApplicationException(String message) { super(message); }
    public ApplicationException(String message, Throwable cause) { super(message, cause); }
}

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceType, Object id) {
        super("%s が見つかりません: %s".formatted(resourceType, id));
    }
}

public class BusinessRuleViolationException extends ApplicationException {
    public BusinessRuleViolationException(String rule) {
        super("業務ルール違反: %s".formatted(rule));
    }
}
```

### リソース管理
- **try-with-resources パターン**を必須とする。`finally` ブロックでの手動クローズは禁止

```java
// ❌ 悪い例: 手動クローズ
InputStream is = null;
try {
    is = new FileInputStream(file);
    // ...
} finally {
    if (is != null) is.close();
}

// ✅ 良い例: try-with-resources
try (var is = new FileInputStream(file)) {
    // ...
}
```

### `@ControllerAdvice` による統一例外ハンドリング
- グローバルな例外ハンドラーを実装し、全エンドポイントで統一されたエラーレスポンスを返す
- **スタックトレースをクライアントに露出しない**
- HTTP レスポンスには**適切なステータスコード**を返す（全て 500 にしない）

---

## 5. ログ出力

### 基本ルール
- **`System.out.println` は絶対禁止**。SLF4J（`@Slf4j`）を使用する
- パラメータは**プレースホルダー `{}` を使用**（文字列結合禁止）

```java
// ❌ Critical 違反
System.out.println("User created: " + user.getName());

// ❌ 悪い例: 文字列結合
log.info("User created: " + user.getName());

// ✅ 良い例: プレースホルダー
log.info("ユーザーを作成しました: userId={}", user.getId());
```

### ログレベルの使い分け

| レベル | 用途 | 本番環境 | 例 |
|--------|------|---------|-----|
| `ERROR` | システム障害、回復不能なエラー | 有効 | DB 接続不能、外部 API の致命的障害 |
| `WARN` | 想定外だが回復可能な状態 | 有効 | リトライ成功、フォールバック動作 |
| `INFO` | ビジネスイベント、処理の開始/完了 | 有効 | ユーザー作成、注文処理完了 |
| `DEBUG` | デバッグ情報 | **無効** | メソッド引数、中間計算結果 |
| `TRACE` | 詳細トレース | **無効** | 全メソッド呼び出し記録 |

### ログの禁止事項
- **個人情報をログに出力しない**: パスワード、トークン、クレジットカード番号、メールアドレス全文等
- **秘密情報をログに出力しない**: API キー、秘密鍵、接続文字列等
- **大量データをログに出力しない**: コレクションの全要素ダンプ、大きなリクエストボディ等

### 例外ログ
- 例外をログに記録する際は**スタックトレース全体を含める**（第 2 引数に例外オブジェクト）

```java
// ❌ 悪い例: メッセージのみ（スタックトレースなし）
log.error("処理エラー: {}", e.getMessage());

// ✅ 良い例: スタックトレースを含む
log.error("処理エラー: {}", e.getMessage(), e);
```

---

## 6. Null Safety

### 基本原則
- **`null` の直接返却を避ける**。`Optional` を活用する
- メソッド引数の `null` チェックは `Objects.requireNonNull()` を使用する
- **コレクションの代わりに `null` を返さない**。空コレクション（`List.of()`, `Collections.emptyList()`）を返す

```java
// ❌ 悪い例: null 返却
public User findUser(Long id) {
    return userRepository.findById(id).orElse(null);
}

// ✅ 良い例: Optional 返却
public Optional<User> findUser(Long id) {
    return userRepository.findById(id);
}
```

```java
// ❌ 悪い例: null コレクション返却
public List<User> findUsers(String department) {
    if (department == null) return null;
    // ...
}

// ✅ 良い例: 空コレクション返却
public List<User> findUsers(String department) {
    Objects.requireNonNull(department, "department must not be null");
    // ... 結果がなければ List.of() を返す
}
```

### Optional の使用ルール
- **Optional をフィールドに使用しない**（`Optional` は戻り値専用）
- **Optional をメソッド引数に使用しない**（オーバーロードまたは `@Nullable` を使用）
- `Optional.get()` は禁止。`orElse()`, `orElseThrow()`, `ifPresent()`, `map()` を使用する

```java
// ❌ 悪い例: Optional.get() の使用
var user = userRepository.findById(id).get();

// ✅ 良い例: orElseThrow の使用
var user = userRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User", id));
```

---

## 7. Spring Boot 規約

### DI（依存性注入）
- **コンストラクタインジェクションを必須**とする
- `@Autowired` のフィールドインジェクションは禁止

```java
// ❌ 悪い例: フィールドインジェクション
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// ✅ 良い例: コンストラクタインジェクション
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

### アノテーションの使い分け
- `@Service`: ビジネスロジック
- `@Repository`: データアクセス
- `@Controller` / `@RestController`: Web 層
- `@Component`: 上記に該当しない汎用コンポーネント（安易に使用しない）

### 設定管理
- 型安全な設定には `@ConfigurationProperties` を使用する
- `@Value` の散在は設定管理の一元性を損なうため最小限にする

---

## 8. コード構造

### メソッド設計
- 1 メソッド **30 行以下**を推奨。50 行を超える場合は分割を検討する
- メソッドのパラメータ数は **3 個以下**を推奨。4 個以上はパラメータオブジェクト（レコードクラス）への集約を検討する
- ネスト深度は **3 段階以下**。早期リターン（ガード節）パターンを活用する

```java
// ❌ 悪い例: 深いネスト
public void process(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (order.hasItems()) {
                // 処理...
            }
        }
    }
}

// ✅ 良い例: ガード節による早期リターン
public void process(Order order) {
    if (order == null) return;
    if (!order.isValid()) return;
    if (!order.hasItems()) return;
    // 処理...
}
```

### クラス設計
- 1 クラス **300 行以下**を推奨。500 行を超える場合は SRP 違反の可能性を検討する
- 単一責任原則（SRP）: 1 クラスが複数の変更理由を持たないようにする

### マジックナンバー / マジックストリングの禁止
- リテラル値は定数として切り出す

```java
// ❌ 悪い例: マジックナンバー
if (retryCount > 3) { ... }
if (status.equals("ACTIVE")) { ... }

// ✅ 良い例: 定数使用
private static final int MAX_RETRY_COUNT = 3;
if (retryCount > MAX_RETRY_COUNT) { ... }
if (status == UserStatus.ACTIVE) { ... }  // enum を使用
```

---

## 9. 禁止事項チェックリスト

| # | 禁止事項 | 重要度 | 理由 |
|---|---------|--------|------|
| 1 | `System.out.println` / `System.err.println` | Critical | SLF4J を使用すること |
| 2 | `catch (Exception e) {}` （例外の握りつぶし） | Critical | 必ずログ出力または再スローする |
| 3 | 秘密情報のハードコード（`password = "..."`, `apiKey = "..."`） | Critical | 環境変数・シークレット管理を使用 |
| 4 | 文字列結合による SQL 構築（`"SELECT ... WHERE " + param`） | Critical | SQLi 脆弱性。パラメータバインドを使用 |
| 5 | `Optional.get()` の使用 | High | `orElseThrow()` / `orElse()` 等を使用 |
| 6 | `@Autowired` フィールドインジェクション | High | コンストラクタインジェクションを使用 |
| 7 | ログへの個人情報出力 | High | マスキングまたは出力しない |
| 8 | ログでの文字列結合（`log.info("user: " + name)`） | Medium | プレースホルダー `{}` を使用 |
| 9 | `null` 返却（コレクション型） | Medium | 空コレクションを返す |
| 10 | `new Thread()` の直接使用 | Medium | Virtual Thread / スレッドプールを使用 |
