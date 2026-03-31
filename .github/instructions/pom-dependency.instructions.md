---
applyTo: "**/pom.xml"
---

# POM 依存関係管理 Instructions

本 Instructions は `**/pom.xml` に自動適用される。依存関係の追加・変更時に以下のチェック観点を遵守すること。

---

## 1. 依存関係の最小化

### 基本原則
- **不要な依存関係を追加しない**。1 つの機能のために巨大なライブラリを追加する前に、既存の依存関係で実現可能か検討する
- 使用していないライブラリが pom.xml に残っていないか確認する
- 同一機能を提供する複数ライブラリの共存を避ける（例: 複数の JSON ライブラリ、複数のログ実装）

### スコープの適切な設定
- `<scope>` を正しく設定し、不要な依存関係がランタイムに含まれないようにする

| スコープ | 用途 | 例 |
|---------|------|-----|
| `compile`（デフォルト） | 全フェーズで必要 | Spring Boot Starter, Spring AI |
| `runtime` | 実行時のみ必要（コンパイル時不要） | JDBC ドライバー、ログ実装（Logback） |
| `provided` | コンテナが提供（ビルド成果物に含めない） | Servlet API, Lombok（アノテーションプロセッサ） |
| `test` | テスト時のみ必要 | JUnit, Mockito, Spring Boot Test |

```xml
<!-- ✅ 良い例: スコープの適切な設定 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- ❌ 悪い例: テスト用ライブラリにスコープ指定なし -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <!-- scope が compile になり本番に含まれる -->
</dependency>
```

### Spring Boot スターターの活用
- 個別ライブラリの直接指定ではなく、**Spring Boot スターター**を使用する
- スターターが管理する推移的依存関係を個別に追加しない

```xml
<!-- ✅ 良い例: スターターを使用 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- ❌ 悪い例: スターターの内部ライブラリを個別指定 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>7.0.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.0</version>
</dependency>
```

---

## 2. バージョン管理

### SNAPSHOT バージョンの禁止
- **本番ブランチ（main / release）に SNAPSHOT バージョンを含めることは絶対禁止**
- `-SNAPSHOT` が付いた依存関係を検出した場合は Critical 指摘とする

```xml
<!-- ❌ Critical 違反: SNAPSHOT バージョン -->
<version>1.0.0-SNAPSHOT</version>

<!-- ✅ 良い例: リリースバージョン -->
<version>1.0.0</version>
```

### バージョンの一元管理
- バージョン番号は `<properties>` で一元管理する。依存関係内にバージョンを直接記述しない

```xml
<!-- ✅ 良い例: properties で一元管理 -->
<properties>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>

<!-- ❌ 悪い例: バージョンを直接記述 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### BOM（Bill of Materials）の活用
- Spring Boot の `spring-boot-dependencies` BOM を活用し、**個別バージョン指定を最小化**する
- BOM で管理されている依存関係にバージョンを上書き指定しない（整合性の破壊リスク）
- 追加の BOM（Spring AI、Spring Cloud 等）は `<dependencyManagement>` で管理する

```xml
<!-- ✅ 良い例: BOM で管理される依存にはバージョンを指定しない -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- バージョン指定なし: spring-boot-starter-parent で管理 -->
</dependency>

<!-- ❌ 悪い例: BOM 管理下の依存にバージョンを上書き -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>4.1.0</version>  <!-- BOM との不整合リスク -->
</dependency>
```

### バージョンレンジの禁止
- `[1.0,2.0)` のようなバージョンレンジ指定は**禁止**（ビルドの再現性が失われる）

```xml
<!-- ❌ 悪い例: バージョンレンジ -->
<version>[1.0,2.0)</version>

<!-- ✅ 良い例: 固定バージョン -->
<version>1.5.3</version>
```

---

## 3. 既知脆弱性（CVE）

### 脆弱性チェック
- 依存ライブラリに既知の脆弱性（CVE）がないか確認する
- `mvn dependency:tree` で**推移的依存関係**も含めた全依存関係を確認する
- 古いバージョンの使用を避け、定期的にアップデートする

### 脆弱性発見時の対応

| CVSS スコア | 対応 |
|---|---|
| 9.0-10.0（Critical） | **即座のバージョンアップが必須**。修正版がない場合はライブラリの除去を検討 |
| 7.0-8.9（High） | **リリース前のバージョンアップが必須** |
| 4.0-6.9（Medium） | 計画的にバージョンアップを実施 |
| 0.1-3.9（Low） | 次回アップデート時に対応 |

### 推移的依存関係の管理
- `mvn dependency:tree -Dverbose` でバージョン競合を検出する
- 推移的依存関係に脆弱性がある場合、親ライブラリのアップグレードまたは `<exclusions>` + 安全なバージョンの直接指定で対応する

```xml
<!-- ✅ 良い例: 脆弱な推移的依存の排除と安全版の直接指定 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-library</artifactId>
    <version>2.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.vulnerable</groupId>
            <artifactId>vulnerable-lib</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.vulnerable</groupId>
    <artifactId>vulnerable-lib</artifactId>
    <version>1.5.0</version>  <!-- CVE 修正済みバージョン -->
</dependency>
```

---

## 4. 依存関係の重複排除

- 同一ライブラリの異なるバージョンが共存していないか確認する
- `<exclusions>` を使用して不要な推移的依存関係を排除する
- `mvn dependency:analyze` で未使用の依存関係を検出する

```xml
<!-- ✅ 良い例: 不要な推移的依存の排除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

---

## 5. ライセンス互換性

- **GPL 系ライセンス**のライブラリは商用プロジェクトで使用前に法務確認が必要
- **ライセンス不明**のライブラリは使用禁止
- 新規ライブラリ追加時は OSS 審査（`oss-reviewer`）の対象となることを認識する

| ライセンス | 商用利用 | 注意事項 |
|-----------|---------|---------|
| MIT, Apache 2.0, BSD | ✅ 可 | 著作権表示・免責事項の記載 |
| LGPL | ⚠️ 条件付き | 動的リンクなら通常可 |
| GPL | ❌ 要法務確認 | プロジェクト全体への伝播リスク |
| SSPL, BSL | ❌ 要法務確認 | 商用利用に重大な制約 |

---

## 6. プラグイン管理

- ビルドプラグインのバージョンも `<properties>` で管理する
- `maven-compiler-plugin` の `source` / `target` / `release` が Java 21 に設定されているか確認する

```xml
<!-- ✅ 良い例: Java 21 の設定 -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>${java.version}</maven.compiler.release>
</properties>
```

---

## 7. 禁止事項チェックリスト

| # | 禁止事項 | 重要度 | 理由 |
|---|---------|--------|------|
| 1 | SNAPSHOT バージョンの本番ブランチ混入 | Critical | ビルドの再現性が失われ、テスト未実施のコードが混入するリスク |
| 2 | CVE（Critical/High）のある依存関係の放置 | Critical | セキュリティインシデントに直結 |
| 3 | バージョンレンジ指定（`[1.0,2.0)`） | High | ビルドの再現性が失われる |
| 4 | BOM 管理下の依存へのバージョン上書き | High | 依存関係の整合性が崩れる |
| 5 | ライセンス不明のライブラリの使用 | High | 法的リスク |
| 6 | テスト用依存に `<scope>test</scope>` なし | Medium | 本番ランタイムに不要なライブラリが含まれる |
| 7 | `<properties>` を使わないバージョン直接指定 | Medium | バージョン管理の分散 |
| 8 | 未使用の依存関係の放置 | Medium | イメージサイズ増大、攻撃面の拡大 |
