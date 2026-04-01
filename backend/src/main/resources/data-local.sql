-- 테스트 유저
INSERT INTO users (id, email, name, profile_image, provider, provider_id, role, created_at, updated_at)
VALUES (1, 'test@example.com', '테스트 유저', NULL, 'GITHUB', '12345', 'USER', NOW(), NOW());

-- 면접
INSERT INTO interview (id, public_id, user_id, position, level, duration_minutes, status, question_generation_status, overall_comment, created_at, updated_at)
VALUES (1, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 1, 'BACKEND', 'JUNIOR', 30, 'COMPLETED', 'COMPLETED', '전체 1개 질문세트 완료', NOW(), NOW());

INSERT INTO interview_interview_types (interview_id, interview_type) VALUES (1, 'CS_FUNDAMENTAL');

-- 질문세트
INSERT INTO question_set (id, interview_id, category, order_index, file_metadata_id, created_at, updated_at)
VALUES (1, 1, 'CS', 0, NULL, NOW(), NOW());

-- 질문
INSERT INTO question (id, question_set_id, question_type, question_text, model_answer, reference_type, feedback_perspective, order_index)
VALUES (1, 1, 'MAIN', 'JPA의 영속성 컨텍스트에 대해 설명해주세요.', '영속성 컨텍스트는 엔티티를 영구 저장하는 환경으로, 1차 캐시, 변경 감지, 지연 로딩 등의 기능을 제공합니다.', 'MODEL_ANSWER', 'TECHNICAL', 0);

INSERT INTO question (id, question_set_id, question_type, question_text, model_answer, reference_type, feedback_perspective, order_index)
VALUES (2, 1, 'MAIN', '트랜잭션 격리 수준에 대해 설명해주세요.', 'READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE 4단계가 있으며, MySQL 기본값은 REPEATABLE READ입니다.', 'MODEL_ANSWER', 'TECHNICAL', 1);

-- 질문세트 분석
INSERT INTO question_set_analysis (id, question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at)
VALUES (1, 1, 'COMPLETED', 'PENDING', true, true, NOW(), NOW());

-- 답변
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (1, 1, 0, 45000);
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (2, 2, 45000, 90000);

-- 질문세트 피드백
INSERT INTO question_set_feedback (id, question_set_id, question_set_comment, created_at)
VALUES (1, 1, '언어 분석 2/2개 완료, 비언어 분석 2/2개 완료되었습니다.', NOW());

-- 피드백 1
INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, verbal_comment, filler_word_count,
  eye_contact_level, posture_level, expression_label, nonverbal_comment,
  overall_comment, is_analyzed,
  filler_words, speech_pace, tone_confidence_level, emotion_label, vocal_comment,
  accuracy_issues, coaching_structure, coaching_improvement
) VALUES (
  1, 1, 1, 0, 45000,
  '영속성 컨텍스트는 음 엔티티를 관리하는 환경인데요, 1차 캐시가 있어서 같은 엔티티를 조회하면 DB를 안 거치고 캐시에서 가져옵니다. 그리고 어 변경 감지 기능이 있어서 트랜잭션 커밋 시점에 자동으로 UPDATE 쿼리가 나갑니다. 기본 격리 수준이 SERIALIZABLE이라서 동시성 문제도 해결됩니다.',
  '✓ 영속성 컨텍스트의 핵심 개념(1차 캐시, 변경 감지)을 정확히 설명했습니다
△ 격리 수준과 영속성 컨텍스트를 혼동하여 부정확한 내용이 포함되었습니다
→ 영속성 컨텍스트의 생명주기와 트랜잭션 범위를 구분하여 설명해보세요',
  2,
  'GOOD', 'AVERAGE', 'CONFIDENT',
  '✓ 카메라를 안정적으로 응시하며 자신감 있는 표정을 유지했습니다
△ 답변 후반부에 자세가 약간 기울어졌습니다
→ 의자 등받이에 기대지 말고 상체를 곧게 유지해보세요',
  '', true,
  '["음", "어"]', '적절', 'GOOD', '자신감',
  '✓ 말 속도가 적절하고 자신감 있는 어조입니다
△ 문장 시작 시 필러워드가 반복됩니다
→ 짧은 호흡 후 바로 핵심 내용으로 시작하는 연습을 해보세요',
  '[{"claim":"기본 격리 수준이 SERIALIZABLE","correction":"MySQL 기본 격리 수준은 REPEATABLE READ이며, 영속성 컨텍스트와 DB 격리 수준은 별개의 개념입니다"}]',
  '개념→원리→실무적용 순서로 설명했으나, 격리 수준 언급이 주제에서 벗어났습니다',
  '영속성 컨텍스트의 생명주기와 지연 로딩의 관계를 추가로 설명하면 더 깊이 있는 답변이 됩니다'
);

-- 피드백 2
INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, verbal_comment, filler_word_count,
  eye_contact_level, posture_level, expression_label, nonverbal_comment,
  overall_comment, is_analyzed,
  filler_words, speech_pace, tone_confidence_level, emotion_label, vocal_comment,
  accuracy_issues, coaching_structure, coaching_improvement
) VALUES (
  2, 1, 2, 45000, 90000,
  '트랜잭션 격리 수준은 READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE 4단계가 있습니다. MySQL은 기본적으로 REPEATABLE READ를 사용하고, 이 수준에서는 팬텀 리드가 발생할 수 있지만 MySQL은 갭 락으로 이를 방지합니다.',
  '✓ 4단계 격리 수준을 정확히 나열하고 MySQL 기본값을 올바르게 설명했습니다
△ 각 격리 수준별 발생 가능한 문제에 대한 설명이 부족합니다
→ 각 수준에서 어떤 이상 현상이 발생/방지되는지 표로 정리해서 설명해보세요',
  0,
  'GOOD', 'GOOD', 'CONFIDENT',
  '✓ 안정적인 시선 처리와 바른 자세를 유지했습니다
△ 특별한 보완 사항이 없습니다
→ 현재 수준을 유지하세요',
  '', true,
  '[]', '적절', 'GOOD', '자신감',
  '✓ 필러워드 없이 깔끔하게 전달했습니다
△ 특별한 보완 사항이 없습니다
→ 현재 수준을 유지하세요',
  '[]',
  '개념 나열에서 MySQL 특성, 내부 동작까지 깊이 있게 구조화했습니다',
  '실무에서 격리 수준 변경이 필요했던 경험이나 데드락 시나리오를 덧붙이면 더 설득력 있는 답변이 됩니다'
);
