-- V14__analysis_state_redesign.sql
-- 분석 파이프라인 상태 관리 재설계: QuestionSetAnalysis 테이블 분리

-- ============================================================
-- Phase 1: 신규 테이블 생성
-- ============================================================

CREATE TABLE question_set_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    analysis_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    convert_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_verbal_completed BOOLEAN NOT NULL DEFAULT FALSE,
    is_nonverbal_completed BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(500),
    failure_detail TEXT,
    convert_failure_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qs_analysis_question_set FOREIGN KEY (question_set_id) REFERENCES question_set(id) ON DELETE CASCADE,
    CONSTRAINT uq_qs_analysis_question_set UNIQUE (question_set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 좀비 감지 스케줄러 쿼리 최적화용 인덱스
CREATE INDEX idx_qs_analysis_status_updated ON question_set_analysis(analysis_status, updated_at);
CREATE INDEX idx_qs_analysis_convert_status_updated ON question_set_analysis(convert_status, updated_at);

-- ============================================================
-- Phase 2: in-flight 데이터 안전 처리
-- ============================================================

-- 마이그레이션 시점에 ANALYZING 상태인 데이터를 FAILED로 리셋
-- (Lambda가 실행 중이더라도 상태 업데이트 시 version 충돌로 안전하게 실패)
UPDATE question_set
SET analysis_status = 'FAILED',
    failure_reason = 'MIGRATION_RESET',
    failure_detail = '마이그레이션 중 진행 상태 리셋. 재시도 필요.'
WHERE analysis_status = 'ANALYZING';

-- ============================================================
-- Phase 3: 기존 데이터 이관
-- ============================================================

INSERT INTO question_set_analysis (
    question_set_id,
    analysis_status,
    convert_status,
    is_verbal_completed,
    is_nonverbal_completed,
    failure_reason,
    failure_detail,
    created_at,
    updated_at
)
SELECT
    qs.id,
    qs.analysis_status,
    CASE
        WHEN fm.status = 'CONVERTED' THEN 'COMPLETED'
        WHEN fm.status = 'CONVERTING' THEN 'PROCESSING'
        WHEN fm.status = 'FAILED' AND fm.failure_reason LIKE '%convert%' THEN 'FAILED'
        WHEN fm.status = 'FAILED' AND fm.failure_reason LIKE '%CONVERT%' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    CASE WHEN qs.analysis_status = 'COMPLETED' THEN TRUE ELSE FALSE END,
    CASE WHEN qs.analysis_status = 'COMPLETED' THEN TRUE ELSE FALSE END,
    qs.failure_reason,
    qs.failure_detail,
    qs.created_at,
    qs.updated_at
FROM question_set qs
LEFT JOIN file_metadata fm ON qs.file_metadata_id = fm.id;

-- ============================================================
-- Phase 4: FileMetadata 상태 단순화
-- ============================================================

-- 변환 상태는 question_set_analysis.convert_status로 이동
UPDATE file_metadata SET status = 'UPLOADED' WHERE status = 'CONVERTING';
UPDATE file_metadata SET status = 'UPLOADED' WHERE status = 'CONVERTED';

-- ============================================================
-- Phase 5: QuestionSet에서 상태 컬럼 제거
-- ============================================================

-- 인덱스 먼저 제거 (V4에서 생성한 인덱스)
DROP INDEX idx_question_set_status_updated ON question_set;

-- 컬럼 제거
ALTER TABLE question_set
    DROP COLUMN analysis_status,
    DROP COLUMN analysis_progress,
    DROP COLUMN failure_reason,
    DROP COLUMN failure_detail;

-- ============================================================
-- Phase 6: file_metadata 인덱스 정리
-- ============================================================

-- 기존 status 기반 인덱스는 유지 (PENDING/UPLOADED/FAILED 조회에 여전히 유효)
