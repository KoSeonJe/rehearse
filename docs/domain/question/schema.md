# question 도메인 스키마

> 대상 마이그레이션: `backend/src/main/resources/db/migration/V*.sql` 중 question 도메인 관련
> 핵심 V 파일: V4 (`question` / `question_set` / `question_answer (=question_set_answer)` 생성), V5 (`question_set_answer → question_answer` rename + `FOLLOWUP_*` → `FOLLOWUP` 통합), V11 (`question_pool` 생성 + `question.question_pool_id` FK), V20 (pool 미사용 컬럼 cleanup), V23 (`tts_text` / `tts_content`), V35 / V41 / V42 (resume meta 도입 → 5-way CHECK 강화 → 제거), V40 (followup unique), V44 (RESUME_BASED 단일화 + UNIQUE)
>

## 테이블 목록

| 테이블 | 성격 | 관계 |
|--------|------|------|
| `question` | 질문 1행 = 1개 질문 (MAIN / FOLLOWUP / RESUME_*) | `question_set` 1 / `question_pool` 1 (옵션) / `question_answer` 1:N |
| `question_pool` | 캐시 가능한 사전 생성 질문 풀 (cache_key 별 비활성/활성) | `question` N (`question_pool_id` FK) |
| `question_answer` | 영상 내 답변 구간 (`startMs`–`endMs`) | `question` 1 |
| `question_set` | 질문 세트 = 영상 1개 단위 (녹화 / 분석 / 피드백) | `interview` 1 / `question` 1:N / `file_metadata` 1:1 / `question_set_analysis` 1:1 |

> ⚠️ **코드 / 정책 갭** (Issue #407 추적):
> - `cs_topic_question_pool` / `CsTopicQuestionPoolService` 는 코드 부재. CS 트랙은 단일 `question_pool` 테이블에서 `cache_key` (`{level}:CS_FUNDAMENTAL`) + `category` 필터링으로 처리한다 (`CacheableQuestionProvider`). 별도 테이블 분리 정책은 미정 (#407 비스코프).
> - `interview_answer` 테이블은 V1에 생성됐다가 V3 (`drop_feedback_tables.sql`) 에서 DROP 되었다. **현재 존재하지 않는다.** 답변 = `question_answer` 단일 소스.

---

## question

### 성격
질문 1개 = 1행. MAIN (메인 질문, 사전 생성 또는 AI 생성) / FOLLOWUP (CS 트랙 꼬리질문, 런타임 생성) / RESUME_OPENER / RESUME_PLAYGROUND / RESUME_INTERROGATION (이력서 트랙 FSM 단계별 발화).

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_set_id` | BIGINT | FK → `question_set.id` ON DELETE CASCADE, NOT NULL | 부모 질문 세트 |
| `question_type` | VARCHAR(20) | NOT NULL | `QuestionType` enum (`MAIN` / `FOLLOWUP` / `RESUME_OPENER` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION`) |
| `question_text` | TEXT | NOT NULL, blank 불가 (`Question` 생성자) | 질문 본문 |
| `tts_text` | TEXT | NULL | TTS 친화 변형 (`Question.ttsText`) |
| `model_answer` | TEXT | NULL | 모범답안 |
| `reference_type` | VARCHAR(20) | NULL | `ReferenceType` enum (`MODEL_ANSWER` / `GUIDE`) — Step B 프롬프트 모드 분기 |
| `feedback_perspective` | VARCHAR(20) | NULL | `FeedbackPerspective` enum (TECHNICAL / BEHAVIORAL / EXPERIENCE) — feedback 도메인 사용 |
| `order_index` | INT | NOT NULL | 세트 내 순서. MAIN=0, FOLLOWUP=1.. |
| `question_pool_id` | BIGINT | FK → `question_pool.id` ON DELETE SET NULL, NULL | 풀에서 가져온 경우 출처 |

### 인덱스
- PK `id`
- `idx_question_question_set_id` (`question_set_id`) — V4
- UNIQUE `uq_question_question_set_order` (`question_set_id`, `order_index`) — V4 + V40 동등 (V40은 명시적 재선언 / 현 스키마는 `uq_question_set_order`) — followup 동시 INSERT 시 race 차단
- FK `fk_question_question_set` (`question_set_id`) ON DELETE CASCADE — V41 재생성
- FK `fk_question_pool` (`question_pool_id`) ON DELETE SET NULL — V11

### 불변 / 정책

- **`question_text` blank 금지** (`Question` 생성자 / `resume()` 팩토리 가드).
- **`question_type` null 금지** (생성자 가드).
- **`(question_set_id, order_index)` UNIQUE** — follow-up 동시 호출로 인한 중복 행 차단. 위반 시 `DataIntegrityViolationException` → `INTERVIEW_011 FOLLOWUP_DUPLICATE (409)` 변환 (`FollowUpTransactionHandler`).
- **`Question.resume(...)` 팩토리는 `RESUME_*` 타입만 허용**. `MAIN` / `FOLLOWUP` 전달 시 `IllegalArgumentException`.
- **CHECK 제약 (`chk_question_track_meta_v2`) 은 V42에서 DROP**. V35 / V41 시기에 5-way 정밀 CHECK + `chain_id` / `chain_step_type` / `project_id` 컬럼이 존재했으나 V42에서 일괄 제거됨. 현 코드는 chain 컨텍스트를 `InterviewRuntimeState` (in-memory) + `interview_plan` 테이블로만 관리.
- **`question_pool_id` SET NULL CASCADE** — pool 비활성화·삭제 시 question 의 추적 링크만 끊고 question 자체는 보존.
- **soft-delete 미도입**. interview 삭제 시 `InterviewDeletionService` 가 명시 순서로 자식 → 본체 hard-delete (CASCADE 보완).

### 마이그레이션 히스토리

- `V4__add_question_set_and_file_metadata.sql` — 테이블 생성 (`question_set_id`, `question_type`, `question_text`, `model_answer`, `reference_type`, `order_index`, UNIQUE `(question_set_id, order_index)`)
- `V5__rename_answer_and_update_feedback.sql` — `FOLLOWUP_1/2/3` → `FOLLOWUP` 통합 (`UPDATE question SET question_type = 'FOLLOWUP'`)
- `V11__create_question_pool.sql` — `question_pool_id` 컬럼 + FK ON DELETE SET NULL
- `V23__add_tts_text_columns.sql` — `tts_text` 컬럼 추가
- `V35__add_question_resume_meta.sql` — `chain_id` / `chain_step_type` / `project_id` + CHECK `chk_question_track_meta` (V41에서 v2로 교체 → V42에서 제거)
- `V40__followup_unique_order_index.sql` — `uq_question_set_order` 명시 (V4 `uq_question_question_set_order` 보강)
- `V41__integrity_patches.sql` — `fk_question_question_set` ON DELETE CASCADE 재생성 + CHECK v2 강화
- `V42__drop_question_resume_meta.sql` — chain meta 컬럼 / CHECK 일괄 제거

---

## question_pool

### 성격
캐시 가능한 사전 생성 질문 풀. `cache_key` (`{level}:{type}` 또는 `{position}:{level}:{techStack}:{type}`) 단위로 묶음. 한 row = 풀 후보 질문 1개. `is_active = false` 면 풀 선택 후보에서 제외.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `cache_key` | VARCHAR(255) | NOT NULL | `QuestionCacheKeyGenerator` 산출 (`POSITION_AGNOSTIC_TYPES` = `CS_FUNDAMENTAL` / `BEHAVIORAL` / `SYSTEM_DESIGN` 은 `{level}:{type}`, 외 `{position}:{level}:{techStack}:{type}`) |
| `content` | TEXT | NOT NULL, blank 불가 (`QuestionPool.create`) | 질문 본문 |
| `tts_content` | TEXT | NULL | TTS 친화 변형 (V23) |
| `category` | VARCHAR(100) | NULL | CS 세부 주제 라벨 (`CsSubTopic.categoryName` — "운영체제" / "네트워크" / "데이터베이스" / "자료구조") 또는 외부 `questionCategory` |
| `model_answer` | TEXT | NULL | 모범답안 |
| `reference_type` | VARCHAR(50) | NULL | `MODEL_ANSWER` / `GUIDE` 문자열 |
| `is_active` | BOOLEAN | NOT NULL, default TRUE | 풀 선택 후보 여부. `deactivate()` 로만 false 전이 |
| `created_at` | DATETIME(6) | NOT NULL, updatable=false | `@CreatedDate` |

### 인덱스
- PK `id`
- `idx_qp_cache_key_active` (`cache_key`, `is_active`) — V11. 풀 카운트 / 후보 조회 표준 경로
- `idx_qp_created_at` (`created_at`) — V11. 운영 회수성 검토용

### 불변 / 정책

- **`cache_key`, `content` blank 금지** (`QuestionPool.create` 가드).
- **`is_active` 단방향 전이** (`deactivate()` 만 존재. 재활성화 메서드 없음).
- **`POOL_SOFT_CAP = 200`** (`QuestionPoolService:24`). cache_key 별 활성 행 수 ≥ 200 이면 신규 AI 생성 결과는 풀 저장 생략 (메모리 / DB 비대화 방지). 단 호출자에게는 즉시 사용용으로 반환 (`convertAndCacheIfEligible`).
- **`POOL_SUFFICIENCY_MULTIPLIER = 3`** (`QuestionPoolService:22`). 풀 충분 판정 = `activeCount ≥ requiredCount × 3`.
- **`USER_SUFFICIENCY_MULTIPLIER = 2.0`** (`QuestionPoolService:23`). 사용자가 이미 사용한 풀 ID 가 있을 때 = `availableCount ≥ ceil(requiredCount × 2.0)` (사용자 중복 회피 + 여유).
- **`@Transactional(propagation = REQUIRES_NEW)`** (`convertAndCacheIfEligible`). AI 호출 결과 저장은 호출자 트랜잭션과 분리 (외부 호출 트랜잭션 길이 제한).
- ⚠️ **UNIQUE / dedup 부재** (Issue #407 추적): `cache_key` 당 `content` 중복 INSERT 가능. V41 블록 2 주석에 "별도 V43에서 처리" 라고 적혀 있으나 V43 부재 (`question_resume_meta` 와 별개). `selectWithCategoryDistribution` 가 같은 카테고리 큐에서 셔플 + poll 로 충돌 회피만 함.
- ⚠️ **`category` 화이트리스트 서버 검증 부재** (Issue #407 추적): AI 가 임의 라벨 반환 가능. `KeywordMatcher` / `CsSubTopic.toCategoryName` 매칭 실패 시 silent skip.
- ⚠️ **갱신 정책 미정** (Issue #407 추적): pool 재학습 / 재생성 시점, AI 모델 버전 변경 시 풀 invalidate 정책 등.

### 마이그레이션 히스토리

- `V11__create_question_pool.sql` — 테이블 생성 + `prepared_follow_up` 동시 생성 (현재 미사용 — V12에서 `prepared_follow_up` DROP)
- `V20__cleanup_question_pool_columns.sql` — `evaluation_criteria` / `follow_up_strategy` / `question_order` / `quality_score` 컬럼 일괄 제거 (저장만 되고 미조회)
- `V23__add_tts_text_columns.sql` — `tts_content` 추가

---

## question_answer

### 성격
영상 내 답변 구간 1개 = 1행. `startMs` ~ `endMs` (ms 단위). 한 `question` 당 1행 보장 정책 (멱등 SAVE — 재호출 시 같은 `question_set_id` 의 기존 행 일괄 DELETE 후 INSERT).

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_id` | BIGINT | FK → `question.id` ON DELETE CASCADE, NOT NULL | 답변 대상 질문 |
| `start_ms` | BIGINT | NOT NULL | 영상 내 시작 ms |
| `end_ms` | BIGINT | NOT NULL | 영상 내 종료 ms |

### 인덱스
- PK `id`
- `idx_answer_question_id` (`question_id`) — V4
- FK `fk_answer_question` (`question_id`) ON DELETE CASCADE — V4 + V41 재생성

### 불변 / 정책

- **멱등 INSERT** — `QuestionSetService.saveAnswers` 가 `answerRepository.deleteByQuestionSetId` 후 `saveAll`. 동일 `questionSetId` 재호출 시 기존 답변은 모두 삭제 후 신규 set 으로 교체. FE 의 면접 종료 복구 루프 동시성 방어 (defense-in-depth).
- **빈 요청 가드** — `request.getAnswers()` empty/null → `WARN` 로그 + no-op (기존 데이터 보호). FE 가 비정상 상태로 빈 POST 보낼 경우 데이터 손실 차단.
- **`startMs`, `endMs` 검증 부재** — `startMs ≥ 0`, `endMs > startMs`, video duration 내 검증 등 도메인 룰 부재. ⚠️ **정책-코드 갭** (Issue #407 추적): 입력 검증 없이 raw 저장.
- **CASCADE 삭제** — `interview` 삭제 시 `InterviewDeletionService` 가 `deleteAllByInterviewId` (JPQL `DELETE FROM QuestionAnswer a WHERE a.question.id IN (SELECT q.id FROM Question q WHERE q.questionSet.interview.id = :interviewId)`) 로 명시 선삭제. V41 ON DELETE CASCADE 가 fallback.

### 마이그레이션 히스토리

- `V4__add_question_set_and_file_metadata.sql` — `question_set_answer` 로 최초 생성
- `V5__rename_answer_and_update_feedback.sql` — `question_set_answer → question_answer` rename
- `V41__integrity_patches.sql` — FK ON DELETE CASCADE 재생성

---

## question_set

> `question_set` 은 `domain/question/` 하위 엔티티이며, 질문 / 답변 / 풀 / 세트는 모두 question 도메인 책임이다.

### 성격
영상 1개 단위 = 1행. `interview` 1:N. 녹화 / 분석 / 피드백 파이프라인 단위. category (`QuestionSetCategory`) 가 `RESUME_BASED` 면 인터뷰당 1행 UNIQUE.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `interview_id` | BIGINT | FK → `interview.id` ON DELETE CASCADE, NOT NULL | 부모 면접 |
| `category` | VARCHAR(20) | NOT NULL | `QuestionSetCategory` enum (CS_FUNDAMENTAL / BEHAVIORAL / RESUME_BASED / LANGUAGE_FRAMEWORK / SYSTEM_DESIGN / FULLSTACK_STACK / UI_FRAMEWORK / BROWSER_PERFORMANCE / INFRA_CICD / CLOUD / DATA_PIPELINE / SQL_MODELING) |
| `order_index` | INT | NOT NULL | interview 내 정렬 순서 |
| `file_metadata_id` | BIGINT | FK → `file_metadata.id` (NULL 허용) | 녹화 영상 메타 |
| `analysis_status` 외 | — | — | 별도 1:1 분리 (`question_set_analysis`) — 현 코드 (`QuestionSetAnalysis` entity) 기준 |
| `created_at` / `updated_at` | DATETIME(6) | NOT NULL | 감사 |

### 인덱스
- PK `id`
- UNIQUE `uq_question_set_interview_order` (`interview_id`, `order_index`) — V4
- UNIQUE `uq_resume_per_interview` (functional: `CASE WHEN category='RESUME_BASED' THEN interview_id END`) — V44. RESUME_BASED 카테고리는 인터뷰당 최대 1행 보장
- `idx_question_set_interview_id` (`interview_id`) — V4
- `idx_question_set_status_updated` (`analysis_status`, `updated_at`) — V4 (V14 redesign 후 `question_set_analysis` 로 분리됐으나 컬럼 명목상 잔존 가능 — V14 운영 검증 필요)
- FK `fk_question_set_interview` ON DELETE CASCADE — V41 재생성

### 불변 / 정책

- **RESUME_BASED 인터뷰당 1행 UNIQUE** (V44 functional UNIQUE). 사전생성 경로의 N행 사용은 V44에서 정리 + 코드 제거. 현 코드는 FSM 경로의 단일 row 만 INSERT.
- **CASCADE 삭제** — interview 삭제 시 `InterviewDeletionService` 가 자식 (`question_answer`, `timestamp_feedback`, `question_set_feedback`, `question_set_analysis`) 명시 선삭제 후 `questionSetRepository.deleteAll` (전체 자식 fetch + delete).
- **`assignFileMetadata`, `addQuestion` 도메인 메서드** — 외부에서 setter 직접 접근 차단. `Question.assignQuestionSet` 양방향 동기화 강제.
- **`getQuestions` immutable view** — `Collections.unmodifiableList` 반환. 외부 변경 차단.

### 마이그레이션 히스토리

- `V4__add_question_set_and_file_metadata.sql` — 테이블 생성 + UNIQUE `(interview_id, order_index)`
- `V14__analysis_state_redesign.sql` — analysis 상태 redesign (`question_set_analysis` 분리 가정)
- `V21__convert_question_set_category_to_interview_type.sql` — category enum 보정
- `V41__integrity_patches.sql` — FK ON DELETE CASCADE 재생성
- `V44__resume_questionset_unification.sql` — RESUME_BASED 1행 UNIQUE + 사전생성 경로 잔존 데이터 cleanup

---

## 연관 의존성

`question` 도메인 테이블 / 코드가 직접 호출 / 참조하는 외부 패키지·클래스. `import` / 호출 그래프 근거.

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.question.entity.QuestionSet` | 질문 세트 엔티티 (1:N 부모) | calls — `Question.questionSet`, `QuestionSetAssembler.assemble` (질문 세트 생성) |
| `com.rehearse.api.domain.question.entity.QuestionSetCategory` | 세트 카테고리 enum | calls — `QuestionSetAssembler.fromPool/fromGenerated` |
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | 세트 저장 / 조회 | calls — `QuestionGenerationTransactionHandler.saveResults` |
| `com.rehearse.api.domain.feedback.entity.FeedbackPerspective` | 질문 관점 (TECHNICAL/BEHAVIORAL/EXPERIENCE) | calls — `Question.feedbackPerspective`, `QuestionSetAssembler.perspectiveOf` |
| `com.rehearse.api.domain.interview.entity.Interview` | 면접 엔티티 | calls — `QuestionGenerationTransactionHandler` (`startQuestionGeneration` / `completeQuestionGeneration` / `failQuestionGeneration`) |
| `com.rehearse.api.domain.interview.entity.InterviewType` / `InterviewLevel` / `Position` / `TechStack` | 면접 메타 enum | calls — `QuestionGenerationService.generateQuestions` 시그니처, `QuestionCacheKeyGenerator`, `QuestionDistribution` |
| `com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent` | 질문 생성 트리거 이벤트 | event-listener — `QuestionGenerationEventHandler` (interview 도메인이 발행, 질문 생성은 그 핸들러가 `QuestionGenerationService` 호출) |
| `com.rehearse.api.domain.interview.repository.InterviewRepository` | 면접 조회 (생성 상태 전이) | calls — `QuestionGenerationTransactionHandler` |
| `com.rehearse.api.domain.interview.entity.InterviewRuntimeState` / `service.InterviewRuntimeStateCache` | runtime state in-memory | calls — `ResumeTrackInitiator` (`getOrInit` + seed) |
| `com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator` / `ResumePlanPreparationService` / `PreparedResume` | 이력서 트랙 진입 | calls — `ResumeTrackInitiator.initiate` |
| `com.rehearse.api.domain.file.entity.FileMetadata` / `repository.FileMetadataRepository` | 영상 파일 메타 | calls — `QuestionSetService.generateUploadUrl` |
| `com.rehearse.api.domain.feedback.repository.{QuestionSetFeedbackRepository, TimestampFeedbackRepository}` | 피드백 정리 | called-by — `InterviewDeletionService` (CASCADE 보완) |
| `com.rehearse.api.domain.feedback.score.repository.{QuestionScoreRepository, QuestionScoreDimensionRepository}` | 루브릭 점수 | calls — `QuestionSetService.getFeedback` |
| `com.rehearse.api.infra.ai.AiClient` | LLM 진입점 (`ResilientAiClient`) | calls — `CacheableQuestionProvider.generateWithStampedeProtection`, `FreshQuestionProvider.provide` |
| `com.rehearse.api.infra.ai.dto.{GeneratedQuestion, QuestionGenerationRequest}` | AI 호출 DTO | calls |
| `com.rehearse.api.infra.ai.prompt.QuestionCountCalculator` | 질문 개수 계산 | calls — `StandardTrackQuestionGenerator.generate` |
| `com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder` | 프롬프트 빌더 (infra) | called-by — `OpenAiClient` / `ClaudeApiClient` 내부 (도메인 직접 호출 X) |
| `com.rehearse.api.infra.aws.{S3KeyGenerator, S3Service}` | S3 presigned URL / 키 | calls — `QuestionSetService.generateUploadUrl` / `getFeedback` |
| `com.rehearse.api.global.config.AsyncConfig` (`vtExecutor`) | 가상 스레드 풀 | calls — `StandardTrackQuestionGenerator` 내부 `Executors.newVirtualThreadPerTaskExecutor()` (별도 풀, AsyncConfig 사용 X — 직접 생성 / `@PreDestroy` close) |

---

## 정책 출처

- 비즈니스 룰 본문 = 본 schema.md 각 테이블 "불변 / 정책" 섹션
- 임계값 / 디폴트 (직접 인용):
  - `QuestionPoolService.POOL_SUFFICIENCY_MULTIPLIER = 3` — 풀 충분 판정 배수
  - `QuestionPoolService.USER_SUFFICIENCY_MULTIPLIER = 2.0` — 사용자 미사용 풀 충분 판정 배수
  - `QuestionPoolService.POOL_SOFT_CAP = 200` — cache_key 별 활성 풀 soft cap
  - `StandardTrackQuestionGenerator.PARALLEL_TIMEOUT_SEC = 60` — cacheable / fresh 병렬 future 타임아웃
  - `QuestionCountCalculator.MINUTES_PER_QUESTION = 3`, `MIN = 2`, `MAX = 24`, `SINGLE_TYPE = 5`, `DOUBLE_TYPE = 6`, `MULTI_TYPE = 8` — 질문 개수 산출
  - `QuestionCacheKeyGenerator.POSITION_AGNOSTIC_TYPES = {CS_FUNDAMENTAL, BEHAVIORAL, SYSTEM_DESIGN}` — 포지션·스택 무관 캐시 키 분기
  - `ResumeTrackInitiator.DEFAULT_DURATION_MIN = 30` — 이력서 트랙 듀레이션 디폴트
- ❓TODO(사용자 확인) — Issue #407 비스코프 (정책 미결):
  1. `question_pool` 의 `(cache_key, content)` UNIQUE / dedup 정책 — V41 주석에 V43 분리 예정으로 적혀있으나 미구현
  2. `category` 화이트리스트 서버 검증 (AI 가 임의 라벨 반환 시 처리)
  3. pool 재학습 / 재생성 / 모델 버전 변경 시 invalidate 정책
  4. `question_answer.startMs/endMs` 도메인 검증 (음수 / 역전 / video duration 초과)
  5. `cs_topic_question_pool` 별도 테이블 분리 여부 (현재는 단일 `question_pool` + category 필터)
- ⚠️ **정책-코드 갭** (Issue #407 추적):
  - `question_pool` UNIQUE 부재 (위 ❓TODO 1)
  - `category` 화이트리스트 서버 검증 부재 (위 ❓TODO 2)
  - `question_answer` 입력 검증 부재 (위 ❓TODO 4)
