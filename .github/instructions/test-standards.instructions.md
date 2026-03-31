---
applyTo:
  - "**/*Test.java"
  - "**/*Tests.java"
---

# テスト規約 Instructions

本 Instructions は `**/*Test.java` および `**/*Tests.java` に自動適用される。テストコードの作成・編集時に以下の規約を遵守すること。

---

## 1. テスト命名

### 命名パターン
- **`should_期待結果_when_条件`** の命名パターンを推奨する
- テストメソッド名から「何を」「どの条件で」「どうなることを期待するか」が明確に読み取れること
- `@DisplayName` で日本語の説明を併記することを推奨する

```java
// ✅ 良い例: 命名パターンに準拠
@Test
@DisplayName("有効なIDが指定された場合、ユーザーを返す")
void should_returnUser_when_validIdProvided() { ... }

@Test
@DisplayName("存在しないIDが指定された場合、ResourceNotFoundExceptionをスローする")
void should_throwResourceNotFoundException_when_userNotFound() { ... }

@Test
@DisplayName("null名前で作成を試みた場合、バリデーションエラーとなる")
void should_failValidation_when_nameIsNull() { ... }
```

```java
// ❌ 悪い例: 何をテストしているか不明
@Test
void test1() { ... }

@Test
void testUser() { ... }

@Test
void findById() { ... }  // テスト対象メソッド名をそのまま使用
```

---

## 2. AAA パターン（Arrange-Act-Assert）

- テストメソッドは **3 つのセクションに明確に分離**する
- セクション間は空行で区切り、コメント（`// Arrange`, `// Act`, `// Assert`）を付ける

| セクション | 役割 | 内容 |
|-----------|------|------|
| **Arrange** | 準備 | テストデータの作成、モックのセットアップ |
| **Act** | 実行 | テスト対象メソッドの呼び出し（**1 メソッド呼び出しのみ**） |
| **Assert** | 検証 | 結果の検証、例外の検証、モック呼び出しの検証 |

```java
// ✅ 良い例: AAA パターン
@Test
@DisplayName("有効なIDが指定された場合、ユーザーを返す")
void should_returnUser_when_validIdProvided() {
    // Arrange
    var expectedUser = new User(1L, "test");
    when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

    // Act
    var result = userService.findById(1L);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("test");
}
```

```java
// ❌ 悪い例: セクション混在
@Test
void testFindUser() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "test")));
    var result = userService.findById(1L);
    assertThat(result).isPresent();
    when(userRepository.findById(2L)).thenReturn(Optional.empty());  // Arrange が混在
    var result2 = userService.findById(2L);
    assertThat(result2).isEmpty();  // 複数の Act-Assert が混在
}
```

---

## 3. カバレッジ基準

### 目標値
- **分岐カバレッジ 80% 以上**を必須とする
- **全パブリックメソッド**に対して単体テストを作成する

| カバレッジ率 | 判定 |
|---|---|
| 80% 以上 | ✅ 基準達成 |
| 60-79% | ⚠️ 基準未達（テスト追加が必要） |
| 60% 未満 | ❌ 深刻な不足 |

### 優先的にカバーすべきコード
- Service 層のビジネスロジック
- バリデーションロジック
- エラーハンドリング・例外処理パス
- 条件分岐（if / switch）の全分岐

---

## 4. 異常系テストの必須化

- **異常系テストは正常系と同等以上のテストケース数**を作成する
- 以下の異常パターンを網羅すること

### 入力バリデーション

```java
// ✅ 必須: null / 空文字 / 境界値のテスト
@Test
void should_throwException_when_nameIsNull() { ... }

@Test
void should_throwException_when_nameIsEmpty() { ... }

@Test
void should_throwException_when_nameExceedsMaxLength() { ... }

@Test
void should_succeed_when_nameIsExactlyMaxLength() { ... }  // 境界値
```

### 外部サービス障害

```java
// ✅ 必須: 外部依存の障害テスト
@Test
void should_throwServiceException_when_repositoryThrowsDataAccessException() {
    // Arrange
    when(userRepository.findById(anyLong()))
        .thenThrow(new DataAccessException("DB connection failed") {});

    // Act & Assert
    assertThatThrownBy(() -> userService.findById(1L))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining("ユーザー取得に失敗しました");
}
```

### 境界値テスト

| データ型 | テストすべき値 |
|---------|-------------|
| **数値** | 0, 1, -1, `Integer.MAX_VALUE`, `Integer.MIN_VALUE`, 上限値, 上限値+1 |
| **文字列** | `null`, `""`, `" "`（空白のみ）, 最大長, 最大長+1, 特殊文字 |
| **コレクション** | `null`, 空リスト, 1 件, 大量件数 |
| **日付** | `null`, 過去日, 未来日, 閏年 2/29, 月末 |

---

## 5. アサーション品質

### AssertJ の使用を推奨
- JUnit の `assertEquals` / `assertTrue` よりも **AssertJ の `assertThat`** を優先する（エラーメッセージの可読性）

```java
// ❌ 非推奨: JUnit の基本アサーション
assertTrue(result != null);
assertEquals("test", result.getName());
assertTrue(result.getItems().size() == 3);

// ✅ 推奨: AssertJ の流暢なアサーション
assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("test");
assertThat(result.getItems()).hasSize(3);
```

### 具体的なアサーション

```java
// ❌ 悪い例: 曖昧なアサーション
assertThat(result).isNotNull();  // 値が null でないことしか検証しない

// ✅ 良い例: 具体的な値の検証
assertThat(result.getName()).isEqualTo("田中太郎");
assertThat(result.getEmail()).isEqualTo("tanaka@example.com");
assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
```

### 例外テスト
- 例外の型だけでなく、**メッセージ内容も検証**する

```java
// ✅ 良い例: 例外の型 + メッセージの検証
assertThatThrownBy(() -> userService.findById(999L))
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessageContaining("User が見つかりません: 999");
```

### コレクションのアサーション

```java
// ✅ 良い例: コレクションの内容検証
assertThat(users)
    .hasSize(2)
    .extracting(User::getName)
    .containsExactly("Alice", "Bob");
```

---

## 6. テストの独立性と信頼性

### テストの独立性
- 各テストは**単独で実行可能**であること。他のテストの実行結果に依存しない
- テスト間で**共有状態を持たない**。テストごとに `@BeforeEach` でセットアップする
- テストの実行順序に依存しない

### フレイキーテスト（不安定なテスト）の禁止
- 実行のたびに結果が変わるテストは**テストなしと同等に危険**

| 問題パターン | 原因 | 対策 |
|---|---|---|
| 時刻依存 | `LocalDateTime.now()` の使用 | `Clock` をインジェクションし、テストで固定 |
| 乱数依存 | `Random` の使用 | シード値を固定、または入力をパラメータ化 |
| 非同期タイミング | `Thread.sleep()` でのタイミング調整 | `Awaitility` ライブラリ / `CompletableFuture` の直接テスト |
| 外部サービス依存 | 実際の DB / API への接続 | モック / テストコンテナを使用 |
| 実行順序依存 | 共有状態の汚染 | `@BeforeEach` での初期化 |

```java
// ❌ 悪い例: 時刻依存テスト
@Test
void should_beExpired_when_createdOneHourAgo() {
    var token = new Token(LocalDateTime.now().minusHours(1));  // 実行時刻に依存
    assertThat(token.isExpired()).isTrue();
}

// ✅ 良い例: Clock による時刻制御
@Test
void should_beExpired_when_createdOneHourAgo() {
    var fixedClock = Clock.fixed(Instant.parse("2026-03-18T12:00:00Z"), ZoneId.of("UTC"));
    var token = new Token(LocalDateTime.now(fixedClock).minusHours(1), fixedClock);
    assertThat(token.isExpired()).isTrue();
}
```

### 常に Pass するテストの禁止
- アサーションなしのテスト、`assertTrue(true)` のテストは **Critical 違反**

```java
// ❌ Critical 違反: アサーションなし（何も検証していない）
@Test
void testCreateUser() {
    userService.create(new CreateUserRequest("test", "test@example.com"));
    // アサーションなし
}
```

---

## 7. モック / スタブの適切な使用

### 外部依存のモック化
- データベース、外部 API、メッセージキュー等の**外部依存はモック / スタブで分離**する
- Mockito を使用する

```java
// ✅ 良い例: モックによる外部依存の分離
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    void should_returnUser_when_exists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "test")));

        // Act
        var result = userService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        verify(userRepository).findById(1L);
    }
}
```

### モックの過剰使用の禁止
- テスト対象のクラス自体をモックしない（モックするのは**依存先**のみ）
- モックが 5 つ以上必要な場合、テスト対象クラスの**責務過多**（SRP 違反）を疑う

---

## 8. テストデータ管理

### テストデータの原則
- テストデータは**テストメソッド内で完結**させる
- **本番データ（実在するメールアドレス、電話番号等）をテストコードに含めない**
- パラメタライズドテストで複数パターンを効率的にテストする

```java
// ✅ 良い例: パラメタライズドテスト
@ParameterizedTest
@ValueSource(strings = {"", " ", "   "})
@DisplayName("空白文字列の場合、バリデーションエラーとなる")
void should_failValidation_when_nameIsBlank(String invalidName) {
    var request = new CreateUserRequest(invalidName, "test@example.com");
    var violations = validator.validate(request);
    assertThat(violations).isNotEmpty();
}

@ParameterizedTest
@CsvSource({
    "abc, true",
    "ab, false",
    "'', false"
})
@DisplayName("名前の長さに応じたバリデーション結果")
void should_validateName_correctly(String name, boolean expectedValid) { ... }
```

---

## 9. テストの構造

### テストクラスとプロダクションコードの対応
- テストクラスは対象クラスと**同一パッケージ構成**に配置する
- テストクラス名は `対象クラス名 + Test`（例: `UserService` → `UserServiceTest`）

### `@Disabled` テストの管理
- `@Disabled` テストには**理由と対応予定**をコメントで明記する
- 理由なき `@Disabled` は Medium 指摘

```java
// ❌ 悪い例: 理由なし
@Disabled
@Test
void should_sendEmail() { ... }

// ✅ 良い例: 理由と対応予定あり
@Disabled("メールサーバーのテスト環境構築待ち。2026-04 対応予定。Issue #42")
@Test
void should_sendEmail() { ... }
```

---

## 10. 禁止事項チェックリスト

| # | 禁止事項 | 重要度 | 理由 |
|---|---------|--------|------|
| 1 | アサーションなしのテスト | Critical | 何も検証していない（テストとして無意味） |
| 2 | `assertTrue(true)` / `assertThat(true).isTrue()` | Critical | 常に Pass する偽テスト |
| 3 | テスト内の本番個人情報 | Critical | セキュリティ・コンプライアンス違反 |
| 4 | `Thread.sleep()` によるタイミング調整 | High | フレイキーテストの原因 |
| 5 | 外部 DB / API への直接接続（モック不使用） | High | テストの再現性・独立性の破壊 |
| 6 | テスト間の共有状態（static 変数等） | High | テストの実行順序依存 |
| 7 | 理由なきの `@Disabled` / `@Ignore` | Medium | テストカバレッジのサイレントな低下 |
| 8 | 例外テストで型のみ検証（メッセージ未検証） | Medium | 間違った例外の検出漏れ |
| 9 | 1 テストメソッド内の複数 Act-Assert | Medium | テスト失敗時の原因特定が困難 |
| 10 | モック 5 つ以上のテスト | Medium | テスト対象クラスの SRP 違反の疑い |
