-- V40: DB 무결성 패치 (PK, UNIQUE, CASCADE FK, CHECK 제약 추가)

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 1: ElementCollection PK 추가
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE interview_interview_types
    ADD CONSTRAINT pk_interview_types PRIMARY KEY (interview_id, interview_type);

ALTER TABLE interview_cs_sub_topics
    ADD CONSTRAINT pk_cs_sub_topics PRIMARY KEY (interview_id, cs_sub_topic);

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 2: V4 FK에 ON DELETE CASCADE 재생성
-- DROP CONSTRAINT는 MySQL 8.0.19+에서 FK에 지원됨 (prod: 8.0.45 확인).
-- ─────────────────────────────────────────────────────────────────────────────

-- question_set → interview
ALTER TABLE question_set
    DROP CONSTRAINT fk_question_set_interview;
ALTER TABLE question_set
    ADD CONSTRAINT fk_question_set_interview
        FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE;

-- question → question_set
ALTER TABLE question
    DROP CONSTRAINT fk_question_question_set;
ALTER TABLE question
    ADD CONSTRAINT fk_question_question_set
        FOREIGN KEY (question_set_id) REFERENCES question_set(id) ON DELETE CASCADE;

-- question_set_answer → question
ALTER TABLE question_set_answer
    DROP CONSTRAINT fk_answer_question;
ALTER TABLE question_set_answer
    ADD CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE;

-- question_set_feedback → question_set
ALTER TABLE question_set_feedback
    DROP CONSTRAINT fk_qs_feedback_question_set;
ALTER TABLE question_set_feedback
    ADD CONSTRAINT fk_qs_feedback_question_set
        FOREIGN KEY (question_set_id) REFERENCES question_set(id) ON DELETE CASCADE;

-- timestamp_feedback → question_set_feedback
ALTER TABLE timestamp_feedback
    DROP CONSTRAINT fk_ts_feedback_qs_feedback;
ALTER TABLE timestamp_feedback
    ADD CONSTRAINT fk_ts_feedback_qs_feedback
        FOREIGN KEY (question_set_feedback_id) REFERENCES question_set_feedback(id) ON DELETE CASCADE;

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 3: question CHECK 5-way 강화
-- V35의 chk_question_track_meta를 DROP하고 5-way 정밀 제약으로 교체.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE question
    DROP CONSTRAINT chk_question_track_meta;

ALTER TABLE question
    ADD CONSTRAINT chk_question_track_meta_v2 CHECK (
        (question_type IN ('MAIN', 'FOLLOWUP')
            AND chain_id IS NULL AND chain_step_type IS NULL AND project_id IS NULL)
        OR (question_type = 'RESUME_OPENER'
            AND chain_id IS NULL AND chain_step_type IS NULL AND project_id IS NOT NULL)
        OR (question_type = 'RESUME_PLAYGROUND'
            AND chain_id IS NULL AND project_id IS NOT NULL)
        OR (question_type = 'RESUME_INTERROGATION'
            AND chain_id IS NOT NULL AND chain_step_type IS NOT NULL AND project_id IS NOT NULL)
        OR (question_type = 'RESUME_WRAP_UP'
            AND chain_id IS NULL AND chain_step_type IS NULL AND project_id IS NULL)
    );

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 4: timestamp_feedback level 컬럼 CHECK
-- TimestampFeedback 엔티티 기준: eyeContactLevel, postureLevel, toneConfidenceLevel
-- 각각 VARCHAR(20), 값 도메인: GOOD / AVERAGE / NEEDS_IMPROVEMENT
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE timestamp_feedback
    ADD CONSTRAINT chk_eye_contact_level CHECK (
        eye_contact_level IS NULL
        OR eye_contact_level IN ('GOOD', 'AVERAGE', 'NEEDS_IMPROVEMENT')
    );

ALTER TABLE timestamp_feedback
    ADD CONSTRAINT chk_posture_level CHECK (
        posture_level IS NULL
        OR posture_level IN ('GOOD', 'AVERAGE', 'NEEDS_IMPROVEMENT')
    );

ALTER TABLE timestamp_feedback
    ADD CONSTRAINT chk_tone_confidence_level CHECK (
        tone_confidence_level IS NULL
        OR tone_confidence_level IN ('GOOD', 'AVERAGE', 'NEEDS_IMPROVEMENT')
    );
