---
applyTo: "**/*.sql"
---

# SQL スキーマレビュー Instructions

本 Instructions は `**/*.sql` に自動適用される。SQL ファイル（DDL、マイグレーションスクリプト等）の作成・編集時に以下のチェック観点を遵守すること。

---

## 1. 正規化

- **第 3 正規形以上**を基本とする
- 意図的な非正規化を行う場合は、**パフォーマンス上の根拠をコメントで明記**する
- 冗長なデータの保存を避ける（同一データを複数テーブルに保持しない）

```sql
-- ❌ 悪い例: 非正規化（根拠なし）
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100),  -- users テーブルと重複
    user_email VARCHAR(255)  -- users テーブルと重複
);

-- ✅ 良い例: 正規化 + 外部キー
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ✅ 良い例: 意図的な非正規化（根拠あり）
CREATE TABLE order_snapshots (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    -- 非正規化: 注文時点のユーザー名を保持（監査要件のため変更後も参照可能にする）
    user_name_at_order VARCHAR(100) NOT NULL
);
```

---

## 2. テーブル設計

### 命名規則
- テーブル名: **snake_case**、**複数形**（`users`, `order_items`）
- カラム名: **snake_case**（`created_at`, `user_id`）
- 制約名: 種類のプレフィックス付き（`pk_`, `fk_`, `uq_`, `idx_`, `ck_`）
- **データベース予約語をテーブル名・カラム名に使用しない**（`order`, `user`, `group` 等）

```sql
-- ✅ 良い例: 命名規則に準拠
CREATE TABLE order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0)
);
```

### 必須カラム
- 全テーブルに以下の監査カラムを含める

| カラム | 型 | 用途 |
|--------|-----|------|
| `created_at` | `TIMESTAMP WITH TIME ZONE` | レコード作成日時 |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | レコード最終更新日時 |
| `created_by` | `VARCHAR` / `BIGINT` | 作成者（必要に応じて） |
| `updated_by` | `VARCHAR` / `BIGINT` | 更新者（必要に応じて） |

### データ型の選択

| 用途 | 推奨型 | 禁止・非推奨 | 理由 |
|------|--------|------------|------|
| 金額・通貨 | `DECIMAL(12, 2)` / `NUMERIC` | `FLOAT`, `DOUBLE` | 丸め誤差の防止 |
| 日時 | `TIMESTAMP WITH TIME ZONE` | `VARCHAR` で日時保存 | タイムゾーン対応・ソート・比較の正確性 |
| 真偽値 | `BOOLEAN` | `INT`（0/1）, `CHAR(1)` | 意味の明確さ |
| UUID | `UUID`（対応 DB）/ `CHAR(36)` | `VARCHAR(255)` | サイズの無駄 |
| ステータス・種別 | `SMALLINT` + `CHECK` / `ENUM` | `VARCHAR` で自由記述 | 型安全性・データ整合性 |
| 大量テキスト | `TEXT` | 不必要な `VARCHAR(4000)` | サイズ制約の明確化 |

---

## 3. 制約設計

### 制約は DB 層で必ず設定する
- **「アプリケーションで制御するから DB 制約は不要」は許容しない**
- アプリケーションのバグやデータ移行時にも整合性が保たれるよう、DB 層で防御する

| 制約 | チェック内容 |
|------|-----------|
| **PRIMARY KEY** | 全テーブルに PK を定義する。サロゲートキー（自動採番）を推奨 |
| **FOREIGN KEY** | テーブル間の参照関係に外部キーを設定する。`ON DELETE` / `ON UPDATE` の動作を明示する |
| **NOT NULL** | NULL を許容する明確な理由がないカラムには `NOT NULL` を設定する |
| **UNIQUE** | ビジネス上のユニーク制約（メールアドレス、コード値等）を DB 層で保証する |
| **CHECK** | 値域の制約（`quantity > 0`, `status IN (...)` 等）を DB 層で保証する |
| **DEFAULT** | 適切なデフォルト値を設定する（`created_at DEFAULT CURRENT_TIMESTAMP` 等） |

### 外部キーの ON DELETE / ON UPDATE

| 動作 | 使用場面 | 例 |
|------|---------|-----|
| `RESTRICT`（デフォルト） | 子レコードがある親の削除を禁止 | ユーザー → 注文 |
| `CASCADE` | 親削除時に子も連鎖削除 | 注文 → 注文明細（慎重に使用） |
| `SET NULL` | 親削除時に子の FK を NULL に | 推奨カテゴリの削除時 |
| `NO ACTION` | RDBMSによりRESTRICTと同等 | — |

- **`CASCADE` は意図しない大量削除のリスクがあるため、使用時はコメントで理由を明記する**

```sql
-- ✅ 良い例: CASCADE の使用理由を明記
CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE  -- 注文削除時に明細も連鎖削除（業務要件: 注文と明細は不可分）
```

---

## 4. インデックス設計

### インデックスの基本ルール
- **WHERE 句で頻繁に使用されるカラム**にインデックスを設定する
- **JOIN 条件のカラム**（外部キー）にインデックスを設定する
- **ORDER BY / GROUP BY で使用されるカラム**にインデックスを検討する

### 複合インデックス
- カラム順序は**選択性（カーディナリティ）の高い順**に配置する
- クエリの `WHERE` 句で使用されるカラムの順序と一致させる

```sql
-- ✅ 良い例: 複合インデックス（選択性の高い順）
CREATE INDEX idx_orders_user_status ON orders (user_id, status);
-- user_id（高カーディナリティ）→ status（低カーディナリティ）の順

-- ❌ 悪い例: 逆順（低選択性が先頭）
CREATE INDEX idx_orders_status_user ON orders (status, user_id);
```

### インデックスの注意事項
- **不要なインデックスを追加しない**（INSERT / UPDATE / DELETE の性能に影響する）
- 他のインデックスに包含されるインデックスを作成しない（例: `(a)` と `(a, b)` の両方は不要）
- **外部キーカラムにはインデックスを設定する**（JOIN 性能・CASCADE DELETE 性能に影響）
- 論理削除（`deleted_at`）を使用する場合、**部分インデックス**を検討する

```sql
-- ✅ 良い例: 論理削除テーブルの部分インデックス
CREATE INDEX idx_users_email_active ON users (email) WHERE deleted_at IS NULL;
```

---

## 5. マイグレーション安全性

### 基本原則
- **全てのマイグレーションに UP / DOWN の両方を定義する**
- DOWN（ロールバック）が技術的に不可能な変更には、その理由をコメントで明記する

### 安全な操作 vs 危険な操作

| 安全な操作 | 危険な操作（要注意） |
|-----------|-------------------|
| カラム追加（`NULL` 許容 or `DEFAULT` 付き） | カラム削除 |
| テーブル追加 | テーブル削除 / テーブル名変更 |
| インデックス追加 | カラム名変更 |
| 制約の緩和（`NOT NULL` → `NULL` 許容） | データ型変更（精度低下方向） |
| `DEFAULT` 値の追加 | 制約の追加（`NULL` 許容 → `NOT NULL`） |

### 危険な操作への対処

```sql
-- ❌ 危険: カラムの直接削除（ロールバック不可）
ALTER TABLE users DROP COLUMN middle_name;

-- ✅ 安全: 段階的な削除（2 段階リリース）
-- Step 1: アプリケーションから参照を除去（このリリース）
-- Step 2: カラムを削除（次回リリースで、Step 1 の安定稼働を確認後）
ALTER TABLE users DROP COLUMN middle_name;  -- Step 2 で実行
```

```sql
-- ❌ 危険: NOT NULL 制約の直接追加（既存データに NULL があると失敗）
ALTER TABLE users ALTER COLUMN phone SET NOT NULL;

-- ✅ 安全: データ更新後に制約追加
UPDATE users SET phone = 'unknown' WHERE phone IS NULL;
ALTER TABLE users ALTER COLUMN phone SET NOT NULL;
```

### 大量データテーブルへの変更
- 大量データ（100 万行以上目安）のテーブルへの DDL は**オンライン DDL** を検討する
- `ALTER TABLE` によるテーブルロックがサービスに影響しないか確認する
- 必要に応じて、DDL の実行をメンテナンスウィンドウに計画する

### マイグレーションファイルの管理
- ファイル名にバージョン番号を含める（Flyway: `V1__create_users.sql`, `V2__add_email_index.sql`）
- **既に適用済みのマイグレーションファイルを修正しない**（新しいマイグレーションで対応する）
- マイグレーションの依存関係を考慮した順序にする

---

## 6. データ保全

### 論理削除 vs 物理削除

| 方式 | メリット | デメリット | 適用場面 |
|------|---------|----------|---------|
| **物理削除** | シンプル、ストレージ効率 | データ復旧不可 | ログデータ、一時データ |
| **論理削除**（`deleted_at`） | データ復旧可能、監査対応 | UNIQUE 制約との組合せが複雑、データ蓄積 | ユーザーデータ、取引データ |

- 論理削除を使用する場合、**UNIQUE 制約との整合性**を設計する

```sql
-- ✅ 良い例: 論理削除 + UNIQUE（アクティブレコードのみ一意）
CREATE UNIQUE INDEX uq_users_email_active ON users (email) WHERE deleted_at IS NULL;
```

### バックアップ対応
- テーブル設計時に**バックアップ・リカバリの容易さ**を考慮する
- 大量データテーブルはパーティショニングを検討する（バックアップ単位の分割）

---

## 7. パフォーマンス考慮

- **`SELECT *` を避ける**ことを前提にしたカラム設計（カラム数の過度な増加を避ける）
- BLOB / CLOB 等の大きなカラムは別テーブルに分離を検討する
- **パーティショニング**が必要なテーブル（日時ベースのログ、大量トランザクション）を識別する
- カラム数が 30 を超えるテーブルは設計の見直しを検討する

---

## 8. 禁止事項チェックリスト

| # | 禁止事項 | 重要度 | 理由 |
|---|---------|--------|------|
| 1 | 外部キー制約なしのテーブル間参照 | Critical | データ整合性の破綻リスク |
| 2 | 金額カラムに `FLOAT` / `DOUBLE` 使用 | Critical | 丸め誤差による金額不一致 |
| 3 | 日時カラムに `VARCHAR` 使用 | High | ソート・比較・タイムゾーン処理の不正確さ |
| 4 | `NOT NULL` の理由なき省略 | High | 意図しない NULL データの蓄積 |
| 5 | `CASCADE` の理由なき使用 | High | 意図しない大量連鎖削除のリスク |
| 6 | 既に適用済みのマイグレーションファイルの修正 | High | マイグレーション履歴の整合性破壊 |
| 7 | カラム削除のロールバックスクリプトなし | High | ロールバック不能な変更 |
| 8 | 外部キーカラムへのインデックス未設定 | Medium | JOIN / CASCADE 性能の劣化 |
| 9 | 監査カラム（`created_at` / `updated_at`）の不在 | Medium | 変更追跡の不能 |
| 10 | DB 予約語のテーブル名・カラム名使用 | Medium | クエリ記述の煩雑化、移植性の低下 |
