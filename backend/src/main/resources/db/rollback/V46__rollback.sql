-- 수동 실행 전용. 컬럼만 복원되며 데이터는 복원되지 않음 (Phase 1 부터 적재 중단).

ALTER TABLE question
    ADD COLUMN reference_type VARCHAR(20) NULL,
    ADD COLUMN feedback_perspective VARCHAR(20) NULL;

ALTER TABLE question_pool
    ADD COLUMN reference_type VARCHAR(50) NULL;

ALTER TABLE question_score
    ADD COLUMN feedback_perspective VARCHAR(32) NULL;
