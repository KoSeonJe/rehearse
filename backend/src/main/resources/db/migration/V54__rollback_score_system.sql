-- 1. develop 루브릭 점수 테이블 제거 (child first)
DROP TABLE IF EXISTS question_score_dimension;
DROP TABLE IF EXISTS question_score;

-- 2. main Content/Delivery 가 읽는 timestamp_feedback 컬럼 재추가 (V34 + V48 이 드롭)
ALTER TABLE timestamp_feedback
    ADD COLUMN verbal_comment        TEXT,
    ADD COLUMN accuracy_issues       TEXT,
    ADD COLUMN coaching_structure    VARCHAR(500),
    ADD COLUMN coaching_improvement  VARCHAR(500),
    ADD COLUMN nonverbal_comment     TEXT,
    ADD COLUMN overall_comment       TEXT,
    ADD COLUMN vocal_comment         TEXT,
    ADD COLUMN attitude_comment      TEXT,
    ADD COLUMN speech_pace           VARCHAR(10),
    ADD COLUMN tone_confidence_level VARCHAR(20),
    ADD COLUMN emotion_label         VARCHAR(20),
    ADD COLUMN eye_contact_level     VARCHAR(20),
    ADD COLUMN posture_level         VARCHAR(20),
    ADD COLUMN expression_label      VARCHAR(50);
