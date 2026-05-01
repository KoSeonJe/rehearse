ALTER TABLE question
    ADD COLUMN chain_id VARCHAR(128) NULL,
    ADD COLUMN chain_step_type VARCHAR(16) NULL,
    ADD COLUMN project_id VARCHAR(64) NULL;

ALTER TABLE question
    ADD CONSTRAINT chk_question_track_meta CHECK (
        (question_type IN ('MAIN', 'FOLLOWUP')
            AND chain_id IS NULL AND chain_step_type IS NULL AND project_id IS NULL)
        OR
        (question_type IN ('RESUME_OPENER', 'RESUME_PLAYGROUND', 'RESUME_INTERROGATION', 'RESUME_WRAP_UP'))
    );

CREATE INDEX idx_question_chain_id ON question (chain_id);
