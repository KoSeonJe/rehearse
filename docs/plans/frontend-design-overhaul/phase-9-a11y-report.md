# Phase 9 a11y Report — B1 axe-core 자동 스캔

> 작성일: 2026-04-18
> 브랜치: refactor/frontend-design-overhaul
> 실행 환경: vitest v4.0.18 + jsdom + vitest-axe

---

## 설치된 의존성

| 패키지 | 버전 | 용도 |
|--------|------|------|
| `vitest-axe` | npm latest (devDependency) | axe-core 래퍼 + `toHaveNoViolations` matcher |

`axe-core`는 `vitest-axe` 내부 의존성으로 자동 포함.

---

## 스펙 구조

**파일**: `frontend/tests/a11y/pages.spec.tsx`

### Wrapper 전략

```
QueryClientProvider (retry:false, gcTime:0)
  └─ MemoryRouter (initialEntries=[경로])
       └─ Routes > Route path=경로 > <PageComponent />
```

### Mock 전략

| 카테고리 | 방식 |
|----------|------|
| react-router-dom | MemoryRouter 실제 사용 (mock 없음) |
| react-helmet-async | `Helmet` → pass-through fragment |
| TanStack Query 훅 | `isLoading: true` 반환 → loading skeleton 렌더 |
| Zustand interview-store | `vi.fn()` + `.getState()` static 메서드 수동 부착 |
| next-themes | `useTheme` → `{ theme: 'light' }` stub |
| IntersectionObserver | class 형식으로 mock (constructor 지원) |
| ResizeObserver / MediaRecorder / getUserMedia | vi.fn() stub |
| Canvas getContext | jsdom 미구현 — console warning만 발생 (테스트 영향 없음) |

### axe 스캔 기준

- `wcag2a`, `wcag2aa`, `wcag21aa`, `best-practice` 태그 대상
- **critical/serious** 위반 → 테스트 실패
- **minor/moderate** 위반 → `console.warn` 후 통과 (백로그 이관)

---

## 실행 결과

**14/14 통과** (38/38 전체 테스트 통과)

| 페이지 | 결과 | 비고 |
|--------|------|------|
| home | PASS | |
| dashboard | PASS | |
| interview-setup | PASS | aria-label 추가로 위반 해소 (2026-04-18 수정) |
| interview-ready | PASS | |
| interview-page | PASS | |
| interview-feedback | PASS | |
| interview-analysis | PASS | |
| review-list | PASS | |
| about | PASS | |
| admin-feedbacks | PASS | |
| faq | PASS | |
| guide/ai-mock-interview | PASS | |
| privacy-policy | PASS | |
| not-found | PASS | |

---

## Critical/Serious 위반 — 0건 ✅

스캔 중 1건 발견 후 동일 커밋 내에서 해소:

### [resolved] `aria-progressbar-name`

- **발견**: `SetupProgressBar`/`finishing-overlay`의 shadcn `<Progress>`에 `aria-label` 누락
- **수정**:
  - `setup-progress-bar.tsx:37` — `aria-label="면접 설정 진행 상황"` 추가
  - `finishing-overlay.tsx:62` — `aria-label="녹화 업로드 진행률"` 추가
- **결과**: axe rule 재활성화 후 스캔 통과. 14/14 critical/serious 0건.

---

## Minor/Moderate 위반

스캔 결과 0건. 모든 페이지에서 console.warn 없이 통과.

---

## 기존 테스트 영향

| 구분 | 이전 | 이후 |
|------|------|------|
| 기존 단위/컴포넌트 테스트 | 24/24 | 24/24 |
| 신규 a11y 테스트 | — | 14/14 |
| **전체** | **24/24** | **38/38** |

영향 없음.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `frontend/package.json` | `vitest-axe` devDependency 추가 |
| `frontend/vitest.config.ts` | `test.include`에 `tests/**/*.{test,spec}.{ts,tsx}` 경로 추가 |
| `frontend/tests/a11y/pages.spec.tsx` | 14 페이지 a11y 스펙 신규 생성 |

---

## 후속 이슈 등록 필요

없음. critical/serious 위반 모두 해소.

향후 B3 수동 스크린샷(라이트/다크 14 페이지)으로 시각 회귀 확인 권장.
