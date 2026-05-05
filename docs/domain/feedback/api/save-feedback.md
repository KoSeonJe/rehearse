# API: Lambda 분석 결과 저장

> Endpoint: `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback`
> Action: Lambda (analysis) 가 한 QuestionSet 분석 결과 (verbal + nonverbal) 를 저장 / 진척 갱신
> 관련 테이블: `question_set_feedback` (write), `timestamp_feedback` (write), `question_score` + `question_score_dimension` (nonverbal write), `question_set_analysis` (write)
> 관련 외부 의존: 없음 (Lambda → BE 콜백 endpoint)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `interviewId` | Long | required | 대상 인터뷰 |
| path | `questionSetId` | Long | required | 대상 세트 |
| body | `questionSetComment` | string | optional | 세트 단위 코멘트 |
| body | `timestampFeedbacks[]` | array | required | 턴별 피드백 |
| body | `timestampFeedbacks[].questionId` | Long | nullable | 매핑 질문 (resolve 실패 시 null 저장) |
| body | `timestampFeedbacks[].startMs/endMs` | int | required | 영상 구간 |
| body | `timestampFeedbacks[].transcript` | string | optional | STT |
| body | `timestampFeedbacks[].nonverbalScore` | object | optional | fluency / confidence_tone / eye_contact_posture / composure |
| body | `timestampFeedbacks[].(nonverbal/overall/vocal/attitude)Comment` | object/text | optional | CommentBlock |
| body | `isVerbalCompleted` | bool | required | verbal 완료 여부 |
| body | `isNonverbalCompleted` | bool | required | nonverbal 완료 여부 |
| header | `Authorization` | internal token | required | 내부 호출 인증 |

---

## 출력 (200)

| 필드 | 타입 | 의미 |
|------|------|------|
| (body 없음) | — | 저장 성공 |

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INVALID_INPUT` | schema 위반 |
| 404 | `NOT_FOUND` | interview / questionSet 부재 |
| 409 | `CONFLICT` | 낙관락 충돌 — `@Retryable(maxAttempts=3, backoff=100ms)` 소진 시 |
| 500 | `INTERNAL_ERROR` | persist 예외 |

---

## 흐름

### 1. 인증 + 리소스 검증
- 내부 토큰 검증
- interview / questionSet 존재 확인 (404)

### 2. 영속화
- `FeedbackService.saveFeedback` 진입 — `@Retryable(ObjectOptimisticLockingFailureException, maxAttempts=3, backoff=100ms)`
- `QuestionSetFeedbackPersister.persist`:
  1. QuestionSetFeedback (헤더) UPSERT — UNIQUE(question_set_id) 기반
  2. `TimestampFeedbackBatch.attachTo`: 턴별 mapper 변환 + question_id resolve (실패 시 WARN + null)
  3. `nonverbalScorePersister.persistAll`: 턴별 nonverbal 점수 → `QuestionScorePersister.saveNonverbal` (idempotent: findByQuestionIdAndRubricId pre-check)

### 3. 진척 갱신
- `analysis.completeAnalysis(verbal, nonverbal)` — QuestionSetAnalysis 상태 전이 (PARTIAL/COMPLETED/FAILED)

### 4. 인터뷰 종합 트리거
- 인터뷰 산하 모든 QuestionSetAnalysis `isResolved()` (COMPLETED|PARTIAL|SKIPPED) 인 경우
- `DeliveryEnrichmentRequestedEvent(interviewId)` publish
- → `SessionFeedbackEventListener` 가 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async(sessionFeedbackExecutor, Virtual Thread)` 로 enrichDelivery

### 5. 응답
- 200 No body

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 동시 콜백 (Lambda 재시도 + 정상) | UNIQUE(question_set_id) + idempotent persister → 안전 |
| `@Version` 낙관락 충돌 | `@Retryable` 3회 재시도 (100ms exponential), 소진 시 409 |
| `questionId` resolve 실패 | null 저장 + WARN 로그 — soft fail (#406 D4) |
| isVerbalCompleted=false / isNonverbalCompleted=false | 부분 완료 — analysis 상태 PARTIAL 처리. enrichment 트리거 = `isResolved()` 만 본다 (verbal 만 와도 시작 가능, 정책 미정 #406 G4) |
| 인터뷰 미존재 (삭제됨) | 404 — Lambda 측 로깅 후 폐기 |

---

## 상태 전이

```
QuestionSetAnalysis: PENDING → ANALYZING → PARTIAL/COMPLETED/FAILED
SessionFeedback (간접): (없음) → PRELIMINARY (InterviewCompletedEvent 시) → COMPLETE
```

---

## 관찰성

- **로그**: `FeedbackService` — `interviewId`, `questionSetId`, `timestampCount`, `verbal`, `nonverbal`, `retryAttempt`
- **메트릭**: `aiCallMetrics.incrementRubricFailure("persist_failed")` (간접: 본 endpoint 는 nonverbal 만, rubric 채점 실패는 별도 listener)
- **알람**: 409 비율 > 5% (5분) — 낙관락 충돌 과다 의심 (#406 D5)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.controller.FeedbackController` | endpoint | entry |
| `com.rehearse.api.domain.feedback.service.FeedbackService` | 트랜잭션 + 재시도 | calls |
| `com.rehearse.api.domain.feedback.service.QuestionSetFeedbackPersister` | 헤더 + 자식 + nonverbal 저장 | calls |
| `com.rehearse.api.domain.feedback.service.TimestampFeedbackBatch` | Question resolve | calls |
| `com.rehearse.api.domain.feedback.mapper.TimestampFeedbackMapper` | DTO→Entity (JSON 직렬화) | calls |
| `com.rehearse.api.domain.feedback.rubric.service.NonverbalScorePersister` | nonverbal rubric 저장 | calls |
| `com.rehearse.api.domain.feedback.score.service.QuestionScorePersister` | upsert (idempotent) | calls |
| `com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis` | 진척 상태 | persister |
| `com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent` | 종합 트리거 | event-publisher |

---

## 정책 출처

- `FeedbackService` `@Retryable(maxAttempts=3, backoff=100ms, ObjectOptimisticLockingFailureException)` (코드 직접 인용)
- `QuestionScorePersister.saveNonverbal` idempotent 보장 — findByQuestionIdAndRubricId
- `TimestampFeedbackBatch` Question resolve 실패 시 WARN + null
- `QuestionSetAnalysis.isResolved` = COMPLETED|PARTIAL|SKIPPED
- 부분 완료 enrichment 트리거 정책 = #406 G4 (의도 결정 필요)
