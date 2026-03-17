# Feature Specification: 면접 진행 페이지 (영상 녹화 + STT + 비언어 분석)

> **문서 ID**: PLAN-002
> **작성일**: 2026-03-10
> **작성자**: Planner
> **상태**: Draft
> **우선순위**: P0 (Must-have)
> **의존성**: PLAN-001 (면접 세션 생성) 완료

---

## Overview

### 문제 정의

면접 세션이 생성되고 질문이 준비되었지만, 실제 면접을 진행하는 화면이 없다. 사용자가 카메라/마이크를 통해 응답하고, 비언어적 행동을 실시간 추적하며, 음성을 텍스트로 변환하는 핵심 기능이 필요하다.

### 솔루션 요약

면접 진행 페이지에서 사용자가 AI 면접관의 질문에 카메라/마이크로 응답하면:
1. **MediaRecorder**가 영상을 녹화하고
2. **Web Speech API**가 음성을 실시간 텍스트로 변환하고
3. **Web Audio API**가 음성 패턴(음량/속도/침묵)을 분석하고
4. **MediaPipe**가 시선/표정/자세를 실시간 추적한다

모든 데이터는 브라우저 메모리/Zustand 스토어에 타임스탬프와 함께 저장된다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | Critical - 이 기능이 Rehearse의 핵심 사용 경험. 면접 없이는 피드백/리포트 불가 |
| **Effort** | High - MediaPipe 통합, 영상 녹화, STT, 오디오 분석, 복잡한 상태 관리 |
| **결론** | **P0** - Critical Impact. 단계별 구현으로 복잡도 관리 |

---

## User Stories

### US-1: 면접 시작 및 미디어 권한

**As a** 면접 준비 중인 개발자
**I want to** 카메라/마이크 권한을 허용하고 면접을 시작하고 싶다
**So that** AI 면접관과 실시간으로 면접을 진행할 수 있다

**Acceptance Criteria:**
- [ ] interview-ready 페이지에서 "면접 시작" 클릭 시 면접 진행 페이지로 이동
- [ ] 카메라/마이크 권한 요청 다이얼로그가 표시된다
- [ ] 권한 거부 시 안내 메시지와 함께 권한 재요청 방법을 안내한다
- [ ] 카메라 프리뷰가 표시되어 사용자가 본인 화면을 확인할 수 있다
- [ ] 면접 상태가 IN_PROGRESS로 변경된다 (PATCH /api/v1/interviews/{id}/status)

### US-2: 질문 표시 및 응답 녹화

**As a** 면접 진행 중인 사용자
**I want to** AI 면접관의 질문을 보고 음성으로 답변하고 싶다
**So that** 실제 면접처럼 질문에 응답할 수 있다

**Acceptance Criteria:**
- [ ] 현재 질문이 화면 상단에 표시된다 (질문 번호, 카테고리, 내용)
- [ ] "답변 시작" 버튼으로 녹화/STT를 시작한다
- [ ] "답변 완료" 버튼으로 현재 질문 답변을 종료한다
- [ ] MediaRecorder로 영상이 녹화된다 (WebM/VP8)
- [ ] 답변 중 경과시간 타이머가 표시된다
- [ ] 질문 간 전환 시 이전 답변 데이터가 유지된다
- [ ] TTS로 질문을 음성으로 읽어주는 기능 (선택적, Web Speech Synthesis)

### US-3: 실시간 STT 변환

**As a** 면접 진행 중인 사용자
**I want to** 내 답변이 실시간으로 텍스트로 표시되는 것을 보고 싶다
**So that** 내 답변이 제대로 인식되고 있는지 확인할 수 있다

**Acceptance Criteria:**
- [ ] Web Speech API로 실시간 음성→텍스트 변환이 진행된다
- [ ] 실시간 인식 중인 텍스트가 화면 하단에 표시된다 (interim results)
- [ ] 최종 인식 결과는 타임스탬프와 함께 저장된다
- [ ] 브라우저 미지원 시 안내 메시지 표시 (Chrome 권장)
- [ ] STT 결과는 후속 질문 생성 및 피드백에 사용된다

### US-4: 비언어 분석 (MediaPipe)

**As a** 면접 진행 중인 사용자
**I want to** 면접 중 나의 비언어적 행동이 자동으로 추적되길 원한다
**So that** 면접 종료 후 시선/표정/자세에 대한 피드백을 받을 수 있다

**Acceptance Criteria:**
- [ ] MediaPipe Face Mesh가 시선 방향을 실시간 추적한다
- [ ] MediaPipe Pose가 자세(어깨 기울기 등)를 실시간 추적한다
- [ ] 이벤트 트리거 조건 충족 시 NonVerbalEvent가 기록된다
- [ ] 이벤트: 시선 3초+ 이탈, 표정 경직, 어깨 기울임, 손 터치
- [ ] 분석은 별도 워커/requestAnimationFrame에서 비동기로 동작하여 UI 블로킹 없음
- [ ] 분석 결과는 화면에 실시간으로 표시하지 않음 (면접 집중을 위해)

### US-5: 음성 분석 (Web Audio API)

**As a** 면접 진행 중인 사용자
**I want to** 면접 중 나의 음성 패턴이 자동으로 분석되길 원한다
**So that** 말 빠르기, 침묵, 필러 단어에 대한 피드백을 받을 수 있다

**Acceptance Criteria:**
- [ ] Web Audio API로 음량(dB)을 실시간 모니터링한다
- [ ] 3초 이상 침묵 감지 시 VoiceEvent가 기록된다
- [ ] 음성 데이터는 타임스탬프와 함께 저장된다
- [ ] 음량 레벨 인디케이터가 화면에 표시된다 (마이크 동작 확인용)

### US-6: 면접 종료 및 데이터 저장

**As a** 면접을 마친 사용자
**I want to** 면접을 종료하고 결과 데이터가 저장되길 원한다
**So that** 이후 AI 피드백과 리포트를 받을 수 있다

**Acceptance Criteria:**
- [ ] 모든 질문 답변 완료 후 "면접 종료" 버튼이 활성화된다
- [ ] 면접 종료 시 녹화가 중지되고 Blob URL이 생성된다
- [ ] 면접 상태가 COMPLETED로 변경된다
- [ ] STT 결과, 비언어 이벤트, 음성 이벤트가 Zustand 스토어에 저장된다
- [ ] 종료 후 피드백 리뷰 페이지로 이동한다 (향후 구현)

---

## 구현 범위

### Phase 1 (이번 구현 - 핵심)

| 구분 | 항목 | 설명 |
|------|------|------|
| Frontend | interview-page.tsx | 면접 진행 메인 페이지 |
| Frontend | use-media-stream.ts | 카메라/마이크 스트림 훅 |
| Frontend | use-media-recorder.ts | 영상 녹화 훅 |
| Frontend | use-speech-recognition.ts | STT 훅 (Web Speech API) |
| Frontend | use-audio-analyzer.ts | 음성 분석 훅 (Web Audio API) |
| Frontend | interview-store.ts | 면접 진행 상태 관리 (Zustand) |
| Frontend | video-preview.tsx | 카메라 프리뷰 컴포넌트 |
| Frontend | question-display.tsx | 현재 질문 표시 컴포넌트 |
| Frontend | transcript-display.tsx | 실시간 STT 표시 컴포넌트 |
| Frontend | interview-controls.tsx | 답변 시작/완료/면접 종료 컨트롤 |
| Frontend | audio-level-indicator.tsx | 마이크 음량 인디케이터 |
| Frontend | interview-timer.tsx | 경과시간 타이머 |
| Types | interview.ts 확장 | NonVerbalEvent, TranscriptSegment 등 타입 추가 |

### Phase 2 (이번 구현 - MediaPipe)

| 구분 | 항목 | 설명 |
|------|------|------|
| Frontend | use-face-mesh.ts | MediaPipe Face Mesh 훅 |
| Frontend | use-pose-detection.ts | MediaPipe Pose 훅 |
| Frontend | lib/mediapipe/face-analyzer.ts | 시선/표정 분석 로직 |
| Frontend | lib/mediapipe/pose-analyzer.ts | 자세 분석 로직 |
| Frontend | lib/mediapipe/event-detector.ts | 이벤트 트리거 판정 로직 |

### 미포함 (후속 기능)

- 후속 질문 생성 (별도 PLAN-003)
- AI 피드백 생성 (별도 PLAN-004)
- 피드백 리뷰 페이지 (별도 PLAN-005)
- 종합 리포트 (별도 PLAN-006)

---

## UI 레이아웃

```
┌─────────────────────────────────────────────────┐
│  Rehearse    면접 진행 중   ●REC  00:05:23       │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │  Q1 / 5  [자료구조]                     │    │
│  │                                         │    │
│  │  HashMap과 TreeMap의 차이점과 각각의    │    │
│  │  시간 복잡도를 설명해주세요.            │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │                                         │    │
│  │           [ 카메라 프리뷰 ]              │    │
│  │            (사용자 영상)                 │    │
│  │                                         │    │
│  │  🎤 ████████░░░░  (음량 레벨)           │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │  실시간 자막:                           │    │
│  │  "HashMap은 해시 테이블 기반으로..."    │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  ┌──────────────┐  ┌──────────────────────┐    │
│  │  답변 완료    │  │   면접 종료 (5/5)    │    │
│  └──────────────┘  └──────────────────────┘    │
│                                                 │
│  ← 이전 질문              다음 질문 →          │
└─────────────────────────────────────────────────┘
```

---

## 데이터 모델 (Zustand Store)

```typescript
interface InterviewStore {
  // 면접 기본 정보
  interviewId: number | null;
  questions: Question[];
  currentQuestionIndex: number;
  status: 'preparing' | 'recording' | 'paused' | 'completed';

  // 타이머
  startTime: number | null;
  elapsedTime: number;

  // 미디어
  mediaStream: MediaStream | null;
  videoBlob: Blob | null;
  videoBlobUrl: string | null;

  // STT 결과
  transcripts: TranscriptSegment[];
  currentTranscript: string; // 실시간 interim

  // 비언어 이벤트
  nonVerbalEvents: NonVerbalEvent[];

  // 음성 이벤트
  voiceEvents: VoiceEvent[];

  // 액션
  setInterview: (id: number, questions: Question[]) => void;
  startRecording: () => void;
  stopRecording: () => void;
  nextQuestion: () => void;
  prevQuestion: () => void;
  addTranscript: (segment: TranscriptSegment) => void;
  addNonVerbalEvent: (event: NonVerbalEvent) => void;
  addVoiceEvent: (event: VoiceEvent) => void;
  completeInterview: () => void;
}
```

---

## Task 분해

| # | Task | 담당 | 의존성 | 예상 |
|---|------|------|--------|------|
| T1 | 타입 정의 확장 (NonVerbalEvent, TranscriptSegment 등) | Frontend | - | Small |
| T2 | interview-store.ts (Zustand) | Frontend | T1 | Medium |
| T3 | use-media-stream.ts (카메라/마이크) | Frontend | - | Small |
| T4 | use-media-recorder.ts (영상 녹화) | Frontend | T3 | Medium |
| T5 | use-speech-recognition.ts (STT) | Frontend | T3 | Medium |
| T6 | use-audio-analyzer.ts (음성 분석) | Frontend | T3 | Medium |
| T7 | UI 컴포넌트 (video-preview, question-display, transcript-display, controls, timer, audio-level) | Frontend | T2 | Large |
| T8 | interview-page.tsx (메인 조합) | Frontend | T2~T7 | Large |
| T9 | MediaPipe Face Mesh 통합 | Frontend | T3 | Large |
| T10 | MediaPipe Pose 통합 | Frontend | T3, T9 | Medium |
| T11 | 이벤트 트리거 판정 로직 | Frontend | T9, T10 | Medium |
| T12 | 라우팅 연결 + 상태 전이 연동 | Frontend | T8 | Small |

**구현 순서**: T1 → T2 → T3 → T4, T5, T6 (병렬) → T7 → T8 → T12 → T9 → T10 → T11
