---
applyTo:
  - "**/Dockerfile"
  - "**/docker-compose*.yml"
---

# Dockerfile / インフラ設定 Instructions

本 Instructions は `**/Dockerfile` および `**/docker-compose*.yml` に自動適用される。コンテナ設定の作成・編集時に以下のチェック観点を遵守すること。

---

## 1. マルチステージビルド

- ビルドステージとランタイムステージを**必ず分離**する
- ビルドツール（Maven, Gradle, ソースコード）をランタイムイメージに含めない
- 最終イメージのサイズを最小化する

```dockerfile
# ✅ 良い例: マルチステージビルド
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```dockerfile
# ❌ 悪い例: 単一ステージ（JDK + ソースコードが残る）
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests
ENTRYPOINT ["java", "-jar", "target/*.jar"]
```

### レイヤーキャッシュの最適化
- `pom.xml` と依存関係のダウンロードを**ソースコードのコピーより先に配置**する（依存関係が変わらない限りキャッシュが効く）
- 頻繁に変更されるファイル（ソースコード）はできるだけ最後のレイヤーに配置する
- 不要なファイルは `.dockerignore` で除外する

```
# ✅ .dockerignore の設定例
target/
.git/
.github/
*.md
.idea/
*.iml
```

---

## 2. ベースイメージ

- **公式の信頼できるベースイメージ**を使用する（`eclipse-temurin`, `amazoncorretto` 等）
- タグは **`latest` を使用禁止**。特定バージョンに固定する
- 可能であれば**ダイジェスト（SHA256）でイメージを固定**する（サプライチェーン攻撃の防止）
- ランタイムには **JRE イメージ**（`-jre`）を使用する。JDK は不要

```dockerfile
# ✅ 良い例: バージョン固定 + JRE
FROM eclipse-temurin:21-jre

# ⚠️ 許容: バージョン固定（ダイジェストなし）
FROM eclipse-temurin:21-jre-alpine

# ❌ 悪い例: latest タグ
FROM eclipse-temurin:latest

# ❌ 悪い例: JDK をランタイムに使用
FROM eclipse-temurin:21-jdk
```

| 選択基準 | 推奨イメージ | 備考 |
|---------|-----------|------|
| 標準環境 | `eclipse-temurin:21-jre` | 安定性重視 |
| イメージサイズ重視 | `eclipse-temurin:21-jre-alpine` | Alpine ベースで軽量。glibc 依存に注意 |
| AWS 環境 | `amazoncorretto:25` | AWS 最適化 |

---

## 3. 非 root 実行

- **`USER` 命令で非 root ユーザーを指定する**（必須）
- アプリケーションを root 権限で実行しない
- アプリケーションユーザーは最小限の権限を持つ

```dockerfile
# ✅ 良い例: 非 root ユーザーの作成と使用
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```dockerfile
# ❌ 悪い例: USER 命令なし（root で実行される）
FROM eclipse-temurin:21-jre
COPY app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. ヘルスチェック

- **`HEALTHCHECK` 命令を必ず定義する**
- Spring Boot Actuator の `/actuator/health` エンドポイントを使用する
- 適切な間隔・タイムアウト・リトライを設定する

```dockerfile
# ✅ 良い例: ヘルスチェック設定
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

| パラメータ | 推奨値 | 説明 |
|-----------|-------|------|
| `--interval` | 30s | チェック間隔 |
| `--timeout` | 10s | タイムアウト |
| `--start-period` | 60s | 初期起動猶予時間（Spring Boot の起動時間を考慮） |
| `--retries` | 3 | 失敗とみなすまでのリトライ回数 |

- **curl が含まれないイメージの場合**は `wget` または Java ベースのヘルスチェックを使用する

```dockerfile
# ✅ curl なしイメージ向け: wget を使用
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

---

## 5. JVM メモリ設定

- コンテナのメモリ制限と JVM メモリ設定を**整合させる**
- `-XX:MaxRAMPercentage` を使用してコンテナメモリに対する比率で設定する（固定値 `-Xmx` より柔軟）
- GC ログを有効化し、運用時のチューニングに備える

```dockerfile
# ✅ 良い例: JVM メモリ設定
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/heapdump.hprof"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

| 設定 | 推奨値 | 説明 |
|------|-------|------|
| `MaxRAMPercentage` | 75.0 | コンテナメモリの 75% をヒープに割り当て（残りは Metaspace, スレッドスタック等） |
| `HeapDumpOnOutOfMemoryError` | 有効 | OOM 発生時にヒープダンプを自動取得（障害分析用） |
| `UseG1GC` | 有効 | Java 21 ではデフォルト。明示的に指定しても可 |

---

## 6. セキュリティ

- **不要なパッケージをインストールしない**。`apt-get install` は最小限に
- インストール後に**パッケージマネージャーのキャッシュを削除**する
- **秘密情報（API キー、パスワード等）を Dockerfile に含めない**。ビルド引数（`ARG`）にも含めない
- ポートは**必要最小限のみ公開**する

```dockerfile
# ✅ 良い例: 最小限のポート公開
EXPOSE 8080

# ❌ 悪い例: 管理用ポートも公開
EXPOSE 8080 8081 5005 9090
```

```dockerfile
# ❌ 悪い例: 秘密情報をビルド引数で渡す
ARG DB_PASSWORD
ENV DATABASE_PASSWORD=$DB_PASSWORD
```

---

## 7. docker-compose 設定

### リソース制限
- **CPU / メモリの制限を設定する**（OOM によるホスト影響の防止）

```yaml
# ✅ 良い例: リソース制限
services:
  app:
    image: myapp:latest
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 環境変数管理
- 秘密情報は `environment` に直接書かず、`env_file` または Docker Secrets を使用する
- `env_file` は `.gitignore` に含める

```yaml
# ✅ 良い例: env_file を使用
services:
  app:
    image: myapp:latest
    env_file:
      - .env

# ❌ 悪い例: 秘密情報を直接記述
services:
  app:
    image: myapp:latest
    environment:
      - DB_PASSWORD=mysecretpassword
```

### ヘルスチェック
- docker-compose でもヘルスチェックを定義する

```yaml
# ✅ 良い例: ヘルスチェック
services:
  app:
    image: myapp:latest
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

### 依存関係管理
- `depends_on` に `condition: service_healthy` を使用してサービス起動順序を制御する

```yaml
# ✅ 良い例: ヘルスチェックベースの依存関係
services:
  app:
    depends_on:
      db:
        condition: service_healthy
  db:
    image: postgres:17
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### ネットワーク分離
- フロントエンド / バックエンド / DB 層のネットワークを分離する

```yaml
# ✅ 良い例: ネットワーク分離
services:
  app:
    networks:
      - frontend
      - backend
  db:
    networks:
      - backend

networks:
  frontend:
  backend:
    internal: true  # 外部からのアクセスを遮断
```

---

## 8. 禁止事項チェックリスト

| # | 禁止事項 | 理由 |
|---|---------|------|
| 1 | `FROM ... :latest` の使用 | ビルドの再現性が失われる |
| 2 | `USER` 命令なし（root 実行） | コンテナエスケープ時のリスク増大 |
| 3 | `HEALTHCHECK` なし | 障害検知の遅延 |
| 4 | 秘密情報の Dockerfile / docker-compose 内記述 | イメージ / リポジトリへの秘密情報漏洩 |
| 5 | 単一ステージビルド（JDK + ソースコード残存） | イメージサイズ増大、ソースコード漏洩 |
| 6 | 不必要なポートの公開 | 攻撃面の拡大 |
| 7 | `.dockerignore` なし | イメージへの不要ファイル混入 |
| 8 | リソース制限なし（docker-compose） | OOM によるホスト影響 |
