CREATE TABLE question_score (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    interview_id BIGINT NOT NULL,
    rubric_id VARCHAR(64) NOT NULL,
    feedback_perspective VARCHAR(32) NULL,
    level_flag VARCHAR(16) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_score_question FOREIGN KEY (question_id) REFERENCES question (id) ON DELETE CASCADE,
    CONSTRAINT fk_question_score_interview FOREIGN KEY (interview_id) REFERENCES interview (id) ON DELETE CASCADE,
    CONSTRAINT uq_question_score UNIQUE (question_id, rubric_id)
);

CREATE TABLE question_score_dimension (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_score_id BIGINT NOT NULL,
    dimension_ref VARCHAR(64) NOT NULL,
    score INT NULL,
    observation TEXT NULL,
    evidence_quote TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qsd_question_score FOREIGN KEY (question_score_id) REFERENCES question_score (id) ON DELETE CASCADE,
    CONSTRAINT uq_qsd_question_score_dimension UNIQUE (question_score_id, dimension_ref)
);
