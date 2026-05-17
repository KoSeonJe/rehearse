ALTER TABLE question_score
    ADD CONSTRAINT uk_question_score_question_rubric UNIQUE (question_id, rubric_id);
