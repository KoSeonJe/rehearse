-- V50: RESUME_BASED QuestionSet 의 인터뷰당 1행 UNIQUE 제거
-- Why: Resume 트랙 메인 질문 = QuestionSet 1개 분리 적재로 전환 (FE 의 "N개 질문세트" 라벨과 정합 + followup 부모 QSet 분리).
-- V44 가 도입한 uq_resume_per_interview 가 더 이상 유효하지 않음.
-- 호환성: 기존 1행 데이터는 그대로 유효 — 신규 면접부터 draft 당 QSet 분리 적재.
-- RESUME 트랙은 1 interview = 1회 생성이므로 기존/신규 혼재 가능성 없음.

ALTER TABLE question_set
  DROP INDEX uq_resume_per_interview;
