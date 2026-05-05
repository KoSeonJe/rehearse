# Frontend Conventions

기반: [Toss Frontend Fundamentals](https://frontend-fundamentals.com) 4원칙 — **가독성 / 예측가능성 / 응집도 / 결합도**.
React 18 + TS strict + Tailwind + Zustand + TanStack Query 특화.

---

## 1. 가독성 (Readability)

- **매직 상수화**: 리터럴은 의미 이름 부여. 임계값 = `constants/` 분리.
- **시점 분리**: 조건 분기 = early return → ternary → `&&`. 중첩 ternary 금지.
- **조건 이름**: 복합 조건은 boolean 변수 추출. `is/has/should/can` 접두사.
- **함수 1책임**: UI / 비즈 / 부수효과 혼재 금지. 커스텀 훅 추출.

```tsx
// ✅ early return + 매직 상수
const HIGH_VOLUME_THRESHOLD = 0.7;
if (phase === 'completed') return <CompletedView />;
if (audioLevel > HIGH_VOLUME_THRESHOLD) showWarning();
```

---

## 2. 예측가능성 (Predictability)

- **동일 형태 반환**: 같은 종류 훅 = 동일 시그니처. Query = `{ data, isLoading, isError }` / Mutation = `{ mutate, isPending }`.
- **숨은 로직 금지**: 함수명과 다른 부수효과 X. `getUser()` 가 로깅 / 분석 호출 X.
- **이름 충돌 방지**: 도메인 prefix (`useInterviewSession`, not `useSession`). util 동명 함수 X.

---

## 3. 응집도 (Cohesion)

**Co-location** — 함께 수정되는 파일 = 같은 디렉토리. 도메인 컴포넌트 = `components/{domain}/` 직속.

```
src/
├── pages/             *-page.tsx (라우트 단위)
├── components/
│   ├── ui/            shadcn primitive
│   ├── common/        도메인 무관 공용
│   ├── layout/        헤더/푸터/네비
│   └── {domain}/      interview, feedback, review, dashboard, home, setup, content
├── hooks/             use-*.ts (도메인 별)
├── stores/            *-store.ts (Zustand)
├── api/               서버 통신 (도메인 모듈)
├── lib/               외부 라이브러리 래퍼
├── utils/             순수 유틸
├── types/ constants/  공용 타입 / 상수
├── mocks/             MSW 핸들러
└── test/              테스트 셋업
```

**컴포넌트 크기** — Stateless ≥ 40 줄 → 자식 분리. Stateful ≥ 250 줄 → 커스텀 훅 추출.

---

## 4. 결합도 (Coupling)

- **단일 책임 훅**: 1 훅 = 1 관심사. 오케스트레이션 훅이 작은 훅 조합.
- **Rule of Three**: 3회 이상 중복 + 동일 변경 사유 시만 추상화. 그 전 중복 허용.
- **Props Drilling**: ≤ 2 levels 직접 전달 / ≥ 3 levels Composition or Context.
- **Container/Presentational 금지** → Custom Hook 패턴 우선 (로직 = 훅, 컴포넌트 = 조립).

```tsx
const audio = useAudioAnalyzer(stream);
const stt = useSpeechRecognition();
const session = useInterviewSession({ audio, stt });
```

---

## 네이밍

| 대상 | 룰 | 예 |
|------|----|----|
| 파일 | kebab-case | `interview-setup-page.tsx` |
| 컴포넌트 | PascalCase | `InterviewSetupPage` |
| 훅 | `use*` camelCase | `useInterviewSession` |
| 타입 / 인터페이스 | PascalCase | `InterviewResponse` |
| 상수 | UPPER_SNAKE_CASE | `MAX_QUESTIONS` |
| Boolean | `is/has/should/can` | `isLoading` |
| Handler | 내부 `handle*`, props `on*` | `handleClick` / `onClick` |

---

## TypeScript

- `any` **금지** → `unknown` + 타입 가드.
- `as` 단언 **금지** → 타입 가드.
- Props 형 = `interface`. Union / literal = `type`.
- 외부 입력 = zod 검증.
- 비동기 / 상태 머신 = Discriminated Union.

```tsx
type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };
```

---

## 상태 관리

| Scope | 도구 | 예 |
|-------|------|----|
| 서버 | TanStack Query | 인터뷰 / 피드백 |
| 글로벌 클라이언트 | Zustand | 인터뷰 phase |
| 로컬 UI | useState / useReducer | 폼 입력 |

- **파생 상태 = 직접 계산** (`useState + useEffect` 동기화 X). 비싸면 `useMemo`.
- **Zustand = 개별 selector**. 전체 구독 금지. 이벤트 핸들러는 `getState()`.
- **Props → state 복사 금지**. props 직접 사용, reset = `key`.

---

## API / 부수효과

- API fetch = TanStack Query 위임. `useEffect` 안 직접 호출 X.
- `useEffect` cleanup 필수. `AbortController` 로 race 방어.
- Ref 패턴으로 콜백 안정화 (deps array 누수 방지).

---

## Tailwind

- 클래스 순서: Layout → Spacing → Border → Background → Typography → State (`prettier-plugin-tailwindcss` 자동).
- variant = Record 매핑. 동적 생성 (`bg-${color}-500`) **금지**.
- arbitrary 값 (`h-[427px]`) **금지** → 디자인 토큰 사용.

---

## 성능

- **측정 후 최적화**. `React.memo` / `useMemo` / `useCallback` = 병목 확인 시만.
- 라우트 단위 코드 분할 (`lazy()` + `Suspense`).
- 상태는 사용처 가까이. 불필요 hoist 금지.

---

## 에러 처리

- Error Boundary 계층: App → Page → Widget.
- 데이터 표시 영역 4-state: **Loading → Error → Empty → Data**.

```tsx
if (isLoading) return <Skeleton />;
if (isError) return <ErrorFallback />;
if (!data?.length) return <EmptyState />;
return <DataView items={data} />;
```

---

## 접근성 (a11y)

- 시맨틱 HTML 우선 (`<button>` not `<div onClick>`).
- 아이콘 버튼 `aria-label` 필수.
- `focus-visible:ring-2` (키보드 한정 표시).
- 색만으로 정보 전달 X (아이콘 / 텍스트 동반).
- WCAG AA: normal 4.5:1, large 3:1.

---

## 금지 (즉시 반려)

- `any` / `as` 단언 / class 컴포넌트 / barrel export.
- `console.log` 커밋 / 매직 넘버 / 중첩 ternary.
- `useState + useEffect` 파생 상태 sync / props → state 복사 / `useEffect` API fetch / cleanup 누락 / deps 누락.
- 동적 Tailwind 클래스 / arbitrary 값.
- DOMPurify 없는 `dangerouslySetInnerHTML`.
- 거대 단일 훅 (멀티 관심사).
