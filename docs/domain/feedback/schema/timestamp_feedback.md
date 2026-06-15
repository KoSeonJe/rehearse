# feedback 스키마 — timestamp_feedback

> 대상 마이그레이션: `V4`, `V5`, `V13`, `V16`, `V18`, `V34`, `V41`, `V54`

## 테이블 목록

| 테이블 | 성격 | 1:N 관계 |
|--------|------|---------|
| `timestamp_feedback` | 영상 타임스탬프 구간 1개 = 답변 1턴 피드백 | — (자식 없음, ReviewBookmark 가 참조) |

---

## timestamp_feedback

### 성격
QuestionSetFeedback 산하 1턴 (= 질문 1개 답변 1개) 단위 피드백. row 1개 = QuestionSet 의 한 질문 응답 구간 1개. 영상 타임스탬프 (start_ms / end_ms) 와 묶여 비언어/음성/내용 코멘트를 보유.

### 컬럼

| 컬럼 | 타입 | 제약 | 성격 |
|------|------|-----|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 식별자 |
| `question_set_feedback_id` | BIGINT | FK → question_set_feedback.id, NOT NULL, ON DELETE CASCADE | 부모 세트 피드백 |
| `question_id` | BIGINT | FK → question.id, NULL 허용 | 매핑된 질문 (resolve 실패 시 null) |
| `start_ms` | INT | NOT NULL | 구간 시작 (ms) |
| `end_ms` | INT | NOT NULL | 구간 종료 (ms) |
| `transcript` | TEXT | NULL | STT 텍스트 |
| `filler_word_count` | INT | NULL | 필러 단어 횟수 |
| `eye_contact_level` | VARCHAR(20) | CHECK ∈ {GOOD, AVERAGE, NEEDS_IMPROVEMENT} | 시선 |
| `posture_level` | VARCHAR(20) | CHECK ∈ {GOOD, AVERAGE, NEEDS_IMPROVEMENT} | 자세 |
| `tone_confidence_level` | VARCHAR(20) | CHECK ∈ {GOOD, AVERAGE, NEEDS_IMPROVEMENT} | 음성 자신감 |
| `expression_label` | VARCHAR(40) | NULL | 표정 라벨 |
| `nonverbal_comment` | TEXT (JSON `CommentBlock`) | NULL | 비언어 코멘트 |
| `overall_comment` | TEXT (JSON `CommentBlock`) | NULL | 종합 코멘트 |
| `vocal_comment` | TEXT (JSON `CommentBlock`) | NULL | 음성 코멘트 |
| `attitude_comment` | TEXT | NULL | 태도 코멘트 (V18) |
| `verbal_comment` | TEXT | NULL | 언어 코멘트 (V34 제거 → V54 재추가) |
| `accuracy_issues` | TEXT | NULL | 정확도 이슈 (V34 제거 → V54 재추가) |
| `coaching_structure` | VARCHAR(500) | NULL | 코칭 구조 (V34 제거 → V54 재추가) |
| `coaching_improvement` | VARCHAR(500) | NULL | 코칭 개선 방향 (V34 제거 → V54 재추가) |
| `filler_words` | TEXT (JSON array) | NULL | 필러 단어 목록 |
| `speech_pace` | VARCHAR(10) | NULL | 발화 속도 라벨 |
| `emotion_label` | VARCHAR(20) | NULL | 감정 라벨 |
| `is_analyzed` | BOOLEAN | NOT NULL DEFAULT false | 분석 완료 플래그 (mapper 가 true 세팅) |
| `created_at` | DATETIME(6) | NOT NULL | — |
| `updated_at` | DATETIME(6) | NOT NULL | — |

### 인덱스
- FK index 자동 — `question_set_feedback_id`, `question_id`

### 불변 / 정책
- 부모 세트 피드백 삭제 시 CASCADE
- `question_id` resolve 실패 시 null 저장 + WARN 로그 (`TimestampFeedbackBatch`) — soft fail
- level 컬럼 3종은 V41 CHECK 제약. 신규 enum 값 추가 시 마이그레이션 동반 필수
- comment 계열은 `CommentBlock` JSON 직렬화 후 TEXT 저장 (mapper 책임)
- ReviewBookmark (`domain/reviewbookmark`) 가 본 row 참조 — 직접 삭제 시 cascade/orphan 정책 검토

### 마이그레이션 히스토리
- `V4__init_question_set.sql` — 최초 생성
- `V5__rename_answer_table_and_align_question_types.sql` — `question_id` FK 추가
- `V13__add_vocal_columns_to_timestamp_feedback.sql` — filler_words / speech_pace / tone_confidence / emotion_label / vocal_comment
- `V16__rubric_perspective_levels.sql` — eye/posture/tone level VARCHAR 화 + 점수 컬럼 제거
- `V18__add_attitude_comment.sql` — attitude_comment
- `V34__drop_legacy_comment_columns.sql` — verbal_comment / accuracy_issues / coaching_* 제거 (V54 에서 재추가)
- `V41__feedback_integrity_patch.sql` — ON DELETE CASCADE + level CHECK 제약
- `V54__rollback_score_system.sql` — verbal_comment / accuracy_issues / coaching_structure / coaching_improvement 재추가 (Content/Delivery 2탭 롤백)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.feedback.entity.QuestionSetFeedback` | 부모 헤더 | persister |
| `com.rehearse.api.domain.feedback.mapper.TimestampFeedbackMapper` | DTO→Entity + JSON 직렬화 | persister |
| `com.rehearse.api.domain.feedback.service.TimestampFeedbackBatch` | Question resolve + null 허용 | persister |
| `com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark` | 북마크 | called-by — cross-domain consumer |
| `com.rehearse.api.domain.question.service.QuestionSetService#getFeedback` | 응답 조립 | called-by |
| `com.rehearse.api.domain.interview.service.InterviewDeletionService` | 명시 삭제 순서 | called-by |
