#!/usr/bin/env bash
# dependency-check.sh — 依存関係の脆弱性チェックスクリプト
# security-audit Skill から呼び出される
#
# 実行方法: .github/skills/security-audit/scripts/dependency-check.sh
# 前提条件: Maven がインストールされていること
#
# チェック内容:
#   1. 依存関係ツリーの取得
#   2. SNAPSHOT バージョンの検出
#   3. バージョン競合の検出
#   4. OWASP Dependency-Check の実行（利用可能な場合）
#   5. 秘密情報のハードコード検出
#   6. Actuator 設定の検証
#
set -euo pipefail

REPORT_FILE="dependency-check-report-$(date '+%Y%m%d_%H%M%S').txt"
EXIT_CODE=0

echo "=== Dependency & Security Check ===" | tee "$REPORT_FILE"
echo "実行日時: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "$REPORT_FILE"
echo "プロジェクト: $(basename "$PWD")" | tee -a "$REPORT_FILE"
echo "" | tee -a "$REPORT_FILE"

# --- 1. Maven dependency tree の取得 ---
echo "--- [1/6] 依存関係ツリーの取得 ---" | tee -a "$REPORT_FILE"
if command -v mvn &> /dev/null; then
    if mvn dependency:tree -DoutputType=text -q 2>/dev/null; then
        DIRECT_DEPS=$(mvn dependency:tree -q 2>/dev/null | grep -c "\\[INFO\\].*:.*:.*:.*:" || echo "0")
        echo "直接依存数: $DIRECT_DEPS" | tee -a "$REPORT_FILE"
    else
        echo "WARNING: mvn dependency:tree の実行に失敗しました" | tee -a "$REPORT_FILE"
    fi
else
    echo "ERROR: Maven が見つかりません。依存関係チェックを実行できません。" | tee -a "$REPORT_FILE"
    EXIT_CODE=1
fi
echo "" | tee -a "$REPORT_FILE"

# --- 2. SNAPSHOT バージョンの検出 ---
echo "--- [2/6] SNAPSHOT バージョンの検出 ---" | tee -a "$REPORT_FILE"
if [ -f "pom.xml" ]; then
    # プロジェクト自身のバージョン
    project_snapshot=$(grep -c "<version>.*SNAPSHOT.*</version>" pom.xml 2>/dev/null || echo "0")
    # 依存関係の SNAPSHOT
    dep_snapshot=$(grep -n "SNAPSHOT" pom.xml 2>/dev/null | grep -v "<!--" || true)

    if [ "$project_snapshot" -gt 0 ] || [ -n "$dep_snapshot" ]; then
        echo "CRITICAL: pom.xml に SNAPSHOT バージョンが検出されました:" | tee -a "$REPORT_FILE"
        grep -n "SNAPSHOT" pom.xml 2>/dev/null | grep -v "<!--" | tee -a "$REPORT_FILE" || true
        EXIT_CODE=1
    else
        echo "OK: SNAPSHOT バージョンは検出されませんでした" | tee -a "$REPORT_FILE"
    fi
else
    echo "ERROR: pom.xml が見つかりません" | tee -a "$REPORT_FILE"
    EXIT_CODE=1
fi
echo "" | tee -a "$REPORT_FILE"

# --- 3. バージョン競合の検出 ---
echo "--- [3/6] バージョン競合の検出 ---" | tee -a "$REPORT_FILE"
if command -v mvn &> /dev/null; then
    conflicts=$(mvn dependency:tree -Dverbose -q 2>/dev/null | grep "omitted for conflict" || true)
    if [ -n "$conflicts" ]; then
        conflict_count=$(echo "$conflicts" | wc -l | tr -d ' ')
        echo "WARNING: バージョン競合が ${conflict_count} 件検出されました:" | tee -a "$REPORT_FILE"
        echo "$conflicts" | head -20 | tee -a "$REPORT_FILE"
        if [ "$conflict_count" -gt 20 ]; then
            echo "... (残り $((conflict_count - 20)) 件省略)" | tee -a "$REPORT_FILE"
        fi
    else
        echo "OK: バージョン競合は検出されませんでした" | tee -a "$REPORT_FILE"
    fi
fi
echo "" | tee -a "$REPORT_FILE"

# --- 4. OWASP Dependency-Check の実行 ---
echo "--- [4/6] OWASP Dependency-Check ---" | tee -a "$REPORT_FILE"
if command -v dependency-check &> /dev/null; then
    echo "OWASP Dependency-Check を実行中..." | tee -a "$REPORT_FILE"
    dependency-check --project "$(basename "$PWD")" --scan . --format JSON --out dependency-check-report.json 2>/dev/null || {
        echo "WARNING: Dependency-Check の実行に失敗しました" | tee -a "$REPORT_FILE"
    }
    if [ -f "dependency-check-report.json" ]; then
        echo "レポートが dependency-check-report.json に出力されました" | tee -a "$REPORT_FILE"
    fi
elif mvn -pl . org.owasp:dependency-check-maven:check -q 2>/dev/null; then
    echo "Maven プラグインで Dependency-Check を実行しました" | tee -a "$REPORT_FILE"
else
    echo "INFO: OWASP Dependency-Check が利用できません。手動での CVE 確認を推奨します。" | tee -a "$REPORT_FILE"
    echo "  インストール方法: https://owasp.org/www-project-dependency-check/" | tee -a "$REPORT_FILE"
    echo "  または Maven プラグイン: mvn org.owasp:dependency-check-maven:check" | tee -a "$REPORT_FILE"
fi
echo "" | tee -a "$REPORT_FILE"

# --- 5. 秘密情報のハードコード検出 ---
echo "--- [5/6] 秘密情報のハードコード検出 ---" | tee -a "$REPORT_FILE"
SECRET_PATTERNS=(
    'password\s*=\s*"[^"]*[a-zA-Z0-9]'
    'passwd\s*=\s*"[^"]*[a-zA-Z0-9]'
    'apiKey\s*=\s*"[^"]*[a-zA-Z0-9]'
    'api_key\s*=\s*"[^"]*[a-zA-Z0-9]'
    'secret\s*=\s*"[^"]*[a-zA-Z0-9]'
    'token\s*=\s*"[^"]*[a-zA-Z0-9]'
    'BEGIN PRIVATE KEY'
    'BEGIN RSA PRIVATE KEY'
)

secret_found=0
for pattern in "${SECRET_PATTERNS[@]}"; do
    results=$(grep -rn "$pattern" --include="*.java" --include="*.properties" --include="*.yml" --include="*.yaml" --include="*.xml" . 2>/dev/null | grep -v "target/" | grep -v ".git/" | grep -v "test/" || true)
    if [ -n "$results" ]; then
        echo "CRITICAL: 秘密情報の疑いがある記述を検出:" | tee -a "$REPORT_FILE"
        echo "$results" | head -10 | tee -a "$REPORT_FILE"
        secret_found=1
        EXIT_CODE=1
    fi
done
if [ "$secret_found" -eq 0 ]; then
    echo "OK: 秘密情報のハードコードは検出されませんでした" | tee -a "$REPORT_FILE"
fi
echo "" | tee -a "$REPORT_FILE"

# --- 6. Actuator 設定の検証 ---
echo "--- [6/6] Actuator 設定の検証 ---" | tee -a "$REPORT_FILE"
for config_file in $(find . -name "application*.properties" -o -name "application*.yml" 2>/dev/null | grep -v "target/" | grep -v ".git/"); do
    # 全エンドポイント公開の検出
    if grep -q "exposure.include=\*" "$config_file" 2>/dev/null || grep -q "include: \"\*\"" "$config_file" 2>/dev/null; then
        echo "WARNING: $config_file で Actuator 全エンドポイントが公開されています" | tee -a "$REPORT_FILE"
        echo "  本番プロファイルでないことを確認してください" | tee -a "$REPORT_FILE"
    fi
    # スタックトレース公開の検出
    if grep -q "include-stacktrace=always" "$config_file" 2>/dev/null; then
        echo "WARNING: $config_file でスタックトレースが公開設定になっています" | tee -a "$REPORT_FILE"
    fi
    # ddl-auto=update の検出
    if grep -q "ddl-auto=update\|ddl-auto=create" "$config_file" 2>/dev/null; then
        echo "WARNING: $config_file で ddl-auto が update/create に設定されています" | tee -a "$REPORT_FILE"
        echo "  本番プロファイルでないことを確認してください" | tee -a "$REPORT_FILE"
    fi
done
echo "" | tee -a "$REPORT_FILE"

# --- サマリー ---
echo "=== チェック完了 ===" | tee -a "$REPORT_FILE"
echo "レポート: $REPORT_FILE" | tee -a "$REPORT_FILE"
if [ "$EXIT_CODE" -ne 0 ]; then
    echo "結果: ❌ Critical な問題が検出されました。是正が必要です。" | tee -a "$REPORT_FILE"
else
    echo "結果: ✅ Critical な問題は検出されませんでした。" | tee -a "$REPORT_FILE"
fi

exit $EXIT_CODE
