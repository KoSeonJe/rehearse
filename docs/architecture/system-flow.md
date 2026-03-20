# 리허설(Rehearse) — 시스템 상호작용 흐름도

> 홈페이지부터 종합 리포트까지, 클라이언트·서버·인프라 간 전체 상호작용 문서
>
> 최종 업데이트: 2026-03-20

---

## 전체 아키텍처 개요

```
┌──────────────────┐          ┌──────────────────┐
│ 클라이언트 (Vercel) │          │ AI 서비스          │
│ React 18 + TS     │          │ Claude API        │
│ Zustand           │          │ (질문·후속질문·리포트) │
│ TanStack Query    │          │ OpenAI API        │
└────────┬─────────┘          │ (Whisper·Vision·LLM)│
         │ REST API            └────────┬─────────┘
         ▼                              │
┌──────────────────┐                    │
│ API 서버 (EC2)    │◄──────────────────┘
│ Spring Boot 3.4   │
│ Java 21           │
└──┬──────┬────────┘
   │ JPA  │ Internal API
   ▼      ▼
┌──────┐ ┌────────────────────────────────────────┐
│ RDS  │ │ AWS 인프라                               │
│MySQL │ │ S3 → EventBridge →┬→ Analysis Lambda    │
└──────┘ │    (WebM/MP4)     │   → OpenAI API      │
         │       ▲           └→ Convert Lambda     │
         │  Presigned PUT       → MediaConvert → S3│
         └────────────────────────────────────────┘
```

**주요 기술 스택**: React 18 + Vite + Tailwind | Spring Boot 3.4 + JPA | Claude API (질문·후속질문·리포트) | OpenAI API (STT·Vision·LLM 분석) | S3 + EventBridge + Lambda + MediaConvert | MySQL 8.0

**핵심 설계 원칙**:
- DB 접근은 API 서버만 수행 (Lambda는 Internal API 호출)
- S3 이벤트 기반 자동 트리거 (API 서버가 Lambda를 직접 호출하지 않음)
- 질문 세트 단위 녹화 + 즉시 업로드로 분석 지연 최소화

---

## Phase 1: 홈 + 온보딩 (`/`)

정적 페이지. API 호출 없음.

- 서비스 소개, CTA 버튼 → `/interview/setup`으로 네비게이션

---

## Phase 2: 면접 Setup (`/interview/setup`)

4단계 위저드를 통해 면접 설정 후 세션을 생성한다.

```
사용자 → FE: 4단계 위저드 입력
  Step 1: 포지션 (BACKEND/FRONTEND/DEVOPS/DATA_ENGINEER/FULLSTACK)
  Step 2: 레벨 (JUNIOR/MID/SENIOR)
  Step 3: 면접 시간 (15/30/45/60분 프리셋, 5~120분 범위)
  Step 4: 면접 유형 선택 + 이력서 업로드 (RESUME_BASED 시)

FE → BE: POST /api/v1/interviews (multipart: request JSON + resumeFile PDF)
  BE: ① PDF 텍스트 추출 (Apache PDFBox)
  BE → DB: ② Interview 엔티티 저장 (status=READY, questionGenerationStatus=PENDING)
  BE → FE: 201 Created (interviewId)
  BE: ③ QuestionGenerationRequestedEvent 발행 (트랜잭션 커밋 후)
FE: /interview/{id}/ready로 이동

--- 비동기 처리 (@Async("questionGenerationExecutor") + @TransactionalEventListener(AFTER_COMMIT)) ---
  BE: questionGenerationStatus = GENERATING
  BE → Claude API: 질문 생성 호출 (포지션+레벨+유형+이력서 텍스트)
  Claude → BE: 질문 세트 목록 (각 질문세트: 원본질문 1 + 후속질문 3 + 모범답변)
  BE → DB: QuestionSet + Question 엔티티 저장
  BE: questionGenerationStatus = COMPLETED
```

**API 상세**:
- `POST /api/v1/interviews` (multipart/form-data)
  - `request` 파트: `{ position, level, interviewTypes[], durationMinutes, csSubTopics?[] }`
  - `resumeFile` 파트: PDF (optional, max 10MB)
- 질문 수 계산: `round(durationMinutes / 5)` (최소 2, 최대 24)
- 질문 생성은 비동기: `@Async("questionGenerationExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`
- Claude 모델: `claude-sonnet-4-20250514`, maxTokens=4096, temperature=0.9 (다양하고 자연스러운 질문 생성을 위해 높은 temperature 사용)

---

## Phase 3: 면접 준비 (`/interview/:id/ready`)

질문 생성 완료를 대기하고, 장치 테스트 후 면접을 시작한다.

```
[폴링: 2초 간격, PENDING/GENERATING일 때]
FE → BE: GET /api/v1/interviews/{id}
BE → FE: InterviewResponse (questionGenerationStatus, questionSets[])
→ COMPLETED 시 폴링 중단, 질문 표시

사용자: 카메라/마이크/스피커 테스트 (getUserMedia + AudioContext, 브라우저 로컬)

사용자: "면접 시작" 클릭
FE → BE: PATCH /api/v1/interviews/{id}/status { status: "IN_PROGRESS" }
  BE → DB: status: READY → IN_PROGRESS
FE: /interview/{id}/conduct로 이동
```

**실패 처리**:
- 질문 생성 실패(FAILED) → `POST /api/v1/interviews/{id}/retry-questions` → 재생성

---

## Phase 4: 면접 진행 (`/interview/:id/conduct`)

질문세트 단위로 녹화·답변 제출·S3 업로드를 반복한다.

### 4-1. 전체 면접 진행 시퀀스

```
[동시 활성화: MediaRecorder(WebM/VP9 2.5Mbps), AudioCapture(opus 128kbps), TTS]

FE → 사용자: TTS "안녕하세요, 면접을 시작하겠습니다"

┌── 각 질문세트 (N개) 반복 ──────────────────────────────────────┐
│ FE → 사용자: TTS 원본 질문 읽기                                 │
│ [MediaRecorder 녹화 시작 — 질문세트 단위, 중간 일시정지 없음]     │
│                                                                │
│ ┌── 답변 루프 (원본 1 + 후속 3 = 4라운드) ──────────────────┐   │
│ │ 사용자 → FE: 답변 (음성)                                  │   │
│ │ FE: 사용자 수동 종료 → 답변 종료, 타임스탬프 기록            │   │
│ │                                                           │   │
│ │ [후속질문 라운드 - 최대 3회]                                │   │
│ │ FE → BE: POST /interviews/{id}/follow-up (multipart)      │   │
│ │   request 파트: { questionSetId, questionContent,          │   │
│ │                   answerText, nonVerbalSummary,            │   │
│ │                   previousExchanges[] }                    │   │
│ │   audio 파트: WebM 오디오 (optional)                       │   │
│ │ BE: (audio 있으면) OpenAI Whisper STT → answerText 보강    │   │
│ │ BE → Claude: 후속질문 생성 (maxTokens=1024)                │   │
│ │ BE → FE: FollowUpResponse (question, reason, type)        │   │
│ │ FE → 사용자: TTS 후속질문 읽기                             │   │
│ │ 사용자 → FE: 답변 (음성)                                  │   │
│ └───────────────────────────────────────────────────────────┘   │
│                                                                │
│ [MediaRecorder 녹화 종료]                                      │
│                                                                │
│ [병렬 처리]                                                    │
│ ├─ FE → BE: POST .../question-sets/{qsId}/answers             │
│ │   (타임스탬프 4개: MAIN + FOLLOWUP 1~3)                      │
│ │   BE: QuestionAnswer(questionId, startMs, endMs) 저장        │
│ │   BE: analysisStatus = PENDING_UPLOAD                        │
│ └─ FE → BE: POST .../question-sets/{qsId}/upload-url          │
│    BE → FE: { uploadUrl, s3Key, fileMetadataId }               │
│    FE → S3: PUT (WebM 영상 직접 업로드, 3회 재시도)             │
│    FE: IndexedDB에 Blob 백업 → 업로드 성공 시 삭제              │
│                                                                │
│ [다음 질문세트로 이동, 업로드·분석은 백그라운드]                 │
└────────────────────────────────────────────────────────────────┘

FE → BE: POST /interviews/{id}/skip-remaining (미응답 질문세트 SKIPPED)
FE → BE: PATCH /interviews/{id}/status { status: "COMPLETED" }
FE: /interview/{id}/analysis로 이동
```

### 4-2. Zustand 상태 머신 (InterviewPhase)

```
[*] → preparing → greeting → ready → recording → paused → finishing → completed → [*]
                                  ↑               │
                                  └───────────────┘
                                  (다음 질문 답변 시작)
```

| 전이 | 트리거 |
|------|--------|
| preparing → greeting | 면접 초기화 완료 |
| greeting → ready | 인사 TTS 완료 |
| ready → recording | 답변 시작 (수동) |
| recording → paused | 답변 종료 (수동) |
| paused → recording | 다음 질문 답변 시작 |
| paused → finishing | 마지막 답변 완료 또는 시간 만료 |
| finishing → completed | 업로드 완료, 상태 업데이트 |

### 4-3. 브라우저 API 활용

| 기술 | 역할 | 데이터 |
|------|------|--------|
| MediaRecorder | 질문세트 단위 영상 녹화 | WebM/VP9 2.5Mbps Blob |
| AudioCapture | 병렬 오디오 전용 녹음 | opus 128kbps |
| ~~Web Speech API~~ | ~~실시간 음성→텍스트~~ | 제거됨 — STT는 Lambda(OpenAI Whisper)에서 처리 |
| TTS (SpeechSynthesis) | 질문 음성 출력 | — |

---

## Phase 5: 비동기 분석 파이프라인 (서버사이드)

S3에 영상이 업로드되면 EventBridge를 통해 2개 Lambda가 동시 트리거된다.

### 인증 & 추적

Lambda → API 서버 호출 시 `X-Internal-Api-Key` 헤더 필수.
- 헤더명: `X-Internal-Api-Key`
- 값: 환경변수 `INTERNAL_API_KEY` (config: `internal.api-key`)
- 인증 실패 시: 401 `{ success: false, code: "AUTH_001", message: "유효하지 않은 내부 API 키입니다." }`

**Correlation ID**: Lambda handler 진입 시 고유 ID 생성 → 모든 Internal API 호출에 `X-Correlation-Id` 헤더 전파.
- Analysis: `{interviewId}-{questionSetId}-{uuid8자리}` (예: `42-7-a1b2c3d4`)
- Convert: `convert-{s3key}-{uuid8자리}`
- API 서버: `InternalApiKeyFilter`에서 헤더 추출 → SLF4J MDC 설정 → logback 로그에 `[correlationId]` 자동 포함
- 장애 추적: CloudWatch에서 correlation_id 확인 → API 서버 로그에서 같은 ID 검색

### 파이프라인 흐름

```
S3 PutObject (videos/{interviewId}/qs_{qsId}.webm)
  → EventBridge (rehearse-video-uploaded-dev)
    ├── Analysis Lambda (동시 트리거)
    └── Convert Lambda (동시 트리거)

=== Analysis Lambda (질문세트 1개당 약 2~5분) ===

1. S3 키에서 interviewId, questionSetId 파싱
   LA → BE: GET .../answers (멱등성 체크: analysisStatus == COMPLETED면 즉시 종료)

2. LA → BE: PUT .../progress { "STARTED" }
   BE: analysisStatus PENDING_UPLOAD → ANALYZING

3. LA → S3: 영상 다운로드
   LA: FFmpeg 추출
     - 오디오: PCM 16-bit, 16kHz, mono (Whisper 요구사항)
     - 프레임: 3초 간격, 최대 10장 (quality level 2)

4. [언어 → 비언어 순차 실행]
   ├── 언어 분석 파이프라인 (먼저 실행):
   │   LA → OpenAI: Whisper STT (whisper-1, language="ko", verbose_json)
   │   → 세그먼트별 start_ms, end_ms, text
   │   LA → OpenAI: GPT-4o LLM (답변별 언어 분석)
   │   → verbal_score(0~100), filler_word_count, tone_label, comment
   └── 비언어 분석 파이프라인 (이후 실행, 분석+변환 Lambda는 EventBridge 동시 트리거로 병렬):
       LA → OpenAI: GPT-4o Vision (프레임 base64, detail="low")
       → eye_contact_score(0~100), posture_score(0~100),
         expression_label(CONFIDENT|NERVOUS|NEUTRAL|ENGAGED|UNCERTAIN)

5. LA: 종합 점수 계산
   overall = verbal_score × 0.6 + nonverbal_score × 0.4
   (nonverbal = (eye_contact + posture) / 2)

6. LA → BE: POST .../feedback (분석 결과 저장)
   BE → DB: QuestionSetFeedback + TimestampFeedback 저장
   BE: analysisStatus = COMPLETED

7. LA → BE: GET .../check-all-completed
   [모든 질문세트 분석 완료 시]
   LA → BE: POST /api/internal/interviews/{iId}/report (리포트 생성 트리거, timeout 60초)

=== Convert Lambda (질문세트 1개당 약 30초~1분) ===

1. S3 키에서 interviewId, questionSetId 파싱
2. LC → BE: GET /api/internal/files/by-s3-key (멱등성 체크)
3. LC → BE: PUT /api/internal/files/{id}/status { UPLOADED } (PENDING→UPLOADED)
4. LC → BE: PUT /api/internal/files/{id}/status { CONVERTING }
5. LC → MediaConvert: WebM → MP4 변환 Job 생성
   - H.264 HIGH, QVBR Level 7, Max 5Mbps
   - AAC 128kbps, 48kHz, stereo
   - MOOV: PROGRESSIVE_DOWNLOAD (faststart)
6. LC: Job 상태 폴링 (10초 간격, 최대 600초)
   - COMPLETE → 성공
   - ERROR/CANCELED → 예외
   - 600초 초과 → TimeoutError
7. LC → BE: PUT /api/internal/files/{id}/status { CONVERTED }
   + streamingS3Key 업데이트 (videos/{id}/qs_{id}.mp4)
```

### Analysis Lambda 진행 단계 (AnalysisProgress)

| 단계 | 설명 | Internal API |
|------|------|-------------|
| `STARTED` | 분석 시작, 멱등성 체크 | `PUT .../progress` |
| `EXTRACTING` | FFmpeg 오디오/프레임 추출 | `PUT .../progress` |
| `STT_PROCESSING` | Whisper 음성→텍스트 변환 | `PUT .../progress` |
| `VERBAL_ANALYZING` | GPT-4o LLM 언어 분석 | `PUT .../progress` |
| `NONVERBAL_ANALYZING` | GPT-4o Vision 비언어 분석 | `PUT .../progress` |
| `FINALIZING` | 종합 평가 생성 | `PUT .../progress` |
| `FAILED` | 분석 실패 (터미널 상태) | `PUT .../progress` |

### 장애 격리 & 실패 처리

**Lambda 내부 HTTP 재시도** (`lambda/common/retry.py`):
- `@retry_on_transient()` 데코레이터: 3회, 지수 백오프 + 지터 (1s→2s→4s)
- 재시도 대상: `httpx.TimeoutException`, `httpx.ConnectError`, 5xx 응답
- 즉시 실패: 4xx (클라이언트 에러)
- API 서버 순간 장애 시 Lambda 전체 재실행 없이 내부 복구

**OpenAI API 재시도** (STT/Vision/Verbal 전 analyzer):
- 3회 재시도, 지수 백오프 (2s→4s→8s)
- `RateLimitError`: 지수 백오프 대기 후 재시도
- `AuthenticationError`: 즉시 실패 (재시도 낭비 방지)
- Vision 실패 시 50점 + NEUTRAL 폴백, STT 실패 시 비언어만 분석

**낙관적 잠금** (`@Version`):
- `QuestionSet`, `FileMetadata` 엔티티에 version 필드
- 좀비 스케줄러와 Lambda 동시 업데이트 시 `OptimisticLockException` → 데이터 정합성 보장
- 스케줄러에서 version 충돌 시 해당 엔티티 스킵 (Lambda가 이미 처리한 것)

**복구 경로**:
- Lambda 크래시 → 좀비 스케줄러 (60초 간격) 감지: 10분 이상 ANALYZING → FAILED
- 분석 실패 → `POST /api/internal/.../retry-analysis` (FAILED → PENDING_UPLOAD)
- 변환 실패 → `POST /api/internal/files/{id}/retry-convert` (FAILED → UPLOADED)
- 변환 미실패 → 원본 WebM으로 재생 폴백
- API 서버 호출 실패 → 3회 내부 재시도 후에도 실패 시 S3 JSON 백업 (`analysis-backup/{id}/qs_{id}.json`)

---

## Phase 6: 분석 대기 (`/interview/:id/analysis`)

모든 질문세트의 분석 완료를 대기한다.

```
[폴링: 5초 간격, 질문세트별 병렬 (useAllQuestionSetStatuses)]
FE → BE: GET .../question-sets/{qsId}/status (각 질문세트 병렬 요청)
BE → FE: { analysisStatus, analysisProgress, fileStatus }

질문세트별 분석 상태 표시:
  EXTRACTING        → "음성/영상 추출 중..."
  STT_PROCESSING    → "음성을 텍스트로 변환 중..."
  VERBAL_ANALYZING  → "답변 내용을 분석 중..."
  NONVERBAL_ANALYZING → "표정과 자세를 분석 중..."
  FINALIZING        → "종합 평가를 생성 중..."

[완료된 질문세트 모범답변 미리보기]
FE → BE: GET .../question-sets/{qsId}/questions-with-answers

[분석 실패 시]
사용자: "재시도" 클릭
FE → BE: POST .../question-sets/{qsId}/retry-analysis

[모든 질문세트 COMPLETED/SKIPPED → /interview/{id}/feedback으로 이동]
```

---

## Phase 7: 타임스탬프 피드백 (`/interview/:id/feedback`)

질문세트별 영상과 타임스탬프 피드백을 3way 동기화하여 표시한다.

```
FE → BE: GET .../question-sets/{qsId}/feedback
BE → FE: QuestionSetFeedbackResponse (점수, 코멘트, 타임스탬프 피드백[], streamingUrl, fallbackUrl)
FE → S3: MP4 스트리밍 재생 (streamingUrl) 또는 WebM 폴백 (fallbackUrl)

3way 동기화 (useFeedbackSync, 200ms 폴링):
  ① 영상 플레이어 ↔ ② 타임라인 마커 ↔ ③ 피드백 패널

사용자: 피드백 카드 클릭 → 영상 seek (해당 타임스탬프로 이동)
사용자: 영상 재생 → 타임스탬프 통과 시 해당 피드백 카드 하이라이트 + 스크롤
사용자: 질문세트 탭 전환 → 다른 질문세트 피드백 로드
```

**피드백 카드 데이터 (TimestampFeedback)**:
- `startMs`, `endMs`: 구간 타임스탬프
- `transcript`: STT 텍스트
- `verbalScore` (0~100), `fillerWordCount`: 언어 분석
- `eyeContactScore`, `postureScore` (0~100): 비언어 분석
- `expressionLabel`: CONFIDENT/NERVOUS/NEUTRAL/ENGAGED/UNCERTAIN
- `nonverbalComment`, `overallComment`: AI 코멘트

**UI 레이아웃**:
- 좌측 (60%): 영상 플레이어 + 타임라인 (피드백 마커, 점수별 색상)
- 우측 (40%): 피드백 카드 패널 (MAIN/FOLLOWUP 그룹, 필러워드 하이라이트)
- 상단: 질문세트 탭 전환

**Presigned URL 만료 처리**:
- 영상 로드 실패 시 `onUrlExpired()` → 쿼리 무효화 → 새 URL 자동 발급

---

## Phase 8: 종합 리포트 (`/interview/:id/report`)

모든 분석 완료 시 자동 생성된 리포트를 표시한다.

```
--- 리포트 자동 생성 흐름 (Analysis Lambda가 트리거, 비동기) ---
BE → DB: 모든 QuestionSetFeedback 조회
BE → Claude API: 종합 리포트 생성 (maxTokens=2048)
Claude → BE: 점수 + 요약 + 강점 + 개선점
BE → DB: InterviewReport 저장

--- InterviewCompletionService (30초 스케줄러) ---
BE: IN_PROGRESS 면접 중 모든 질문세트가 resolved(COMPLETED/SKIPPED) → COMPLETED 자동 전환
BE: overallScore = QuestionSetFeedback.questionSetScore 평균

--- 클라이언트 조회 ---
FE → BE: GET /api/v1/interviews/{interviewId}/report

[리포트 존재] → 200 OK: { id, interviewId, overallScore, summary, strengths[], improvements[], feedbackCount }
[리포트 생성 중] → 202 Accepted (REPORT_GENERATING) → 5초 폴링, 최대 24회 (2분)
[분석 미완료] → 409 Conflict (ANALYSIS_NOT_COMPLETED) → 5초 폴링, 최대 60회 (5분)
```

**리포트 생성 트리거**:
- Analysis Lambda에서 모든 질문세트 완료 확인 시 `POST /api/internal/interviews/{iId}/report` 호출
- 또는 `InternalQuestionSetService.saveFeedback()`에서 `AllAnalysisCompletedEvent` 발행
- `AllAnalysisCompletedEventListener`가 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`으로 수신 → `ReportService.generateReport()` 호출

---

## 부록

### A. 상태 전이 다이어그램

#### Interview 상태 (InterviewStatus)

```
[*] → READY → IN_PROGRESS → COMPLETED → [*]
      (생성)   (PATCH /status) (PATCH /status 또는 30초 스케줄러)
```

#### 질문 생성 상태 (QuestionGenerationStatus)

```
[*] → PENDING → GENERATING → COMPLETED
      (생성)    (비동기 시작)  (질문 저장 완료)
                    │
                    ▼
                  FAILED ──(retry-questions)──→ PENDING
```

#### 질문세트 분석 상태 (AnalysisStatus)

```
[*] → PENDING → PENDING_UPLOAD → ANALYZING → COMPLETED
      (생성)    (답변 제출)       (Lambda 시작) (피드백 저장)
         │            │              │              │
         ├→ SKIPPED   ├→ FAILED      ├→ FAILED      ├→ FAILED
         │  (skip)    │              │  (좀비 감지)   │  (후처리)
         ├→ FAILED    │              │              │
         │  (타임아웃) │              │              │
         │            │              │              │
         │      FAILED ←─────────────┘              │
         │        │                                  │
         │        ├→ PENDING_UPLOAD (retry-analysis) │
         │        ├→ ANALYZING (retry-analysis)      │
         │        └→ COMPLETED (수동 복구)            │
```

#### 파일 상태 (FileStatus)

```
[*] → PENDING → UPLOADED → CONVERTING → CONVERTED
      (생성)    (Lambda)   (MediaConvert) (완료)
         │         │           │
         ├→ FAILED ├→ FAILED   ├→ FAILED
         │         │           │  (타임아웃)
         │         │           │
         │   FAILED → UPLOADED (재시도)
```

### B. 이벤트 목록

| 이벤트 | 발행 시점 | 리스너 | 처리 |
|--------|----------|--------|------|
| `QuestionGenerationRequestedEvent` | `InterviewService.createInterview()` 트랜잭션 커밋 후 | `QuestionGenerationService` | `@Async("questionGenerationExecutor")` Claude API 호출 → QuestionSet 저장 |
| `AllAnalysisCompletedEvent` | `InternalQuestionSetService.saveFeedback()` 에서 모든 질문세트 완료 확인 시 | `AllAnalysisCompletedEventListener` | `@Async` Claude API 호출 → InterviewReport 저장 |
| S3 PutObject → EventBridge | 클라이언트 S3 업로드 완료 시 (`videos/*.webm` 패턴) | Analysis Lambda + Convert Lambda | 동시 트리거 |

### C. API 엔드포인트 전체 목록

#### 클라이언트 API (`/api/v1`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `POST` | `/api/v1/interviews` | 면접 세션 생성 (multipart: request + resumeFile) |
| `GET` | `/api/v1/interviews/{id}` | 면접 상세 조회 |
| `PATCH` | `/api/v1/interviews/{id}/status` | 면접 상태 변경 |
| `POST` | `/api/v1/interviews/{id}/follow-up` | 후속질문 생성 (multipart: request + audio) |
| `POST` | `/api/v1/interviews/{id}/retry-questions` | 질문 재생성 |
| `POST` | `/api/v1/interviews/{id}/skip-remaining` | 미응답 질문세트 스킵 |
| `POST` | `/api/v1/interviews/{iId}/question-sets/{qsId}/answers` | 답변 타임스탬프 저장 |
| `POST` | `/api/v1/interviews/{iId}/question-sets/{qsId}/upload-url` | Presigned URL 발급 |
| `GET` | `/api/v1/interviews/{iId}/question-sets/{qsId}/status` | 분석 상태 조회 |
| `GET` | `/api/v1/interviews/{iId}/question-sets/{qsId}/feedback` | 질문세트 피드백 조회 |
| `GET` | `/api/v1/interviews/{iId}/question-sets/{qsId}/questions-with-answers` | 모범답변 조회 |
| `POST` | `/api/v1/interviews/{iId}/question-sets/{qsId}/retry-analysis` | 분석 재시도 |
| `GET` | `/api/v1/interviews/{iId}/report` | 종합 리포트 조회 |

#### 내부 API (`/api/internal`) — Lambda → API 서버

인증: `X-Internal-Api-Key` 헤더 필수

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `PUT` | `/api/internal/interviews/{iId}/question-sets/{qsId}/progress` | 분석 진행 상태 업데이트 |
| `GET` | `/api/internal/interviews/{iId}/question-sets/{qsId}/answers` | 답변 구간 타임스탬프 조회 |
| `POST` | `/api/internal/interviews/{iId}/question-sets/{qsId}/feedback` | 분석 결과(피드백) 저장 |
| `POST` | `/api/internal/interviews/{iId}/question-sets/{qsId}/retry-analysis` | Lambda 수동 invoke |
| `PUT` | `/api/internal/files/{fileMetadataId}/status` | 파일 변환 상태 업데이트 |
| `GET` | `/api/internal/files/by-s3-key` | S3 키로 파일 메타 조회 |
| ~~`POST`~~ | ~~`/api/internal/interviews/{iId}/report`~~ | 제거됨 — 리포트 생성은 Spring Event(AllAnalysisCompletedEvent) 또는 FE POST `/api/v1/.../report`로 트리거 |

### D. 폴링 전략

| 대상 | 훅 | 간격 | 조건 | 중단 |
|------|-----|------|------|------|
| 질문 생성 | `useInterview` | 2초 | `questionGenerationStatus ∈ {PENDING, GENERATING}` | COMPLETED 또는 FAILED |
| 분석 상태 (개별) | `useQuestionSetStatus` | 3초 | caller가 `enabled=true` 전달 시 | COMPLETED/SKIPPED/FAILED |
| 분석 상태 (전체) | `useAllQuestionSetStatuses` | 5초 | ANALYZING인 질문세트 존재 시 | 모든 질문세트 COMPLETED/SKIPPED/FAILED |
| 리포트 (생성 중) | `useReport` | 5초 | 202 응답 (REPORT_GENERATING) | 200 응답 (리포트 반환), 최대 24회 (2분) |
| 리포트 (분석 중) | `useReport` | 5초 | 409 응답 (ANALYSIS_NOT_COMPLETED) | 200 응답, 최대 60회 (5분) |
| 피드백 동기화 | `useFeedbackSync` | 200ms | 영상 재생 중 | 영상 일시정지 |

### E. 에러 처리 & 재시도

| 실패 유형 | 감지 방법 | 복구 방법 |
|----------|----------|----------|
| 질문 생성 실패 | `questionGenerationStatus = FAILED` | `POST .../retry-questions` |
| Claude API 장애 | 3회 지수 백오프 재시도 (1s→2s→4s), 429/5xx 재시도 | 최종 실패 시 BusinessException |
| 분석 Lambda 크래시 | 좀비 스케줄러 (10분 ANALYZING → FAILED) | `POST .../retry-analysis` |
| 변환 Lambda 크래시 | 좀비 스케줄러 (10분 CONVERTING → FAILED) | `POST .../retry-convert` 또는 원본 WebM 폴백 |
| 업로드 타임아웃 | 좀비 스케줄러 (30분 PENDING → FAILED) | FE 재업로드 |
| S3 업로드 실패 | FE 3회 재시도 (1s, 2s, 4s 지수 백오프) | IndexedDB Blob 보관 → 재업로드 |
| OpenAI API 실패 | Lambda 내부 3회 재시도 (2s→4s→8s 지수 백오프) | 부분 실패 허용 (Vision 실패 시 50점 폴백) |
| OpenAI Rate Limit | RateLimitError 분리 감지, 지수 백오프 대기 | AuthenticationError는 즉시 실패 |
| API 서버 호출 실패 | `@retry_on_transient` 3회 재시도 (1s→2s→4s + jitter) | 최종 실패 시 S3 JSON 백업 |
| 동시 상태 업데이트 | `@Version` 낙관적 잠금 → OptimisticLockException | 스케줄러: 스킵, Lambda: 5xx 수신 후 재시도 |

### F. 프론트엔드 상태 관리

| 저장소 | 역할 | 데이터 |
|--------|------|--------|
| **TanStack Query** | 서버 데이터 캐싱 + 폴링 | Interview, QuestionSetStatus, Feedback, Report |
| **Zustand (useInterviewStore)** | 면접 진행 상태 머신 | phase, questionSets, questionSetAnswers, uploadStatus, followUpHistory, currentFollowUp, followUpRound, videoBlob |
| **React State** | 로컬 UI 상태 | Setup 위저드 step, 기기 테스트 상태, 피드백 탭 선택 |

### G. 스케줄러 목록

| 스케줄러 | 클래스 | 간격 | 로직 |
|---------|--------|------|------|
| 면접 자동 완료 | `InterviewCompletionService` | 30초 (`fixedDelay=30_000`) | IN_PROGRESS 면접 중 모든 질문세트 resolved(COMPLETED+SKIPPED) && completedCount > 0 → COMPLETED 전환 + overallScore 계산 |
| 분석 좀비 감지 | `AnalysisScheduler.detectAnalysisZombies` | 60초 (`fixedDelay=60_000`) | ANALYZING 상태 10분 이상 → FAILED (reason: ZOMBIE_TIMEOUT). `@Version` 충돌 시 스킵. Lambda 실행 시간 제한(15분) + 네트워크 장애로 상태 업데이트 누락 가능하므로 필요 |
| 변환 좀비 감지 | `AnalysisScheduler.detectFileConvertingZombies` | 60초 (`fixedDelay=60_000`) | CONVERTING 상태 10분 이상 → FAILED (reason: CONVERTING_TIMEOUT). `@Version` 충돌 시 스킵 |
| 업로드 좀비 감지 | `AnalysisScheduler.detectUploadZombies` | 300초 (`fixedDelay=300_000`) | PENDING 상태 30분 이상 → FAILED (reason: UPLOAD_TIMEOUT). `@Version` 충돌 시 스킵 |

### H. AWS 인프라 상세

#### 서버 구성

| 컴포넌트 | 스펙 | 비고 |
|---------|------|------|
| EC2 | t3.small (2 vCPU, 2GB RAM) | 리전: ap-northeast-2 |
| Docker Compose | MySQL 8.0 + Spring Boot + Nginx + Certbot | 4개 서비스 |
| Nginx | TLS 1.2/1.3, client_max_body_size 10MB | `api-dev.rehearse.co.kr` |
| JVM | `-Xms512m -Xmx512m`, eclipse-temurin:21-jre | 멀티스테이지 Docker 빌드 |

#### S3 (rehearse-videos-dev)

| 경로 패턴 | 설명 |
|----------|------|
| `videos/{interviewId}/qs_{questionSetId}.webm` | 원본 녹화 영상 |
| `videos/{interviewId}/qs_{questionSetId}.mp4` | MP4 변환 영상 |
| `analysis-backup/{id}/qs_{id}.json` | 분석 실패 시 JSON 백업 |

Presigned URL 유효기간: PUT 30분, GET 2시간

S3 수명주기: 30일 → Intelligent-Tiering, 90일 → WebM 삭제, 180일 → 전체 삭제

#### EventBridge

- 규칙: `rehearse-video-uploaded-dev`
- 패턴: S3 PutObject → `videos/*.webm`
- 타겟: `rehearse-analysis-dev` + `rehearse-convert-dev` (동시)

#### Lambda 설정

| Lambda | 메모리 | 타임아웃 | 런타임 | Layer |
|--------|--------|---------|--------|-------|
| `rehearse-analysis-dev` | 2048 MB | 900초 (15분) | Python 3.12 | ffmpeg-static:1 (56MB) |
| `rehearse-convert-dev` | 256 MB | 300초 (5분) | Python 3.12 | — |

ffmpeg-static Layer: `/opt/bin/ffmpeg`, `/opt/bin/ffprobe` (FFmpeg 7.x)

#### MediaConvert

| 설정 | 값 |
|------|-----|
| 입력 | WebM (VP9 + Opus/Vorbis) |
| 비디오 코덱 | H.264 HIGH |
| 비트레이트 | QVBR Level 7, Max 5Mbps |
| MOOV | PROGRESSIVE_DOWNLOAD (faststart) |
| 오디오 | AAC 128kbps, 48kHz, stereo |

#### CI/CD (deploy-dev.yml)

- 트리거: `develop` 브랜치 push
- 변경 감지: `dorny/paths-filter` (backend, frontend, nginx, infra)
- BE 테스트: JDK 21, Gradle, H2 in-memory
- FE 빌드: Node 20, `VITE_API_URL=https://api-dev.rehearse.co.kr`
- FE 배포: S3 sync + CloudFront 캐시 무효화
- BE 배포: ECR push → EC2 SSH docker compose up → health check (`/actuator/health`)
- Nginx: 설정 변경 시 조건부 재시작

### I. AI 클라이언트 구조

#### AiClient 인터페이스 (BE)

```
interface AiClient {
  generateQuestions(position, positionDetail, level, interviewTypes, csSubTopics, resumeText, durationMinutes) → List<GeneratedQuestion>
  generateFollowUpQuestion(questionContent, answerText, nonVerbalSummary, previousExchanges) → GeneratedFollowUp
  generateReport(feedbackSummary) → GeneratedReport
}
```

| 구현체 | 조건 | 용도 |
|--------|------|------|
| `ClaudeApiClient` | `claude.api-key` 설정 시 | 프로덕션 |
| `MockAiClient` | API 키 미설정 시 | 로컬 개발/테스트 |

#### Claude API 설정

| 항목 | 값 |
|------|-----|
| 모델 | `claude-sonnet-4-20250514` |
| API 버전 | `2023-06-01` |
| 질문 생성 | maxTokens=4096, temperature=0.9 (다양성 확보) |
| 후속질문 | maxTokens=1024 |
| 리포트 | maxTokens=2048 |
| 타임아웃 | connect=5초, read=60초 |

#### OpenAI API (Lambda)

| 용도 | 모델 | 설정 |
|------|------|------|
| STT | whisper-1 | language="ko", response_format="verbose_json" |
| 비언어 분석 | gpt-4o | Vision, 최대 10 프레임, detail="low" |
| 언어 분석 | gpt-4o | 답변별 분석, JSON 응답 |

재시도: 3회, 2초 지수 백오프. Vision 실패 시 50점 + NEUTRAL 폴백.

#### 비동기 설정

```
questionGenerationExecutor:
  corePoolSize: 2
  maxPoolSize: 5
  queueCapacity: 10
  threadNamePrefix: "question-gen-"
```
