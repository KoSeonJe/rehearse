#!/usr/bin/env bash
# Auto-update related docs on git events (commit/revert/amend/rebase/reset).
# Invoked by .git/hooks/{post-commit,post-rewrite,reference-transaction}.
# Runs `claude -p` in background. Leaves edits in working tree (no auto-commit).
#
# Disable: export SKIP_DOC_HOOK=1   (per-shell)
#          commit msg containing [skip-doc-hook]
#
# Log: .git/auto-doc-update.log

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$REPO_ROOT" || exit 0

EVENT="${1:-commit}"   # commit | amend | rebase | revert | reset
LOG="$REPO_ROOT/.git/auto-doc-update.log"
LOCK="$REPO_ROOT/.git/auto-doc-update.lock"

[ "${SKIP_DOC_HOOK:-0}" = "1" ] && exit 0
command -v claude >/dev/null 2>&1 || { echo "[$(date +%T)] claude CLI missing, skip" >>"$LOG"; exit 0; }

# Recursion guard via marker in HEAD message
HEAD_MSG="$(git log -1 --pretty=%B 2>/dev/null || true)"
case "$HEAD_MSG" in
  *"[skip-doc-hook]"*) exit 0 ;;
esac

# Single-flight lock (skip if another instance running)
if ! ( set -o noclobber; echo $$ > "$LOCK" ) 2>/dev/null; then
  echo "[$(date +%T)] another instance running, skip" >>"$LOG"
  exit 0
fi
trap 'rm -f "$LOCK"' EXIT

# Gather context
SUBJECT="$(git log -1 --pretty=%s 2>/dev/null)"
BODY="$(git log -1 --pretty=%b 2>/dev/null)"
SHA="$(git rev-parse --short HEAD 2>/dev/null)"
FILES="$(git show --name-only --pretty=format: HEAD 2>/dev/null | sed '/^$/d')"
STAT="$(git show --stat --pretty=format: HEAD 2>/dev/null | head -50)"

PROMPT_FILE="$(mktemp -t auto-doc-update.XXXXXX)"
cat >"$PROMPT_FILE" <<EOF
Git event: $EVENT (HEAD=$SHA)

Commit subject: $SUBJECT

Commit body:
$BODY

Changed files:
$FILES

Diff stat:
$STAT

---

당신은 이 저장소의 문서 동기화 담당입니다. 위 git 이벤트가 다음 중 하나에 해당할 때만 관련 문서를 업데이트하세요:

1. 도메인 로직 수정 (서비스/유스케이스/엔티티 의미 변화)
2. 설계 변경 (아키텍처, 레이어 책임, 데이터 흐름, 외부 연동)
3. 작업 완료/취소/롤백 (plan progress 행 상태 변화 필요)
4. revert/reset → 이전 문서 갱신 분량 되돌릴 필요가 있는지 검토

업데이트 대상 후보 (존재할 때만):
- ./CLAUDE.md, backend/CLAUDE.md, frontend/CLAUDE.md, lambda/CLAUDE.md
- docs/architecture/**.md
- docs/plans/**/progress.md

규칙:
- 관련 없으면 **아무것도 수정하지 말 것**. "no-op" 한 줄만 보고.
- 단순 typo/포맷팅/테스트 추가/주석 수정 → no-op.
- 변경 시 핵심 한두 줄만 갱신. 장황한 서술 금지.
- 새 파일 생성 금지. 기존 파일만 Edit.
- 절대 git commit / git add 하지 말 것. 변경은 working tree에만 둠.
- 최대 5개 파일 이내.

마지막에 다음 형식 한 줄 출력:
RESULT: <updated|noop> files=<n> reason=<짧게>
EOF

{
  echo "[$(date '+%F %T')] event=$EVENT sha=$SHA subject=\"$SUBJECT\""
  claude -p --permission-mode acceptEdits < "$PROMPT_FILE" 2>&1 | tail -40
  echo "---"
} >>"$LOG" 2>&1

rm -f "$PROMPT_FILE"
