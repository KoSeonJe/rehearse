# Plan 08: shadcn 대량 전환 (Group C)

> 상태: Draft
> 작성일: 2026-04-18

## Why

Phase 2~6에서 **Button/Input/Dialog/Card/AlertDialog/Label/Skeleton** 7개 primitive만 shadcn으로 전환되었고, 나머지 UI 패턴(Tabs/Badge/Toast/Progress/Tooltip/Separator)은 커스텀 구현이 산재해 있다. 결과:

- `feedback-panel.tsx` 커스텀 탭: Radix 미사용으로 키보드 네비게이션(←/→/Home/End) 부재, `role`/`aria-controls` 수동
- 뱃지 유형 **17파일 산재** (beta/level/source/category) — 색상/사이즈/variant 불일치
- `review-toast.tsx` 커스텀: portal/stacking/swipe-dismiss/aria-live 수작업
- `setup-progress-bar.tsx`, `finishing-overlay` 진행 바: Radix Progress 미사용으로 `aria-valuenow` 누락

**Goal**: shadcn/ui 표준 primitive로 전환해 a11y/키보드/포커스 관리를 "무료"로 확보하고, 토큰-variant 체계를 일원화한다.

**Evidence**: `components.json` 이미 설정됨(`style: new-york`, `baseColor: neutral`). 기존 shadcn 도입 성공 경험(Phase 2/3a~3d).

**Trade-offs**:
- 신규 의존성 몇 개 추가(`@radix-ui/react-tabs`, `@radix-ui/react-tooltip`, `sonner` 등) — 번들 미세 증가 허용
- 기존 커스텀 API 호환 래퍼 필요(예: `showReviewToast()` → `toast()`) — 호출부 수정 최소화

## 생성/수정 파일

### 신규 (shadcn add)

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/tabs.tsx` | `npx shadcn@latest add tabs` |
| `frontend/src/components/ui/badge.tsx` | `npx shadcn@latest add badge` |
| `frontend/src/components/ui/sonner.tsx` | `npx shadcn@latest add sonner` |
| `frontend/src/components/ui/progress.tsx` | `npx shadcn@latest add progress` |
| `frontend/src/components/ui/tooltip.tsx` | `npx shadcn@latest add tooltip` |
| `frontend/src/components/ui/separator.tsx` | `npx shadcn@latest add separator` |

### 수정 (리라이트 / 교체)

| 파일 | 작업 |
|------|------|
| `frontend/src/App.tsx` (또는 `main.tsx`) | 루트에 `<Toaster />` 1회 마운트 + `<TooltipProvider>` 래핑 |
| `frontend/src/components/feedback/feedback-panel.tsx` | 커스텀 탭 → shadcn `Tabs`/`TabsList`/`TabsTrigger`/`TabsContent` |
| `frontend/src/components/ui/beta-badge.tsx` | Badge variant 래퍼로 재작성 |
| `frontend/src/components/feedback/level-badge.tsx` | Badge + intensity variant 매핑 |
| `frontend/src/pages/admin-feedbacks-page.tsx` | 인라인 SourceBadge → Badge variant |
| `frontend/src/components/dashboard/{interview-card,interview-table,interview-list}.tsx` | CS/상태 뱃지 → Badge |
| `frontend/src/components/dashboard/{sidebar,dashboard-header}.tsx` | 카운터 → Badge `variant="secondary"` |
| `frontend/src/components/setup/step-tech-stack.tsx` | 태그 뱃지 → Badge |
| `frontend/src/components/interview/{question-display,question-card}.tsx` | 카테고리 뱃지 → Badge |
| `frontend/src/components/feedback/delivery-tab.tsx` | 소분류 뱃지 → Badge |
| `frontend/src/components/common/review-toast.tsx` | sonner `toast()` wrapper로 재구현 (기존 `showReviewToast` export 유지) |
| `frontend/src/components/feedback/bookmark-toggle-button.tsx` | 호출부 확인(래퍼로 동작 시 변경 없음) |
| `frontend/src/components/feedback/__tests__/bookmark-toggle-button.test.tsx` | `sonner` mock으로 업데이트 |
| `frontend/src/components/setup/setup-progress-bar.tsx` | shadcn `Progress` 교체 |
| `frontend/src/components/interview/finishing-overlay.tsx` | 내부 진행 바 → `Progress` (다크 톤 오버라이드 유지) |
| `frontend/src/components/dashboard/sidebar.tsx` | 축소 모드 아이콘 `Tooltip` 도입 (선택) |
| `frontend/src/components/content/content-page-shell.tsx`, `login-modal.tsx`, `feedback-panel.tsx` 등 | 명확한 구분선 `border-t`/`border-b` → `Separator` (10파일 내외) |

## 상세

### 우선순위 및 커밋 분할

| # | 세부 | 우선순위 | 커밋 |
|---|------|----------|------|
| 08-1 | shadcn add (tabs/badge/sonner/progress/tooltip/separator) + Toaster/TooltipProvider 마운트 | P0 | `chore(fe): shadcn Tabs/Badge/Sonner/Progress/Tooltip/Separator 설치` |
| 08-2 | feedback-panel Tabs 전환 | P0 | `refactor(fe): feedback-panel 커스텀 탭 → shadcn Tabs` |
| 08-3 | Badge 통합 (beta/level/source/category/counter 17파일) | P0 | `refactor(fe): Badge 통합 — 17파일 뱃지 shadcn Badge 전환` |
| 08-4 | review-toast → sonner | P0 | `refactor(fe): review-toast → sonner 마이그레이션` |
| 08-5 | setup-progress-bar/finishing-overlay → Progress | P1 | `refactor(fe): 진행 바 shadcn Progress 전환` |
| 08-6 | sidebar/controls Tooltip 도입 | P1 | `refactor(fe): sidebar 축소 모드 Tooltip 적용` |
| 08-7 | border 구분선 → Separator | P1 | `refactor(fe): 구조적 구분선 shadcn Separator 전환` |

각 커밋은 `npm run lint && npm run test && npm run build` 통과 후 진행.

### 마이그레이션 원칙

- **API 호환 래퍼 유지**: `showReviewToast(...)`은 sonner `toast()` 감싸서 호출부 수정 최소화
- **variant 매핑 명확화**: Badge는 `default/secondary/destructive/outline` 4가지 + 필요 시 `muted`/`success` 등 확장
- **스튜디오 다크 UI 보존**: `finishing-overlay`는 Progress 내부 색만 CSS 변수로 오버라이드 (구조는 유지)
- **테스트 유지**: 기존 vitest 테스트(review-list-filter-bar, bookmark-toggle-button 등) 통과 유지

### 스코프 제외

- **C8 RadioGroup** (setup 선택 카드): 카드형 UI 리라이트 비용 큼. 수동 `role="radio"`/`aria-checked` 부여로 절충 → 별도 spec 이관
- **C9 Sidebar (shadcn v4)**: 대시보드 사이드바 전면 리라이트 → 별도 spec 이관

## 담당 에이전트

- Implement: `frontend` — shadcn add, 각 컴포넌트 리라이트, 호출부 치환
- Implement 보조: `/shadcn` 스킬 — 컴포넌트별 설치 가이드 및 variant 추천
- Review: `code-reviewer` — API 호환성, variant 일관성, 테스트 mock 정합
- Review: `designer` — 시각 회귀(Badge/Tabs/Toast 디자인 일관성)
- Review: `architect-reviewer` — `<Toaster/>` / `<TooltipProvider>` 루트 배치 및 provider 경계

## 검증

- **설치 확인**: `ls frontend/src/components/ui/` → tabs/badge/sonner/progress/tooltip/separator 존재
- **Tabs**: feedback 탭 스위칭 기능 회귀 없음, 키보드 ←/→/Home/End 동작
- **Badge**: 17파일 grep로 인라인 `rounded-full bg-* text-*` 뱃지 패턴 대폭 감소, Badge import 17파일에서 확인
- **Sonner**: bookmark-toggle-button.test.tsx green, 실제 북마크 저장 시 토스트 표시
- **Progress**: `aria-valuenow` 표기 확인, 진행 값 업데이트 확인
- **Tooltip**: 사이드바 축소 모드에서 hover 1초 후 라벨 표시
- **Separator**: 구조 구분선 시각적 유지
- `npm run lint` / `npm run test` / `npm run build` 모두 green
- 번들 사이즈 증가 <50KB (현재 ~582KB 기준)
- `progress.md` Task 8 → Completed
