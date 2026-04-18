# Plan 10 Phase B.1 — Layout Primitives (6종) + Sheet + useBreakpoint

> **Status**: Implemented
> **Base branch**: `develop`
> **Working branch**: `refactor/fe-phase-b1-layout-primitives`
> **Parent spec**: `.omc/plans/2026-04-18-product-ui-redesign.md` §4–§5
> **Handoff context**: `docs/plans/frontend-design-overhaul/phase-10-handoff.md`

---

## Why

Phase A는 토큰(`--accent-editorial`, `--signal-*`, `--utility-bar-height`)과 DESIGN.md 개정까지 완료했지만, 그 토큰 위에서 동작할 **레이아웃 primitive**가 아직 없다. Phase B.2(feedback-page 3-pane 적용)는 primitive 없이 진행 불가.

Phase B.1은 **구조 primitive 6종 + 의존성(shadcn Sheet, useBreakpoint 훅)** 을 별도 PR로 제공해 B.2가 페이지 전환에만 집중할 수 있게 한다.

---

## Scope

### 신규 primitive (spec §5)
- `frontend/src/components/layout/page-grid.tsx` (§5.1) — 12-col asymmetric wrapper, responsive 4→8→12
- `frontend/src/components/layout/reading-column.tsx` (§5.2) — `55ch` + `text-[1.0625rem]/[1.65]` scan-first
- `frontend/src/components/layout/sticky-rail.tsx` (§5.4-A) — 순수 sticky col + offset
- `frontend/src/components/layout/chapter-marker.tsx` (§5.6) — over-line 숫자 + display-lg 제목
- `frontend/src/components/layout/utility-bar.tsx` (§5.5) — 44/56px sticky 헤더, `h-[var(--utility-bar-height)]`
- `frontend/src/components/layout/sticky-outline/` (§5.3 compound)
  - `types.ts` — `OutlineItem`, `StickyOutlineBaseProps`
  - `desktop.tsx` — xl+ col-span-2 sticky
  - `tab-bar.tsx` — lg horizontal tabs
  - `mobile-sheet.tsx` — md/sm Radix Sheet (JS state 기반 open)
  - `index.ts` — `StickyOutline` namespace export

### 의존성
- `frontend/src/components/ui/sheet.tsx` — shadcn Sheet (기존 `@radix-ui/react-dialog` 재사용, 별도 설치 없음)
- `frontend/src/hooks/use-breakpoint.ts` — `matchMedia` 기반 SSR-safe 훅 (xl/lg/md/sm/2xl)

### Tier 1 RTL 테스트 (spec §13)
- `src/components/layout/__tests__/` — page-grid, reading-column, sticky-rail, chapter-marker, utility-bar (16 tests)
- `src/components/layout/sticky-outline/__tests__/sticky-outline.test.tsx` (8 tests)
- `src/hooks/__tests__/use-breakpoint.test.tsx` (2 tests)

**총 신규 26 tests, 기존 a11y 14 + 유지 24 = 64/64 pass.**

---

## Decisions

| 결정 | 근거 |
|------|------|
| VideoDock을 B.1에서 제외 | `VideoPlayer` ref / duration 추적은 feedback-page 통합 레이어. 스코프 분리 위해 B.2로 이관 |
| `ReadingColumn`에서 `text-[1.0625rem]/[1.65]` 단일 token | `tailwind-merge`가 `text-[x]` + `leading-[y]` 인접 시 leading을 삭제. Tailwind 3.3+ `text-[size]/[line]` 결합 문법으로 회피 |
| `MobileSheet`를 JS state 제어 | Portal은 CSS `hidden/block` 제어 불가 → `useState(open)` + `onSelect` 시 자동 close 처리 |
| `StickyOutline.TabBar`에 `hidden lg:flex xl:hidden` | spec은 `flex xl:hidden`만 기재했으나 md 이하에서 TabBar가 보이면 MobileSheet 트리거와 중복. lg 전용으로 한정 |
| `max-w-canvas` 유틸리티 사용 | Phase A에서 이미 `maxWidth: { 'canvas': '1440px' }` 추가됨 |

---

## Non-goals

- feedback-page 실제 마이그레이션 — **B.2**
- VideoDock, IntersectionObserver activeId 훅 — **B.2**
- review-coach-mark Drop + dismissible callout — **B.2 말미 또는 Phase D**
- Playwright Tier 2 sticky/scroll 테스트 — 추후 infra 셋업 후

---

## Verification (Phase B.1 gate)

| 항목 | 결과 |
|------|------|
| `npm run lint` | ✓ 0 warnings |
| `npx tsc -b --noEmit` | ✓ 0 errors |
| `npm run test` | ✓ 64 passed (26 신규 + 38 기존) |
| `npm run build` (vite) | ✓ built in 3.71s |
| 회귀 (a11y 14 페이지) | ✓ critical/serious 0건 유지 |

---

## Next

- B.2: `interview-feedback-page.tsx`를 `PageGrid` + `ChapterMarker` + `ReadingColumn` + `VideoDock(new)` + `StickyOutline` 조합으로 재배치. `max-w-6xl mx-auto` 제거. `QuestionList` → `StickyOutline`.
- 시작 전 gate: 본 PR CI green + 머지 확인 (또는 PR 순차성 보장).

---

## Related

- Phase A: `c3fe454` (handoff), `ca6443e` (토큰 + DESIGN.md)
- 마스터 스펙: `.omc/plans/2026-04-18-product-ui-redesign.md`
- 핸드오프: `docs/plans/frontend-design-overhaul/phase-10-handoff.md`
