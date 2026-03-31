# パイプラインフロー — フルレビューパイプラインの実行フロー

> **バージョン**: 2.0  
> **更新日**: 2026-03-18

---

## 実行順序と並行度

全 13 Agent を 4 グループに分けて実行する。各グループ内の Agent は並行実行し、グループ間は順次実行する。

```
[Phase 1: 準備]
    プロジェクト構造分析 + 前回レビュー確認
        │
[Phase 2-G1: 企画・要件]
    ├── business-analyst
        │
[Phase 2-G2: 設計・実装]（最大並行度 = 7）
    ├── architect
    ├── security-reviewer
    ├── tech-lead
    ├── dba-reviewer
    ├── compliance-reviewer
    ├── oss-reviewer
    └── ux-accessibility-reviewer
        │
[Phase 2-G3: テスト]
    ├── qa-manager
    └── performance-reviewer
        │
[Phase 2-G4: デプロイ・運用]
    ├── release-manager
    ├── infra-ops-reviewer
    └── audit-reviewer
        │
[Phase 3: 集約]
    重複排除 → 競合解決 → エスカレーション集約
        │
[Phase 4: レポート生成]
    ダッシュボード + Top 10 + 技術的負債 + 差分
```

### グループ分けの根拠

| グループ | 根拠 |
|---------|------|
| **G1: 企画・要件** | 要件レベルの評価は他のレビューの前提情報となるため先行実行 |
| **G2: 設計・実装** | 最多の 7 Agent。相互に独立して実行可能。並行度を最大化 |
| **G3: テスト** | テストの評価にはコード品質（G2）の結果を参照可能なため G2 の後に実行 |
| **G4: デプロイ・運用** | 運用準備・リリース判定は全体の品質を把握した上で評価するため最後に実行 |

---

## 各 Agent のフルレビュー深度

フルレビュー時、各 Agent は以下の深度でレビューを実施する:

| Agent | フルレビュー固有のチェック |
|-------|----------------------|
| `business-analyst` | 要件と実装の乖離チェック、スコープクリープの検出 |
| `architect` | 全チェック項目 + アーキテクチャ・エロージョンの検出 |
| `security-reviewer` | 全チェック項目 + 依存関係の最終脆弱性チェック |
| `tech-lead` | 全チェック項目 + コードベース全体の技術的負債の横断評価 |
| `dba-reviewer` | 全チェック項目 + データ量増大シミュレーション |
| `compliance-reviewer` | 全チェック項目 + ベンダー契約確認 + ライセンス横断チェック |
| `oss-reviewer` | 全チェック項目 + メンテナンス状態の経年変化確認 |
| `ux-accessibility-reviewer` | 全チェック項目 + 実装のレスポンス設計方針との整合確認 |
| `qa-manager` | 全チェック項目 + テスト戦略の全体的な成熟度評価 |
| `performance-reviewer` | 全チェック項目 + システム全体のボトルネック横断分析 |
| `release-manager` | 全チェック項目 + リリースプロセス全体の成熟度評価 |
| `infra-ops-reviewer` | 全チェック項目 + 運用成熟度の総合評価 |
| `audit-reviewer` | 全チェック項目 + 内部統制の設計・運用有効性の包括評価 |

---

## 集約ルール

### 重複指摘の統合

複数の Agent が同一の問題を指摘する場合（例: `security-reviewer` と `oss-reviewer` が同一 CVE を指摘）:

1. **同一ファイル・同一行**の指摘はマージする
2. 重要度が異なる場合、**最も高い重要度**を採用する
3. 出典 Agent を**全て記録**する（「security-reviewer + oss-reviewer」）
4. 推奨対応は最も具体的な Agent の提案を採用する

### 競合解決プロトコル

矛盾する指摘がある場合、Orchestrator の競合解決ルールに従う:

| 競合パターン | 解決ルール |
|---|---|
| `architect` vs `tech-lead` | フルレビューでは `tech-lead` 優先（KISS）。ただし構造的問題は `architect` 優先 |
| `security-reviewer` vs `performance-reviewer` | `security-reviewer` 常に優先 |
| `compliance-reviewer` vs `audit-reviewer` | `compliance-reviewer` 優先。人間にエスカレーション |
| その他 | 両方の指摘を併記し「要人間判断」 |

### エスカレーション集約

各 Agent の「要人間判断」エスカレーション事項を以下の優先度で整理する:

| 優先度 | 対象 | 例 |
|--------|------|-----|
| **最優先** | 法規制・セキュリティ | `compliance-reviewer`, `security-reviewer` 由来 |
| **高優先** | アーキテクチャ・運用 | `architect`, `infra-ops-reviewer` 由来 |
| **通常** | その他 | その他の Agent 由来 |

---

## 全体判定基準

| 条件 | 判定 | 推奨アクション |
|------|------|-------------|
| Critical 0 件、High 3 件以下 | ✅ **健全** | 通常の開発を継続。Medium/Low は次スプリントで対応 |
| Critical 0 件、High 4 件以上 | ⚠️ **要改善** | High 指摘の対応計画を策定。次回レビューまでに改善 |
| Critical 1 件以上 | ❌ **要緊急対応** | Critical 指摘を即時是正。是正後に対象 Agent の再レビューを実施 |

---

## レポート保存ルール

- ファイル命名: `.github/review-reports/full-review/YYYY-MM-DD_HH-MM.md`
- 過去のレポートは**削除・修正しない**（監査証跡として保全）
- 各レポートの冒頭に全体判定を明記し、`audit-reviewer` による追跡を容易にする
