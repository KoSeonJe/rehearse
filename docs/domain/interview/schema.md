# interview 도메인 스키마

> 대상 마이그레이션: `backend/src/main/resources/db/migration/V*.sql` 중 interview 관련
> 핵심 V 파일: V1 (`interview` / `interview_interview_types` / `interview_cs_sub_topics`), V10 (`public_id`), V17 (`user_id` FK), V41 (PK / CASCADE / 무결성 패치), V31 (interview_plan 컬럼)

## 테이블 목록

| 테이블 | 성격 | 관계 |
|--------|------|------|
| `interview` | 면접 세션 1행 = 1회 모의면접 | `interview_interview_types` N (ElementCollection) / `interview_cs_sub_topics` N / `question_set` N (외부 도메인) |
| `interview_interview_types` | 세션의 면접 유형 다중 선택 (CS_FUNDAMENTAL, RESUME_BASED, …) | `interview` 1 |
| `interview_cs_sub_topics` | 세션의 CS 세부 주제 (string set) | `interview` 1 |

> ⚠️ `interview_session` / `interview_turn` 테이블은 **존재하지 않는다**. 도메인 코드의 "session" 표현 (`session_plan_id` 등) 은 이력서 트랙의 별도 식별자이며, "턴" 은 `question_set.questions` (RESUME_INTERROGATION / FOLLOWUP) 로 표현된다.

---

## interview

### 성격
사용자가 시작한 모의면접 1회 단위. 라이프사이클: 생성 (`READY`) → 질문 생성 비동기 진행 → 시작 (`IN_PROGRESS`) → 종료 (`COMPLETED`). row 1개 = 모의면접 1회.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `public_id` | VARCHAR(36) | UNIQUE, NOT NULL, 갱신 불가 | 외부 노출용 UUID. JPA `@PrePersist` 가 INSERT 시 1회 생성 |
| `user_id` | BIGINT | FK → `users.id`, **NULL 허용** | 소유자. NULL = V17 이전 레거시 row |
| `position` | VARCHAR(20) | NOT NULL | `Position` enum (`BACKEND` / `FRONTEND` / `FULLSTACK` / `DEVOPS` / `DATA`) |
| `position_detail` | VARCHAR(100) | NULL | 직무 부가 설명 (자유 텍스트) |
| `level` | VARCHAR(20) | NOT NULL | `InterviewLevel` enum |
| `duration_minutes` | INT | NOT NULL, 5-120 | 사용자가 설정한 면접 분량 (DTO 검증) |
| `tech_stack` | VARCHAR(30) | NULL | `TechStack` enum. NULL 시 직무 디폴트 적용 (`getEffectiveTechStack()`) |
| `status` | VARCHAR(20) | NOT NULL | `InterviewStatus`: `READY` / `IN_PROGRESS` / `COMPLETED` |
| `question_generation_status` | VARCHAR(20) | NOT NULL | `QuestionGenerationStatus`: `PENDING` / `GENERATING` / `COMPLETED` / `FAILED` |
| `failure_reason` | TEXT | NULL | 질문 생성 FAILED 시 사유. 재시도 시 클리어 |
| `created_at` | DATETIME(6) | NOT NULL | `@CreatedDate` |
| `updated_at` | DATETIME(6) | NOT NULL | `@LastModifiedDate` |

### 인덱스
- PK `id`
- UNIQUE `idx_interview_public_id` (`public_id`) — 외부 식별자 충돌 방지
- `idx_interview_user_id` (`user_id`) — 사용자별 목록 / 카운트 조회 (`findAllByUserId`, `countByUserId*`)
- FK `fk_interview_user` (`user_id` → `users.id`) — V17

### 불변 / 정책

- **`public_id` 갱신 금지** (`updatable = false`). INSERT 1회 후 외부 공개 식별자로 고정.
- **`user_id` NULL 허용**. V17 이전 row 마이그레이션 정책 미정 (❓TODO 참조). 신규 row 는 `@AuthenticationPrincipal Long userId` 로부터 항상 NOT NULL 로 생성됨.
- **상태 전이는 `Interview.updateStatus()` + `InterviewStatus.canTransitionTo()` 강제**:
  - `READY → IN_PROGRESS` (단, `question_generation_status = COMPLETED` 필요 — `InterviewService.updateStatus` 추가 검증)
  - `IN_PROGRESS → COMPLETED` (`InterviewCompletionService` 30초 polling 또는 명시 PATCH)
  - 외 모든 전이 금지. 위반 시 `IllegalStateException` → `INTERVIEW_002 INVALID_STATUS_TRANSITION (409)`
- **삭제 정책**: hard delete. `InterviewDeletionService` 가 자식 (`question_answer` / `timestamp_feedback` / `question_set_feedback` / `question_set_analysis` / `question_set`) 를 명시 순서로 선삭제 후 본 row 삭제. 단 `INTERVIEW_009 CANNOT_DELETE_COMPLETED (400)` — 완료 면접 삭제 차단 정책: **차단** (Issue #404 #1 — 정책 코드 갭. `InterviewStatus.isDeletable()` 은 항상 true 반환 → 별도 검증 필요).
- **soft-delete 미도입**. 모든 삭제는 hard. ❓TODO(seonje, 2026-05-15): soft-delete 도입 여부.
- **복합 invariant**:
  - `interview_types` 가 `RESUME_BASED` 포함 시 size = 1 (배타). Service 단 `validateResumeExclusivity` 강제 (`RESUME_EXCLUSIVITY_VIOLATION`).
  - `tech_stack` 이 `Position` 의 허용 셋에 없으면 `INVALID_TECH_STACK (400)`.
  - `question_generation_status = COMPLETED` 가 아니면 `READY → IN_PROGRESS` 차단 (`INTERVIEW_004`).

### 마이그레이션 히스토리

- `V1__init_schema.sql` — 테이블 생성 (id / position / position_detail / level / duration_minutes / status / created_at / updated_at)
- `V6__add_question_generation_status...` — `question_generation_status` 추가 (질문 생성 비동기화)
- `V9__add_tech_stack_column.sql` — `tech_stack` 컬럼 추가
- `V10__add_public_id_to_interview.sql` — `public_id` 추가 + 기존 row UUID backfill + UNIQUE 인덱스
- `V13__add_vocal_feedback_columns.sql` — 보컬 피드백 컬럼 (interview 테이블 자체에는 영향 미미)
- `V17__add_user_id_to_interview.sql` — `user_id` FK 추가 (NULL 허용 — 기존 row backfill 부재)
- `V40__followup_unique_order_index.sql` — followup 중복 삽입 방지 (자식 테이블)
- `V41__integrity_patches.sql` — `interview_interview_types` / `interview_cs_sub_topics` PK 추가, CASCADE FK 재생성, ElementCollection 중복 row 정리

---

## interview_interview_types

### 성격
JPA `@ElementCollection` — 한 면접 세션의 면접 유형 다중 선택 결과. `Set<InterviewType>` 매핑.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `interview_id` | BIGINT | PK 일부, FK → `interview.id` ON DELETE CASCADE | 부모 면접 |
| `interview_type` | VARCHAR(30) | PK 일부, NOT NULL | `InterviewType` enum 값 |

### 인덱스
- PK (`interview_id`, `interview_type`) — V41 추가 (이전엔 PK 부재 → 중복 row 발생)
- `idx_types_interview_id` (`interview_id`) — V1
- FK `fk_types_interview` (`interview_id`) ON DELETE CASCADE — V41 재생성

### 불변 / 정책
- 한 (`interview_id`, `interview_type`) 조합은 1회만 (PK 보장).
- `RESUME_BASED` 가 포함되면 다른 type 동시 보유 금지 (Service `validateResumeExclusivity`).
- `InterviewType.cacheStrategy` = `CACHEABLE` / `FRESH` 분기. `RESUME_BASED` 만 `FRESH` → 항상 실시간 생성.

### 마이그레이션 히스토리
- `V1__init_schema.sql` — 테이블 생성
- `V41__integrity_patches.sql` — 중복 dedup + PK 추가 + CASCADE FK

---

## interview_cs_sub_topics

### 성격
JPA `@ElementCollection<String>` — CS 세부 주제 (예: 운영체제 / 네트워크) 다중 선택.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `interview_id` | BIGINT | PK 일부, FK → `interview.id` ON DELETE CASCADE | 부모 면접 |
| `cs_sub_topic` | VARCHAR(50) | PK 일부, NOT NULL (V41 이후) | 자유 문자열 (CS 주제 라벨) |

### 인덱스
- PK (`interview_id`, `cs_sub_topic`) — V41 추가
- `idx_cs_topics_interview_id` (`interview_id`) — V1
- FK ON DELETE CASCADE — V41

### 불변 / 정책
- 컬럼 자체에 enum 제약 없음 (FE 가 미리 정의된 셋만 선택). 서버측 화이트리스트 검증 부재 — ⚠️ **정책-코드 갭** (추적: 별도 추후 확인 필요): 임의 문자열 INSERT 가능.
- (`interview_id`, `cs_sub_topic`) 중복 불가.

### 마이그레이션 히스토리
- `V1__init_schema.sql` — 테이블 생성 (NULL 허용)
- `V41__integrity_patches.sql` — NOT NULL 강제 + PK + CASCADE

---

## InterviewRuntimeState (DB 미사용)

### 성격
**DB 테이블 아님.** 메모리 객체 (`POJO`) — Caffeine 캐시 (`InterviewRuntimeStateCache`) 에만 보유. `interviewId` 키로 in-memory 보관, 재시작 시 휘발 (재계산 허용).

### 보유 필드 (요약)
- `coveredClaims` (Deque) — 인터뷰 진행 중 다뤄진 claim 목록
- `activeChain` (List<Long>) — 이력서 트랙 RESUME_INTERROGATION 활성 chain id 스택
- `playgroundTurns` (AtomicInteger) — 이력서 PLAYGROUND 모드 턴 카운터
- `turnAnalysisCache` (Map<Long, TurnAnalysis>) — Step A AnswerAnalyzer 결과
- `resumeSkeletonCache` / `interviewPlanCache` — 이력서 트랙 캐시
- `compactedDialogueSummaries` — L3 dialogue compaction 결과
- `startedAt` (Instant) — 세션 시작 시각 (휘발성)
- `resumeMode` — `PLAYGROUND` / `INTERROGATION` 등 FSM 상태

### 정책
- V29 마이그레이션은 **placeholder** (`SELECT 1`). DDL 영향 없음.
- 캐시 evict 시 `FollowUpService.isResumeTrack` 가 `Interview.interviewTypes` 와 `ResumeSkeletonPersister` 조회로 재초기화.
- 동시성: `ConcurrentHashMap`, `AtomicInteger`, `CopyOnWriteArrayList`, `volatile` 사용.

---

## 연관 의존성

`interview` 도메인 테이블 / 코드가 직접 호출 / 참조하는 외부 패키지·클래스. `import` / 호출 그래프 근거.

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.user.entity.User` | 사용자 엔티티 (FK 대상) | called-by — `interview.user_id` FK |
| `com.rehearse.api.domain.question.entity.QuestionSet` | 면접의 질문 세트 (1:N) | calls — `QuestionSetRepository.findByInterviewIdWithQuestions` (Query / Deletion) |
| `com.rehearse.api.domain.question.service.QuestionSetService` | 질문세트 스킵 처리 | calls — `InterviewService.skipRemainingQuestionSets` |
| `com.rehearse.api.domain.question.repository.QuestionAnswerRepository` | 답변 정리 | calls — `InterviewDeletionService` (CASCADE 보완) |
| `com.rehearse.api.domain.feedback.repository.{Timestamp,QuestionSet}FeedbackRepository` | 피드백 정리 | calls — `InterviewDeletionService` |
| `com.rehearse.api.domain.question.repository.QuestionSetAnalysisRepository` | 분석 결과 정리 | calls — `InterviewDeletionService` |
| `com.rehearse.api.domain.resume.entity.{ResumeSkeleton, InterviewPlan}` | 이력서 트랙 캐시 객체 | reads — `InterviewRuntimeState` 보유 |
| `com.rehearse.api.domain.resume.service.{ResumeInterviewOrchestrator, ResumeSkeletonPersister, InterviewPlanPersister, ResumeInterviewPlanner}` | 이력서 트랙 분기 | calls — `FollowUpService.delegateToResumeOrchestrator` |
| `com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent` | 턴 완료 이벤트 | event-publisher — `FollowUpService.publishTurnCompletedEvent` |
| `com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent` | 질문 생성 트리거 | event-publisher — `InterviewCreationService` / `InterviewService.retryQuestionGeneration` |
| `com.rehearse.api.domain.interview.event.InterviewCompletedEvent` | 인터뷰 완료 이벤트 | event-publisher — `InterviewCompletionService` (30초 polling) |
| `com.rehearse.api.infra.ai.PdfTextExtractor` | 이력서 PDF 텍스트 추출 | calls — `InterviewCreationService` |
| `com.rehearse.api.infra.ai.AiClient` | OpenAI/Claude 진입점 | calls — `IntentClassifier`, `AnswerAnalyzer`, `FollowUpQuestionWriter` |
| `com.rehearse.api.infra.ai.context.InterviewContextBuilder` | 프롬프트 컨텍스트 조립 | calls — Step A / Step B 전 |
| `com.rehearse.api.infra.ai.metrics.AiCallMetrics` | AI 호출 메트릭 | calls — FollowUpService skip 분기 |
| `com.github.benmanes.caffeine.cache.Cache` | runtime state 캐시 backend | wraps — `InterviewRuntimeStateCache` |
| `com.rehearse.api.global.util.FileHasher` | 이력서 파일 해시 (사전생성 캐시 키) | calls — `InterviewCreationService` |
| `com.rehearse.api.global.config.IntentClassifierProperties` | intent fallback 임계값 | reads — `IntentClassifier` |

---

## 정책 출처

- 비즈니스 룰 본문 = 본 schema.md 각 테이블 "불변 / 정책" 섹션
- 임계값 / 디폴트 (직접 인용):
  - `rehearse.interview.policy.standard.max-follow-up-rounds: 2` (`application.yml:55`) — CS 트랙 follow-up 최대 라운드
  - `ResumeTrackPolicy.HARD_TURN_CAP = 7` — 이력서 트랙 follow-up 하드 캡 (코드 상수)
  - `rehearse.intent-classifier.fallback-on-low-confidence: 0.7` (`application.yml:58`) — intent 신뢰도 임계
  - `rehearse.intent-classifier.off-topic-consecutive-limit: 3` (`application.yml:60`) — OFF_TOPIC 연속 escalation 임계
  - `InterviewCompletionService` `@Scheduled(fixedDelay = 30_000)` — 완료 polling 30초
  - `jwt.expiration-ms: 604800000` (`application-dev.yml:58`) — JWT 7일 (소유자 검증 컨텍스트)
- ❓TODO(사용자 확인) — Issue #404 비스코프 항목:
  1. `user_id` NULL 레거시 row 마이그레이션 정책 (백필 / 강제 NOT NULL / 보류)
  2. 상태 전이 락 정책 (낙관락 / 비관락 / 무락 — 동시 PATCH 시 race)
  3. soft-delete 도입 여부 (감사 / 복구 요건 발생 시)
  4. `getStats` 통계 윈도우 = `Asia/Seoul` 고정 (`InterviewQueryService:67`) — 다중 타임존 사용자 정책
  5. AI 메트릭 알람 임계 (intent fallback 비율 / Step A skip 비율 / 503 비율)
- ⚠️ **정책-코드 갭** (Issue #404 추적):
  - #1 — `InterviewStatus.isDeletable()` 항상 true vs 정책 "완료 면접 삭제 차단" → A안 채택 (정책: 차단)
  - #3 — `cs_sub_topics` 화이트리스트 서버 검증 부재 → A안 도입 예정 (서버 측 enum / 사전 정의 셋 검증)
