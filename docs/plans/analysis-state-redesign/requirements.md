# 분석 파이프라인 상태 관리 재설계 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-24

## Why

### 1. Why? — 어떤 문제를 해결하는가?

현재 분석 파이프라인 상태 관리에 4가지 구조적 문제가 있다:

1. **언어/비언어 부분 실패 미식별**: `analysisStatus` 하나로 관리 → Vision만 실패해도 COMPLETED → 부분 재시도 불가
2. **상태 필드 산재**: 분석 상태는 QuestionSet에, 변환 상태는 FileMetadata에 → "전체 완료" 판정이 어려움
3. **Status/Progress 이중 관리**: `analysisStatus` + `analysisProgress` 두 필드의 역할 구분 불명확. 클라이언트 입장에서 혼란
4. **레거시 잔재**: AnalysisProgress에 Gemini 전환 후 미사용 값 3개, FileStatus에 영상 특화 네이밍(CONVERTING/CONVERTED)

### 2. Goal — 구체적인 결과물과 성공 기준

- `QuestionSetAnalysis` 별도 테이블로 분석/변환 상태를 통합 관리
- `analysisStatus`에 Progress를 흡수하여 단일 상태 필드로 운영
- `isVerbalCompleted` / `isNonverbalCompleted`로 부분 실패 식별 + 부분 재시도
- `convertStatus`로 변환 상태를 분석과 함께 한 곳에서 추적
- FileMetadata는 업로드 여부만 관리 (순수 파일 메타)
- `isFullyReady()` 도메인 메서드로 "분석 + 변환 모두 완료" 판정

| 지표 | 현재 | 목표 |
|------|------|------|
| QuestionSet 상태 필드 | 4개 | 0개 (QuestionSetAnalysis로 이동) |
| 부분 실패 식별 | 불가 | 가능 (isVerbalCompleted / isNonverbalCompleted) |
| 전체 완료 판정 | 2개 엔티티 조합 | 1개 메서드 (isFullyReady) |
| Status + Progress 필드 | 2개 | 1개 (Status에 Progress 흡수) |

### 3. Evidence — 근거 데이터

- `handler.py:183-184` — Gemini 전체 실패 시만 폴백, 개별 Vision 실패는 None으로 처리 후 COMPLETED
- `AnalysisProgress.java` — 8개 값 중 Gemini 경로에서 4개만 사용 (STARTED, EXTRACTING, ANALYZING, FINALIZING)
- `FileStatus.java` — CONVERTING/CONVERTED는 VIDEO 타입에만 해당. RESUME은 UPLOADED에서 끝남
- `QuestionSetStatusResponse.java` — analysisStatus + analysisProgress + fileStatus를 각각 내려주고, 클라이언트가 조합해서 판단

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 대안 제외 이유 |
|------|------|----------------|
| 별도 테이블 (QuestionSetAnalysis) | QuestionSet에 필드 추가 | 상태 필드가 6개로 늘어남, 관심사 미분리 |
| boolean (isVerbalCompleted) | Enum (AnalysisResult) | 3값(NONE/COMPLETED/FAILED)이면 "진행 중"을 표현해야 하는데, 진행 중은 analysisStatus로 이미 추적. boolean이 단순 |
| Status에 Progress 흡수 | 두 필드 유지 | 클라이언트 혼란, 이중 관리 부담. 하나로 합치면 단순해짐 |
| convertStatus를 QuestionSetAnalysis에 | FileMetadata에 유지 | 분석+변환 상태를 한 곳에서 보고 싶음. "전체 완료" 판정이 단순해짐 |

## 아키텍처 / 설계

### 변경 전 (As-Is)

```
QuestionSet                      FileMetadata
├── analysisStatus (6값)          ├── status (5값: PENDING~CONVERTED)
├── analysisProgress (8값)        ├── failureReason
├── failureReason                └── failureDetail
└── failureDetail

→ "전체 완료" = QuestionSet.analysisStatus == COMPLETED && FileMetadata.status == CONVERTED
→ 2개 엔티티를 조합해야 판정 가능
```

### 변경 후 (To-Be)

```
QuestionSet                      QuestionSetAnalysis (1:1)         FileMetadata
└── (상태 필드 없음)               ├── analysisStatus (9값, 통합)     ├── status (3값: PENDING/UPLOADED/FAILED)
                                 ├── convertStatus (4값)           └── (순수 파일 메타만)
                                 ├── isVerbalCompleted (boolean)
                                 ├── isNonverbalCompleted (boolean)
                                 ├── failureReason
                                 └── failureDetail

→ "전체 완료" = QuestionSetAnalysis.isFullyReady()
→ 1개 엔티티, 1개 메서드로 판정
```

### Enum 설계

**AnalysisStatus (Status + Progress 통합)**
```
PENDING → PENDING_UPLOAD → EXTRACTING → ANALYZING → FINALIZING → COMPLETED / PARTIAL / FAILED
                                                                     ↓
                                                                  SKIPPED
```

| 값 | 의미 | canTransitionTo |
|----|------|-----------------|
| PENDING | 초기 상태 | PENDING_UPLOAD, SKIPPED, FAILED |
| PENDING_UPLOAD | 업로드 대기 | EXTRACTING, FAILED |
| EXTRACTING | FFmpeg 추출 중 | ANALYZING, FAILED |
| ANALYZING | AI 분석 중 | FINALIZING, FAILED |
| FINALIZING | 피드백 생성 중 | COMPLETED, PARTIAL, FAILED |
| COMPLETED | 전체 완료 | FAILED |
| PARTIAL | 부분 완료 | EXTRACTING, COMPLETED, FAILED |
| FAILED | 실패 | EXTRACTING, COMPLETED |
| SKIPPED | 건너뜀 | (종료) |

**ConvertStatus (신규)**

| 값 | 의미 | canTransitionTo |
|----|------|-----------------|
| PENDING | 변환 대기 | PROCESSING, FAILED |
| PROCESSING | 변환 중 | COMPLETED, FAILED |
| COMPLETED | 변환 완료 | FAILED |
| FAILED | 실패 | PROCESSING |

**FileStatus (축소)**

| 값 | 의미 | canTransitionTo |
|----|------|-----------------|
| PENDING | 초기 상태 | UPLOADED, FAILED |
| UPLOADED | S3 업로드 완료 | FAILED |
| FAILED | 업로드 실패 | UPLOADED |

### 집계 판정 로직

```java
// QuestionSetAnalysis 도메인 메서드
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

public boolean isFullyReady() {
    boolean analysisOk = analysisStatus == COMPLETED || analysisStatus == PARTIAL;
    boolean convertOk = convertStatus == ConvertStatus.COMPLETED;
    return analysisOk && convertOk;
}
```

## Scope

### In

- QuestionSetAnalysis 엔티티 + 테이블 신규 생성
- ConvertStatus enum 신규 생성
- AnalysisStatus에 EXTRACTING, ANALYZING, FINALIZING, PARTIAL 추가
- AnalysisProgress enum 삭제
- FileStatus에서 CONVERTING/CONVERTED 제거
- QuestionSet에서 상태 필드 4개 제거
- DB 마이그레이션 (데이터 이관)
- InternalQuestionSetService 전체 수정 (updateProgress, saveFeedback, retryAnalysis)
- InternalQuestionSetController 엔드포인트 수정
- AnalysisScheduler 좀비 감지 수정
- Lambda analysis/convert 상태 문자열 변경
- FE 타입 정의 + 폴링 훅 + 분석 대기 UI + 피드백 뷰어 수정

### Out

- Vision 폴백 (Gemini Vision) — 별도 태스크로 분리
- 답변 레벨 부분 재시도 — 현재 QuestionSet 단위 재시도로 충분
- 분산 트레이싱 — 별도 스코프
- FE 전면 리디자인 — 기존 UI에 PARTIAL 상태만 추가

## 제약조건 / 환경

- Java 21 + Spring Boot 3.4 + JPA + MySQL 8.0
- Lambda (Python 3.12)에서 Internal API로 상태 업데이트 (문자열 → Enum 자동 변환)
- 기존 canTransitionTo() 패턴 유지
- 기존 데이터 하위 호환 (마이그레이션으로 처리)
- QuestionSet ↔ FileMetadata 1:1 관계 유지
- QuestionSet ↔ QuestionSetAnalysis 1:1 관계 신규
- Flyway 마이그레이션 사용
- FE는 React 18 + TypeScript + TanStack Query

## 영향 범위 요약

### Backend (15+ 파일)

| 영향도 | 파일 | 변경 |
|:---:|------|------|
| CRITICAL | `AnalysisProgress.java` | 삭제 |
| CRITICAL | `QuestionSet.java` | 상태 필드 4개 제거, QuestionSetAnalysis 연관 추가 |
| CRITICAL | `InternalQuestionSetService.java` | updateProgress/saveFeedback/retryAnalysis 재작성 |
| CRITICAL | `FileStatus.java` | CONVERTING/CONVERTED 제거 |
| HIGH | `AnalysisStatus.java` | EXTRACTING/ANALYZING/FINALIZING/PARTIAL 추가 |
| HIGH | `InternalQuestionSetController.java` | /progress 엔드포인트 수정 |
| HIGH | `UpdateProgressRequest.java` | AnalysisProgress → AnalysisStatus 변경 |
| HIGH | `QuestionSetStatusResponse.java` | analysisProgress 제거, 새 필드 추가 |
| HIGH | `AnalysisScheduler.java` | detectFileConvertingZombies 삭제/재작성 |
| HIGH | `SaveFeedbackRequest.java` | isVerbalCompleted/isNonverbalCompleted 추가 |
| MEDIUM | `InterviewCompletionService.java` | QuestionSetAnalysis 참조로 변경 |
| MEDIUM | `InternalFileService.java` | CONVERTING/CONVERTED 참조 제거 |
| HIGH | 테스트 6+ 파일 | 전체 수정 |

### Lambda (4 파일)

| 영향도 | 파일 | 변경 |
|:---:|------|------|
| HIGH | `lambda/analysis/handler.py` | progress 값 변경, save_feedback payload 추가 |
| MEDIUM | `lambda/analysis/api_client.py` | update_progress 파라미터 변경 |
| HIGH | `lambda/convert/handler.py` | CONVERTING→PROCESSING, CONVERTED→COMPLETED, API 엔드포인트 변경 |
| MEDIUM | `lambda/convert/api_client.py` | update_file_status → convertStatus 업데이트로 변경 |

### Frontend (6+ 파일)

| 영향도 | 파일 | 변경 |
|:---:|------|------|
| HIGH | `types/interview.ts` | AnalysisStatus 확장, FileStatus 축소, AnalysisProgress 제거, 새 타입 추가 |
| HIGH | `pages/interview-analysis-page.tsx` | PROGRESS_STEPS 재작성, 상태 조건 분기, PARTIAL 처리 |
| MEDIUM | `pages/interview-feedback-page.tsx` | statusConfig에 PARTIAL 추가, 재시도 조건 |
| MEDIUM | `hooks/use-question-sets.ts` | 폴링 응답 타입 변경 |
| LOW | `components/feedback/feedback-panel.tsx` | 영향 최소 |
| LOW | `components/feedback/timeline-bar.tsx` | 영향 최소 |
