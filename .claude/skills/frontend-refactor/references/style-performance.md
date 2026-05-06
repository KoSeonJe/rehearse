# Style & Performance Reference

## §1 Tailwind CSS

### 동적 클래스 → 정적 매핑

Tailwind은 빌드 시 소스 코드를 정적 스캔하여 사용된 클래스만 포함한다. 템플릿 리터럴로 조합된 클래스는 스캔되지 않아 CSS에 포함되지 않는다.

```tsx
// BAD: 빌드 시 스캔 불가
<div className={`bg-${color}-500`} />

// GOOD: Record로 정적 매핑
const colorMap: Record<Color, string> = {
  red: 'bg-red-500',
  blue: 'bg-blue-500',
  green: 'bg-green-500',
};
<div className={colorMap[color]} />
```

### Variant 스타일 패턴

이 프로젝트의 Button 컴포넌트가 사용하는 패턴. 긴 클래스를 배열로 나누면 가독성이 높아진다.

```tsx
const variantStyles: Record<ButtonVariant, string> = {
  primary: [
    'bg-accent text-white shadow-lg shadow-accent/20',
    'hover:bg-accent-hover active:scale-[0.98]',
    'disabled:bg-border disabled:text-text-tertiary',
  ].join(' '),
  secondary: [
    'bg-white text-text-primary border border-border',
    'hover:bg-background active:scale-[0.98]',
  ].join(' '),
};
```

### 조건부 클래스 조합

```tsx
// BAD: 문자열 연결
const className = `px-4 py-2 ${isActive ? 'bg-blue-500' : 'bg-gray-300'}`;

// GOOD: 배열 + filter + join (이 프로젝트 패턴)
<button className={[
  'inline-flex items-center justify-center transition-all',
  variantStyles[variant],
  fullWidth ? 'w-full' : '',
].filter(Boolean).join(' ')} />

// 또는 cn() 유틸리티 (clsx + tailwind-merge)
import { cn } from '@/lib/cn';
<button className={cn(
  'px-4 py-2 rounded',
  isActive && 'bg-accent text-white',
  !isActive && 'bg-surface text-text-secondary',
)} />
```

### 금지 패턴

- **`@apply` 남용**: Tailwind의 유틸리티 퍼스트 장점을 상실. 컴포넌트 추출로 대체
- **임의 값** (`h-[427px]`): 디자인 토큰으로 대체
- **인라인 style**: Tailwind 클래스로 대체 가능하면 사용하지 않음

---

## §2 성능

### memo / useMemo / useCallback 판단 기준

메모이제이션은 공짜가 아니다. 비교 비용과 코드 복잡성이 증가한다. 프로파일링으로 실제 병목을 확인한 후에만 적용.

| 도구 | 의미 있는 경우 | 의미 없는 경우 |
|------|-------------|-------------|
| `React.memo` | 부모가 자주 리렌더, 자식 props 불변 | 자식이 항상 다른 props를 받음 |
| `useMemo` | 대량 데이터 sort/filter | 1+1 같은 저비용 계산 |
| `useCallback` | memo된 자식에 전달할 함수 | memo 안 된 자식에 전달 (효과 없음) |

```tsx
// BAD: 저비용 연산에 useMemo
const sum = useMemo(() => 1 + 1, []);

// BAD: memo 안 된 자식에 useCallback
const handleChange = useCallback((e) => setValue(e.target.value), []);
return <input onChange={handleChange} />; // input은 memo 안 됨

// GOOD: 실제 비용 높은 연산
const sorted = useMemo(() => [...items].sort(compareFn), [items]);
```

### 파생 상태 — useState + useEffect 동기화 금지

useState + useEffect로 파생 값을 동기화하면 불필요한 리렌더가 2번 발생한다 (원본 변경 → effect 실행 → 파생 상태 변경). 직접 계산하면 1번.

```tsx
// BAD: 리렌더 2회
const [filteredItems, setFilteredItems] = useState<Item[]>([]);
useEffect(() => { setFilteredItems(items.filter(i => i.active)); }, [items]);

// GOOD: 리렌더 1회, 직접 계산
const filteredItems = items.filter(i => i.active);

// GOOD: 비용 높으면 useMemo
const sorted = useMemo(() => [...items].sort(compareFn), [items]);
```

### Props → State 복사 금지

props를 state로 복사하면 props가 변경되어도 state는 그대로다. 동기화를 위해 useEffect를 추가하면 위의 파생 상태 문제와 동일.

```tsx
// BAD
const [localValue, setLocalValue] = useState(props.value);

// GOOD: props 직접 사용
return <input value={props.value} onChange={props.onChange} />;

// GOOD: 초기값이 필요하면 key로 리셋
<EditForm key={item.id} initialValue={item.name} />
```

### 렌더 내 컴포넌트 정의 금지

렌더 함수 안에서 컴포넌트를 정의하면 매 렌더마다 새로운 컴포넌트 타입이 생성된다. React는 타입이 다르면 언마운트 후 리마운트하므로 내부 상태가 초기화된다.

```tsx
// BAD: 매 렌더마다 ChildComponent가 새 타입
const Parent = () => {
  const Child = () => <div>Child</div>;
  return <Child />;
};

// GOOD: 모듈 레벨에 정의
const Child = () => <div>Child</div>;
const Parent = () => <Child />;
```

### 코드 스플리팅

라우트 단위로 lazy loading하면 초기 번들 크기를 줄일 수 있다. 사용자가 방문하지 않는 페이지의 코드를 미리 로드할 필요 없다.

```tsx
import { lazy, Suspense } from 'react';

const InterviewPage = lazy(() => import('@/pages/interview-page'));
const DashboardPage = lazy(() => import('@/pages/dashboard-page'));

<Route
  path="/interview/:id"
  element={
    <Suspense fallback={<PageSkeleton />}>
      <InterviewPage />
    </Suspense>
  }
/>
```
