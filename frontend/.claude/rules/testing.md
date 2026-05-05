# Frontend Testing Rule

## 핵심 원칙

1. **행위 테스트 우선** — 사용자 시점 검증. 구현 디테일 단언 X.
2. **경계만 Mock** — 네트워크 (msw), 브라우저 API (`MediaRecorder`, `localStorage`). 내부 컴포넌트 / 훅 = 실제.
3. **피라미드** — Unit 多 / Integration 中 / E2E 少.
4. **비결정 snapshot 금지** — LLM 출력 / timestamp / 랜덤 X.

---

## 스택

- Vitest + jsdom
- React Testing Library + `@testing-library/user-event`
- MSW (네트워크 mock — `src/mocks/`)
- Setup: `src/test/setup.ts`

---

## 카테고리 + 선택 기준

| 카테고리 | 대상 | 도구 | 위치 | 비중 |
|---------|------|------|------|------|
| **Unit** | 순수 함수, 작은 훅 (외부 의존 X) | Vitest | `__tests__/` (대상 옆) | ≥50% |
| **Integration** | 컴포넌트 + 훅 + 비동기 흐름 | RTL + user-event + msw | `__tests__/` (대상 옆) | 35–40% |
| **E2E (Page Scenario)** | 페이지 마운트 + 라우트 + 사용자 시나리오 | RTL + msw + Provider 전체 | `frontend/tests/` 또는 `pages/__tests__/` | ≤10% |

### 결정 트리

```
변경 = 순수 로직 (계산식 / 포맷터 / 상태 머신 reducer)?
  → Unit

변경 = 비즈 로직 → 화면 변경 (점수 ≥5 → 배지 표시 / phase 변경 → 버튼 토글)?
  → Integration  (컴포넌트 + 훅 결합 검증)

변경 = 여러 컴포넌트 + 라우트 + API 흐름 (인터뷰 시작 → 녹화 → 피드백)?
  → E2E (Page Scenario)  — 1 시나리오만
```

> **사용자 핵심 우려 — 비즈 로직 변경 시 화면 영향**: 대부분 **Integration**. 로직만 따로 단위 검증 + 화면 결합은 통합 검증. E2E 는 흐름 1~2개로 한정.

---

## Unit

- 대상: `lib/`, `utils/`, 순수 훅 (네트워크 / DOM 의존 X).
- 외부 의존 모두 모킹 가능 시점에서 생각. 의존 많으면 Integration 으로 승격.
- 예: `format-utils`, score 계산, reducer, `useBreakpoint`.

```ts
test('점수 ≥5 면 PASS 반환', () => {
  expect(scoreToBadge(5)).toBe('PASS');
});
```

---

## Integration

- **비즈 로직 → 화면 매핑 검증의 본진**. 실 컴포넌트 + 실 훅 + msw 네트워크.
- `render(<Component />)` + `userEvent.click()` + `screen.findByRole(...)` 표준 패턴.
- 외부만 mock: API (msw) / `MediaRecorder` / `localStorage` / `navigator.mediaDevices`.
- **컴포넌트 내부 함수 mock 금지** — 행위 변화로 검증.

```tsx
test('답변 제출 후 피드백 카드 노출', async () => {
  const user = userEvent.setup();
  render(<InterviewPage />, { wrapper });
  await user.click(screen.getByRole('button', { name: '답변 제출' }));
  expect(await screen.findByText(/피드백/)).toBeInTheDocument();
});
```

### 비즈 로직 → 화면 변경 시나리오

| 변경 | 검증 항목 |
|------|----------|
| 점수 환산 식 변경 | 점수 표시 / 배지 / 색상 토큰 변화 |
| 상태 머신 추가 phase | 신규 phase UI 노출 / 이전 phase 사라짐 / 버튼 enable·disable |
| API 응답 필드 추가 | 신규 필드 컴포넌트 렌더 / 폴백 처리 |
| 권한 정책 변경 | 권한 없는 사용자 = 라우트 리다이렉트 / 메시지 노출 |

---

## E2E (Page Scenario)

- **최소화**. 핵심 사용자 플로우 1~2개만.
- 대상: 페이지 컴포넌트 전체 마운트 (`<MemoryRouter>` + `QueryClientProvider` + msw).
- 검증: 라우트 이동 / 다중 컴포넌트 협력 / 진입 → 종료 흐름.
- 동일 케이스 Integration 으로 가능하면 Integration 우선. E2E 는 흐름 검증 한정.
- **브라우저 자동화 없음** (Playwright 미사용). vitest jsdom 기반 시나리오 테스트.

---

## Mock 정책

| 대상 | 정책 |
|------|------|
| 네트워크 (`apiClient`, fetch) | **msw 핸들러** (`src/mocks/`) |
| 브라우저 API (`MediaRecorder`, `getUserMedia`, `localStorage`, `IndexedDB`) | `vi.stubGlobal` / `vi.spyOn` |
| 시간 (`setTimeout`, `Date.now`) | `vi.useFakeTimers()` |
| 외부 라이브러리 (sonner toast 등) | 필요 시만 mock |
| **내부 컴포넌트 / 훅** | **mock 금지** — 실제 렌더 / 호출 |
| LLM 결과 / timestamp | mock으로 결정값 고정 (snapshot X) |

---

## 네이밍

- 파일: `<대상>.test.ts(x)` 또는 `<대상>.spec.ts(x)`.
- 위치: 대상 모듈 옆 `__tests__/` (co-location).
- 테스트명: 한국어 한 줄, 도메인 행위 표현.
  - `'답변 제출 후 피드백 카드 노출'` ✅
  - `'should render correctly'` ❌

---

## 안티 패턴 (즉시 반려)

- 내부 컴포넌트 / 훅 mock.
- `getByTestId` 남발 — `getByRole` / `getByLabelText` 우선.
- 구현 디테일 단언 (state 값 / private 메서드 / class name).
- LLM / timestamp / 랜덤 출력 snapshot.
- `act()` 강제 wrapping (RTL 자동 처리 — 필요 시만).
- 비동기 단언 `setTimeout` 대기 — `findBy*` / `waitFor` 사용.
- 네트워크 실 호출 (msw 미설정).
- 테스트 위해 production 코드 export 추가.

---

## 진입 체크리스트

새 기능 / 변경 진입 시:

1. **순수 로직?** → Unit. 의존 0개.
2. **비즈 → 화면?** → Integration. 컴포넌트 + 훅 + msw.
3. **다중 페이지 / 라우트 흐름?** → E2E 1 시나리오.
4. 모든 외부 경계 mock 했는지 확인.
5. 테스트명 = 도메인 행위 한국어.
