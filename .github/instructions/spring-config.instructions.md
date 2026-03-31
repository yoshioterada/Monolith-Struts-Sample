---
applyTo:
  - "**/application*.properties"
  - "**/application*.yml"
---

# Spring 設定ファイル Instructions

本 Instructions は `**/application*.properties` および `**/application*.yml` に自動適用される。Spring Boot の設定ファイル作成・編集時に以下のチェック観点を遵守すること。

---

## 1. 秘密情報の外部化

### 絶対禁止事項
- **API キー、パスワード、トークン、接続文字列、秘密鍵を設定ファイルに直接記述することは絶対禁止**
- 違反は Critical 指摘

```properties
# ❌ Critical 違反: 秘密情報の直接記述
spring.datasource.password=mysecretpassword
spring.ai.openai.api-key=sk-1234567890abcdef
spring.mail.password=mailpassword123

# ✅ 良い例: 環境変数で参照
spring.datasource.password=${DB_PASSWORD}
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.mail.password=${MAIL_PASSWORD}
```

### 外部化の方法

| 方法 | 適用場面 | セキュリティレベル |
|------|---------|----------------|
| 環境変数 `${ENV_VAR}` | 標準的な外部化 | 中（プロセス一覧で見える可能性あり） |
| Docker Secrets | コンテナ環境 | 高 |
| Vault / AWS Secrets Manager | エンタープライズ | 最高 |
| `spring.config.import=configserver:` | Spring Cloud Config | 高（暗号化設定必須） |

### テスト環境の秘密情報
- テスト用の秘密情報は `application-test.properties` にのみ記載する
- **テスト用の値は本番値とは異なるダミー値**を使用する
- テスト用設定ファイルに本番の秘密情報を絶対に記載しない

```properties
# ✅ 良い例: application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.ai.openai.api-key=test-dummy-key-not-real
```

---

## 2. プロファイル分離

### プロファイル構成

| ファイル | 用途 | 含める設定 |
|---------|------|-----------|
| `application.properties` | **デフォルト（全環境共通）** | アプリケーション名、共通設定、安全なデフォルト値 |
| `application-dev.properties` | 開発環境 | ローカル DB、DEBUG ログ、Actuator 全公開 |
| `application-test.properties` | テスト環境 | インメモリ DB（H2）、テスト用ダミー設定 |
| `application-staging.properties` | ステージング環境 | ステージング DB、INFO ログ |
| `application-prod.properties` | 本番環境 | 環境変数参照、WARN ログ、Actuator 制限、セキュリティ強化 |

### プロファイル設計の原則
- **デフォルトプロファイルに本番設定を含めない**。デフォルトは安全なフォールバックとする
- **デフォルトプロファイルに秘密情報を含めない**
- デフォルトの設定は**安全側に倒す**（デバッグ無効、Actuator 最小限、エラー詳細非出力）

```properties
# ✅ 良い例: application.properties（デフォルト = 安全側）
spring.application.name=spring-ai-sample

# デフォルトは安全な設定
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-exception=false

# Actuator はデフォルトで制限
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never

# ログはデフォルトで WARN
logging.level.root=WARN
logging.level.com.example=INFO
```

```properties
# ✅ 良い例: application-dev.properties（開発向け緩和）
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.level.org.springframework.web=DEBUG

management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

server.error.include-message=always
server.error.include-binding-errors=always

spring.jpa.show-sql=true
```

---

## 3. アクチュエータ設定

### 本番環境の Actuator 制限

| エンドポイント | 本番公開 | 理由 |
|---|---|---|
| `/actuator/health` | ✅ 公開可 | ヘルスチェック用（Liveness / Readiness） |
| `/actuator/info` | ⚠️ 条件付き | ビルド情報のみ（Git コミット等の内部情報は非公開） |
| `/actuator/prometheus` | ⚠️ 条件付き | メトリクス収集用（内部ネットワークのみ公開） |
| `/actuator/env` | ❌ 非公開 | 環境変数・設定値の漏洩リスク |
| `/actuator/configprops` | ❌ 非公開 | 設定プロパティの漏洩リスク |
| `/actuator/beans` | ❌ 非公開 | Bean 構造の漏洩リスク |
| `/actuator/mappings` | ❌ 非公開 | API エンドポイント一覧の漏洩リスク |
| `/actuator/heapdump` | ❌ 非公開 | ヒープダンプの漏洩（秘密情報を含む可能性） |
| `/actuator/threaddump` | ❌ 非公開 | 内部スレッド構造の漏洩 |
| `/actuator/shutdown` | ❌ 非公開 | サービス停止の攻撃ベクトル |

```properties
# ✅ 良い例: 本番環境の Actuator 設定
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
management.endpoint.health.probes.enabled=true

# Actuator のベースパスを変更（推測を困難にする）
management.endpoints.web.base-path=/internal/manage

# Actuator の管理ポートを分離（外部ネットワークから隔離）
management.server.port=8081
```

```properties
# ❌ 悪い例: 全エンドポイント公開
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

### ヘルスチェック設定（Kubernetes / コンテナ向け）

```properties
# ✅ Liveness / Readiness プローブの有効化
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

---

## 4. ログ設定

### ログレベルの環境別設定

| 環境 | root レベル | アプリケーション | フレームワーク |
|------|-----------|----------------|-------------|
| 本番（prod） | `WARN` | `INFO` | `WARN` |
| ステージング（staging） | `INFO` | `INFO` | `WARN` |
| 開発（dev） | `INFO` | `DEBUG` | `DEBUG`（Web 層のみ） |
| テスト（test） | `WARN` | `INFO` | `WARN` |

```properties
# ✅ 良い例: 本番環境のログ設定
logging.level.root=WARN
logging.level.com.example.springaisample=INFO
logging.level.org.springframework.security=WARN

# SQL ログは本番で無効
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=OFF
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=OFF
```

### ログの禁止事項
- **`spring.jpa.show-sql=true` は本番プロファイルで禁止**（SQL がログに出力され、パラメータ値が漏洩する可能性）
- `logging.level.org.hibernate.SQL=DEBUG` は本番で禁止
- 個人情報をログに出力する設定にしない

### 構造化ログ（推奨）
- JSON 形式のログ出力を推奨する（ログ集約ツールとの統合を容易にする）

```properties
# ✅ 推奨: 構造化ログ（JSON）
logging.structured.format.console=ecs
```

---

## 5. データソース設定

### コネクションプール（HikariCP）

```properties
# ✅ 良い例: HikariCP の本番設定
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

| 設定 | 推奨値 | 説明 |
|------|-------|------|
| `maximum-pool-size` | 10-20 | 最大コネクション数（DB の最大接続数を考慮） |
| `minimum-idle` | 5 | アイドルコネクションの最小数 |
| `connection-timeout` | 30000ms | コネクション取得のタイムアウト |
| `max-lifetime` | 1800000ms | コネクションの最大寿命（DB 側タイムアウトより短く設定） |
| `leak-detection-threshold` | 60000ms | コネクションリーク検出のしきい値 |

### JPA 設定

```properties
# ✅ 良い例: 本番環境の JPA 設定
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
```

| 設定 | 本番値 | 理由 |
|------|--------|------|
| `ddl-auto` | `none` | **本番で `update` / `create` は絶対禁止**。マイグレーションツール（Flyway / Liquibase）を使用 |
| `open-in-view` | `false` | OSIV を無効化し、遅延ロードの問題を早期に検出 |
| `show-sql` | `false` | SQL ログを無効化（パラメータ漏洩防止） |

```properties
# ❌ Critical 違反: 本番で ddl-auto=update
spring.jpa.hibernate.ddl-auto=update

# ❌ 悪い例: 本番で open-in-view=true（デフォルト）
spring.jpa.open-in-view=true
```

---

## 6. サーバー設定

### エラーレスポンスの制限

```properties
# ✅ 良い例: 本番環境のエラー設定
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-exception=false
server.error.whitelabel.enabled=false
```

### グレースフルシャットダウン

```properties
# ✅ 良い例: グレースフルシャットダウン
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### リクエストサイズ制限

```properties
# ✅ 良い例: リクエストサイズ制限（DoS 防止）
server.tomcat.max-http-form-post-size=2MB
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=20MB
server.tomcat.max-swallow-size=2MB
```

---

## 7. セキュリティ関連設定

### CORS 設定

```properties
# ❌ 悪い例: ワイルドカード（全オリジン許可）
# カスタム CorsConfigurer で allowedOrigins("*") を設定

# ✅ 良い例: 明示的なオリジン指定（Java Config で設定）
# → WebMvcConfigurer または SecurityFilterChain で具体的なドメインを指定
```

### HTTPS 強制

```properties
# ✅ 良い例: HTTPS 関連設定
server.ssl.enabled=true
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
```

### セッション管理

```properties
# ✅ 良い例: セッション設定
server.servlet.session.timeout=30m
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

---

## 8. 環境別設定の差分チェック

設定ファイル編集時は、以下の設定が環境間で意図的に異なることを確認する:

| 設定 | dev | staging | prod | 検証ポイント |
|------|-----|---------|------|------------|
| `logging.level.root` | INFO | INFO | **WARN** | 本番で過剰なログを出力していないか |
| `spring.jpa.show-sql` | true | false | **false** | 本番で SQL がログに出力されていないか |
| `spring.jpa.hibernate.ddl-auto` | update | validate | **none** | 本番で自動 DDL が無効か |
| `management.endpoints.web.exposure.include` | * | health,info,prometheus | **health,info** | 本番で最小限か |
| `management.endpoint.health.show-details` | always | when-authorized | **never** | 本番で内部情報非公開か |
| `server.error.include-stacktrace` | always | never | **never** | 本番でスタックトレース非公開か |
| `server.error.include-message` | always | never | **never** | 本番でエラーメッセージ非公開か |

---

## 9. 禁止事項チェックリスト

| # | 禁止事項 | 重要度 | 理由 |
|---|---------|--------|------|
| 1 | 秘密情報（パスワード、API キー等）の直接記述 | Critical | 情報漏洩リスク |
| 2 | 本番プロファイルで `ddl-auto=update/create/create-drop` | Critical | 意図しないスキーマ変更によるデータ損失 |
| 3 | 本番プロファイルで `show-sql=true` | High | SQL パラメータ値の漏洩 |
| 4 | 本番プロファイルで `management.endpoints.web.exposure.include=*` | High | 内部情報の漏洩、攻撃面の拡大 |
| 5 | 本番プロファイルで `server.error.include-stacktrace=always` | High | スタックトレースによる内部構造漏洩 |
| 6 | デフォルトプロファイルに本番の秘密情報を含める | High | プロファイル未指定時に漏洩 |
| 7 | `management.endpoint.shutdown.enabled=true` | High | 外部からのサービス停止リスク |
| 8 | `spring.jpa.open-in-view=true`（本番） | Medium | 遅延ロードの問題隠蔽、パフォーマンスリスク |
| 9 | コネクションプール設定なし（HikariCP デフォルト依存） | Medium | 本番負荷でのコネクション枯渇 |
| 10 | `server.shutdown=immediate`（デフォルト） | Medium | 処理中リクエストの断絶 |
