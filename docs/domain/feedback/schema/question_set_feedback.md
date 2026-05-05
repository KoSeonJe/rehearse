# feedback 스키마 — question_set_feedback

> 대상 마이그레이션: `V4__init_question_set.sql`, `V41__feedback_integrity_patch.sql`

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `question_set_feedback` | QuestionSet 1회 분석 결과 헤더 | `timestamp_feedback` N |

---

## question_set_feedback

### 성격
QuestionSet (= 질문 세트 1회 녹화 단위) 의 분석 결과 헤더. row 1개 = QuestionSet 1개 분석 결과 1건. QuestionSet 과 1:1 (UNIQUE).

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_set_id` | BIGINT | FK → question_set.id, UNIQUE, NOT NULL, ON DELETE CASCADE | 분석 대상 세트 |
| `question_set_comment` | TEXT | NULL | 세트 단위 종합 코멘트 (Lambda 생성) |
| `created_at` | DATETIME(6) | NOT NULL | 분석 결과 생성 시각 |
| `updated_at` | DATETIME(6) | NOT NULL | 마지막 갱신 |

### 인덱스
- `uk_question_set_feedback_question_set_id` (`question_set_id`) — UNIQUE — QuestionSet:Feedback = 1:1 강제

### 불변 / 정책
- INSERT 후 `question_set_id` 변경 금지 (소유 세트 불변)
- QuestionSet 삭제 시 CASCADE 로 동반 삭제 (V41)
- 자식 `timestamp_feedback` 은 cascade ALL + orphanRemoval (`QuestionSetFeedback.timestampFeedbacks`)
- `addTimestampFeedback()` rich-domain 메서드로만 자식 추가 (양방향 동기화)

### 마이그레이션 히스토리
- `V4__init_question_set.sql` — 최초 생성
- `V41__feedback_integrity_patch.sql` — FK ON DELETE CASCADE 보강

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.questionset.entity.QuestionSet` | 세트 본체 | 1:1 owner — `question_set_id` FK |
| `com.rehearse.api.domain.questionset.service.QuestionSetService#getFeedback` | 응답 조립 | called-by — feedback + score 합성 |
| `com.rehearse.api.domain.feedback.service.QuestionSetFeedbackPersister` | 저장 오케스트레이터 | persister |
| `com.rehearse.api.domain.interview.service.InterviewDeletionService` | 인터뷰 삭제 | called-by — FK 순서 명시 삭제 |
