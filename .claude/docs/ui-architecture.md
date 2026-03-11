# Rehearse UI Architecture

사용자 여정(User Journey)에 따른 페이지 설계와 컴포넌트 구조입니다.

---

## 라우트 구조

```
/                           → HomePage
/interview/setup            → InterviewSetupPage
/interview/:id/ready        → InterviewReadyPage
/interview/:id/conduct      → InterviewPage
/interview/:id/complete     → InterviewCompletePage
/interview/:id/review       → InterviewReviewPage
/interview/:id/report       → InterviewReportPage
```

---

## 1. Landing Page (`home-page.tsx`)

- **Role**: 서비스 가치 전달 및 신뢰 구축
- **Layout**: Sticky header + 3개 섹션 (Hero, Journey, Final CTA)
- **Strategy**: Journey-based Storytelling — [설정 → 면접 → 리포트] 미니어처 카드를 스크롤 타임라인으로 배치
- **Components**: `Logo`, `Character`, 3단계 소개 카드
- **Hooks**: `useNavigate()`, `useFadeInOnScroll()` (3개 섹션별 애니메이션)
- **Visual**: 토스 스타일 섀도우, 모바일 반응형, Fade-in 스크롤 애니메이션

---

## 2. Interview Setup (`interview-setup-page.tsx`)

- **Role**: 면접 설정 수집 — 4단계 위저드
- **Layout**: Header + Progress Bar + Step Content + Navigation Buttons

### 위저드 스텝

| Step | 내용 | UI |
|------|------|----|
| 1 | 직무 선택 (Position) | 2x3 그리드 `SelectionCard` |
| 2 | 레벨 선택 (Level) | 리스트형 버튼 |
| 3 | 시간 선택 (Duration) | 2x2 프리셋 카드 (5/15/30/60분) |
| 4 | 면접 유형 선택 + CS 세부주제 + 이력서 업로드 | 복수 선택 + 조건부 UI |

- **Hooks**: `useCreateInterview()`, `useNavigate()`
- **State**: `currentStep`, `position`, `level`, `durationMinutes`, `interviewTypes`, `csSubTopics`, `resumeFile`
- **Features**:
  - PDF 드래그앤드롭 + 파일 선택 (10MB 제한)
  - Position 변경 시 InterviewType 필터링
  - CS_FUNDAMENTAL 토글 → 하위 주제 옵션 표시
  - RESUME_BASED 토글 → 파일 업로드 영역 표시
  - Progress bar (원형 + 선형), 스텝 전환 애니메이션
- **Visual**: 토스의 '한 번에 하나씩' 철학 적용

---

## 3. Interview Ready (`interview-ready-page.tsx`)

- **Role**: 기기 테스트 및 면접 시작 게이트
- **Layout**: Header + Intro (설정 태그 표시) + DeviceTestSection + Start Button
- **Components**: `DeviceTestSection` (카메라/마이크/스피커 테스트), `Character` (에러 상태)
- **Hooks**: `useInterview()`, `useUpdateInterviewStatus()`, `useDeviceTest()`
- **State**: `useDeviceTest` → `state`, `micLevel`, `videoRef`, `allPassed`
- **Features**:
  - 3단계 기기 테스트 (카메라 → 마이크 → 스피커)
  - `allPassed` 체크로 시작 버튼 활성화 제어
  - 면접 설정 태그 표시 (Position, Level, Types, Duration)
  - 404 에러 처리 (`Character mood="confused"`)

---

## 4. Interview Studio (`interview-page.tsx`)

- **Role**: 몰입도 높은 실전 면접 경험 제공
- **Layout**: Header (타이머 + 오디오 레벨) + Dual-view Main

### 레이아웃 구조

```
┌─────────────────────────────────────────────────┐
│  Header: InterviewTimer │ AudioLevelIndicator    │
├────────────────────────┬────────────────────────┤
│                        │                        │
│  좌측: AI 면접관        │  우측: 사용자           │
│  • Character           │  • VideoPreview        │
│  • AudioWaveform       │  • TranscriptDisplay   │
│    (TTS 재생 중)       │    (실시간 STT)        │
│  • QuestionCard        │  • InterviewControls   │
│    (현재 질문)         │    (버튼 컨트롤)       │
│                        │                        │
├────────────────────────┴────────────────────────┤
│  AutoTransition 토스트 메시지                     │
└─────────────────────────────────────────────────┘
```

- **Components**: `InterviewTimer`, `AudioLevelIndicator`, `AudioWaveform`, `VideoPreview`, `TranscriptDisplay`, `InterviewControls`, `QuestionCard`, `Character`
- **Hooks**:
  - `useInterviewStore()` — Zustand 면접 상태 (phase, questions, answers, currentQuestionIndex)
  - `useInterview()` — 면접 정보 조회
  - `useMediaStream()` — 웹캠 스트림
  - `useMediaRecorder()` — WebM 영상 녹화
  - `useSpeechRecognition()` — Web Speech API STT
  - `useAudioAnalyzer()` — Web Audio API 음성 분석
  - `useInterviewSession()` — 면접 세션 오케스트레이션
- **Phase 흐름**: `preparing → greeting → ready → recording ⇄ paused → completed`
- **Features**:
  - VAD 기반 자동 녹음 시작/종료
  - TTS 질문 읽기 + 에코 방지
  - 실시간 STT (interim + final 분리)
  - 침묵 감지 → 자동 다음 질문 전환
  - 타이머 경고 (2분 전)
  - 후속질문 로딩 인디케이터

---

## 5. Interview Complete (`interview-complete-page.tsx`)

- **Role**: AI 피드백 자동 생성 + 결과 안내
- **Layout**: 중앙 정렬 카드 (Character + 메시지 + 상태별 UI)
- **Components**: `Character`, `AnalysisProgress` (4단계 애니메이션)
- **Hooks**: `useInterviewStore()`, `useGenerateFeedback()`

### 상태별 UI

| 상태 | UI |
|------|----|
| `isPending` | AnalysisProgress — 4단계 진행 표시 (3초 간격) |
| `isSuccess` | "분석 완료" + 2개 버튼 (타임스탬프 리뷰, 종합 리포트) |
| `isError` | "분석 실패" + 홈으로 버튼 |

- **Features**:
  - 페이지 진입 시 자동 피드백 생성 요청 (useEffect)
  - AnswerData 변환: 최종 STT 텍스트 + 비언어/음성 분석 요약

---

## 6. Feedback Review (`interview-review-page.tsx`)

- **Role**: 타임스탬프 기반 피드백과 영상 동기화 리뷰
- **Layout**: Header + Main (좌60%: 비디오 + 타임라인, 우40%: 피드백 패널)

### 레이아웃 구조

```
┌─────────────────────────────────────────────────┐
│  Header: BackLink │ "종합 리포트" 버튼           │
├──────────────────────────┬──────────────────────┤
│                          │                      │
│  VideoPlayer (60%)       │  FeedbackPanel (40%) │
│  ┌──────────────────┐    │  ┌────────────────┐  │
│  │  녹화 영상 재생   │    │  │ 📝 피드백 #1   │  │
│  └──────────────────┘    │  │ 📝 피드백 #2   │  │
│  ┌──────────────────┐    │  │ 📝 피드백 #3   │  │
│  │ FeedbackTimeline  │    │  │ ...            │  │
│  │ ●──●────●──●───── │    │  └────────────────┘  │
│  └──────────────────┘    │  (Sticky 스크롤)     │
│                          │                      │
└──────────────────────────┴──────────────────────┘
```

- **Components**: `VideoPlayer`, `FeedbackTimeline`, `FeedbackPanel`, `TimelineMarker`
- **Hooks**: `useFeedbacks()`, `useReviewStore()`, `useVideoSync()`
- **Features**:
  - 피드백 클릭 → 영상 해당 시점으로 seek
  - 영상 재생 중 → 현재 시점 피드백 하이라이트
  - 우측 패널 Sticky 스크롤

---

## 7. Insight Report (`interview-report-page.tsx`)

- **Role**: 성취감 고취 및 구체적 교정 가이드 제공
- **Layout**: Sticky Header + Score Hero + 4개 섹션

### 섹션 구조

```
┌─────────────────────────────────────┐
│  Score Hero                         │
│  • 종합 점수 (0~100) 대담한 표기     │
│  • 점수별 동적 메시지 (80+ vs <80)   │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│  Summary Card                       │
│  • 종합 평가 텍스트 (2~3문장)        │
│  • 피드백 개수 표시                  │
└─────────────────────────────────────┘
┌────────────────────┬────────────────┐
│  ✅ 강점 (2~5개)   │ ⚠️ 보완점 (2~5개)│
│  • 강점1           │ • 개선점1       │
│  • 강점2           │ • 개선점2       │
└────────────────────┴────────────────┘
┌─────────────────────────────────────┐
│  CTA: "타임스탬프 리뷰 보기"         │
│  → /interview/{id}/review 이동      │
│  (Dark 배경 블랙 버튼)              │
└─────────────────────────────────────┘
```

- **Components**: `ScoreCard`, `ImprovementList`
- **Hooks**: `useReport()`
- **Visual**: Clean Data Report 스타일, 면(Surface) 기반 카드

---

## 컴포넌트 계층 구조

```
src/components/
├── ui/                          # 범용 UI 컴포넌트
│   ├── button.tsx               # 공통 버튼 (variant: primary/secondary/ghost)
│   ├── selection-card.tsx       # 선택 카드 (Setup 위저드)
│   ├── text-input.tsx           # 텍스트 입력
│   ├── skeleton.tsx             # 로딩 스켈레톤
│   ├── spinner.tsx              # 로딩 스피너
│   ├── back-link.tsx            # 뒤로가기 링크
│   ├── logo.tsx / logo-icon.tsx # 로고
│   └── character.tsx            # AI 캐릭터 (mood별 표정)
│
├── icons/                       # SVG 아이콘 컴포넌트
│   ├── code-icon.tsx
│   ├── server-icon.tsx
│   ├── globe-icon.tsx
│   └── ... (9개)
│
├── interview/                   # 면접 진행 관련
│   ├── video-preview.tsx        # 사용자 비디오 프리뷰
│   ├── transcript-display.tsx   # 실시간 STT 자막
│   ├── audio-level-indicator.tsx # 음성 레벨 바
│   ├── audio-waveform.tsx       # TTS 재생 중 파형 애니메이션
│   ├── interview-timer.tsx      # 경과 시간 타이머
│   ├── interview-controls.tsx   # 답변 중지/다음/종료 버튼
│   ├── question-card.tsx        # 현재 질문 표시
│   ├── question-card-skeleton.tsx
│   ├── question-display.tsx     # 질문 목록 표시
│   └── device-test-section.tsx  # 기기 테스트 (Ready 페이지)
│
└── review/                      # 리뷰/리포트 관련
    ├── video-player.tsx         # 녹화 영상 플레이어
    ├── feedback-timeline.tsx    # 타임라인 (피드백 위치 마커)
    ├── timeline-marker.tsx      # 타임라인 마커
    ├── feedback-panel.tsx       # 피드백 상세 패널
    ├── score-card.tsx           # 종합 점수 카드
    └── improvement-list.tsx     # 강점/보완점 리스트
```

---

## 상태 관리 전략

| 계층 | 도구 | 용도 | 페이지 |
|------|------|------|--------|
| **서버 상태** | TanStack Query | API 캐싱, 자동 리페치 | 전체 |
| **글로벌 클라이언트** | Zustand (`interview-store`) | 면접 진행 상태 (phase, answers, transcripts) | Interview, Complete |
| **글로벌 클라이언트** | Zustand (`review-store`) | 리뷰 피드백 상태 | Review |
| **로컬 UI** | `useState` | 위저드 step, 기기 테스트, UI 토글 | 각 페이지 |

---

## 훅 의존 관계 (면접 진행 페이지)

```
InterviewPage
├── useInterview()                    ← TanStack Query (서버 데이터)
├── useInterviewStore()               ← Zustand (클라이언트 상태)
├── useMediaStream()                  ← getUserMedia
├── useMediaRecorder()                ← MediaRecorder API
├── useSpeechRecognition()            ← Web Speech API
├── useAudioAnalyzer()                ← Web Audio API (AnalyserNode)
│   └── audioLevelRef                 → useVad (ref 기반 동기화)
└── useInterviewSession()             ← 오케스트레이션 훅
    ├── useVad()                      ← 음성 감지 (적응형 threshold)
    ├── useTts()                      ← 질문 읽기 (Chrome 15s 워크어라운드)
    ├── useThinkingTimeDetector()     ← "생각할 시간" 감지
    └── useInterviewEventRecorder()   ← 이벤트 로깅
```

---

## 디자인 원칙

- **토스 스타일**: 모노톤 + coral accent, Pretendard 폰트, 미니멀
- **한 번에 하나씩**: Setup 위저드 스텝 분리
- **Cinematic Dark Mode**: 면접 진행 페이지 몰입감
- **Clean Data Report**: 리포트/리뷰 페이지 데이터 시각화
- **반응형**: 모바일 ~ 데스크톱 대응
- **접근성**: 키보드 네비게이션, ARIA 라벨
