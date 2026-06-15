---
name: frontend-refactor
description: >
  React 18 + TypeScript + Tailwind CSS + Zustand + TanStack Query 프론트엔드 코드의 종합 리팩토링 스킬.
  컴포넌트 설계, 훅 분리, 상태관리, 성능, 클린코드 8개 영역에서 안티패턴을 탐지하고 단계별로 리팩토링한다.
  프론트엔드 리팩토링, 컴포넌트 분리, 훅 정리, 리렌더 최적화, FE 코드 품질 개선, React 클린코드 등
  프론트엔드 코드 품질과 관련된 모든 요청에 이 스킬을 사용한다.
  "코드 좀 정리해줘", "이 컴포넌트 너무 큰데", "훅이 복잡해" 같은 요청도 포함.
triggers:
  - 프론트엔드 리팩토링
  - FE 리팩토링
  - frontend refactor
  - 컴포넌트 리팩토링
  - component refactor
  - 훅 리팩토링
  - hook refactor
  - 스토어 리팩토링
  - store refactor
  - 리액트 클린코드
  - react clean code
  - 프론트 코드 개선
  - 프론트 코드 정리
  - FE 코드 품질
  - 컴포넌트 분리
  - 훅 분리
  - 리렌더 최적화
  - re-render
argument-hint: "<target-component-or-directory> [--analyze-only] [--scope=component|hook|store|style|query|full]"
---

# Frontend Refactoring Skill

React 18 + TypeScript 5.9 + Tailwind CSS 3.4 + Zustand 4.5 + TanStack Query 5 코드를 8개 영역에서 분석하고 리팩토링한다. 단순 스타일 변경이 아닌 **컴포넌트 책임 재배치**, **상태 관리 최적화**, **타입 안전성 강화**가 목표.

## Workflow

### Phase 1: 분석 (`--analyze-only` 시 여기서 종료)

1. 대상 컴포넌트/디렉토리의 `.tsx`, `.ts` 파일을 Read/Grep으로 탐색
2. 아래 **안티패턴 체크리스트** 실행
3. 분석 리포트 출력 (아래 템플릿)

### Phase 2: 리팩토링

**Refactoring Checklist 순서**대로 점진적으로 변경. 각 단계마다 `npm run lint && npm run build && npm run test` 통과 확인.

---

## 안티패턴 체크리스트

| # | 안티패턴 | 탐지 방법 | 심각도 |
|---|---------|----------|--------|
| 1 | **God Component** | 150+ LOC 또는 3+ useState — 여러 관심사가 혼재하면 변경 영향 범위가 넓어진다 | HIGH |
| 2 | **파생 상태 동기화** | useState + useEffect로 다른 state/props에서 파생 — 불필요한 리렌더와 동기화 버그 유발 | HIGH |
| 3 | **any 타입** | `any`, `as any` — 타입 시스템의 보호를 무력화, 런타임 에러로 이어짐 | HIGH |
| 4 | **전체 스토어 구독** | `useXxxStore()` selector 없이 호출 — 무관한 상태 변경에도 리렌더 발생 | HIGH |
| 5 | **God Hook** | 단일 훅에 3+ 관심사, 100+ LOC — 테스트·재사용·이해가 어려워짐 | HIGH |
| 6 | **Props → State 복사** | `useState(props.value)` — props 변경 시 동기화 안 됨 | HIGH |
| 7 | **useEffect 데이터 페칭** | useEffect 내 fetch (TanStack Query 미사용) — 캐싱·재시도·경합 처리 누락 | MEDIUM |
| 8 | **매직 넘버/문자열** | 하드코딩된 값 — 의미 불명, 변경 시 산탄총 수술 필요 | MEDIUM |
| 9 | **Props Drilling 3+** | 동일 prop 3단계 이상 전달 — composition이나 context로 해결 | MEDIUM |
| 10 | **동적 Tailwind 클래스** | `` `bg-${color}-500` `` — Tailwind은 빌드 시 정적 스캔이라 동적 클래스 미생성 | MEDIUM |
| 11 | **cleanup 누락 useEffect** | addEventListener/setInterval 후 cleanup 없음 — 메모리 누수·좀비 리스너 | MEDIUM |
| 12 | **중첩 삼항** | 삼항 내 삼항 — 가독성 급락, guard clause로 대체 | MEDIUM |
| 13 | **과잉 메모이제이션** | 저비용 연산에 useMemo/useCallback — 복잡성만 증가, 프로파일링 없이 최적화하지 말 것 | LOW |
| 14 | **코드 스플리팅 미적용** | 라우트가 lazy 없이 정적 import — 초기 번들 비대화 | LOW |
| 15 | **비의미적 HTML** | `<div onClick>` — 접근성·키보드 내비게이션 불가 | LOW |

---

## 분석 리포트 템플릿

```
## 프론트엔드 리팩토링 분석 리포트: {ComponentName}

### 발견된 안티패턴
- [HIGH] God Component: InterviewPage.tsx (320 LOC, useState 7개)
- [HIGH] 파생 상태 동기화: filteredItems를 useState + useEffect로 관리
- [MEDIUM] 매직 넘버: audioLevel > 0.7 (상수 미추출)

### 리팩토링 권장사항
1. InterviewPage → InterviewControls + QuestionPanel + VideoPreview 분리
2. filteredItems → useMemo로 직접 계산
3. 0.7 → HIGH_VOLUME_THRESHOLD 상수 추출

### 예상 변경 파일
- interview-page.tsx (컴포넌트 분리)
- use-interview-session.ts (훅 관심사 분리)
- constants/interview.ts (상수 추출)
```

---

## 8개 영역 핵심 규칙

각 영역의 상세 패턴과 코드 예시는 `references/` 참조. 여기서는 판단 기준만 정리.

### 1. 컴포넌트 설계

- **크기 기준**: Stateless 40 LOC / Stateful 150 LOC / Page 200 LOC — 초과 시 분리
- **이름에 "And"** 들어가면 SRP 위반 → 분리 대상
- **Compound Component**: boolean prop 5개 이상이면 Context 기반 합성으로 전환
- **조건부 렌더링**: Early return → 삼항 → && 순서. 중첩 삼항 금지
- 상세 예시: `references/component-patterns.md` §1

### 2. TypeScript

- `any` → `unknown` + 타입 가드. `as` 캐스팅 → 타입 가드로 대체
- boolean 플래그 조합 → **discriminated union** (`status: 'idle' | 'loading' | 'success' | 'error'`)
- Props/객체 → `interface`, Union/literal → `type`
- 상세 예시: `references/component-patterns.md` §2

### 3. 클린코드

- **네이밍**: 컴포넌트 PascalCase, 훅 `use*`, 내부 핸들러 `handle*`, props 핸들러 `on*`, boolean `is/has/should`, 상수 UPPER_SNAKE, 파일 kebab-case
- **매직 넘버**: `constants/` 디렉토리에 도메인별 상수 파일로 추출
- **함수**: 20줄 이하, 인자 3개 이하, guard clause로 early return, 최대 2단계 중첩
- 상세 예시: `references/component-patterns.md` §3

### 4. 커스텀 훅

- **One Hook = One Concern**: 오디오·STT·녹화를 각각 분리, 오케스트레이션 훅이 조합
- **크기**: 단일 관심사 50 LOC / 오케스트레이션 150 LOC
- **cleanup 필수**: addEventListener, SpeechRecognition, setInterval 등 리소스는 반드시 정리
- **Ref 패턴**: 콜백 안정화가 필요하면 `useRef`로 최신 값 유지
- **AbortController**: 비동기 경합 방지
- 상세 예시: `references/component-patterns.md` §4

### 5. Zustand

- **도메인별 분리**: 하나의 거대 스토어 X → 기능별 스토어 (interview, auth 등)
- **Selector 필수**: `useStore((s) => s.phase)` — 전체 구독은 불필요한 리렌더의 주범
- **getState()**: 이벤트 핸들러에서 stale closure 방지용
- **불변 업데이트**: `state.items.push()` X → `[...state.items, item]`
- 상세 예시: `references/state-management.md` §1

### 6. TanStack Query

- **Query Key 중앙화**: `queryKeys` 객체에 계층적 키 정의 → 오타 방지, 계층적 무효화
- **커스텀 Query Hook**: 컴포넌트에 useQuery 직접 사용 X → `useFetchInterview(id)` 훅으로 캡슐화
- **Mutation**: onMutate(optimistic) → onError(rollback) → onSettled(invalidate) 패턴
- 상세 예시: `references/state-management.md` §2

### 7. Tailwind CSS

- **동적 클래스 금지**: `` `bg-${color}-500` `` → `Record<Color, string>` 정적 매핑
- **Variant 패턴**: `Record<Variant, string>` + `array.join(' ')` (이 프로젝트의 Button 패턴)
- **조건부 클래스**: `[...classes].filter(Boolean).join(' ')` 또는 `cn()` 유틸리티
- **금지**: `@apply` 남용, 임의 값 (`h-[427px]`), 인라인 style
- 상세 예시: `references/style-performance.md` §1

### 8. 성능

- **memo/useMemo/useCallback**: 프로파일링 먼저. memo된 자식에 전달할 때만 의미 있음
- **파생 상태**: useState + useEffect 동기화 X → 직접 계산 또는 useMemo
- **Props → State 복사 금지**: props 직접 사용, 초기값 리셋은 `key` prop 활용
- **렌더 내 컴포넌트 정의 금지**: 매 렌더마다 언마운트/리마운트 반복됨
- **코드 스플리팅**: `lazy()` + `Suspense` (라우트 단위)
- 상세 예시: `references/style-performance.md` §2

---

## Refactoring Checklist (실행 순서)

점진적으로 진행. 각 단계마다 lint + build + test 통과 확인.

1. **any 타입 제거** → interface/type, unknown + 타입 가드 (ROI 최고)
2. **파생 상태 동기화 제거** → 직접 계산 / useMemo
3. **God Component 분리** → 자식 컴포넌트 추출, 훅으로 로직 분리
4. **God Hook 분리** → 관심사별 단일 훅 + 오케스트레이션
5. **Zustand selector 적용** → 개별 selector, getState()
6. **TanStack Query 통일** → queryKeys 중앙화, 커스텀 훅
7. **Tailwind 정적 매핑** → Record, 동적 클래스 제거
8. **코드 스플리팅** → lazy + Suspense

## 검증 기준

- [ ] `npm run lint` 통과
- [ ] `npm run build` 통과 (tsc + vite)
- [ ] `npm run test` 통과
- [ ] 브라우저 기능 정상 동작
- [ ] any 타입 0개
- [ ] useState + useEffect 파생 상태 0개
- [ ] Zustand 전체 스토어 구독 0개
- [ ] cleanup 없는 side-effect useEffect 0개

## Usage

```
/frontend-refactor InterviewPage --analyze-only          # 분석만
/frontend-refactor InterviewPage --scope=full            # 전체 리팩토링
/frontend-refactor src/components/interview --analyze-only  # 디렉토리 분석
/frontend-refactor src/hooks --scope=hook                # 훅만
/frontend-refactor src/stores --scope=store              # 스토어만
/frontend-refactor src/hooks --scope=query               # TanStack Query만
/frontend-refactor src/components/ui --scope=style       # Tailwind만
```

## Notes

- **점진적 리팩토링**: 한 번에 전부 적용하지 않는다. 체크리스트 순서대로.
- **테스트 우선**: 리팩토링 전 기존 테스트 통과 확인. 없으면 먼저 작성.
- **컨벤션 우선**: `frontend/.claude/rules/conventions.md` / `architecture.md` 규칙이 이 스킬과 충돌하면 해당 문서가 우선.
- **한국어**: 분석 리포트와 커밋 메시지 모두 한국어.
