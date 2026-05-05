# Rehearse Design System

> 디자인 / UI 작업 한정 단일 소스. 코드 컨벤션은 `frontend/.claude/rules/conventions.md`.
> 기조: Cal.com inspired (monochrome restraint) + Light-only Teal (Quiet Rigor).

---

## 1. Color (2026-04-18 Light-only Teal)

> 개정 사유: 기존 warm off-neutral 베이스 = Anthropic Claude 팔레트와 식별 불가 → 전면 cool tone. Light-only 배포 (다크모드 폐지, interview-page 만 극장 스테이지 scope).

### Core 토큰

| 토큰 | 값 | 용도 |
|------|----|------|
| `--background` | `#ffffff` Pure White | 모든 페이지 배경 |
| `--foreground` | `#042f2e` teal-950 | 본문 / 제목 (대비 17.8:1 AAA) |
| `--muted-foreground` | `#5a7574` teal-gray | 보조 텍스트 / 캡션 (4.63:1 AA) |
| `--muted` / `--secondary` | `#eff7f6` cool mint | hover / accent surface |
| `--border` | `#e6f0ef` teal hairline | border / input |

### Brand (시그니처 — Teal)

| 토큰 | Light | Dark | 용도 |
|------|-------|------|------|
| `--brand` | `#0F766E` teal-700 (7.3:1 AAA) | `#2dd4bf` teal-400 (6.8:1) | Primary CTA / 인라인 링크 / focus ring / active nav / selected pill / scrubber |
| `--brand-hover` | `#0D9488` teal-600 | `#14b8a6` teal-500 | CTA hover |
| `--brand-bg` | `#ccfbf1` teal-100 | `#134e4a` teal-900 | Active subtle surface |
| `--brand-foreground` | `#fafaf7` | `#14130f` | brand 배경 위 텍스트 |

**역할 단일화**:
- Brand = 액션 / 선택 / 활성 (Primary CTA, link, focus, active nav, selected)
- Primary (`#14130f`) = 일반 UI 강조 (본문 텍스트, secondary dark 버튼)
- `--link` / `--ring` 모두 `--brand` 참조 통합

### Semantic 4종 (엄격 분리)

| 토큰 | Light | Dark | 용도 | 제한 |
|------|-------|------|------|------|
| `--accent-editorial` | `#a65131` terracotta | `#c57458` | Feedback 하이라이트 / ChapterMarker 숫자 | hue 13°, ochre와 ≥30° 이격 |
| `--signal-record` | `#c8322a` warm red | `#d46b5f` | 녹화 dot + "REC" | `interview-page` 전용 |
| `--signal-warning` | `#d4a017` ochre | `#e6b946` | 타임 카운트다운 / 초과 경고 | 타임 경고 한정 |
| `--signal-success` | `#5a7a4a` muted sage | `#87a26f` | 저장 완료 toast | toast 1개 한정. 페이지 배경 X |

각 signal `-bg` suffix 변수 제공 (light 95% L pastel / dark 14–16% L shade).

**Interview Stage 전용** (극장 몰입 — 다른 페이지 X):
- bg `#031f1e` / fg `#f0fdfa`

**Gradient 금지** — 깊이는 shadow 만.

---

## 2. Typography

### 폰트 (3종 확정)

| 역할 | 폰트 | 클래스 | 용도 |
|------|------|--------|------|
| Sans (필수) | Pretendard Variable / Pretendard | `font-sans` | 한글·영문 UI 본문·헤딩 |
| Serif (영문 display 단일) | Fraunces | `font-serif` | 영문 헤드라인 / 숫자(`01`) / Metrics 초대형 / 영문 부분 |
| Mono | JetBrains Mono | `font-mono` | 코드 / 기술 스택 태그 |
| Tabular 유틸 | — | `.font-tabular` | 타이머·통계 숫자 **필수** (`tnum`) |

> Cal Sans 제거 (2026-04-18). Fraunces = 영문 display 단일. `font-display` 는 Fraunces alias 하위 호환만, 신규 사용 금지.
>
> **한글 위계** = Pretendard 굵기(700→600→500→400) + 자간(-0.02em→0) + 크기. Fraunces 한글 강제 X (mix 파편화).

### 위계

| Role | Font | Size | Weight | LH | LS | 비고 |
|------|------|------|--------|-----|-----|------|
| `display-xl` | Fraunces / Pretendard | 64 | 700 | 1.05 | -0.02em | Metrics 초대형 / 히어로 영문 |
| `display-lg` | Fraunces / Pretendard | 48 | 700 | 1.10 | -0.02em | ChapterMarker 질문 제목 |
| `h1` | Pretendard | 40 | 700 | 1.20 | -0.01em | 페이지 타이틀 |
| `h2` | Pretendard | 28 | 700 | 1.30 | -0.01em | 섹션 헤딩 |
| `h3` | Pretendard | 20 | 600 | 1.40 | 0 | 서브 섹션 |
| `body-lg` | Pretendard | 18 | 400 | 1.65 | 0 | 피드백 본문 (scan-first) |
| `body` | Pretendard | 16 | 400 | 1.70 | 0 | 일반 |
| `body-sm` | Pretendard | 14 | 400 | 1.60 | 0 | 보조 |
| `caption` | Pretendard | 12 | 500 | 1.50 | +0.02em | 레이블 |
| `over-line` | Pretendard | 11 | 600 | 1.50 | +0.10em | ChapterMarker 숫자 caption |
| `mono` | JetBrains Mono | 13 | 400 | 1.60 | 0 | 코드 |
| `numeric-display` | Fraunces | any | 700 | 1.05 | -0.02em | 숫자 단독 (`01`, `42min`) |

---

## 3. Components

### Buttons
- **Primary CTA** = `bg-brand` (teal). dark primary `bg-primary` 는 secondary / 본문 강조 한정.
- **Ghost** = white bg + shadow ring + dark text.
- **Pill** = `rounded-full` (9999px) — 배지 / 태그.
- Radius = 6–8px 표준. inset highlight (`rgba(255,255,255,0.15) 0 2px 0 inset`) 선택.

### Cards
- White bg + 멀티 shadow stack (Section 5 참조).
- Radius: 8 (표준) / 12 (큰 컨테이너) / 16 (prominent).

### Inputs
- White bg + 1px hairline border + 8px radius.
- Focus = `focus-visible:ring-2 ring-ring` (= brand teal).

### Image
- 제품 스크린샷이 유일한 비주얼 콘텐츠. 일러스트 / 추상 그래픽 / 장식 X.

---

## 4. Layout

### Spacing (8px base)
스케일: 1 / 2 / 4 / 6 / 8 / 12 / 16 / 20 / 24 / 28 / 80 / 96.
- 섹션 간격 = 80–96 (모바일 sm 48 축소).
- 카드 패딩 = 12–24.
- 컴포넌트 간격 = 4–8.

### 12-col Asymmetric Grid
- 전역: `grid grid-cols-12 gap-x-6 max-w-[1440px] mx-auto px-4 md:px-8 lg:px-12`
- 좌측 anchor 원칙. 중앙 정렬 금지 (`max-w-* mx-auto` 폐기).
- 반응형: `sm 4-col` / `md 8-col` / `lg+ 12-col`

### Recipe (페이지별)

| Recipe | 비율 | 적용 |
|--------|------|------|
| A: 7+5 | col-7 + col-5 | Home Hero |
| B: 4+8 | sticky col-4 + col-8 | Setup |
| C: 2+6+4 | col-2 + col-6 + col-4 | Feedback (StickyOutline + ReadingColumn + VideoDock) |
| D: 8+4 | col-8 + col-4 | Dashboard |

### Border Radius
2 / 4 / 6–7 / **8 표준** / 12 / 16 / 29 / 100 / 1000 / **9999 pill**.

### Structural Primitives (`components/layout/`)
`PageGrid`, `ReadingColumn` (max-w-[55ch], leading-1.65), `StickyOutline` (Desktop / TabBar / MobileSheet), `StickyRail`, `VideoDock`, `ChapterMarker` (over-line 11px + 40–48px 제목), `UtilityBar` (h-11 / md:h-14).

---

## 5. Elevation (Shadow only, no border)

| Level | Treatment | 용도 |
|-------|-----------|------|
| 0 Flat | shadow X | 페이지 캔버스 |
| 1 Inset | `rgba(0,0,0,0.16) 0 1px 1.9px 0 inset` | 입력 well / pressed |
| 2 Ring + Soft | `rgba(19,19,22,0.7) 0 1px 5px -4px, rgba(34,42,53,0.08) 0 0 0 1px, rgba(34,42,53,0.05) 0 4px 8px 0` | **카드 표준** |
| 3 Ring Alt | `rgba(36,36,36,0.7) 0 1px 5px -4px, rgba(36,36,36,0.05) 0 4px 8px` | alt elevation |
| 4 Inset Highlight | `rgba(255,255,255,0.15) 0 2px 0 inset` | 버튼 3D bevel |
| 5 Soft | `rgba(34,42,53,0.05) 0 4px 8px` | 앰비언트 |

**철학**: ring shadow (`0 0 0 1px`) = CSS border 대체. layout 영향 없는 hairline. 콤마 stack 으로 multi-layer composit.

---

## 6. Responsive

### Breakpoint

| BP | 범위 | 레이아웃 | 변화 |
|----|------|----------|------|
| sm | <768 | 단일 컬럼 | VideoDock sticky 25vh, StickyOutline → MobileSheet |
| md | 768–1023 | 단일 | VideoDock sticky 30vh, StickyOutline → MobileSheet |
| lg | 1024–1279 | 2-pane | StickyOutline → TabBar, col-8+4 |
| xl | ≥1280 | 3-pane | StickyOutline → Desktop, col-2+6+4 |

### 터치 (WCAG 2.5.5)
- 모든 interactive 최소 **44×44px**.
- UtilityBar: `--utility-bar-height: 44px` desktop / `56px` mobile (max-width 767).
- Interview 종료 / 일시중지 = **항상 visible** (hover 숨김 X).

### Primitive Fallback
- `StickyOutline.Desktop`: `hidden xl:flex`
- `StickyOutline.TabBar`: `flex xl:hidden`
- `StickyOutline.MobileSheet`: trigger `lg:hidden` (Portal = JS state)

---

## 7. Do / Don't

### Do
- Fraunces = 영문 + 숫자 (subset `0123456789` 로드).
- Pretendard = 한글 + 영문 본문. 굵기 변주로 위계.
- Cool teal 팔레트 유지 (`#ffffff` / `#042f2e` / brand teal-700).
- Shadow ring + diffused 조합 유지.
- `font-tabular` = 타이머 / 통계 숫자 **필수**.
- 섹션 간격 80–96 (sm 48).

### Don't
- **warm off-white / off-black 재도입 X** (`#fafaf7`, `#14130f` 베이스 — Claude 팔레트 충돌, 2026-04-18 제거).
- **warm tint 일체 X** (terracotta surface, peach, cream). surface tint = `--muted` (`#eff7f6` cool mint) 만.
- **일반 다크모드 X**. `.dark` = `interview-page` 극장 scope 한정.
- **`bg-primary` CTA X** — Primary CTA = `bg-brand`.
- **Cal Sans 재도입 X** (2026-04-18 제거).
- **`backdrop-blur` sticky header X** — opaque `bg-background` + hairline.
- **Fraunces 한글 강제 X** (mix 파편화).
- **`accent-editorial` ↔ `signal-warning` 인접 배치 X** (의미 다름).
- **Purple / indigo brand X** (AI slop).
- **`transition-all duration-300` 무차별 X** — 필요 property 만 (`transition-colors` / `-transform`).
- **일러스트 / 추상 그래픽 / 3D 장식 X** — typography + 제품 UI only.
- **모바일 섹션 간격 < 48 X**.
- **gradient X** — 깊이는 shadow.

---

## 8. Asset Policy

### 자산 (5종)

| 자산 | 크기 | 용도 |
|------|------|------|
| `feedback-3pane-{light,dark}.png` | 2400×1500 @2x | Home Hero col-5 |
| `feedback-timeline-closeup.png` | 800×600 @2x | 피드백 클로즈업 |
| `interview-theater-preview.png` | 1920×1080 @2x | 극장 모드 미리보기 |
| `dashboard-headline.png` | 1600×1000 @2x | 대시보드 헤드라인 |
| `section-{pain-points,journey}.png` | 1200×800 @2x | Home 섹션 증빙 |

### 정책
- 저장: `frontend/public/mockups/`
- 형식: `{page}-{variant}-{theme}.{ext}`
- 해상도 @2x PNG / 애니메이션 5MB 이하 / `<picture>` + `srcset` 필수
- 캡처: Playwright `e2e/capture-mockups.ts` 자동 (Phase B/C 완료 시 재생성). 더미 fixture (`fixtures/mockup-interview.json`).
- 다크 / 라이트 별도 (`-light` / `-dark` suffix).
- 일러스트 / 추상 / 가상 UI **금지** — 실제 제품 UI 만.

---

## 9. Iteration Self-Check

기존 화면 리파인 시:
1. 영문 / 숫자 = Fraunces? 한글 = Pretendard?
2. 색상 = teal 팔레트? warm off-neutral / Cal Sans / pure `#000`·`#fff` 잔존 X?
3. 카드 elevation = multi-shadow stack? CSS `border` 대체?
4. 섹션 간격 80+? cramped 시 추가.
5. CTA = `bg-brand`? `bg-primary` 오용 X?
6. focus ring = `ring-ring` (= brand)?
7. 타이머 / 통계 = `font-tabular`?
