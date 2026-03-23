-- timestamp_feedback 테이블 음성 특성 필드 추가 (Gemini 네이티브 오디오 분석 지원)
ALTER TABLE timestamp_feedback ADD COLUMN filler_words TEXT;
ALTER TABLE timestamp_feedback ADD COLUMN speech_pace VARCHAR(10);
ALTER TABLE timestamp_feedback ADD COLUMN tone_confidence INT;
ALTER TABLE timestamp_feedback ADD COLUMN emotion_label VARCHAR(20);
ALTER TABLE timestamp_feedback ADD COLUMN vocal_comment TEXT;

-- question_set_feedback 테이블 종합 리포트 필드 추가
ALTER TABLE question_set_feedback ADD COLUMN verbal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN vocal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN nonverbal_summary TEXT;
ALTER TABLE question_set_feedback ADD COLUMN strengths TEXT;
ALTER TABLE question_set_feedback ADD COLUMN improvements TEXT;
ALTER TABLE question_set_feedback ADD COLUMN top_priority_advice TEXT;
