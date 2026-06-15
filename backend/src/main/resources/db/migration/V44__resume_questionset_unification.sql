-- V44: RESUME_BASED QuestionSet 의미 단일화 + 인터뷰당 1행 UNIQUE
-- Why: 사전생성 경로 (MAIN 질문 N행) 와 FSM 경로 (RESUME_OPENER 등 1행) 가
--      같은 RESUME_BASED 카테고리를 다른 cardinality 로 사용해 충돌. 사전생성
--      경로 자체를 코드에서 제거 → DB 도 1행만 허용
-- Note: question.id 를 참조하는 자식 FK (timestamp_feedback, question_score,
--       question_answer) 가 ON DELETE CASCADE 가 아니므로 자식부터 정리

CREATE TEMPORARY TABLE _v44_target_qs (id BIGINT PRIMARY KEY);
INSERT INTO _v44_target_qs (id)
  SELECT qs.id
    FROM question_set qs
   WHERE qs.category = 'RESUME_BASED'
     AND qs.id NOT IN (
       SELECT q.question_set_id
         FROM question q
        WHERE q.question_type IN
              ('RESUME_OPENER','RESUME_PLAYGROUND','RESUME_INTERROGATION','RESUME_WRAP_UP')
          AND q.question_set_id IS NOT NULL
        GROUP BY q.question_set_id
     );

CREATE TEMPORARY TABLE _v44_target_q (id BIGINT PRIMARY KEY);
INSERT INTO _v44_target_q (id)
  SELECT q.id FROM question q
   WHERE q.question_set_id IN (SELECT id FROM _v44_target_qs);

DELETE FROM timestamp_feedback WHERE question_id IN (SELECT id FROM _v44_target_q);
DELETE FROM question_score     WHERE question_id IN (SELECT id FROM _v44_target_q);
DELETE FROM question_answer    WHERE question_id IN (SELECT id FROM _v44_target_q);
DELETE FROM question           WHERE id          IN (SELECT id FROM _v44_target_q);

DELETE FROM question_set_analysis WHERE question_set_id IN (SELECT id FROM _v44_target_qs);
DELETE FROM question_set_feedback WHERE question_set_id IN (SELECT id FROM _v44_target_qs);
DELETE FROM question_set          WHERE id              IN (SELECT id FROM _v44_target_qs);

DROP TEMPORARY TABLE _v44_target_q;
DROP TEMPORARY TABLE _v44_target_qs;

ALTER TABLE question_set
  ADD UNIQUE INDEX uq_resume_per_interview
  ((CASE WHEN category = 'RESUME_BASED' THEN interview_id END));
