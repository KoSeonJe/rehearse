-- V42: question 테이블 resume 관련 컬럼 및 인덱스 제거
-- V41이 chk_question_track_meta를 DROP하고 chk_question_track_meta_v2로 교체함 → 신 이름으로 멱등 DROP

SET @chk_exists = (
    SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'chk_question_track_meta_v2'
);
SET @drop_chk = IF(@chk_exists > 0,
    'ALTER TABLE question DROP CHECK chk_question_track_meta_v2',
    'SELECT 1'
);
PREPARE stmt FROM @drop_chk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP INDEX idx_question_chain_id ON question;

ALTER TABLE question
    DROP COLUMN chain_id,
    DROP COLUMN chain_step_type,
    DROP COLUMN project_id;
