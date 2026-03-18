-- V5__rename_answer_and_update_feedback.sql
-- Phase 1: 엔티티 정리 (QuestionSetAnswer → QuestionAnswer, TimestampFeedback question FK 추가)

-- 1. question_set_answer → question_answer 테이블 이름 변경
RENAME TABLE question_set_answer TO question_answer;

-- 2. timestamp_feedback: question_id 컬럼 추가
ALTER TABLE timestamp_feedback ADD COLUMN question_id BIGINT;
ALTER TABLE timestamp_feedback ADD CONSTRAINT fk_ts_feedback_question
    FOREIGN KEY (question_id) REFERENCES question(id);
CREATE INDEX idx_ts_feedback_question_id ON timestamp_feedback(question_id);

-- 3. timestamp_feedback: answer_type NOT NULL 제약 해제 (점진적 제거)
ALTER TABLE timestamp_feedback MODIFY answer_type VARCHAR(20) NULL;

-- 4. question: 기존 FOLLOWUP_1/2/3 → FOLLOWUP 마이그레이션
UPDATE question SET question_type = 'FOLLOWUP'
    WHERE question_type IN ('FOLLOWUP_1', 'FOLLOWUP_2', 'FOLLOWUP_3');
