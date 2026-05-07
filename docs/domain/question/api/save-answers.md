# API: 답변 구간 저장 (영상 타임스탬프 매핑)

> Endpoint: `POST /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/answers`
> Action: 클라이언트가 녹화 종료 후 영상 내 각 질문에 대한 답변 구간 (`startMs`, `endMs`) 을 묶어 전달. 서버는 멱등하게 갱신 + 분석 단계 (`AnalysisStatus.PENDING_UPLOAD`) 진입.
> 관련 테이블: `question_set` (read) / `question` (read) / `question_answer` (write — DELETE + INSERT) / `question_set_analysis` (write — `analysis_status` 전이)
> 관련 외부 의존: 없음 (S3 업로드는 `/upload-url` 별도 엔드포인트)


---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `interviewId` | Long | required | 소유권 검증용 |
| path | `questionSetId` | Long | required | 답변 대상 세트 |
| body | `answers` | `List<AnswerTimestamp>` | `@NotEmpty` (controller `@Valid`) | 답변 구간 N개 |
| body | `answers[].questionId` | Long | `@NotNull` | 매핑 질문 |
| body | `answers[].startMs` | Long | `@NotNull` | 영상 시작 ms |
| body | `answers[].endMs` | Long | `@NotNull` | 영상 종료 ms |
| header | `Authorization` | Bearer | required | JWT |

> ⚠️ `answers` 가 빈 배열인 경우 controller `@NotEmpty` 가 400 (`INVALID_INPUT`) 반환. 그러나 service 진입 시점에서도 `request.getAnswers() == null || isEmpty()` 가드가 존재한다 (defense-in-depth, no-op). `QuestionSetService.saveAnswers:64-67` 참고.

---

## 출력 (201)

본문 없음 (`ApiResponse<Void>`).

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INVALID_INPUT` | `answers` 빈 배열 / 필수 필드 누락 |
| 401 | `UNAUTHORIZED` | JWT 부재 / 무효 |
| 403 | `INTERVIEW_002 FORBIDDEN` | `validateOwner(userId)` 실패 — 다른 사용자 interview |
| 404 | `INTERVIEW_001 NOT_FOUND` | `interviewId` 부재 |
| 404 | `QUESTION_SET_001 NOT_FOUND` | `questionSetId` 부재 |
| 404 | `QUESTION_SET_001 NOT_FOUND` | `answers[].questionId` 부재 — `findById` 실패 시 `QuestionSetErrorCode.NOT_FOUND` 로 변환 (실코드: `QuestionSetService.saveAnswers:78`) |

> ⚠️ **개선 후보 (Issue #407)**: `questionId` 부재 시 `QUESTION_SET_001 NOT_FOUND` 로 응답하지만 실제 실패 원인은 question 자체. 별도 `QUESTION_NOT_FOUND` 에러 코드 도입 후보.

---

## 흐름

### 1. 권한 검증 (controller)

- `interviewFinder.findById(interviewId).validateOwner(userId)` — interview 소유자만 진입.
- 위치: `QuestionSetController.saveAnswers:36`.

### 2. questionSet 로드

- `QuestionSetService.findQuestionSet(questionSetId)` — `QuestionSetRepository.findById` → 부재 시 `QUESTION_SET_001 NOT_FOUND`.

### 3. 빈 요청 가드 (no-op)

- `request.getAnswers() == null || isEmpty()` → WARN 로그 + 조용히 return.
- 사유: 클라이언트 면접 종료 복구 플로우 경합 상황에서 부분 POST 가 정상 데이터를 지우는 것을 방지 (defense-in-depth).

### 4. 멱등성 보장 — 기존 답변 삭제

- `answerRepository.deleteByQuestionSetId(questionSetId)` — JPQL DELETE (`@Modifying`). subquery `Question.questionSet.id = :questionSetId` 매칭 row 일괄 제거.
- `answerRepository.flush()` — 후속 INSERT 와 DELETE 순서 보장.
- 사유: 같은 질문셋에 대한 재호출 시 중복 행이 쌓이지 않도록 강제 (`QuestionAnswerRepository:20-24`).

### 5. 신규 답변 INSERT

- `request.getAnswers().stream()` 각 항목에 대해:
  - `questionRepository.findById(questionId)` — 부재 시 `QUESTION_SET_001 NOT_FOUND`.
  - `QuestionAnswer.builder().question(q).startMs(...).endMs(...).build()`.
- `answerRepository.saveAll(answers)` — JPA persist (단일 트랜잭션 내 batch).

### 6. 분석 상태 전이

- `findOrCreateAnalysis(questionSet)` — `question_set_analysis` row 부재 시 신규 생성, 존재 시 기존 사용.
- `analysis.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD)` — 다음 단계 (영상 업로드 / Lambda 분석 트리거) 진입 마킹.

### 7. 응답

- `201 CREATED` + `ApiResponse.ok(null)`.
- 부수 효과: `analysis_status = PENDING_UPLOAD` 전이. 후속 `/upload-url` 호출에서 S3 presigned URL 발급 + Lambda 분석 트리거 (S3 이벤트 → EventBridge → Lambda).

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 빈 요청 (`answers = []`) | controller `@NotEmpty` 400. service 가드는 defense-in-depth (실 도달 안 함) |
| `questionId` 가 다른 questionSet 의 question | 검증 부재. ⚠️ `QuestionAnswer.question_id` 만 FK 검증. Issue #407 후보 (cross-set ID 위조) |
| 동시 POST (같은 questionSetId) | DELETE-INSERT 직렬화 미보장. 후행 트랜잭션이 DELETE 후 선행 INSERT 가 살아남으면 partial state. ⚠️ `@Version` / 비관락 부재 (Issue #407) |
| `startMs > endMs` 또는 음수 | 검증 부재. ⚠️ Issue #407 후보 (range 검증) |
| 권한 mismatch | `interview.validateOwner` 403 |
| `questionId` 부재 | 404 (`QUESTION_SET_001 NOT_FOUND` — 부정확한 코드. 개선 후보) |
| `analysis_status` 가 이미 후속 단계 (`COMPLETED` / `IN_PROGRESS`) 인 상태에서 재호출 | 무조건 `PENDING_UPLOAD` 로 후행 전이. ⚠️ 분석 완료 후 답변 변경 시 분석 결과 stale. Issue #407 후보 (FSM invariant) |

---

## 상태 전이

```
question_set_analysis.analysis_status:
{없음}                  → PENDING_UPLOAD  (saveAnswers 첫 호출)
PENDING_UPLOAD          → PENDING_UPLOAD  (재호출 — 멱등)
IN_PROGRESS / COMPLETED → PENDING_UPLOAD  ⚠️ 의도적 미설계 (FSM 가드 부재)
```

---

## 관찰성

- **로그**: `QuestionSetService` `@Slf4j` —
  - WARN: `답변 저장 요청이 비어 있음 — 기존 데이터 보호를 위해 skip: questionSetId={}`
  - INFO: `답변 구간 저장 완료: questionSetId={}, count={}`
- **메트릭**: 없음 (미도입).
- **알람**: 없음.

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.question.controller.QuestionSetController` | HTTP 진입 | controller |
| `com.rehearse.api.domain.question.service.QuestionSetService` | 트랜잭션 경계 + 비즈 로직 | app service |
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | `question_set` 조회 | persister |
| `com.rehearse.api.domain.question.entity.QuestionSet` | 부모 엔티티 | aggregate root |
| `com.rehearse.api.domain.question.entity.QuestionSetAnalysis` | `analysis_status` 보유 | child aggregate |
| `com.rehearse.api.domain.question.repository.QuestionSetAnalysisRepository` | analysis row 조회 / 생성 | persister |
| `com.rehearse.api.domain.question.dto.SaveAnswersRequest` | 요청 DTO (validation 포함) | input |
| `com.rehearse.api.domain.question.entity.Question` | 답변 매핑 대상 질문 | read |
| `com.rehearse.api.domain.question.repository.QuestionRepository` | question 조회 | persister |
| `com.rehearse.api.domain.question.entity.QuestionAnswer` | 답변 구간 엔티티 | write |
| `com.rehearse.api.domain.question.repository.QuestionAnswerRepository` | DELETE / saveAll | persister |
| `com.rehearse.api.domain.interview.service.InterviewFinder` | `interviewId` 조회 + `validateOwner` | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/question/schema.md` `question_answer` / `question_set_analysis` 섹션
- 멱등성 / 빈 요청 가드 사유: `QuestionSetService.saveAnswers:62-72` 코드 주석 인용
- 동시성 / FSM invariant 부재: ❓ 정책 미결 — Issue #407 추적 (후속 결정 필요 항목)
- ❓TODO(사용자 확인): cross-set `questionId` 위조 검증 / `startMs <= endMs` range 검증 / `analysis_status` 후속 전이 가드 — 정책 결정 필요
