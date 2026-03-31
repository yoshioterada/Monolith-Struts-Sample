---
agent: "architect"
argument-hint: "対象 API エンドポイント（例: POST /api/v1/users, GET /api/v1/orders/{id}）"
description: "API 仕様書の雛形を生成する"
---

# API 仕様書の生成

指定された API エンドポイントの仕様書雛形を、ミッションクリティカルな企業アプリケーション品質で生成してください。

対象エンドポイント: **{{input}}**

---

## 生成すべきセクション

以下の全セクションを含む、完全な API 仕様書を生成してください。

### 1. エンドポイント定義

| 項目 | 内容 |
|------|------|
| **HTTP メソッド** | GET / POST / PUT / PATCH / DELETE |
| **URI パス** | `/api/v1/...`（バージョニング付き） |
| **概要** | エンドポイントの目的を 1〜2 文で説明 |
| **べき等性** | べき等 / 非べき等 |
| **安全性** | 安全（状態変更なし） / 非安全 |

### 2. 認証・認可

- **認証方式**: Bearer Token (JWT) / Session / API Key 等
- **必要なロール / 権限**: `@PreAuthorize` で使用するロール・権限を明記
- **オブジェクトレベル認可**: パスパラメータの ID に対するオーナーシップチェックの要否（IDOR 防止）

### 3. リクエスト仕様

以下を全て含めること:

- **パスパラメータ**: 名前、型、必須/任意、バリデーションルール、説明
- **クエリパラメータ**: 名前、型、必須/任意、デフォルト値、バリデーションルール、説明
- **リクエストヘッダー**: 必須ヘッダー（`Authorization`, `Content-Type`, `Accept-Language`, `Idempotency-Key` 等）
- **リクエストボディ**（該当する場合）:
  - JSON スキーマ（フィールド名、型、必須/任意、制約、説明）
  - Bean Validation アノテーション（`@NotBlank`, `@Size`, `@Email` 等）を明記
  - リクエスト例（正常な入力の JSON サンプル）

```
リクエストボディ例:
{
  "name": "田中太郎",
  "email": "tanaka@example.com"
}
```

### 4. レスポンス仕様

以下の全ステータスコードについて定義すること:

#### 成功レスポンス
- **200 OK**: データ返却あり（GET / PUT / PATCH）
- **201 Created**: リソース作成成功（POST）。`Location` ヘッダーに新リソースの URI を含める
- **204 No Content**: ボディなし成功（DELETE）

各成功レスポンスに以下を含める:
- レスポンスボディの JSON スキーマ（フィールド名、型、説明）
- レスポンス例（JSON サンプル）
- レスポンスヘッダー（`Location`, `X-Request-Id` 等）

#### エラーレスポンス（RFC 7807 Problem Details 形式）
以下の各エラーパターンについて、RFC 7807 形式のレスポンス例を生成すること:

| ステータス | 条件 | Problem Details の `type` 例 |
|-----------|------|----------------------------|
| **400 Bad Request** | バリデーションエラー。フィールド別エラー詳細（`errors` 配列）を含める | `https://example.com/errors/validation-failed` |
| **401 Unauthorized** | 認証情報なし / 認証失敗 | `https://example.com/errors/unauthorized` |
| **403 Forbidden** | 認証済みだが権限不足 | `https://example.com/errors/forbidden` |
| **404 Not Found** | 指定されたリソースが存在しない | `https://example.com/errors/not-found` |
| **409 Conflict** | 楽観的ロック競合 / 一意制約違反 | `https://example.com/errors/conflict` |
| **422 Unprocessable Entity** | ビジネスルール違反（バリデーションは通過するが業務的に処理不能） | `https://example.com/errors/business-rule-violation` |
| **429 Too Many Requests** | レート制限超過（該当する場合） | `https://example.com/errors/rate-limit-exceeded` |
| **500 Internal Server Error** | サーバー内部エラー（スタックトレースを含めない） | `https://example.com/errors/internal-error` |

バリデーションエラーのレスポンス例は以下の形式で生成すること:

```json
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
    }
  ]
}
```

### 5. ページネーション（コレクションリソースの場合）

- ページネーション方式（カーソルベース / オフセットベース）
- クエリパラメータ（`page`, `size`, `sort`）の仕様
- レスポンスのページネーションメタデータ

### 6. レート制限（該当する場合）

- レート制限の有無と上限値
- `Retry-After` ヘッダーの仕様

### 7. べき等性設計（POST の場合）

- `Idempotency-Key` ヘッダーの要否
- 重複リクエスト時の振る舞い

### 8. セキュリティ考慮事項

- 入力バリデーションの要件（ホワイトリスト検証、サイズ制限）
- 出力に含めてはならない情報（内部 ID、スタックトレース、DB カラム名等）
- OWASP Top 10 の関連リスク（該当する場合）

### 9. 実装ガイド

以下の実装コードの雛形を生成すること:

- **Controller クラス**（`@RestController`, `@Valid`, `@PreAuthorize` 付き）
- **リクエスト DTO**（レコードクラス + Bean Validation アノテーション）
- **レスポンス DTO**（レコードクラス）
- **例外ハンドラー**（`@ControllerAdvice` での RFC 7807 対応）

---

## 生成時の注意事項

- 日時フォーマットは **ISO 8601**（`2026-03-18T14:30:00Z`）で統一すること
- フィールド命名は **camelCase** で統一すること
- レスポンスにエンティティクラスを直接使用せず、**DTO（レコードクラス）** を使用すること
- エラーメッセージは**ユーザーフレンドリーな日本語**で記述すること（技術用語を避ける）
- エラーレスポンスに**スタックトレースや内部情報を含めない**こと
