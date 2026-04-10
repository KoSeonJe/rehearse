-- V20: QuestionPool 미사용 컬럼 삭제
-- evaluation_criteria, follow_up_strategy, question_order, quality_score는
-- 저장만 되고 어디서도 조회/활용되지 않으므로 제거

ALTER TABLE question_pool DROP COLUMN evaluation_criteria;
ALTER TABLE question_pool DROP COLUMN follow_up_strategy;
ALTER TABLE question_pool DROP COLUMN question_order;
ALTER TABLE question_pool DROP COLUMN quality_score;
