---
name: create-implement-plan
description: "기존 product-spec + tech-spec 기반 implement.md (또는 implement-be.md / implement-fe.md) 생성. Phase 분해 / 변경 파일 / 의존 / Verification / 분리 임계 메타인지 강제. Task 8개+ 또는 단일 Task 50줄+ 시 tasks/ 자동 분리. docs/plans/{N}-{slug}/implement*.md."
---

# Create Implement Plan

`product-spec.md` + `tech-spec.md` → `implement.md` (단일 영역) 또는 `implement-be.md` + `implement-fe.md` (BE+FE) 생성. 구현 순서 / Phase / Task 메타인지 강제 대화형 스킬.

## 전제 (Read 필수, Blocking)

스킬 시작 직후 다음 4개 문서 `Read`. 미로드 시 진행 금지.

- `docs/plans/AGENTS.md` — 워크플로우 / 승인 게이트 / BE/FE 분리 룰 / 분리 임계 / 안티패턴 (특히 Section 4 / 5 / 6 / 10)
- `docs/plans/_templates/implement.md` — 단일 영역 템플릿
- `docs/plans/_templates/implement-be.md` — BE 템플릿
- `docs/plans/_templates/implement-fe.md` — FE 템플릿

분리 임계 (Task 8개+ / 50줄+) 도달 시 추가 Read:

- `docs/plans/_templates/task.md` — Task 상세 템플릿

추가 전제:

- 사용자가 product-spec + tech-spec 모두 작성 + tech-spec 사용자 명시 승인 완료 상태여야 함.
- tech-spec 미승인 시 → "tech-spec 사용자 승인 미완료. AGENTS.md Section 4 승인 게이트 위반." 종료.
- implement 는 tech-spec 의 HOW 를 **실행 순서 / Phase / 변경 파일** 단위로 분해. 새 설계 결정 X (있으면 tech-spec 갱신 후 진행).
- 모든 단계에서 `docs/plans/AGENTS.md` 룰 준수. 충돌 시 AGENTS.md 우선.

## 핵심 원칙

- **한 번에 1 질문** (brainstorming 패턴).
- **`AskUserQuestion` 우선** — Phase 단위 결정 / 분리 여부 / PR 분할 같은 메타인지 결정은 다중선택. 자유서술은 변경 파일 / 핵심 로직 / 의사코드 같이 자연어 필요한 필드.
- **tech-spec NF 결정 자동 반영** — 영향범위 / 마이그레이션 / 강결합 / 동시성 답변을 implement Phase 분할 / 의존 / 선행 강제에 활용.
- **분리 임계 자동 감지** — Task 수 / 본문 줄 수 추적. 임계 초과 시 사용자에게 분리 권유.
- **승인 게이트** — preview → confirm → write (Blocking).

## Step 1 — 대상 plan 폴더 선택

후보 자동 수집:

```bash
# tech-spec.md 존재 + product-spec.md 존재 + handoff.md 존재 우선
find docs/plans -maxdepth 2 -name tech-spec.md -exec stat -f "%m %N" {} \; | sort -rn | head -5
```

각 폴더 implement*.md 존재 여부 별도 체크 (덮어쓰기 / skip 결정용).

`AskUserQuestion` (최대 4개):

```
question: "어떤 plan 의 implement 작성할까요?"
options:
  - "042-interview-quality (handoff 진행중, 추천)"
  - "048-payment-intro (tech-spec 최근)"
  - "051-resume-preview"
  - "다른 폴더 — 직접 경로 입력"
```

선택된 폴더의 `product-spec.md`, `tech-spec.md` 둘 다 `Read` → 컨텍스트 흡수.

implement 파일 이미 존재 시:

```
question: "implement 파일 이미 존재. 어떻게?"
options:
  - "갱신 — 기존 내용 보여주고 부분 수정"
  - "덮어쓰기 — 처음부터"
  - "취소"
```

## Step 2 — 영향 범위 확인 + 파일 형태 결정

tech-spec NF 결정 영향 범위 (`tech-spec.md#nf-결정` 또는 Architecture 섹션) 자동 추출. 명시 없으면 `AskUserQuestion`:

```
question: "이 작업 영향 범위? (tech-spec 기반 자동 추론)"
options:
  - "BE only → implement.md"
  - "FE only → implement.md"
  - "lambda only → implement.md"
  - "BE + FE → implement-be.md + implement-fe.md"
```

BE+FE 선택 시: `tech-spec.md` 의 **API contract** 섹션 존재 여부 확인. 부재 시 → STOP. "API contract 부재. tech-spec 갱신 후 재시도. (AGENTS.md Section 5)" 종료.

강결합 (BE 선행 강제) 케이스 자동 감지:

- tech-spec NF 답변에 마이그레이션 / 백필 / 이벤트 페이로드 변경 포함 시 → `AskUserQuestion`:

```
question: "BE 선행 강제 케이스로 보임. 맞나요?"
options:
  - "예 — BE 머지 후 FE 시작 (강결합)"
  - "아니오 — mock 으로 FE 병렬 진행 가능"
```

→ 답변에 따라 implement-fe.md Phase 0 / Phase 4 의존 표기 다르게 작성.

## Step 3 — Phase 분해 (메타인지 질문)

### 3-1. 자연 분할 후보 추출

tech-spec Architecture / Data Model / API contract 기반으로 Phase 후보 자동 제안. 예:

- BE: (1) Entity / 마이그레이션, (2) Repository / Service, (3) Controller / Validation, (4) 통합 테스트
- FE: (1) API client + 타입 (mock), (2) Hook / Store, (3) UI 컴포넌트, (4) BE 통합 (mock 제거) + E2E
- 단일: (1) 기반 셋업, (2) 핵심 로직, (3) 통합 / 테스트

`AskUserQuestion`:

```
question: "이 Phase 분할로 진행할까요?"
options:
  - "그대로 진행 (추천)"
  - "병합 — 작업 작아서 Phase 줄이기"
  - "추가 분할 — 더 작은 PR 단위로"
  - "직접 정의"
```

병합 / 추가 분할 / 직접 정의 시 자유서술로 Phase 목록 받음.

### 3-2. 각 Phase 별 디테일 (Phase 마다 반복)

Phase 별로 한 번에 다음 4개 자유서술 받음 (한 메시지에 묶음 OK — Phase 단위 응집):

> "Phase {N}: {제목}
> 1. **변경 파일** (path 단위, 무엇을/왜)
> 2. **핵심 로직** (단계별 의사코드 1-5줄)
> 3. **선행 의존** (이전 Phase / 외부)
> 4. **Verification** (명령 + 통과 기준)
> 5. **커밋 메시지** (Conventional Commits)"

모호 답 = 재질문. 변경 파일 "interview 영역" 류 = "구체 클래스 / 파일 경로?" 재질문.

### 3-3. 분리 임계 자동 감지

각 Phase 응답 누적 후 체크:

- Phase / Task 총 8개+ → 분리 권유
- 단일 Phase 본문 50줄+ (코드블록 포함) → 분리 권유

`AskUserQuestion`:

```
question: "분리 임계 초과 ({사유: Task N개 / 본문 N줄}). tasks/ 폴더로 분리?"
options:
  - "분리 — implement*.md = 목록 + 링크, 상세는 tasks/{NN}-{slug}.md (추천)"
  - "유지 — 단일 파일로 진행 (가독성 책임)"
```

분리 선택 시: `docs/plans/_templates/task.md` 구조로 각 Task 상세 작성. implement 본문은 표 + 1줄 요약 + 링크만.

## Step 4 — 통합 Verification 정리

> "전체 작업 완료 판정 기준? tech-spec.md Verification 항목 그대로 쓸지, 추가 회귀 체크 있을지."

`AskUserQuestion`:

```
options:
  - "tech-spec.md Verification 그대로 참조"
  - "tech-spec + 추가 회귀 항목"
  - "implement 단계에서 새로 정의 (tech-spec 업데이트 필요)"
```

세 번째 선택 시 → STOP. "tech-spec Verification 갱신 권유. AGENTS.md 안티패턴 (verification 통과 기준 없음) 회피." 종료.

## Step 5 — 리뷰 게이트 명시

`AskUserQuestion`:

```
question: "지정 리뷰어?"
options:
  - "BE: code-reviewer-backend (단일 BE / BE+FE)"
  - "FE: code-reviewer-frontend (단일 FE / BE+FE)"
  - "둘 다 병렬 (BE+FE 작업)"
  - "기타 (직접 입력)"
```

`CLAUDE.md` "Custom Sub-Agent Usage" + memory `feedback_post_impl_review.md` (Post-Impl Review MANDATORY) 반영.

## Step 6 — preview

수집한 답변을 템플릿에 매핑.

- 단일 영역 → `docs/plans/_templates/implement.md` 그대로
- BE+FE → `implement-be.md` + `implement-fe.md` 둘 다 (각각 templates 파일 그대로)
- 분리 시 → 추가로 `tasks/{be|fe|}-{NN}-{slug}.md` 다수 (각각 `task.md` 템플릿)

템플릿 섹션 / 헤더 / 메타데이터 블록 (작성자 / 답하는 질문 / 승인 게이트) 그대로 유지. 자의적 변형 X.

`AskUserQuestion`:

```
question: "이 implement 로 파일 생성할까요?"
options:
  - "생성 — 그대로 진행"
  - "수정 — 특정 Phase / 섹션 다시 답변"
  - "취소"
```

수정 선택 시 → 어느 Phase / 섹션? → 해당 step 만 재실행 → 다시 preview.

## Step 7 — 파일 작성

승인 후 `Write`:

- 단일: `$PLAN_DIR/implement.md`
- BE+FE: `$PLAN_DIR/implement-be.md` + `$PLAN_DIR/implement-fe.md`
- 분리 시: `$PLAN_DIR/tasks/` 디렉토리 `mkdir -p` 후 각 task.md 작성

## Step 8 — 후속 안내

- "implement 작성 완료. **사용자 명시 승인** 후 구현 agent 호출 단계."
- 단일 BE → `Agent(subagent_type=backend, ...)` 권유. 호출 시 `backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md` Read 강제 (CLAUDE.md "Sub-Agent Convention Contract").
- 단일 FE → `Agent(subagent_type=frontend, ...)` 권유. 호출 시 `frontend/.claude/rules/conventions.md` + `frontend/.claude/rules/architecture.md` Read 강제.
- BE+FE → 양 agent 병렬 호출 (FE 는 mock 진행). 강결합 시 BE 선행.
- 구현 후 지정 리뷰어 병렬 실행 필수 (memory: Post-Impl Review MANDATORY).
- 커밋은 별도. 사용자 결정.

## 안티 패턴

- product-spec / tech-spec 부재인데 implement 진행 (요구사항 / 설계 추측).
- tech-spec 미승인 상태에서 implement 시작 (AGENTS.md Section 4 승인 게이트 위반).
- 한 메시지에 Phase 메타인지 질문 여러 개 (Phase 디테일은 Phase 단위 묶음 OK, 그 외 1개씩).
- BE+FE 작업인데 API contract 부재 진행.
- 강결합 (마이그레이션 / 이벤트 페이로드 변경) 인데 FE 병렬 시작 표기.
- Phase 변경 파일 모호 ("interview 영역") 그대로 수용.
- 분리 임계 (Task 8개+ / 50줄+) 무시하고 단일 파일 강제.
- Verification 을 implement 단계에서 새로 정의 (tech-spec Verification 영역 침범).
- 지정 리뷰어 미명시 (memory Post-Impl Review MANDATORY 위반).
- preview 없이 파일 작성.
- 템플릿 섹션 / 헤더 자의적 변형.
