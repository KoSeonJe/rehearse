CREATE TABLE service_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    rating INT NULL,
    source VARCHAR(20) NOT NULL,
    completed_count_snapshot INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_feedback_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_service_feedback_user_id ON service_feedback(user_id);
CREATE INDEX idx_service_feedback_created_at ON service_feedback(created_at DESC);
