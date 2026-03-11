# 음성 면접 시스템 지연(Latency) 최적화 계획

- **작성일**: 2026-03-11
- **상태**: Draft (검증 완료, 구현 대기)
- **선행 조건**: Phase 1-2 버그 수정 완료 후 진행
- **참고**: `2026-03-11-voice-interview-technical-analysis.md`

---

## Why

현재 사용자가 답변을 마친 후 다음 질문까지 ~7-8초가 걸린다. 실제 면접관은 1-2초 내에 반응한다. 이 지연은 면접 몰입감을 크게 저하시키며, 서비스 품질의 핵심 병목이다.

## 실제 지연 분석 (코드 기반)

```
사용자 답변 완료
  ↓ 3000ms  — silenceEndDelay (use-interview-session.ts:11, DEFAULT_SILENCE_DELAY)
  ↓ ~0ms    — processAnswer() 호출 (비동기, 타이머와 병렬)
  ↓ 2500ms  — autoTransitionTimer (use-interview-session.ts:215)
  ↓ ~0ms    — nextQuestion() 호출
  ↓ ~50ms   — TTS 발화 시작 (Web Speech Synthesis, 거의 즉시)
  ────────────
  합계: ~5550ms (고정 타이머만)
  + Claude API: ~1000-2000ms (별도 병렬, 후속질문 생성)
```

### 핵심 발견: 병목은 API가 아니라 고정 타이머

| 구간 | 시간 | 비율 | 수정 가능 |
|---|---|---|---|
| **침묵 감지 (silenceEndDelay)** | 3000ms | 43% | O (threshold 조절) |
| **자동 전환 대기 (autoTransition)** | 2500ms | 36% | O (즉시 단축 가능) |
| **Claude API 호출** | ~1500ms | 21% | 스트리밍으로 개선 가능 |
| **TTS 시작** | ~50ms | <1% | 이미 최적 |

**고정 타이머 5500ms가 전체 지연의 79%를 차지한다.**
API 최적화(스트리밍, 프리페칭)보다 타이머 단축이 압도적으로 효과적이다.

---

## 제안 전략 (우선순위순)

### Strategy 0: autoTransition 타이머 단축 [즉시 적용 가능]

- **현재**: 2500ms 대기 후 다음 질문으로 전환
- **개선**: 1000ms로 단축 (사용자가 "다음 질문으로 넘어갑니다" 메시지를 읽을 최소 시간)
- **효과**: -1500ms
- **위험**: 없음 (UI 메시지 표시 시간만 단축)
- **변경**: `use-interview-session.ts:215` — `setTimeout(..., 2500)` → `setTimeout(..., 1000)`
- **난이도**: 1줄 변경

### Strategy 1: 침묵 감지 최적화 [테스트 필요]

- **현재**: `DEFAULT_SILENCE_DELAY = 3000ms`
- **개선**: 2000ms로 단축 (단계적 테스트: 2500 → 2000 → 1500)
- **효과**: -1000ms (2000ms 적용 시)
- **위험**: 사용자가 생각하며 쉬는 구간에서 조기 차단
- **완화**:
  - 기존 `use-thinking-time-detector`가 "잠시만" 감지 시 25초로 확장 (이미 구현됨)
  - 단계적 테스트로 최적값 탐색
- **변경**: `use-interview-session.ts:11` — `DEFAULT_SILENCE_DELAY` 값 변경
- **난이도**: 1줄 변경 + 사용자 테스트

**주의**: 현재 `use-thinking-time-detector`가 silenceDelay를 25초로 확장하는 로직이 이미 있다. 기본값을 너무 낮추면 (1.5s) 생각 모드 전환 전에 차단되는 UX 갭이 발생할 수 있다. 2000ms가 안전한 시작점.

### Strategy 2: 필러 문구 (Filler Phrases) [설계 필요]

- **목적**: LLM 응답 대기 중 면접관처럼 즉각 반응하여 체감 지연 감소
- **효과**: 체감 지연 대폭 감소 (실제 지연 변화 없음)

#### Phase 상태 머신과의 상호작용 (Critical)

현재 TTS 콜백 구조:
```
TTS onStart → STT stop + isSpeaking=true → VAD 비활성화
TTS onEnd → STT 재시작 (phase=recording일 때만) + isSpeaking=false → VAD 활성화
```

**문제점**: 필러 TTS가 기존 `onStart`/`onEnd` 콜백을 트리거하면:
1. STT가 정지됨 (불필요)
2. VAD가 비활성화됨 → 사용자가 필러 도중 말하면 감지 못함
3. `onEnd`에서 phase가 `paused`이므로 STT 재시작 안 됨

**해결 방안**: 필러 전용 TTS 채널 분리
```typescript
// 필러용 speak — onStart/onEnd 콜백 우회
const speakFiller = (text: string) => {
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.voice = voiceRef.current
  utterance.lang = 'ko-KR'
  utterance.rate = 1.0
  // isSpeaking 상태를 변경하지 않음 → VAD/STT에 영향 없음
  speechSynthesis.speak(utterance)
}
```

#### 필러 문구 풀

```typescript
const FILLER_PHRASES = [
  '네, 알겠습니다.',
  '좋은 답변이네요.',
  '네, 잘 들었습니다.',
  '감사합니다.',
]
```

#### 적용 위치

`use-interview-session.ts`의 `onSpeechEnd` 콜백에서:
```
침묵 감지 → stopRecording() → processAnswer() → 필러 TTS 즉시 재생 → autoTransition 타이머
```

- **난이도**: 중간 (필러 TTS 분리 + 타이밍 설계)
- **선행 조건**: Phase 1의 TTS 15초 버그 워크어라운드 완료

### Strategy 3: 후속질문 도착 시 즉시 전환 [설계 필요]

현재 `processAnswer()`와 `autoTransitionTimer`가 병렬 실행되지만, autoTransition은 후속질문 도착 여부와 무관하게 2500ms 후 전환된다.

**개선**: 후속질문이 먼저 도착하면 autoTransition 타이머를 취소하고 즉시 전환

```
현재:  processAnswer() ──── 1500ms ──── 응답 도착 (무시됨)
       autoTransition ──── 2500ms ──── 전환 (응답 대기 안 함)

개선:  processAnswer() ──── 1500ms ──── 응답 도착 → 즉시 전환
       autoTransition ──── 2500ms ──── (이미 전환됨, 취소)
```

- **효과**: 후속질문 생성이 빠르면 ~1000ms 단축
- **위험**: 후속질문이 느리면 기존과 동일
- **난이도**: 중간 (mutation onSuccess에서 autoTransition 취소 + 즉시 전환)

### Strategy 4: Claude 스트리밍 응답 [장기, 높은 비용]

- **범위**: BE + FE 대규모 변경
  - BE: `RestClient` → `WebClient` (reactive) 또는 SSE 엔드포인트
  - FE: `useMutation` → 커스텀 `EventSource`/`ReadableStream` 훅
  - TTS: 문장 경계 감지 + 부분 텍스트 발화
- **효과**: ~800ms 단축
- **난이도**: 높음 (4+ 파일, BE/FE 동시 변경, 1-2주)
- **판단**: 효과 대비 비용이 높음. Strategy 0-3으로 충분한 개선 달성 후 필요 시 검토

### Strategy 5: 투기적 프리페칭 [보류, 비추천]

- **문제점**:
  1. STT interim 결과는 불안정 → 최종 텍스트와 차이 큼 → 프리페치 결과 무효화 빈번
  2. Claude API 호출 비용 낭비 (사용되지 않는 프리페치)
  3. 트리거 조건 정의 어려움 (언제 프리페치 시작?)
  4. `useMutation` 취소 메커니즘 없음 (AbortController 커스텀 필요)
  5. 구현 복잡도 대비 불확실한 효과
- **판단**: 비추천. CPU 투기적 실행과 달리 LLM API 호출은 비용이 있고 미스율이 높다.

---

## 예상 효과

| 전략 조합 | 예상 지연 | 절감 | 구현 비용 |
|---|---|---|---|
| **현재** | ~7500ms | — | — |
| **Strategy 0만** | ~6000ms | -1500ms | 1줄 |
| **Strategy 0 + 1** | ~5000ms | -2500ms | 2줄 + 테스트 |
| **Strategy 0 + 1 + 2** | ~5000ms (체감 ~1초) | 체감 대폭 개선 | 중간 |
| **Strategy 0 + 1 + 3** | ~3500ms | -4000ms | 중간 |
| **전체 (0+1+2+3)** | ~3500ms (체감 ~1초) | 최적 | 중간 |
| **+Strategy 4 추가** | ~2700ms | -4800ms | 높음 (1-2주) |

**목표**: Strategy 0+1+3으로 실제 지연 ~3.5초, 필러 문구로 체감 ~1초 달성

---

## 구현 순서 (권장)

```
1. Strategy 0 (autoTransition 단축)     — 즉시, 1줄 변경
2. Strategy 1 (침묵 감지 2000ms)         — 즉시, 1줄 + 사용자 테스트
3. Strategy 3 (후속질문 즉시 전환)       — 1-2일, 중간 난이도
4. Strategy 2 (필러 문구)               — 2-3일, 설계 필요
5. Strategy 4 (스트리밍)                — 장기, 필요 시
```

---

## 측정 방법

기존 `useInterviewEventRecorder`를 활용하여 지연 측정:

```typescript
// 이미 기록되는 이벤트들
recordEvent('silence_detected', questionIndex)   // 침묵 감지 시점
recordEvent('auto_transition', questionIndex)     // 자동 전환 시점
recordEvent('question_read_tts', questionIndex)   // 질문 TTS 시작 시점

// 추가 측정 필요
recordEvent('followup_received', questionIndex)   // 후속질문 API 응답 시점
recordEvent('filler_played', questionIndex)       // 필러 재생 시점
```

`silence_detected` → `question_read_tts` 간격 = 실제 체감 지연

---

## Open Questions

1. `autoTransition`을 1000ms보다 더 줄여도 되는가? (500ms면 메시지를 못 읽을 수 있음)
2. `processAnswer()`의 후속질문 mutation과 `autoTransition`의 기존 레이스 컨디션 — 후속질문이 `nextQuestion()` 후에 도착하면 이전 질문의 후속질문이 표시되지 않는 문제가 있는지?
3. 침묵 감지 최적값은 실제 사용자 테스트로 결정해야 함 — A/B 테스트 인프라가 필요한가?

---

## 참고 자료

- [실시간 vs 턴 기반 음성 에이전트](https://softcery.com/lab/ai-voice-agents-real-time-vs-turn-based-tts-stt-architecture)
- [Voice AI Stack 2026](https://www.assemblyai.com/blog/the-voice-ai-stack-for-building-agents)
- [Engineering for Real-Time Voice Agent Latency](https://cresta.com/blog/engineering-for-real-time-voice-agent-latency/)
- [The 300ms Rule - AssemblyAI](https://www.assemblyai.com/blog/low-latency-voice-ai)
