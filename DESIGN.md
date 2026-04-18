# Design System Inspired by Cal.com

## 1. Visual Theme & Atmosphere

Cal.com's website is a masterclass in monochromatic restraint — a grayscale world where boldness comes not from color but from the sheer confidence of black text on white space. Inspired by Uber's minimal aesthetic, the palette is deliberately stripped of hue: near-black headings (`#242424`), mid-gray secondary text (`#898989`), and pure white surfaces. Color is treated as a foreign substance — when it appears (a rare blue link, a green trust badge), it feels like a controlled accent in an otherwise black-and-white photograph.

Cal Sans, the brand's custom geometric display typeface designed by Mark Davis, is the visual centerpiece. Letters are intentionally spaced extremely close at large sizes, creating dense, architectural headlines that feel like they're carved into the page. At 64px and 48px, Cal Sans headings sit at weight 600 with a tight 1.10 line-height — confident, compressed, and immediately recognizable. For body text, the system switches to Inter, providing "rock-solid" readability that complements Cal Sans's display personality. The typography pairing creates a clear division: Cal Sans speaks, Inter explains.

The elevation system is notably sophisticated for a minimal site — 11 shadow definitions create a nuanced depth hierarchy using multi-layered shadows that combine ring borders (`0px 0px 0px 1px`), soft diffused shadows, and inset highlights. This shadow-first approach to depth (rather than border-first) gives surfaces a subtle three-dimensionality that feels modern and polished. Built on Framer with a border-radius scale from 2px to 9999px (pill), Cal.com balances geometric precision with soft, rounded interactive elements.

**Key Characteristics:**
- Purely grayscale brand palette — no brand colors, boldness through monochrome
- Cal Sans custom geometric display font with extremely tight default letter-spacing
- Multi-layered shadow system (11 definitions) with ring borders + diffused shadows + inset highlights
- Cal Sans for headings, Inter for body — clean typographic division
- Wide border-radius scale from 2px to 9999px (pill) — versatile rounding
- White canvas with near-black (#242424) text — maximum contrast, zero decoration
- Product screenshots as primary visual content — the scheduling UI sells itself
- Built on Framer platform

## 2. Color Palette & Roles

### Primary (Phase A 개정: warm off-neutral)

> **개정 이유**: Cal.com 모노크롬 철학은 유지하되, pure black/white 대신 warm off-tone으로 미세 조정. 눈의 피로 감소 + "조용한 온기" 방향성 반영.

- **Warm Off-Black** (`#14130f`, CSS: `--foreground`): Primary heading, button text — warm off-black (pure black 금지)
- **Warm Off-White** (`#fafaf7`, CSS: `--background`): Primary background — warm off-white (pure white 금지)
- **Midnight** (`#111111`): 필요 시 deep overlay용으로만 (50% opacity)

### Secondary & Accent
- **Link Blue** (`#0099ff`): In-text links with underline decoration — the only blue in the system, reserved strictly for hyperlinks
- **Focus Ring** (`#3b82f6` at 50% opacity): Keyboard focus indicator — accessibility-only, invisible in normal interaction

### Surface & Background
- **Warm Off-White** (`#fafaf7`, `--background`): Primary page background — warm tint, not pure white
- **Warm Off-White Alt** (`#f8f7f4`, `--foreground` dark mode): Dark mode foreground
- **Mid Gray** (`#737373`, `--muted-foreground`): Secondary text, descriptions and muted labels — 4.53:1 contrast on `#fafaf7` (AA 통과)

### Neutrals & Text
- **Warm Off-Black** (`#14130f`, `--foreground`): Headlines, buttons, primary UI text — 17.8:1 on `#fafaf7` (AAA)
- **Mid Gray** (`#737373`, `--muted-foreground`): Descriptions, secondary labels, muted content
- **Border Warm** (`--border`): Hairline separators — warm-tinted, not cold gray

### Semantic Color 4종 (Phase A 신규)

> Cal.com은 의도적으로 무채색이지만, Rehearse는 "면접 도구"로서 상태 신호가 필요하다. 4종을 엄격히 분리해 오염 없이 사용한다.

| 토큰 | 값 (light) | 값 (dark) | 용도 | 사용 제한 |
|------|-----------|-----------|------|-----------|
| `--accent-editorial` | `#a65131` (terracotta) | `#c57458` | 피드백 하이라이트, 챕터 숫자 마커 | hue 13°로 ochre(warning)와 ≥30° 이격 |
| `--signal-record` | `#c8322a` (warm red) | `#d46b5f` | 녹화 중 상태 dot + "REC" 라벨 | `interview-page` 전용. 다른 페이지 사용 금지 |
| `--signal-warning` | `#d4a017` (ochre) | `#e6b946` | 타임 카운트다운, 시간 초과 경고 | 타임 경고에만 |
| `--signal-success` | `#5a7a4a` (muted sage) | `#87a26f` | 저장 완료 toast | toast 1개에만. 페이지 수준 배경 사용 금지 |

**배경 tint 변수**: 각 signal에 `-bg` suffix 제공 (`--accent-editorial-bg`, `--signal-record-bg` 등). 라이트는 95% L pastel tint, 다크는 14–16% L shade로 명도 내려 `#14130f` 배경 위에서 과발광 방지.

**테마 기본값**: light-default, system-following (`prefers-color-scheme: dark`로 자동 진입). interview-page는 테마와 무관하게 `bg-interview-stage` 강제 (몰입 모드).

### Gradient System
- No gradients on the marketing site — the design is fully flat and monochrome
- Depth is achieved entirely through shadows, not color transitions

## 3. Typography Rules

### Font Family (2026-04-18 designer critique 반영 — 3-폰트 확정)

> **개정 이유**: 기존 "Pretendard + Cal Sans + Fraunces" 3-폰트 시스템은 Cal Sans와 Fraunces가 영문 display 역할을 놓고 경쟁하는 햇지 구조였다. 한글 지배 페이지에서 Fraunces가 `01` 숫자 장식으로만 남아 editorial voice가 발생하지 않았다. **Cal Sans를 제거하고 Fraunces를 영문 display 단일 폰트로 승격**해 "Quiet Rigor"의 타이포그래피 서명을 확보한다.

- **Sans (필수)**: `Pretendard Variable` / `Pretendard` — 한글·영문 UI 본문·헤딩. `font-sans` 클래스. 이미 CDN 로드 중
- **Serif (영문 display 단일)**: `Fraunces` — 영문 헤드라인, ChapterMarker 숫자(`01`, `02`), MetricsSection 초대형 숫자, 피드백 질문 제목 중 영문 부분. `font-serif` 클래스
- **Mono**: `JetBrains Mono` — 코드, 기술 스택 태그. `font-mono` 클래스
- **Display (하위 호환 alias)**: `font-display` 클래스는 Fraunces 스택으로 재포인트. 신규 사용 금지 — `font-serif` 사용
- **Tabular (유틸리티)**: `.font-tabular` CSS 클래스 — `font-feature-settings: 'tnum' 1`. 타이머·통계 숫자에 **필수** 적용

> **Cal Sans 제거 결정**: 실제 `frontend/src/**/*.tsx`에서 `font-display` / `Cal Sans` 참조 0건 확인. Cal Sans는 Cal.com 정체성이지 Rehearse 정체성이 아니며, 제거 시 마이그레이션 비용 실효 없음.

> **한글 위계 전략**: Fraunces는 한글 미지원. 한글 헤딩은 Pretendard 굵기(700→600→500) + 자간(-0.02em → 0) + 크기 차등만으로 위계 구성. Fraunces는 영문 단어·숫자가 등장하는 지점에만 노출해 폰트 믹스 파편화 회피.

### Hierarchy (Quiet Rigor — Pretendard + Fraunces + JetBrains Mono)

| Role | Font | Size | Weight | Line Height | Letter Spacing | Notes |
|------|------|------|--------|-------------|----------------|-------|
| `display-xl` | Fraunces (영문) / Pretendard (한글) | 64px | 700 | 1.05 | -0.02em | MetricsSection 초대형 숫자, 히어로 영문 |
| `display-lg` | Fraunces / Pretendard | 48px | 700 | 1.10 | -0.02em | ChapterMarker 질문 제목 |
| `h1` | Pretendard | 40px | 700 | 1.20 | -0.01em | 페이지 주요 타이틀 |
| `h2` | Pretendard | 28px | 700 | 1.30 | -0.01em | 섹션 헤딩 |
| `h3` | Pretendard | 20px | 600 | 1.40 | 0 | 서브 섹션 |
| `body-lg` | Pretendard | 18px | 400 | 1.65 | 0 | 피드백 본문 (scan-first) |
| `body` | Pretendard | 16px | 400 | 1.70 | 0 | 일반 본문 |
| `body-sm` | Pretendard | 14px | 400 | 1.60 | 0 | 보조 설명 |
| `caption` | Pretendard | 12px | 500 | 1.50 | +0.02em | 레이블, 메타 정보 |
| `over-line` | Pretendard | 11px | 600 | 1.50 | +0.10em | ChapterMarker 숫자 caption, UtilityBar 챕터 표시 |
| `mono` | JetBrains Mono | 13px | 400 | 1.60 | 0 | 코드, 기술 스택 태그 |
| `numeric-display` | Fraunces | any | 700 | 1.05 | -0.02em | 숫자 단독 표기 (`01`, `42min`) |
| `tabular` | Pretendard | any | — | — | `font-feature-settings: 'tnum'` | 타이머·통계 숫자 **필수** |

### Principles (2026-04-18 개정)
- **Fraunces는 영문·숫자 전용**: 한글에 Fraunces 강제 시 폰트 믹스 파편화 발생. 영문 단어·숫자가 독립적으로 나타나는 지점에만 노출
- **Pretendard 굵기 변주가 한글 위계의 전부**: 700(주 헤딩) → 600(서브) → 500(캡션) → 400(본문). 자간 -0.02em ~ 0 범위에서 크기에 비례
- **숫자는 항상 tabular**: 타이머·통계는 `font-tabular` 클래스 필수, 시각 정렬 유지
- **scan-first 피드백**: `body-lg`는 1.65 line-height (독서가 아닌 스캔)
- **over-line(11px) > display 숫자 장식**: ChapterMarker는 질문 제목이 주인공, 숫자는 참조 레이블

## 4. Component Stylings

### Buttons
- **Dark Primary**: `#242424` (or `#1e1f23`) background, white text, 6–8px radius. Hover: opacity reduction to 0.7. The signature CTA — maximally dark on white
- **White/Ghost**: White background with shadow-ring border, dark text. Uses the multi-layered shadow system for subtle elevation
- **Pill**: 9999px radius for rounded pill-shaped actions and badges
- **Compact**: 4px padding, small text — utility actions within product UI
- **Inset highlight**: Some buttons feature `rgba(255, 255, 255, 0.15) 0px 2px 0px inset` — a subtle inner-top highlight creating a 3D pressed effect

### Cards & Containers
- **Shadow Card**: White background, multi-layered shadow — `rgba(19, 19, 22, 0.7) 0px 1px 5px -4px, rgba(34, 42, 53, 0.08) 0px 0px 0px 1px, rgba(34, 42, 53, 0.05) 0px 4px 8px 0px`. The ring shadow (0px 0px 0px 1px) acts as a shadow-border
- **Product UI Cards**: Screenshots of the scheduling interface displayed in card containers with shadow elevation
- **Radius**: 8px for standard cards, 12px for larger containers, 16px for prominent sections
- **Hover**: Likely subtle shadow deepening or scale transform

### Inputs & Forms
- **Select dropdown**: White background, `#000000` text, 1px solid `rgb(118, 118, 118)` border
- **Focus**: Uses Framer's focus outline system (`--framer-focus-outline`)
- **Text input**: 8px radius, standard border treatment
- **Minimal form presence**: The marketing site prioritizes CTA buttons over complex forms

### Navigation
- **Top nav**: White/transparent background, Cal Sans links at near-black
- **Nav text**: `#111111` (Midnight) for primary links, `#000000` for emphasis
- **CTA button**: Dark Primary in the nav — high contrast call-to-action
- **Mobile**: Collapses to hamburger with simplified navigation
- **Sticky**: Fixed on scroll

### Image Treatment
- **Product screenshots**: Large scheduling UI screenshots — the product is the primary visual
- **Trust logos**: Grayscale company logos in a horizontal trust bar
- **Aspect ratios**: Wide landscape for product UI screenshots
- **No decorative imagery**: No illustrations, photos, or abstract graphics — pure product + typography

## 5. Layout Principles

### Spacing System
- **Base unit**: 8px
- **Scale**: 1px, 2px, 3px, 4px, 6px, 8px, 12px, 16px, 20px, 24px, 28px, 80px, 96px
- **Section padding**: 80px–96px vertical between major sections (generous)
- **Card padding**: 12px–24px internal
- **Component gaps**: 4px–8px between related elements
- **Notable jump**: From 28px to 80px — a deliberate gap emphasizing the section-level spacing tier

### Grid & Container
- **Max width**: ~1200px content container, centered
- **Column patterns**: Full-width hero, centered text blocks, 2-3 column feature grids
- **Feature showcase**: Product screenshots flanked by description text
- **Breakpoints**: 98px, 640px, 768px, 810px, 1024px, 1199px — Framer-generated

### Whitespace Philosophy
- **Lavish section spacing**: 80px–96px between sections creates a breathable, premium feel
- **Product-first content**: Screenshots dominate the visual space — minimal surrounding decoration
- **Centered headlines**: Cal Sans headings centered with generous margins above and below

### Border Radius Scale
- **2px**: Subtle rounding on inline elements
- **4px**: Small UI components
- **6px–7px**: Buttons, small cards, images
- **8px**: Standard interactive elements — buttons, inputs, images
- **12px**: Medium containers — links, larger cards, images
- **16px**: Large section containers
- **29px**: Special rounded elements
- **100px**: Large rounding — nearly circular on small elements
- **1000px**: Very large rounding
- **9999px**: Full pill shape — badges, links

## 6. Depth & Elevation

| Level | Treatment | Use |
|-------|-----------|-----|
| Level 0 (Flat) | No shadow | Page canvas, basic text containers |
| Level 1 (Inset) | `rgba(0,0,0,0.16) 0px 1px 1.9px 0px inset` | Pressed/recessed elements, input wells |
| Level 2 (Ring + Soft) | `rgba(19,19,22,0.7) 0px 1px 5px -4px, rgba(34,42,53,0.08) 0px 0px 0px 1px, rgba(34,42,53,0.05) 0px 4px 8px` | Cards, containers — the workhorse shadow |
| Level 3 (Ring + Soft Alt) | `rgba(36,36,36,0.7) 0px 1px 5px -4px, rgba(36,36,36,0.05) 0px 4px 8px` | Alt card elevation without ring border |
| Level 4 (Inset Highlight) | `rgba(255,255,255,0.15) 0px 2px 0px inset` or `rgb(255,255,255) 0px 2px 0px inset` | Button inner highlight — 3D pressed effect |
| Level 5 (Soft Only) | `rgba(34,42,53,0.05) 0px 4px 8px` | Subtle ambient shadow |

### Shadow Philosophy
Cal.com's shadow system is the most sophisticated element of the design — 11 shadow definitions using a multi-layered compositing technique:
- **Ring borders**: `0px 0px 0px 1px` shadows act as borders, avoiding CSS `border` entirely. This creates hairline containment without affecting layout
- **Diffused soft shadows**: `0px 4px 8px` at 5% opacity add gentle ambient depth
- **Sharp contact shadows**: `0px 1px 5px -4px` at 70% opacity create tight bottom-edge shadows for grounding
- **Inset highlights**: White inset shadows at the top of buttons create a subtle 3D bevel
- Shadows are composed in comma-separated stacks — each surface gets 2-3 layered shadow definitions working together

### Decorative Depth
- No gradients or glow effects
- All depth comes from the sophisticated shadow compositing system
- The overall effect is subtle but precise — surfaces feel like physical cards sitting on a table

## 7. Do's and Don'ts

### Do
- Use **Fraunces for English display and all numerals** — ChapterMarker 숫자, MetricsSection 대형 수치, 영문 헤드라인. 숫자 `subset=0123456789` 로드로 비용 최소화
- Use **Pretendard for all Korean content** (headings + body) — 굵기 변주(700→600→500→400)로 위계 구성
- Maintain warm off-neutral palette (`#fafaf7` / `#14130f`) — no pure black/white
- Use the 5-step shadow system (`shadow-xs` ~ `shadow-lg`) — ring border + diffused shadow 조합 유지
- Apply `font-tabular` to all timers and statistical numerals — visual alignment 필수
- Let product screenshots be the visual content — no illustrations, no decorative graphics
- Apply generous section spacing (80px–96px) — breathing room is essential to Quiet Rigor

### Don't
- **Do NOT re-introduce Cal Sans** — removed 2026-04-18 (designer critique). Fraunces is the sole English display font
- **Do NOT use `backdrop-blur` on sticky headers** — anti-slop rule (`frontend-design-rules.md`). Use opaque `bg-background` + hairline border
- **Do NOT force Fraunces on Korean text** — font mix 파편화 발생. 한글은 Pretendard 전담
- **Do NOT place `--accent-editorial` near `--signal-warning`** as if they're the same token — terracotta vs ochre는 색상환 ≥30° 이격이지만 의미(하이라이트 vs 경고)가 다르므로 용도로만 구분
- Add purple/indigo brand colors — AI slop, `frontend-design-rules.md` 명시 금지
- Apply `transition-all duration-300` 무차별 — `transition-colors` / `transition-transform` 등 필요 property만 명시
- Use illustrations, abstract graphics, or decorative 3D elements — typography + product UI only
- Reduce section spacing below 48px on mobile — generous whitespace is core to the aesthetic

## 8. Responsive Behavior

### Breakpoints
| Name | Width | Key Changes |
|------|-------|-------------|
| Mobile | <640px | Single column, hero text ~36px, stacked features, hamburger nav |
| Tablet Small | 640px–768px | 2-column begins for some elements |
| Tablet | 768px–810px | Layout adjustments, fuller grid |
| Tablet Large | 810px–1024px | Multi-column feature grids |
| Desktop | 1024px–1199px | Full layout, expanded navigation |
| Large Desktop | >1199px | Max-width container, centered content |

### Touch Targets
- Buttons: 8px radius with comfortable padding (10px+ vertical)
- Nav links: Dark text with adequate spacing
- Mobile CTAs: Full-width dark buttons for easy thumb access
- Pill badges: 9999px radius creates large, tappable targets

### Collapsing Strategy
- **Navigation**: Full horizontal nav → hamburger on mobile
- **Hero**: 64px Cal Sans display → ~36px on mobile
- **Feature grids**: Multi-column → 2-column → single stacked column
- **Product screenshots**: Scale within containers, maintaining aspect ratios
- **Section spacing**: Reduces from 80px–96px to ~48px on mobile

### Image Behavior
- Product screenshots scale responsively
- Trust logos reflow to multi-row grid on mobile
- No art direction changes — same compositions at all sizes
- Images use 7px–12px border-radius for consistent rounded corners

## 9. Agent Prompt Guide

### Quick Color Reference
- Primary Text: Charcoal (`#242424`)
- Deep Text: Midnight (`#111111`)
- Secondary Text: Mid Gray (`#898989`)
- Background: Pure White (`#ffffff`)
- Link: Link Blue (`#0099ff`)
- CTA Button: Charcoal (`#242424`) bg, white text
- Shadow Border: `rgba(34, 42, 53, 0.08)` ring

### Example Component Prompts
- "Create a hero section with white background, 64px Cal Sans heading at weight 600, line-height 1.10, #242424 text, centered layout with a dark CTA button (#242424, 8px radius, white text)"
- "Design a scheduling card with white background, multi-layered shadow (0px 1px 5px -4px rgba(19,19,22,0.7), 0px 0px 0px 1px rgba(34,42,53,0.08), 0px 4px 8px rgba(34,42,53,0.05)), 12px radius"
- "Build a navigation bar with white background, Inter links at 14px weight 500 in #111111, a dark CTA button (#242424), sticky positioning"
- "Create a trust bar with grayscale company logos, horizontally centered, 16px gap between logos, on white background"
- "Design a feature section with 48px Cal Sans heading (weight 600, #242424), 16px Inter body text (weight 300, #898989, line-height 1.50), and a product screenshot with 12px radius and the card shadow"

### Iteration Guide
When refining existing screens generated with this design system:
1. Verify headings use Cal Sans at weight 600, body uses Pretendard — never mix them
2. Check that the palette is warm off-neutral — if you see pure #000/#fff, replace with `--foreground`/`--background`
3. Ensure card elevation uses the multi-layered shadow stack, not CSS borders
4. Confirm section spacing is generous (80px+) — if sections feel cramped, add more space
5. The overall tone should feel like a quiet, rigorous practice room — warm monochrome with editorial detail

---

## 10. Layout System (Phase A 신규)

> 기존 Cal.com 중앙 정렬(max-w-* mx-auto) 패턴에서 벗어나 12-col 비대칭 그리드로 전환한다. 각 페이지가 다른 컬럼 점유로 시각 비율을 차별화한다.

### 12-column Asymmetric Grid

- **전역 컨테이너**: `grid grid-cols-12 gap-x-6 max-w-[1440px] mx-auto px-4 md:px-8 lg:px-12`
- **본문 anchor**: 좌측 anchor 원칙 — 중앙 정렬 금지
- **반응형**: `sm: 4-col` / `md: 8-col` / `lg+: 12-col`

### Page Layout Recipes (4종)

| Recipe | 비율 | 적용 페이지 | 설명 |
|--------|------|-------------|------|
| **A: 7+5** | col-7 + col-5 | Home Hero | 좌 anchor copy + 우 제품 스크린샷 |
| **B: 4+8** | col-4 sticky + col-8 | Setup | 좌 sticky 목차 + 우 스텝 편집 영역 |
| **C: 2+6+4** | col-2 + col-6 + col-4 | Feedback | StickyOutline + ReadingColumn + VideoDock |
| **D: 8+4** | col-8 + col-4 | Dashboard | 메인 콘텐츠 + 활동 타임라인 |

### Elevation Levels (L1–L6 원칙)

| Level | 원칙 | 적용 |
|-------|------|------|
| L1 | `max-w-* mx-auto` 중앙 정렬 제거 → PageGrid 교체 | 모든 Priority 1 페이지 |
| L2 | 12-col Recipe 적용 (A/B/C/D 중 택일) | 페이지별 |
| L3 | 박스형 섹션 카드 → ReadingColumn + ChapterMarker | Feedback/Setup |
| L4 | SaaS 표준 헤더 → UtilityBar (44px/56px) | 전 페이지 |
| L5 | `space-y-6` 균등 → 챕터 96px / 단락 24px 차등 | 본문 영역 |
| L6 | 고정 2분할 → Structural Primitive 조합 | Feedback/Setup/Interview |

### Structural Primitives (Phase B 도입 — 신규 6개)

저장 경로: `frontend/src/components/layout/`

| Primitive | 역할 | Phase |
|-----------|------|-------|
| `<PageGrid>` | 12-col 전역 wrapper | B |
| `<ReadingColumn>` | 피드백 본문 컨테이너 (`max-w-[55ch]`, `leading-1.65`) | B |
| `<StickyOutline>` | 질문 목차 compound (Desktop/TabBar/MobileSheet) | B |
| `<StickyRail>` | col 배치 + sticky top 순수 layout primitive | B |
| `<VideoDock>` | StickyRail + VideoPlayer + TimelineBar composite | B |
| `<ChapterMarker>` | 섹션 구분 — 숫자 over-line(11px) + 질문 제목(40–48px) | B |
| `<UtilityBar>` | 전 페이지 헤더 대체 (h-11 / md:h-14) | B |

---

## 11. Responsive Strategy (Phase A 신규)

### Breakpoint 매트릭스

| Breakpoint | 범위 | 레이아웃 | 주요 변화 |
|------------|------|----------|-----------|
| `sm` | < 768px | 단일 컬럼 | VideoDock sticky-top 25vh, StickyOutline → MobileSheet |
| `md` | 768–1023px | 단일 컬럼 | VideoDock sticky-top 30vh, StickyOutline → MobileSheet |
| `lg` | 1024–1279px | 2-pane | StickyOutline → TabBar, col-8 + col-4 |
| `xl` | ≥ 1280px | 3-pane | StickyOutline → Desktop, col-2 + col-6 + col-4 |

### Primitive별 Fallback 원칙

- `<StickyOutline.Desktop>`: `hidden xl:flex` — xl 미만 CSS 숨김
- `<StickyOutline.TabBar>`: `flex xl:hidden` — xl 이상 CSS 숨김
- `<StickyOutline.MobileSheet>`: trigger `lg:hidden`, Portal은 CSS 제어 불가 (JS state)
- `<PageGrid>`: `grid-cols-4 md:grid-cols-8 lg:grid-cols-12` 순차 확장
- Recipe B(4+8): `lg+ 분할`, `sm/md 단일 컬럼 + 상단 tab bar`

### 터치 타겟 규칙 (WCAG 2.5.5)

- 모든 인터랙티브 요소 hitbox 최소 **44×44px**
- `--utility-bar-height: 44px` (데스크탑) / `56px` (모바일 `max-width: 767px`)
- UtilityBar 내 아이콘 버튼: `min-w-11 min-h-11` 보장
- Interview 종료·일시중지 버튼: **항상 visible** — hover에 숨기지 않음
- 모바일 하단 고정 bar: `h-14` (56px), 버튼 전체 높이 활용

### 섹션 간격 축소 규칙

- 데스크탑: 섹션 간 80–96px (`py-20 md:py-24`)
- 모바일 (`sm`): 48px로 축소 (`py-12`)
- 카드 내부 패딩: 12–24px 유지

---

## 12. Asset Policy (Phase A 신규)

### 제품 스크린샷 정책

Rehearse 마케팅 및 UI에서 사용할 제품 목업 이미지는 아래 정책을 따른다.

### 필요 자산 5종

| 자산명 | 크기 | 용도 |
|--------|------|------|
| `feedback-3pane-light.png` / `feedback-3pane-dark.png` | 2400×1500 @2x | Home Hero 우 5-col 제품 목업 |
| `feedback-timeline-closeup.png` | 800×600 @2x | 피드백 UI 시연 클로즈업 |
| `interview-theater-preview.png` | 1920×1080 @2x | 극장식 몰입 모드 면접 화면 |
| `dashboard-headline.png` | 1600×1000 @2x | 대시보드 초대형 숫자 헤드라인 |
| `section-pain-points.png` / `section-journey.png` | 1200×800 @2x 각 | Home 섹션 기능 증빙 |

### 제작 방법

**옵션 A (Playwright 자동 캡처) — 권장**: Phase B/C 완료 시마다 `e2e/capture-mockups.ts`로 자동 재생성. 목업 데이터는 `fixtures/mockup-interview.json` (실명 없는 더미).

**옵션 B (초기 임시)**: Phase B 완료 전 Home 랜딩 작업 시 Figma 임시 목업 사용 가능. Phase B 완료 후 옵션 A로 교체 필수.

### 저장소 및 파일명 규칙

```
frontend/public/mockups/
  feedback-3pane-light.png
  feedback-3pane-dark.png
  feedback-timeline-closeup.png
  interview-theater-preview.png
  dashboard-headline.png
  section-pain-points.png
  section-journey.png
```

- 파일명 형식: `{page}-{variant}-{theme}.{ext}`
- 해상도: @2x PNG 기본, 애니메이션(GIF/WebM) 5MB 이하
- 지원하지 않는 브라우저 대비 `<picture>` + `srcset` 필수

### srcset 템플릿

```html
<picture>
  <source media="(min-width: 1280px)" srcset="/mockups/feedback-3pane-light.png 2x">
  <img
    src="/mockups/feedback-3pane-light.png"
    alt="Rehearse 피드백 3-pane 화면"
    loading="lazy"
    decoding="async"
  >
</picture>
```

### 이미지 철학 (Cal.com 계승)

- 제품 스크린샷이 주요 시각 콘텐츠 — 일러스트, 추상 그래픽, 장식 이미지 금지
- 모든 이미지는 실제 제품 UI (더미 데이터 허용, 가상 UI 불가)
- 다크/라이트 테마별 별도 캡처 제공 (`-light` / `-dark` suffix)
