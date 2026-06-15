#!/usr/bin/env bash
# Claude Code PostToolUse hook (matcher=Bash).
# git pull / git merge 직후 변경 파일 분석 → docs 동기화 작업 지시를 메인 세션에 inject.
# 대체 대상: .git/hooks/post-{commit,merge} + scripts/{auto-doc-update,sync-domain-docs}.sh (모두 폐기됨).
#
# 비활성화:
#   - SKIP_DOC_SYNC_HOOK=1 (env, per-shell)
#   - 머지 커밋 메시지에 [skip-doc-sync]
#
# Stdin: PostToolUse JSON payload (tool_input.command 추출)
# Stdout: hookSpecificOutput.additionalContext JSON (관련 변경 시) / 빈 출력 (무관 시)

set -u

[ "${SKIP_DOC_SYNC_HOOK:-0}" = "1" ] && exit 0

PAYLOAD="$(cat)"
CMD="$(printf '%s' "$PAYLOAD" | python3 -c "import sys,json
try:
    d=json.load(sys.stdin)
    print(d.get('tool_input',{}).get('command','') or '')
except Exception:
    pass" 2>/dev/null)"

# Trigger: git pull / git merge 만. fetch / rebase / checkout 등 제외.
case "$CMD" in
  *"git pull --rebase"*) exit 0 ;;
  *"git pull"*|*"git merge "*|*"git merge"|*"&& git merge"*) ;;
  *) exit 0 ;;
esac

REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null)}"
[ -z "$REPO_ROOT" ] && exit 0
cd "$REPO_ROOT" || exit 0

# 머지 커밋 마커 체크
HEAD_MSG="$(git log -1 --pretty=%B 2>/dev/null || true)"
case "$HEAD_MSG" in
  *"[skip-doc-sync]"*) exit 0 ;;
esac

# ORIG_HEAD = 머지 직전 head (git pull/merge 가 자동 세팅)
git rev-parse --verify ORIG_HEAD >/dev/null 2>&1 || exit 0

# ORIG_HEAD == HEAD = 실제 머지 없음 (already up-to-date)
if [ "$(git rev-parse ORIG_HEAD)" = "$(git rev-parse HEAD)" ]; then
  exit 0
fi

CHANGED="$(git diff --name-only ORIG_HEAD..HEAD 2>/dev/null || true)"
[ -z "$CHANGED" ] && exit 0

# 카테고리별 필터
DOMAIN_BASE="backend/src/main/java/com/rehearse/api/domain"
FLYWAY_BASE="backend/src/main/resources/db/migration"

DOMAIN_FILES="$(echo "$CHANGED" | grep -E "^${DOMAIN_BASE}/[^/]+/" || true)"
FLYWAY_FILES="$(echo "$CHANGED" | grep -E "^${FLYWAY_BASE}/V[^/]+\.sql$" || true)"
ARCH_FILES="$(echo "$CHANGED" | grep -E "^docs/architecture/.*\.md$" || true)"
PROGRESS_FILES="$(echo "$CHANGED" | grep -E "^docs/plans/[^/]+/progress\.md$" || true)"

# 무관 = 종료 (아무 inject 안함)
if [ -z "$DOMAIN_FILES" ] && [ -z "$FLYWAY_FILES" ] && [ -z "$ARCH_FILES" ] && [ -z "$PROGRESS_FILES" ]; then
  exit 0
fi

# 직접 영향 도메인 추출
DIRECT_DOMAINS="$(echo "$DOMAIN_FILES" | sed -n "s|^${DOMAIN_BASE}/\([^/]*\)/.*|\1|p" | sort -u | tr '\n' ',' | sed 's/,$//')"
EXISTING_DOMAINS="$(ls -1 docs/domain 2>/dev/null | grep -vE '^(AGENTS\.md|_templates)$' | tr '\n' ',' | sed 's/,$//')"
SHA="$(git rev-parse --short HEAD 2>/dev/null)"
TODAY="$(date +%F)"

# Helper: 카테고리별 파일 목록 (없으면 -)
fmt() { local v="$1"; [ -z "$v" ] && echo "-" || echo "$v" | tr '\n' ' '; }

CONTEXT="[Auto-doc-sync] git pull/merge 감지 (HEAD=$SHA). 다음 변경분 docs 동기화 검토.

== 변경 카테고리 ==
- 직접 영향 BE 도메인: ${DIRECT_DOMAINS:--}
- Flyway 마이그레이션: $(fmt "$FLYWAY_FILES")
- docs/architecture: $(fmt "$ARCH_FILES")
- docs/plans/*/progress.md: $(fmt "$PROGRESS_FILES")

== 기존 docs/domain 폴더 ==
${EXISTING_DOMAINS}

== 작업 룰 (필수) ==
1. **docs/domain/{name}/** — 영향 도메인의 schema.md / api/{action}.md 부분 갱신.
   - docs/domain/{name}/ 미존재 = 노터치. 사용자에게 'docs/domain/AGENTS.md §3 5단계 절차로 신규 생성 필요' 보고.
   - 기존 파일 Edit only. 새 파일 생성 X.
   - 코드에서 추론 가능한 사실만 반영.
   - 정책 추론 불가 (state 전이 / 임계값 / fallback / 동시성 / 보존 / 권한 / 외부 의존 fallback / 관찰성 임계 — docs/domain/AGENTS.md §7 8항목) = 그대로 두거나 \`❓TODO(auto-sync, ${TODAY}): <짧은 설명>\` 마킹.
   - 기존 ❓TODO 절대 제거 / 변경 X.
   - 한 도메인당 최대 5 파일.
2. **docs/architecture/** + **docs/plans/*/progress.md** + **CLAUDE.md/AGENTS.md** — 변경 발생 시 핵심 1-2줄만 갱신. 장황 X.
3. **자동 commit 금지** — working tree 만 수정. 사용자가 diff 확인 후 수동 commit.
4. 관련 없거나 trivial (오타 / 포맷 / 테스트 추가 / 주석) = 'no-op' 보고.
5. Flyway 파일 = 테이블명 prefix 로 도메인 추론 (interview_*, qset_*, fb_*). 모호하면 노터치 + 사용자에게 확인 요청.
6. 상세 룰: docs/domain/AGENTS.md §6 (프롬프트 템플릿) + §7 (메타인지 8항목).

== 출력 형식 ==
작업 시작 전 한 줄로 계획 요약. 작업 후 변경 파일 목록 + 노터치 도메인 (있으면) 보고."

# Emit additionalContext via JSON
printf '%s' "$CONTEXT" | python3 -c "import json,sys
ctx = sys.stdin.read()
print(json.dumps({'hookSpecificOutput':{'hookEventName':'PostToolUse','additionalContext':ctx}}))"
