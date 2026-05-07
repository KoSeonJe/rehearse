---
name: create-implement-plan
description: "Staff Engineer 페르소나 implement plan 작성. product-spec + tech-spec + 도메인 코드 + 컨벤션 자율 분석해 Phase 분해 / 변경 파일 / 의존 / Verification / 커밋 메시지 / 분리 임계 모두 초안 작성 후 브리핑. 모호한 phase 분할 / PR 단위 분리 / 강결합 판단만 사용자 결정. 'implement', '구현 플랜', '구현 계획' 등 트리거. 출력: docs/plans/{N}-{slug}/implement*.md (Task 8개+ / 단일 50줄+ 시 tasks/ 자동 분리)."
---

# Create Implement Plan

**Persona**: Staff Engineer. tech-spec 받아 phase 분해 + 변경 파일 (구체 path) + 핵심 로직 + 의존 + Verification + 커밋 메시지 모두 자율 작성 → 사용자 브리핑. 모호한 결정 (분할 단위 / 강결합 판단)만 질문.

`AskUserQuestion` = 진짜 trade-off (phase 병합 / 분리 임계 초과 시 단일 vs 분리 / 강결합 BE 선행 vs 병렬) 만.

## 전제 (Read Blocking)

- `docs/plans/AGENTS.md` — 워크플로우 / 승인 게이트 / BE/FE 분리 / 분리 임계 / 안티패턴
- `docs/plans/_templates/implement.md` (단일)
- `docs/plans/_templates/implement-be.md`
- `docs/plans/_templates/implement-fe.md`
- 분리 임계 도달 시 추가: `docs/plans/_templates/task.md`

product-spec + tech-spec 모두 + tech-spec 사용자 명시 승인 완료 필요. 미승인 시 → "tech-spec 사용자 승인 미완료. AGENTS.md Section 4 위반." 종료.

implement = tech-spec HOW → 실행 순서 / Phase / 변경 파일 분해. 새 설계 결정 X (있으면 tech-spec 갱신 후 재진입).

## 핵심 원칙

- **자율 분석 + 초안**. tech-spec NF / Architecture / Data Model / API contract 직접 매핑해 Phase + 변경 파일 path 추정.
- **변경 파일 = 구체 path**. "interview 영역" 류 모호 자체 금지.
- **분리 임계 자동 감지**. Task 8개+ / 단일 Phase 50줄+ → 사용자 결정 게이트.
- **추정 + 근거**. 모든 Phase 결정에 1줄 근거.
- **모호도 셀프 채점**.
- **최종 confirm Blocking**.

## Phase A — Investigation (자율)

### A-1. plan 폴더 + spec 흡수

```bash
find docs/plans -maxdepth 2 -name tech-spec.md -exec stat -f "%m %N" {} \; | sort -rn | head -5
```

`AskUserQuestion` 4개 옵션 (handoff 진행중 우선). 선택 폴더의 `product-spec.md`, `tech-spec.md` 둘 다 Read.

implement 파일 이미 존재 시:

```
question: "implement 파일 존재."
options:
  - "갱신 — 차이만 수정"
  - "덮어쓰기 — 처음부터"
  - "취소"
```

### A-2. tech-spec NF 자동 매핑

tech-spec 다음 항목 자동 추출:

- 영향 범위 → 단일 / BE+FE / lambda 결정
- 마이그레이션 + 백필 → BE Phase 1 후보 + 강결합 신호
- 이벤트 페이로드 변경 → 강결합 신호
- API contract 존재 → BE+FE 병렬 가능 검증
- Verification → implement 통합 Verification 참조원

API contract 부재 + BE+FE 영향 = STOP. "API contract 부재. tech-spec 갱신 후 재시도. (AGENTS.md Section 5)" 종료.

### A-3. 도메인 코드 / 컨벤션 단서 수집

```bash
# 영향 도메인 디렉토리
ls backend/src/main/java/.../{domain}/
ls frontend/src/{area}/

# 유사 phase 분해 참고 (인접 plan)
find docs/plans -maxdepth 2 -name implement.md -o -name implement-be.md -o -name implement-fe.md | head -5
```

### A-4. 분석 결과 정리 (내부)

- 영향 범위 + 파일 형태 (단일 / BE+FE)
- 강결합 여부 (마이그레이션 / 이벤트 페이로드)
- 변경 파일 후보 path 목록
- Phase 분할 후보
- 모호 영역 마킹

## Phase B — Synthesis (초안 설계)

### B-1. 영향 범위 + 파일 형태 결정

A-2 매핑 결과 그대로:

| 영향 | 출력 |
|---|---|
| BE only | `implement.md` |
| FE only | `implement.md` |
| lambda only | `implement.md` |
| BE+FE | `implement-be.md` + `implement-fe.md` |

강결합 (마이그레이션/이벤트) 자동 감지 시 → Phase C 결정 후보 ("BE 선행 vs FE 병렬 mock").

### B-2. Phase 분할 (자동 제안)

tech-spec Architecture / Data Model / API contract 기반:

**BE 기본 분할**:
1. Entity / Migration (Flyway DDL) — 마이그레이션 있을 시
2. Repository / Service / Domain 로직
3. Controller / Validation / DTO
4. 통합 테스트 (Testcontainers)

**FE 기본 분할**:
1. API client + 타입 (mock 가능)
2. Hook / Store
3. UI 컴포넌트
4. BE 통합 (mock 제거) + E2E

**단일 영역 분할** (작업 작음):
1. 기반 셋업
2. 핵심 로직
3. 통합 / 테스트

작업 규모 단서 부족 시 = 모호 마킹 (Phase C 결정).

### B-3. 각 Phase 디테일 (Staff 작성)

Phase 별:

- **구현 에이전트** (필수): Phase 작업 영역 따라 자동 결정
  - BE Phase → `backend`
  - FE Phase → `frontend`
  - lambda Phase → `general-purpose` (전담 에이전트 없음, 사용자에게 신규 생성 추천)
  - 영역 혼합 단일 Phase = 분할 권유 (Phase C 결정)
- **변경 파일** (구체 path, 무엇을 / 왜):
  - `backend/.../domain/interview/IntentScoreCalculator.java` — 신규 (intent 계산 도메인 서비스)
  - `backend/.../resources/db/migration/V42__add_intent_score.sql` — Flyway DDL
- **핵심 로직** (의사코드 1-5줄): tech-spec Architecture 기반
- **선행 의존**: 이전 Phase / 외부
- **Verification Hook**: 명령 + 통과 기준
  - `./gradlew test --tests IntentScoreCalculatorTest`
  - 통과 기준: 모든 케이스 green
- **커밋 메시지**: Conventional Commits (Korean) — `.claude/rules/commit.md`
  - `feat(BE): IntentScoreCalculator 추가`

**출력 형식 — Phase / Step 개요 표 컬럼 강제**:

| Phase | 제목 | 구현 에이전트 | 의존 | 커밋 |

각 Phase 본문 첫 줄에 동일 정보 재명시:

```
- **구현**: `backend` — {Phase 영역 책임 1줄}
```

근거: `.claude/rules/plan-mode.md` Section 5 ("각 task 마다 Implement 에이전트 명시"). Review 는 Phase 별 X — 구현 종료 후 통합 리뷰 1회 (Phase B-6 / 리뷰 게이트 섹션 참조).

### B-4. 분리 임계 자동 감지

Phase 누적 후 체크:

- Phase / Task 합 8개+ → 분리 권유 (Phase C)
- 단일 Phase 본문 50줄+ → 분리 권유
- 둘 다 미초과 → 단일 implement.md 진행

분리 결정 시 `docs/plans/_templates/task.md` 구조로 각 Task 상세 작성. implement 본문은 표 + 1줄 요약 + 링크.

### B-5. 통합 Verification

기본 = "tech-spec.md Verification 참조" 1줄. 추가 회귀 항목 있으면 명시. 새 정의 = STOP (tech-spec 갱신 권유).

### B-6. 리뷰 게이트 (출력 강제)

영향 범위 따라 자동 결정:

- BE only → `code-reviewer-backend`
- FE only → `code-reviewer-frontend`
- BE+FE → 둘 다 **병렬** (단일 메시지 multiple tool_use)

**출력 강제** — implement*.md 마지막 섹션에 `## 리뷰 게이트 (MANDATORY)` 반드시 포함:

```markdown
## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - {영역별 리뷰어 명시}
- [ ] 컨벤션 위반 0건 ({영역별 rules 경로})
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치
```

근거: `.claude/rules/plan-mode.md` Section "구현 후 (Post-Impl)". 누락 = 안티패턴.

## Phase C — Briefing + 결정 게이트

```
## Staff 분해 — Plan {NNN}-{slug}

### Evidence
- tech-spec NF: 영향=BE+FE, 마이그레이션+백필, API contract 있음
- 도메인 코드: backend/.../interview/* (12 클래스)
- 유사 plan: 038-... 패턴 참고 (Phase 4분할)

### 제안 plan

**파일 형태**: implement-be.md + implement-fe.md (BE+FE)
**강결합**: BE 선행 (마이그레이션 + 백필) — 추정 (확신 80%)

**BE Phase**:
1. Migration + Entity (V42 + IntentScore VO) — 12줄
2. IntentScoreCalculator 도메인 서비스 + 이벤트 publish — 18줄
3. Controller + DTO + Validation — 10줄
4. 통합 테스트 — 8줄

**FE Phase**:
1. API client + 타입 (mock 가능) — 6줄
2. useIntentScore hook + store — 10줄
3. IntentScoreBadge 컴포넌트 — 8줄
4. BE 통합 + E2E (BE 머지 후) — 6줄

**분리 임계**: Phase 8개 합 (임계 8 도달) — tasks/ 분리 권장 (확신 추정)

**통합 리뷰** (구현 종료 후 1회): code-reviewer-backend + code-reviewer-frontend 병렬

### 결정 필요
1. tasks/ 분리 — Phase 8 임계 도달. 분리할까 단일 유지?
2. FE 시작 시점 — BE 선행 권장. mock 으로 병렬 진행 가능?
```

`결정 필요` 만 `AskUserQuestion`. 질문 작성 룰 = `.claude/rules/reporting.md` "질문 친절도" 준수 (맥락 1-2줄 / 약어 풀이 / 사용자 관점 차이).

```
question: "Task 상세를 별도 파일로 쪼갤까요?
  (배경: 현재 implement.md 가 Task N개 / 본문 N줄. 분리 임계 (8 Task 또는 50줄+) 초과. 가독성 vs 파일 수 trade-off)"
options:
  - "분리 (추천) — implement*.md = Task 목차 + 링크, 상세는 tasks/be-NN-*.md. 사유: 다음 세션이 필요한 Task 만 열어 읽기 좋음"
  - "단일 유지 — 한 파일에 다 보임. 단점: 스크롤 길어짐 / Task 간 점프 비용 ↑"
```

수정 시 해당 Phase 만 재작성 후 재브리핑.

## Phase D — 파일 작성

승인 후 `Write`:

- 단일: `$PLAN_DIR/implement.md`
- BE+FE: `$PLAN_DIR/implement-be.md` + `$PLAN_DIR/implement-fe.md`
- 분리 시: `$PLAN_DIR/tasks/` `mkdir -p` 후 각 task.md

템플릿 섹션 / 헤더 / 메타데이터 블록 그대로 유지. 자의적 변형 X.

## 후속

- "implement 작성 완료. **사용자 명시 승인** 후 구현 agent 호출."
- BE → `Agent(subagent_type=backend, ...)`. 호출 시 `backend/.claude/rules/conventions.md` + `testing.md` Read 강제 (CLAUDE.md "Sub-Agent Convention Contract").
- FE → `Agent(subagent_type=frontend, ...)`. `frontend/.claude/rules/conventions.md` + `architecture.md` Read 강제.
- BE+FE → 양 agent 병렬. 강결합 시 BE 선행.
- 구현 후 지정 리뷰어 병렬 필수.
- 커밋 별도.

## 안티 패턴

- Phase A 자율 분석 생략하고 Phase 별 4-5개 자유서술 사용자에게 떠넘김.
- 변경 파일 모호 ("interview 영역") 그대로 spec 작성.
- BE+FE 작업인데 API contract 부재 진행.
- 강결합인데 FE 병렬 시작 표기 (사용자 결정 없이).
- 분리 임계 도달 무시.
- Verification 새 정의 (tech-spec 영역 침범).
- 지정 리뷰어 미명시 (memory Post-Impl Review MANDATORY 위반).
- Phase 별 **구현 에이전트** 미명시 (B-3 출력 형식 위반).
- Phase 별 리뷰 에이전트 명시 (정책 = 통합 리뷰 1회. Phase 별 리뷰는 안티패턴).
- 한 메시지에 5+ 결정 떠넘김.
- preview 없이 파일 작성.
- 템플릿 섹션 자의적 변형.
