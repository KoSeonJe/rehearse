# CLAUDE.md

@AGENTS.md

본 문서는 **Claude 세션 전용 추가 규칙** 만 보유 (서브에이전트, 스킬, 위임 정책). Codex 는 무시.

---

## Custom Sub-Agent Usage (Required)

- `.claude/agents/` 정의된 커스텀 서브에이전트 적극 사용.
- **빌트인보다 커스텀 우선** — 프로젝트 컨텍스트 최적화됨.
- Key agents:
  - **Code review (BE)**: `code-reviewer-backend` (Opus) — 룰 위배 + 성능 / 확장성 / 클린코드 / 쿼리 효율성. 자기 코드 셀프 승인 금지
  - **Code review (FE)**: `code-reviewer-frontend` (Opus) — 룰 위배 + 결함·사이드이펙트 / 성능 / 확장성 / 클린코드 / 데이터 페칭 효율성. 자기 코드 셀프 승인 금지
  - **Spec review (Product)**: `spec-reviewer-product` (Opus) — product-spec.md 리뷰. Goal 측정성 / AC 검증성 / Non-Goals / 비스코프 / HOW 침범 / Issue 정합성. **`/create-product-spec` 직후 Phase E 강제 호출**
  - **Spec review (Tech)**: `spec-reviewer-tech` (Opus) — tech-spec.md 리뷰. Architecture 구체성 / NF 11개 / Trade-off / Data Model / API contract / Verification / 컨벤션 매핑 / 분기 결정. **`/create-tech-spec` 직후 Phase E 강제 호출**
  - **FE 구현**: `frontend` (Opus) — 구현 설계 (tech-spec.md) + 컴포넌트 / 훅 / store / API / 테스트
  - **BE 구현**: `backend` — API, 비즈니스 로직, DB 스키마, 구현 설계 (tech-spec.md)
  - **Git/PR (Sonnet)**: `git-manager` — 브랜치 푸시, `/create-pr`, PR 머지, develop 동기화, 태그
  - **Docs (Sonnet)**: `docs-manager` — `progress.md` 갱신, 핸드오프 문서, spec status, README/AGENTS.md 편집
  - **Debugging (BE)**: `debugger-backend` (Opus) — 재현 / 원인 분석 / minimal fix. 큰 변경 = backend 위임
  - **Debugging (FE)**: `debugger-frontend` (Opus) — 재현 정보 수집 / 가설 / minimal fix. 큰 변경 = frontend 위임, 5xx 의심 = debugger-backend 위임
  - **테스트 작성**: 별도 agent 없음. 구현 agent (`backend` / `frontend`) 가 동시 작성
- 복잡 작업은 다중 에이전트 **병렬** 실행.

## Mandatory Delegation — Git/Docs (Required)

다음 작업 **반드시 위임**. 메인 세션 (Opus) 직접 수행 금지 — 비용 차이 + 정형 절차.

**`git-manager` (Sonnet)**:
- `/create-pr` 스킬 PR 생성
- `gh pr merge --squash` (사용자 사전 승인 시)
- 브랜치 푸시 / `git checkout` / `git pull` 머지 후 동기화
- 태그 push (사용자 명시 시)

**`docs-manager` (Sonnet)**:
- `docs/plans/{N}-{slug}/progress.md` 진행 로그 추가 / 행 상태 변경
- `docs/plans/{N}-{slug}/handoff.md` 세션 인계 문서 작성·갱신 (세션 종료 시)
- spec status 갱신, README / AGENTS.md / CLAUDE.md 부분 편집

위 작업 등장 시 즉시 `Agent(subagent_type=git-manager|docs-manager, ...)`. 결과만 받아 사용자 보고.

## Plan Mode

플랜 작성 규칙: `.claude/rules/plan-mode.md` (root AGENTS.md `@import`). 에이전트 배정 / 승인 게이트 / Pre-Post 상태 / Verification 포함.

## Sub-Agent Convention Contract (Required, Blocking)

모든 서브에이전트는 **착수 전** 담당 영역 컨벤션 문서를 `Read` 직접 로드 후 구현.

**Backend (backend / debugger)**:
- `backend/.claude/rules/conventions.md`, `backend/.claude/rules/testing.md`

**Frontend (frontend / frontend-developer / designer / debugger)**:
- `frontend/.claude/rules/conventions.md`, `frontend/.claude/rules/architecture.md`
- `frontend/DESIGN.md` — 디자인 / UI 작업 시에만

**리뷰 (code-reviewer-backend)**:
- 대상 BE/FE 컨벤션 + `AGENTS.md` (루트) + `CLAUDE.md` (Claude 세션 시)

**테스트 작성**: 구현 agent (`backend` / `frontend`) 가 구현과 동시 작성. 별도 test 전용 agent 없음.

**공통 룰 (재확인)**:
- BE: `backend/.claude/rules/conventions.md` (계층 / 트랜잭션 / Flyway / Lombok / 로깅 등)
- FE: `any` 금지, `console.log` 커밋 금지, Claude API 직접 호출 금지, shadcn primitive 우선
- 주석 규칙: `.claude/rules/comments.md` (root AGENTS.md `@import`)

**호출 프롬프트 예시**:
```
backend 에이전트로 {기능} 구현. 시작 전 backend/.claude/rules/conventions.md,
backend/.claude/rules/testing.md, backend/AGENTS.md 를 Read 확인.
Entity 직접 반환 금지, @Transactional(readOnly=true) 기본, 주석 최소화 준수.
```

문서 로드 없이 구현 진행 시 **리뷰 단계에서 컨벤션 위반 사유 재작업** 요청.
