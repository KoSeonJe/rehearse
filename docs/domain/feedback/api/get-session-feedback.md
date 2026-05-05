# API: 인터뷰 종합 피드백 조회

> Endpoint:
> - `GET /api/v1/interviews/{interviewId}/session-feedback` (사용자)
> - `GET /api/v1/admin/interviews/{interviewId}/session-feedback` (관리자, `@PreAuthorize("hasRole('ADMIN')")`)
> Action: 인터뷰 1회 종합 피드백 조회 (PRELIMINARY 또는 COMPLETE 상태 그대로)
> 관련 테이블: `session_feedback` (read)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `interviewId` | Long | required | 인터뷰 |
| header | `Authorization` | Bearer | required | JWT |

---

## 출력 (200)

| 필드 | 타입 | 의미 |
|------|------|------|
| `interviewId` | Long | 인터뷰 |
| `status` | enum | PRELIMINARY / COMPLETE |
| `coverage` | string? | 커버리지 라벨 |
| `overall` | object? | OverallSection |
| `strengths` | array | StrengthItem[] |
| `gaps` | array | GapItem[] |
| `delivery` | object? | DeliverySection (COMPLETE 일 때만 신뢰) |
| `weekPlan` | array | WeekPlanItem[] |
| `lastFailureReason` | string? | 최근 실패 사유 |
| `deliveryRetryable` | bool? | 재시도 가능 |

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 403 | `FORBIDDEN` | 본인 인터뷰 아님 (사용자 endpoint) |
| 404 | `NOT_FOUND` | session_feedback 미생성 |

---

## 흐름

### 1. 인증 / 권한
- 사용자: `SessionFeedbackService.getByInterviewForUser(interviewId, userId)` — ownership 검증 (403)
- 관리자: `getByInterview(interviewId)` — ownership 검증 skip

### 2. 조회
- `session_feedback` UNIQUE(interview_id) 기반 단건 조회
- 부재 시 404 (`SessionFeedbackErrorCode.NOT_FOUND`)

### 3. 응답
- entity → DTO 변환 후 반환

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 인터뷰 진행 중 (InterviewCompletedEvent 미발행) | 404 |
| PRELIMINARY 상태 | 200 — delivery 누락 가능. FE skeleton + polling 5s 표현 (#406 G3 검증) |
| Watchdog 타임아웃 (PRELIMINARY > 10분) | COMPLETE + lastFailureReason 세팅 → 200 (#406 D7) |

---

## 상태 전이 (참고)

```
(없음) → PRELIMINARY (InterviewCompletedEvent + synthesizePreliminary 성공)
PRELIMINARY → COMPLETE (DeliveryEnrichmentRequestedEvent + enrichDelivery 성공)
PRELIMINARY → COMPLETE (Watchdog 강제 — markCompleteDueToTimeout, fixedDelay=60_000ms, default 10분 cutoff)
PRELIMINARY → PRELIMINARY (recordSynthesisFailure, retryAttempts++)
```

---

## 관찰성

- **로그**: `SessionFeedbackService` — `interviewId`, `userId`, `status`
- **메트릭**: 조회 카운트 (별도 계측 없음 — controller 표준 access log 의존)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.session.controller.UserSessionFeedbackController` | endpoint | entry |
| `com.rehearse.api.domain.feedback.session.controller.AdminSessionFeedbackController` | admin endpoint | entry |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackService` | 도메인 서비스 | calls |
| `com.rehearse.api.domain.feedback.session.exception.SessionFeedbackErrorCode` | NOT_FOUND/FORBIDDEN | calls |

---

## 정책 출처

- ownership: `SessionFeedbackService.getByInterviewForUser` — interview.userId 비교
- Watchdog timeout: `SessionFeedbackWatchdog` `@Scheduled(fixedDelay=60_000)`, `rehearse.feedback-synthesizer.delivery-timeout-minutes=10`
- FE PRELIMINARY 표현: skeleton + polling 5s (#406 G3 — FE 구현 검증 필요)
