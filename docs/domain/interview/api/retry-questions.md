# API: 질문 생성 재시도

> Endpoint: `POST /api/v1/interviews/{id}/retry-questions`
> Action: 면접 생성 시 질문 생성이 `FAILED` 상태인 경우, 사용자가 재시도를 트리거한다.
> 관련 테이블: `interview` (write — questionGenerationStatus, failureReason)
> 관련 외부 의존: 없음 (실제 AI 호출은 외부 도메인 listener 가 수행)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| path | `id` | Long | required | 면접 PK |

---

## 출력 (200)

`InterviewResponse` (전체 메타 + `questionSets` 풀). `questionGenerationStatus` 가 `PENDING` 으로 reset 되어 응답.

## 출력 (4xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 401 | — | JWT 부재 / 만료 |
| 403 | `INTERVIEW_008 FORBIDDEN` | 본인 소유 아님 |
| 404 | `INTERVIEW_001 NOT_FOUND` | 면접 ID 없음 |
| 409 | `INTERVIEW_005 QUESTION_GENERATION_NOT_FAILED` | 현재 status != `FAILED` (재시도 가능 조건 미달) |

---

## 흐름

### 1. 조회 + 권한
- `InterviewFinder.findById(id)` → `validateOwner(userId)` (403)

### 2. 사전 조건
- `interview.questionGenerationStatus == FAILED` 필수
- 위반 시 `INTERVIEW_005 (409)` (예: `PENDING` / `GENERATING` / `COMPLETED` 모두 거부)

### 3. 도메인 메서드 호출
- `interview.resetForRetry()`:
  - `questionGenerationStatus = PENDING`
  - `failureReason = null`

### 4. 이벤트 발행
- `QuestionGenerationRequestedEvent` 재발행 (생성 시와 동일 페이로드 구조).
- ⚠️ **resumeText / resumeFileHash = null 로 발행** (`InterviewService.java:77-78`). 즉 RESUME_BASED 인 경우 재시도 시 이력서 컨텍스트 손실 가능.
  - 정책: 현재 코드는 RESUME_BASED 재시도에서 이력서 텍스트 미전달. 재시도 listener 가 다시 PDF 추출하거나 fallback 처리해야 함.
  - ⚠️ **정책-코드 갭** (추적: 별도 추후 검토 필요 — Issue #404 비스코프 외): RESUME_BASED 재시도 시 이력서 컨텍스트 재조달 경로 명확화 필요.

### 5. 응답
- 트랜잭션 commit 시 dirty checking → UPDATE
- `questionSetRepository.findByInterviewIdWithQuestions(id)` 재조회
- `InterviewResponse.from(interview, questionSets)` 반환

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| `PENDING` 에서 retry 호출 | `INTERVIEW_005 (409)` — `FAILED` 만 허용 |
| `COMPLETED` 에서 retry 호출 | `INTERVIEW_005 (409)` |
| 동시 retry 호출 | 무락 → 둘 다 PENDING reset + 이벤트 2회 발행 가능. 정책: ❓TODO 락 정책 (Issue #404 비스코프) |
| 재시도 listener 가 다시 FAILED | `failureReason` 갱신 + `FAILED` 유지. 무한 재시도 차단은 정책 미정 |
| RESUME_BASED 재시도 | 이력서 텍스트 / 해시 = null 로 발행 (⚠️ 위 4번 갭) |

---

## 상태 전이

```
questionGenerationStatus:
FAILED ── retryQuestionGeneration ──► PENDING
                                          │
                        listener 재실행 ──┼──► GENERATING ──► COMPLETED
                                          │                 └► FAILED (다시 재시도 가능)
                                          └► (외부 도메인 책임)
```

`interview.status` (READY / IN_PROGRESS / COMPLETED) 자체는 변경 안 됨.

---

## 관찰성

- **로그**: `InterviewService.retryQuestionGeneration` — `질문 생성 재시도 이벤트 발행: id={}` (INFO)
- **메트릭**: 직접 발행 없음. 재시도 listener 가 AI 호출 메트릭 기록.
- **알람**: 별도 없음. 무한 재시도 / 반복 FAILED 모니터링 정책 미정 (❓TODO).

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.entity.Interview` (`resetForRetry`) | 상태 reset | calls |
| `com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent` | 재시도 트리거 | event-publisher |
| `com.rehearse.api.domain.questionset.repository.QuestionSetRepository` | 응답용 fetch | calls |
| `org.springframework.context.ApplicationEventPublisher` | 이벤트 발행 | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (questionGenerationStatus 흐름)
- 임계값:
  - `QuestionGenerationStatus = FAILED` 만 retry 허용 (`InterviewService.java:63`)
  - 재시도 횟수 / cooldown / 무한 retry 방지 = 정책 부재
- ❓TODO(사용자 확인):
  - 재시도 횟수 제한 / cooldown 정책 (현재 무제한)
  - RESUME_BASED 재시도 시 이력서 컨텍스트 재조달 경로 (정책-코드 갭, Issue #404 비스코프 외 — 추후 검토)
  - 동시 retry 락 정책 (Issue #404 비스코프 보류)
