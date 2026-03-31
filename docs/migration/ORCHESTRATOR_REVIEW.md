# オーケストレーター総合レビュー結果
## 移行設計書（DESIGN.md）/ 移行計画書（PLAN.md）

**レビュー日**: 2026-03-31  
**レビュー者**: orchestrator.agent（architect / security-reviewer / dba-reviewer / qa-manager / oss-reviewer / infra-ops-reviewer / business-analyst / compliance-reviewer を統括）  
**対象文書**: `docs/migration/DESIGN.md`, `docs/migration/PLAN.md`

---

## エグゼクティブサマリー

**判定: ✅ 条件付き承認（Conditional Approval）**

初稿時点で以下の Critical / High 指摘を検出し、レビューと同時に全て修正済み。修正適用後、全 Critical 指摘がクローズされたため、移行実装フェーズへの進行を条件付きで承認する。

| 重要度 | 検出件数 | 修正済み | 残存 |
|--------|---------|---------|------|
| Critical | 2 | 2 | 0 |
| High | 6 | 6 | 0 |
| Medium | 4 | 4 | 0 |
| Low | 2 | 0 | 2（任意対応） |

---

## Critical 指摘（全て修正済み）

### C-1: thymeleaf-extras-springsecurity6 が pom.xml 依存に含まれていなかった

**担当エージェント**: OSS レビュー / セキュリティ  
**該当箇所**: PLAN.md Phase 1（pom.xml 依存一覧）  
**リスク**: `${#authentication.name}` 等の Spring Security コンテキスト参照が Thymeleaf テンプレートで実行時エラーとなる。テンプレートが全て動作不能になる Critical 障害。  
**修正内容**: `thymeleaf-extras-springsecurity6` および `thymeleaf-layout-dialect`（Tiles 置換）を依存リストに明記し、各々の用途をコメントで注記した。  
**ステータス**: ✅ 修正済み

---

### C-2: パスワードハッシュ移行 SQL がロールバック計画なしで断片的にしか記載されていなかった

**担当エージェント**: セキュリティ / DBA  
**該当箇所**: DESIGN.md §11.2, PLAN.md Phase 7  
**リスク**: `DelegatingPasswordEncoder` を導入しても DB のパスワードハッシュに `{sha256}` プレフィックスが付いていなければ全既存ユーザーがログイン不能になる。かつロールバック SQL がなければ障害時の復旧手段がない。  
**修正内容**:
- DESIGN.md §11.2 に Flyway 形式（`V2__add_password_prefix.sql`）のマイグレーション SQL とロールバック SQL を明記
- PLAN.md Phase 7 に `7-4: パスワードハッシュプレフィックス Flyway マイグレーション` を独立ステップとして追加
- ログイン成功時の BCrypt 自動アップグレードロジックの設計を明記  

**ステータス**: ✅ 修正済み

---

## High 指摘（全て修正済み）

### H-1: `@EnableMethodSecurity` / `@PreAuthorize` がセキュリティ設計に含まれていなかった

**担当エージェント**: セキュリティ / アーキテクト  
**該当箇所**: DESIGN.md §11.1, PLAN.md Phase 7  
**リスク**: URL ベースの認可のみでは、URL マッピングが変更・追加された際に認可漏れが生じる。Service レイヤーへの直接呼び出し（テスト・内部呼び出し等）では認可が機能しない。  
**修正内容**: `@EnableWebSecurity` に `@EnableMethodSecurity` を追加。PLAN.md Phase 7 に `@PreAuthorize` の実装ステップを追加。DESIGN.md §11.1 の設定コードブロックに `@EnableMethodSecurity` とセキュリティヘッダー（CSP, X-Frame-Options, HSTS）設定を追記。  
**ステータス**: ✅ 修正済み

---

### H-2: Flyway によるスキーマバージョン管理が設計になかった

**担当エージェント**: インフラ / DBA  
**該当箇所**: DESIGN.md §3.1（技術スタック）/ PLAN.md Phase 1  
**リスク**: schema.sql の単純コピーでは本番環境での増分変更が管理できない。パスワードプレフィックス追加 SQL 等の移行 SQL がバージョン管理されない。  
**修正内容**: 技術スタックに Flyway を追加。PLAN.md Phase 1 に Flyway の `db/migration/` ディレクトリ構成を示す。`application-test.properties` に `spring.flyway.enabled=false` を追加（テストでは JPA DDL を使用）。  
**ステータス**: ✅ 修正済み

---

### H-3: N+1 クエリ対策が設計文書に記載されていなかった

**担当エージェント**: DBA / アーキテクト  
**該当箇所**: DESIGN.md §8.4（JPA エンティティ設計原則）  
**リスク**: LAZY フェッチを使用すると一覧取得時に N+1 問題が発生し、商品一覧・注文一覧等でパフォーマンスが著しく低下する。  
**修正内容**: 「`@EntityGraph` または `JOIN FETCH` を使用する」「`@BatchSize(size = 50)` でコレクション関連を軽減する」を JPA 設計原則に追記。  
**ステータス**: ✅ 修正済み

---

### H-4: Actuator エンドポイントの認可設定が設計になかった

**担当エージェント**: セキュリティ / インフラ  
**該当箇所**: DESIGN.md §11.1  
**リスク**: `/actuator/**` を全公開にすると、DB 接続情報・環境変数・スレッドダンプ等が外部に漏洩する。Spring Boot Actuator はデフォルト以外のエンドポイントも存在するため、明示的な認可設定が必要。  
**修正内容**: SecurityConfig のサンプルに Actuator 認可設定（`/actuator/health`, `/actuator/info` は permitAll、その他は ADMIN 専用）を追記。  
**ステータス**: ✅ 修正済み

---

### H-5: ReturnRepository / ReturnDao が DAO・Repository 一覧から漏れていた

**担当エージェント**: ビジネスアナリスト / QA  
**該当箇所**: DESIGN.md §2.4、PLAN.md Phase 3  
**リスク**: `OrderService` が `ReturnDao` に依存しているにもかかわらず、リポジトリ一覧に `ReturnRepository` が含まれていなかった。移行時に `ReturnRepository` を実装し忘れる恐れがある。  
**修正内容**: DESIGN.md DAO 一覧に `ReturnDao`、パッケージ構成に `ReturnRepository.java` を追加。PLAN.md Phase 3 に `ReturnRepository`（3-15）を追加し、以降の番号を繰り上げ。  
**ステータス**: ✅ 修正済み

---

### H-6: Logback 設定（%X{reqId}・PII ログ制限）が設計なされていなかった

**担当エージェント**: コンプライアンス / テックリード  
**該当箇所**: DESIGN.md §12  
**リスク**: Logback の設定なしでは `%X{reqId}` が出力されず、`RequestIdFilter` の実装が無意味になる。またサービス層のログに個人情報が含まれると GDPR 等のコンプライアンス違反リスクが生じる。  
**修正内容**: DESIGN.md §12 に「Logback 設定」セクション（12.4）を新規追加。`%X{reqId}` パターンのサンプル XML を提供。PII のログ出力禁止を NOTE として明示。  
**ステータス**: ✅ 修正済み

---

## Medium 指摘（修正済み）

### M-1: 管理者ユーザー管理がチェックリストに含まれていなかった

既存の 2nd challenge で実装されていた `AdminUserController` のシナリオ（ユーザーロック・アンロック）がチェックリストに含まれていなかった。PLAN.md §13-B に「管理ユーザー一覧（ロック・アンロック操作サポートがあれば含む）」を追加。

**ステータス**: ✅ 修正済み

### M-2: テスト環境の Flyway 無効化設定が欠如していた

`application-test.properties` でテスト実行時に Flyway を有効にすると、スキーマ作成と DDL `create-drop` が競合する。`spring.flyway.enabled=false` を追記。

**ステータス**: ✅ 修正済み

### M-3: 認可チェックリストが URL ベースのみ記述されていた

`@PreAuthorize` を追加したことで、チェックリスト §13-B に「`@PreAuthorize` によるメソッドレベル認可が URL 変更後も有効か」「Actuator エンドポイントが未認証でアクセス不可能か」を追加。

**ステータス**: ✅ 修正済み

### M-4: 非機能チェックリストに Flyway 確認と PII ログ確認が欠如していた

Flyway のマイグレーション適用確認と PII ログ出力禁止確認を §13-C に追加。

**ステータス**: ✅ 修正済み

---

## Low 指摘（任意対応）

### L-1: メールテンプレートの移行が設計に記載されていない

`src/main/resources/mail/` ディレクトリにメールテンプレートが存在する可能性があるが、移行対象として明示されていない。パスワードリセットメール等の文面は Thymeleaf テンプレートに移行することを推奨するが、Freemarker や Velocity でも動作するため設計の Critical 問題ではない。

**推奨**: メールテンプレートの移行規模を確認し、必要に応じて PLAN.md Phase 6 に追加スコープとして記載する。

### L-2: StageGate ゲート定義が移行計画に含まれていない

各フェーズにゲートレビュー（architect, tech-lead 等の承認）の定義がない。大規模チームでは各フェーズ完了時に専門エージェントによるゲートレビューを実施することを推奨。

---

## 各 Agent レビュー詳細

### アーキテクト

**評価**: 設計原則は概ね適切。レイヤー依存方向（controller → service → repository）が明確に定義されている。  
**追記**: N+1 対策・Actuator 認可・Flyway 統合を修正で対応済み。`@Transactional` の伝播設定（注文確定等の複数テーブル更新）の詳細は実装フェーズで確認すること。

### セキュリティ（OWASP Top 10）

| OWASP 項目 | 評価 |
|-----------|------|
| A01: アクセス制御の不備 | ✅ URL + メソッドレベル認可設計（修正済み） |
| A02: 暗号化の失敗 | ✅ BCrypt + DelegatingPasswordEncoder（修正済み） |
| A03: インジェクション | ✅ Spring Data JPA Specification で文字列結合 SQL を禁止 |
| A04: 安全でない設計 | ✅ 多層防御・セッション固定防止・ブルートフォース対策設計あり |
| A05: セキュリティの誤設定 | ✅ Actuator 認可追加済み。CSP/HSTS ヘッダー設定済み |
| A06: 脆弱なコンポーネント | ✅ Log4j 1.x 撤廃・OWASP Dependency Check 実施予定 |
| A07: 識別・認証の失敗 | ✅ Spring Security + セッション固定攻撃防止・アカウントロック |
| A08: ソフトウェアとデータの整合性の失敗 | ⚠️ CSRF 保護設計済み。CI/CD でコード署名は別途検討 |
| A09: セキュリティログとモニタリングの失敗 | ✅ SecurityLog エンティティ維持・Logback X-Request-Id（修正済み） |
| A10: サーバーサイドリクエストフォージェリ | ✅ 外部 URL への直接フェッチ機能なし（影響なし） |

### DBA

**懸念**: `cart_items` テーブルに `cart_id + product_id` のユニーク制約インデックスが存在するため、JPA `@UniqueConstraint` で反映が必要。既存スキーマとの整合を `@DataJpaTest` でテスト実行。  
**H2/PostgreSQL 方言差異**: `MODE=PostgreSQL` の設定が PLAN.md 8-3 に記載済み。`UUID` 型と `VARCHAR(36)` の扱いに注意すること。

### QA

**評価**: テスト戦略（ユニット・スライス・統合・E2E・セキュリティ）は網羅的。カバレッジ目標（全体 70%+）は適切。  
**補足**: `@DataJpaTest` でのリポジトリテストと `@WebMvcTest` でのコントローラーテストが相互に独立していることを確認すること（コンテキストロードの混在を避ける）。

### OSS

**除去確認**:
- ✅ `struts-*` ライブラリ: 移行先 pom.xml に含まれない
- ✅ `log4j` 1.x: SLF4J + Logback に置換
- ✅ `commons-dbcp` / `commons-dbutils`: HikariCP + Spring Data JPA に置換
- ✅ `javax.*` → `jakarta.*`: Spring Boot 3.x で自動対応
- ✅ `strutstestcase`: 移行先 pom.xml に含まれない

**追加確認**:
- `springdoc-openapi-starter-webmvc-ui:2.3.x`: Spring Boot 3.2.x と互換性確認済み
- `thymeleaf-layout-dialect:3.x`: Thymeleaf 3.1.x と互換性確認済み

### インフラ・運用

**評価**: Dockerfile（JRE 21 Alpine）、Actuator（health/prometheus）、環境変数による機密情報管理が設計されている。  
**追記**: Flyway を追加したことで DB マイグレーションの再現性と監査性が向上。

### ビジネスアナリスト

全 29 Action が Controller マッピング表に記載されていることを確認。`AdminShippingMethodEditAction`（GET/POST）は `AdminShippingController` の GET（編集画面）+ PUT（更新）へ正しくマッピングされている。`ReturnRepository` の追加漏れを修正済み。

### コンプライアンス

**評価**: `SecurityLog` エンティティが移行対象に含まれている。PII のログ出力禁止が設計書に明記された（修正済み）。パスワードリセットトークンの有効期限管理（`PasswordResetToken` エンティティ）が移行対象に含まれている。

---

## 最終承認判定

| チェック | 判定 |
|---------|------|
| Critical 指摘 0 件 | ✅ |
| High 指摘 0 件 | ✅ |
| セキュリティ OWASP Top 10 対応 | ✅ |
|全 29 Action の移行計画網羅 | ✅ |
| Flyway スキーマバージョン管理 | ✅ |
| テスト戦略の完全性 | ✅ |
| 機密情報の外部化設計 | ✅ |

**判定: ✅ 条件付き承認 → Phase 0（事前準備）への進行を承認**

---

## 次のアクションアイテム

1. **実装前**: PLAN.md Phase 0 のチェックリスト（環境確認・ベースライン確定）を完了させる
2. **Phase 1 で**: `LegacySha256PasswordEncoder` の実装を最初に行い、既存ユーザーのログインが動作することを確認する
3. **Phase 2 で**: `@UniqueConstraint` 等の DB 制約を JPA エンティティに反映し、`@DataJpaTest` でスキーマ整合を最初に確認する
4. **Phase 4 で**: `@EnableMethodSecurity` を有効化し、`AdminProductService` 等の管理者サービスに `@PreAuthorize` を付与する
5. **Low 指摘 L-1**: メールテンプレートディレクトリ（`src/main/resources/mail/`）を確認し、移行対象を確定すること
