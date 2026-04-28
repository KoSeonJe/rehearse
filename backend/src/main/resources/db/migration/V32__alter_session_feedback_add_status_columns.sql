ALTER TABLE session_feedback
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PRELIMINARY' AFTER interview_id,
    ADD COLUMN coverage VARCHAR(64) NULL AFTER week_plan_json,
    ADD COLUMN delivery_retryable BOOLEAN NOT NULL DEFAULT TRUE AFTER coverage,
    ADD COLUMN last_failure_reason VARCHAR(64) NULL AFTER delivery_retryable,
    ADD COLUMN retry_attempts INT NOT NULL DEFAULT 0 AFTER last_failure_reason,
    ADD COLUMN retry_started_at DATETIME NULL AFTER retry_attempts,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER retry_started_at,
    ADD COLUMN updated_at DATETIME NULL AFTER created_at;

UPDATE session_feedback SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE session_feedback
    MODIFY COLUMN updated_at DATETIME NOT NULL,
    MODIFY COLUMN overall_json JSON NULL,
    MODIFY COLUMN strengths_json JSON NULL,
    MODIFY COLUMN gaps_json JSON NULL,
    MODIFY COLUMN week_plan_json JSON NULL,
    DROP COLUMN synthesizer_model;
