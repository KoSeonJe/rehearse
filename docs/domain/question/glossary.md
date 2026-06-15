# question 도메인 용어집

> 패키지: `com.rehearse.api.domain.question` (질문 본체 / 풀 / 답변 / 세트 / 분석 / 업로드)

## 핵심 용어

### Question (질문)

`question` 테이블 1행. `Question` 엔티티. 질문 1개 = 1행. 면접 중 클라이언트에 노출되는 발화 단위.

`QuestionType`:
- `MAIN` — 메인 질문. 사전 생성 (풀 / AI 생성).
- `FOLLOWUP` — CS 트랙 꼬리질문. 런타임 생성. `(question_set_id, order_index)` UNIQUE 로 동시 INSERT race 차단.
- `RESUME_OPENER` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` — 이력서 트랙 FSM 단계별 발화. `Question.resume(...)` 팩토리 전용.

### QuestionSet (질문 세트)

`question_set` 테이블 1행. `QuestionSet` 엔티티 (`domain/question/entity/`). 영상 1개 단위. 질문 N개 + 답변 N개 + 분석 1개 + 피드백 1개 + 영상 파일 1개 (`file_metadata` 1:1) 묶음.

`QuestionSetCategory`:
- `STANDARD` — 일반 면접 (단일 세트).
- 외 추가 카테고리는 `QuestionSetCategory` enum 참조.

`question_set.order_index` — `interview` 내 세트 순서 (현재 단일 세트 사용).

### QuestionPool (질문 풀)

`question_pool` 테이블 1행. `QuestionPool` 엔티티. 캐시 가능한 사전 생성 질문 풀. `cache_key` (`{level}:CS_FUNDAMENTAL` 또는 `{position}:{level}:{techStack}:{type}`) 단위로 묶음.

- `is_active` — 풀 활성/비활성. false 면 선택 후보 제외.
- `category` — CS 트랙 세부 분류 (`OS` / `NETWORK` / `DATABASE` / `DATA_STRUCTURE`).
- 풀 → 질문 인용은 `question.question_pool_id` FK ON DELETE SET NULL (풀 삭제 시 question 보존).

> ⚠️ `cs_topic_question_pool` 테이블 / `CsTopicQuestionPoolService` 는 코드 부재. CS 트랙은 단일 `question_pool` 의 `cache_key` + `category` 필터링으로 처리 (`CacheableQuestionProvider`).

### QuestionAnswer (답변)

`question_answer` 테이블 1행. `QuestionAnswer` 엔티티. 영상 내 답변 구간 (`startMs` ~ `endMs`). 한 question 에 0..N 개 (멱등 갱신 — saveAnswers 재호출 시 DELETE-INSERT).

> ⚠️ 과거 V1 에 `interview_answer` 테이블이 존재했으나 V3 (`drop_feedback_tables.sql`) 에서 DROP. 현재 답변의 단일 소스는 `question_answer` 단일 테이블.

### QuestionSetAnalysis (분석)

`question_set_analysis` 테이블 1행. `QuestionSetAnalysis` 엔티티 (`domain/question/entity/`). 영상 분석 단계 추적.

`AnalysisStatus`:
- `PENDING_UPLOAD` — saveAnswers 후 진입. S3 업로드 대기.
- `UPLOADED` — S3 영상 업로드 완료. Lambda 분석 대기.
- `IN_PROGRESS` — Lambda 분석 진행 중.
- `COMPLETED` — Lambda 분석 완료. 피드백 생성 가능.
- `FAILED` — 분석 실패. retry 가능.
- `SKIPPED` — 미응답 세트 (skipRemaining 호출 시).

### ReferenceType

`question.reference_type` 컬럼. `ReferenceType` enum.
- `MODEL_ANSWER` — 모범답안 직접 비교 모드. Step B 프롬프트 분기.
- `GUIDE` — 가이드 / 평가 기준 모드.

### FeedbackPerspective

`question.feedback_perspective` 컬럼. `FeedbackPerspective` enum (실 정의는 feedback 도메인). question 도메인은 컬럼 보유만 — 실 사용은 feedback 도메인 채점/코칭 시.
- `TECHNICAL` / `BEHAVIORAL` / `EXPERIENCE`.

### CsSubTopic

CS 트랙 세부 주제. `CsSubTopic` enum (`domain/question/entity/`). `interview_cs_sub_topics` ElementCollection 으로 interview 와 연동. 현재 enum 값: `OS` / `NETWORK` / `DATABASE` / `DATA_STRUCTURE`.

> ⚠️ `interview_cs_sub_topics.cs_sub_topic` 컬럼은 `VARCHAR(50)` Set<String> — enum 검증 부재. AI prompt 직접 삽입 시 enum / whitelist 부재 (Issue #404 보안 항목).

### QuestionDistribution

`QuestionDistribution` 클래스 (`domain/question/entity/`). 질문 N개 중 카테고리별 분배 비율 정의 (예: technical 60% / behavioral 40%). 풀 / AI 생성 시 분배 결정에 사용.

---

## 핵심 서비스 (`domain/question/service/`)

| 클래스 | 역할 |
|--------|------|
| `QuestionGenerationService` | 질문 생성 orchestrator (AI 호출 + 풀 사용 결정) |
| `StandardTrackQuestionGenerator` | 일반 트랙 질문 생성 |
| `ResumeTrackInitiator` | 이력서 트랙 초기화 (RESUME_OPENER 발화 생성) |
| `QuestionPoolService` | 풀 read/write + 활성화 토글 |
| `CacheableQuestionProvider` | 풀 캐시 후보 우선 사용 (`cache_key` 매칭) |
| `FreshQuestionProvider` | AI 직접 호출 (캐시 미스 / 이력서 트랙) |
| `QuestionCacheKeyGenerator` | `cache_key` 문자열 생성 규칙 |
| `PoolSelectionCriteria` | 풀 선택 기준 (level / position / techStack 매칭) |
| `KeywordMatcher` | techStack 키워드 매칭 헬퍼 |
| `QuestionSetAssembler` | 풀 / AI 결과 → `question` row 조립 |
| `QuestionGenerationLock` | 동시 생성 락 (`@Version` + race 방지) |
| `QuestionGenerationTransactionHandler` | 생성 트랜잭션 + UNIQUE 위반 핸들링 (`FOLLOWUP_DUPLICATE`) |

---

## 핵심 서비스 (`domain/question/service/`)

| 클래스 | 역할 |
|--------|------|
| `QuestionSetService` | 세트 CRUD + saveAnswers + uploadUrl + getStatus / getFeedback 조회 |
| `InternalQuestionSetService` | Lambda 콜백용 internal 엔드포인트 (분석 상태 갱신 / retry) |
| `AnalysisScheduler` | 분석 상태 폴링 / scheduled job |

---

## 흐름 요약

```
1. 면접 생성 (POST /api/v1/interviews)
   → InterviewCreationService.createInterview
   → @TransactionalEventListener AFTER_COMMIT → QuestionGenerationRequestedEvent
   → QuestionGenerationService.handle (async)
   → 풀 캐시 후보 우선 → 부족분 AI 직접 호출
   → question_set 1 + question N persist

2. 클라이언트 면접 진행 (CS 트랙 follow-up 시 런타임 추가 INSERT)

3. 면접 종료 / 답변 매핑
   → POST /api/v1/interviews/{id}/question-sets/{id}/answers
   → QuestionSetService.saveAnswers
   → question_answer DELETE-INSERT (멱등) + analysis_status = PENDING_UPLOAD

4. 영상 업로드
   → POST /api/v1/interviews/{id}/question-sets/{id}/upload-url
   → S3 presigned URL 발급 → 클라이언트 직접 업로드
   → S3 이벤트 → EventBridge → Lambda (analysis / convert)

5. 분석 / 피드백 생성
   → Lambda → InternalQuestionSetService callback
   → analysis_status 전이 → feedback 도메인 생성

6. 면접 삭제
   → DELETE /api/v1/interviews/{id}
   → InterviewDeletionService.deleteInterview (cross-domain cascade)
```

---

## 발견 이슈 추적

본 도메인 정책-코드 갭은 다음 GitHub Issue 로 추적:

- **Issue #407** — question 도메인 발견 이슈 통합 (안정성 / 보안 / cleanup 12건 Epic)

각 API 문서 본문의 ⚠️ 마킹 항목이 위 Issue 로 분류된다.
