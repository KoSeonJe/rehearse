ALTER TABLE interview_plan
    ADD COLUMN session_plan_id VARCHAR(255) NOT NULL AFTER interview_id,
    ADD COLUMN duration_hint_min INT NOT NULL AFTER session_plan_id,
    ADD COLUMN total_projects INT NOT NULL AFTER duration_hint_min;
