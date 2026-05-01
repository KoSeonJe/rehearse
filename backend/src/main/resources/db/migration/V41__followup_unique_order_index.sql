-- follow-up 동시 호출로 인한 중복 orderIndex 삽입 방지
-- 기존 중복 데이터가 있을 경우 마이그레이션이 실패하므로 운영 적용 전 확인 필요
ALTER TABLE question
    ADD CONSTRAINT uq_question_set_order UNIQUE (question_set_id, order_index);
