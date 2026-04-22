CREATE TABLE rubric_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  turn_id BIGINT NOT NULL,
  rubric_id VARCHAR(64) NOT NULL,
  scores_json JSON NOT NULL,
  level_flag VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_interview_turn (interview_id, turn_id),
  INDEX idx_rubric_interview (rubric_id, interview_id),
  CONSTRAINT fk_rubric_score_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
