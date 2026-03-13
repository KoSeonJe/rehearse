# Frontend Coding Guide

> **How** to write code. For naming/directory/tooling rules, see `CONVENTIONS.md`.

---

## 1. Component Design

### Single Responsibility

If a component name contains "And", split it.

| Type | Max Lines | Action |
|------|-----------|--------|
| Stateless | 40 | Extract child components |
| Stateful | 250 | Extract logic to custom hooks |

### Custom Hook Pattern (preferred over Container/Presentational)

```tsx
// ❌ Logic concentrated in container
const InterviewContainer = () => {
  const [phase, setPhase] = useState('preparing');
  // ... 200 lines of logic
  return <InterviewView phase={phase} />;
};

// ✅ Hook extracts logic → component only composes
const InterviewPage = () => {
  const { phase, questions, handlers } = useInterviewSession({ ... });
  return (
    <QuestionCard question={questions[current]} />
    <InterviewControls phase={phase} onNext={handlers.next} />
  );
};
```

### Compound Component Pattern

For complex UI (tabs, dropdowns), use Context-based composition.

```tsx
<Tabs defaultValue="feedback">
  <Tabs.List>
    <Tabs.Trigger value="feedback">Feedback</Tabs.Trigger>
  </Tabs.List>
  <Tabs.Content value="feedback"><FeedbackPanel /></Tabs.Content>
</Tabs>
```

---

## 2. Clean Code

### Conditional Rendering Priority

**Early return → Ternary → `&&`** (in that order). Nested ternaries are banned.

```tsx
// ✅ Early return (guard clause)
if (phase === 'completed') return <CompletedView />;
if (phase === 'preparing') return <PreparingView />;
return <RecordingControls />;

// ✅ Ternary (binary choice)
return isRecording ? <StopButton /> : <StartButton />;

// ❌ Nested ternary
return isLoading ? <Spinner /> : hasError ? <Error /> : <Data />;
```

### No Magic Numbers/Strings

```tsx
// ❌
if (audioLevel > 0.7) { ... }

// ✅
const HIGH_VOLUME_THRESHOLD = 0.7;
if (audioLevel > HIGH_VOLUME_THRESHOLD) { ... }
```

### Props Drilling

| Depth | Strategy |
|-------|----------|
| ≤ 2 levels | Pass directly |
| ≥ 3 levels | Composition pattern or Context |

### Naming

- **Booleans**: `is`/`has`/`should` prefix (`isLoading`, `hasError`)
- **Handlers**: `handle` prefix internally, `on` prefix for props
- **DRY rule of three**: Abstract only after 3+ repetitions with same change reason

---

## 3. Custom Hook Design

### One Hook = One Concern

```tsx
// ❌ Single hook doing everything (500 lines)
const useInterview = () => { /* audio + STT + recording + state */ };

// ✅ Separated by concern, orchestration hook composes
const useAudioAnalyzer = (stream) => { ... };
const useSpeechRecognition = () => { ... };
const useMediaRecorder = (stream) => { ... };
const useInterviewSession = (params) => {
  const audio = useAudioAnalyzer(stream);
  const stt = useSpeechRecognition();
  const recorder = useMediaRecorder(stream);
};
```

### Side Effect Management

- **Cleanup is mandatory** — prevent resource leaks
- **AbortController** for race conditions in async effects
- **Ref pattern** for callback stabilization (avoid deps array issues)

```tsx
// ✅ Cleanup
useEffect(() => {
  const recognition = new SpeechRecognition();
  recognition.start();
  return () => { recognition.stop(); recognition.onresult = null; };
}, []);

// ✅ Ref pattern for stable callbacks
const callbackRef = useRef(onVoiceEvent);
callbackRef.current = onVoiceEvent;
useEffect(() => {
  const handler = () => callbackRef.current(event);
  window.addEventListener('voicechange', handler);
  return () => window.removeEventListener('voicechange', handler);
}, []);
```

---

## 4. State Management

### Derived State = Direct Computation (no useState+useEffect sync)

```tsx
// ❌
const [filteredItems, setFilteredItems] = useState([]);
useEffect(() => { setFilteredItems(items.filter(i => i.active)); }, [items]);

// ✅ Compute directly (useMemo for expensive ops)
const filteredItems = items.filter(i => i.active);
const sorted = useMemo(() => [...items].sort(compareFn), [items]);
```

### Scope Rules

| Scope | Tool | Example |
|-------|------|---------|
| Server data | TanStack Query | Interview list, feedback |
| Global client | Zustand | Interview phase, video player |
| Local UI | useState/useReducer | Form input, modal toggle |

### Zustand Rules

```tsx
// ❌ Subscribe to entire store
const store = useInterviewStore();

// ✅ Individual selectors — minimize re-renders
const phase = useInterviewStore((s) => s.phase);

// ✅ Use getState() in event handlers to avoid stale closures
const handleKeyDown = useCallback((e: KeyboardEvent) => {
  const { phase } = useInterviewStore.getState();
}, []);
```

### Banned: Copying Props to State

```tsx
// ❌ Props → state copy (sync bugs)
const [localValue, setLocalValue] = useState(value);

// ✅ Use props directly, or reset with key
<Component key={id} initialValue={value} />
```

---

## 5. TypeScript

| Use Case | Choice |
|----------|--------|
| Props, object shapes | `interface` |
| Union, intersection, literals | `type` |

### Discriminated Union for Async/State Machines

```tsx
type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };
```

### Banned

- `any` type
- `as` type assertion (use type guards instead)
- Non-validated external input (use zod)

---

## 6. Tailwind CSS

### Class Order

`Layout → Spacing → Border → Background → Typography → State`
(auto-sorted by `prettier-plugin-tailwindcss`)

### Long Class Management

Use Record mapping for variants, array join for conditionals.

```tsx
const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-accent text-white hover:bg-accent-hover',
  secondary: 'bg-surface text-text-primary border border-border',
};
```

### Banned

- Dynamic class generation (`bg-${color}-500`) — Tailwind can't detect
- Arbitrary values (`h-[427px]`) — use design tokens

---

## 7. Performance

### memo / useMemo / useCallback Criteria

**Profile first.** Only optimize measured bottlenecks.

| Tool | When |
|------|------|
| `React.memo` | Parent re-renders often, child props don't change |
| `useMemo` | Expensive computation (sort/filter), object/array for memo'd child |
| `useCallback` | Function prop for memo'd child, event listener registration |

### Re-render Minimization

- Zustand individual selectors (§4)
- Place state close to where it's used (don't hoist unnecessarily)
- Route-based code splitting with `lazy()` + `Suspense`

---

## 8. Error Handling

### Error Boundary Hierarchy

```
App (top-level)
├── InterviewPage (page-level → "retry" button)
│   ├── VideoPreview (widget-level → fallback, rest works)
│   └── FeedbackPanel (widget-level)
└── ReviewPage (page-level)
```

### 3-State Fallback Pattern

Every data display area handles: **Loading → Error → Empty → Data**

```tsx
if (isLoading) return <Skeleton />;
if (isError) return <ErrorFallback />;
if (!data?.length) return <EmptyState />;
return <DataView items={data} />;
```

---

## 9. Accessibility (a11y)

- **Semantic HTML first**: `<button>` not `<div onClick>`, `<ul><li>` not `<div><div>`
- **Icon buttons**: `aria-label` required
- **Focus**: `focus-visible:ring-2` (keyboard users only)
- **Color**: Never convey info by color alone (pair with icon/text)
- **Contrast**: WCAG AA — normal text 4.5:1, large text 3:1

---

## 10. useEffect Anti-patterns

| Anti-pattern | Fix |
|-------------|-----|
| Derived state sync (`useState` + `useEffect`) | Compute directly, `useMemo` |
| Props → state copy | Use props directly, reset with `key` |
| Missing cleanup | Always return cleanup function |
| Missing deps | Include all referenced values |
| API fetch in useEffect | Delegate to TanStack Query |
| Race conditions | AbortController, ref flag |

---

## 11. Code Review Checklist

### Quality
- [ ] Component SRP (no "And" in name)
- [ ] No magic numbers/strings
- [ ] Booleans use `is`/`has`/`should` prefix
- [ ] useEffect has cleanup
- [ ] No derived state via useState+useEffect

### Performance
- [ ] Zustand individual selectors
- [ ] memo/useMemo/useCallback only for measured bottlenecks
- [ ] Route-level code splitting

### Security
- [ ] No API keys in frontend
- [ ] User input sanitized
- [ ] DOMPurify for `dangerouslySetInnerHTML`

### Accessibility
- [ ] Semantic HTML (`button`, not `div onClick`)
- [ ] `aria-label` on icon buttons
- [ ] `focus-visible` styles

### TypeScript
- [ ] No `any`, no `as` assertion
- [ ] Props interface defined
- [ ] Union types for state (not strings)

### Maintenance
- [ ] Custom hooks = single concern
- [ ] No 3+ level props drilling
- [ ] No `console.log`
- [ ] Tailwind: static mapping only (no dynamic classes)
