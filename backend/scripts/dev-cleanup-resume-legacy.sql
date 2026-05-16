-- Plan 481 Resume Track Simplification — dev DB 정리 스크립트
--
-- 목적: Big Bang 리팩토링으로 사용 중단된 InterviewPlan / 구 RESUME_* QuestionType row 제거.
-- 운영자(사용자)가 dev 환경에서 수동 실행한다. Agent 자동 실행 금지.
--
-- 실행 전 점검:
--   1. 본 스크립트는 DDL 변경이 아닌 DML (row 삭제) 이다. Flyway 등록 금지.
--   2. dev 환경에서만 실행. prod / staging 적용 금지.
--   3. 백업 또는 snapshot 확인 후 실행 권장.
--
-- 실행 순서:
--   mysql -h <dev-host> -u <user> -p <db> < dev-cleanup-resume-legacy.sql

-- 1) 구 interview_plan 테이블 row 제거 (Resume FSM Mode/Chain/Plan 폐기로 미사용)
--    NOTE: 테이블 자체는 후속 PR 에서 DROP. 본 스크립트는 row 만 비운다.
DELETE FROM interview_plan;

-- 2) 구 QuestionType (RESUME_PLAYGROUND_OPENER / RESUME_PLAYGROUND_RESPONDER / RESUME_CHAIN_INTERROGATION 등)
--    레거시 row 가 dev DB 에 남아 있을 경우 새 enum (RESUME_OPENER / RESUME_MAIN / RESUME_FOLLOWUP) 와 충돌.
--    참고: 새 enum 매핑은 별도 마이그레이션 PR 에서 수행. 본 SQL 은 dev 잔여물 정리 목적.
DELETE FROM question
 WHERE question_type IN (
       'RESUME_PLAYGROUND_OPENER',
       'RESUME_PLAYGROUND_RESPONDER',
       'RESUME_CHAIN_INTERROGATION',
       'RESUME_CHAIN_FOLLOWUP'
 );

-- 3) 위 question 삭제로 고아가 된 question_set (Resume 카테고리, question 0개) 정리
DELETE qs
  FROM question_set qs
  LEFT JOIN question q ON q.question_set_id = qs.id
 WHERE qs.category = 'RESUME_BASED'
   AND q.id IS NULL;
