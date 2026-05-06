ALTER TABLE interview
  ADD COLUMN question_gen_retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN question_gen_last_retried_at DATETIME(6) NULL;
