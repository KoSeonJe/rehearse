# feedback 스키마 — question_score

> 대상 마이그레이션: `V36__add_question_score_tables.sql`, `V38__drop_legacy_score_tables.sql`, `V54__rollback_score_system.sql`

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `question_score` | 1턴(질문) × 1루브릭 채점 헤더 | `question_score_dimension` N |

---

## question_score

### 성격
질문 1개에 대한 루브릭 1개 채점 결과 헤더. row 1개 = (질문, 루브릭) 1쌍. 루브릭은 텍스트 식별자 (`gpt-4o-mini` 산출물 + `nonverbal` 가상 루브릭 포함).

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_id` | BIGINT | FK → question.id, NOT NULL | 채점 대상 질문 |
| `interview_id` | BIGINT | NOT NULL | 인터뷰 식별자 (조회 효율) |
| `rubric_id` | VARCHAR(64) | NOT NULL | 루브릭 ID (yaml 정의 또는 `nonverbal`) |
| `feedback_perspective` | VARCHAR(32) | NULL | TECHNICAL / BEHAVIORAL / EXPERIENCE |
| `level_flag` | VARCHAR(16) | NULL | 인터뷰 난이도 |
| `created_at` / `updated_at` | DATETIME(6) | NOT NULL | — |

### 인덱스
- `uk_question_score_question_rubric` (`question_id`, `rubric_id`) — UNIQUE — 동일 (질문, 루브릭) 중복 채점 방지 (idempotent 보장 근거)

### 불변 / 정책
- `QuestionScorePersister.saveRubric` / `saveNonverbal` 모두 `findByQuestionIdAndRubricId` pre-check 후 upsert — **idempotent**
- `rubric_id="nonverbal"` 행은 `QuestionSetService#getFeedback` 응답에서 제외 (별도 표현)
- TurnCompletedEvent intent 가 `CLARIFY_REQUEST` / `GIVE_UP` 인 경우 `Rubric.selectDimensions` 가 빈 dimensions 반환 → 본 행 미생성

### 마이그레이션 히스토리
- `V36__add_question_score_tables.sql` — 신규 생성
- `V38__drop_legacy_score_tables.sql` — 레거시 `rubric_score` / `nonverbal_score` 제거 (본 테이블이 대체)
- `V54__rollback_score_system.sql` — 본 테이블 DROP (score 시스템 롤백)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.score.service.QuestionScorePersister` | upsert | persister |
| `com.rehearse.api.domain.feedback.rubric.service.RubricScoringEventListener` | TurnCompletedEvent 비동기 처리 | called-by |
| `com.rehearse.api.domain.feedback.rubric.service.NonverbalScorePersister` | nonverbal rubric 저장 | called-by |
| `com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler` | session synthesis 입력 집계 | called-by |
| `com.rehearse.api.domain.question.service.QuestionSetService#getFeedback` | 응답 조립 | called-by |
