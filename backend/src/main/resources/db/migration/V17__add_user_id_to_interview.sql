ALTER TABLE interview ADD COLUMN user_id BIGINT NULL COMMENT '면접 소유자 (users.id)';

ALTER TABLE interview ADD CONSTRAINT fk_interview_user
    FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_interview_user_id ON interview(user_id);
