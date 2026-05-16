SET FOREIGN_KEY_CHECKS = 0;

-- 테스트 유저
INSERT INTO users (id, email, name, profile_image, provider, provider_id, role, created_at, updated_at)
VALUES (1, 'test@example.com', '테스트 유저', NULL, 'GITHUB', '12345', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 파일 메타데이터 (영상)
INSERT INTO file_metadata (id, file_type, status, s3_key, streaming_s3_key, bucket, content_type, file_size_bytes, created_at, updated_at, version)
VALUES (1, 'VIDEO', 'UPLOADED', 'videos/1/qs_1.webm', 'videos/1/qs_1.mp4', 'rehearse-videos-dev', 'video/webm', 5242880, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO file_metadata (id, file_type, status, s3_key, streaming_s3_key, bucket, content_type, file_size_bytes, created_at, updated_at, version)
VALUES (2, 'VIDEO', 'UPLOADED', 'videos/2/qs_2.webm', 'videos/2/qs_2.mp4', 'rehearse-videos-dev', 'video/webm', 4194304, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO file_metadata (id, file_type, status, s3_key, streaming_s3_key, bucket, content_type, file_size_bytes, created_at, updated_at, version)
VALUES (3, 'VIDEO', 'UPLOADED', 'videos/2/qs_3.webm', 'videos/2/qs_3.mp4', 'rehearse-videos-dev', 'video/webm', 3145728, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO file_metadata (id, file_type, status, s3_key, streaming_s3_key, bucket, content_type, file_size_bytes, created_at, updated_at, version)
VALUES (4, 'VIDEO', 'UPLOADED', 'videos/3/qs_4.webm', 'videos/3/qs_4.mp4', 'rehearse-videos-dev', 'video/webm', 6291456, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id = id;

-- 면접
INSERT INTO interview (id, public_id, user_id, position, level, duration_minutes, status, question_generation_status, created_at, updated_at)
VALUES (1, 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 1, 'BACKEND', 'JUNIOR', 30, 'COMPLETED', 'COMPLETED', NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO interview_interview_types (interview_id, interview_type)
VALUES (1, 'CS_FUNDAMENTAL')
ON DUPLICATE KEY UPDATE interview_id = interview_id;

-- 질문세트
INSERT INTO question_set (id, interview_id, category, order_index, file_metadata_id, created_at, updated_at)
VALUES (1, 1, 'CS_FUNDAMENTAL', 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 질문
INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (1, 1, 'TECH_MAIN', 'JPA의 영속성 컨텍스트에 대해 설명해주세요.', '영속성 컨텍스트는 엔티티를 영구 저장하는 환경으로, 1차 캐시, 변경 감지, 지연 로딩 등의 기능을 제공합니다.', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (2, 1, 'TECH_MAIN', '트랜잭션 격리 수준에 대해 설명해주세요.', 'READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE 4단계가 있으며, MySQL 기본값은 REPEATABLE READ입니다.', 1)
ON DUPLICATE KEY UPDATE id = id;

-- 질문세트 분석
INSERT INTO question_set_analysis (id, question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at)
VALUES (1, 1, 'COMPLETED', 'COMPLETED', true, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 답변
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (1, 1, 0, 45000)
ON DUPLICATE KEY UPDATE id = id;
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (2, 2, 45000, 90000)
ON DUPLICATE KEY UPDATE id = id;

-- 질문세트 피드백
INSERT INTO question_set_feedback (id, question_set_id, question_set_comment, created_at)
VALUES (1, 1, '언어 분석 2/2개 완료, 비언어 분석 2/2개 완료되었습니다.', NOW())
ON DUPLICATE KEY UPDATE id = id;

-- 피드백 1
INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  1, 1, 1, 0, 45000,
  '영속성 컨텍스트는 음 엔티티를 관리하는 환경인데요, 1차 캐시가 있어서 같은 엔티티를 조회하면 DB를 안 거치고 캐시에서 가져옵니다. 그리고 어 변경 감지 기능이 있어서 트랜잭션 커밋 시점에 자동으로 UPDATE 쿼리가 나갑니다. 기본 격리 수준이 SERIALIZABLE이라서 동시성 문제도 해결됩니다.',
  2, true, '["음", "어"]'
) ON DUPLICATE KEY UPDATE id = id;

-- 피드백 2
INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  2, 1, 2, 45000, 90000,
  '트랜잭션 격리 수준은 READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ, SERIALIZABLE 4단계가 있습니다. MySQL은 기본적으로 REPEATABLE READ를 사용하고, 이 수준에서는 팬텀 리드가 발생할 수 있지만 MySQL은 갭 락으로 이를 방지합니다.',
  0, true, '[]'
) ON DUPLICATE KEY UPDATE id = id;

-- ===== 면접 2: 프론트엔드 · UI 프레임워크 =====
INSERT INTO interview (id, public_id, user_id, position, position_detail, level, duration_minutes, status, question_generation_status, created_at, updated_at)
VALUES (2, 'b2c3d4e5-f6a7-8901-bcde-f23456789012', 1, 'FRONTEND', 'React/TypeScript', 'MID', 30, 'COMPLETED', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO interview_interview_types (interview_id, interview_type) VALUES (2, 'UI_FRAMEWORK')
ON DUPLICATE KEY UPDATE interview_id = interview_id;
INSERT INTO interview_interview_types (interview_id, interview_type) VALUES (2, 'BEHAVIORAL')
ON DUPLICATE KEY UPDATE interview_id = interview_id;

INSERT INTO question_set (id, interview_id, category, order_index, file_metadata_id, created_at, updated_at)
VALUES (2, 2, 'UI_FRAMEWORK', 0, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set (id, interview_id, category, order_index, file_metadata_id, created_at, updated_at)
VALUES (3, 2, 'BEHAVIORAL', 1, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (3, 2, 'TECH_MAIN', 'React의 Virtual DOM 동작 원리와 Reconciliation 과정을 설명해주세요.', 'Virtual DOM은 메모리에 UI의 가상 표현을 유지하고, 상태 변경 시 새 Virtual DOM 트리를 생성한 뒤 이전 트리와 비교(diffing)하여 최소한의 실제 DOM 조작만 수행합니다.', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (4, 2, 'TECH_MAIN', 'useEffect와 useLayoutEffect의 차이점을 실무 관점에서 설명해주세요.', 'useEffect는 브라우저 페인트 이후 비동기로 실행되고, useLayoutEffect는 DOM 변경 후 페인트 이전에 동기적으로 실행됩니다. 레이아웃 측정이나 깜빡임 방지가 필요할 때 useLayoutEffect를 사용합니다.', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (5, 3, 'BEHAVIORAL_MAIN', '팀에서 기술적 의견 충돌이 있었던 경험과 어떻게 해결했는지 설명해주세요.', NULL, 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_analysis (id, question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at)
VALUES (2, 2, 'COMPLETED', 'COMPLETED', true, true, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_analysis (id, question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at)
VALUES (3, 3, 'COMPLETED', 'COMPLETED', true, true, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (3, 3, 0, 60000)
ON DUPLICATE KEY UPDATE id = id;
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (4, 4, 60000, 120000)
ON DUPLICATE KEY UPDATE id = id;
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (5, 5, 0, 55000)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_feedback (id, question_set_id, question_set_comment, created_at)
VALUES (2, 2, 'React 핵심 개념에 대한 이해도가 높습니다.', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_feedback (id, question_set_id, question_set_comment, created_at)
VALUES (3, 3, '행동 면접 답변이 구체적이고 설득력 있습니다.', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  3, 2, 3, 0, 60000,
  'Virtual DOM은 실제 DOM의 경량 복사본 같은 건데요, 상태가 바뀌면 새로운 Virtual DOM 트리를 만들고 이전 트리와 비교해서 바뀐 부분만 실제 DOM에 반영합니다. React는 Fiber 아키텍처를 통해 이 과정을 청크 단위로 나눠서 처리할 수 있고, 우선순위도 조절할 수 있습니다.',
  1, true, '["음"]'
) ON DUPLICATE KEY UPDATE id = id;

INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  4, 2, 4, 60000, 120000,
  'useEffect는 컴포넌트가 화면에 그려진 다음에 실행되고, useLayoutEffect는 DOM이 변경된 직후 화면에 그리기 전에 실행됩니다. 실무에서는 대부분 useEffect를 쓰는데, 스크롤 위치를 복원하거나 DOM 크기를 측정해서 바로 반영해야 할 때는 useLayoutEffect를 씁니다.',
  0, true, '[]'
) ON DUPLICATE KEY UPDATE id = id;

INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  5, 3, 5, 0, 55000,
  '이전 팀에서 상태 관리 라이브러리 선택을 두고 의견이 나뉜 적이 있었습니다. 저는 Zustand를 제안했고 다른 분은 Redux Toolkit을 선호했는데, 각자 PoC를 만들어서 번들 사이즈, 보일러플레이트 양, 러닝 커브를 비교한 뒤 팀 투표로 결정했습니다.',
  0, true, '[]'
) ON DUPLICATE KEY UPDATE id = id;

-- ===== 면접 3: 백엔드 · 시스템 설계 =====
INSERT INTO interview (id, public_id, user_id, position, position_detail, level, duration_minutes, status, question_generation_status, created_at, updated_at)
VALUES (3, 'c3d4e5f6-a7b8-9012-cdef-345678901234', 1, 'BACKEND', 'Java/Spring', 'MID', 30, 'COMPLETED', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO interview_interview_types (interview_id, interview_type) VALUES (3, 'SYSTEM_DESIGN')
ON DUPLICATE KEY UPDATE interview_id = interview_id;

INSERT INTO question_set (id, interview_id, category, order_index, file_metadata_id, created_at, updated_at)
VALUES (4, 3, 'SYSTEM_DESIGN', 0, 4, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (6, 4, 'TECH_MAIN', '대규모 트래픽을 처리하는 URL 단축 서비스를 설계해주세요.', '해시 기반 키 생성, Base62 인코딩, 읽기 캐시(Redis), 수평 확장 가능한 DB 샤딩, 301/302 리다이렉트 전략, 분석용 비동기 이벤트 처리 등을 고려해야 합니다.', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question (id, question_set_id, question_type, question_text, best_answer, order_index)
VALUES (7, 4, 'TECH_MAIN', '채팅 시스템의 메시지 전달 보장을 어떻게 설계하시겠습니까?', 'At-least-once 전달 + 클라이언트 중복 제거(idempotency key), 메시지 큐(Kafka), 읽음 확인 프로토콜, 오프라인 사용자를 위한 저장 후 전달 패턴을 설계합니다.', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_analysis (id, question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at)
VALUES (4, 4, 'COMPLETED', 'COMPLETED', true, true, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (6, 6, 0, 90000)
ON DUPLICATE KEY UPDATE id = id;
INSERT INTO question_answer (id, question_id, start_ms, end_ms) VALUES (7, 7, 90000, 180000)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO question_set_feedback (id, question_set_id, question_set_comment, created_at)
VALUES (4, 4, '시스템 설계 역량이 우수합니다.', DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  6, 4, 6, 0, 90000,
  'URL 단축 서비스는 먼저 API를 정의하고, POST로 원본 URL을 받아 단축 URL을 반환하고, GET으로 단축 URL에 접근하면 리다이렉트합니다. 키 생성은 해시 후 Base62로 인코딩하고, 충돌 시 재해시합니다. 읽기가 많으니까 Redis 캐시를 앞에 두고, DB는 NoSQL로 수평 확장합니다.',
  0, true, '[]'
) ON DUPLICATE KEY UPDATE id = id;

INSERT INTO timestamp_feedback (
  id, question_set_feedback_id, question_id, start_ms, end_ms,
  transcript, filler_word_count, is_analyzed, filler_words
) VALUES (
  7, 4, 7, 90000, 180000,
  '채팅 메시지 전달 보장은 At-least-once 전략을 기본으로 하고, 클라이언트에서 메시지 ID로 중복을 필터링합니다. 서버 간 메시지 전달은 Kafka를 사용하고, 오프라인 사용자는 메시지를 DB에 저장했다가 접속 시 전달합니다. WebSocket으로 실시간 연결을 유지하고, 연결이 끊기면 롱 폴링으로 폴백합니다.',
  0, true, '[]'
) ON DUPLICATE KEY UPDATE id = id;

-- ===== 복습 북마크 샘플 (피드백 3, 6, 7에 대해 북마크) =====
INSERT INTO review_bookmark (id, user_id, timestamp_feedback_id, resolved_at, created_at)
VALUES (1, 1, 3, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO review_bookmark (id, user_id, timestamp_feedback_id, resolved_at, created_at)
VALUES (2, 1, 6, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO review_bookmark (id, user_id, timestamp_feedback_id, resolved_at, created_at)
VALUES (3, 1, 7, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE id = id;

-- AUTO_INCREMENT 보정 (시드 데이터 최대 id 이후 값으로 설정)
ALTER TABLE users AUTO_INCREMENT = 10;
ALTER TABLE file_metadata AUTO_INCREMENT = 10;
ALTER TABLE interview AUTO_INCREMENT = 10;
ALTER TABLE question_set AUTO_INCREMENT = 10;
ALTER TABLE question AUTO_INCREMENT = 10;
ALTER TABLE question_set_analysis AUTO_INCREMENT = 10;
ALTER TABLE question_answer AUTO_INCREMENT = 10;
ALTER TABLE question_set_feedback AUTO_INCREMENT = 10;
ALTER TABLE timestamp_feedback AUTO_INCREMENT = 10;
ALTER TABLE review_bookmark AUTO_INCREMENT = 10;

SET FOREIGN_KEY_CHECKS = 1;
