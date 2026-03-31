---
agent: "infra-ops-reviewer"
argument-hint: "対象サービス名（例: spring-ai-sample, user-service, payment-gateway）"
description: "運用手順書（Runbook）の雛形を生成する"
---

# 運用手順書（Runbook）の生成

指定されたサービスの運用手順書（Runbook）を、ミッションクリティカルな企業アプリケーション品質で生成してください。

**深夜 3 時に障害が発生しても、このランブックだけで初動対応ができる**レベルの具体性・網羅性を目指すこと。

対象サービス: **{{input}}**

---

## 生成すべきセクション

以下の全セクションを含む、完全な運用手順書を生成してください。

### 1. サービス概要

| 項目 | 内容 |
|------|------|
| サービス名 | {{input}} |
| 用途 | サービスの目的を 1〜2 文で説明 |
| 技術スタック | Java 25 / Spring Boot 4.1 / Spring AI 2.0 |
| デプロイ先 | コンテナ / クラウド / オンプレミス |
| 重要度 | Tier 1（最重要）/ Tier 2 / Tier 3 |
| SLA / SLO | 稼働率目標、レスポンスタイム目標 |

#### 依存関係マップ

| 依存先 | 種別 | 障害時の影響 | フォールバック |
|--------|------|------------|-------------|
| PostgreSQL | データベース | サービス停止 | なし（必須依存） |
| OpenAI API | 外部 AI サービス | AI 機能停止 | 縮退運転（AI 機能無効化） |
| Redis | キャッシュ | 性能劣化 | DB 直接参照にフォールバック |
| ... | ... | ... | ... |

#### ネットワーク構成

| ポート | 用途 | 公開範囲 |
|--------|------|---------|
| 8080 | アプリケーション | ロードバランサー経由 |
| 8081 | Actuator（管理用） | 内部ネットワークのみ |

### 2. 通常運用手順

#### 起動手順

```bash
# 1. 前提条件の確認
# - DB が起動していること
# - 必要な環境変数が設定されていること

# 2. アプリケーション起動
docker compose up -d app

# 3. 起動確認（ヘルスチェック）
# Readiness probe が OK になるまで待機（最大 120 秒）
timeout 120 bash -c 'until curl -sf http://localhost:8081/actuator/health/readiness; do sleep 5; done'
echo "起動完了"
```

#### 停止手順（グレースフルシャットダウン）

```bash
# 1. ロードバランサーからの除外（新規リクエストの停止）
# （LB 固有のコマンド）

# 2. 処理中リクエストの完了を待機
# server.shutdown=graceful が設定されていること
# spring.lifecycle.timeout-per-shutdown-phase=30s

# 3. アプリケーション停止
docker compose stop app

# 4. 停止確認
docker compose ps app  # Exit 0 を確認
```

#### 再起動手順

```bash
# グレースフルシャットダウン後に起動
docker compose restart app

# ヘルスチェックで復旧確認
curl -sf http://localhost:8081/actuator/health
```

#### ログ確認

```bash
# リアルタイムログ
docker compose logs -f app

# 直近のエラーログ
docker compose logs app --since 30m | grep -i error

# 特定の Trace ID でのログ検索
docker compose logs app | grep "traceId=abc123"
```

### 3. ヘルスチェック

#### ヘルスチェックエンドポイント

| エンドポイント | 目的 | 正常応答 | 異常時のアクション |
|---|---|---|---|
| `/actuator/health/liveness` | プロセスの生存確認 | `{"status":"UP"}` | コンテナ再起動 |
| `/actuator/health/readiness` | トラフィック受入可否 | `{"status":"UP"}` | ロードバランサーから除外 |
| `/actuator/health` | 全体ヘルス | `{"status":"UP"}` | 詳細を確認 |

#### 手動ヘルスチェック手順

```bash
# 1. Liveness チェック
curl -sf http://localhost:8081/actuator/health/liveness
# 期待: {"status":"UP"}

# 2. Readiness チェック
curl -sf http://localhost:8081/actuator/health/readiness
# 期待: {"status":"UP"}

# 3. 詳細ヘルス（DB 接続、外部サービス等）
curl -sf http://localhost:8081/actuator/health | jq .
# 各コンポーネントの status が UP であることを確認

# 4. メトリクス確認
curl -sf http://localhost:8081/actuator/prometheus | head -50
```

### 4. 障害対応手順

#### 障害レベルの定義

| レベル | 定義 | 応答時間 | 対応時間目標 |
|--------|------|---------|------------|
| **P1（Critical）** | サービス全停止、データ損失 | 即時 | 1 時間以内に復旧 |
| **P2（High）** | 主要機能の障害、性能劣化（SLA 違反） | 30 分以内 | 4 時間以内に復旧 |
| **P3（Medium）** | 一部機能の障害、回避策あり | 2 時間以内 | 翌営業日 |
| **P4（Low）** | 軽微な問題、ユーザー影響なし | 翌営業日 | 次回リリース |

#### 障害パターン別対応手順

##### パターン 1: アプリケーション停止（P1）

```
症状: ヘルスチェック失敗、リクエスト応答なし
```

1. **状況確認**
   ```bash
   docker compose ps app              # コンテナ状態確認
   docker compose logs app --tail 100  # 直近ログ確認
   ```

2. **原因切り分け**
   - コンテナが停止 → OOM Killer 確認: `dmesg | grep -i oom`
   - コンテナは起動中 → アプリケーション内部のデッドロック / ハング確認

3. **一次対応: 再起動**
   ```bash
   docker compose restart app
   # ヘルスチェック確認
   curl -sf http://localhost:8081/actuator/health
   ```

4. **再起動で復旧しない場合** → P1 エスカレーション

##### パターン 2: DB 接続不能（P1）

```
症状: ヘルスチェックの DB コンポーネントが DOWN、5xx エラー増加
```

1. **DB 状態確認**
   ```bash
   docker compose ps db
   docker compose logs db --tail 50
   ```

2. **コネクションプール確認**
   ```bash
   curl -sf http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq .
   curl -sf http://localhost:8081/actuator/metrics/hikaricp.connections.pending | jq .
   ```

3. **一次対応**
   - DB が停止 → DB 再起動: `docker compose restart db`
   - DB は稼働中、コネクション枯渇 → アプリケーション再起動

4. **復旧確認**
   ```bash
   curl -sf http://localhost:8081/actuator/health | jq '.components.db'
   # {"status":"UP"} を確認
   ```

##### パターン 3: メモリ枯渇（P2）

```
症状: レスポンスタイム増大、GC 頻度増加、OOMKiller によるプロセス終了
```

1. **メモリ状況確認**
   ```bash
   curl -sf http://localhost:8081/actuator/metrics/jvm.memory.used | jq .
   curl -sf http://localhost:8081/actuator/metrics/jvm.gc.pause | jq .
   ```

2. **ヒープダンプ取得**（可能な場合）
   ```bash
   # HeapDumpOnOutOfMemoryError が設定されていれば /tmp/heapdump.hprof に自動生成
   docker compose cp app:/tmp/heapdump.hprof ./heapdump_$(date +%Y%m%d_%H%M%S).hprof
   ```

3. **一次対応: 再起動**
   ```bash
   docker compose restart app
   ```

4. **再発する場合** → メモリリークの疑い。ヒープダンプを開発チームに引き渡し

##### パターン 4: ディスク満杯（P2）

```
症状: ログ出力停止、DB 書き込みエラー
```

1. **ディスク使用量確認**
   ```bash
   df -h
   du -sh /var/log/* | sort -rh | head -10
   ```

2. **一次対応: 不要ファイル削除**
   ```bash
   # 古いログファイルの削除
   find /var/log -name "*.log" -mtime +7 -delete
   # Docker のログ削除
   docker system prune -f
   ```

##### パターン 5: 外部サービス障害（P2-P3）

```
症状: 特定機能のエラー、外部 API タイムアウト
```

1. **外部サービス状態確認**
   ```bash
   curl -sf http://localhost:8081/actuator/health | jq '.components'
   # 各外部依存のステータスを確認
   ```

2. **縮退運転への切替**（フォールバックが設計されている場合）
   - 依存関係マップのフォールバック列を参照

3. **外部サービス復旧後の確認**
   - ヘルスチェックの該当コンポーネントが UP に復帰することを確認

### 5. 監視項目

#### 必須メトリクス

| カテゴリ | メトリクス | 閾値（Warning） | 閾値（Critical） | 確認コマンド |
|---------|----------|----------------|-----------------|------------|
| **リクエスト** | エラーレート | > 1% | > 5% | `actuator/metrics/http.server.requests` |
| **リクエスト** | レスポンスタイム（p95） | > 500ms | > 2000ms | `actuator/metrics/http.server.requests` |
| **JVM** | ヒープ使用率 | > 70% | > 85% | `actuator/metrics/jvm.memory.used` |
| **JVM** | GC 停止時間 | > 500ms | > 2000ms | `actuator/metrics/jvm.gc.pause` |
| **DB** | コネクションプール使用率 | > 70% | > 90% | `actuator/metrics/hikaricp.connections.active` |
| **DB** | コネクション待ち数 | > 0 | > 5 | `actuator/metrics/hikaricp.connections.pending` |
| **システム** | CPU 使用率 | > 70% | > 90% | OS コマンド / cAdvisor |
| **システム** | ディスク使用率 | > 70% | > 90% | `df -h` |

#### アラート対応ルール

| アラートレベル | 対応 | 通知先 |
|---|---|---|
| **Critical** | 即時対応。障害対応手順に従う | オンコール担当（電話） |
| **Warning** | 勤務時間内に調査 | 運用チーム（チャット） |
| **Info** | 記録のみ | ログ |

### 6. エスカレーション

#### エスカレーションフロー

```
一次対応（運用担当）
    ↓ 30 分以内に復旧不能 or P1 障害
二次対応（開発チーム）
    ↓ 1 時間以内に復旧不能 or データ損失
三次対応（アーキテクト + マネージャー）
    ↓ サービス全停止 2 時間超
経営層報告
```

#### 連絡先（テンプレート）

| 役割 | 担当者 | 連絡方法 | 備考 |
|------|--------|---------|------|
| 一次対応（オンコール） | （担当者名） | （電話番号 / チャット） | ローテーション表参照 |
| 二次対応（開発） | （担当者名） | （連絡先） | — |
| 三次対応（アーキテクト） | （担当者名） | （連絡先） | — |
| マネージャー | （担当者名） | （連絡先） | P1 障害時 |

### 7. バックアップ / リストア

#### バックアップ

| 対象 | 方式 | 頻度 | 保持期間 | 保存先 |
|------|------|------|---------|--------|
| PostgreSQL（フルバックアップ） | `pg_dump` | 日次 | 30 日 | 外部ストレージ |
| PostgreSQL（WAL アーカイブ） | 継続的 | リアルタイム | 7 日 | 外部ストレージ |
| 設定ファイル | Git 管理 | コミット時 | 永続 | Git リポジトリ |

#### リストア手順

```bash
# 1. 復旧対象の特定
# RPO（許容データ損失）: X 時間
# RTO（復旧目標時間）: Y 時間

# 2. バックアップファイルの確認
ls -la /backup/postgresql/

# 3. リストア実行
pg_restore -h localhost -U postgres -d mydb /backup/postgresql/backup_YYYYMMDD.dump

# 4. データ整合性確認
psql -h localhost -U postgres -d mydb -c "SELECT count(*) FROM users;"

# 5. アプリケーション起動・ヘルスチェック
docker compose up -d app
curl -sf http://localhost:8081/actuator/health
```

### 8. 定期メンテナンス

| 作業 | 頻度 | 手順 | 備考 |
|------|------|------|------|
| ログローテーション確認 | 週次 | ディスク使用量・ログサイズ確認 | — |
| DB バキューム | 週次 | `VACUUM ANALYZE` 実行 | 大量削除後は即時実行 |
| 証明書有効期限確認 | 月次 | TLS 証明書の有効期限チェック | 期限 30 日前にアラート |
| セキュリティパッチ確認 | 月次 | OS・ランタイムのパッチ確認 | — |
| DR 訓練 | 四半期 | バックアップからのリストアテスト | — |

### 9. ポストモーテム（障害振り返り）テンプレート

障害発生後、以下のテンプレートで振り返りを記録すること:

```markdown
## ポストモーテム — YYYY-MM-DD 障害

| 項目 | 内容 |
|------|------|
| 障害発生日時 | YYYY-MM-DD HH:MM |
| 復旧日時 | YYYY-MM-DD HH:MM |
| 影響時間 | X 時間 Y 分 |
| 影響範囲 | サービス全停止 / 一部機能停止 / 性能劣化 |
| 障害レベル | P1 / P2 / P3 / P4 |

### タイムライン
- HH:MM — 障害検知
- HH:MM — 一次対応開始
- HH:MM — エスカレーション
- HH:MM — 復旧完了

### 根本原因
...

### 対応内容
...

### 再発防止策
| # | 対策 | 担当 | 期限 |
|---|------|------|------|
| 1 | ... | ... | YYYY-MM-DD |
```

---

## 生成時の注意事項

- 全てのコマンドは**コピー&ペーストで即座に実行可能**な形式で記述すること
- 「詳しくは XX を参照」のような曖昧な参照を避け、**手順書内で完結する**記述にすること
- 障害対応手順は**判断不要で機械的に実行できる**レベルの具体性で記述すること
- 連絡先はテンプレート（プレースホルダー）で記述し、実際の情報は別途セキュアに管理すること
- Spring Boot Actuator のエンドポイント（`/actuator/health`, `/actuator/metrics`）を活用した確認手順を含めること
