# API: 면접 세션 삭제

> Endpoint: `DELETE /api/v1/interviews/{id}`
> Action: 사용자가 자기가 생성한 면접 세션을 삭제한다 (자식 엔티티 일괄 cascade 정리).
> 관련 테이블: `interview` (write — DELETE) / `question_set` / `question` / `question_answer` / `timestamp_feedback` / `question_set_feedback` / `question_set_analysis` (write — DELETE) / `interview_interview_types` / `interview_cs_sub_topics` (CASCADE)
> 관련 외부 의존: 없음

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| path | `id` | Long | required | 면접 PK |

---

## 출력 (200)

본문 없음 (`ApiResponse.ok(null)`).

## 출력 (4xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 401 | — | JWT 부재 / 만료 |
| 403 | `INTERVIEW_008 FORBIDDEN` | 본인 소유 아님 |
| 404 | `INTERVIEW_001 NOT_FOUND` | 면접 ID 없음 |
| 400 | `INTERVIEW_009 CANNOT_DELETE_COMPLETED` | ⚠️ 현재 코드에서 던지지 않음 (정책-코드 갭, Issue #404 #1 — 차단 정책 도입 예정) |

---

## 흐름

### 1. 조회 + 권한
- `InterviewFinder.findById(id)` → `validateOwner(userId)` (403)

### 2. 사전 조건 (현재 코드)
- 별도 status 검증 없음. **모든 status (`READY` / `IN_PROGRESS` / `COMPLETED`) 삭제 가능**.
- 정책: **차단해야 함** (`InterviewStatus.isDeletable()` 가 항상 true 반환은 갭).
- ⚠️ **정책-코드 갭** (추적: #404 #1): A안 채택 = `COMPLETED` 면접 삭제 차단 도입 예정.

### 3. 자식 엔티티 명시 삭제 (FK 제약 위반 방지)
순서 중요 (FK 의존성 역순):
1. `questionAnswerRepository.deleteAllByInterviewId(id)` — 답변
2. `timestampFeedbackRepository.deleteAllByInterviewId(id)` — 타임스탬프 피드백
3. `questionSetFeedbackRepository.deleteAllByInterviewId(id)` — 질문세트 피드백
4. `questionSetAnalysisRepository.deleteAllByInterviewId(id)` — 분석 결과
5. `questionSetRepository.deleteAll(findByInterviewIdOrderByOrderIndex(id))` — 질문세트 (질문도 V41 CASCADE 로 따라감)

> Note: V41 이 `question_set → interview` / `question → question_set` / `question_answer → question` / `question_set_feedback → question_set` / `timestamp_feedback → question_set_feedback` 에 ON DELETE CASCADE 추가. 그럼에도 명시 삭제하는 이유 = `question_set_analysis` 등 일부 자식이 cascade 미적용 + JPA 영속성 컨텍스트 정리 + 명시적 삭제 의도 표현.

### 4. 본 행 삭제
- `interviewRepository.delete(interview)` — `interview` row 삭제
- ON DELETE CASCADE 로 `interview_interview_types`, `interview_cs_sub_topics` 자동 정리 (V41)

### 5. 응답
- 200, 본문 없음

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| `COMPLETED` 면접 삭제 시도 | 현재 허용 (갭). 정책 채택 후 `INTERVIEW_009 (400)` 반환 예정 (#404 #1) |
| 동시 DELETE 호출 | 무락. 둘 다 진행 시 한 쪽 `OptimisticLockException` / `EmptyResultDataAccessException` 가능 — 락 정책 ❓TODO |
| 자식 삭제 중 일부 실패 | 트랜잭션 롤백 → 면접 row 보존 |
| `InterviewRuntimeState` Caffeine 캐시 잔존 | 명시적 evict 없음 (TTL 만료까지 보유). 동일 ID 재생성 사례는 거의 없음 (id 는 AUTO_INCREMENT) |
| feedback / score / runtime state 외 자식 (예: NonverbalScore, RubricScore) | V41 CASCADE 또는 별도 cleanup 필요 — ⚠️ 정책-코드 갭 가능성 (V33 nonverbal_score 의 cascade 미확인) |

---

## 트랜잭션 노트

- `InterviewDeletionService.deleteInterview` = `@Transactional` (write) 단일 트랜잭션
- 자식 삭제 + 본 행 삭제 모두 same TX → 부분 실패 시 롤백
- `default_batch_fetch_size: 100` 적용 (delete 루프 시 N+1 감소)

---

## 관찰성

- **로그**: `InterviewDeletionService` — `면접 세션 삭제: id={}, userId={}` (INFO)
- **메트릭**: 직접 발행 없음.
- **알람**: 별도 없음.

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | 질문세트 정리 | calls |
| `com.rehearse.api.domain.question.repository.QuestionSetAnalysisRepository` | 분석 결과 정리 | calls |
| `com.rehearse.api.domain.question.repository.QuestionAnswerRepository` | 답변 정리 | calls |
| `com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository` | 타임스탬프 피드백 정리 | calls |
| `com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository` | 질문세트 피드백 정리 | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (삭제 정책 + isDeletable 갭)
- 임계값 / 코드 인용:
  - `InterviewStatus.isDeletable() == true` (`InterviewStatus.java:17`) — 현재 코드 (갭)
  - V41 CASCADE FK (question_set / question / question_answer / question_set_feedback / timestamp_feedback / interview_interview_types / interview_cs_sub_topics)
- ⚠️ **정책-코드 갭** (Issue #404 #1):
  - `COMPLETED` 면접 삭제 차단 정책 vs `isDeletable()` 항상 true → A안 채택 (정책: 차단)
- ❓TODO(사용자 확인) — Issue #404 비스코프 (보류):
  - soft-delete 도입 여부 (감사 / 복구)
  - 동시 DELETE 락 정책
