-- timestamp_feedback 테이블 음성 특성 필드 추가 (Gemini 네이티브 오디오 분석 지원)
ALTER TABLE timestamp_feedback ADD COLUMN filler_words TEXT;
ALTER TABLE timestamp_feedback ADD COLUMN speech_pace VARCHAR(10);
ALTER TABLE timestamp_feedback ADD COLUMN tone_confidence INT;
ALTER TABLE timestamp_feedback ADD COLUMN emotion_label VARCHAR(20);
ALTER TABLE timestamp_feedback ADD COLUMN vocal_comment TEXT;

