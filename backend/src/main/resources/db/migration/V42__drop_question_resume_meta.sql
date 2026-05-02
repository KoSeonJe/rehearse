ALTER TABLE question DROP CHECK chk_question_track_meta;

DROP INDEX idx_question_chain_id ON question;

ALTER TABLE question
    DROP COLUMN chain_id,
    DROP COLUMN chain_step_type,
    DROP COLUMN project_id;
