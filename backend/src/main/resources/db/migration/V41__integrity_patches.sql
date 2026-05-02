-- V41: DB 무결성 패치 (PK, UNIQUE, CASCADE FK, CHECK 제약 추가)

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 0: ElementCollection / cache_key 중복 제거 (PK·UNIQUE 추가 전제)
-- Why: PK/UNIQUE 부재 상태에서 누적된 중복 행을 정리. truncate-replace 방식으로 멱등 보장.
-- ─────────────────────────────────────────────────────────────────────────────

-- interview_interview_types 중복 제거 (확인된 중복: interview_id 5·7 각 2건)
CREATE TEMPORARY TABLE _iit_dedup AS
    SELECT DISTINCT interview_id, interview_type FROM interview_interview_types;
TRUNCATE TABLE interview_interview_types;
INSERT INTO interview_interview_types (interview_id, interview_type)
    SELECT interview_id, interview_type FROM _iit_dedup;
DROP TEMPORARY TABLE _iit_dedup;

-- interview_cs_sub_topics 중복 제거 (예방적 적용, 현재 중복 없음 확인됨)
CREATE TEMPORARY TABLE _ics_dedup AS
    SELECT DISTINCT interview_id, cs_sub_topic
    FROM interview_cs_sub_topics
    WHERE cs_sub_topic IS NOT NULL;
TRUNCATE TABLE interview_cs_sub_topics;
INSERT INTO interview_cs_sub_topics (interview_id, cs_sub_topic)
    SELECT interview_id, cs_sub_topic FROM _ics_dedup;
DROP TEMPORARY TABLE _ics_dedup;

-- question_pool cache_key 중복 제거: 각 cache_key 당 MAX(id) 행만 보존
-- Why: UNIQUE 제약 추가 전, 동일 cache_key 중복 행(dev: 30~35건/key) 제거
DELETE FROM question_pool
WHERE id NOT IN (
    SELECT max_id FROM (
        SELECT MAX(id) AS max_id FROM question_pool GROUP BY cache_key
    ) AS _qp_keep
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 1: ElementCollection PK 추가
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE interview_interview_types
    ADD PRIMARY KEY (interview_id, interview_type);

-- cs_sub_topic 은 V1에서 NULL 허용으로 생성됨 → PK 전 NOT NULL 강제
ALTER TABLE interview_cs_sub_topics
    MODIFY cs_sub_topic VARCHAR(50) NOT NULL;

ALTER TABLE interview_cs_sub_topics
    ADD PRIMARY KEY (interview_id, cs_sub_topic);

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 2: question_pool.cache_key UNIQUE 제약
-- V11에서 INDEX만 생성됨, UNIQUE 누락
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE question_pool
    ADD CONSTRAINT uq_question_pool_cache_key UNIQUE (cache_key);

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 3: V4 FK에 ON DELETE CASCADE 재생성
-- ─────────────────────────────────────────────────────────────────────────────

-- question_set → interview
ALTER TABLE question_set
    DROP FOREIGN KEY fk_question_set_interview;
ALTER TABLE question_set
    ADD CONSTRAINT fk_question_set_interview
        FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE;

-- question → question_set
ALTER TABLE question
    DROP FOREIGN KEY fk_question_question_set;
ALTER TABLE question
    ADD CONSTRAINT fk_question_question_set
        FOREIGN KEY (question_set_id) REFERENCES question_set(id) ON DELETE CASCADE;

-- question_answer → question (V5에서 question_set_answer → question_answer로 rename됨)
ALTER TABLE question_answer
    DROP FOREIGN KEY fk_answer_question;
ALTER TABLE question_answer
    ADD CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE;

-- question_set_feedback → question_set
ALTER TABLE question_set_feedback
    DROP FOREIGN KEY fk_qs_feedback_question_set;
ALTER TABLE question_set_feedback
    ADD CONSTRAINT fk_qs_feedback_question_set
        FOREIGN KEY (question_set_id) REFERENCES question_set(id) ON DELETE CASCADE;

-- timestamp_feedback → question_set_feedback
ALTER TABLE timestamp_feedback
    DROP FOREIGN KEY fk_ts_feedback_qs_feedback;
ALTER TABLE timestamp_feedback
    ADD CONSTRAINT fk_ts_feedback_qs_feedback
        FOREIGN KEY (question_set_feedback_id) REFERENCES question_set_feedback(id) ON DELETE CASCADE;

-- interview_interview_types → interview (ElementCollection, V1에서 CASCADE 누락)
ALTER TABLE interview_interview_types
    DROP FOREIGN KEY fk_types_interview;
ALTER TABLE interview_interview_types
    ADD CONSTRAINT fk_types_interview
        FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE;

-- interview_cs_sub_topics → interview (ElementCollection, V1에서 CASCADE 누락)
ALTER TABLE interview_cs_sub_topics
    DROP FOREIGN KEY fk_cs_topics_interview;
ALTER TABLE interview_cs_sub_topics
    ADD CONSTRAINT fk_cs_topics_interview
        FOREIGN KEY (interview_id) REFERENCES interview(id) ON DELETE CASCADE;

-- ─────────────────────────────────────────────────────────────────────────────
-- 블록 4: question CHECK 5-way 강화
-- V35의 chk_question_track_meta를 DROP하고 5-way 정밀 제약으로 교체.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE question
    DROP CHECK chk_question_track_meta;

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
-- 블록 5: timestamp_feedback level 컬럼 CHECK
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
