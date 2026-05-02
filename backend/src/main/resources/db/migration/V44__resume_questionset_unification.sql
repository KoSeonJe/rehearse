-- V44: RESUME_BASED QuestionSet 의미 단일화 + 인터뷰당 1행 UNIQUE
-- Why: 사전생성 경로 (MAIN 질문 N행) 와 FSM 경로 (RESUME_OPENER 등 1행) 가
--      같은 RESUME_BASED 카테고리를 다른 cardinality 로 사용해 충돌. 사전생성
--      경로 자체를 코드에서 제거 → DB 도 1행만 허용

DELETE q FROM question q
  JOIN question_set qs ON q.question_set_id = qs.id
 WHERE qs.category = 'RESUME_BASED'
   AND qs.id NOT IN (
     SELECT t.qsi FROM (
       SELECT q2.question_set_id AS qsi
         FROM question q2
        WHERE q2.question_type IN
          ('RESUME_OPENER','RESUME_PLAYGROUND','RESUME_INTERROGATION','RESUME_WRAP_UP')
        GROUP BY q2.question_set_id
     ) t
   );

DELETE FROM question_set
 WHERE category = 'RESUME_BASED'
   AND id NOT IN (
     SELECT t.qsi FROM (
       SELECT q.question_set_id AS qsi
         FROM question q
        WHERE q.question_type IN
          ('RESUME_OPENER','RESUME_PLAYGROUND','RESUME_INTERROGATION','RESUME_WRAP_UP')
        GROUP BY q.question_set_id
     ) t
   );

ALTER TABLE question_set
  ADD UNIQUE INDEX uq_resume_per_interview
  ((CASE WHEN category = 'RESUME_BASED' THEN interview_id END));
