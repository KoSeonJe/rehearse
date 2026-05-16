# 영상 분석 파이프라인 상태 관리

> 인터뷰 답변 영상이 업로드 → 변환 → 분석 → 피드백 저장 → 세션 합성으로 진행되는 동안 어떤 상태가 어디서 바뀌는지 정리한 문서.
> 코드 변경 시 본 문서도 같이 갱신.

---

## 1. 파이프라인 개요

```
[브라우저 녹화]
   │  MediaRecorder (WebM)
   ▼
[FE → S3 PUT (presigned URL)]
   │  key: interviews/raw/v1/{interviewId}/{questionSetId}/{correlationId}.webm
   ▼
[S3 ObjectCreated → EventBridge]
   │
   ├─────────────────────────┬──────────────────────────┐
   ▼                         ▼                          ▼
[convert Lambda]       [analysis Lambda]          (file 메타 갱신)
 MediaConvert            Gemini / Vision / Whisper
 WebM → MP4              verbal + nonverbal 분석
   │                         │
   ▼                         ▼
 convert-status          progress (EXTRACTING → ANALYZING → FINALIZING)
 PROCESSING → COMPLETED  → feedback POST (verbal/nonverbal completed)
   │                         │
   └──────────┬──────────────┘
              ▼
  [모든 QuestionSet isResolved()]
   → DeliveryEnrichmentRequestedEvent
   → SessionFeedback: PRELIMINARY → COMPLETE
```

핵심 원칙:
- **Lambda 는 이벤트 구동**. Backend 가 호출하지 않음. Backend 는 결과 수신자.
- **상태 전이는 enum 메서드 `canTransitionTo` 가 강제**. 불법 전이 시 예외.
- **낙관락 (`@Version`) + 재시도 3회 / 100ms backoff** — Lambda 동시 콜백 충돌 대응.
- **convert / analysis 는 독립 트랙** — 한 쪽 실패가 다른 쪽 진행 막지 않음.

---

## 2. 상태 enum 인벤토리

### 2.1 `AnalysisStatus` — 분석 진행 상태

`backend/src/main/java/com/rehearse/api/domain/question/entity/AnalysisStatus.java`

| 값 | 의미 |
|----|------|
| `PENDING` | QuestionSet 생성 직후. 파일 업로드 전 |
| `PENDING_UPLOAD` | 업로드 URL 발급 후 / S3 도착 대기 |
| `EXTRACTING` | Lambda 가 ffmpeg 로 frame / audio 추출 중 |
| `ANALYZING` | Gemini audio + Vision frame + Whisper STT 실행 중 |
| `FINALIZING` | LLM 합성 (rubric 채점 / 피드백 작성) 진행 중 |
| `COMPLETED` | verbal + nonverbal 모두 성공 |
| `PARTIAL` | 일부 트랙만 성공 (예: verbal OK, nonverbal 실패) |
| `FAILED` | 전체 실패. 재시도 가능 |
| `SKIPPED` | 사용자가 건너뛴 질문. 종착 상태. 재시도 불가 |

전이 규칙 (`canTransitionTo`):
```
PENDING        → PENDING_UPLOAD | EXTRACTING | SKIPPED | FAILED
PENDING_UPLOAD → EXTRACTING | FAILED
EXTRACTING     → ANALYZING | FAILED
ANALYZING      → FINALIZING | FAILED
FINALIZING     → COMPLETED | PARTIAL | FAILED
COMPLETED      → FAILED                       // 사후 검증 실패 한정
PARTIAL        → EXTRACTING | COMPLETED | FAILED   // 재시도
FAILED         → EXTRACTING | COMPLETED
SKIPPED        → (없음, 종착)
```

판정 헬퍼:
- `isResolved()` — `COMPLETED | PARTIAL | SKIPPED`. Lambda 더 이상 작업 불필요. 세션 합성 트리거 기준.
- `isFullyCompleted()` — `COMPLETED` 만.
- `isRetryable()` — `FAILED | PARTIAL`. 재시도 API 진입 조건.
- `hasAnalysisResult()` — `COMPLETED | PARTIAL`. 피드백 데이터 존재.
- `isInProgress()` — `EXTRACTING | ANALYZING | FINALIZING`. 좀비 감지 대상.

### 2.2 `ConvertStatus` — 영상 변환 상태

`backend/src/main/java/com/rehearse/api/domain/question/entity/ConvertStatus.java`

| 값 | 의미 |
|----|------|
| `PENDING` | MediaConvert job 미개시 |
| `PROCESSING` | MediaConvert WebM → MP4 진행 |
| `COMPLETED` | MP4 인코딩 완료. `streamingS3Key` 저장됨 |
| `FAILED` | MediaConvert 실패 |

전이:
```
PENDING    → PROCESSING | FAILED
PROCESSING → COMPLETED | FAILED
COMPLETED  → FAILED                  // 사후 무결성 검증 실패 한정
FAILED     → PROCESSING               // 재시도
```

### 2.3 `FileStatus` — 원본 파일 업로드 상태

`backend/src/main/java/com/rehearse/api/domain/file/entity/FileStatus.java`

| 값 | 의미 |
|----|------|
| `PENDING` | 업로드 URL 발급 / S3 PUT 대기 |
| `UPLOADED` | S3 ObjectCreated 확인됨 (convert Lambda 가 마킹) |
| `FAILED` | 업로드 실패 / 검증 실패 |

전이:
```
PENDING  → UPLOADED | FAILED
UPLOADED → FAILED
FAILED   → UPLOADED                   // 재업로드 대응
```

### 2.4 `SessionFeedbackStatus` — 세션 레벨 합성 상태

`backend/src/main/java/com/rehearse/api/domain/feedback/session/entity/SessionFeedbackStatus.java`

| 값 | 의미 |
|----|------|
| `PRELIMINARY` | 모든 QuestionSet `isResolved()`. 텍스트 기반 합성 (strengths / gaps / week plan) 완료. delivery (비언어) enrichment 대기 |
| `COMPLETE` | delivery enrichment 합성 완료 또는 watchdog timeout 으로 종착 |

---

## 3. API ↔ 상태 변경 매트릭스

### 3.1 사용자 (FE) → Backend (`/api/v1`)

| API | 트리거 | 변경 대상 | 전이 |
|-----|--------|----------|------|
| `POST /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/upload-url` | FE 가 녹화 종료 후 호출 | `FileMetadata.status` | (생성 시) `PENDING` |
| (S3 PUT, presigned) | FE 가 영상 직접 업로드 | — (S3 만) | — |
| `POST /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/retry-analysis` | 사용자가 실패 후 재시도 | `QuestionSetAnalysis.analysisStatus` | `FAILED|PARTIAL → EXTRACTING`, `isVerbal/NonverbalCompleted` 리셋, `failureReason/Detail` clear. AFTER_COMMIT 콜백으로 S3 CopyObject 트리거 → EventBridge 재발화 |
| `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/status` | FE 폴링 | (read-only) | — |
| `GET /api/v1/interviews/{interviewId}/question-sets/{questionSetId}/feedback` | `hasAnalysisResult()` 이후 | (read-only) | — |

컨트롤러: `backend/src/main/java/com/rehearse/api/domain/question/controller/QuestionSetController.java`.

### 3.2 Lambda → Backend Internal (`/api/internal`)

인증: `X-Internal-Api-Key` 헤더. 외부 노출 금지.

| API | 호출자 | 페이로드 핵심 필드 | 변경 대상 | 부수 효과 |
|-----|--------|------------------|----------|----------|
| `PUT /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/progress` | analysis Lambda | `status` (`EXTRACTING` / `ANALYZING` / `FINALIZING` / `FAILED` / `SKIPPED`), `failureReason`, `failureDetail` | `QuestionSetAnalysis.analysisStatus` (전이 검증 후) | 동일 상태로 호출 시 no-op. 낙관락 충돌 시 3회 재시도 |
| `GET /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/answers` | analysis Lambda | — | (read-only) | Lambda 가 입력 컨텍스트 + 현재 status 조회. `COMPLETED` 면 스킵 (멱등성) |
| `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback` | analysis Lambda | `feedbacks[]` (timestamp / score / frameFeedback), `verbalCompleted`, `nonverbalCompleted` | `QuestionSetFeedback` insert + `QuestionSetAnalysis.{analysisStatus, isVerbalCompleted, isNonverbalCompleted}` | 트랜잭션 1개로 묶음. status = `completeAnalysis(verbal, nonverbal)` → 둘 다 true=`COMPLETED` / 둘 다 false=`FAILED` / 혼합=`PARTIAL`. 인터뷰의 모든 QS `isResolved()` 면 `DeliveryEnrichmentRequestedEvent` 발행 (AFTER_COMMIT) |
| `PUT /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/convert-status` | convert Lambda | `status` (`PROCESSING` / `COMPLETED` / `FAILED`), `streamingS3Key` (COMPLETED 시), `failureReason` | `QuestionSetAnalysis.convertStatus`, `FileMetadata.streamingS3Key` (COMPLETED 시) | 낙관락 3회 재시도 |
| `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/retry-analysis` | 내부 운영 / FE 우회 경로 동일 진입 | — | `QuestionSetAnalysis.analysisStatus` `→ EXTRACTING` | AFTER_COMMIT S3 CopyObject |
| `PUT /api/internal/files/{fileMetadataId}/status` | convert Lambda (업로드 확인 직후) | `status` (`UPLOADED` / `FAILED`) | `FileMetadata.status` | 전이 검증 |
| `POST /api/internal/files/{fileMetadataId}/retry-convert` | 운영 재처리 | — | (재처리 트리거) | S3 재발화 |
| `GET /api/internal/files/by-s3-key?key=...` | convert Lambda | — | (read-only) | Lambda 가 file 메타 조회 |

컨트롤러: `InternalQuestionSetController.java` / `FeedbackController.java` / `InternalFileController.java`.

### 3.3 Backend 내부 이벤트 (도메인 간)

| 이벤트 | 발행 시점 | 수신 / 효과 |
|-------|----------|------------|
| `DeliveryEnrichmentRequestedEvent` | 인터뷰 내 **모든** QS `isResolved()` 도달 (feedback 저장 트랜잭션 AFTER_COMMIT) | SessionFeedback 합성: `PRELIMINARY` → delivery enrichment 적용 → `COMPLETE` |
| Watchdog 발화 | `SessionFeedback` 가 정해진 시간 내 `COMPLETE` 미도달 | `lastFailureReason=TIMEOUT` 으로 `COMPLETE` 강제 종착, `deliveryRetryable=true` 마킹 |

---

## 4. 상태 흐름 다이어그램

### 4.1 정상 흐름 (단일 질문)

```
[QS 생성]
   └─ analysisStatus=PENDING, convertStatus=PENDING, fileStatus=(생성 시 PENDING)

[FE upload-url 호출 + S3 PUT]
   └─ (변화 없음 — S3 만)

[S3 ObjectCreated → EventBridge]
   ├──► convert Lambda
   │     ├─ PUT /files/{id}/status → fileStatus=UPLOADED
   │     ├─ PUT /convert-status (PROCESSING) → convertStatus=PROCESSING
   │     ├─ MediaConvert 작업
   │     └─ PUT /convert-status (COMPLETED, streamingS3Key) → convertStatus=COMPLETED
   │
   └──► analysis Lambda
         ├─ GET /answers (현재 상태 + 컨텍스트 조회)
         ├─ PUT /progress (EXTRACTING) → analysisStatus=EXTRACTING
         ├─ ffmpeg 추출
         ├─ PUT /progress (ANALYZING) → analysisStatus=ANALYZING
         ├─ Gemini / Vision / Whisper 분석
         ├─ PUT /progress (FINALIZING) → analysisStatus=FINALIZING
         ├─ LLM 합성
         └─ POST /feedback (verbal=true, nonverbal=true)
                → analysisStatus=COMPLETED, feedback row insert
                → 모든 QS isResolved() 면 DeliveryEnrichmentRequestedEvent 발행

[SessionFeedback 합성]
   └─ PRELIMINARY → COMPLETE
```

### 4.2 실패 / 재시도 흐름

```
EXTRACTING/ANALYZING/FINALIZING 중 예외
   └─ PUT /progress (FAILED, failureReason, failureDetail) → analysisStatus=FAILED

verbal OK + nonverbal 실패
   └─ POST /feedback (verbal=true, nonverbal=false) → analysisStatus=PARTIAL (피드백 일부 저장)

[사용자 재시도 API: POST /retry-analysis]
   ├─ isRetryable() 검증 (FAILED | PARTIAL 만 통과)
   ├─ analysisStatus=EXTRACTING, verbal/nonverbal=false, failureReason clear
   └─ AFTER_COMMIT: S3 CopyObject (same key, REPLACE metadata) → ObjectCreated 재발화 → analysis Lambda 재진입
```

### 4.3 SKIPPED

```
사용자가 질문 건너뛰기 (UI)
   └─ analysisStatus=SKIPPED (PENDING 에서만 진입 가능)
   └─ 분석 / 피드백 없음. isResolved()=true 이므로 세션 합성에는 포함
```

---

## 5. Lambda 측 호출 순서 (참조)

### 5.1 `lambda/analysis/handler.py`

1. S3 key 파싱 → `interviewId`, `questionSetId`, `correlationId`
2. `GET /answers` — 답변 메타 + 현재 `analysisStatus` 조회
3. `analysisStatus == COMPLETED` → 즉시 종료 (멱등성)
4. `PUT /progress` `EXTRACTING`
5. ffmpeg: audio (wav) + frames (jpg N장) 추출 → /tmp
6. `PUT /progress` `ANALYZING`
7. 병렬: Gemini 오디오 통합 분석 + Vision 프레임 분석 + (필요 시) Whisper STT fallback
8. `PUT /progress` `FINALIZING`
9. LLM 합성 (rubric 채점 / 타임스탬프 피드백 작성)
10. `POST /feedback` (verbal / nonverbal 플래그 + 피드백 array)
11. 예외 발생 시 마지막 단계에서 `PUT /progress` `FAILED` (`failureReason` = 발생 위치, `failureDetail` = stack trace 요약)

### 5.2 `lambda/convert/handler.py`

1. S3 key 파싱 + bucket / key 추출
2. `GET /api/internal/files/by-s3-key?key=...` → `fileMetadataId`
3. `PUT /api/internal/files/{id}/status` `UPLOADED`
4. `PUT /convert-status` `PROCESSING`
5. MediaConvert job 생성 + 완료 대기
6. 성공: `PUT /convert-status` `COMPLETED` + `streamingS3Key`
7. 실패: `PUT /convert-status` `FAILED` + `failureReason`

---

## 6. Frontend 상태 소비

### 6.1 폴링

`useQuestionSetStatus(interviewId, questionSetId)` — `GET /status` 3s 폴링.

응답 (`QuestionSetStatusResponse`):
```
{
  id, analysisStatus, convertStatus, fileStatus,
  isVerbalCompleted, isNonverbalCompleted,
  fullyReady,        // analysisStatus.hasAnalysisResult() && convertStatus == COMPLETED
  failureReason
}
```

### 6.2 피드백 로드

`GET /feedback` — `analysisStatus ∈ {COMPLETED, PARTIAL}` 진입 시 1회. staleTime=Infinity (수동 갱신).

### 6.3 세션 합성

`GET /session-feedback` — 모든 QS `isResolved()` 후 1회 fetch. 폴링 없음 (~10–30s 합성 시간이므로 사용자 액션 시 fetch).

### 6.4 UI 매핑

`frontend/src/constants/analysis-progress.ts` (대략):
```
PENDING_UPLOAD → "대기"
EXTRACTING     → "추출"
ANALYZING      → "분석"
FINALIZING     → "생성"
COMPLETED      → 결과 화면 전환
PARTIAL        → 부분 결과 + 재시도 버튼
FAILED         → 실패 표시 + 재시도 버튼
SKIPPED        → 건너뜀 표시
```

---

## 7. 동시성 / 멱등성

| 위험 | 방어 |
|------|------|
| Lambda 가 같은 status 로 중복 PUT | `updateProgress` 가 동일 상태면 no-op |
| convert / analysis Lambda 가 동시에 같은 row 갱신 | `@Version` 낙관락 + 3회 재시도 (100ms backoff) |
| 재시도 API 중복 호출 | `isRetryable()` 가드. 이미 `EXTRACTING` 진행 중이면 차단 |
| S3 PUT 가 EventBridge 2번 발화 | 재진입 시 `analysisStatus == COMPLETED` 체크로 스킵 |
| 분석 진행 중 사용자 화면 이탈 | Lambda 는 FE 와 무관하게 진행 — FE 재진입 시 폴링이 현재 상태 픽업 |

---

## 8. 좀비 / Stale 처리

- `AnalysisStatus.isInProgress()` (`EXTRACTING | ANALYZING | FINALIZING`) = Lambda 가 진행 중이라 가정되는 상태. Lambda 가 죽으면 영구 stuck.
- `inProgressStatuses()` 헬퍼로 배치 쿼리 가능 (좀비 감지 워치독 진입점).
- `SessionFeedback` watchdog: 모든 QS `isResolved()` 도달 못 한 채 정해진 시간 경과 → `COMPLETE` 로 강제 종착 + `deliveryRetryable=true`.

---

## 9. 엔티티 관계 요약

```
Interview
  ├─ QuestionSet[1..N]
  │    ├─ QuestionSetAnalysis (1:1)
  │    │    ├─ analysisStatus       [AnalysisStatus]
  │    │    ├─ convertStatus        [ConvertStatus]
  │    │    ├─ isVerbalCompleted    [boolean]
  │    │    ├─ isNonverbalCompleted [boolean]
  │    │    ├─ failureReason / failureDetail
  │    │    └─ @Version
  │    ├─ FileMetadata (1:1)
  │    │    ├─ status               [FileStatus]
  │    │    ├─ s3Key                (raw WebM)
  │    │    └─ streamingS3Key       (MP4, convert Lambda 가 설정)
  │    └─ QuestionSetFeedback (1:1) — TimestampFeedback[N]
  └─ SessionFeedback (1:1)
       ├─ status                    [SessionFeedbackStatus]
       └─ overall / strengths / gaps / delivery / weekPlan (JSON)
```

---

## 10. 참고 파일

- `backend/src/main/java/com/rehearse/api/domain/question/entity/AnalysisStatus.java`
- `backend/src/main/java/com/rehearse/api/domain/question/entity/ConvertStatus.java`
- `backend/src/main/java/com/rehearse/api/domain/file/entity/FileStatus.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/entity/SessionFeedbackStatus.java`
- `backend/src/main/java/com/rehearse/api/domain/question/controller/QuestionSetController.java`
- `backend/src/main/java/com/rehearse/api/domain/question/controller/InternalQuestionSetController.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/controller/FeedbackController.java`
- `backend/src/main/java/com/rehearse/api/domain/file/controller/InternalFileController.java`
- `backend/src/main/java/com/rehearse/api/domain/question/service/InternalQuestionSetService.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/FeedbackService.java`
- `lambda/analysis/handler.py`
- `lambda/convert/handler.py`
