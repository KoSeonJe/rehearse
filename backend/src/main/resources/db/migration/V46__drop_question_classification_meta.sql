ALTER TABLE question
    DROP COLUMN reference_type,
    DROP COLUMN feedback_perspective;

ALTER TABLE question_pool
    DROP COLUMN reference_type;

ALTER TABLE question_score
    DROP COLUMN feedback_perspective;
