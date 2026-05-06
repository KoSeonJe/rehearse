# API: 면접 상태 전이 + 미응답 스킵

> Endpoints (2개 묶음):
> - `PATCH /api/v1/interviews/{id}/status` — 명시 상태 전이 (`READY → IN_PROGRESS` / `IN_PROGRESS → COMPLETED`)
> - `POST /api/v1/interviews/{id}/skip-remaining` — 진행 중 면접의 미응답 question_set 일괄 스킵
>
> Action: 사용자가 면접 시작 / 종료 / 미응답 스킵 처리를 트리거한다.
> 관련 테이블: `interview` (write — status) / `question_set` (write — analysis_status, 외부 도메인)
> 관련 외부 의존: 없음 (별도 폴링 종료는 `InterviewCompletionService` 30초 스케줄러 — 본 API 와 독립)

---

## 입력 — 명시 전이

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| path | `id` | Long | required | 면접 PK |
| body | `status` | enum | required | 목표 상태 (`IN_PROGRESS` / `COMPLETED`) |

## 입력 — 미응답 스킵

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| path | `id` | Long | required | 면접 PK |

---

## 출력 — 명시 전이 (200)

| 필드 | 타입 | 의미 |
|------|------|------|
| `id` | Long | 면접 PK |
| `status` | enum | 변경된 상태 |

## 출력 — 미응답 스킵 (200)

본문 없음 (`ApiResponse.ok(null)`).

## 출력 (4xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 401 | — | JWT 부재 / 만료 |
| 403 | `INTERVIEW_008 FORBIDDEN` | 본인 소유 아님 |
| 404 | `INTERVIEW_001 NOT_FOUND` | 면접 ID 없음 |
| 409 | `INTERVIEW_002 INVALID_STATUS_TRANSITION` | 허용 안 된 전이 (예: `COMPLETED → READY`, `READY → COMPLETED` 등) |
| 409 | `INTERVIEW_003 NOT_IN_PROGRESS` | skip-remaining 호출 시 `status != IN_PROGRESS` |
| 409 | `INTERVIEW_004 QUESTION_GENERATION_NOT_COMPLETED` | `READY → IN_PROGRESS` 시 `questionGenerationStatus != COMPLETED` |
| 400 | Bean Validation | `status` 미지정 / 알 수 없는 값 |

---

## 흐름

### 1. 명시 전이 (`PATCH /{id}/status`)

#### 1-1. 조회 + 권한
- `InterviewFinder.findById(id)` → `validateOwner(userId)` (403)

#### 1-2. 분기: 목표 상태별 사전 조건

##### READY → IN_PROGRESS
- `interview.questionGenerationStatus == COMPLETED` 필수
- 위반 시 `INTERVIEW_004 (409)`

##### IN_PROGRESS → COMPLETED
- 별도 사전 조건 없음 (사용자 명시 종료 허용)

#### 1-3. 도메인 메서드 호출
- `interview.updateStatus(target)`:
  - 내부에서 `InterviewStatus.canTransitionTo(target)` 검사
  - 위반 시 `IllegalStateException` → Service 가 `INTERVIEW_002 (409)` 로 래핑
- 허용 매트릭스:
  ```
  READY        → IN_PROGRESS  (+ questionGenerationStatus = COMPLETED 추가 검증)
  IN_PROGRESS  → COMPLETED
  COMPLETED    → (없음)        // canTransitionTo 항상 false
  READY        → READY / COMPLETED  // 금지
  IN_PROGRESS  → READY               // 금지
  ```

#### 1-4. 저장 + 응답
- 트랜잭션 commit 시 dirty checking 으로 UPDATE
- `UpdateStatusResponse.from(interview)` 반환

### 2. 미응답 스킵 (`POST /{id}/skip-remaining`)

#### 2-1. 조회 + 권한
- 동일 (`findById` → `validateOwner`)

#### 2-2. 사전 조건
- `interview.status == IN_PROGRESS` 필수 (위반 시 `INTERVIEW_003 (409)`)

#### 2-3. 위임
- `questionSetService.skipRemaining(id)` — 외부 도메인 (`questionset`) 이 미응답 (`PENDING` analysis) 인 question_set 들을 `SKIPPED` 로 일괄 갱신
- `interview.status` 자체는 변경하지 않음. `InterviewCompletionService` 30초 polling 이 모든 question_set 이 `SKIPPED|COMPLETED|PARTIAL` 이 되면 자동 `COMPLETED` 전이.

#### 2-4. 응답
- 200, 본문 없음

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 동시 PATCH (같은 사용자, 같은 ID) | 무락 — 둘 다 `READY → IN_PROGRESS` 시도 시 dirty checking last-writer-wins. 정책: ❓TODO 락 정책 미정 (Issue #404 비스코프) |
| `COMPLETED` 면접에 PATCH | `canTransitionTo` 항상 false → `INTERVIEW_002 (409)` |
| 질문 생성 미완에서 시작 시도 | `INTERVIEW_004 (409)` |
| skip-remaining 후 question_set 모두 SKIPPED | 30초 내 polling 으로 `IN_PROGRESS → COMPLETED` 자동 전이 (`InterviewCompletionService`) |
| 부분 완료 (`partial > 0` + 미해결 0) | polling 이 `isAllResolved` 통과 → COMPLETED |

---

## 상태 전이

```
READY ── PATCH(IN_PROGRESS) ──► IN_PROGRESS  (조건: questionGenerationStatus = COMPLETED)
                                    │
                                    ├── PATCH(COMPLETED) ──► COMPLETED  (사용자 명시)
                                    │
                                    └── (모든 question_set 해결) ──► COMPLETED  (스케줄러 자동, 30초 polling)
```

`questionGenerationStatus` 흐름 (참고):
```
PENDING → GENERATING → COMPLETED | FAILED
                FAILED ── retryQuestionGeneration ──► PENDING
```

---

## 관찰성

- **로그**:
  - `InterviewService.updateStatus` — `면접 세션 상태 변경: id={}, newStatus={}` (INFO)
  - `InterviewService.skipRemainingQuestionSets` — `미응답 질문세트 스킵 처리: interviewId={}` (INFO)
  - `InterviewCompletionService` — `면접 완료 처리: interviewId={}, completed={}, partial={}, skipped={}` (INFO)
- **메트릭**: 직접 발행 없음.
- **알람**: 별도 없음.

---

## 동시성 / 트랜잭션 노트

- `InterviewService.updateStatus` / `skipRemainingQuestionSets` / `retryQuestionGeneration` = `@Transactional` (write)
- `InterviewCompletionService.checkAndCompleteInterviews` = `@Transactional` + `@Scheduled(fixedDelay = 30_000)`. 매번 fresh `findById` 로 stale 방어.
- 락: 무 (낙관락 / 비관락 미적용). ❓TODO (Issue #404 비스코프 보류).

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.entity.Interview` (도메인 메서드 `updateStatus`) | 상태 전이 로직 | calls |
| `com.rehearse.api.domain.interview.entity.InterviewStatus.canTransitionTo` | 전이 매트릭스 | reads |
| `com.rehearse.api.domain.questionset.service.QuestionSetService` | skipRemaining 위임 | calls |
| `com.rehearse.api.domain.questionset.entity.AnalysisStatus` | 완료 판정 | reads — `InterviewCompletionService` |
| `com.rehearse.api.domain.interview.event.InterviewCompletedEvent` | 완료 이벤트 | event-publisher — `InterviewCompletionService` |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (상태 전이 + isDeletable 갭 #404 #1)
- 임계값:
  - `InterviewStatus` 매트릭스 (코드 상수, `InterviewStatus.java:8-13`)
  - completion polling 30초 (`InterviewCompletionService:29` `@Scheduled(fixedDelay = 30_000)`)
- ❓TODO(사용자 확인) — Issue #404 비스코프 (보류):
  - 상태 전이 락 정책 (낙관락 / 비관락 / 무락 — 동시 PATCH 정책)
