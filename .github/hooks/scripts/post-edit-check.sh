#!/usr/bin/env bash
# post-edit-check.sh — 編集後の自動チェック
# PostToolUse Hook（タイムアウト: 30秒）
# チェック: 秘密情報, System.out, 例外握りつぶし, SQL結合, SNAPSHOT
set -euo pipefail

W=""
C=0

JF=$(find . -path "*/src/*" -name "*.java" -not -path "*/target/*" 2>/dev/null | head -100)
CF=$(find . -path "*/src/*" \( -name "*.properties" -o -name "*.yml" \) -not -path "*/target/*" 2>/dev/null | head -20)

# 1. 秘密情報のハードコード検出
SP='(password|passwd|secret|api[_-]?key|token)\s*[:=]\s*["'"'"'][^"'"'"']{8,}'
if [ -n "$JF" ] || [ -n "$CF" ]; then
  SH=$(echo "$JF $CF" | tr ' ' '\n' | xargs grep -iElr "$SP" 2>/dev/null || true)
  if [ -n "$SH" ]; then
    W="${W}[CRITICAL]秘密情報ハードコード検出。環境変数化要。 "
    C=1
  fi
fi

# 2. System.out.println の検出
if [ -n "$JF" ]; then
  SO=$(echo "$JF" | tr ' ' '\n' | xargs grep -lr "System\.out\.println\|System\.err\.println" 2>/dev/null || true)
  if [ -n "$SO" ]; then
    W="${W}[CRITICAL]System.out.println検出。SLF4J使用要。 "
    C=1
  fi
fi

# 3. 例外の握りつぶし検出
if [ -n "$JF" ]; then
  EX=$(echo "$JF" | tr ' ' '\n' | xargs grep -l "catch.*{[[:space:]]*}" 2>/dev/null || true)
  if [ -n "$EX" ]; then
    W="${W}[CRITICAL]例外握りつぶし検出。ログか再スロー要。 "
    C=1
  fi
fi

# 4. SQL 文字列結合の検出
if [ -n "$JF" ]; then
  SQ=$(echo "$JF" | tr ' ' '\n' | xargs grep -l '"SELECT.*"\s*+\|"INSERT.*"\s*+\|"UPDATE.*"\s*+\|"DELETE.*"\s*+' 2>/dev/null || true)
  if [ -n "$SQ" ]; then
    W="${W}[CRITICAL]SQL文字列結合検出。パラメータバインド要。 "
    C=1
  fi
fi

# 5. SNAPSHOT バージョンの検出（本番ブランチ）
if [ -f "pom.xml" ]; then
  SN=$(grep -c "SNAPSHOT" pom.xml 2>/dev/null || echo "0")
  if [ "$SN" -gt 0 ]; then
    BR=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    if [ "$BR" = "main" ] || [ "$BR" = "master" ]; then
      W="${W}[CRITICAL]本番ブランチ(${BR})でSNAPSHOT検出。 "
      C=1
    else
      W="${W}[INFO]SNAPSHOT${SN}件(本番では禁止)。 "
    fi
  fi
fi

# 結果出力
if [ "$C" -eq 1 ]; then
  echo "{\"systemMessage\":\"❌ 編集後チェック: Critical違反検出。${W}\"}"
elif [ -n "$W" ]; then
  echo "{\"systemMessage\":\"⚠️ 編集後チェック: ${W}\"}"
else
  echo "{\"systemMessage\":\"✅ 編集後チェック: 禁止事項違反なし\"}"
fi
