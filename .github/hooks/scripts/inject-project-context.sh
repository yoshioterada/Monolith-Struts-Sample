#!/usr/bin/env bash
# inject-project-context.sh — セッション開始時のコンテキスト注入
# SessionStart Hook（タイムアウト: 10秒）
# プロジェクト状態を収集し AI Agent にコンテキスト注入する
set -euo pipefail

BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

UNCOMMITTED=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
if [ "$UNCOMMITTED" -gt 0 ]; then
  CHANGES="${UNCOMMITTED}件の未コミット変更あり"
else
  CHANGES="未コミット変更なし"
fi

if [ -f "pom.xml" ]; then
  VER=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')
else
  VER="unknown"
fi

SNAP=""
if [ -f "pom.xml" ]; then
  SC=$(grep -c "SNAPSHOT" pom.xml 2>/dev/null || echo "0")
  if [ "$SC" -gt 0 ]; then
    SNAP=", SNAPSHOT=${SC}件"
  fi
fi

GATE=""
if [ -d ".github/review-reports" ]; then
  LR=$(find .github/review-reports -name "*.md" -type f 2>/dev/null | sort -r | head -1)
  if [ -n "$LR" ]; then
    GATE=", 最新レビュー=${LR}"
  fi
fi

JV=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' 2>/dev/null || echo "unknown")

echo "{\"systemMessage\":\"プロジェクト: ブランチ=${BRANCH}, バージョン=${VER}, Java=${JV}, ${CHANGES}${SNAP}${GATE}. スタック: Java 25, Spring Boot 4.1, Spring AI 2.0.\"}"
