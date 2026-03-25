# Plan 02: Service + API 변경 (BE)

> 상태: Draft
> 작성일: 2026-03-24

## Why

QuestionSetAnalysis 엔티티가 생기면, 기존에 QuestionSet을 직접 수정하던 서비스/API 레이어를 QuestionSetAnalysis 기반으로 전환해야 한다. Internal API 엔드포인트, DTO, 스케줄러, 면접 완료 판정 등 전체가 영향받는다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/.../questionset/service/InternalQuestionSetService.java` | updateProgress(): QuestionSetAnalysis.analysisStatus 업데이트로 변경. saveFeedback(): completeAnalysis(verbal, nonverbal) 호출. retryAnalysis(): PARTIAL 상태 처리 + 실패 분석만 리셋 |
| `backend/.../questionset/dto/UpdateProgressRequest.java` | AnalysisProgress → AnalysisStatus로 타입 변경. progress 필드 타입 수정 |
| `backend/.../questionset/dto/SaveFeedbackRequest.java` | isVerbalCompleted, isNonverbalCompleted boolean 필드 추가 |
| `backend/.../questionset/dto/QuestionSetStatusResponse.java` | analysisProgress 제거. convertStatus, isVerbalCompleted, isNonverbalCompleted, fullyReady 추가 |
| `backend/.../questionset/dto/AnswersResponse.java` | analysisStatus 참조를 QuestionSetAnalysis에서 조회하도록 변경 |
| `backend/.../questionset/controller/InternalQuestionSetController.java` | /progress 엔드포인트: UpdateProgressRequest 변경 반영. convertStatus 업데이트 엔드포인트 신규 추가 (Convert Lambda용) |
| `backend/.../questionset/scheduler/AnalysisScheduler.java` | detectFileConvertingZombies() → detectConvertZombies()로 재작성 (QuestionSetAnalysis.convertStatus 기반). detectAnalysisZombies() QuestionSetAnalysis 기반으로 변경 |
| `backend/.../interview/service/InterviewCompletionService.java` | analysisStatus 참조를 QuestionSetAnalysis에서 조회하도록 변경 |
| `backend/.../questionset/service/QuestionSetService.java` | saveAnswers()에서 QuestionSetAnalysis 생성 추가 |
| `backend/.../questionset/dto/QuestionSetResponse.java` | analysisStatus, failureReason 참조를 QuestionSetAnalysis에서 조회하도록 변경. InterviewResponse에 포함되는 핵심 DTO |
| `backend/.../file/service/InternalFileService.java` | CONVERTING/CONVERTED 참조 제거. updateFileStatus()에서 변환 상태 업데이트 로직 제거 (QuestionSetAnalysis로 이동) |
| `backend/.../file/dto/UpdateFileStatusRequest.java` | FileStatus enum 축소 반영 (CONVERTING/CONVERTED 제거) |

## 상세

### InternalQuestionSetService 주요 변경

```java
// updateProgress — AnalysisProgress → AnalysisStatus
@Transactional
public void updateProgress(Long questionSetId, UpdateProgressRequest request) {
    QuestionSetAnalysis analysis = findAnalysis(questionSetId);

    if (request.getStatus() == AnalysisStatus.FAILED) {
        analysis.markFailed(request.getFailureReason(), request.getFailureDetail());
        return;
    }

    analysis.updateAnalysisStatus(request.getStatus());
}

// saveFeedback — completeAnalysis 호출
@Transactional
public void saveFeedback(Long questionSetId, SaveFeedbackRequest request) {
    QuestionSet questionSet = findQuestionSet(questionSetId);
    QuestionSetAnalysis analysis = findAnalysis(questionSetId);

    // ... 기존 피드백 저장 로직 유지 ...

    feedbackRepository.save(feedback);
    analysis.completeAnalysis(
        request.isVerbalCompleted(),
        request.isNonverbalCompleted()
    );
}

// retryAnalysis — PARTIAL 상태 지원
@Transactional
public void retryAnalysis(Long questionSetId) {
    QuestionSetAnalysis analysis = findAnalysis(questionSetId);
    AnalysisStatus status = analysis.getAnalysisStatus();

    if (status != AnalysisStatus.FAILED && status != AnalysisStatus.PARTIAL) {
        throw new BusinessException(QuestionSetErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
    }

    // FAILED인 분석만 리셋
    if (!analysis.isVerbalCompleted()) {
        analysis.resetVerbalResult();
    }
    if (!analysis.isNonverbalCompleted()) {
        analysis.resetNonverbalResult();
    }

    analysis.updateAnalysisStatus(AnalysisStatus.EXTRACTING);
    // ... S3 재트리거 ...
}
```

### Convert Lambda용 엔드포인트 추가

```java
// InternalQuestionSetController — 변환 상태 업데이트
@PutMapping("/{questionSetId}/convert-status")
public ResponseEntity<Void> updateConvertStatus(
        @PathVariable Long interviewId,
        @PathVariable Long questionSetId,
        @RequestBody @Valid UpdateConvertStatusRequest request) {
    internalQuestionSetService.updateConvertStatus(questionSetId, request);
    return ResponseEntity.ok().build();
}
```

### updateConvertStatus 서비스 — streamingS3Key 처리

Convert Lambda가 변환 완료 시 `streamingS3Key`를 전송한다. QuestionSetAnalysis에는 이 필드가 없으므로, 서비스에서 FileMetadata에도 함께 저장한다:

```java
@Transactional
public void updateConvertStatus(Long questionSetId, UpdateConvertStatusRequest request) {
    QuestionSetAnalysis analysis = findAnalysis(questionSetId);
    analysis.updateConvertStatus(request.getStatus());

    // streamingS3Key는 FileMetadata에 저장 (기존 필드 활용)
    if (request.getStreamingS3Key() != null) {
        QuestionSet qs = analysis.getQuestionSet();
        FileMetadata file = qs.getFileMetadata();
        if (file != null) {
            file.updateStreamingS3Key(request.getStreamingS3Key());
        }
    }

    if (request.getStatus() == ConvertStatus.FAILED) {
        analysis.setConvertFailureReason(request.getFailureReason());
    }
}
```

### InterviewCompletionService — PARTIAL 처리

```java
// 변경 전: COMPLETED와 SKIPPED만 카운트
long completedCount = countByStatus(COMPLETED) + countByStatus(SKIPPED);

// 변경 후: PARTIAL도 완료로 카운트
long completedCount = countByStatus(COMPLETED) + countByStatus(PARTIAL) + countByStatus(SKIPPED);
```

### AnalysisScheduler — 전체 메서드 QuestionSetAnalysis 기반으로 변경

- `detectAnalysisZombies()`: QuestionSetAnalysis.analysisStatus 기반으로 변경
- `detectConvertZombies()`: QuestionSetAnalysis.convertStatus = PROCESSING + updatedAt > 10분 → FAILED
- `detectPendingUploadZombies()`: QuestionSetAnalysis.analysisStatus = PENDING_UPLOAD 기반으로 변경
- `detectUploadZombies()`: QuestionSetAnalysis 기반으로 변경

### 동시성 처리 — @Version 충돌 대응

분석 Lambda와 변환 Lambda가 동시에 QuestionSetAnalysis를 업데이트할 수 있다 (analysisStatus vs convertStatus). @Version 낙관적 잠금으로 한쪽이 실패할 수 있음.

```java
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
@Transactional
public void updateConvertStatus(Long questionSetId, UpdateConvertStatusRequest request) { ... }

@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
@Transactional
public void updateProgress(Long questionSetId, UpdateProgressRequest request) { ... }
```

### QuestionSetAnalysis 생성 시점

QuestionSet 생성 시 eager하게 함께 생성한다 (SKIPPED 질문세트도 포함):

```java
// QuestionSetService 또는 QuestionSet 엔티티 생성 로직에서
QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
    .questionSet(questionSet)
    .build();
analysisRepository.save(analysis);
```

### QuestionSetStatusResponse 변경

```java
@Getter @Builder
public class QuestionSetStatusResponse {
    private final Long id;
    private final AnalysisStatus analysisStatus;     // 유지 (Progress 흡수)
    private final ConvertStatus convertStatus;        // 신규
    private final FileStatus fileStatus;              // 유지 (업로드 여부)
    private final boolean isVerbalCompleted;           // 신규
    private final boolean isNonverbalCompleted;        // 신규
    private final boolean fullyReady;                  // 신규
    private final String failureReason;
}
```

## 담당 에이전트

- Implement: `backend` — 서비스, DTO, 컨트롤러, 스케줄러 수정
- Review: `code-reviewer` — 기존 API 하위 호환성, 트랜잭션 경계

## 검증

- updateProgress API: AnalysisStatus 값으로 정상 업데이트 확인
- saveFeedback API: isVerbalCompleted/isNonverbalCompleted 조합별 analysisStatus 판정 (COMPLETED/PARTIAL/FAILED)
- retryAnalysis API: PARTIAL에서 재시도 → COMPLETED 전환
- updateConvertStatus API: ConvertStatus 전이 정상 동작
- AnalysisScheduler: 좀비 감지 쿼리가 QuestionSetAnalysis 기반으로 동작
- InterviewCompletionService: 면접 완료 판정이 QuestionSetAnalysis 기반으로 동작
- `progress.md` 상태 업데이트 (Task 2 → Completed)
