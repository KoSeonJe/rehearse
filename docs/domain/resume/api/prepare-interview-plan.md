# API: 인터뷰 플랜 준비 (prepare plan)

> Endpoint: 별도 endpoint 없음 (`ResumeTrackInitiator.initiate` 내부)
> Action: skeleton + duration 기반으로 `InterviewPlan` (project plans / chains / claims 트리) 생성·검증·저장
> 관련 테이블: `interview_plan` (write, INSERT-only) / `resume_skeleton` (read)
> 관련 외부 의존: OpenAI GPT-4o-mini → Claude Haiku (resume-planner)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| arg | `interviewId` | Long | required | 대상 인터뷰 |
| arg | `userId` | Long | required | 인터뷰 소유자 |
| arg | `durationMin` | int | required, > 0 | 인터뷰 총 길이 (분). null 시 30 default |

> skeleton 은 `resume_skeleton` 에서 조회. 호출자가 ingest 완료 보장.

---

## 출력 (정상)

| 필드 | 타입 | 의미 |
|------|------|------|
| `interviewPlanId` | Long | 저장된 plan PK |
| `sessionPlanId` | string | LLM 산출 trace 식별자 (관찰성용) |
| `chainCount` | int | 생성된 chain 수 (관찰용) |

부수 효과: `interview_plan` 1행 INSERT (V25 UNIQUE).

## 출력 (실패)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 404 | `PLAN_NOT_FOUND` | skeleton 부재 (`resume_skeleton` row 없음) |
| 409 | `INTERVIEW_PLAN_ALREADY_ASSIGNED` | 이미 plan 부착됨 (UNIQUE 위반 — `assignToInterview` 재할당 거부) |
| 500 | `INVALID_PLAN` | LLM plan 응답 schema 검증 2회 실패 / chain_id hallucination 재시도 후도 missing |
| 500 | `ORPHAN_CHAIN` / `ORPHAN_CLAIM` / `PROJECT_NOT_FOUND_IN_SKELETON` | 트리 invariant 위반 |
| 502 | `AI_PARSE_FAILED` / `AI_CLIENT_ERROR` | LLM 결함 |
| 503 | `AI_SERVICE_UNAVAILABLE` | OpenAI + Claude 모두 실패 |
| 504 | `AI_TIMEOUT` | LLM 응답 타임아웃 |
| 429 | `RATE_LIMITED` | Resilience4j |

---

## 흐름

### 1. skeleton 조회
- `ResumePlanPreparationService.prepare(interviewId, userId, durationMin)` 진입.
- `ResumeSkeletonPersister.findByInterviewId` — 부재 시 `RESUME_PLAN_NOT_READY` (409) (skeleton 미부착 = plan 작성 불가).
- `durationMin` null fallback = 30 (`DEFAULT_DURATION_MINUTES`).

### 2. plan 생성 (LLM)
- `ResumeInterviewPlanner.plan(skeleton, durationMin)` 호출.
- `ResumeInterviewPlannerPromptBuilder` 로 prompt 구성 (yml `rehearse.resume-planner.{model:gpt-4o-mini, temperature:0.3, max-tokens:2048}`).
- Provider: GPT-4o-mini → Claude Haiku fallback (`ResilientAiClient`).
- `skeleton.candidateLevel` null fallback = `MID` (`DEFAULT_USER_LEVEL`).

### 3. plan 검증 (adapter + validator)
- `ResumeInterviewPlanAdapter.execute` — 응답 raw JSON → 도메인 객체.
  - `chain_id` allowlist 외 필드 drop (skeleton 의 `synthesizeChainId(projectId, topic)` 만 허용).
  - drop 후 `primaryChains.isEmpty()` 인 ProjectPlan 발견 시 `withSchemaRetryHint("chain_id 는 ALLOWED_CHAIN_IDS 안에서만...")` 1회 재호출 + `parseOrRetry` 재실행.
  - 재시도 후도 missing → `INVALID_PLAN` (500).
  - `projectName` resolution: skeleton 의 `Project.projectName` 우선. LLM 응답 projectName 이 skeleton 과 mismatch 시 WARN 로그 + skeleton 값 사용 (no-throw, 길이만 로깅, 원문 미노출).
- `ResumeInterviewPlanValidator.validate` — 트리 정합성 (chain → claim 부모 참조, projectId 매칭).
  - `ORPHAN_CHAIN` / `ORPHAN_CLAIM` / `PROJECT_NOT_FOUND_IN_SKELETON` (500).

### 4. 저장
- `InterviewPlanPersister.save(interviewId, plan)` — `assignToInterview()` Entity mutator 호출 → `interview_plan` INSERT.
- `interview_id` UNIQUE (V25) — 중복 시 `DataIntegrityViolationException` catch + WARN + 재조회. `assignToInterview()` Entity-side 검증으로 재할당 시 `INTERVIEW_PLAN_ALREADY_ASSIGNED` (409).

### 5. 응답
- plan id + sessionPlanId + chainCount 반환.

---

## 외부 호출 상세

- Sampling: yml `rehearse.resume-planner.{model:gpt-4o-mini, temperature:0.3, max-tokens:2048}`
- Retry layers (총 2 layer):
  - **Parser layer** (`AiResponseParser.parseOrRetry`): 1차 파싱 실패 → schema-hint 1회 재호출 → 2차 실패 `PARSE_FAILED`
  - **Adapter layer** (`ResumeInterviewPlanAdapter`): chain_id hallucination drop 후 missing 시 `withSchemaRetryHint` 1회 재호출 + parseOrRetry 재실행 → 재실패 `INVALID_PLAN`
  - 두 layer 합산 worst path 외부 호출 = 4회 (1차 chat + 1차 parser-retry + hallucination chat + hallucination parser-retry). + provider-internal retry (OpenAI 2 + Claude 3) 별도 누적
- `CLIENT_ERROR` / `PARSE_FAILED` 즉시 throw (fallback 진입 X)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| skeleton 부재 상태로 호출 | 409 `RESUME_PLAN_NOT_READY` |
| 이미 plan 부착된 인터뷰 재호출 | 409 `INTERVIEW_PLAN_ALREADY_ASSIGNED` (Entity `assignToInterview()` 가드 + V25 UNIQUE) |
| LLM 응답 chain_id 필드 누락 (1차) | adapter schema-hint 1회 재호출 |
| LLM 응답 chain_id 필드 누락 (2차) | 500 `INVALID_PLAN` |
| LLM plan 의 chain.parentClaimId 가 claim 트리에 없음 | 500 `ORPHAN_CHAIN` |
| LLM plan 의 claim.projectId 가 skeleton 에 없음 | 500 `PROJECT_NOT_FOUND_IN_SKELETON` |
| LLM plan projectPlans priority 중복 / 역순 | 500 `INVALID_PLAN` (Entity constructor invariant) |
| OpenAI 5xx → Claude 5xx | 503 `AI_SERVICE_UNAVAILABLE` |
| 동일 인터뷰 동시 prepare 호출 | UNIQUE 위반 → 후행 trip catch 후 idempotent skip 또는 재조회 |
| `durationMin` null | default 30 사용 |

---

## 상태 전이
N/A — `interview_plan` INSERT-only, mutable status 없음.

---

## 관찰성

- **로그**: `[ResumePlanPreparationService]` / `[ResumeInterviewPlanner]` / `[ResumeInterviewPlanAdapter]` / `[ResumeInterviewPlanValidator]` / `[InterviewPlanPersister]`
  - key fields: `interviewId`, `userId`, `durationMin`, `sessionPlanId`, `projectCount`, `chainCount`, `claimCount`, `provider`, `model`, `latencyMs`
- **메트릭** (`AiCallMetrics`):
  - `rehearse.ai.call.duration{call.type=RESUME_INTERVIEW_PLANNER, model, provider, fallback, outcome}`
  - `rehearse.ai.parse.fail.total{stage=first|second, call.type=RESUME_INTERVIEW_PLANNER}`
  - `rehearse.ai.call.tokens.*`
- **알람**: 미정 (Issue #408)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.resume.service.ResumePlanPreparationService` | 진입점 / 트랜잭션 조립 | calls |
| `com.rehearse.api.domain.resume.service.ResumeInterviewPlanner` | LLM plan 빌더 | calls |
| `com.rehearse.api.infra.ai.adapter.ResumeInterviewPlanAdapter` | LLM raw → 도메인 + chain hallucination retry | calls |
| `com.rehearse.api.domain.resume.service.ResumeInterviewPlanValidator` | 트리 invariant 검증 | calls |
| `com.rehearse.api.domain.resume.service.InterviewPlanPersister` | DB persist + idempotent skip | calls — persister |
| `com.rehearse.api.infra.ai.ResilientAiClient` | LLM 이중화 | calls |
| `com.rehearse.api.infra.ai.AiResponseParser` | parse + schema-hint | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeInterviewPlannerPromptBuilder` | planner prompt | calls |
| `com.rehearse.api.domain.resume.service.ResumeSkeletonPersister` | skeleton 조회 | calls |
| `com.rehearse.api.domain.question.service.ResumeTrackInitiator` | resume 트랙 초기화 진입점 | called-by |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/resume/schema.md#interview_plan` (인터뷰 1:1 / INSERT-only / CASCADE / Entity invariant)
- LLM 설정 출처: `application-*.yml` `rehearse.resume-planner.*`
- Retry / fallback: `ResilientAiClient`, `AiResponseParser.parseOrRetry`, `ResumeInterviewPlanAdapter.withSchemaRetryHint`
- Default fallback: `DEFAULT_DURATION_MINUTES=30`, `DEFAULT_USER_LEVEL="MID"`
- ❓ 잔존 결정 항목: Issue #408
  - B1 dead config (`playground-max-turns`, `chain-max-depth`)
  - C3 SchemaExampleRegistry 에 `GeneratedInterviewPlan` 등록 여부 (1차 파싱 실패 회복률 향상)
