-- V44: Resume-Track FSM 전용 QuestionSet 카테고리 분리
-- Why: RESUME_BASED 가 (a)레거시 사전생성 N행/인터뷰 와 (b)FSM 1행/인터뷰 두 의미로 충돌
--      → 동적 트랙은 RESUME_DYNAMIC 으로 분리하고 인터뷰당 1행 UNIQUE 강제

-- 1) 기존 데이터 백필: FSM 이 만든 동적 질문(RESUME_OPENER/PLAYGROUND/INTERROGATION/WRAP_UP)
--    을 보유한 question_set 만 RESUME_DYNAMIC 으로 재태깅
UPDATE question_set
   SET category = 'RESUME_DYNAMIC'
 WHERE id IN (
   SELECT qsi FROM (
     SELECT q.question_set_id AS qsi
       FROM question q
      WHERE q.question_type IN
        ('RESUME_OPENER','RESUME_PLAYGROUND','RESUME_INTERROGATION','RESUME_WRAP_UP')
      GROUP BY q.question_set_id
   ) t
 );

-- 2) functional UNIQUE: RESUME_DYNAMIC 인 행만 (interview_id) UNIQUE
ALTER TABLE question_set
  ADD UNIQUE INDEX uq_resume_dynamic_per_interview
  ((CASE WHEN category = 'RESUME_DYNAMIC' THEN interview_id END));
