CREATE TABLE resume_skeleton (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  file_hash VARCHAR(64) NOT NULL,
  candidate_level VARCHAR(16) NOT NULL,
  target_domain VARCHAR(32),
  skeleton_json JSON NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_interview (interview_id),
  INDEX idx_file_hash (file_hash),
  CONSTRAINT fk_resume_skeleton_interview FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
