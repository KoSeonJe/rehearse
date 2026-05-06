# feedback 스키마 — question_score_dimension

> 대상 마이그레이션: `V36__add_question_score_tables.sql`

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `question_score_dimension` | question_score 산하 dimension 별 점수 1개 | — |

---

## question_score_dimension

### 성격
question_score 1행 산하의 dimension (예: `clarity`, `depth`, `fluency`, `composure` 등 yaml 정의 식별자) 별 점수와 근거. row 1개 = (question_score, dimension_ref) 1쌍.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_score_id` | BIGINT | FK → question_score.id, NOT NULL, ON DELETE CASCADE | 부모 채점 |
| `dimension_ref` | VARCHAR(64) | NOT NULL | dimension 코드 (`_dimensions.yaml` 키) |
| `score` | INT | NULL | 점수 (rubric=1..3, nonverbal=루브릭별 정의) |
| `observation` | TEXT | NULL | 관찰 노트 |
| `evidence_quote` | TEXT | NULL | 근거 인용 (rubric scoring 강제 항목) |
| `created_at` / `updated_at` | DATETIME(6) | NOT NULL | — |

### 불변 / 정책
- `RubricScoringAdapter`: SCORE_MIN=1, SCORE_MAX=3 (out-of-range → null 저장)
- `RubricScoringAdapter`: `evidence_quote` 누락 시 1회 schema retry, 재실패 시 score 무효화 (NA fallback)
- 부모 question_score 삭제 시 CASCADE
- (질문, 루브릭, dimension) 유일성은 부모 unique + `dimension_ref` 어플리케이션 레벨로 보장 (DB UNIQUE 없음 — 중복 INSERT 시 어플리케이션 책임)

### 마이그레이션 히스토리
- `V36__add_question_score_tables.sql` — 신규 생성

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.score.entity.QuestionScore` | 부모 헤더 | persister |
| `com.rehearse.api.infra.ai.adapter.RubricScoringAdapter` | LLM 결과 → dimension 매핑 + 검증 | persister |
| `com.rehearse.api.domain.feedback.rubric.service.NonverbalScorePersister` | nonverbal 4개 dimension (fluency / confidence_tone / eye_contact_posture / composure) | persister |
