-- interview 테이블에 public_id 컬럼 추가
ALTER TABLE interview ADD COLUMN public_id VARCHAR(36) NULL;

-- 기존 데이터 backfill (MySQL: UUID(), H2: RANDOM_UUID())
UPDATE interview SET public_id = UUID() WHERE public_id IS NULL;

-- NOT NULL 제약 추가 (MySQL 호환 문법)
ALTER TABLE interview MODIFY COLUMN public_id VARCHAR(36) NOT NULL;

-- 유니크 인덱스 추가
CREATE UNIQUE INDEX idx_interview_public_id ON interview(public_id);
