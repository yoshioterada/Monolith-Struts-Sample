# SkiShop Application

SkiShop は Java 5 / Struts 1.3 から **Java 21 / Spring Boot 3.2** へ移行したスキー用品 EC サイトです。

---

## 技術スタック

| 項目 | バージョン |
|------|---------|
| Java | 21 (LTS) |
| Spring Boot | 3.2.12 |
| Spring Security | 6.2.x |
| Spring Data JPA | 3.2.x |
| Thymeleaf | 3.1.x |
| Flyway | 10.x |
| PostgreSQL | 15 |
| Lombok | 1.18.x |

---

## ローカル起動（Docker Compose）

### 前提条件

- Docker Desktop 24.0 以上
- JDK 21（ビルドのみ。Docker ビルドでは JDK イメージを利用）

### 起動手順

```bash
# 1. プロジェクトのルートディレクトリに移動
cd appmod-migrated-java21-spring-boot-3rd

# 2. コンテナ起動（PostgreSQL + SkiShop App + MailHog）
docker compose up -d

# 3. 起動確認（全サービスが healthy になるまで待機）
docker compose ps

# 4. ブラウザでアクセス
# アプリ: http://localhost:8080
# MailHog（メール確認）: http://localhost:8025
# ヘルスチェック: http://localhost:8080/actuator/health

# 5. 停止
docker compose down
```

### 初期データ（Flyway シード）

起動時に Flyway マイグレーションが自動実行されます:
- `V1__initial_schema.sql` — スキーマ作成
- `V2__add_password_prefix.sql` — パスワードハッシュ移行
- `R__seed_data.sql` — テストデータ投入

### テスト用ログインアカウント

| ロール | メールアドレス | パスワード | 用途 |
|--------|--------------|-----------|------|
| 一般ユーザー | `user@example.com` | `P@ssw0rd!` | 商品閲覧・カート・注文・マイページ |
| 管理者 | `admin@example.com` | `Admin123!` | 上記 + `/admin/**` 管理画面（商品・注文・クーポン管理） |

---

## ローカル起動（Maven）

### 前提条件

- JDK 21
- PostgreSQL 15 が `localhost:5432` で起動済み
  - DB 名: `skishop`、ユーザー: `skishop`、パスワード: 環境変数で設定

### 環境変数設定

```bash
export DB_URL=jdbc:postgresql://localhost:5432/skishop
export DB_USERNAME=skishop
export DB_PASSWORD=your_password
export MAIL_HOST=localhost
export MAIL_PORT=1025
export MAIL_USERNAME=
export MAIL_PASSWORD=
```

### 開発プロファイルで起動

```bash
export JAVA_HOME=/path/to/jdk-21
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

アプリが `http://localhost:8080` で起動します。

---

## テスト実行

```bash
# 全テスト実行（H2 in-memory DB 使用）
./mvnw clean test

# カバレッジレポートを含むフル検証
./mvnw clean verify

# カバレッジレポートの場所
open target/site/jacoco/index.html
```

### テスト構成

| 種別 | アノテーション | 対象 |
|------|------------|------|
| Unit Test | JUnit 5 + Mockito | Service, Util |
| Web スライステスト | `@WebMvcTest` | Controller |
| DB スライステスト | `@DataJpaTest` | Repository（H2 + MODE=PostgreSQL） |
| 統合テスト | `@SpringBootTest` | 全レイヤー |
| セキュリティテスト | `@WithMockUser` | 認証/認可 |

カバレッジ目標: Service **80%+** / Controller **70%+** / 全体 **70%+**

---

## 環境変数一覧

| 変数名 | 説明 | 必須 | デフォルト |
|--------|------|------|---------|
| `DB_URL` | PostgreSQL 接続 URL | ✅ | — |
| `DB_USERNAME` | DB ユーザー名 | ✅ | — |
| `DB_PASSWORD` | DB パスワード | ✅ | — |
| `MAIL_HOST` | SMTP ホスト | ✅ | — |
| `MAIL_PORT` | SMTP ポート | — | `587` |
| `MAIL_USERNAME` | SMTP ユーザー名 | — | — |
| `MAIL_PASSWORD` | SMTP パスワード | — | — |
| `SPRING_PROFILES_ACTIVE` | Spring プロファイル | — | `dev` |

---

## プロファイル一覧

| プロファイル | 用途 | DB | ログ |
|------------|------|----|----|
| `dev` | ローカル開発 | PostgreSQL（ローカル） | INFO |
| `test` | テスト | H2 in-memory | DEBUG |
| `staging` | ステージング | PostgreSQL | INFO |
| `prod` | 本番 | PostgreSQL（環境変数必須） | WARN |

---

## Docker イメージのビルド（単体）

```bash
# JAR ファイルをビルド
./mvnw clean package -DskipTests

# Docker イメージをビルド
docker build -t skishop-app:latest .

# コンテナ起動（PostgreSQL が別途起動済みの場合）
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/skishop \
  -e DB_USERNAME=skishop \
  -e DB_PASSWORD=your_password \
  -e MAIL_HOST=localhost \
  skishop-app:latest

# 起動確認
curl http://localhost:8080/actuator/health
```

---

## API ドキュメント（Swagger UI）

開発・ステージング環境では Swagger UI が利用可能です:

```
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

---

## セキュリティ注意事項

- `application-prod.properties` には機密情報を直接記述しないこと。環境変数を使用すること。
- `.env` ファイルに機密情報を記述する場合は `.gitignore` に必ず追加すること。
- 本番環境では `SPRING_PROFILES_ACTIVE=prod` を設定し、スタックトレースが外部に漏れないことを確認すること。

---

## 移行概要

| 移行元 | 移行後 |
|--------|--------|
| Java 5 | Java 21 |
| Struts 1.3.10 | Spring Boot 3.2.12 |
| Tomcat 6（外部） | 組み込み Tomcat |
| WAR パッケージ | JAR パッケージ |
| JDBC / 手動 DAO | Spring Data JPA |
| JSP + Tiles | Thymeleaf + Layout Dialect |
| Acegi Security | Spring Security 6 |
| Log4j 1.x | SLF4J + Logback |
| SHA-256 パスワード | BCrypt（DelegatingPasswordEncoder で段階移行） |

詳細は [docs/migration/DESIGN.md](../docs/migration/DESIGN.md) を参照。
