-- V11: 질문 Pool 캐싱 시스템 테이블 생성 + Question FK 추가

CREATE TABLE question_pool (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key           VARCHAR(255) NOT NULL,
    content             TEXT NOT NULL,
    category            VARCHAR(100),
    question_order      INT,
    evaluation_criteria TEXT NOT NULL,
    model_answer        TEXT,
    reference_type      VARCHAR(50),
    follow_up_strategy  VARCHAR(20) NOT NULL DEFAULT 'PREPARED',
    quality_score       DECIMAL(3,2) DEFAULT 1.00,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_qp_cache_key_active ON question_pool(cache_key, is_active);
CREATE INDEX idx_qp_created_at ON question_pool(created_at);

CREATE TABLE prepared_follow_up (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_pool_id    BIGINT NOT NULL,
    content             TEXT NOT NULL,
    model_answer        TEXT,
    match_keywords      TEXT,
    match_threshold     INT NOT NULL DEFAULT 2,
    display_order       INT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    CONSTRAINT fk_pfu_question_pool
        FOREIGN KEY (question_pool_id) REFERENCES question_pool(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pfu_question_pool_id ON prepared_follow_up(question_pool_id);

ALTER TABLE question ADD COLUMN question_pool_id BIGINT NULL;
ALTER TABLE question ADD CONSTRAINT fk_question_pool
    FOREIGN KEY (question_pool_id) REFERENCES question_pool(id) ON DELETE SET NULL;
