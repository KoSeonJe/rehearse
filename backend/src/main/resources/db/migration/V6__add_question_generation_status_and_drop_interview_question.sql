-- V6: Interview 비동기 질문 생성 상태 추가 + InterviewQuestion 레거시 제거

-- 1. interview 테이블에 질문 생성 상태 컬럼 추가
ALTER TABLE interview ADD COLUMN question_generation_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE interview ADD COLUMN failure_reason TEXT;

-- 2. interview_question 레거시 테이블 삭제
DROP TABLE IF EXISTS interview_question;
