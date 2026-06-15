ALTER TABLE timestamp_feedback
    ADD COLUMN verbal_comment TEXT,
    ADD COLUMN accuracy_issues TEXT,
    ADD COLUMN coaching_structure VARCHAR(500),
    ADD COLUMN coaching_improvement VARCHAR(500);
