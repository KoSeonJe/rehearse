-- V4__add_question_set_and_file_metadata.sql
-- 질문세트 단위 녹화-분석-피드백 파이프라인 스키마

-- 1. file_metadata (S3 파일 라이프사이클)
CREATE TABLE file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    streaming_s3_key VARCHAR(500),
    bucket VARCHAR(100),
    content_type VARCHAR(100),
    file_size_bytes BIGINT,
    failure_reason VARCHAR(500),
    failure_detail TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_file_metadata_status_updated ON file_metadata(status, updated_at);
CREATE UNIQUE INDEX idx_file_metadata_s3_key ON file_metadata(s3_key);

-- 2. question_set (질문 세트 - 분석/녹화 단위)
CREATE TABLE question_set (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    order_index INT NOT NULL,
    file_metadata_id BIGINT,
    analysis_status VARCHAR(30) NOT NULL,
    analysis_progress VARCHAR(30),
    failure_reason VARCHAR(500),
    failure_detail TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_question_set_interview FOREIGN KEY (interview_id) REFERENCES interview(id),
    CONSTRAINT fk_question_set_file_metadata FOREIGN KEY (file_metadata_id) REFERENCES file_metadata(id),
    CONSTRAINT uq_question_set_interview_order UNIQUE (interview_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_question_set_interview_id ON question_set(interview_id);
CREATE INDEX idx_question_set_status_updated ON question_set(analysis_status, updated_at);

-- 3. question (개별 질문 + 모범답변)
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    model_answer TEXT,
    reference_type VARCHAR(20),
    order_index INT NOT NULL,
    CONSTRAINT fk_question_question_set FOREIGN KEY (question_set_id) REFERENCES question_set(id),
    CONSTRAINT uq_question_question_set_order UNIQUE (question_set_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_question_question_set_id ON question(question_set_id);

-- 4. question_set_answer (영상 내 답변 구간)
CREATE TABLE question_set_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    start_ms BIGINT NOT NULL,
    end_ms BIGINT NOT NULL,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_answer_question_id ON question_set_answer(question_id);

-- 5. question_set_feedback (세트 종합 피드백, 1:1)
CREATE TABLE question_set_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    question_set_score INT NOT NULL,
    question_set_comment TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_qs_feedback_question_set FOREIGN KEY (question_set_id) REFERENCES question_set(id),
    CONSTRAINT uq_qs_feedback_question_set UNIQUE (question_set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. timestamp_feedback (구간별 상세 피드백)
CREATE TABLE timestamp_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_feedback_id BIGINT NOT NULL,
    answer_type VARCHAR(20) NOT NULL,
    start_ms BIGINT NOT NULL,
    end_ms BIGINT NOT NULL,
    transcript TEXT,
    verbal_score INT,
    verbal_comment TEXT,
    filler_word_count INT,
    eye_contact_score INT,
    posture_score INT,
    expression_label VARCHAR(50),
    nonverbal_comment TEXT,
    overall_comment TEXT,
    is_analyzed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ts_feedback_qs_feedback FOREIGN KEY (question_set_feedback_id) REFERENCES question_set_feedback(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ts_feedback_qs_feedback_id ON timestamp_feedback(question_set_feedback_id);

-- 7. interview 테이블에 overall_score, overall_comment 추가
ALTER TABLE interview ADD COLUMN overall_score INT;
ALTER TABLE interview ADD COLUMN overall_comment TEXT;
