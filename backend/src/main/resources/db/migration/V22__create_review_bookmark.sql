CREATE TABLE review_bookmark (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT   NOT NULL,
    timestamp_feedback_id  BIGINT   NOT NULL,
    resolved_at            DATETIME NULL,
    created_at             DATETIME NOT NULL,
    CONSTRAINT fk_rb_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rb_tsf  FOREIGN KEY (timestamp_feedback_id)
        REFERENCES timestamp_feedback(id) ON DELETE CASCADE,
    CONSTRAINT uk_rb_user_tsf UNIQUE (user_id, timestamp_feedback_id)
);

CREATE INDEX idx_rb_user_created ON review_bookmark (user_id, created_at DESC);
