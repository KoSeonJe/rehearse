ALTER TABLE rubric_score
    ADD CONSTRAINT uk_rubric_interview_turn UNIQUE (interview_id, turn_id);
