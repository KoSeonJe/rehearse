-- feedback-redesign: Question 엔티티에 feedbackPerspective 추가
ALTER TABLE question ADD COLUMN feedback_perspective VARCHAR(20) NULL;

-- feedback-redesign: TimestampFeedback 새 필드 추가
ALTER TABLE timestamp_feedback ADD COLUMN accuracy_issues TEXT NULL;
ALTER TABLE timestamp_feedback ADD COLUMN coaching_structure VARCHAR(500) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN coaching_improvement VARCHAR(500) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN eye_contact_level VARCHAR(20) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN posture_level VARCHAR(20) NULL;
ALTER TABLE timestamp_feedback ADD COLUMN tone_confidence_level VARCHAR(20) NULL;

-- feedback-redesign: score 컬럼 제거
ALTER TABLE timestamp_feedback DROP COLUMN verbal_score;
ALTER TABLE timestamp_feedback DROP COLUMN eye_contact_score;
ALTER TABLE timestamp_feedback DROP COLUMN posture_score;
ALTER TABLE timestamp_feedback DROP COLUMN tone_confidence;

-- feedback-redesign: questionSetScore 제거
ALTER TABLE question_set_feedback DROP COLUMN question_set_score;

-- feedback-redesign: Interview overallScore 제거
ALTER TABLE interview DROP COLUMN overall_score;
