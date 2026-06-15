# API: 전달력 재시도 (관리자)

> Endpoint: `POST /api/v1/admin/interviews/{interviewId}/session-feedback/retry-delivery` (`@PreAuthorize("hasRole('ADMIN')")`)
> Action: enrichDelivery 실패 / 누락된 SessionFeedback 의 전달력 분석 재실행
> 관련 테이블: `session_feedback` (write)
> 관련 외부 의존: OpenAI GPT-4o-mini (synthesizer), Claude fallback `claude-sonnet-4-20250514`

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `interviewId` | Long | required | 인터뷰 |
| header | `Authorization` | Bearer (ADMIN) | required | JWT |

---

## 출력 (200)
| 필드 | 타입 | 의미 |
|------|------|------|
| (응답 본문은 갱신된 SessionFeedback DTO) | — | — |

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 403 | `FORBIDDEN` | ADMIN 아님 |
| 404 | `NOT_FOUND` | session_feedback 미생성 |
| 409 | `BUSY` | cool-down 60s 이내 또는 `delivery_retryable=false` |
| 500 | `PARSE_FAILED` | LLM 파싱 실패 (parser 검증 실패) |

---

## 흐름

### 1. 권한
- `hasRole('ADMIN')` 검증 (403)

### 2. 사전 검증
- `SessionFeedback.isRetryCoolingDown()` (`retry_started_at + 60초`) → BUSY
- `SessionFeedbackFailurePolicy.isRetryable(lastFailureReason)` 현재 모든 사유 retryable=true (상한 없음 — #406 G1/D2)

### 3. 재실행
- `incrementRetry()` (낙관락 `@Version`)
- `SessionFeedbackEventListener` 동일 경로로 enrichDelivery 호출 (synchronous in admin path)
- `SessionFeedbackSynthesizer.synthesize()` — `gpt-4o-mini`, temp=0.4
- PARSE_FAILED → 1회 retry → 실패 시 `recordFailure(lastFailureReason)` (64자 cap)
- `NoOpLambdaRetryTrigger` 호출 — 모든 프로파일 no-op (실 재트리거 미구현, #406 D1)

### 4. 결과
- 성공: `applyDeliveryEnrichment()` + `markComplete()` → COMPLETE
- 실패: `markCompleteWithFailure()` 또는 `recordFailure()` (재시도 여지 보존)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| cool-down 60s 이내 | 409 BUSY |
| delivery_retryable=false | 409 BUSY (현재 항상 true 라 발동 안 됨 #406 G1) |
| `@Version` 낙관락 충돌 | (현재 명시 retry 없음) — 호출자 재시도 |
| LLM 두 provider 모두 실패 | 500 PARSE_FAILED 또는 503 (ResilientAiClient SERVICE_UNAVAILABLE) |
| retry_attempts 누적 | 무제한 증가 (#406 G1/D2/D9) |

---

## 관찰성

- **로그**: `SessionFeedbackService.retryDelivery` — `interviewId`, `retryAttempts`, `lastFailureReason`
- **메트릭**: `aiCallMetrics.incrementSynthesizerFailure(reason)`

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.session.controller.AdminSessionFeedbackController` | endpoint | entry |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackService.retryDelivery` | 오케스트레이터 | calls |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackFailurePolicy` | retryable 판정 (현재 항상 true) | calls |
| `com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackSynthesizer` | LLM | calls |
| `com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser` | 검증 | calls |
| `com.rehearse.api.domain.feedback.session.infra.NoOpLambdaRetryTrigger` | Lambda 재시도 (no-op, 모든 프로파일) | calls |

---

## 정책 출처

- cool-down: `SessionFeedback.isRetryCoolingDown` = 60초
- 모델 / 온도: `SessionFeedbackSynthesizerPromptBuilder` (`gpt-4o-mini`, `0.4`)
- failure reason cap: 64자 (`SessionFeedbackService` substring + 컬럼 제약)
- `SessionFeedbackFailurePolicy.isRetryable` 현재 모든 사유 retryable=true (#406 G1)
- `NoOpLambdaRetryTrigger` `@Component` 만 — 모든 프로파일 no-op (#406 D1)
- Claude fallback 모델: `application-{local,dev,prod}.yml` `claude.model=claude-sonnet-4-20250514`
