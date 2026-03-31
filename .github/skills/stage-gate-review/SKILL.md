---
name: stage-gate-review
description: "指定フェーズのステージゲートレビューを実行。Use when: Gate レビュー、フェーズ完了チェック、Go/No-Go 判定"
argument-hint: "Gate番号（1-5）を指定"
---

# stage-gate-review — ステージゲートレビュー Skill

## 目的

指定されたフェーズ（Gate 1〜5）のステージゲートレビューを実行する。  
Orchestrator Agent を通じて、対象ゲートに必要な Agent セットを呼び出し、結果を集約して **Go/No-Go 判定**を提示する。

**Fail-Safe 原則**: 判定に迷う場合は No-Go に倒す。AI の判定はあくまで「助言」であり、最終判断は人間が行う。

## 前提条件

- 対象ゲート番号（1〜5）が指定されていること
- Gate 2 以降の場合: 先行ゲートの判定記録が `.github/review-reports/` に存在すること
- レビュー対象のソースコード・ドキュメントにアクセス可能であること

## 手順

以下の手順でステージゲートレビューを実施する。

### Phase 1: 準備

1. **Gate 番号の確定**: 開発者から Gate 番号（1〜5）の指定を受ける
2. **Agent セットの決定**: [ゲート基準](./references/gate-criteria.md) の Agent セットマトリクスに基づき、呼び出す Agent を決定する
3. **先行ゲート通過確認**: Gate 2 以降の場合、先行ゲートが Go / Conditional Go（条件充足）で通過済みか確認する。No-Go のままのゲートがある場合はレビュー実行不可

### Phase 2: レビュー実行

4. **Orchestrator Agent にレビューを依頼**: Gate 番号を伝達し、Orchestrator が対象 Agent セットを呼び出す
5. **各 Agent のレビュー結果を収集**: 各 Agent は自身のゲート別レビュー深度に従ってレビューを実施
6. **Agent レビュー不完全時の対処**: Agent が結果を返せない場合、「検証不能」として記録し Fail-Safe 原則に基づき Conditional Go とする

### Phase 3: 結果集約

7. **重複指摘の統合**: 同一ファイル・同一問題の指摘をマージし、最も高い重要度を採用する
8. **競合解決プロトコルの適用**: 矛盾する指摘がある場合、[ゲート基準](./references/gate-criteria.md) の競合解決ルールに従う
9. **エスカレーション事項の集約**: 各 Agent の「要人間判断」を優先度付きで集約する

### Phase 4: 判定・レポート

10. **Go/No-Go 判定の算出**: [ゲート基準](./references/gate-criteria.md) の判定ルールに基づき判定を算出する
11. **[チェックリスト](./references/checklist-templates.md) の確認**: 対象ゲートのチェックリスト全項目の確認状況を記録する
12. **統合レポートの生成**: Orchestrator の統合レポートフォーマットで結果を出力する
13. **レポートの永続化**: `.github/review-reports/gate-{N}/YYYY-MM-DD_HH-MM.md` に保存する

### Phase 5: 再審（No-Go の場合）

14. No-Go 判定後、是正が完了したら、**No-Go の原因となった Agent のみを再実行**する（全 Agent の再実行は不要）
15. 再審結果を同一ゲートのレポートディレクトリに追加保存する（初回→是正→再審のチェーンを追跡可能に）
16. 同一ゲートで **3 回 No-Go** が続いた場合、「⚠️ プロジェクト根本課題の疑い」として人間にエスカレーションする

## 参照ドキュメント

- [ゲート基準](./references/gate-criteria.md) — Agent セット・判定ルール・競合解決プロトコル
- [チェックリストテンプレート](./references/checklist-templates.md) — 各ゲートの詳細チェック項目
