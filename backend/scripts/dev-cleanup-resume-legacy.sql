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
--    NOTE: 테이블 자체는 후속 PR(V48 DROP TABLE) 에서 삭제. 본 스크립트는 row 만 비운다.
DELETE FROM interview_plan;

-- 2) 구 QuestionType row 정리
--    V44 마이그레이션 시점에 실제로 적재 가능했던 literal:
--      RESUME_PLAYGROUND, RESUME_INTERROGATION, RESUME_WRAP_UP
--    (RESUME_OPENER 는 신/구 공통으로 살아있으므로 제외)
--    참고: question 의 자식 FK (timestamp_feedback, question_score, question_answer)
--    가 ON DELETE CASCADE 가 아니므로 자식 row 먼저 제거.

CREATE TEMPORARY TABLE _cleanup_target_q (id BIGINT PRIMARY KEY);
INSERT INTO _cleanup_target_q (id)
  SELECT id FROM question
   WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION', 'RESUME_WRAP_UP');

DELETE FROM timestamp_feedback WHERE question_id IN (SELECT id FROM _cleanup_target_q);
DELETE FROM question_score     WHERE question_id IN (SELECT id FROM _cleanup_target_q);
DELETE FROM question_answer    WHERE question_id IN (SELECT id FROM _cleanup_target_q);
DELETE FROM question           WHERE id          IN (SELECT id FROM _cleanup_target_q);

DROP TEMPORARY TABLE _cleanup_target_q;

-- 3) 위 question 삭제로 고아가 된 question_set (Resume 카테고리, question 0개) 정리.
--    question_set 자식 FK 도 동일 원칙으로 먼저 제거.
CREATE TEMPORARY TABLE _cleanup_target_qs (id BIGINT PRIMARY KEY);
INSERT INTO _cleanup_target_qs (id)
  SELECT qs.id
    FROM question_set qs
    LEFT JOIN question q ON q.question_set_id = qs.id
   WHERE qs.category = 'RESUME_BASED'
     AND q.id IS NULL;

DELETE FROM question_set_analysis WHERE question_set_id IN (SELECT id FROM _cleanup_target_qs);
DELETE FROM question_set_feedback WHERE question_set_id IN (SELECT id FROM _cleanup_target_qs);
DELETE FROM question_set          WHERE id              IN (SELECT id FROM _cleanup_target_qs);

DROP TEMPORARY TABLE _cleanup_target_qs;
