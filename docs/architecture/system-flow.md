# 리허설(Rehearse) — 시스템 상호작용 흐름도

> 면접 Setup부터 피드백 추출까지, 사용자·클라이언트·서버 간 전체 흐름

---

## 전체 흐름 요약

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌───────────┐
│  사용자  │ ──→ │  프론트엔드  │ ──→ │   백엔드 API  │ ──→ │ Claude AI │
│ (브라우저)│ ←── │  (React)    │ ←── │ (Spring Boot)│ ←── │ (Sonnet)  │
└─────────┘     └─────────────┘     └──────────────┘     └───────────┘
                                           │
                                    ┌──────┴──────┐
                                    │  MySQL / H2  │
                                    └─────────────┘
```

---

## Phase 1: 면접 Setup (질문 생성)

```
사용자                     프론트엔드                        백엔드                         Claude AI
  │                          │                               │                               │
  │  직무/레벨/유형/시간 선택  │                               │                               │
  │  이력서 PDF 첨부 (선택)   │                               │                               │
  │ ─────────────────────→   │                               │                               │
  │                          │                               │                               │
  │                          │  POST /api/v1/interviews      │                               │
  │                          │  (multipart: request + PDF)   │                               │
  │                          │ ─────────────────────────────→│                               │
  │                          │                               │                               │
  │                          │                               │  ① PDF → 텍스트 추출          │
  │                          │                               │  ② Interview 엔티티 생성       │
  │                          │                               │     status = READY             │
  │                          │                               │  ③ DB 저장 (Interview)         │
  │                          │                               │                               │
  │                          │                               │  ④ 🤖 Claude API 호출 #1      │
  │                          │                               │  "면접 질문 생성해줘"           │
  │                          │                               │  (직무+레벨+유형+이력서)        │
  │                          │                               │ ─────────────────────────────→│
  │                          │                               │                               │
  │                          │                               │  ⑤ 질문 목록 응답              │
  │                          │                               │←─────────────────────────────│
  │                          │                               │                               │
  │                          │                               │  ⑥ InterviewQuestion 엔티티    │
  │                          │                               │     DB 저장 (N개 질문)         │
  │                          │                               │                               │
  │                          │  InterviewSession 응답         │                               │
  │                          │  (id, questions[], status)    │                               │
  │                          │←─────────────────────────────│                               │
  │                          │                               │                               │
  │  /interview/{id}/ready   │                               │                               │
  │  페이지로 이동            │                               │                               │
  │←─────────────────────   │                               │                               │
```

**API 상세**:
- **URL**: `POST /api/v1/interviews`
- **요청**: multipart/form-data (`request` JSON part + `resumeFile` PDF part)
- **DB 저장**: Interview + InterviewQuestion (cascade)
- **Claude 호출**: 질문 생성 (max_tokens: 4096)
- **질문 수 계산**: `round(durationMinutes / 5)` (최소 2, 최대 24)

---

## Phase 2: 면접 준비 (기기 테스트)

```
사용자                     프론트엔드                        백엔드
  │                          │                               │
  │  준비 페이지 진입          │                               │
  │ ─────────────────────→   │                               │
  │                          │  GET /api/v1/interviews/{id}  │
  │                          │ ─────────────────────────────→│
  │                          │  InterviewSession 응답         │
  │                          │←─────────────────────────────│
  │                          │                               │
  │  카메라 테스트 ✅          │  (브라우저 로컬)              │
  │  마이크 테스트 ✅          │  getUserMedia()              │
  │  스피커 테스트 ✅          │  AudioContext                │
  │                          │                               │
  │  "면접 시작" 버튼 클릭     │                               │
  │ ─────────────────────→   │                               │
  │                          │  PATCH /interviews/{id}/status │
  │                          │  { status: "IN_PROGRESS" }    │
  │                          │ ─────────────────────────────→│
  │                          │                               │  상태 전이: READY → IN_PROGRESS
  │                          │←─────────────────────────────│
  │                          │                               │
  │  /interview/{id}/conduct │                               │
  │  면접 진행 페이지로 이동   │                               │
  │←─────────────────────   │                               │
```

**API 상세**:
- **조회**: `GET /api/v1/interviews/{id}` (questions 포함)
- **상태 변경**: `PATCH /api/v1/interviews/{id}/status` → READY → IN_PROGRESS

---

## Phase 3: 면접 진행 (음성 대화 루프)

```
사용자                     프론트엔드 (브라우저 API)                        백엔드          Claude AI
  │                          │                                              │               │
  │                          │  ┌─────────────────────────────────┐        │               │
  │                          │  │ 동시 활성화:                     │        │               │
  │                          │  │ • MediaRecorder (영상 녹화)      │        │               │
  │                          │  │ • Web Speech API (STT)          │        │               │
  │                          │  │ • Web Audio API (음성 분석)      │        │               │
  │                          │  │ • MediaPipe (비언어 분석)        │        │               │
  │                          │  │ • VAD (음성 감지)                │        │               │
  │                          │  │ • TTS (질문 읽기)                │        │               │
  │                          │  └─────────────────────────────────┘        │               │
  │                          │                                              │               │
  │                          │  🔊 TTS: "안녕하세요, 면접을 시작하겠습니다"  │               │
  │←── 인사 음성 ───────────│                                              │               │
  │                          │                                              │               │
  │                          │  🔊 TTS: "첫 번째 질문입니다. ..."           │               │
  │←── 질문 음성 ───────────│                                              │               │
  │                          │                                              │               │
  │                          │                                              │               │
  │  ┌──── 답변 루프 (질문당 반복) ─────────────────────────────────────────┐               │
  │  │                                                                      │               │
  │  │  "답변을 말합니다..."   │                                              │               │
  │  │ ─────────────────────→│                                              │               │
  │  │                        │                                              │               │
  │  │                        │  🎤 VAD: 음성 감지 → 녹음 시작              │               │
  │  │                        │  🎤 STT: 실시간 텍스트 변환                  │               │
  │  │                        │  📹 MediaRecorder: 영상 녹화 중             │               │
  │  │                        │  👁️ MediaPipe: 표정/자세 분석 중            │               │
  │  │                        │  📊 AudioAnalyzer: 음성 톤 분석 중          │               │
  │  │                        │                                              │               │
  │  │  (침묵 3초 감지)        │                                              │               │
  │  │                        │  🔇 VAD: 침묵 감지 → 답변 종료              │               │
  │  │                        │                                              │               │
  │  │                        │  POST /interviews/{id}/follow-up             │               │
  │  │                        │  { questionContent, answerText }             │               │
  │  │                        │ ─────────────────────────────────────────→  │               │
  │  │                        │                                              │               │
  │  │                        │                                              │  🤖 Claude API│
  │  │                        │                                              │  호출 #2     │
  │  │                        │                                              │  "후속질문"   │
  │  │                        │                                              │ ────────────→│
  │  │                        │                                              │←────────────│
  │  │                        │                                              │               │
  │  │                        │  FollowUpResponse (question, reason, type)   │               │
  │  │                        │←─────────────────────────────────────────  │               │
  │  │                        │                                              │               │
  │  │                        │  "2.5초 후 다음 질문으로 넘어갑니다..."       │               │
  │  │                        │  🔊 TTS: 다음 질문 읽기                      │               │
  │  │                        │                                              │               │
  │  └──── 답변 루프 끝 ───────────────────────────────────────────────────┘               │
  │                          │                                              │               │
  │                          │  (마지막 질문 답변 완료)                      │               │
  │                          │                                              │               │
  │                          │  PATCH /interviews/{id}/status               │               │
  │                          │  { status: "COMPLETED" }                     │               │
  │                          │ ─────────────────────────────────────────→  │               │
  │                          │                                              │  상태 전이:    │
  │                          │←─────────────────────────────────────────  │  IN_PROGRESS   │
  │                          │                                              │  → COMPLETED   │
  │                          │                                              │               │
  │  /interview/{id}/complete│                                              │               │
  │←─────────────────────   │                                              │               │
```

**브라우저 API 활용 상세**:

| 기술 | 역할 | 데이터 |
|------|------|--------|
| MediaRecorder | 영상 녹화 | WebM/VP9 Blob |
| Web Speech API | 음성→텍스트 | TranscriptSegment[] |
| Web Audio API | 음성 레벨/톤 분석 | VoiceEvent[] |
| MediaPipe | 표정/자세 분석 | NonVerbalEvent[] |
| VAD (커스텀) | 발화/침묵 감지 | 녹음 시작/종료 트리거 |
| TTS | 질문 음성 출력 | SpeechSynthesis |

**후속질문 API**:
- **URL**: `POST /api/v1/interviews/{id}/follow-up`
- **Claude 호출**: 후속질문 생성 (max_tokens: 1024)
- **유형**: DEEP_DIVE / CLARIFICATION / CHALLENGE / APPLICATION

---

## Phase 4: 피드백 생성 (면접 완료 직후)

```
사용자                     프론트엔드                        백엔드                         Claude AI
  │                          │                               │                               │
  │  완료 페이지 진입          │                               │                               │
  │ ─────────────────────→   │                               │                               │
  │                          │                               │                               │
  │                          │  ┌─────────────────────────┐  │                               │
  │                          │  │ 데이터 수집 (Zustand):   │  │                               │
  │                          │  │ • STT 텍스트 (답변)      │  │                               │
  │                          │  │ • 비언어 분석 요약       │  │                               │
  │                          │  │ • 음성 분석 요약         │  │                               │
  │                          │  └─────────────────────────┘  │                               │
  │                          │                               │                               │
  │                          │  POST /interviews/{id}/feedbacks                              │
  │                          │  {                            │                               │
  │                          │    answers: [                 │                               │
  │                          │      {                        │                               │
  │                          │        questionIndex: 0,      │                               │
  │                          │        questionContent: "...",│                               │
  │                          │        answerText: "...",     │                               │
  │                          │        nonVerbalSummary: ".",│                               │
  │                          │        voiceSummary: "..."    │                               │
  │                          │      }, ...                   │                               │
  │                          │    ]                          │                               │
  │                          │  }                            │                               │
  │                          │ ─────────────────────────────→│                               │
  │                          │                               │                               │
  │                          │                               │  ① InterviewAnswer 엔티티     │
  │                          │                               │     DB 저장 (질문별 답변)      │
  │                          │                               │                               │
  │                          │                               │  ② 🤖 Claude API 호출 #3     │
  │                          │                               │  "타임스탬프별 피드백 생성"     │
  │                          │                               │ ─────────────────────────────→│
  │                          │                               │                               │
  │                          │                               │  ③ 피드백 목록 응답            │
  │                          │                               │←─────────────────────────────│
  │                          │                               │                               │
  │                          │                               │  ④ Feedback 엔티티            │
  │                          │                               │     DB 저장 (10~20개)         │
  │                          │                               │                               │
  │                          │  FeedbackListResponse          │                               │
  │                          │  [{timestampSeconds, category, │                               │
  │                          │    severity, content, ...}]   │                               │
  │                          │←─────────────────────────────│                               │
  │                          │                               │                               │
  │  "분석 완료!" 버튼 표시    │                               │                               │
  │←─────────────────────   │                               │                               │
```

**API 상세**:
- **URL**: `POST /api/v1/interviews/{id}/feedbacks`
- **DB 저장**: InterviewAnswer (답변) + Feedback (피드백)
- **Claude 호출**: 피드백 생성 (max_tokens: 4096)
- **피드백 카테고리**: VERBAL / NON_VERBAL / CONTENT
- **피드백 심각도**: INFO (긍정) / WARNING (주의) / SUGGESTION (제안)

---

## Phase 5A: 타임스탬프 피드백 리뷰

```
사용자                     프론트엔드                        백엔드
  │                          │                               │
  │  리뷰 페이지 진입          │                               │
  │ ─────────────────────→   │                               │
  │                          │  GET /interviews/{id}/feedbacks│
  │                          │ ─────────────────────────────→│
  │                          │  피드백 목록 (timestamp 순서)   │
  │                          │←─────────────────────────────│
  │                          │                               │
  │  ┌─────────────────────────────────────────────────┐    │
  │  │              리뷰 화면 레이아웃                    │    │
  │  │                                                   │    │
  │  │  ┌──────────────────┐  ┌──────────────────┐      │    │
  │  │  │   영상 플레이어   │  │   피드백 패널     │      │    │
  │  │  │   (60%)          │  │   (40%)          │      │    │
  │  │  │                  │  │                  │      │    │
  │  │  │  ▶ 녹화 영상     │  │  📝 피드백 #1    │      │    │
  │  │  │                  │  │  📝 피드백 #2    │      │    │
  │  │  │                  │  │  📝 피드백 #3    │      │    │
  │  │  └──────────────────┘  │  ...             │      │    │
  │  │  ┌──────────────────┐  └──────────────────┘      │    │
  │  │  │ 타임라인 (피드백) │                            │    │
  │  │  │ ●──●────●──●──── │  ← 클릭 시 영상 seek      │    │
  │  │  └──────────────────┘                            │    │
  │  └─────────────────────────────────────────────────┘    │
  │                          │                               │
  │  피드백 클릭 →            │  영상 seek (timestampSeconds)  │
  │  영상 해당 시점으로 이동   │                               │
```

**API 상세**:
- **URL**: `GET /api/v1/interviews/{id}/feedbacks`
- **서버 처리**: DB 조회만 (Claude 호출 없음)
- **동기화**: 피드백의 `timestampSeconds` ↔ 영상 재생 위치

---

## Phase 5B: 종합 리포트

```
사용자                     프론트엔드                        백엔드                         Claude AI
  │                          │                               │                               │
  │  리포트 페이지 진입        │                               │                               │
  │ ─────────────────────→   │                               │                               │
  │                          │  GET /interviews/{id}/report  │                               │
  │                          │ ─────────────────────────────→│                               │
  │                          │                               │                               │
  │                          │                               │  리포트 존재? ─── Yes → 반환   │
  │                          │                               │       │                       │
  │                          │                               │      No (최초 조회)            │
  │                          │                               │       │                       │
  │                          │                               │  ① 피드백 DB 조회              │
  │                          │                               │  ② 피드백 텍스트 요약 생성      │
  │                          │                               │                               │
  │                          │                               │  ③ 🤖 Claude API 호출 #4     │
  │                          │                               │  "종합 리포트 생성"             │
  │                          │                               │ ─────────────────────────────→│
  │                          │                               │                               │
  │                          │                               │  ④ 리포트 응답                 │
  │                          │                               │←─────────────────────────────│
  │                          │                               │                               │
  │                          │                               │  ⑤ InterviewReport 엔티티     │
  │                          │                               │     DB 저장 (1회만 생성)       │
  │                          │                               │                               │
  │                          │  InterviewReport               │                               │
  │                          │  { overallScore, summary,     │                               │
  │                          │    strengths[], improvements[]}│                               │
  │                          │←─────────────────────────────│                               │
  │                          │                               │                               │
  │  ┌─────────────────────────────────┐                    │                               │
  │  │  종합 점수: 78 / 100            │                    │                               │
  │  │  ──────────────────────         │                    │                               │
  │  │  종합 평가:                      │                    │                               │
  │  │  "전반적으로 기술 이해도가..."    │                    │                               │
  │  │  ──────────────────────         │                    │                               │
  │  │  ✅ 강점        ⚠️ 보완점       │                    │                               │
  │  │  • 논리적 설명   • 구체적 사례   │                    │                               │
  │  │  • 기술 이해도   • 비언어 표현   │                    │                               │
  │  └─────────────────────────────────┘                    │                               │
```

**API 상세**:
- **URL**: `GET /api/v1/interviews/{id}/report`
- **Lazy Generation**: 첫 조회 시 생성, 이후 캐싱
- **Claude 호출**: 리포트 생성 (max_tokens: 2048)

---

## Claude AI 호출 시점 총정리

| # | 시점 | API 엔드포인트 | 서비스 메서드 | 입력 | 출력 | max_tokens |
|---|------|---------------|-------------|------|------|-----------|
| 1 | 면접 생성 | `POST /interviews` | `InterviewService.createInterview()` | 직무+레벨+유형+이력서 | 질문 목록 (N개) | 4096 |
| 2 | 답변 후 | `POST /interviews/{id}/follow-up` | `InterviewService.generateFollowUp()` | 원 질문+답변+비언어 | 후속질문 1개 | 1024 |
| 3 | 면접 완료 | `POST /interviews/{id}/feedbacks` | `FeedbackService.generateFeedback()` | 전체 답변 데이터 | 피드백 10~20개 | 4096 |
| 4 | 리포트 조회 | `GET /interviews/{id}/report` | `ReportService.getReport()` | 피드백 요약 | 점수+강점+보완점 | 2048 |

---

## DB 저장 시점 총정리

| 시점 | 저장 엔티티 | 트리거 |
|------|-----------|--------|
| 면접 생성 | `Interview` + `InterviewQuestion[]` | Setup 완료 버튼 |
| 면접 시작 | `Interview.status = IN_PROGRESS` | 준비 페이지 시작 버튼 |
| 면접 완료 | `Interview.status = COMPLETED` | 마지막 질문 답변 후 자동 |
| 피드백 생성 | `InterviewAnswer[]` + `Feedback[]` | 완료 페이지 자동 호출 |
| 리포트 생성 | `InterviewReport` | 리포트 페이지 첫 조회 |

---

## 데이터 모델 관계도

```
Interview (1)
├── status: READY → IN_PROGRESS → COMPLETED
│
├──→ InterviewQuestion (N)     [면접 생성 시 저장]
│    ├── content: "질문 내용"
│    ├── category: "CS/행동/기술"
│    └── evaluationCriteria: "평가 기준"
│
├──→ InterviewAnswer (N)       [피드백 생성 시 저장]
│    ├── answerText: "STT 변환 텍스트"
│    ├── nonVerbalSummary: "비언어 분석"
│    └── voiceSummary: "음성 분석"
│
├──→ Feedback (N)              [피드백 생성 시 저장]
│    ├── timestampSeconds: 32.5
│    ├── category: VERBAL | NON_VERBAL | CONTENT
│    ├── severity: INFO | WARNING | SUGGESTION
│    └── content + suggestion
│
└──→ InterviewReport (1)       [리포트 첫 조회 시 저장]
     ├── overallScore: 0~100
     ├── summary: "종합 평가"
     ├── strengths: ["강점1", "강점2"]
     └── improvements: ["보완점1", "보완점2"]
```

---

## 프론트엔드 상태 관리

| 저장소 | 역할 | 데이터 |
|--------|------|--------|
| **TanStack Query** | 서버 데이터 캐싱 | Interview, Feedbacks, Report |
| **Zustand (interview-store)** | 면접 진행 상태 | phase, questions, answers, transcripts, events |
| **Zustand (review-store)** | 리뷰 페이지 상태 | feedbacks, currentTimestamp |
| **React State** | 로컬 UI 상태 | Setup 위저드 step, 기기 테스트 상태 |
