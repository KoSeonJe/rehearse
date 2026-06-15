# Handoff — 424-resume-wrap-up-removal

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 (2026-05-07)
> **다음 세션**: plan 폴더 진입 시 이 파일 먼저 읽음

---

## 현재 상태

- **BE 구현**: 완료 + 머지. PR #440 squash merge → develop (`44af94b`).
  - PR 제목: `[BE] refactor: 이력서 면접 시간 종료 시 회고 단계 없이 즉시 마무리`
  - Phase 1~4 전체 구현 완료. code-reviewer-backend 리뷰 P0 2건 + P1 반영 완료.
- **FE 구현**: 미시작 — BE dev 배포 + E2E 검증 후 진입 권장.
- **브랜치**: BE 브랜치 `feat/424-resume-wrap-up-removal` 머지 완료 (worktree `/Users/koseonje/dev/devlens-424-be` 잔존 — `git worktree remove` + `git branch -d` 가능).
- **메인 worktree develop**: `44af94b` 동기화 완료.
- **빌드 / 테스트**: BE CI 통과 (머지 완료 기준).

## 다음 세션 시작점

### 1순위 — BE dev 배포 + E2E 검증

BE dev 배포 후 수동 1회:

- Resume 다중 턴 → 시간 종료 후 답변 제출 → 즉시 마무리 (WRAP_UP 미진입 확인)
- hard timeout backstop 정상 동작 (`log.warn` 기록 확인)

### 2순위 — FE 구현 (BE dev 배포 검증 후)

```
Agent(subagent_type=frontend, prompt=
  "docs/plans/424-resume-wrap-up-removal/implement-fe.md 의 Phase 1~4 를 순서대로 구현.
   착수 전 아래 파일 Read 필수:
   - frontend/.claude/rules/conventions.md
   - frontend/.claude/rules/architecture.md
   - docs/plans/424-resume-wrap-up-removal/implement-fe.md (전체)
   - docs/plans/424-resume-wrap-up-removal/tech-spec.md (API contract + Pre/Post State + Verification)
   BE dev 배포 확인 후 진입. mock 진행 없음.
   브랜치 feat/424-resume-wrap-up-removal-fe 생성 후 시작.")
```

## 미해결 질문 / Blocker

| 항목 | 상태 |
|------|------|
| BE dev 배포 + E2E 수동 검증 | **대기** — FE 진입 전 완료 권장 |
| FE PR 진입 타이밍 | **결정 필요** — BE dev 배포 검증 후 vs 즉시 병렬. 현재 권장: dev 배포 후 진입. |
| BE worktree `/Users/koseonje/dev/devlens-424-be` 정리 | 사용자 판단으로 `git worktree remove devlens-424-be && git branch -d feat/424-resume-wrap-up-removal` |

## 컨텍스트 메모

### BE 핵심 변경 요약
- Resume FSM: 3단계 (PLAYGROUND → INTERROGATION → WRAP_UP) → 2단계 (PLAYGROUND → INTERROGATION) 축소.
- `FollowUpRequest.terminate` 필드 추가 (FE 신호 수신 + hard timeout backstop 양방향 종료).
- WRAP_UP 모드 일괄 제거. `terminateResponse()` = `followUpExhausted=true / skip=true / presentToUser=false`.

### FE 진입 시 핵심 정책
- `QuestionType` 유니언에서 `'RESUME_WRAP_UP'` 제거 → TS strict 에러로 사용처 일괄 확인.
- `FollowUpRequest` 타입에 `terminate?: boolean` 추가 (선택, 미전송 = false BE 기본값).
- 종료 페이즈 분기: `followUpExhausted=true` 기존 분기 재활용 (신규 UI 불필요).
- 답변 입력 도중 잔여 ≤ 0 도달해도 끊지 않음 — 현재 답변 완료 후 제출 시점에만 terminate 신호.

### Flyway
- 신규 Flyway 마이그레이션 0건 (V46/V47 폐기). V42 가 이미 constraint drop 완료.
- 운영 SQL cleanup 불필요 — RESUME_WRAP_UP row dev 환경에만 존재 / prod 부재 확인.

### 후속 issue (별도 ticket, 본 plan 범위 외)
- #438 [BE] refactor: ResumeInterviewOrchestrator.processUserTurn 시그니처 record 도입 (P3)
- #439 [BE] feat: ResumeOrchestrator terminate / hard timeout 로그 userId / MDC (P3)

## 관련 파일

```
docs/plans/424-resume-wrap-up-removal/
├── product-spec.md          # 기획 배경 + 수용 기준
├── tech-spec.md             # API contract / Pre-Post State / Verification
├── implement-be.md          # BE Phase 1~4 (완료)
└── implement-fe.md          # FE Phase 1~4 (BE 머지 후 진입)
```

## 참고 명령

```bash
# FE 브랜치 생성
git checkout develop && git pull
git checkout -b feat/424-resume-wrap-up-removal-fe

# FE 개발 서버
cd frontend && npm run dev

# FE 타입 검증
cd frontend && npx tsc --noEmit

# FE 테스트 전체
cd frontend && npm run test

# WRAP_UP 잔존 확인 (FE Phase 4 green 기준)
grep -rEn "RESUME_WRAP_UP" frontend/src

# BE worktree 정리 (선택)
git worktree remove /Users/koseonje/dev/devlens-424-be
git branch -d feat/424-resume-wrap-up-removal

# BE 로컬 부팅
cd backend
docker compose -f docker-compose.local.yml up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

업데이트: 2026-05-07 (운영 SQL 단락 제거 — RESUME_WRAP_UP row dev 한정 / prod 부재 확인)
