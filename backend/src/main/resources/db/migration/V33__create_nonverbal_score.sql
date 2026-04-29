CREATE TABLE nonverbal_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  turn_id BIGINT NOT NULL,
  d11_fluency TINYINT,
  d12_tone TINYINT,
  d13_posture TINYINT,
  d14_composure TINYINT,
  raw_signals JSON NOT NULL,
  context_multiplier DECIMAL(3,2),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_nonverbal_score_interview_turn (interview_id, turn_id),
  INDEX idx_nonverbal_score_interview (interview_id),
  CONSTRAINT fk_nonverbal_score_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
