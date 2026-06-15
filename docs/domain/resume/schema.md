# resume 도메인 스키마

> 대상 마이그레이션: `backend/src/main/resources/db/migration/V*__*.sql` 중 resume 관련 (V24 / V25 / V28 / V31 / V35 / V39 / V41 / V42 / V44)

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `resume_skeleton` | 인터뷰 1건당 LLM 추출 이력서 골격 1행 (projects / claims / implicitCsTopics 포함 JSON) | — (1:1 with interview) |
| `interview_plan` | 인터뷰 1건당 ProjectPlan 리스트 1행 (Playground / Interrogation phase 메타) | — (1:1 with interview) |

> resume 트랙의 **질문 자체는 `question` 테이블** (외부 도메인) 에 저장. 단 `RESUME_OPENER / RESUME_PLAYGROUND / RESUME_INTERROGATION` 3종은 resume FSM 산출. `question_set` (외부 도메인) 의 `RESUME_BASED` 카테고리도 인터뷰당 1행 (V44 unique).

---

## resume_skeleton

### 성격
LLM 이 이력서 텍스트로부터 추출한 정규화 골격. `interview_id` 단위 1행 (V28 UNIQUE). Playground / Interrogation FSM 의 입력 단일 소스.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `interview_id` | BIGINT | NOT NULL, UNIQUE (V28), FK → interview.id ON DELETE CASCADE | 1:1 인터뷰 결합 |
| `file_hash` | VARCHAR(64) | NOT NULL | SHA-256 hex. 재업로드 동일성 판정 키 (`ResumeIngestionService` DB 히트 조건) |
| `candidate_level` | VARCHAR(16) | NOT NULL | `JUNIOR / MID / SENIOR` (대소문자 무시 파싱, fallback=JUNIOR) |
| `target_domain` | VARCHAR(32) | NULL | 직무 도메인 힌트 (LLM 자유 텍스트, 모호 시 NULL) |
| `skeleton_json` | JSON | NOT NULL | `ResumeSkeleton` record 직렬화. 컬럼 외 모든 정책 데이터 (projects, claims, implicitCsTopics, interrogationPriorityMap) 가 여기 담김 |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 시각 (갱신 트리거 없음) |

### 인덱스
- PK `id`
- `idx_interview` (`interview_id`) — V24
- `idx_file_hash` (`file_hash`) — V24. 코드 사용처 부재 (현재 `findByInterviewId` 만 사용). 정리 여부 = Issue #408
- `uk_resume_skeleton_interview` UNIQUE (`interview_id`) — V28

### 불변 / 정책
- `interview_id` UNIQUE — 인터뷰당 skeleton 1행 강제 (V28). 중복 INSERT 시 `DataIntegrityViolationException` → `ResumeSkeletonPersister` 가 재조회로 idempotent 처리.
- `skeleton_json` 갱신 패스 코드상 부재 — 현재 INSERT-only. 재업로드 hash 다를 시 UNIQUE 충돌 → 재조회 → 구 skeleton 반환. 즉 인터뷰 시작 후 이력서 교체 미지원 (Issue #408 A2).
- soft delete 없음 — interview ON DELETE CASCADE 로 동시 삭제.
- `candidate_level` 도메인 enum 4값 (`CandidateLevel.JUNIOR/MID/SENIOR`). DB CHECK 제약 부재 — 비정상 값 진입 시 fallback=JUNIOR.

### 마이그레이션 히스토리
- `V24__create_resume_skeleton.sql` — 테이블 + idx_interview + idx_file_hash + FK CASCADE.
- `V28__add_unique_to_resume_skeleton.sql` — `interview_id` UNIQUE 추가 (인터뷰당 1행 보장).

---

## interview_plan

### 성격
Skeleton 으로부터 LLM Planner 가 산출한 인터뷰 진행 계획. `ProjectPlan` 리스트 (priority 오름차순 / 중복 금지) + Playground / Interrogation phase 메타. 인터뷰당 1행 (V25 UNIQUE).

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `interview_id` | BIGINT | UNIQUE (V25), FK → interview.id ON DELETE CASCADE, NULL 허용 (Entity-side `assignToInterview()` 늦은 바인딩) | 1:1 인터뷰 결합 |
| `session_plan_id` | VARCHAR(255) | NOT NULL (V31 추가) | LLM 산출 plan 식별자 (관찰성 / 추적용. 복원 시 PK 가 아님) |
| `total_projects` | INT | NOT NULL (V31 추가) | `projectPlans.size()` 캐시 (역정규화) |
| `plan_json` | JSON | NOT NULL | `List<ProjectPlan>` (`ProjectPlanListJsonConverter`). priority 오름차순 + 중복 금지 invariant |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 시각 |

> V31 에서 `duration_hint_min` 도 추가됐다가 V39 에서 DROP. 현 schema 미보유.

### 인덱스
- PK `id`
- `interview_id` UNIQUE (V25 inline)

### 불변 / 정책
- `interview_id` UNIQUE — 인터뷰당 1행. 중복 INSERT 시 `DataIntegrityViolationException` → `InterviewPlanPersister` idempotent skip.
- `assignToInterview()` Entity 메서드 — 한번 할당된 plan 재할당 금지 (`INTERVIEW_PLAN_ALREADY_ASSIGNED` 409).
- `projectPlans` priority 중복·역순 금지 (Entity constructor invariant).
- `plan_json` 갱신 패스 부재 — INSERT-only.
- soft delete 없음 — interview ON DELETE CASCADE.
- `session_plan_id` UNIQUE 제약 부재 — DB 강제 X (코드상 generation 한 번이라 사실상 unique).

### 마이그레이션 히스토리
- `V25__create_interview_plan.sql` — 테이블 + interview_id UNIQUE + FK CASCADE.
- `V31__add_interview_plan_columns.sql` — `session_plan_id`, `duration_hint_min`, `total_projects` 추가.
- `V39__drop_interview_plan_duration_hint_min.sql` — `duration_hint_min` 컬럼 제거 (Planner 가 hint 미사용).

---

## resume 가 사용하는 외부 테이블 (참조 / 쓰기)

resume 도메인은 **`question`, `question_set`** 에 직접 INSERT. `ResumeQuestionPersister` 경유.

| 테이블 | 도메인 | resume 의 사용 |
|-------|--------|---------------|
| `question_set` | question | `category=RESUME_BASED` 1행을 `find or create` (V44 으로 인터뷰당 RESUME_BASED 1행 UNIQUE) |
| `question` | question | FSM 단계별 INSERT — `RESUME_OPENER / RESUME_PLAYGROUND / RESUME_INTERROGATION` (V42 로 chk_question_track_meta_v2 + chain_id / chain_step_type / project_id 컬럼 DROP — application enum 만 신뢰) |
| `interview` | interview | `interview_types` 에 `RESUME_BASED` 포함 시 트랙 진입. resume_skeleton.interview_id / interview_plan.interview_id FK 부모 |

> 외부 테이블 DDL 변경 (V41 / V44) 이 resume 트랙 제약과 직결 — 이 도메인 정책 갱신 시 함께 검토.

---

## 연관 의존성

도메인 외부에서 resume 데이터 / 상태에 직접 접근하거나, resume 가 직접 호출하는 패키지·클래스. `import` / 호출 그래프 근거.

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.entity.InterviewRuntimeState` | Caffeine 캐시 보관 세션 런타임. resume 트랙은 여기에 `ResumeMode`, `ChainStateTracker`, `playgroundTurns`, `playgroundCumulativeLength`, `resumeOrderCounter`, `resumeSkeletonCache`, `interviewPlanCache` 보관 | resume → interview entity (resume entity 4종 import — 양방향 결합 / Issue #408 B4) |
| `com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache` | Caffeine 기반. `getOrInit / get / update / evict`. resume 의 모든 모드 핸들러가 사용 | calls — interview cache wrapper |
| `com.rehearse.api.global.config.RuntimeCacheConfig` | Caffeine 설정 — `expireAfterWrite(8h)`, `maximumSize(10_000)`, Micrometer 노출 | configuration |
| `com.rehearse.api.domain.interview.service.TurnAnalysisPipeline` | text-only IntentClassifier + AnswerAnalyzer 조합. `ResumeInterviewOrchestrator` 진입 시 호출 | calls |
| `com.rehearse.api.domain.interview.service.IntentDispatcher` | non-ANSWER intent 분기 (`OFF_TOPIC` 3회 누적 → `GIVE_UP` escalate, yml `off-topic-consecutive-limit: 3`) | calls |
| `com.rehearse.api.domain.interview.service.IntentClassifier` | yml `rehearse.intent-classifier.fallback-on-low-confidence: 0.7` 미만 시 `forceAnswer()`. temp 0.1, maxTokens 200 | calls |
| `com.rehearse.api.domain.interview.service.AnswerAnalyzer` | temp 0.2, maxTokens 800. L1 False Negative Guard (`claims=[] AND quality≤1 AND action!=CLARIFICATION` → 강제 CLARIFICATION) | calls |
| `com.rehearse.api.domain.interview.service.InterviewFinder` | Interview entity 조회 — durationMinutes / level / userId / interviewTypes | calls |
| `com.rehearse.api.domain.interview.entity.Interview` | `validateOwner(userId)` 권한 검증 (`FollowUpTransactionHandler.loadFollowUpContext` 진입 시 호출). `getTrack()` resume 트랙 분기 기준 | called-by guard |
| `com.rehearse.api.domain.interview.entity.AnswerAnalysis / IntentResult / IntentType / TurnAnalysisResult` | 턴 분석 산출 VO | data flow |
| `com.rehearse.api.domain.interview.dto.FollowUpResponse / FollowUpRequest.FollowUpExchange` | resume 트랙 응답 DTO + 이전 대화 입력 | response / input shape |
| `com.rehearse.api.domain.interview.service.FollowUpService` | resume 트랙 진입 라우터. `isResumeTrack()` 판정 후 `resumeOrchestrator.processUserTurn` 위임 | called-by — resume 진입점 |
| `com.rehearse.api.domain.question.service.ResumeTrackInitiator` | `RESUME_BASED` interview type 시 `QuestionGenerationService` 가 호출 → `ResumePlanPreparationService.prepare` + `runtimeStateStore.getOrInit` + `ResumeInterviewOrchestrator.startSession` 트리거 | called-by — 사전준비 진입점 |
| `com.rehearse.api.domain.question.service.QuestionGenerationService` | resume 트랙 분기 라우팅 (interview type 기반) | called-by |
| `com.rehearse.api.domain.question.entity.Question / QuestionType` | `QuestionType` enum 3종 사용 (`RESUME_OPENER / RESUME_PLAYGROUND / RESUME_INTERROGATION`). `Question.resume(...)` factory 강제 | persister write |
| `com.rehearse.api.domain.question.repository.QuestionRepository` | `ResumeQuestionPersister` 경유 INSERT (`QuestionSet.questions` collection 미갱신 — Issue #408) | persister |
| `com.rehearse.api.domain.question.entity.QuestionSet / QuestionSetCategory.RESUME_BASED` | 인터뷰당 RESUME_BASED set 1행 (V44 UNIQUE) | persister write |
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | `findByInterviewIdAndCategory`, `countByInterviewId` 으로 캐시 / orderIndex 산출 | reads + writes |
| `com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent` | `ofResumeTrack(...)` 팩토리. resumeMode / currentChainLevel / resumeSkeleton 페이로드 포함 (resume entity 2종 import) | event-publisher (`ResumeTurnEventPublisher` 발행) |
| `com.rehearse.api.infra.ai.AiClient` (=`ResilientAiClient`) | LLM 단일 진입점. OpenAI primary (내부 retry 2회) + Claude fallback (내부 retry 3회). `CLIENT_ERROR / PARSE_FAILED` 즉시 throw (fallback X). worst path 5회 외부 호출 | calls |
| `com.rehearse.api.infra.ai.AiResponseParser` | `parseOrRetry` — 1차 실패 시 schema-hint 1회 재호출 → 실패 시 `PARSE_FAILED` | calls |
| `com.rehearse.api.infra.ai.PdfTextExtractor` | PDF → 정규화 텍스트. `MAX_FILE_SIZE=5MB`, `MAX_TEXT_LENGTH=5000`, magic byte 검증, 한국어 토큰 복원 (max 3 iter) | calls |
| `com.rehearse.api.infra.ai.adapter.ResumeInterviewPlanAdapter` | Planner LLM 응답 → InterviewPlan 변환 + `chain_id` allowlist 필터 + chain 부족 시 `withSchemaRetryHint` 1회 재시도 → 재실패 `INVALID_PLAN` | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeExtractorPromptBuilder` | Skeleton 추출 system / user prompt | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeInterviewPlannerPromptBuilder` | Planner prompt — yml `rehearse.resume-planner.{model:gpt-4o-mini, temperature:0.3, max-tokens:2048}` | calls |
| `com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder` | Opener / Responder JSON prompt + parse | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder` | Interrogation level 진행 prompt + nextAction 결정 | calls |
| `com.rehearse.api.infra.ai.context.layer.SkeletonCallType` | `RESUME_EXTRACTOR / RESUME_PLAYGROUND_OPENER / RESUME_PLAYGROUND_RESPONDER / RESUME_CHAIN_INTERROGATOR / RESUME_INTERVIEW_PLANNER` callType label (관찰성) | enum value |
| `com.rehearse.api.infra.ai.metrics.AiCallMetrics` | `rehearse.ai.call.duration` Timer + `rehearse.ai.parse.fail.total` Counter + token counters | metrics |
| `com.rehearse.api.global.util.FileHasher` | resume file SHA-256 hash (캐시 키) | calls |
| `com.rehearse.api.global.exception.BusinessException` + `ResumeErrorCode` (RESUME_001~011) + `ResumePlannerErrorCode` (RESUME_PLANNER_001~003) | 도메인 예외 | error mapping |
| `com.rehearse.api.global.exception.GlobalExceptionHandler` | `BusinessException` → 그대로 매핑 / `RequestNotPermitted` → 429 / `Exception.class` → 500 (`IllegalStateException` 포함 — Issue #408 C1) | exception mapping |
| `org.springframework.context.ApplicationEventPublisher` | `TurnCompletedEvent` 발행 | event-publisher |

> resume 도메인은 `file / user / auth / admin / servicefeedback / reviewbookmark` 도메인 직접 import 없음.

---

## 정책 출처

- 마이그레이션 V24 / V25 / V28 / V31 / V35 / V39 / V41 / V42 / V44.
- 코드 상수 인용: 위 표 본문.
- ❓ 잔존 결정 항목: GitHub Issue #408
  - A1 audio 정책 / A2 PDF 보존 / A3 Fallback 동기화 / A4 권한 검증 위치
  - B1 dead config / B2 FOLLOW_UP_MODEL 하드코딩 / B3 L1 guard 중복 / B4 entity 양방향 결합
  - C1 IllegalStateException 래핑 / C2 evict 명시 호출 / C3 SchemaExampleRegistry 등록
