-- 자유서술 4 + vocal raw 3 + vision raw 3 = 10 컬럼 DROP
ALTER TABLE timestamp_feedback
    DROP COLUMN nonverbal_comment,
    DROP COLUMN overall_comment,
    DROP COLUMN vocal_comment,
    DROP COLUMN attitude_comment,
    DROP COLUMN speech_pace,
    DROP COLUMN tone_confidence_level,
    DROP COLUMN emotion_label,
    DROP COLUMN eye_contact_level,
    DROP COLUMN posture_level,
    DROP COLUMN expression_label;
