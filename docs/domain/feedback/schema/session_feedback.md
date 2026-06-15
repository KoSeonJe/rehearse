# feedback 스키마 — session_feedback

> 대상 마이그레이션: `V27__create_session_feedback.sql`, `V32__session_feedback_status_and_retry.sql`

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `session_feedback` | 인터뷰 1회 종합 피드백 (PRELIMINARY → COMPLETE 2단계) | — |

---

## session_feedback

### 성격
인터뷰 (Interview) 1회 종합 피드백. row 1개 = Interview 1개. PRELIMINARY (강점/약점/주차계획) 먼저 생성 후 enrichDelivery (전달력 dimension 합산) 가 채워지면 COMPLETE 로 전이.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `interview_id` | BIGINT | UNIQUE, NOT NULL | 인터뷰 (1:1) |
| `overall` | JSON | NULL | OverallSection (dimensionScores / levelAssessment / narrative / coverage) |
| `strengths` | JSON | NULL | StrengthItem[] |
| `gaps` | JSON | NULL | GapItem[] |
| `delivery` | JSON | NULL | DeliverySection (전달력) |
| `week_plan` | JSON | NULL | WeekPlanItem[] |
| `status` | VARCHAR | NOT NULL DEFAULT 'PRELIMINARY' | PRELIMINARY \| COMPLETE |
| `coverage` | VARCHAR(64) | NULL | 분석 커버리지 라벨 |
| `delivery_retryable` | BOOLEAN | NULL | 재시도 가능 여부 |
| `last_failure_reason` | VARCHAR(64) | NULL | 실패 사유 (64자 cap) |
| `retry_attempts` | INT | NULL | 재시도 횟수 (현재 상한 없음 — 무제한, #406 G1 참조) |
| `retry_started_at` | DATETIME(6) | NULL | 마지막 재시도 시각 (cool-down 60s 기준) |
| `version` | BIGINT | NOT NULL | `@Version` 낙관락 |
| `created_at` / `updated_at` | DATETIME(6) | NOT NULL | — |

### 인덱스
- `uk_session_feedback_interview_id` (`interview_id`) — UNIQUE — 인터뷰 당 1행 강제

### 불변 / 정책
- 상태 전이: `PRELIMINARY → COMPLETE` (단방향). 역전이 없음
- `markComplete()` / `markCompleteWithFailure()` / `applyDeliveryEnrichment()` rich-domain 만 사용
- `isRetryCoolingDown()` = `retry_started_at + 60초` 이내 시 BUSY (409) 응답
- `last_failure_reason` 64자 cap (서비스 레이어에서 substring)
- 동시성: `@Version` 낙관락. 충돌 시 호출자 판단 (현재 listener async 단발)
- `SessionFeedbackFailurePolicy.isRetryable` 현재 모든 사유 retryable=true (상한 없음 — admin cool-down 60s 만 의존). 정책 갭 = #406 G1
- 외부 의존 (`SessionFeedbackSynthesizer` LLM) PARSE_FAILED → 1회 retry → 실패 시 `recordSynthesisFailure` (placeholder 행 생성 가능)

### 마이그레이션 히스토리
- `V27__create_session_feedback.sql` — 기본 스키마
- `V32__session_feedback_status_and_retry.sql` — status / coverage / retry / version 추가, JSON nullable 완화, synthesizer_model 제거

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.session.SessionFeedbackService` | 도메인 오케스트레이터 | persister |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackPersistenceService` | 저장 분리 (preliminary / enriched) | persister |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackEventListener` | InterviewCompletedEvent / DeliveryEnrichmentRequestedEvent 청취 | event-listener |
| `com.rehearse.api.domain.feedback.session.SessionFeedbackWatchdog` | `@Scheduled(fixedDelay=60_000)` PRELIMINARY > 10분 → 강제 COMPLETE | scheduler |
| `com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackSynthesizer` | LLM 호출 (gpt-4o-mini, temp 0.4) | calls — `ResilientAiClient` 경유 |
| `com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser` | 카디널리티 / 추상어 / source-mix 검증 | calls |
| `com.rehearse.api.domain.feedback.session.infra.NoOpLambdaRetryTrigger` | Lambda 재시도 (현재 모든 프로파일 no-op) | calls |
| `com.rehearse.api.domain.interview.event.InterviewCompletedEvent` | publisher = `InterviewCompletionService` (`@Scheduled fixedDelay=30_000ms`) | event-source |
