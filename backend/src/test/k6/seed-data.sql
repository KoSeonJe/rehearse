-- 부하테스트용 50,000건 면접 + 질문세트 데이터 시드
-- interview(IN_PROGRESS) + question_set(FOLLOWUP 0건) → follow-up API 호출 가능

-- 트랜잭션 비활성화 + 벌크 삽입 최적화
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- 1. interview 50,000건
DELIMITER ;;
DROP PROCEDURE IF EXISTS seed_interviews;;
CREATE PROCEDURE seed_interviews()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 50000 DO
    INSERT INTO interview (position, level, duration_minutes, status, question_generation_status, public_id, created_at, updated_at)
    VALUES ('BACKEND', 'JUNIOR', 30, 'IN_PROGRESS', 'COMPLETED', UUID(), NOW(6), NOW(6));
    IF i % 5000 = 0 THEN
      COMMIT;
    END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END;;
DELIMITER ;

CALL seed_interviews();
DROP PROCEDURE IF EXISTS seed_interviews;

-- 2. question_set 50,000건 (interview 1:1)
DELIMITER ;;
DROP PROCEDURE IF EXISTS seed_question_sets;;
CREATE PROCEDURE seed_question_sets()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 50000 DO
    INSERT INTO question_set (interview_id, category, order_index, created_at, updated_at, version)
    VALUES (i, 'CS', 0, NOW(6), NOW(6), 0);
    IF i % 5000 = 0 THEN
      COMMIT;
    END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END;;
DELIMITER ;

CALL seed_question_sets();
DROP PROCEDURE IF EXISTS seed_question_sets;

-- 3. question_set_analysis 50,000건
DELIMITER ;;
DROP PROCEDURE IF EXISTS seed_analysis;;
CREATE PROCEDURE seed_analysis()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 50000 DO
    INSERT INTO question_set_analysis (question_set_id, analysis_status, convert_status, is_verbal_completed, is_nonverbal_completed, created_at, updated_at, version)
    VALUES (i, 'COMPLETED', 'COMPLETED', TRUE, TRUE, NOW(6), NOW(6), 0);
    IF i % 5000 = 0 THEN
      COMMIT;
    END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END;;
DELIMITER ;

CALL seed_analysis();
DROP PROCEDURE IF EXISTS seed_analysis;

-- 4. question (초기 질문 1건씩, FOLLOWUP 없음)
DELIMITER ;;
DROP PROCEDURE IF EXISTS seed_questions;;
CREATE PROCEDURE seed_questions()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 50000 DO
    INSERT INTO question (question_set_id, question_type, question_text, model_answer, order_index)
    VALUES (i, 'MAIN', '정렬 알고리즘의 종류와 각각의 시간복잡도를 설명해주세요.', '퀵정렬은 평균 O(n log n), 병합정렬은 항상 O(n log n)입니다.', 0);
    IF i % 5000 = 0 THEN
      COMMIT;
    END IF;
    SET i = i + 1;
  END WHILE;
  COMMIT;
END;;
DELIMITER ;

CALL seed_questions();
DROP PROCEDURE IF EXISTS seed_questions;

SET unique_checks = 1;
SET foreign_key_checks = 1;
SET autocommit = 1;

SELECT 'Seed complete' AS status,
       (SELECT COUNT(*) FROM interview) AS interviews,
       (SELECT COUNT(*) FROM question_set) AS question_sets,
       (SELECT COUNT(*) FROM question) AS questions;
