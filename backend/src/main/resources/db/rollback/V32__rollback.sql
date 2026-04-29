ALTER TABLE session_feedback
    ADD COLUMN synthesizer_model VARCHAR(32) NOT NULL DEFAULT 'gpt-4o-mini',
    MODIFY COLUMN overall_json JSON NOT NULL,
    MODIFY COLUMN strengths_json JSON NOT NULL,
    MODIFY COLUMN gaps_json JSON NOT NULL,
    MODIFY COLUMN week_plan_json JSON NOT NULL,
    DROP COLUMN status,
    DROP COLUMN coverage,
    DROP COLUMN delivery_retryable,
    DROP COLUMN last_failure_reason,
    DROP COLUMN retry_attempts,
    DROP COLUMN updated_at;
