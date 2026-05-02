ALTER TABLE question
    DROP CONSTRAINT IF EXISTS chk_question_track_meta;

DROP INDEX IF EXISTS idx_question_chain_id ON question;

ALTER TABLE question
    DROP COLUMN IF EXISTS chain_id,
    DROP COLUMN IF EXISTS chain_step_type,
    DROP COLUMN IF EXISTS project_id;
