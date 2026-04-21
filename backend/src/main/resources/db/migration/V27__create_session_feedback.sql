CREATE TABLE session_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL UNIQUE,
  overall_json JSON NOT NULL,
  strengths_json JSON NOT NULL,
  gaps_json JSON NOT NULL,
  delivery_json JSON,
  week_plan_json JSON NOT NULL,
  synthesizer_model VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_session_feedback_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
