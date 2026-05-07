# API: 면접 삭제 시 답변 / 질문세트 cascade

> Endpoint: **별도 엔드포인트 없음**. `DELETE /api/v1/interviews/{id}` (`InterviewController.deleteInterview`) 호출 시 `InterviewDeletionService.deleteInterview` 가 question 도메인 자식 데이터를 명시 순서로 hard-delete.
> Action: 면접 삭제 시 영상 / 답변 / 분석 / 피드백 / 점수 / 질문세트 / 질문을 일괄 제거. soft-delete 미도입 — DB 행 자체 삭제.
> 관련 테이블: `question_answer` (DELETE) / `question` (CASCADE — `question_set_id` FK ON DELETE CASCADE) / `question_set` (DELETE) / `question_set_analysis` (DELETE) / `question_set_feedback` + `timestamp_feedback` (DELETE)
> 관련 외부 의존: 없음 (S3 영상 객체 / Lambda 산출물은 본 흐름에서 제거하지 않음 — ⚠️ Issue #407 후보)

> 삭제 오케스트레이션은 `domain/interview/service/InterviewDeletionService` 가 담당하며, question / feedback Repository 를 직접 참조한다 (cross-domain repository 호출). 컨벤션상 허용 (상위 → 하위).

---

## 입력

`DELETE /api/v1/interviews/{id}` 의 입력을 그대로 따른다 (자세한 명세는 `docs/domain/interview/api/delete-interview.md`).

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `id` | Long | required | 삭제 대상 interview |
| header | `Authorization` | Bearer | required | JWT — `validateOwner` 매칭 |

---

## 출력 (200)

본문 없음 (`ApiResponse<Void>`).

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 401 | `UNAUTHORIZED` | JWT 부재 / 무효 |
| 403 | `INTERVIEW_002 FORBIDDEN` | `validateOwner(userId)` 실패 |
| 404 | `INTERVIEW_001 NOT_FOUND` | `interviewId` 부재 |

> ⚠️ **dead code** (Issue #407): `InterviewErrorCode.CANNOT_DELETE_COMPLETED` 가 정의만 존재 (호출처 0건). 현재 `deleteInterview` 는 status 무관 모두 삭제 허용 — `COMPLETED` 도 hard-delete. 사용자 결정 보류 (제거 / 활성화).

---

## 흐름

### 1. 권한 검증

- `interviewFinder.findById(id)` → `interview.validateOwner(userId)` (`InterviewDeletionService:31-32`).
- 부재 시 404, 권한 불일치 시 403.

### 2. 명시 순서 hard-delete

`InterviewDeletionService.deleteInterview:35-40` 의 호출 순서 — FK 제약 위반 방지를 위해 자식부터 명시 삭제.

```
1. questionAnswerRepository.deleteAllByInterviewId(id)
   → JPQL DELETE: question_answer WHERE question.questionSet.interview.id = :interviewId

2. timestampFeedbackRepository.deleteAllByInterviewId(id)
   → feedback 도메인 — 영상 타임스탬프별 AI 피드백

3. questionSetFeedbackRepository.deleteAllByInterviewId(id)
   → feedback 도메인 — 세트 단위 종합 피드백

4. questionSetAnalysisRepository.deleteAllByInterviewId(id)
   → question 도메인 — Lambda 분석 결과

5. questionSetRepository.deleteAll(findByInterviewIdOrderByOrderIndex(id))
   → question 도메인. 본 단계 호출 시 question 은 CASCADE
     (V41 fk_question_question_set ON DELETE CASCADE) 로 함께 제거

6. interviewRepository.delete(interview)
   → interview / interview_cs_sub_topics ElementCollection / interview_plan
     등 interview 도메인 자식은 별도 cascade / repository 처리
     (docs/domain/interview/api/delete-interview.md 참고)
```

### 3. 트랜잭션

- `@Transactional` (writable) — 6 단계 전부 단일 트랜잭션.
- 어느 단계 실패 시 전체 롤백.

### 4. 응답

- `200 OK` + `ApiResponse.ok(null)`.

---

## CASCADE 매트릭스 (question 도메인)

| 부모 삭제 | 자식 테이블 | 처리 방식 | FK 제약 |
|----------|------------|----------|---------|
| `interview` | `question_set` | service 명시 `deleteAll` | `fk_question_set_interview` ON DELETE CASCADE (DB) — defense in depth |
| `question_set` | `question` | DB CASCADE | `fk_question_question_set` ON DELETE CASCADE (V41) |
| `question_set` | `question_set_analysis` | service 명시 `deleteAllByInterviewId` (5단계 이전) | FK 제약 |
| `question` | `question_answer` | service 명시 `deleteAllByInterviewId` (1단계) | `fk_question_answer_question` 일반 FK (CASCADE 미설정 — 수동 삭제 필요) |
| `question_pool` | `question.question_pool_id` | DB SET NULL | `fk_question_pool` ON DELETE SET NULL (V11) — 풀 삭제 시 question 보존 |

> ⚠️ **이중 처리** (의도적): `question_set_analysis` / `question_set_feedback` / `timestamp_feedback` 은 service 가 명시 DELETE 호출. DB 레벨 ON DELETE CASCADE 가 있어도 `@DataJpaTest` / `truncate` 환경 이슈 / 트랜잭션 가시성 차이 회피 목적.

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| `interviewId` 부재 | 404 (`INTERVIEW_001 NOT_FOUND`) |
| 다른 사용자 interview | 403 |
| `status = COMPLETED` 인 interview | ⚠️ 현재 허용 (`CANNOT_DELETE_COMPLETED` dead code). 정책 결정 필요 (Issue #407) |
| 분석 진행 중 (`IN_PROGRESS`) | 가드 부재 — 즉시 삭제. Lambda 산출물 / S3 객체 orphan 가능 (Issue #407 후보) |
| 동시 호출 (같은 `interviewId`) | last-write-wins. `@Version` 부재 (Issue #404 / #407) |
| 영상 S3 객체 / Lambda 산출물 | **삭제 안 됨** ⚠️ 본 흐름은 DB only. S3 정리는 별도 lifecycle / cleanup job 필요 (현재 미구현 — Issue #407 후보) |

---

## 관찰성

- **로그**: `InterviewDeletionService` —
  - INFO: `면접 세션 삭제: id={}, userId={}` (성공 후 단일 라인)
- 단계별 row count / 실패 위치 로그 없음 ⚠️ 디버깅 용이성 후보 (Issue #407)
- **메트릭**: 없음.
- **알람**: 없음.

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.controller.InterviewController` | HTTP 진입 (`@DeleteMapping("/{id}")`) | controller |
| `com.rehearse.api.domain.interview.service.InterviewDeletionService` | 삭제 오케스트레이션 (cross-domain) | app service |
| `com.rehearse.api.domain.interview.service.InterviewFinder` | interview 조회 + `validateOwner` | calls |
| `com.rehearse.api.domain.interview.repository.InterviewRepository` | 본체 삭제 | persister |
| `com.rehearse.api.domain.interview.entity.Interview` | aggregate root | read / delete |
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | `question_set` 조회 + 삭제 | persister (cross-domain call) |
| `com.rehearse.api.domain.question.repository.QuestionSetAnalysisRepository` | `question_set_analysis` 삭제 | persister (cross-domain call) |
| `com.rehearse.api.domain.question.repository.QuestionAnswerRepository` | `question_answer` 삭제 (`deleteAllByInterviewId`) | persister (cross-domain call) |
| `com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository` | 세트 피드백 삭제 | persister (cross-domain call) |
| `com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository` | 타임스탬프 피드백 삭제 | persister (cross-domain call) |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/api/delete-interview.md` (interview 본체) + `docs/domain/question/schema.md` `question_answer` / `question_set` 섹션
- 명시 순서 사유: `InterviewDeletionService.deleteInterview:34` 코드 주석 (`하위 엔티티부터 명시적 삭제 (FK 제약조건 위반 방지)`)
- ❓TODO(사용자 확인 — Issue #407 / #404 추적):
  - `COMPLETED` interview 삭제 허용 여부 (`CANNOT_DELETE_COMPLETED` dead code 처리 — 제거 / 활성화)
  - S3 영상 객체 / Lambda 산출물 cascade cleanup 정책
  - 분석 진행 중 (`IN_PROGRESS`) 가드 도입 여부
