# Plan 01: QuestionSetAnalysis 엔티티 + Enum + DB 마이그레이션

> 상태: Draft
> 작성일: 2026-03-24

## Why

상태 필드가 QuestionSet과 FileMetadata에 산재되어 있고, Status/Progress 이중 관리로 복잡도가 높다. QuestionSetAnalysis 별도 테이블로 통합하고, Enum을 정리하여 상태 모델을 단순화한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/.../questionset/entity/QuestionSetAnalysis.java` | **신규** — 엔티티 생성 (analysisStatus, convertStatus, isVerbalCompleted, isNonverbalCompleted, failureReason, failureDetail) |
| `backend/.../questionset/entity/ConvertStatus.java` | **신규** — enum (PENDING, PROCESSING, COMPLETED, FAILED) + canTransitionTo() |
| `backend/.../questionset/repository/QuestionSetAnalysisRepository.java` | **신규** — JPA Repository |
| `backend/.../questionset/entity/AnalysisStatus.java` | EXTRACTING, ANALYZING, FINALIZING, PARTIAL 추가 + canTransitionTo() 재작성 |
| `backend/.../questionset/entity/AnalysisProgress.java` | **삭제** |
| `backend/.../file/entity/FileStatus.java` | CONVERTING, CONVERTED 제거 → PENDING, UPLOADED, FAILED만. canTransitionTo() 재작성 |
| `backend/.../questionset/entity/QuestionSet.java` | analysisStatus, analysisProgress, failureReason, failureDetail 필드 제거. updateAnalysisStatus(), updateAnalysisProgress(), markFailed() 메서드 제거. QuestionSetAnalysis @OneToOne 연관 추가 |
| `backend/.../file/entity/FileMetadata.java` | CONVERTING/CONVERTED 관련 상태 전이 제거 확인 |
| `backend/src/main/resources/db/migration/V{N}__analysis_state_redesign.sql` | **신규** — 마이그레이션 |

## 상세

### QuestionSetAnalysis 엔티티

```java
@Entity
@Table(name = "question_set_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuestionSetAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false, unique = true)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConvertStatus convertStatus = ConvertStatus.PENDING;

    @Column(nullable = false)
    private boolean isVerbalCompleted = false;

    @Column(nullable = false)
    private boolean isNonverbalCompleted = false;

    @Column(length = 500)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String failureDetail;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // --- 도메인 메서드 ---

    public void updateAnalysisStatus(AnalysisStatus newStatus) {
        if (!this.analysisStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("분석 상태를 %s에서 %s로 변경할 수 없습니다.", this.analysisStatus, newStatus));
        }
        this.analysisStatus = newStatus;
    }

    public void updateConvertStatus(ConvertStatus newStatus) {
        if (!this.convertStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("변환 상태를 %s에서 %s로 변경할 수 없습니다.", this.convertStatus, newStatus));
        }
        this.convertStatus = newStatus;
    }

    public void completeAnalysis(boolean verbalCompleted, boolean nonverbalCompleted) {
        this.isVerbalCompleted = verbalCompleted;
        this.isNonverbalCompleted = nonverbalCompleted;

        if (verbalCompleted && nonverbalCompleted) {
            updateAnalysisStatus(AnalysisStatus.COMPLETED);
        } else if (!verbalCompleted && !nonverbalCompleted) {
            updateAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            updateAnalysisStatus(AnalysisStatus.PARTIAL);
        }
    }

    public void markFailed(String reason, String detail) {
        updateAnalysisStatus(AnalysisStatus.FAILED);
        this.failureReason = reason;
        this.failureDetail = detail;
    }

    public void resetVerbalResult() {
        this.isVerbalCompleted = false;
    }

    public void resetNonverbalResult() {
        this.isNonverbalCompleted = false;
    }

    public boolean isFullyReady() {
        boolean analysisOk = analysisStatus == AnalysisStatus.COMPLETED
                          || analysisStatus == AnalysisStatus.PARTIAL;
        boolean convertOk = convertStatus == ConvertStatus.COMPLETED;
        return analysisOk && convertOk;
    }
}
```

### DB 마이그레이션

파일: `V14__analysis_state_redesign.sql` (MySQL 8.0, InnoDB)

> 주의: 저트래픽 시간대에 실행 권장. in-flight ANALYZING 데이터를 FAILED로 리셋하므로, 진행 중 분석이 있으면 재시도 필요.

```sql
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
    -- analysis_status: 기존 값 그대로 (PENDING, PENDING_UPLOAD, COMPLETED, FAILED, SKIPPED)
    qs.analysis_status,
    -- convert_status: FileMetadata.status에서 변환 상태 매핑
    CASE
        WHEN fm.status = 'CONVERTED' THEN 'COMPLETED'
        WHEN fm.status = 'CONVERTING' THEN 'PROCESSING'
        WHEN fm.status = 'FAILED' AND fm.failure_reason LIKE '%convert%' THEN 'FAILED'
        WHEN fm.status = 'FAILED' AND fm.failure_reason LIKE '%CONVERT%' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    -- is_verbal_completed: COMPLETED인 경우에만 true
    IF(qs.analysis_status = 'COMPLETED', TRUE, FALSE),
    -- is_nonverbal_completed: COMPLETED인 경우에만 true
    IF(qs.analysis_status = 'COMPLETED', TRUE, FALSE),
    -- failure 정보 이관
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
-- FileMetadata는 업로드 여부만 관리
UPDATE file_metadata SET status = 'UPLOADED' WHERE status = 'CONVERTING';
UPDATE file_metadata SET status = 'UPLOADED' WHERE status = 'CONVERTED';

-- file_metadata의 변환 관련 failure 정보는 이미 question_set_analysis로 이관됨
-- failure_reason/failure_detail은 FileMetadata에 유지 (업로드 실패 시 사용)

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
-- idx_file_metadata_status_updated 유지
```

## 담당 에이전트

- Implement: `backend` — 엔티티, enum, 마이그레이션 스크립트
- Review: `architect-reviewer` — 상태 전이 일관성, 도메인 무결성, 1:1 관계 설계

## 검증

- AnalysisStatus.canTransitionTo() 전이 검증 단위 테스트 (PARTIAL 포함 전 케이스)
- ConvertStatus.canTransitionTo() 전이 검증 단위 테스트
- QuestionSetAnalysis.completeAnalysis() 집계 로직 단위 테스트 (COMPLETED/PARTIAL/FAILED 각 케이스)
- QuestionSetAnalysis.isFullyReady() 판정 단위 테스트
- 마이그레이션 SQL을 H2 테스트 DB에서 검증
- 기존 COMPLETED/FAILED 데이터가 정상 이관되는지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
