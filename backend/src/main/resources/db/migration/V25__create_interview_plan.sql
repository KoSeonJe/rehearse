CREATE TABLE interview_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL UNIQUE,
  plan_json JSON NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_interview_plan_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
