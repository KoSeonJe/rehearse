#!/usr/bin/env bash
# Auto-sync docs/domain/{name}/ on git merge events.
# Invoked by .git/hooks/post-merge. Runs `claude -p` in background.
# Leaves edits in working tree (no auto-commit).
#
# Disable: export SKIP_DOMAIN_DOC_HOOK=1   (per-shell)
#          merge commit msg containing [skip-domain-doc]
#
# Log: .git/sync-domain-docs.log

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$REPO_ROOT" || exit 0

EVENT="${1:-merge}"
LOG="$REPO_ROOT/.git/sync-domain-docs.log"
LOCK="$REPO_ROOT/.git/sync-domain-docs.lock"

[ "${SKIP_DOMAIN_DOC_HOOK:-0}" = "1" ] && exit 0
command -v claude >/dev/null 2>&1 || { echo "[$(date +%T)] claude CLI missing, skip" >>"$LOG"; exit 0; }

# Recursion guard via marker in HEAD message
HEAD_MSG="$(git log -1 --pretty=%B 2>/dev/null || true)"
case "$HEAD_MSG" in
  *"[skip-domain-doc]"*) exit 0 ;;
esac

# Single-flight lock
if ! ( set -o noclobber; echo $$ > "$LOCK" ) 2>/dev/null; then
  echo "[$(date +%T)] another instance running, skip" >>"$LOG"
  exit 0
fi
trap 'rm -f "$LOCK"' EXIT

# Diff range: ORIG_HEAD = pre-merge head (set by git on merge / pull)
if ! git rev-parse --verify ORIG_HEAD >/dev/null 2>&1; then
  echo "[$(date +%T)] ORIG_HEAD missing, skip" >>"$LOG"
  exit 0
fi

CHANGED="$(git diff --name-only ORIG_HEAD..HEAD 2>/dev/null || true)"
[ -z "$CHANGED" ] && { echo "[$(date '+%F %T')] event=$EVENT RESULT: noop reason=no-diff" >>"$LOG"; exit 0; }

DOMAIN_BASE="backend/src/main/java/com/rehearse/api/domain"
FLYWAY_BASE="backend/src/main/resources/db/migration"

DOMAIN_FILES="$(echo "$CHANGED" | grep -E "^${DOMAIN_BASE}/[^/]+/" || true)"
FLYWAY_FILES="$(echo "$CHANGED" | grep -E "^${FLYWAY_BASE}/V[^/]+\.sql$" || true)"

if [ -z "$DOMAIN_FILES" ] && [ -z "$FLYWAY_FILES" ]; then
  echo "[$(date '+%F %T')] event=$EVENT RESULT: noop reason=no-domain-change" >>"$LOG"
  exit 0
fi

# Direct domains from path prefix
DIRECT_DOMAINS="$(echo "$DOMAIN_FILES" | sed -n "s|^${DOMAIN_BASE}/\([^/]*\)/.*|\1|p" | sort -u | tr '\n' ' ')"

# Existing domain dirs (for AI to know what's available)
EXISTING_DOMAINS="$(ls -1 docs/domain 2>/dev/null | grep -vE '^(AGENTS\.md|_templates)$' | tr '\n' ' ')"

SHA="$(git rev-parse --short HEAD 2>/dev/null)"
SUBJECT="$(git log -1 --pretty=%s 2>/dev/null)"

# Diff stat for context (cap)
DIFF_STAT="$(git diff --stat ORIG_HEAD..HEAD -- "$DOMAIN_BASE" "$FLYWAY_BASE" 2>/dev/null | head -80)"

# Flyway file contents (cap each)
FLYWAY_DUMP=""
if [ -n "$FLYWAY_FILES" ]; then
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    [ -f "$f" ] || continue
    FLYWAY_DUMP="${FLYWAY_DUMP}
=== $f ===
$(head -200 "$f")
"
  done <<<"$FLYWAY_FILES"
fi

PROMPT_FILE="$(mktemp -t sync-domain-docs.XXXXXX)"
cat >"$PROMPT_FILE" <<EOF
Git merge event (HEAD=$SHA): $SUBJECT

==== Direct-impact domains (path-prefix matched) ====
$DIRECT_DOMAINS

==== Existing docs/domain/ folders ====
$EXISTING_DOMAINS

==== Changed backend domain files ====
$DOMAIN_FILES

==== Changed Flyway migrations ====
$FLYWAY_FILES

==== Flyway file contents (head 200 lines each) ====
$FLYWAY_DUMP

==== Diff stat ====
$DIFF_STAT

---

당신은 docs/domain 동기화 담당입니다. 위 머지 변경분을 분석해 영향받은 도메인의 docs/domain/{name}/ 파일을 갱신하세요.

**도메인 식별**:
1. 경로 기반: backend/.../domain/{name}/ → 그대로 {name}
2. Flyway: 테이블명 prefix (예: interview_session → interview, qset_* → questionset, fb_* → feedback) → docs/domain 기존 폴더명과 매칭. 모호하면 노터치.

**수정 룰**:
- docs/domain/{name}/ 가 **존재하지 않으면 노터치** (스켈레톤 자동 생성 금지). RESULT 에 missing=<name> 보고.
- 존재하면 schema.md (또는 schema/{table}.md) / api/{action}.md 부분 갱신만. 새 파일 생성 금지.
- 코드에서 추론 가능한 사실 (테이블 컬럼 / endpoint signature / 단순 분기) 만 반영.
- 추론 불가능한 정책 (state 전이 조건 / 임계값 / fallback 발동 / 동시성 / 보존 / 권한 / 외부 fallback / 관찰성 임계 — docs/domain/AGENTS.md §7 8항목) = **❓TODO(auto-sync, $(date +%F)): <짧은 설명>** 마킹. 추론으로 채우지 말 것.
- 기존 ❓TODO 절대 제거 금지 (사용자 미해결 상태). 내용 변경도 금지.
- 템플릿 헤더 / 섹션 구조 변형 금지.
- 한 도메인당 최대 5 파일 수정. 도메인 합산 최대 15 파일.
- 절대 git add / git commit 금지. working tree 만 변경.

**관련 없으면**: 한 줄 noop 보고.

**필수 마지막 줄 출력**:
RESULT: <updated|noop> domains=<쉼표목록 또는 -> files=<n> missing=<쉼표목록 또는 ->
EOF

{
  echo "[$(date '+%F %T')] event=$EVENT sha=$SHA direct=\"$DIRECT_DOMAINS\" flyway_count=$(echo "$FLYWAY_FILES" | grep -c . || true)"
  claude -p --permission-mode acceptEdits < "$PROMPT_FILE" 2>&1 | tail -60
  echo "---"
} >>"$LOG" 2>&1

rm -f "$PROMPT_FILE"
