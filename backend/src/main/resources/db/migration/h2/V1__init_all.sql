-- H2 로컬 개발용 통합 스키마 (MySQL 마이그레이션 전체를 H2 호환 문법으로 통합)

-- interview
CREATE TABLE interview (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id                   VARCHAR(36) NOT NULL,
    position                    VARCHAR(20) NOT NULL,
    position_detail             VARCHAR(100),
    level                       VARCHAR(20) NOT NULL,
    duration_minutes            INT NOT NULL,
    status                      VARCHAR(20) NOT NULL,
    question_generation_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason              TEXT,
    tech_stack                  VARCHAR(30),
    overall_score               INT,
    overall_comment             TEXT,
    created_at                  DATETIME(6) NOT NULL,
    updated_at                  DATETIME(6) NOT NULL,
    CONSTRAINT uq_interview_public_id UNIQUE (public_id)
);

CREATE INDEX idx_interview_public_id ON interview(public_id);

CREATE TABLE interview_interview_types (
    interview_id    BIGINT NOT NULL,
    interview_type  VARCHAR(30) NOT NULL,
    CONSTRAINT fk_types_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
);

CREATE INDEX idx_types_interview_id ON interview_interview_types(interview_id);

CREATE TABLE interview_cs_sub_topics (
    interview_id    BIGINT NOT NULL,
    cs_sub_topic    VARCHAR(50),
    CONSTRAINT fk_cs_topics_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
);

CREATE INDEX idx_cs_topics_interview_id ON interview_cs_sub_topics(interview_id);

-- file_metadata
CREATE TABLE file_metadata (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_type           VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    s3_key              VARCHAR(500) NOT NULL,
    streaming_s3_key    VARCHAR(500),
    bucket              VARCHAR(100),
    content_type        VARCHAR(100),
    file_size_bytes     BIGINT,
    failure_reason      VARCHAR(500),
    failure_detail      TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    CONSTRAINT uq_file_metadata_s3_key UNIQUE (s3_key)
);

CREATE INDEX idx_file_metadata_status_updated ON file_metadata(status, updated_at);

-- question_set
CREATE TABLE question_set (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id        BIGINT NOT NULL,
    category            VARCHAR(20) NOT NULL,
    order_index         INT NOT NULL,
    file_metadata_id    BIGINT,
    analysis_status     VARCHAR(30) NOT NULL,
    analysis_progress   VARCHAR(30),
    failure_reason      VARCHAR(500),
    failure_detail      TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    CONSTRAINT fk_question_set_interview FOREIGN KEY (interview_id) REFERENCES interview(id),
    CONSTRAINT fk_question_set_file_metadata FOREIGN KEY (file_metadata_id) REFERENCES file_metadata(id),
    CONSTRAINT uq_question_set_interview_order UNIQUE (interview_id, order_index)
);

CREATE INDEX idx_question_set_interview_id ON question_set(interview_id);
CREATE INDEX idx_question_set_status_updated ON question_set(analysis_status, updated_at);

-- question_pool
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
);

CREATE INDEX idx_qp_cache_key_active ON question_pool(cache_key, is_active);
CREATE INDEX idx_qp_created_at ON question_pool(created_at);

-- question
CREATE TABLE question (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    question_type   VARCHAR(20) NOT NULL,
    question_text   TEXT NOT NULL,
    model_answer    TEXT,
    reference_type  VARCHAR(20),
    order_index     INT NOT NULL,
    question_pool_id BIGINT,
    CONSTRAINT fk_question_question_set FOREIGN KEY (question_set_id) REFERENCES question_set(id),
    CONSTRAINT fk_question_pool FOREIGN KEY (question_pool_id) REFERENCES question_pool(id) ON DELETE SET NULL,
    CONSTRAINT uq_question_question_set_order UNIQUE (question_set_id, order_index)
);

CREATE INDEX idx_question_question_set_id ON question(question_set_id);

-- question_answer (originally question_set_answer, renamed in V5)
CREATE TABLE question_answer (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    start_ms    BIGINT NOT NULL,
    end_ms      BIGINT NOT NULL,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX idx_answer_question_id ON question_answer(question_id);

-- question_set_feedback
CREATE TABLE question_set_feedback (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_id     BIGINT NOT NULL,
    question_set_score  INT NOT NULL,
    question_set_comment TEXT NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    CONSTRAINT fk_qs_feedback_question_set FOREIGN KEY (question_set_id) REFERENCES question_set(id),
    CONSTRAINT uq_qs_feedback_question_set UNIQUE (question_set_id)
);

-- timestamp_feedback
CREATE TABLE timestamp_feedback (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_feedback_id BIGINT NOT NULL,
    question_id             BIGINT,
    answer_type             VARCHAR(20),
    start_ms                BIGINT NOT NULL,
    end_ms                  BIGINT NOT NULL,
    transcript              TEXT,
    verbal_score            INT,
    verbal_comment          TEXT,
    filler_word_count       INT,
    eye_contact_score       INT,
    posture_score           INT,
    expression_label        VARCHAR(50),
    nonverbal_comment       TEXT,
    overall_comment         TEXT,
    is_analyzed             BOOLEAN NOT NULL DEFAULT FALSE,
    filler_words            TEXT,
    speech_pace             VARCHAR(10),
    tone_confidence         INT,
    emotion_label           VARCHAR(20),
    vocal_comment           TEXT,
    CONSTRAINT fk_ts_feedback_qs_feedback FOREIGN KEY (question_set_feedback_id) REFERENCES question_set_feedback(id),
    CONSTRAINT fk_ts_feedback_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX idx_ts_feedback_qs_feedback_id ON timestamp_feedback(question_set_feedback_id);
CREATE INDEX idx_ts_feedback_question_id ON timestamp_feedback(question_id);

-- users
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    profile_image   VARCHAR(500),
    provider        VARCHAR(20) NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_provider_provider_id UNIQUE (provider, provider_id)
);
