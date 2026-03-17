-- V1__init_schema.sql
-- Rehearse 초기 스키마 - ERD 기반 9개 테이블

-- 1. interview (면접 세션)
CREATE TABLE interview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    position VARCHAR(20) NOT NULL,
    position_detail VARCHAR(100),
    level VARCHAR(20) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. interview_question (면접 질문)
CREATE TABLE interview_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    evaluation_criteria TEXT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_question_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_question_interview_id ON interview_question(interview_id);

-- 3. interview_interview_types (면접 유형 - ElementCollection)
CREATE TABLE interview_interview_types (
    interview_id BIGINT NOT NULL,
    interview_type VARCHAR(30) NOT NULL,
    CONSTRAINT fk_types_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_types_interview_id ON interview_interview_types(interview_id);

-- 4. interview_cs_sub_topics (CS 세부 주제 - ElementCollection)
CREATE TABLE interview_cs_sub_topics (
    interview_id BIGINT NOT NULL,
    cs_sub_topic VARCHAR(50),
    CONSTRAINT fk_cs_topics_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cs_topics_interview_id ON interview_cs_sub_topics(interview_id);

-- 5. feedback (AI 피드백)
CREATE TABLE feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    timestamp_seconds DOUBLE NOT NULL,
    category VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    suggestion TEXT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_feedback_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_feedback_interview_id ON feedback(interview_id);

-- 6. interview_answer (면접 답변)
CREATE TABLE interview_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    question_index INT NOT NULL,
    question_content TEXT NOT NULL,
    answer_text TEXT NOT NULL,
    non_verbal_summary TEXT,
    voice_summary TEXT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_answer_interview FOREIGN KEY (interview_id) REFERENCES interview(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_answer_interview_id ON interview_answer(interview_id);

-- 7. interview_report (종합 리포트)
CREATE TABLE interview_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    overall_score INT NOT NULL,
    summary TEXT NOT NULL,
    feedback_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_report_interview FOREIGN KEY (interview_id) REFERENCES interview(id),
    CONSTRAINT uq_report_interview UNIQUE (interview_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. report_strengths (리포트 강점 - ElementCollection)
CREATE TABLE report_strengths (
    report_id BIGINT NOT NULL,
    strength TEXT NOT NULL,
    CONSTRAINT fk_strengths_report FOREIGN KEY (report_id) REFERENCES interview_report(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_strengths_report_id ON report_strengths(report_id);

-- 9. report_improvements (리포트 개선점 - ElementCollection)
CREATE TABLE report_improvements (
    report_id BIGINT NOT NULL,
    improvement TEXT NOT NULL,
    CONSTRAINT fk_improvements_report FOREIGN KEY (report_id) REFERENCES interview_report(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_improvements_report_id ON report_improvements(report_id);
