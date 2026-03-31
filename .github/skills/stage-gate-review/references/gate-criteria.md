# ゲート基準 — ステージゲート別 Agent セットと判定基準

> **バージョン**: 2.0  
> **更新日**: 2026-03-18

---

## 1. ゲート別 Agent セットマトリクス

Orchestrator は、指定されたゲートに応じて以下の Agent セットを呼び出す。**Agent のスキップは許容しない**。

| ゲート | 呼び出す Agent | 判定基準 | 設計根拠 |
|---|---|---|---|
| **Gate 1: 企画承認** | `business-analyst`, `compliance-reviewer` | 要件の網羅性・明確さ、早期の法規制リスク評価 | 企画段階で法規制リスクを早期発見（Shift Left） |
| **Gate 2: 設計承認** | `architect`, `security-reviewer`, `compliance-reviewer`, `infra-ops-reviewer`, `ux-accessibility-reviewer` | アーキテクチャ適切性、セキュリティ設計、コンプライアンス、運用性、UX | 設計段階でセキュリティ・運用性を組み込む |
| **Gate 3: 実装完了** | `tech-lead`, `security-reviewer`, `oss-reviewer`, `dba-reviewer`, `compliance-reviewer` | コード品質、セキュリティ、OSS 安全性、DB 適切性、法規制準拠 | 最多 Agent の中核ゲート。実装品質を全方位で確認 |
| **Gate 4: テスト完了** | `qa-manager`, `performance-reviewer`, `security-reviewer`, `audit-reviewer` | テスト品質、性能、セキュリティテスト、監査準拠 | テスト完了判定にプロセス監査を含める |
| **Gate 5: リリース承認** | `release-manager`, `infra-ops-reviewer`, `security-reviewer`, `audit-reviewer` | リリース準備、運用性、セキュリティ最終確認、監査最終確認 | CISO 最終承認相当 + 運用準備 + 監査 |

---

## 2. 判定ルール

| 条件 | 判定 | 次のアクション |
|------|------|-------------|
| 全 Agent が Pass | ✅ **Go** | 次フェーズへ進行許可 |
| Critical 指摘 1 件以上 | ❌ **No-Go** | 是正後に再審を実施 |
| High 指摘のみ（Critical なし） | ⚠️ **Conditional Go** | 人間が受容/是正を判断 |
| Medium/Low のみ | ✅ **Go with Notes** | 改善事項を記録し次スプリントで対応 |
| Agent レビュー不完全 | ⚠️ **Conditional Go** | 不完全な観点を明示し人間が判断 |

### 自動 No-Go 条件（人間による覆し不可）

| # | 条件 | 理由 |
|---|------|------|
| 1 | 先行ゲートが No-Go のまま未是正 | プロセスのバイパス |
| 2 | Critical セキュリティ指摘が未是正 | 悪用可能な脆弱性のまま次フェーズへ進行 |
| 3 | SNAPSHOT バージョンの混入（Gate 3以降） | ビルドの再現性が保証されない |

---

## 3. 競合解決プロトコル

複数の Agent が矛盾する指摘を出した場合の解決ルール:

| 競合パターン | 解決ルール | 根拠 |
|---|---|---|
| `architect` vs `tech-lead` | Gate 2 では `architect` 優先。Gate 3 では `tech-lead` 優先（KISS） | フェーズに応じた判断主体の選択 |
| `security-reviewer` vs `performance-reviewer` | `security-reviewer` **常に優先** | 安全性 > 性能 |
| `compliance-reviewer` vs `audit-reviewer` | `compliance-reviewer` 優先。人間にエスカレーション | 法規制 > 監査要件 |
| `architect` vs `dba-reviewer` | データアクセスパターンは `architect`、SQL/インデックスは `dba-reviewer` | 責任範囲の分離 |
| その他 | 両方の指摘を併記し「要人間判断」 | — |

---

## 4. 先行ゲート通過確認ルール

| Gate 2 以降の先行ゲート状況 | 対応 |
|---|---|
| 全て Go で通過済み | 正常に進行 |
| Conditional Go（条件充足済み） | 条件充足のエビデンスを確認の上進行 |
| Conditional Go（条件未充足） | **No-Go**。未充足条件の是正を要求 |
| No-Go のまま | **No-Go**。先行ゲートの是正・再審を要求 |
| 判定記録不在 | **No-Go**。プロセス違反として Critical 指摘 |

---

## 5. 再審プロトコル

| ルール | 内容 |
|------|------|
| 再審対象 | No-Go の原因となった Agent のみを再実行（全 Agent 再実行不要） |
| 是正確認 | 前回の Critical/High 指摘に対する是正が実施されているかを優先確認 |
| 記録 | 再審結果は同一ゲートのディレクトリに時刻付きで追加保存 |
| 上限 | 同一ゲートで 3 回 No-Go → 「⚠️ プロジェクト根本課題の疑い」としてエスカレーション |
