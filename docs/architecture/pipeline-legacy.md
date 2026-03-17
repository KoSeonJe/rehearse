# 영상 녹화 · 분석 · 피드백 · 재생 파이프라인 (Legacy)

> 현재 구현된 파이프라인 아키텍처를 문서화한 것입니다.
> 작성일: 2026-03-17

---

## 전체 파이프라인 개요

```
┌──────────────────────────────────────────────────────────────────┐
│                     INTERVIEW PAGE (면접 진행)                    │
│                                                                    │
│  MediaStream ──┬── MediaRecorder (WebM/VP9, 1초 청크)            │
│                ├── FaceLandmarker → 시선 분석 (10fps)             │
│                ├── PoseLandmarker → 자세 분석 (10fps)             │
│                ├── AnalyserNode → 음성 레벨/침묵 감지 (30fps)    │
│                └── SpeechRecognition → 실시간 STT (ko-KR)        │
│                                                                    │
│  → Zustand Store에 분석 결과 누적 (질문별 QuestionAnswer)        │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 면접 완료
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                  INTERVIEW COMPLETE PAGE (완료 처리)              │
│                                                                    │
│  1. 영상 저장: Blob → IndexedDB + Zustand (Blob URL)            │
│  2. 분석 데이터 수집: transcript + nonVerbal + voice → JSON      │
│  3. POST /api/v1/interviews/{id}/feedbacks                       │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    BACKEND (피드백 생성)                           │
│                                                                    │
│  AnswerData[] 저장 → Claude API 호출 → 피드백 파싱 → DB 저장    │
│  (claude-sonnet-4-20250514, max_tokens=4096)                     │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                  INTERVIEW REVIEW PAGE (리뷰)                     │
│                                                                    │
│  VideoPlayer (IndexedDB Blob) ←→ FeedbackTimeline (마커 클릭)   │
│  타임스탬프 동기화: video.currentTime ↔ feedback.timestampSeconds│
└──────────────────────────────────────────────────────────────────┘
```

---

## 1. 영상 녹화

| 항목 | 값 |
|------|-----|
| API | MediaRecorder |
| 포맷 | WebM (VP9 우선, 폴백 WebM 기본 코덱) |
| 청크 간격 | 1000ms |
| 저장 방식 | `Blob[]` 누적 → 종료 시 단일 Blob 병합 |

### 핵심 파일

- `frontend/src/hooks/use-media-recorder.ts` — 녹화 시작/중지, 청크 수집
- `frontend/src/hooks/use-media-stream.ts` — 카메라/마이크 스트림 획득

### 녹화 흐름

```
useMediaStream().start()          // getUserMedia (video + audio)
       ↓
useMediaRecorder().start(stream)  // MediaRecorder 생성, 1초 청크
       ↓
ondataavailable → chunksRef[]     // 청크 누적
       ↓
useMediaRecorder().stop()         // Blob 병합 → Promise<Blob>
```

---

## 2. 영상 저장

**저장소: 클라이언트 전용 (서버 업로드 없음)**

| 저장소 | 용도 | 수명 |
|--------|------|------|
| Zustand `videoBlobUrl` | 즉시 재생용 (Blob URL) | 세션 내 |
| IndexedDB (`rehearse-video-storage`) | 새로고침 후에도 유지 | 브라우저 정책 |

### 핵심 파일

- `frontend/src/lib/video-storage.ts` — IndexedDB CRUD (`saveVideoBlob`, `loadVideoBlob`, `deleteVideoBlob`)
- `frontend/src/stores/interview-store.ts` — Zustand에 `videoBlobUrl` 관리

### 저장 흐름

```
면접 종료 → recorder.stop() → Blob
  ├── setVideoBlob(blob)                    // Zustand (메모리, Blob URL 생성)
  └── saveVideoBlob(interviewId, blob)      // IndexedDB (비동기 백그라운드)
```

### 로드 우선순위

1. Zustand store의 `videoBlobUrl` (메모리)
2. IndexedDB에서 `loadVideoBlob(interviewId)` → `URL.createObjectURL`
3. 둘 다 없으면 → "녹화 영상이 없습니다"

---

## 3. 실시간 분석

> **모든 분석은 브라우저에서 실시간으로 수행됩니다.** 서버로 원본 영상/오디오를 전송하지 않습니다.

### 3.1 시선(Gaze) 분석 — 실시간

| 항목 | 값 |
|------|-----|
| 라이브러리 | @mediapipe/tasks-vision (FaceLandmarker) |
| 모드 | `runningMode: 'VIDEO'` (실시간) |
| 프레임 레이트 | ~10fps (100ms 스로틀) |
| 이벤트 임계값 | 시선 이탈 3초 이상 지속 시 기록 |

**알고리즘**: 양쪽 홍채 중심 → 눈 너비 기준 정규화 → ±0.15 범위 밖이면 이탈 판정

**핵심 파일**:
- `frontend/src/hooks/use-face-mesh.ts` — FaceLandmarker 초기화, 프레임 루프
- `frontend/src/lib/mediapipe/face-analyzer.ts` — 시선 방향 계산
- `frontend/src/lib/mediapipe/event-detector.ts` — 이벤트 감지 (3초+ 지속)

### 3.2 자세(Posture) 분석 — 실시간

| 항목 | 값 |
|------|-----|
| 라이브러리 | @mediapipe/tasks-vision (PoseLandmarker) |
| 모드 | `runningMode: 'VIDEO'` (실시간) |
| 프레임 레이트 | ~10fps |
| 이벤트 임계값 | 어깨 기울기 10도 이상, 3초 지속 |

**알고리즘**: 좌우 어깨 좌표 → atan2로 기울기 각도 → 10도 이상이면 기울기 판정

**핵심 파일**:
- `frontend/src/hooks/use-pose-detection.ts` — PoseLandmarker 초기화, 프레임 루프
- `frontend/src/lib/mediapipe/pose-analyzer.ts` — 기울기 계산

### 3.3 음성(Voice) 분석 — 실시간

| 항목 | 값 |
|------|-----|
| API | Web Audio API (AnalyserNode) |
| FFT 크기 | 256 |
| 스무딩 | 0.8 |
| 분석 레이트 | ~30fps (33ms 스로틀) |
| 침묵 감지 | -50dB 이하 3초 이상 지속 시 기록 |

**데이터**: 오디오 레벨 (0~1 정규화), 침묵 이벤트 (duration)

**핵심 파일**:
- `frontend/src/hooks/use-audio-analyzer.ts` — AudioContext/AnalyserNode 설정, 침묵 감지

### 3.4 음성 인식(STT) — 실시간

| 항목 | 값 |
|------|-----|
| API | Web Speech API (SpeechRecognition) |
| 언어 | `ko-KR` |
| 모드 | `continuous: true`, `interimResults: true` |
| 재시도 | 지수 백오프, 최대 3회 |

**흐름**:
- interim 결과 → UI에 실시간 표시
- final 결과 → `TranscriptSegment` 생성 (타임스탬프 포함) → Zustand store 누적

**핵심 파일**:
- `frontend/src/hooks/use-speech-recognition.ts` — SpeechRecognition 관리, 재시도 로직

### 3.5 실시간 음성 변환(Voice Conversion)

**미구현.** 음성 변환 기능은 없습니다. 음성 데이터는:
- STT로 텍스트 변환
- 음량/침묵 분석 후 요약 텍스트화
- Claude에게 텍스트 형태로만 전달

---

## 4. 분석 데이터 수집 구조

면접 중 모든 분석 결과는 **질문 단위**로 Zustand store에 누적됩니다.

```
QuestionAnswer (질문별)
├── questionIndex: number
├── startTime: number (ms)
├── endTime: number (ms)
├── transcripts: TranscriptSegment[]
│   └── { text, startTime, endTime, isFinal }
├── nonVerbalEvents: NonVerbalEvent[]
│   └── { timestamp, type: 'gaze'|'posture', severity, data }
└── voiceEvents: VoiceEvent[]
    └── { timestamp, type: 'silence', duration, value }
```

면접 완료 시 변환:

```
QuestionAnswer[] → AnswerData[]
├── questionIndex
├── questionContent
├── answerText         // transcript join
├── nonVerbalSummary   // "gaze: 시선 우측 이탈, posture: 좌측 기울어짐"
└── voiceSummary       // "silence(3000ms), silence(2500ms)"
```

---

## 5. AI 피드백 생성 (Backend)

### API 엔드포인트

```
POST /api/v1/interviews/{id}/feedbacks
Body: GenerateFeedbackRequest { answers: AnswerData[] }
```

### 처리 흐름

1. `AnswerData[]` → `InterviewAnswer` 엔티티 저장
2. `answersJson` + 시스템/유저 프롬프트 → Claude API 호출
3. Claude 응답 JSON 파싱 → `GeneratedFeedback[]`
4. DB 저장 → `FeedbackListResponse` 반환

### Claude API 호출 상세

| 항목 | 값 |
|------|-----|
| 모델 | `claude-sonnet-4-20250514` |
| max_tokens | 4096 |
| 엔드포인트 | `https://api.anthropic.com/v1/messages` |

### 피드백 구조

```
GeneratedFeedback
├── timestampSeconds: number     // 영상 내 시점 (초)
├── category: VERBAL | NON_VERBAL | CONTENT
├── severity: INFO | WARNING | SUGGESTION
├── content: string              // 피드백 내용
└── suggestion: string | null    // 개선 방법
```

### 핵심 파일

- `backend/.../feedback/controller/FeedbackController.java`
- `backend/.../feedback/service/FeedbackService.java`
- `backend/.../infra/ai/ClaudeApiClient.java`
- `backend/.../infra/ai/ClaudePromptBuilder.java`
- `backend/.../infra/ai/ClaudeResponseParser.java`

---

## 6. 영상 재생 + 타임스탬프 피드백 UI

### 페이지 구조

```
InterviewReviewPage
├── VideoPlayer (좌측 60%)
│   ├── <video> (IndexedDB Blob URL)
│   └── 기본 브라우저 컨트롤
├── FeedbackTimeline
│   └── 타임라인 마커 (category별 색상, 위치 = timestampSeconds/totalDuration)
└── FeedbackPanel (우측 40%)
    └── 피드백 목록 (선택 시 하이라이트)
```

### 타임스탬프 동기화

```
마커 클릭 → seekToFeedback(feedbackId)
         → video.currentTime = feedback.timestampSeconds
         → selectFeedback(feedbackId) // UI 하이라이트

영상 재생 → video 'timeupdate' 이벤트
          → setCurrentTime(video.currentTime)
          → 현재 시간 근처 피드백 표시
```

### 상태 관리

`frontend/src/stores/review-store.ts`:
- `currentTime` — 비디오 현재 시간 (초)
- `feedbacks` — 타임스탬프 피드백 목록
- `selectedFeedbackId` — 선택된 피드백
- `isPlaying` — 재생 상태

### 핵심 파일

- `frontend/src/pages/interview-review-page.tsx`
- `frontend/src/components/review/video-player.tsx`
- `frontend/src/components/review/feedback-timeline.tsx`
- `frontend/src/hooks/use-video-sync.ts`

---

## 7. 실시간 분석 여부 요약

| 기능 | 실시간 | 설명 |
|------|--------|------|
| 영상 녹화 | ✅ | MediaRecorder 스트리밍 (1초 청크) |
| 시선 분석 | ✅ | MediaPipe FaceLandmarker (10fps) |
| 자세 분석 | ✅ | MediaPipe PoseLandmarker (10fps) |
| 음성 레벨/침묵 | ✅ | Web Audio AnalyserNode (30fps) |
| STT | ✅ | Web Speech API (연속 + interim) |
| 음성 변환 | ❌ | 미구현 — STT + 요약 텍스트만 |
| AI 피드백 | ❌ (후처리) | 면접 완료 후 Claude API 호출 |
| 영상 서버 업로드 | ❌ | 클라이언트 IndexedDB에만 저장 |

---

## 8. 아키텍처 특징

### 클라이언트 중심 설계
- 모든 실시간 분석은 **브라우저에서** 수행 (서버 부하 없음)
- 영상은 **클라이언트에만** 저장 (IndexedDB, 서버 미전송)
- 서버에는 **텍스트 요약만** 전달

### 성능 최적화
- MediaPipe: 60fps → 10fps 스로틀 (CPU 부하 감소)
- Audio: 30fps 분석 + 100ms 이벤트 체크 (균형)
- 이벤트 필터링: 3초 이상 지속된 이벤트만 기록 (노이즈 제거)

### 한계
- IndexedDB 용량은 브라우저 정책에 의존
- Web Speech API는 Chrome/Edge 위주 지원
- 영상이 클라이언트에만 있으므로, 브라우저 데이터 삭제 시 영상 소실
