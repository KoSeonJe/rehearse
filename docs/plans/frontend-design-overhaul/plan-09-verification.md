# Plan 09: 검증 보강 — a11y + Smoke + 풀 빌드 로그 (Group B)

> 상태: Draft
> 작성일: 2026-04-18

## Why

Plan 05에서 `axe-core` 스캔은 "환경 미구축, 후속 이관"으로 보류되었고, Phase 6 완료 후에도 **자동 a11y 검증 / 시각 회귀 / 풀 smoke 증거**가 기록된 바 없다. shadcn 전환(Plan 08)과 토큰 정리(Plan 07)로 광범위한 UI 변경이 발생하므로, merge 전에 다음을 수집해야 한다:

1. WCAG 2.1 AA critical/serious 위반 0건 증거
2. 라이트/다크 모드 14 페이지 수동 캡처 (드리프트 확인)
3. `lint + test + build` 풀 run log

**Trade-off**: Playwright 풀 도입은 비용이 크므로 vitest + jsdom + `@axe-core/playwright` 대신 **`vitest-axe`** 또는 `jest-axe` 수준으로 시작. 부족하면 추후 Playwright 승격.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/package.json` | `vitest-axe` (또는 `@axe-core/react`) devDependency 추가 |
| `frontend/tests/a11y/pages.spec.ts` | 14 페이지 렌더 + axe 스캔 스펙 |
| `frontend/vitest.config.ts` | a11y 스펙 경로 설정 확인 |
| `docs/plans/frontend-design-overhaul/phase-7-verify.md` | lint/test/build 풀 output, axe 결과, 번들 사이즈 기록 |
| `docs/plans/frontend-design-overhaul/screenshots/` | 14 페이지 × 라이트/다크 스크린샷(28컷) |

## 상세

### B1. axe-core 자동 스캔

**대상 14 페이지**: home, dashboard, interview-setup, interview-ready, interview-page, interview-feedback, interview-analysis, review-list, about, admin-feedbacks, faq, guide, privacy-policy, not-found

**스펙 패턴** (`frontend/tests/a11y/pages.spec.ts`):
```ts
import { render } from '@testing-library/react'
import { axe, toHaveNoViolations } from 'vitest-axe'
import HomePage from '@/pages/home-page'
// ...import all 14 pages

expect.extend({ toHaveNoViolations })

describe('a11y smoke — 14 pages', () => {
  it('home renders without critical/serious violations', async () => {
    const { container } = render(<HomePage />)
    const results = await axe(container, {
      rules: { 'color-contrast': { enabled: true } },
    })
    expect(results).toHaveNoViolations()
  })
  // ...반복
})
```

**전제**: React Router/QueryProvider 등은 테스트용 Wrapper로 래핑. API 호출은 mock 또는 loading state 렌더만 확인.

**수락 기준**:
- critical/serious violation **0건**
- minor/moderate 위반은 issue로 등록 후 backlog 이관 허용

### B2. 빌드/린트/테스트 풀 run 증거

```bash
cd frontend
npm run lint 2>&1 | tee /tmp/lint.log
npm run test 2>&1 | tee /tmp/test.log
npm run build 2>&1 | tee /tmp/build.log
```

- 세 로그의 tail(~30줄)과 실행 시각을 `docs/plans/frontend-design-overhaul/phase-7-verify.md`에 붙임
- 번들 사이즈 변동 기록(baseline ~582KB → 현재)
- test 24/24 또는 증가 시 신규 테스트 목록 명시

### B3. 14 페이지 수동 smoke 스크린샷

- `npm run dev` 후 각 라우트 접근
- Chrome DevTools → Rendering → "prefers-color-scheme: dark" 토글로 라이트/다크 각각 캡처
- 파일명: `home-light.png`, `home-dark.png`, ...
- 눈에 띄는 드리프트(라운드/간격/대비) 발견 시 `phase-7-verify.md`에 메모 + 이슈 등록

## 담당 에이전트

- Implement (B1): `test-engineer` — a11y 스펙 작성, `vitest-axe` 도입
- Implement (B2): `frontend` — 로그 수집, verify 문서 갱신
- Implement (B3): `qa-tester` — 수동 smoke 캡처
- Review: `verifier` — 수락 기준 충족 여부(critical/serious=0, build green, 28컷 수집)

## 검증

- `cd frontend && npx vitest run tests/a11y` → 14 스펙 모두 pass
- `docs/plans/frontend-design-overhaul/phase-7-verify.md` 존재, 3개 로그 + axe 결과 + 번들 사이즈 기록
- `docs/plans/frontend-design-overhaul/screenshots/` 28개 PNG 존재 (라이트 14 + 다크 14)
- Plan 05에서 "후속 이관"으로 남긴 a11y 숙제 해소
- `progress.md` Task 9 → Completed
