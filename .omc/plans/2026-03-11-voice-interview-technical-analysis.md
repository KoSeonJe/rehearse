# 음성 기반 AI 모의면접 시스템 기술 분석

- **작성일**: 2026-03-11
- **상태**: Analysis Complete
- **분석 대상**: 8개 훅 + interview-page.tsx

---

## 1. 시스템 아키텍처 개요

### 1.1 핵심 흐름

```
[TTS 질문 발화] → [VAD 음성 감지] → [녹음 시작 + STT] → [침묵 감지] → [답변 완료] → [후속질문/다음질문]
```

### 1.2 컴포넌트 구조

| 훅 | 역할 | 기반 API |
|---|---|---|
| `use-tts` | 면접관 질문 발화 | Web Speech Synthesis |
| `use-vad` | 음성 활동 감지 | 에너지 기반 (audioLevel threshold) |
| `use-audio-analyzer` | 오디오 레벨 분석 | AudioContext + AnalyserNode |
| `use-speech-recognition` | 음성→텍스트 변환 | Web Speech API (STT) |
| `use-media-recorder` | 영상 녹화 | MediaRecorder API |
| `use-media-stream` | 카메라/마이크 획득 | getUserMedia |
| `use-interview-session` | 전체 오케스트레이션 | Zustand store + phase 기반 |
| `use-thinking-time-detector` | "잠시만" 키워드 감지 | interimText 패턴 매칭 |

### 1.3 Phase 흐름

```
preparing → greeting → ready → recording ⇄ paused → completed
```

---

## 2. 기술 영역별 분석: 현재 구현 vs 베스트 프랙티스

### 2.1 Web Speech API (STT)

#### 현재 구현 (`use-speech-recognition.ts`)

- `continuous: true` + `interimResults: true` (line 55-56)
- `onend`에서 `shouldRestartRef` 확인 후 재시작 (line 92-101)
- `no-speech`, `aborted` 에러만 무시, 나머지는 `isListening = false` (line 87-89)
- `start()` 시 매번 새 recognition 인스턴스 생성 (line 115)

#### 알려진 제약사항

| 제약사항 | 상세 | 현재 대응 |
|---|---|---|
| **60초 자동 종료** | Chrome은 Google 서버 세션 정책으로 ~60초 후 강제 종료 | `onend` 재시작 (표준 워크어라운드) |
| **재시작 갭** | `onend` → `recognition.start()` 사이 수백ms 공백 | 대응 없음 — **발화 유실 가능** |
| **서버 의존성** | Google 서버로 오디오 전송. 오프라인 불가 | 대응 없음 |
| **브라우저 제한** | Chrome/Edge만 지원. Safari/Firefox 미지원 | 대응 없음 |
| **한국어 정확도** | 기술 용어, 복합어, 띄어쓰기에 약함 | 대응 없음 |
| **Rate Limiting** | 빈번한 재시작 시 Google 레이트 제한 가능 | 대응 없음 |

**출처**: [Web Speech API limit of 60 seconds? - Google Groups](https://groups.google.com/a/chromium.org/g/chromium-html5/c/s2XhT-Y5qAc), [MDN - SpeechRecognitionErrorEvent](https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognitionErrorEvent/error)

#### 베스트 프랙티스

**1. 재시작 전략 — onend + 타이머 백업:**
```javascript
// onend가 트리거되지 않을 수 있으므로 타이머 백업 필수
const endTimeout = setTimeout(() => {
  handleRecognitionEnd()
}, 65000)

recognition.onend = () => {
  clearTimeout(endTimeout)
  handleRecognitionEnd()
}
```

**2. 에러 타입별 처리:**
```javascript
recognition.onerror = (event) => {
  switch (event.error) {
    case 'network':
      // 지수 백오프 재시도 (1s → 2s → 4s, 최대 3회)
      retryWithBackoff()
      break
    case 'no-speech':
      // 사용자에게 마이크 상태 안내
      break
    case 'not-allowed':
      // 권한 요청 UI 표시
      break
    case 'service-not-available':
      // 폴백 STT로 전환
      break
  }
}
```

**3. 이전 인스턴스 정리:**
```javascript
const start = (questionIndex: number) => {
  // 기존 인스턴스가 있으면 먼저 정리
  if (recognitionRef.current) {
    shouldRestartRef.current = false
    recognitionRef.current.abort()
    recognitionRef.current = null
  }
  // 새 인스턴스 생성
  const recognition = createRecognition()
  recognitionRef.current = recognition
  recognition.start()
}
```

#### 대안 STT 비교 (2025-2026 최신)

| 솔루션 | 한국어 WER | 실시간 지원 | 비용 | 레이턴시 |
|---|---|---|---|---|
| **Web Speech API** (현재) | 높음 (정확도 낮음) | O (불안정) | 무료 | 즉시 |
| **Deepgram Nova-3** | 6.84% median | O (200-300ms) | $4.30/1000분 | <300ms |
| **Google Cloud Chirp 3** | 중간 | O | $16.00/1000분 | 200-400ms |
| **OpenAI Whisper** | 10.6% | X (배치 전용) | $6.00/1000분 | N/A |
| **ElevenLabs Scribe** | 3.1% (FLEURS) | O | 유료 | 200-400ms |

**권장**: MVP는 Web Speech API 유지, 프로덕션 시 Deepgram Nova-3 전환 (한국어 정확도 + 실시간 + 합리적 비용)

**출처**: [Deepgram Nova-3 소개](https://deepgram.com/learn/introducing-nova-3-speech-to-text-api), [Best STT APIs 2026](https://deepgram.com/learn/best-speech-to-text-apis-2026), [Deepgram vs Whisper](https://www.opentypeless.com/en/blog/deepgram-vs-whisper)

---

### 2.2 VAD (Voice Activity Detection)

#### 현재 구현 (`use-vad.ts`)

- **에너지 기반**: `audioLevel > speechThreshold(0.08)` 단순 비교 (line 72)
- **speechStartDelay**: 500ms 연속 감지 시 speech 판정 (line 81)
- **silenceEndDelay**: 3000ms 침묵 시 speech 종료 (line 92)
- **rAF 루프**: ~16ms 간격 체크 (line 99)
- **audioLevel 외부 주입**: `use-audio-analyzer` → React state → useEffect → ref

#### 기술 비교 (2026 최신 벤치마크)

| 방식 | 정확도 (TPR@5%FPR) | 처리속도 (RTF) | 리소스 | 소음 내성 |
|---|---|---|---|---|
| **에너지 기반 (현재)** | ~50% | 0.001 | 매우 낮음 | 매우 낮음 |
| **WebRTC VAD** | ~50% | 0.001 | 매우 낮음 | 낮음 |
| **Silero VAD v5** | 87.7% | 0.004 | 50-100MB | 높음 |
| **Cobra VAD (Picovoice)** | 98.9% | 0.005 | 30-60MB | 매우 높음 |

#### 권장: @ricky0123/vad-web (Silero VAD)

```javascript
import { useMicVAD } from "@ricky0123/vad-react"

const { isListening, isSpeaking } = useMicVAD({
  startOnLoad: true,
  onSpeechEnd: (audio) => {
    // audio: Float32Array — 바로 STT로 전달 가능
    console.log("Speech detected")
  },
  onSpeechStart: () => {
    console.log("User started speaking")
  },
  onVADMisfire: () => {
    // 짧은 소음 등 오감지 — 무시
  }
})
```

**장점**: MIT 라이선스, React 훅 제공, ONNX Runtime Web 기반, v5에서 3배 성능 향상
**단점**: 번들 크기 ~5MB 증가 (ONNX WASM 포함), 초기 로딩 시간

**출처**: [GitHub - ricky0123/vad](https://github.com/ricky0123/vad), [Best VAD 2026](https://picovoice.ai/blog/best-voice-activity-detection-vad/), [Silero VAD v5](https://github.com/snakers4/silero-vad/discussions/471)

---

### 2.3 AudioContext 관리

#### 현재 구현 (`use-audio-analyzer.ts`)

- `start()` 시 `new AudioContext()` 생성 (line 26)
- 중복 방지 guard: `if (contextRef.current) return` (line 25)
- `context.resume()` 호출 (line 27-29)
- `stop()`에서 `close()` + ref null 처리

#### 발견된 문제: rAF 내 `setAudioLevel` (line 57)

매 프레임(~60fps)마다 React state를 업데이트 → 불필요한 리렌더링 유발.
`audioLevel`이 변경될 때마다 `interview-page.tsx`의 `audio` 객체가 새 참조 → 하위 useEffect 재실행.

**이미 수정한 부분**: useEffect dependency를 `audio` → `audio.start` 등 개별 함수로 변경하여 무한 루프 방지.

#### 베스트 프랙티스

**1. 단일 AudioContext 싱글턴 패턴:**
```javascript
// 앱 전역 싱글턴 — 문서당 1개만 생성
let globalAudioContext: AudioContext | null = null

export const getAudioContext = (): AudioContext => {
  if (!globalAudioContext || globalAudioContext.state === 'closed') {
    globalAudioContext = new AudioContext()
  }
  return globalAudioContext
}
```

**2. audioLevel 전달 최적화 — ref 기반:**
```javascript
// 안티패턴: 매 프레임 setState
setAudioLevel(normalized) // ← 60fps React 리렌더

// 베스트: ref로 저장, 낮은 빈도로 state 업데이트
audioLevelRef.current = normalized
if (frameCount % 6 === 0) { // 10fps로 UI 업데이트
  setAudioLevel(normalized)
}
```

**3. resume() 호출 타이밍:**
- `resume()`은 사용자 제스처(click, touchend) 직후에 호출되어야 함
- 면접 페이지 진입이 사용자 클릭이므로 현재 흐름에서는 대부분 OK
- iOS Safari에서는 탭 전환 시 "interrupted" 상태 → 추가 resume 필요

**출처**: [MDN - Web Audio API Best practices](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Best_practices), [Chrome Autoplay Policy](https://developer.chrome.com/blog/autoplay), [AudioContext memory leak](https://github.com/WebAudio/web-audio-api/issues/2484)

---

### 2.4 TTS (Text-to-Speech)

#### 현재 구현 (`use-tts.ts`)

- Web Speech Synthesis 사용 (line 74)
- 한국어 음성 자동 선택 (line 33)
- `speakWhenReady`: 음성 로드 전 `voiceschanged` 대기 (line 85-108)
- `speak()` 시작 시 이전 발화 `cancel()` (line 52)

#### Chrome 15초 버그 — 미대응 (Critical)

**재현 조건:**
- Chrome Desktop (Windows, Ubuntu, macOS)
- Google 음성 사용 시
- 발화가 ~15초 이상 지속될 때 자동 중단
- `onend`가 호출되지 않고 `onerror('interrupted')` 발생 가능

**영향**: 긴 면접 질문이 중간에 끊김. `onEnd`가 미호출되면 STT 재시작 불가 → 데드락.

**워크어라운드 (Desktop에서만 작동):**
```javascript
const speak = (text: string) => {
  const utterance = new SpeechSynthesisUtterance(text)
  let resumeInterval: ReturnType<typeof setInterval>

  utterance.onstart = () => {
    // 14초마다 pause/resume으로 Chrome 타이머 리셋
    resumeInterval = setInterval(() => {
      if (speechSynthesis.speaking) {
        speechSynthesis.pause()
        speechSynthesis.resume()
      }
    }, 14000)
  }

  utterance.onend = () => clearInterval(resumeInterval)
  utterance.onerror = () => clearInterval(resumeInterval)

  speechSynthesis.speak(utterance)
}
```

**주의**: Android Chrome에서는 `pause()`가 완전 종료로 작동 → `resume()` 불가. 플랫폼 분기 필요.

#### 대안 TTS 비교

| 솔루션 | 한국어 품질 | 레이턴시 (TTFA) | 비용 | 비고 |
|---|---|---|---|---|
| **Web Speech Synthesis** (현재) | 낮음 | 즉시 | 무료 | 15초 버그, 기계적 |
| **ElevenLabs Flash v2.5** | 매우 높음 | 75ms | $5/월~ | 가장 자연스러운 음성 |
| **Google Cloud TTS Chirp 3** | 높음 | 200ms | $4/100만자 | 감정 표현 30종 |
| **Naver Clova Voice** | 매우 높음 (한국어 특화) | 200-400ms | 유료 | 100+ 음성, 감정 조절 |

**출처**: [Chromium Bug #679437](https://bugs.chromium.org/p/chromium/issues/detail?id=679437), [Cross browser speech synthesis](https://dev.to/jankapunkt/cross-browser-speech-synthesis-the-hard-way-and-the-easy-way-353), [ElevenLabs latency](https://elevenlabs.io/docs/developers/best-practices/latency-optimization)

---

### 2.5 TTS-VAD 에코 방지

#### 현재 구현 (`use-interview-session.ts`)

- VAD 조건: `vadEnabled && !tts.isSpeaking` (line 154)
- TTS `onStart`에서 STT stop, `onEnd`에서 STT 재시작 (line 93-104)
- `echoCancellation: true` 설정 (`use-media-stream.ts:23`)

#### 잠재적 문제

1. **TTS 종료 직후 잔향**: `isSpeaking`이 false가 된 직후 스피커 잔향을 VAD가 감지 → 오인
2. **브라우저 AEC 한계**: `echoCancellation: true`는 100% 신뢰 불가

#### 베스트 프랙티스: TTS 종료 후 딜레이 + 최소 발화 길이 검증

```javascript
// TTS 종료 후 300-500ms 딜레이
onEnd: () => {
  setTimeout(() => {
    // VAD 재활성화
    setAgentSpeaking(false)
  }, 300)
}

// 최소 speech 길이 검증 (400ms 미만은 에코로 판정)
onSpeechEnd: (audio) => {
  const duration = audio.length / 16000 * 1000
  if (duration < 400) return // 에코 무시
}
```

**출처**: [Chrome Echo Cancellation](https://developer.chrome.com/blog/more-native-echo-cancellation), [Google Brain - Textual Echo Cancellation](https://google.github.io/speaker-id/publications/TEC/)

---

### 2.6 전체 오케스트레이션

#### 현재 설계: Phase + useEffect 분산

- Zustand의 `phase` 상태로 전체 흐름 제어
- phase 전환이 VAD 콜백, useEffect, 타이머 등 **10+ 개소에 분산**
- `useInterviewStore.setState({ phase: 'ready' })` 직접 호출 (line 184)

#### 문제점

1. **상태 전이의 암묵성**: 어디서 어떤 조건으로 phase가 바뀌는지 추적 어려움
2. **불가능한 전환 미방지**: `recording` → `greeting` 같은 무효 전환을 타입 시스템이 방지하지 않음
3. **stale closure 위험**: 일부는 `getState()`로 최신 상태 읽고, 일부는 클로저 캡처된 값 사용

#### 베스트 프랙티스: 상태 머신 (XState)

```typescript
import { createMachine } from 'xstate'

export const interviewMachine = createMachine({
  id: 'interview',
  initial: 'preparing',
  states: {
    preparing: {
      on: { DATA_LOADED: 'greeting' }
    },
    greeting: {
      entry: 'playGreetingTTS',
      on: {
        SPEECH_DETECTED: { target: 'recording', actions: 'startRecording' },
      }
    },
    recording: {
      on: {
        SILENCE_DETECTED: { target: 'paused', actions: 'pauseRecording' },
      }
    },
    paused: {
      on: {
        SPEECH_DETECTED: { target: 'recording', actions: 'resumeRecording' },
        AUTO_TRANSITION: [
          { target: 'completed', guard: 'isLastQuestion' },
          { target: 'ready', actions: 'nextQuestion' },
        ]
      }
    },
    ready: {
      entry: 'playQuestionTTS',
      on: {
        SPEECH_DETECTED: { target: 'recording', actions: 'startRecording' },
      }
    },
    completed: { type: 'final' }
  }
})
```

**장점**: 모든 전환이 명시적, 불가능한 전환은 타입 에러, 디버깅/시각화 용이

**출처**: [XState Documentation](https://stately.ai/docs/xstate), [State Management Dialog with XState](https://mayashavin.com/articles/state-management-dialog)

---

### 2.7 턴 테이킹 (Turn-Taking)

#### 현재 구현

- VAD `silenceEndDelay`(3초) 만으로 답변 완료 판정
- "잠시만" 등 키워드로 생각 시간 감지 → `silenceEndDelay`를 25초로 확장

#### VAD vs Turn-Taking의 차이

- **VAD**: "사용자가 지금 말하고 있는가?" (현재 상태)
- **Turn-Taking**: "사용자가 말을 마쳤는가?" (종료 의도)

사용자가 "음... 잠깐만요"라고 하면 VAD는 계속 speech를 감지하지만, turn-taking은 "아직 생각 중"임을 이해해야 함.

#### 개선 방향

```javascript
class TurnTakingDetector {
  minSilenceDuration = 600  // 최소 600ms 침묵
  maxTurnDuration = 60000   // 최대 60초

  processProsodyFrame(energy, turnDuration) {
    if (energy < threshold) {
      if (silenceDuration > this.minSilenceDuration && turnDuration > 500) {
        return 'END_OF_TURN'
      }
    }
    if (turnDuration > this.maxTurnDuration) return 'TIMEOUT'
    return 'CONTINUE'
  }
}
```

**고급 기법**: Voice Activity Projection (VAP) — Transformer 기반으로 향후 음성 활동을 예측

**출처**: [Real-Time Turn-taking Prediction](https://arxiv.org/pdf/2401.04868), [LiveKit - Transformer Turn Detection](https://blog.livekit.io/using-a-transformer-to-improve-end-of-turn-detection/)

---

## 3. 발견된 문제점 (심각도별)

### Critical (서비스 품질에 직접적 영향)

| # | 문제 | 위치 | 영향 | 수정 난이도 |
|---|---|---|---|---|
| C1 | **TTS 15초 자동 중단 미대응** | `use-tts.ts:54-74` | 긴 질문 발화 중단 + onEnd 미호출 시 STT 데드락 | 낮음 |
| C2 | **STT 재시작 갭 발화 유실** | `use-speech-recognition.ts:92-98` | 60초마다 수백ms 답변 누락 | 낮음 |
| C3 | **에너지 기반 VAD 낮은 정확도** | `use-vad.ts:72` | 배경 소음 오감지 / 작은 목소리 미감지 | 중간 |

### High (안정성/UX에 영향)

| # | 문제 | 위치 | 영향 |
|---|---|---|---|
| H1 | **VAD 에코 오인** | `use-interview-session.ts:154` | TTS 직후 잔향을 음성으로 감지 |
| H2 | **고정 threshold 0.08** | `use-vad.ts:72` | 환경별 최적값 다름, 적응형 없음 |
| H3 | **Recognition 인스턴스 누수** | `use-speech-recognition.ts:115-118` | 이전 인스턴스 미정리 시 다중 병렬 실행 |
| H4 | **에러 타입 미분류** | `use-speech-recognition.ts:87-89` | 네트워크 에러 시 사용자 안내 없이 중단 |
| H5 | **rAF setAudioLevel 렌더링 부하** | `use-audio-analyzer.ts:57` | ~60fps React state 업데이트 |

### Medium (유지보수/확장성)

| # | 문제 | 위치 | 영향 |
|---|---|---|---|
| M1 | **phase 전환 로직 분산** | `use-interview-session.ts` 전체 | 10+ 개소에 흩어진 전환, 디버깅 곤란 |
| M2 | **직접 setState 사용** | `use-interview-session.ts:184` | setPhase 액션 우회 |
| M3 | **cleanup 이중 실행** | `use-audio-analyzer.ts:88-115` | 기능적 문제 없으나 불필요 |

### 이미 수정된 문제

| # | 문제 | 수정 내용 |
|---|---|---|
| ~~FIXED~~ | **AudioContext 무한 생성** | useEffect deps `audio` → `audio.start` 변경 + guard 추가 |
| ~~FIXED~~ | **AudioContext suspended** | `context.resume()` 추가 |
| ~~FIXED~~ | **greeting TTS 순서** | `mediaStream.isActive` 조건 추가 |

---

## 4. 개선 로드맵 (우선순위별)

### Phase 1: 긴급 수정 (1-2일)

#### 1-1. TTS 15초 버그 워크어라운드 (C1)
- **파일**: `use-tts.ts`
- **방법**: `speak()` 내 14초 간격 `pause()/resume()` 사이클
- **노력**: 낮음

#### 1-2. STT 에러 핸들링 강화 (H3, H4)
- **파일**: `use-speech-recognition.ts`
- **방법**: 이전 인스턴스 `abort()` + 에러 타입별 처리
- **노력**: 낮음

### Phase 2: 안정성 개선 (3-5일)

#### 2-1. VAD 에코 방지 딜레이 (H1)
- **방법**: TTS 종료 후 300ms 딜레이 + 최소 발화 길이 검증 (400ms)

#### 2-2. audioLevel 전달 최적화 (H5)
- **방법**: ref 기반 전달, 10fps로 state 업데이트 빈도 제한

#### 2-3. 적응형 VAD threshold (H2)
- **방법**: 초기 2초간 배경 소음 측정 → EMA 기반 동적 threshold

### Phase 3: 구조 개선 (1-2주)

#### 3-1. XState 상태 머신 도입 (M1, M2)
- **방법**: phase 전환을 선언적 상태 머신으로 통합

#### 3-2. MediaRecorder 에러 핸들링 (M3)
- **방법**: `stop()` Promise에 타임아웃 + reject 추가

### Phase 4: 품질 도약 (장기)

#### 4-1. ML 기반 VAD 전환 (C3)
- **라이브러리**: `@ricky0123/vad-web` (Silero VAD, ONNX Runtime)
- **효과**: VAD 정확도 50% → 87%+
- **트레이드오프**: 번들 크기 ~5MB 증가

#### 4-2. 서버 사이드 STT 전환
- **후보**: Deepgram Nova-3 (WebSocket 실시간, WER 6.84%, $4.30/1000분)
- **구현**: 브라우저 → WebSocket → Deepgram, 100ms 청크 전송
- **효과**: 한국어 인식 정확도 대폭 향상, 60초 제한 해소

#### 4-3. 서버 사이드 TTS 전환
- **후보**: ElevenLabs (75ms TTFA) 또는 Naver Clova Voice (한국어 특화)
- **효과**: 자연스러운 음성, 15초 버그 해소

---

## 5. 지연 시간 분석

### 현재 (턴 기반)

```
총 지연 = VAD 감지(500ms speechStartDelay)
        + STT 처리(~200ms)
        + 침묵 감지(3000ms silenceEndDelay)
        + 자동 전환(2500ms)
        + LLM 후속질문(~1000ms)
        + TTS 발화(즉시)
        ≈ 7200ms (답변 완료 → 다음 질문)
```

### 스트리밍 최적화 시 (목표)

```
총 지연 = Silero VAD 감지(~100ms)
        + STT 스트리밍 첫 결과(100ms)
        + 침묵 감지(600ms)
        + LLM 첫 토큰(150ms)
        + TTS 스트리밍 첫 청크(200ms)
        ≈ 1150ms
```

---

## 6. 참고 자료

### Web Speech API
- [Chrome Web Speech API 소개](https://developer.chrome.com/blog/voice-driven-web-apps-introduction-to-the-web-speech-api)
- [Web Speech API 60초 제한](https://groups.google.com/a/chromium.org/g/chromium-html5/c/s2XhT-Y5qAc)
- [SpeechRecognitionErrorEvent - MDN](https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognitionErrorEvent/error)
- [Speech recognition in JavaScript - AssemblyAI](https://www.assemblyai.com/blog/speech-recognition-javascript-web-speech-api)

### VAD
- [Best VAD 2026: Cobra vs Silero vs WebRTC](https://picovoice.ai/blog/best-voice-activity-detection-vad/)
- [VAD Complete Guide 2026](https://picovoice.ai/blog/complete-guide-voice-activity-detection-vad/)
- [@ricky0123/vad-web](https://github.com/ricky0123/vad)
- [Silero VAD GitHub](https://github.com/snakers4/silero-vad)

### AudioContext
- [Web Audio API Best practices - MDN](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Best_practices)
- [Chrome Autoplay Policy](https://developer.chrome.com/blog/autoplay)
- [AudioContext memory leak](https://github.com/WebAudio/web-audio-api/issues/2484)

### TTS
- [Chrome Speech Synthesis 15초 버그](https://bugs.chromium.org/p/chromium/issues/detail?id=679437)
- [Cross browser speech synthesis](https://dev.to/jankapunkt/cross-browser-speech-synthesis-the-hard-way-and-the-easy-way-353)

### STT 대안
- [Deepgram Nova-3](https://deepgram.com/learn/introducing-nova-3-speech-to-text-api)
- [Best STT APIs 2026](https://deepgram.com/learn/best-speech-to-text-apis-2026)
- [Deepgram WebSocket Live Streaming](https://developers.deepgram.com/docs/lower-level-websockets)
- [한국어 음성 인식 벤치마크](https://github.com/rtzr/Awesome-Korean-Speech-Recognition)

### 음성 AI 아키텍처
- [실시간 vs 턴 기반 음성 에이전트](https://softcery.com/lab/ai-voice-agents-real-time-vs-turn-based-tts-stt-architecture)
- [Voice AI Stack 2026](https://www.assemblyai.com/blog/the-voice-ai-stack-for-building-agents)
- [Pipecat 프레임워크](https://docs.pipecat.ai/getting-started/introduction)
- [LiveKit Turn Detection](https://blog.livekit.io/using-a-transformer-to-improve-end-of-turn-detection/)

### Turn-Taking
- [Voice Activity Projection](https://arxiv.org/pdf/2401.04868)
- [XState Documentation](https://stately.ai/docs/xstate)

### Echo Cancellation
- [Chrome Echo Cancellation](https://developer.chrome.com/blog/more-native-echo-cancellation)
- [Google Brain - Textual Echo Cancellation](https://google.github.io/speaker-id/publications/TEC/)
