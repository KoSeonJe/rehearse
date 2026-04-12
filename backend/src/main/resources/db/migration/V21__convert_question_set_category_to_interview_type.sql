UPDATE question_set SET category = 'RESUME_BASED' WHERE category = 'RESUME';
UPDATE question_set SET category = 'CS_FUNDAMENTAL' WHERE category = 'CS';
UPDATE question_set SET category = 'CS_FUNDAMENTAL'
    WHERE category NOT IN ('RESUME_BASED', 'CS_FUNDAMENTAL');

ALTER TABLE question_set MODIFY COLUMN category VARCHAR(50) NOT NULL;
