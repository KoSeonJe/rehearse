# API: 질문 생성 (interview 생성 / 재시도 비동기 후속)

> Endpoint: **별도 HTTP 엔드포인트 없음.** `interview` 생성 (`POST /api/v1/interviews`) / 질문 생성 재시도 (`POST /api/v1/interviews/{id}/retry-question-generation`) 흐름이 발행하는 `QuestionGenerationRequestedEvent` 의 비동기 핸들러로 진입.
> Action: 면접 메타 (직무 / 레벨 / 면접 유형 다중 선택 / CS 세부 주제 / 이력서 텍스트 / 듀레이션) 로부터 실제 질문 세트 N개를 생성한다. 캐시 가능 유형은 풀에서, 외 (이력서 / 신규) 는 AI 직접 호출.
> 관련 테이블: `interview` (read/write — `question_generation_status` 전이) / `question_set` (write) / `question` (write) / `question_pool` (read/write — 캐시)
> 관련 외부 의존: OpenAI GPT-4o-mini primary + Claude fallback (`ResilientAiClient` 경유 `AiClient.generateQuestions`)

> ⚠️ 사용자 인스트럭션의 `QuestionGenerationOrchestrator` 클래스는 실코드 부재. 현 코드의 orchestrator 역할 = `QuestionGenerationService` (`domain/question/service/`). `QuestionGenerationPromptBuilder` 는 `infra/ai/prompt/` 에 존재 — 도메인은 직접 호출 X, `OpenAiClient` / `ClaudeApiClient` 가 사용.

---

## 트리거 (입력)

`QuestionGenerationRequestedEvent` 발행 시점:
- `InterviewCreationService.createInterview` (interview 생성 트랜잭션 commit 후)
- `InterviewService.retryQuestionGeneration` (재시도 명시 호출 시)

이벤트 페이로드:

| 필드 | 타입 | 의미 |
|------|------|------|
| `interviewId` | Long | 대상 면접 |
| `userId` | Long | 소유자 |
| `position` | `Position` | 직무 |
| `positionDetail` | String | 직무 부가 설명 (현 핸들러 시그니처에 미사용) |
| `level` | `InterviewLevel` | 레벨 |
| `interviewTypes` | `List<InterviewType>` | 면접 유형 다중 |
| `csSubTopics` | `List<String>` | CS 세부 주제 (`CsSubTopic` enum 이름 문자열 — "OS" / "NETWORK" / "DATABASE" / "DATA_STRUCTURE") |
| `resumeText` | String | 이력서 PDF 추출 텍스트 (RESUME_BASED 시) |
| `resumeFileHash` | String | 이력서 파일 해시 (캐시 키 — 이력서 트랙) |
| `durationMinutes` | Integer | 면접 듀레이션 (질문 개수 산출 입력) |
| `techStack` | `TechStack` | 기술 스택 (NULL 시 직무 디폴트) |

리스너: `QuestionGenerationEventHandler.handleQuestionGenerationEvent` — `@Async(VT_EXECUTOR) @TransactionalEventListener(phase = AFTER_COMMIT)`.

---

## 출력

직접 응답 없음 (비동기). 결과:

- 성공 시: `interview.question_generation_status = COMPLETED`, `question_set` / `question` row 일괄 INSERT
- 실패 시: `interview.question_generation_status = FAILED`, `interview.failure_reason = e.getCause().getMessage() ?: e.getMessage()`

상태 전이는 `QuestionGenerationTransactionHandler` 가 트랜잭션 격리.

폴링 / 조회: FE 는 `GET /api/v1/interviews/{id}` 등 인터뷰 조회 API 의 `questionGenerationStatus` 필드로 진행 상태 확인.

---

## 흐름

### 1. 비동기 진입 (`QuestionGenerationEventHandler`)
- AFTER_COMMIT 단계에서 `vtExecutor` (가상 스레드) 로 분리 실행
- 예외 시 `transactionHandler.failGeneration(interviewId, reason)` 호출 → status=FAILED + 사유 영속 + ERROR 로그
- 정상 진행 시 `QuestionGenerationService.generateQuestions(...)` 호출

### 2. 상태 GENERATING 전이 (`QuestionGenerationTransactionHandler.startGeneration`)
- 별도 `@Transactional` (write) — `Interview.startQuestionGeneration()` 호출 → `question_generation_status = GENERATING`
- INFO 로그: `질문 생성 시작: interviewId={}`

### 3. 분기: 트랙

#### 3-A. RESUME_BASED 포함 (`interviewTypes.contains(RESUME_BASED)`)
1. `ResumeTrackInitiator.initiate(interviewId, level, resumeFileHash, resumeText, durationMinutes)` 호출
2. `ResumePlanPreparationService.prepare(...)` — 이력서 PDF → skeleton + interview_plan 생성 (resume 도메인 위임)
3. `InterviewRuntimeStateCache.getOrInit(interviewId, () -> InterviewRuntimeState.seed(level, skeleton, plan))` — Caffeine `computeIfAbsent` (동시 init 1회 보장)
4. `ResumeInterviewOrchestrator.startSession(interviewId, durationMinutes ?: DEFAULT_DURATION_MIN(=30), skeleton, plan)` — 이력서 트랙 FSM 시작 (질문 row 1개 생성: RESUME_OPENER 또는 등가)
5. `transactionHandler.saveResults(interviewId, List.of())` — 빈 questionSets 로 호출 → `Interview.completeQuestionGeneration()` 만 수행 (질문은 FSM 이 별도 INSERT)

> RESUME 트랙은 사전 N개 생성하지 않는다. FSM 이 PLAYGROUND / INTERROGATION 단계별 question 을 런타임에 생성. V44 UNIQUE `(category=RESUME_BASED, interview_id)` 가 question_set 1행 강제.

#### 3-B. 표준 트랙 (`StandardTrackQuestionGenerator.generate`)

1. **질문 개수 계산** — `QuestionCountCalculator.calculate(durationMinutes, typeCount)`
   - `durationMinutes` 있으면: `round(duration / 3)` clamp `[2, 24]`
   - 없으면: `typeCount=1 → 5` / `typeCount=2 → 6` / `else → 8`
2. **분배** — `QuestionDistribution.create(types, totalCount)` — 타입별 균등 분배 (나머지는 앞쪽 타입에 +1)
3. **`effectiveTechStack`** — `techStack ?? TechStack.getDefaultForPosition(position)`
4. **병렬 실행** — 두 `CompletableFuture` 가 `virtualExecutor` (별도 가상 스레드 풀, `@PreDestroy` 정리) 에서 60초 타임아웃:
   - **Cacheable future** = `provideCacheable(...)` — `InterviewType.isCacheable() == true` 타입 (CS_FUNDAMENTAL / BEHAVIORAL / SYSTEM_DESIGN 등)
   - **Fresh future** = `provideFresh(...)` — `cacheable == false` 타입 (예: 이력서 추출 답변 / 일부 도메인 특화)
5. 두 future `.join()` → `CompletionException` 시 양쪽 cancel + RuntimeException 래핑
6. `QuestionSet.updateOrderIndex(i)` 일괄 재배정 (병렬 결과 합친 후 순서 재계산)

##### 3-B.1. provideCacheable (`CacheableQuestionProvider.provide`)

타입별 반복:
1. `cacheKey = QuestionCacheKeyGenerator.generate(position, level, techStack, type)`
   - `POSITION_AGNOSTIC_TYPES` (CS_FUNDAMENTAL / BEHAVIORAL / SYSTEM_DESIGN) → `{level}:{type}`
   - 외 → `{position}:{level}:{techStack}:{type}`
2. **categoryFilter** — `type == CS_FUNDAMENTAL` 일 때 `csSubTopics` → `CsSubTopic.toCategoryName` 매핑 ("OS" → "운영체제" 등). null/empty 면 `CsSubTopic.allCategoryNames()` (전 카테고리)
3. **사용자 사용 풀 ID 조회** — `questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(userId, cacheKey)` (사용자가 이전 면접에서 본 풀 제외)
4. **풀 충분 판정** — `QuestionPoolService.isPoolSufficient(criteria)`:
   - `usedPoolIds` 있으면: 후보 fetch → 미사용 필터 → `availableCount ≥ ceil(required × USER_SUFFICIENCY_MULTIPLIER(=2.0))`
   - `categoryFilter` 있으면: `countByCacheKeyAndIsActiveTrueAndCategoryIn ≥ required × POOL_SUFFICIENCY_MULTIPLIER(=3)`
   - else: `countByCacheKeyAndIsActiveTrue ≥ required × 3`
5. 충분 → `selectFromPool(criteria)`:
   - 후보 fetch → usedPoolIds 필터 → `selectWithCategoryDistribution(candidates, requiredCount)` (카테고리별 round-robin + shuffle)
6. 부족 → **stampede protection** (`generateWithStampedeProtection`):
   - `QuestionGenerationLock.acquire(cacheKey)` — 같은 cacheKey 동시 호출 직렬화 (`ConcurrentHashMap<String, ReentrantLock>`)
   - lock 후 `usedPoolIds` 재조회 → `isPoolSufficient` 재판정 (lock 대기 중 다른 호출이 풀 채웠을 가능성)
   - 여전히 부족 → `aiClient.generateQuestions(QuestionGenerationRequest)` 호출 (단일 type set)
   - `QuestionPoolService.convertAndCacheIfEligible(cacheKey, generated)` (`@Transactional(REQUIRES_NEW)`):
     - `GeneratedQuestion → QuestionPool.create` 매핑
     - `shouldSaveToPool(cacheKey)` (활성 풀 < `POOL_SOFT_CAP(=200)`) → `saveAll`. 초과 → DB 저장 생략 + INFO 로그 ("soft cap 도달")
     - 호출자에게는 어쨌든 `pools` 반환 (즉시 사용용)
   - categoryFilter 있으면 generated 후 필터링 → `selectWithCategoryDistribution` 로 최종 N개
   - finally `lock.release()` (`ReentrantLock.unlock`)

각 풀 row → `QuestionSetAssembler.fromPool(type, pool)` 로 `QuestionSet` (1개 question 보유) 변환.

##### 3-B.2. provideFresh (`FreshQuestionProvider.provide`)

- 비어있으면 즉시 `List.of()` 반환
- 단일 `QuestionGenerationRequest` (다중 fresh type 한 번에 묶음) → `aiClient.generateQuestions`
- 결과 > requiredCount 면 `subList(0, requiredCount)`
- 각 `GeneratedQuestion` → `QuestionSetAssembler.fromGenerated(gq)` 로 `QuestionSet` 변환 (questionPool=null)

### 4. 저장 (`QuestionGenerationTransactionHandler.saveResults`)

- 별도 `@Transactional` (write)
- `Interview` 재조회 (read) → `questionSets.forEach(qs -> qs.assignInterview(interview))`
- `questionSetRepository.saveAll(questionSets)` — `QuestionSet` cascade ALL 로 자식 `Question` 동시 저장
- `Interview.completeQuestionGeneration()` → `question_generation_status = COMPLETED`
- INFO 로그: `질문 생성 완료: interviewId={}, questionSets={}`

### 5. 실패 처리

- `QuestionGenerationEventHandler` catch → `transactionHandler.failGeneration(interviewId, reason)`:
  - 별도 `@Transactional` (write) → `Interview.failQuestionGeneration(reason)` → `question_generation_status = FAILED` + `failure_reason` 영속
  - ERROR 로그: `질문 생성 비동기 작업 실패: interviewId={}` + `질문 생성 실패: interviewId={}, reason={}`

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 동시 호출 (같은 cacheKey, 풀 부족) | `QuestionGenerationLock` 직렬화 — 첫 호출이 풀 채우면 후속은 락 후 재판정 → 풀 히트 |
| 풀 활성 ≥ POOL_SOFT_CAP(200) + AI 호출 발생 | DB 저장 생략, 호출자에 즉시 반환 (pool 비대화 방지) |
| 사용자 이전에 사용한 풀 ID 가 많아 미사용 풀 부족 | AI 호출 진행 (`isPoolSufficient(criteria)` false → generate) |
| 60초 타임아웃 (cacheable / fresh future) | `orTimeout` → `CompletionException` → 양쪽 cancel + RuntimeException → 핸들러가 catch → status=FAILED |
| AI 호출 실패 (OpenAI / Claude 둘 다 실패) | `ResilientAiClient` 가 예외 throw → 핸들러 catch → status=FAILED |
| RESUME_BASED + 이력서 미준비 | `ResumePlanPreparationService.prepare` 가 예외 throw → 핸들러 catch → status=FAILED |
| 동일 인터뷰 재시도 호출 | `InterviewService.retryQuestionGeneration` 이 `failure_reason` 클리어 + 이벤트 재발행. `startGeneration` 이 멱등 (status 전이만) |
| categoryFilter 가 매칭 풀 0개 | `selectWithCategoryDistribution` 가 빈 후보 리턴 → 결국 `selectWithCategoryDistribution(allGenerated, requiredCount)` fallback |
| `csSubTopics` null/empty + CS_FUNDAMENTAL | `CsSubTopic.allCategoryNames()` (전 카테고리) 사용 |
| `techStack` null | `TechStack.getDefaultForPosition(position)` 적용 |
| RESUME_BASED + 다른 InterviewType 동시 | 호출 전 `InterviewService.validateResumeExclusivity` 가 차단 (interview 생성 단계에서 RESUME_EXCLUSIVITY_VIOLATION) |

---

## 상태 전이

```
question_generation_status:
PENDING → GENERATING (startGeneration)
GENERATING → COMPLETED (saveResults — 정상 종료)
GENERATING → FAILED   (failGeneration — 예외 catch)
FAILED → GENERATING   (retry — InterviewService.retryQuestionGeneration)
```

`Interview.startQuestionGeneration` / `completeQuestionGeneration` / `failQuestionGeneration` 가 도메인 메서드로 전이 강제 (외부 setter 차단).

---

## 관찰성

- **로그**:
  - `QuestionGenerationTransactionHandler` — `질문 생성 시작: interviewId={}` / `질문 생성 완료: interviewId={}, questionSets={}` / `질문 생성 실패: interviewId={}, reason={}` (INFO/ERROR)
  - `QuestionGenerationEventHandler` — `질문 생성 비동기 작업 실패: interviewId={}` (ERROR + throwable)
  - `StandardTrackQuestionGenerator` — `[CACHEABLE] 질문 제공 완료: interviewId={}, count={}` / `[FRESH] 질문 제공 완료: interviewId={}, count={}` (INFO)
  - `CacheableQuestionProvider` — `[CACHE] pool 히트: cacheKey={}, required={}, usedByUser={}` / `[CACHE] pool 부족, AI 호출: cacheKey={}, usedByUser={}` / `[CACHE] lock 후 pool 히트: cacheKey={}` / `[CACHE] AI 호출 실패: cacheKey={}` (INFO/ERROR)
  - `FreshQuestionProvider` — `[FRESH] AI 호출 완료: types={}, generated={}` (INFO)
  - `QuestionPoolService` — `[POOL] 저장 완료: cacheKey={}, count={}` / `[POOL] soft cap 도달, DB 저장 생략: cacheKey={}, count={}` (INFO)
- **메트릭**: 도메인 자체 메트릭 미정 — `infra/ai/metrics` 의 AiCallMetrics 가 LLM 호출 단위 latency / token / fallback 측정 (간접)
- **알람**: 정책 미정 (❓TODO Issue #407 비스코프). `question_generation_status = FAILED` 비율 임계 등 미정.

---

## 동시성 / 트랜잭션 노트

- 핸들러 진입: 가상 스레드 풀 (`AsyncConfig.VT_EXECUTOR`) — 트랜잭션 미시작 (외부 AI 호출 중 DB 락 회피)
- `startGeneration` / `saveResults` / `failGeneration` = 별도 짧은 write 트랜잭션 (`QuestionGenerationTransactionHandler` 클래스 단위 `@Transactional`)
- `convertAndCacheIfEligible` = `@Transactional(propagation = REQUIRES_NEW)` — AI 호출 결과 풀 저장은 호출자 트랜잭션 분리
- 표준 트랙 병렬 실행: `StandardTrackQuestionGenerator.virtualExecutor` (`Executors.newVirtualThreadPerTaskExecutor()`) — 별도 풀, `@PreDestroy` 시 close
- 풀 stampede protection: `QuestionGenerationLock` (`ConcurrentHashMap<String, ReentrantLock>`) 같은 cacheKey 직렬화. cacheKey 종류 유한 (수십 개) → 락 객체 누수 미고려
- `InterviewRuntimeStateCache.getOrInit` — Caffeine `computeIfAbsent` atomic
- ⚠️ **잠재 이슈** (Issue #407 추적):
  - `QuestionGenerationLock` 은 단일 JVM 인메모리 락. 멀티 인스턴스 환경에서는 stampede 보호 무효 — 동일 cacheKey 가 인스턴스별 동시 AI 호출 가능
  - `virtualExecutor` 가 `AsyncConfig.vtExecutor` 와 별도 — 풀 가시성 / 모니터링 일관성 부재
  - 60초 타임아웃 시 cancel 호출하지만 AI 호출은 인터럽트 미보장 (외부 SDK 동작 의존)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent` | 트리거 이벤트 | event-listener — `QuestionGenerationEventHandler` (interview 도메인 발행) |
| `com.rehearse.api.domain.interview.event.QuestionGenerationEventHandler` | 비동기 진입점 | calls — `QuestionGenerationService.generateQuestions` |
| `com.rehearse.api.domain.question.service.QuestionGenerationService` | orchestrator (트랙 분기) | calls — `QuestionGenerationTransactionHandler.startGeneration` / `ResumeTrackInitiator` / `StandardTrackQuestionGenerator` / `transactionHandler.saveResults` |
| `com.rehearse.api.domain.question.service.QuestionGenerationTransactionHandler` | 트랜잭션 경계 (start / save / fail) | calls — `InterviewRepository`, `QuestionSetRepository` |
| `com.rehearse.api.domain.question.service.StandardTrackQuestionGenerator` | 표준 트랙 병렬 생성 | calls — `CacheableQuestionProvider`, `FreshQuestionProvider`, `QuestionSetAssembler` |
| `com.rehearse.api.domain.question.service.CacheableQuestionProvider` | 풀 조회 + 부족 시 AI 생성 + stampede protection | calls — `QuestionPoolService`, `QuestionRepository`, `AiClient`, `QuestionGenerationLock` |
| `com.rehearse.api.domain.question.service.FreshQuestionProvider` | 캐시 불가 타입 AI 직접 호출 | calls — `AiClient` |
| `com.rehearse.api.domain.question.service.QuestionPoolService` | 풀 충분 판정 / 선택 / 저장 | calls — `QuestionPoolRepository` |
| `com.rehearse.api.domain.question.service.QuestionGenerationLock` | cacheKey 단위 stampede lock (단일 JVM) | calls |
| `com.rehearse.api.domain.question.service.QuestionCacheKeyGenerator` | 캐시 키 산출 (position-agnostic 분기) | calls |
| `com.rehearse.api.domain.question.service.QuestionSetAssembler` | `QuestionPool` / `GeneratedQuestion` → `QuestionSet` 매핑 | calls |
| `com.rehearse.api.domain.question.service.ResumeTrackInitiator` | 이력서 트랙 진입 (preparation + FSM 시작) | calls — `ResumePlanPreparationService`, `InterviewRuntimeStateCache`, `ResumeInterviewOrchestrator` |
| `com.rehearse.api.domain.question.entity.QuestionDistribution` | 타입별 개수 분배 VO | calls — `StandardTrackQuestionGenerator` |
| `com.rehearse.api.domain.question.entity.{Question, QuestionPool, QuestionType, ReferenceType, CsSubTopic}` | 도메인 엔티티 / enum | calls |
| `com.rehearse.api.infra.ai.AiClient` | LLM 진입점 (`ResilientAiClient`) | calls — primary GPT-4o-mini / fallback Claude |
| `com.rehearse.api.infra.ai.dto.{GeneratedQuestion, QuestionGenerationRequest}` | AI 호출 DTO | calls |
| `com.rehearse.api.infra.ai.prompt.QuestionCountCalculator` | 듀레이션 → 질문 개수 | calls |
| `com.rehearse.api.global.config.AsyncConfig` (`VT_EXECUTOR`) | 가상 스레드 (핸들러 진입) | calls — `@Async(VT_EXECUTOR)` |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/question/schema.md` (각 테이블 "불변 / 정책") + `docs/domain/interview/schema.md` `interview` `question_generation_status` invariant
- 임계값 (직접 인용):
  - `QuestionPoolService.POOL_SUFFICIENCY_MULTIPLIER = 3` / `USER_SUFFICIENCY_MULTIPLIER = 2.0` / `POOL_SOFT_CAP = 200`
  - `StandardTrackQuestionGenerator.PARALLEL_TIMEOUT_SEC = 60`
  - `QuestionCountCalculator` (`MINUTES_PER_QUESTION=3`, `MIN=2`, `MAX=24`, `SINGLE_TYPE=5`, `DOUBLE_TYPE=6`, `MULTI_TYPE=8`)
  - `QuestionCacheKeyGenerator.POSITION_AGNOSTIC_TYPES = {CS_FUNDAMENTAL, BEHAVIORAL, SYSTEM_DESIGN}`
  - `ResumeTrackInitiator.DEFAULT_DURATION_MIN = 30`
- ❓TODO(사용자 확인) — Issue #407 비스코프 (정책 미결):
  - `QuestionGenerationLock` 멀티 인스턴스 대응 (분산 락 / Redis 등 도입 여부)
  - 60초 타임아웃 + AI cancel 보장 (인터럽트 전파 정책)
  - `question_generation_status = FAILED` 알람 임계
  - `virtualExecutor` 통합 (별도 풀 vs `AsyncConfig.vtExecutor`)
