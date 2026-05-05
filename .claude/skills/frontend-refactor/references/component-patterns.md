# Component Patterns Reference

## §1 컴포넌트 설계

### God Component 분리

```tsx
// BAD: 300+ LOC, 모든 로직이 한 컴포넌트에
const InterviewPage = () => {
  const [phase, setPhase] = useState('preparing');
  const [questions, setQuestions] = useState([]);
  const [isRecording, setIsRecording] = useState(false);
  const [audioLevel, setAudioLevel] = useState(0);
  const [transcript, setTranscript] = useState('');
  // ... 200줄의 로직 + JSX
};

// GOOD: 훅으로 로직 추출, 컴포넌트는 조합만
const InterviewPage = () => {
  const { phase, questions, handlers } = useInterviewSession({ ... });
  const { isRecording, audioLevel } = useAudioCapture(stream);

  return (
    <main>
      <QuestionCard question={questions[current]} />
      <InterviewControls phase={phase} onNext={handlers.next} />
      <AudioIndicator level={audioLevel} />
    </main>
  );
};
```

### Compound Component

5개 이상의 boolean prop 조합이 필요한 복합 UI에 적용:

```tsx
// BAD: prop 폭발
<Dialog isOpen onClose title="피드백" showFooter footerAlign="right" showCloseButton size="large" />

// GOOD: Context 기반 합성
<Dialog isOpen={isOpen} onClose={onClose}>
  <Dialog.Header>피드백</Dialog.Header>
  <Dialog.Body><FeedbackContent /></Dialog.Body>
  <Dialog.Footer><Button onClick={onClose}>닫기</Button></Dialog.Footer>
</Dialog>
```

### 조건부 렌더링

Early return → 삼항 → && 순서. 중첩 삼항은 가독성을 급격히 떨어뜨리므로 금지.

```tsx
// BAD
return isLoading ? <Spinner /> : hasError ? <Error /> : <Data />;

// GOOD: guard clause
if (isLoading) return <Spinner />;
if (hasError) return <ErrorFallback />;
if (!data?.length) return <EmptyState />;
return <DataView items={data} />;
```

---

## §2 TypeScript

### any 제거

```tsx
// BAD
function processData(data: any) { return data.result; }

// GOOD
interface AnalysisResult {
  result: { feedback: string; confidence: number };
}
function processData(data: AnalysisResult) { return data.result; }
```

- `any` → `unknown` + 타입 가드로 좁히기
- `as any` → 타입 가드 또는 제네릭으로 해결
- 외부 API 응답 → `interface` 정의 + zod validation

### Discriminated Union

boolean 플래그 조합은 모순 가능한 상태를 만든다 (isLoading=true AND data 존재). discriminated union은 컴파일러가 모순을 방지해준다.

```tsx
// BAD
interface State { isLoading: boolean; isError: boolean; data?: Data; error?: Error; }

// GOOD
type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };
```

### Type Guard (as 캐스팅 대체)

`as`는 런타임 검증 없이 타입을 강제해서 에러를 숨긴다. 타입 가드는 실제 검증을 수행한다.

```tsx
// BAD
const user = response as User;

// GOOD
function isUser(data: unknown): data is User {
  return typeof data === 'object' && data !== null && 'id' in data;
}
if (isUser(response)) { /* response: User */ }
```

---

## §3 클린코드

### 매직 넘버 제거

```tsx
// BAD
if (audioLevel > 0.7) { ... }
setTimeout(() => {}, 5000);

// GOOD
const HIGH_VOLUME_THRESHOLD = 0.7;
const ANALYSIS_TIMEOUT_MS = 5000;
if (audioLevel > HIGH_VOLUME_THRESHOLD) { ... }
```

### 함수 규칙

| 규칙 | 이유 |
|------|------|
| 20줄 이하 | 한 화면에서 전체 로직 파악 |
| 인자 3개 이하 | 인자가 많으면 객체로 묶기 |
| Guard clause | 해피 패스를 기본 들여쓰기에 유지 |
| 최대 2단계 중첩 | 화살표 코드(arrow code) 방지 |

---

## §4 커스텀 훅

### One Hook = One Concern

하나의 훅이 여러 관심사를 담으면 테스트가 어렵고, 한 관심사의 변경이 다른 관심사에 영향을 준다.

```tsx
// BAD: 500 LOC, 오디오+STT+녹화+상태 전부
const useInterview = () => { /* 모든 로직 */ };

// GOOD: 관심사별 분리
const useAudioCapture = (stream: MediaStream) => { ... };
const useSpeechRecognition = () => { ... };
const useMediaRecorder = (stream: MediaStream) => { ... };

// 오케스트레이션 훅이 조합만 담당
const useInterviewSession = (params: SessionParams) => {
  const audio = useAudioCapture(stream);
  const stt = useSpeechRecognition();
  const recorder = useMediaRecorder(stream);
};
```

### Cleanup (리소스 누수 방지)

```tsx
// BAD: cleanup 누락 → 언마운트 후에도 리스너 활성
useEffect(() => {
  const recognition = new SpeechRecognition();
  recognition.start();
}, []);

// GOOD
useEffect(() => {
  const recognition = new SpeechRecognition();
  recognition.start();
  return () => { recognition.stop(); recognition.onresult = null; };
}, []);
```

### Ref 패턴 (콜백 안정화)

deps 배열에 콜백을 넣으면 매번 effect가 재실행된다. Ref로 최신 값을 유지하면 effect는 한 번만 등록.

```tsx
const callbackRef = useRef(onVoiceEvent);
callbackRef.current = onVoiceEvent;

useEffect(() => {
  const handler = (event: Event) => callbackRef.current(event);
  window.addEventListener('voicechange', handler);
  return () => window.removeEventListener('voicechange', handler);
}, []);
```

### AbortController (비동기 경합 방지)

id가 빠르게 변경되면 이전 요청 결과가 늦게 도착해 잘못된 데이터를 표시할 수 있다.

```tsx
useEffect(() => {
  const controller = new AbortController();
  const fetchData = async () => {
    const res = await fetch(url, { signal: controller.signal });
    if (!controller.signal.aborted) setData(await res.json());
  };
  fetchData();
  return () => controller.abort();
}, [url]);
```
