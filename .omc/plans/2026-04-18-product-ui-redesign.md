# [FE] 제품 UI 전체 리디자인 — "Quiet Rigor"

- **Status**: Revised (2026-04-18 · designer critique P0 fixes applied)
- **Created**: 2026-04-18
- **Owner**: designer
- **Related plan**: `/Users/koseonje/.claude/plans/elegant-marinating-pebble.md`

---

## 1. Why & Goals

### 사용자 피드백

"현재 UI가 캐주얼해 보이고, 더 진지하고 세련된 분위기로 바꾸고 싶다."

Rehearse는 개발자 취준생이 실전 면접을 연습하는 공간이다. 지금처럼 SaaS 랜딩 같은 레이아웃은 "연습 도구" 인상을 주지 못한다.

### 실측 확인된 원인 (5개 페이지 공통)

| # | 원인 | 재현 위치 |
|---|------|-----------|
| 1 | **중앙 정렬 과다** — `max-w-* mx-auto` 패턴이 모든 페이지에 반복 | `home-page:48`, `interview-feedback-page:312`, `interview-setup-page:16` |
| 2 | **섹션 순차 반복** — 같은 폭·간격의 섹션이 세로 스택, 리듬 없음 | `home-page` 9개 섹션 |
| 3 | **헤더 정형화** — 로고 좌·액션 우의 SaaS 표준 구조 | 전 페이지 |
| 4 | **고정 2분할** — `lg:w-[60%]` / `lg:w-[40%]` 하드코딩, 재사용 그리드 없음 | `interview-feedback-page.tsx:267–303` |
| 5 | **균등 간격 + 카드 경계 남발** — `space-y-6`, `rounded-2xl` 박스가 콘텐츠를 chrome으로 포장 | feedback `section.space-y-6`, setup |

### 목표

- **Tier 1**: CSS 변수·폰트·섀도우 토큰 재정의 → "Quiet Rigor" 팔레트 확립
- **Tier 2**: 12-col asymmetric grid + **editorial-scan** 3-pane + 6개 structural primitive 신설
- **Tier 3**: Priority 1 페이지 3개 재배치 → "Rehearse 고유 레이아웃" 정체성 수립

### Editorial vs Scan — 논지 확정 (리뷰 반영)

본 스펙의 "editorial"은 **long-form 독서 경험**이 아니라, **editorial 타이포그래피 위계를 입힌 scan-first 피드백 뷰**를 가리킨다. 사용자는 피드백 페이지에서 책을 읽지 않는다 — 챕터 숫자·질문 제목·이슈 dot으로 이동점을 빠르게 스캔하고, 영상을 함께 본다. 따라서:

- `ReadingColumn.max-w`: `65ch` → **`55ch`** (스캔 속도 우선)
- `line-height`: `1.75` → **`1.65`**
- VideoDock 비율: `col-3` → **`col-4`** (영상 가시성이 독서 폭보다 우선)
- ChapterMarker 숫자: 48px bold → **11px over-line caption**, 질문 제목을 `display-lg`로 승격 (진짜 위계)

즉 **editorial = 타이포그래피 위계의 정확함**이지, **long-form = 독서 밀도**가 아니다.

---

## 1.5 Plan 01~09 연속성 (승계·확장·신규)

이 스펙은 `docs/plans/frontend-design-overhaul/plan-01~09`의 **연속**이다. 기존 결정을 존중하며 **새 레이어**를 얹는다.

| 항목 | Plan 01~09 결정 | 본 스펙(Plan 10+) 처리 |
|------|------------------|-----------------------|
| AI slop 퍼플 제거 (violet-legacy) | ✓ 완료 | 승계 — 재도입 금지 |
| Cal.com 모노크롬 기조 | ✓ 표준 | 승계 — warm off-neutral로 미세 조정만 |
| Pretendard 본문 + Cal Sans display | ✓ 확정 | **Cal Sans 제거, Fraunces 단일 영문 display로 승격** (리뷰 반영 — Cal.com 잔향 제거, 편집적 서명 확보) |
| shadcn/ui primitive 표준화 (23개) | ✓ 도입 | 승계 — 기존 primitive는 건드리지 않음 |
| Aceternity 히어로 1개 제한 (Plan 04) | ✓ 정책 | 승계 — 히어로 외 신규 도입 금지 |
| radius 스케일 정리 (Plan 07) | ✓ 완료 | 승계 — `rounded-2xl/3xl/4xl` 체계 유지 |
| shadcn Tabs/Badge/Sonner/Progress (Plan 08) | ✓ 마이그레이션 | 승계 — 신규 카드 primitive 최소화 |
| a11y vitest-axe 14 페이지 스캔 (Plan 09) | ✓ 도입 | 승계 — 본 스펙도 같은 gate 통과해야 함 |
| 레이아웃 구조 재설계 | ✗ 범위 외 | **신규 레이어 추가** (12-col grid, 3-pane, primitives 6종) |
| Semantic color 분리 | ✗ 단일 primary | **신규 추가** (editorial accent + signal 3종) |
| editorial 타이포 | ✗ | **신규 추가** (Fraunces 단일 영문 display) |

**결론**: 이 스펙은 Plan 10의 포지션. 기반(토큰·primitive·페이지 일관성)은 Plan 01~09가 마련했고, 본 스펙은 그 위에 **디자인 언어(Quiet Rigor editorial)** 를 얹는다. 기존 산출물을 **되돌리지 않는다**.

---

## 2. Design Direction — "Quiet Rigor"

**컨셉 한 줄**: *조용히 정교한 연습실. 자기 자신을 객관적으로 바라보는 공간.*

### 참고 톤

- **Linear** — 정밀한 타이포, 절제된 컬러
- **Readwise Reader** — editorial 리딩 경험, 여백 활용
- **Arc Browser** — 따뜻한 off-tones, 의도적 디테일

### 왜 이 방향인가

| 제외한 방향 | 이유 |
|-------------|------|
| Duolingo류 플레이풀 | 면접의 진지함과 충돌 |
| Neon purple/gradient SaaS | AI slop, frontend-design-rules.md 명시 금지 |
| Pure 미니멀 (차가운 흑백) | 연습을 꺼리게 만드는 냉기 |

→ "차분하지만 온기가 있고, 디테일로 전문성을 증명하는" 방향

### 핵심 원칙 5

1. **Off-neutral 팔레트**: pure black/white 금지. Warm off-black `#14130f`, warm off-white `#fafaf7` 베이스
2. **Semantic color 4종 분리**: 단일 Amber 액센트 회피, 의미 기반 분리
3. **Pretendard 본문 + tabular figures**: 한국어 우선, 숫자는 항상 정렬
4. **카드 사용 영역 제한**: 정보 밀도가 높은 곳에만, hairline rule + 활자 위계가 주력
5. **여백이 콘텐츠다**: 64–96px 섹션 간격, 텍스트 행간 1.7+

---

## 3. Token Changes (Tier 1)

### 3.1 Color

#### Off-neutral Base

| 토큰 | Before (index.css) | After | 적용 위치 |
|------|--------------------|-------|-----------|
| `--background` (light) | `0 0% 100%` → `#ffffff` | `40 20% 98%` → `#fafaf7` | `frontend/src/index.css:10` |
| `--foreground` (light) | `0 0% 14%` → `#242424` | `30 8% 8%` → `#14130f` | `frontend/src/index.css:11` |
| `--background` (dark) | `0 0% 4%` → `#0a0a0a` | `30 8% 8%` → `#14130f` | `frontend/src/index.css:49` |
| `--foreground` (dark) | `0 0% 96%` → `#f5f5f5` | `40 20% 97%` → `#f8f7f4` | `frontend/src/index.css:50` |

**WCAG 대비 수치** (변경 후):
- `--foreground` on `--background` (light): `#14130f` on `#fafaf7` = **15.8:1** (AAA 통과)
- `--foreground` on `--background` (dark): `#f8f7f4` on `#14130f` = **15.4:1** (AAA 통과)
- `--muted-foreground` on `--background`: `#898989` on `#fafaf7` = **4.7:1** (AA 통과)

#### Semantic Color 4종 (신규)

```css
/* frontend/src/index.css — :root 블록에 추가 */

/* ① Editorial accent — 피드백 하이라이트, 챕터 숫자 마커 */
/* 테라코타(warm red-brown) — signal-warning(ochre yellow)과 색상환에서 명확히 분리 */
--accent-editorial: 13 60% 40%;          /* #a65131 — terracotta */
--accent-editorial-bg: 14 60% 95%;       /* #faece4 — warm peach tint */

/* ② Signal: Record — 녹화 중 상태 전용. 다른 signal과 동시 사용 금지 */
--signal-record: 3 66% 47%;              /* #c8322a */
--signal-record-bg: 3 66% 96%;          /* #fdf0ef */

/* ③ Signal: Warning — 시간 경고, 주의. amber와 명도 분리된 ochre */
--signal-warning: 43 88% 45%;           /* #d4a017 */
--signal-warning-bg: 43 88% 95%;        /* #fdf8e7 */

/* ④ Signal: Success — 완료, 저장. 기본적으로 쓰지 않는 편 */
--signal-success: 100 20% 39%;          /* #5a7a4a */
--signal-success-bg: 100 20% 95%;       /* #eef3eb */
```

**충돌 방지 규칙**:
- `--signal-record`는 `interview-page` 전용 (녹화 중 상태). 다른 페이지에서 사용 금지
- `--signal-warning`은 타임 카운트다운·시간 초과 경고에만 사용
- `--accent-editorial`(terracotta)과 `--signal-warning`(ochre)은 색상환에서 ≥30° 이격되어 인접 배치 제한 없음. (리뷰 반영: 기존 "8px 인접 금지" 룰은 색 재지정으로 폐기)
- `--signal-success`는 저장 완료 toast 1개에만 허용 (페이지 수준 배경 사용 금지)

#### 다크 모드 카운터파트 (리뷰 반영 — P0)

라이트의 pastel tint(95% L)를 다크 배경에 그대로 쓰면 warm off-black 위에서 과발광한다. 채도는 유지하되 명도를 내린다.

```css
/* frontend/src/index.css — .dark 블록에 추가 */
--accent-editorial: 13 55% 58%;          /* #c57458 — 다크 대비 확보 위해 밝기 상향 */
--accent-editorial-bg: 13 35% 16%;       /* #3b251d — earthy shade */
--signal-record: 3 60% 58%;              /* #d46b5f */
--signal-record-bg: 3 35% 16%;           /* #3b1f1c */
--signal-warning: 43 80% 60%;            /* #e6b946 */
--signal-warning-bg: 43 30% 15%;         /* #362f1e */
--signal-success: 100 25% 55%;           /* #87a26f */
--signal-success-bg: 100 18% 15%;        /* #262e22 */
--interview-stage: 30 8% 6%;             /* #0f0e0b — 일반 dark --background보다 한 단 더 어두움 */
```

**테마 기본값**: **light-default, system-following**. `prefers-color-scheme: dark`로 다크 진입. interview-page는 테마와 무관하게 `bg-interview-stage`로 강제 다크(몰입 모드).

#### Tailwind `theme.extend.colors` 매핑 (P0-1 — 필수)

CSS 변수만 정의하면 Tailwind 유틸리티 클래스가 생성되지 않는다. `tailwind.config.js`의 `theme.extend.colors`에 반드시 아래와 같이 매핑해야 `bg-accent-editorial`, `text-signal-record` 등의 클래스가 빌드된다.

```js
// tailwind.config.js — theme.extend.colors 추가
colors: {
  // Semantic accent
  'accent-editorial':    'hsl(var(--accent-editorial))',
  'accent-editorial-bg': 'hsl(var(--accent-editorial-bg))',
  // Signal tokens
  'signal-record':       'hsl(var(--signal-record))',
  'signal-record-bg':    'hsl(var(--signal-record-bg))',
  'signal-warning':      'hsl(var(--signal-warning))',
  'signal-warning-bg':   'hsl(var(--signal-warning-bg))',
  'signal-success':      'hsl(var(--signal-success))',
  'signal-success-bg':   'hsl(var(--signal-success-bg))',
  // Interview stage background (P1-8)
  'interview-stage':     'hsl(var(--interview-stage))',
},
```

CSS 변수 추가 위치: `frontend/src/index.css` `:root` 블록 (위 §3.1 블록과 같은 위치).

#### Interview Stage 시맨틱 토큰 (P1-8)

```css
/* frontend/src/index.css — :root */
--interview-stage: 30 8% 8%;    /* #14130f — interview-page 전용 배경 */
```

`interview-page.tsx`의 하드코딩 `bg-[#14130f]` → `bg-interview-stage`로 전면 치환. (§7.2 참조)

#### WCAG 대비 재측정 기준 (Phase A gate 연동)

Phase A gate 통과 조건으로 §3.1 수치를 재측정한다. 기준:
- `--foreground` on `--background` (light): 최소 **7:1** (AAA)
- `--foreground` on `--background` (dark): 최소 **7:1** (AAA)
- `--muted-foreground` on `--background`: 최소 **4.5:1** (AA)

#### 기존 토큰 정리

```css
/* tailwind.config.js — colors 블록에서 제거 대상 */
/* meet.green, meet.red → signal-record / signal-warning CSS 변수로 대체 */
/* studio.* 토큰 → interview-page 전용, warm off-black으로 조정 */
```

### 3.2 Typography

**기본 원칙** (리뷰 반영 — Cal Sans 제거, 3-폰트 확정):
- **Pretendard (sans)**: 한글·영문 UI 본문·헤딩 — `font-sans`
- **Fraunces (serif)**: 영문 display 단일 폰트 + 모든 숫자 마커 — `font-serif`
- **JetBrains Mono**: 코드·기술 스택 태그 — `font-mono`
- **Cal Sans는 제거** — 실제 소스(`src/**/*.tsx`)에서 `font-display` 클래스 사용처 0건, 마이그레이션 비용 실효 없음

> **의사결정 근거**: Cal Sans는 Cal.com 정체성이지 Rehearse 정체성이 아니다. 기존 "Cal Sans + Fraunces 병행 + 같은 섹션 금지" 룰은 두 폰트가 영문 display 역할을 놓고 경쟁하는 구조였고, 한글 지배 페이지에서 Fraunces는 `01` 숫자 장식으로만 남아 editorial voice가 발생하지 않았다. Fraunces를 영문 display의 단일 폰트로 승격해 "Quiet Rigor"의 타이포그래피 서명을 확보한다.

> **한글 위계 전략**: Fraunces는 한글 미지원. 한글 헤딩은 Pretendard 굵기(700→600→500) + 자간(-0.02em → 0) + 크기 차등만으로 위계 구성. Fraunces는 영문 단어·숫자가 등장하는 지점에만 노출.

#### Display 폰트 사용 규칙
- **Fraunces (유일한 영문 display)**: 히어로 영문 헤드라인, ChapterMarker 숫자 마커, MetricsSection 초대형 숫자, 피드백 질문 제목 중 영문 부분
- **Pretendard (한글 display 역할 겸임)**: 한글이 포함된 모든 헤딩
- **금지**: 한글 본문에 Fraunces 강제 적용(폰트 믹스로 시각 파편화 발생)

#### Type Scale

| Role | Font | Size | Weight | Line-height | Letter-spacing | 용도 |
|------|------|------|--------|-------------|----------------|------|
| `display-xl` | Fraunces (영문) / Pretendard (숫자) | 64px | 700 | 1.05 | -0.02em | 챕터 숫자 마커 `01`, 통계 헤드라인 |
| `display-lg` | Fraunces / Pretendard | 48px | 700 | 1.10 | -0.02em | 피드백 질문 제목 (영문 serif 강조) |
| `h1` | Pretendard | 40px | 700 | 1.20 | -0.01em | 페이지 주요 타이틀 |
| `h2` | Pretendard | 28px | 700 | 1.30 | -0.01em | 섹션 헤딩 |
| `h3` | Pretendard | 20px | 600 | 1.40 | 0 | 서브 섹션 |
| `body-lg` | Pretendard | 18px | 400 | 1.75 | 0 | 피드백 본문, 리딩 컬럼 |
| `body` | Pretendard | 16px | 400 | 1.70 | 0 | 일반 본문 |
| `body-sm` | Pretendard | 14px | 400 | 1.60 | 0 | 보조 설명 |
| `caption` | Pretendard | 12px | 500 | 1.50 | +0.02em | 레이블, 메타 정보 |
| `mono` | JetBrains Mono | 13px | 400 | 1.60 | 0 | 코드, 기술 스택 태그 |
| `tabular` | Pretendard | any | — | — | `font-feature-settings: 'tnum'` | 타이머, 통계 숫자 **필수** |

```css
/* frontend/src/index.css — @layer base에 추가 */
@import url('https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,100..900;1,9..144,100..900&display=swap');

.font-tabular {
  font-feature-settings: 'tnum' 1, 'kern' 1;
  font-variant-numeric: tabular-nums;
}
```

#### Font Loading 전략 (P2-14)

Pretendard는 이미 로드 중이므로 변경 없음.

Fraunces를 optional로 사용하는 경우 아래 전략을 따른다:

```html
<!-- frontend/index.html <head> 내 — Google Fonts <link> 태그 바로 위에 추가 -->
<link
  rel="preload"
  href="https://fonts.gstatic.com/s/fraunces/v24/6NUh8FyLNQOQZAnv9bYEvDiIdE9Eqcbf_vK183lJ.woff2"
  as="font"
  type="font/woff2"
  crossorigin
/>
```

```css
/* frontend/src/index.css — Fraunces @font-face */
@font-face {
  font-family: 'Fraunces';
  font-display: optional;   /* FOIT 감내, CLS 방지 우선 */
  /* woff2 src 생략 — Google Fonts URL 사용 시 자동 처리 */
}
```

Google Fonts URL에 `&text=0123456789` subset 파라미터를 추가해 숫자 전용 글자만 로드 (약 80KB → 10KB 이하):

```
https://fonts.googleapis.com/css2?family=Fraunces:wght@700&text=0123456789&display=optional
```

#### `font-tabular` 필수 적용 컴포넌트 목록 (P2-15)

아래 컴포넌트에 `font-tabular` 클래스 또는 `font-feature-settings: 'tnum' 1` 적용이 **필수**다:

| 컴포넌트 | 파일 | 적용 대상 |
|----------|------|-----------|
| `interview-timer.tsx` | `components/interview/` | 카운트다운 숫자 전체 |
| `TimelineBar` | `components/feedback/timeline-bar.tsx` | 시간 표시 (00:00 형식) |
| `ChapterMarker` | `components/layout/chapter-marker.tsx` | `01`, `02` 마커 숫자 |
| Dashboard 통계 | `pages/dashboard-page.tsx` | 면접 횟수, 점수 숫자 |
| feedback 타임스탬프 | `pages/interview-feedback-page.tsx` | 질문별 시간 표시 |

### 3.3 Shadow (5단계)

| Level | 값 | 사용 맥락 | 금지 |
|-------|----|-----------|------|
| `shadow-none` | none | Hairline 영역 (reading, hero) | — |
| `shadow-xs` | `0 1px 2px rgba(20,19,15,0.04)` | Input, 얇은 구분 | 카드 elevation에 단독 사용 금지 |
| `shadow-sm` | `0 1px 5px -2px rgba(20,19,15,0.08), 0 0 0 1px rgba(20,19,15,0.04)` | Selection card (대기 상태) | — |
| `shadow-md` | `0 4px 12px -4px rgba(20,19,15,0.12), 0 0 0 1px rgba(20,19,15,0.06)` | Dashboard 통계 묶음, dialog 내부 | 3개 이상 동시 화면에 금지 |
| `shadow-lg` | `0 8px 24px -6px rgba(20,19,15,0.16), 0 0 0 1px rgba(20,19,15,0.08)` | Modal, popover, video-dock | 일반 카드에 금지 |

```js
// tailwind.config.js — boxShadow 블록 교체 (현재 toss/toss-lg → 아래로)
boxShadow: {
  'xs':  '0 1px 2px rgba(20,19,15,0.04)',
  'sm':  '0 1px 5px -2px rgba(20,19,15,0.08), 0 0 0 1px rgba(20,19,15,0.04)',
  'md':  '0 4px 12px -4px rgba(20,19,15,0.12), 0 0 0 1px rgba(20,19,15,0.06)',
  'lg':  '0 8px 24px -6px rgba(20,19,15,0.16), 0 0 0 1px rgba(20,19,15,0.08)',
},
```

### 3.4 Radius (차등 규칙)

| 토큰 | 값 | 사용 맥락 |
|------|----|-----------|
| `radius-xs` | 4px | 인라인 badge, 작은 pill |
| `radius-sm` | 8px | Input, button, 소형 카드 |
| `radius-md` | 12px | 대화상자 내부 카드, selection card |
| `radius-lg` | 16px | Sheet, drawer |
| `radius-xl` | 24px | Modal, video-dock |
| `radius-pill` | 9999px | Tag pill, 상태 indicator |

> **금지**: 모든 카드에 균일 `rounded-2xl`/`rounded-4xl` 사용. Radius는 요소 크기·위계에 따라 차등.

### 3.5 Motion 토큰

```css
/* frontend/src/index.css — :root 블록 */
--duration-instant:  80ms;
--duration-fast:    150ms;   /* hover, focus 상태 전환 */
--duration-normal:  220ms;   /* 패널 전환, 드로어 open */
--duration-slow:    350ms;   /* 페이지 레벨 전환 */

--ease-standard: cubic-bezier(0.2, 0, 0, 1);   /* Material You standard */
--ease-decel:    cubic-bezier(0, 0, 0.2, 1);    /* 요소가 화면 안으로 들어올 때 */
--ease-accel:    cubic-bezier(0.4, 0, 1, 1);    /* 요소가 화면 밖으로 나갈 때 */
```

> **금지**: `transition-all duration-300` 무차별 적용. `transition-colors duration-[var(--duration-fast)]` 형태로 필요 property만 명시.

> **삭제 대상** (`tailwind.config.js`): `glow-pulse`, `rec-pulse`, `ripple`, `tutorial-ring`, `tutorial-nudge` keyframe → 의미 없는 attention animation 제거. `rec-pulse`는 signal-record dot + "REC" 라벨로 대체.

#### Motion 인벤토리 (리뷰 반영 — 6종으로 확장)

토큰 정의만으로는 부족하다. 아래 6가지 용례를 기준 패턴으로 사용하며, 명시되지 않은 전환은 **만들지 않는다**.

**① StickyOutline active underline 전환**
```tsx
// 활성 항목 색·border 변화에만 적용
className="transition-[color,border-color] duration-[var(--duration-fast)]"
```

**② VideoDock seek 위치 이동**
```tsx
// TimelineBar 썸 위치 이동 — transform만 전환
className="transition-transform duration-[var(--duration-normal)] ease-[var(--ease-decel)]"
```

**③ ChapterMarker fade-in (스크롤 진입 시)**
```tsx
// Intersection Observer와 조합
className="animate-fade-in"
// tailwind.config.js keyframes에 추가:
// 'fade-in': { from: { opacity: '0', transform: 'translateY(8px)' }, to: { opacity: '1', transform: 'translateY(0)' } }
```

**④ Modal / Dialog enter (Radix Dialog 기반)**
```tsx
// Radix Dialog data-state 속성과 조합
className={cn(
  'data-[state=open]:animate-toast-slide-in',      // 12px→0 opacity 0→1
  'data-[state=closed]:opacity-0',                 // close는 즉시
  'transition-opacity duration-[var(--duration-normal)] ease-[var(--ease-decel)]'
)}
```

**⑤ Toast enter (Sonner)**
```tsx
// 기존 `toast-slide-in` keyframe 재사용
className="animate-toast-slide-in"
// auto-dismiss 4.5s — 유저 조작 없이 사라지는 유일한 surface
```

**⑥ Focus ring (키보드 포커스 전용)**
```tsx
// Tailwind ring utilities + focus-visible 한정 (마우스 클릭엔 노출 안 됨)
className={cn(
  'focus-visible:outline-none',
  'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  'focus-visible:ring-offset-background',
  'transition-shadow duration-[var(--duration-instant)]'
)}
```

> **명시적 금지**: error-shake, pulse, bounce 등 attention-grabbing 모션. 에러는 `signal-warning` 색상 + 텍스트로만 전달.

> **page-enter 전환 부재는 의도**: SPA 라우팅 시 전환 애니메이션을 넣지 않는다. "조용한" 전환이 Quiet Rigor의 원칙 — 전환 자체가 인지 부하다.

**전역 reduced-motion override** — `frontend/src/index.css` `:root` 블록 이후에 추가:
```css
@media (prefers-reduced-motion: reduce) {
  --duration-instant: 0.01ms;
  --duration-fast:    0.01ms;
  --duration-normal:  0.01ms;
  --duration-slow:    0.01ms;
}
```

#### Dark ↔ Light 전환 모션 규칙 (P1-8)

- interview 진입 시: `bg-interview-stage`로 `350ms` (`--duration-slow`) crossfade
- interview 종료 시: 일반 `--background`로 동일 `350ms` crossfade
- 구현 방식: `body` 또는 페이지 root element에 `transition-colors duration-[var(--duration-slow)]` 적용
- "몰입 리플레이 모드" (feedback-page에서 비디오 시청 시 dark 유지): **Phase D 이후 구현**. 현재는 명세만 예약.

---

## 4. Grid System (Tier 2)

### 4.1 12-column Asymmetric Grid

```js
// tailwind.config.js — theme.extend 추가
gridTemplateColumns: {
  '12': 'repeat(12, minmax(0, 1fr))',
},
maxWidth: {
  'canvas': '1440px',
},
```

- **전역 컨테이너**: `grid grid-cols-12 gap-x-6 max-w-[1440px] mx-auto px-4 md:px-8 lg:px-12`
- 본문은 **좌측 anchor** (중앙 정렬 금지). 각 페이지가 다른 컬럼 점유로 시각 비율 차별화

### 4.2 Page Layout Recipes

#### Recipe A: 7+5 Split (Home Hero)

```
┌──────────────────────────────────────────────────────────────────┐
│ col-span-7                    │ col-span-5                        │
│  Left-anchored copy           │  Product screenshot / demo        │
│  h1 (40px) — left aligned     │  (스크롤 연동 데모)               │
│  CTA — left aligned           │                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Recipe B: 4+8 Split (Setup)

```
┌──────────────────────────────────────────────────────────────────┐
│ col-span-4 sticky             │ col-span-8                        │
│  "당신의 프로필"               │  현재 스텝 편집 영역               │
│  스텝 목차 (하이라이트)         │  인라인 "다음" 링크 (버튼 아님)    │
│  지금까지 선택 누적 표시         │                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### Recipe C: 2+7+3 Editorial (Feedback)

```
┌────┬──────────────────────────┬──────────┐
│ 02  │                          │          │
│ 03  │  Reading Column          │  Video   │
│ 04  │  질문 제목 (display-lg)   │  Dock    │
│ 05  │  피드백 본문 (body-lg)    │  +       │
│     │  hairline 구분           │  Timeline│
│ col │  ChapterMarker 숫자       │  sticky  │
│ -2  │  col-span-7              │  col-3   │
└─────┴──────────────────────────┴──────────┘
```

#### Recipe D: 8+4 Split (Dashboard)

```
┌──────────────────────────────────────────────────────────────────┐
│ col-span-8                    │ col-span-4                        │
│  초대형 숫자 헤드라인            │  최근 활동 타임라인               │
│  면접 이력 테이블 (hairline row) │  (세로 rule 구분)                │
│  (카드 박스 제거)                │                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 4.3 Editorial 3-pane 반응형 매트릭스 (P1-6 scan-first 조정)

컬럼 비율 변경 근거: editorial long-form reading 모드는 "피드백 스캔" 과업과 intent가 맞지 않는다. ReadingColumn을 축소하고 VideoDock을 확장해 스캔 가능 밀도를 높인다.

| Breakpoint | 레이아웃 | StickyOutline | VideoDock |
|------------|----------|---------------|-----------|
| `sm` (< 768px) | Single column | `<StickyOutline.MobileSheet>` (bottom sheet) | `sticky top-0`, height `25vh` |
| `md` (768–1023px) | Single column | `<StickyOutline.MobileSheet>` (bottom sheet) | `sticky top-0`, height `30vh` |
| `lg` (1024–1279px) | 2-pane: `col-8` + `col-4` | `<StickyOutline.TabBar>` (horizontal tab bar) | `col-4 sticky` |
| `xl` (≥ 1280px) | 3-pane: **`col-2` + `col-6` + `col-4`** | `<StickyOutline.Desktop>` col-2 sticky | `col-4 sticky` |

> **변경 전 `2+7+3` → 변경 후 `2+6+4`**: VideoDock col 확장(3→4)으로 비디오 + 타임라인 가시성 향상. ReadingColumn은 `max-w-[55ch]`로 좁혀 세로 스캔 속도 높임 (§5.2 참조).

**`--utility-bar-height` CSS 변수 (P0-3 신설)**:

```css
/* frontend/src/index.css — :root */
--utility-bar-height: 44px;

@media (max-width: 767px) {
  --utility-bar-height: 56px;
}
```

§5.3 `StickyOutline.Desktop`, §5.4 `StickyRail` / `VideoDock`의 모든 `top-[44px]` → `top-[var(--utility-bar-height)]`로 치환. 하드코딩된 픽셀값 사용 금지.

---

## 5. Structural Primitives (Tier 2 — 신규 6개)

> **저장 경로**: `frontend/src/components/layout/`
> **Phase B 도입 순서**: `PageGrid` → `ReadingColumn` → `StickyOutline` → `VideoDock` → `ChapterMarker` → `UtilityBar`

### 5.1 `<PageGrid>`

**용도**: 모든 페이지의 12-col 전역 wrapper. 각 페이지가 `col-span-*`으로 레이아웃 recipe를 선택.

```tsx
// frontend/src/components/layout/page-grid.tsx
interface PageGridProps {
  children: React.ReactNode
  className?: string
}

export const PageGrid = ({ children, className }: PageGridProps) => (
  <div
    className={cn(
      'mx-auto w-full max-w-[1440px] px-4 md:px-8 lg:px-12',
      'grid grid-cols-4 gap-x-4',      // sm: 4-col
      'md:grid-cols-8 md:gap-x-5',     // md: 8-col
      'lg:grid-cols-12 lg:gap-x-6',    // lg+: 12-col
      className,
    )}
  >
    {children}
  </div>
)
```

### 5.2 `<ReadingColumn>` (P1-6 scan-first 조정)

**용도**: Feedback·Review의 long-form 본문 전용 컨테이너. "피드백 스캔" 과업에 맞게 행간·폭을 조정한다.

**변경 근거**: `max-w-prose(65ch)` + `leading-1.75`는 독서 전용 밀도로 피드백 스캔 과업과 맞지 않는다. `max-w-[55ch]`로 폭을 좁혀 세로 스캔을 가속하고, 행간을 `1.65`로 줄여 스캔 가능 밀도로 조정한다.

```tsx
interface ReadingColumnProps {
  children: React.ReactNode
  className?: string
}

export const ReadingColumn = ({ children, className }: ReadingColumnProps) => (
  <div
    className={cn(
      'max-w-[55ch] leading-[1.65] text-[1.0625rem]',  // 65ch→55ch, 1.75→1.65
      '[&>p+p]:mt-6',                  // 단락 간격
      '[&>h2]:mt-12 [&>h2]:mb-4',     // 섹션 헤딩 여백
      className,
    )}
  >
    {children}
  </div>
)
```

### 5.3 `<StickyOutline>` — Compound 패턴 (P0-2 전면 개정)

**설계 근거**: 단일 컴포넌트에서 Portal 기반 Sheet와 CSS `hidden/block` 토글을 혼용하면 God Component 안티패턴이 된다. Portal은 CSS visibility로 제어 불가하므로 breakpoint별 3종 compound로 분할한다.

```
StickyOutline/
  index.tsx          — export { Desktop, TabBar, MobileSheet }
  desktop.tsx        — xl+: col-span-2 sticky
  tab-bar.tsx        — lg: 상단 horizontal tab
  mobile-sheet.tsx   — md/sm: Radix Sheet (Portal 기반 bottom sheet)
```

#### 공통 타입

```tsx
// frontend/src/components/layout/sticky-outline/types.ts
export interface OutlineItem {
  id: string
  label: string
  index: number
  hasIssue?: boolean    // P1-6: signal-warning dot 표시 여부
}

export interface StickyOutlineBaseProps {
  items: OutlineItem[]
  activeId: string
  onSelect: (id: string) => void
}
```

#### `StickyOutline.Desktop` (xl+)

```tsx
// frontend/src/components/layout/sticky-outline/desktop.tsx
interface DesktopProps extends StickyOutlineBaseProps {
  className?: string
}

export const Desktop = ({ items, activeId, onSelect, className }: DesktopProps) => (
  <nav
    className={cn(
      'hidden xl:flex flex-col col-span-2',
      'sticky top-[var(--utility-bar-height)] self-start',
      'pt-12 gap-1',
      className,
    )}
    aria-label="질문 목차"
  >
    {items.map((item) => (
      <button
        key={item.id}
        onClick={() => onSelect(item.id)}
        className={cn(
          'flex items-center gap-2 text-left px-2 py-1.5 rounded-sm',
          'text-[13px] transition-[color,border-color] duration-[var(--duration-fast)]',
          activeId === item.id
            ? 'text-foreground font-medium border-l-2 border-accent-editorial'
            : 'text-muted-foreground border-l-2 border-transparent hover:text-foreground',
        )}
        aria-current={activeId === item.id ? 'true' : undefined}
      >
        <span className="font-tabular text-[11px] w-5 shrink-0">
          {String(item.index).padStart(2, '0')}
        </span>
        <span className="truncate">{item.label}</span>
        {item.hasIssue && (
          <span
            className="ml-auto w-1.5 h-1.5 rounded-full bg-signal-warning shrink-0"
            aria-label="이슈 있음"
          />
        )}
      </button>
    ))}
  </nav>
)
```

#### `StickyOutline.TabBar` (lg)

```tsx
// frontend/src/components/layout/sticky-outline/tab-bar.tsx
interface TabBarProps extends StickyOutlineBaseProps {
  className?: string
}

export const TabBar = ({ items, activeId, onSelect, className }: TabBarProps) => (
  <nav
    className={cn(
      'flex xl:hidden overflow-x-auto',
      'sticky top-[var(--utility-bar-height)] z-10',
      // anti-slop: backdrop-blur 금지 — 불투명 배경으로 스크롤 시 가독성 확보
      'bg-background border-b border-foreground/8',
      'px-4 gap-1',
      className,
    )}
    aria-label="질문 탭"
  >
    {items.map((item) => (
      <button
        key={item.id}
        onClick={() => onSelect(item.id)}
        className={cn(
          'shrink-0 px-3 py-2.5 text-[13px] relative',
          'transition-[color,border-color] duration-[var(--duration-fast)]',
          activeId === item.id
            ? 'text-foreground border-b-2 border-accent-editorial font-medium'
            : 'text-muted-foreground border-b-2 border-transparent hover:text-foreground',
        )}
        aria-current={activeId === item.id ? 'true' : undefined}
      >
        {String(item.index).padStart(2, '0')}
        {item.hasIssue && (
          <span className="absolute top-2 right-1 w-1.5 h-1.5 rounded-full bg-signal-warning" />
        )}
      </button>
    ))}
  </nav>
)
```

#### `StickyOutline.MobileSheet` (md/sm — Portal 기반)

```tsx
// frontend/src/components/layout/sticky-outline/mobile-sheet.tsx
// Radix Sheet 사용 — Portal이므로 CSS hidden/block 제어 불가, JS state로 open 관리
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'

interface MobileSheetProps extends StickyOutlineBaseProps {
  triggerLabel?: string
}

export const MobileSheet = ({ items, activeId, onSelect, triggerLabel = '목차' }: MobileSheetProps) => (
  <Sheet>
    <SheetTrigger asChild>
      <button
        className="lg:hidden fixed bottom-6 right-4 z-30 h-11 px-4 rounded-pill bg-foreground text-background text-sm font-medium shadow-lg"
        aria-label={triggerLabel}
      >
        ≡ {triggerLabel}
      </button>
    </SheetTrigger>
    <SheetContent side="bottom" className="max-h-[60vh] rounded-t-[var(--radius-lg)]">
      <nav className="flex flex-col gap-1 py-4" aria-label="질문 목차">
        {items.map((item) => (
          <button
            key={item.id}
            onClick={() => { onSelect(item.id) }}
            className={cn(
              'flex items-center gap-3 px-4 py-3 text-left rounded-sm',
              'transition-colors duration-[var(--duration-fast)]',
              activeId === item.id ? 'text-foreground font-medium' : 'text-muted-foreground',
            )}
          >
            <span className="font-tabular text-[11px] w-5 text-accent-editorial">
              {String(item.index).padStart(2, '0')}
            </span>
            {item.label}
            {item.hasIssue && (
              <span className="ml-auto w-1.5 h-1.5 rounded-full bg-signal-warning" />
            )}
          </button>
        ))}
      </nav>
    </SheetContent>
  </Sheet>
)
```

#### `useBreakpoint()` 훅 활용

```tsx
// 사용처 (interview-feedback-page.tsx)
// useBreakpoint 훅으로 분기 렌더링 — SSR hydration mismatch 주의
const isXl = useBreakpoint('xl')   // >= 1280px
const isLg = useBreakpoint('lg')   // >= 1024px

// xl+: Desktop, lg: TabBar, md/sm: MobileSheet는 항상 마운트 (Portal이므로)
<StickyOutline.Desktop items={items} activeId={activeId} onSelect={handleSelect} />
<StickyOutline.TabBar  items={items} activeId={activeId} onSelect={handleSelect} />
<StickyOutline.MobileSheet items={items} activeId={activeId} onSelect={handleSelect} />
```

**activeId 추적 — IntersectionObserver 권장 (P2-16)**:

scroll listener 대신 IntersectionObserver를 사용한다. scroll listener는 throttle 없이 사용 시 성능 저하가 발생한다.

```ts
// rootMargin: 상단 44px(UtilityBar) + TabBar 높이를 제외하고, 하단 80%는 무시
const observer = new IntersectionObserver(callback, {
  rootMargin: '-44px 0px -80% 0px',
  threshold: 0,
})
```

**반응형 클래스 정리**:
- `StickyOutline.Desktop`: `hidden xl:flex` (CSS로 lg 이하 숨김)
- `StickyOutline.TabBar`: `flex xl:hidden` (CSS로 xl 이상 숨김)
- `StickyOutline.MobileSheet`: trigger는 `lg:hidden`, SheetContent는 Portal → CSS 제어 불필요

### 5.4 `<StickyRail>` + `<VideoDock>` (P0-4 재구조)

**재구조 근거**: `<VideoDock>`은 `TimestampFeedback[]` 도메인 타입과 seek 로직을 보유하므로 layout primitive가 아니라 feedback composite이다. layout 책임과 도메인 책임을 분리한다.

#### 5.4-A `<StickyRail>` — layout primitive (순수)

**저장 경로**: `frontend/src/components/layout/sticky-rail.tsx`

**용도**: col 배치 + sticky top만 담당. 도메인 의존성 없음.

```tsx
// frontend/src/components/layout/sticky-rail.tsx
interface StickyRailProps {
  children: React.ReactNode
  col?: string          // 기본값: 'col-span-4'
  offset?: string       // 기본값: 'top-[var(--utility-bar-height)]'
  className?: string
}

export const StickyRail = ({
  children,
  col = 'col-span-4',
  offset = 'top-[var(--utility-bar-height)]',
  className,
}: StickyRailProps) => (
  <aside
    className={cn(
      col,
      'sticky self-start',
      offset,
      'max-h-[calc(100vh-var(--utility-bar-height))] overflow-y-auto',
      className,
    )}
  >
    {children}
  </aside>
)
```

#### 5.4-B `<VideoDock>` — feedback composite

**저장 경로**: `frontend/src/components/feedback/video-dock.tsx`

**용도**: `StickyRail` + `VideoPlayer` + `TimelineBar` 컴포지션. `TimestampFeedback[]` 도메인 타입 소유.

```tsx
// frontend/src/components/feedback/video-dock.tsx
interface VideoDockProps {
  streamingUrl: string
  fallbackUrl?: string
  feedbacks: TimestampFeedback[]
  currentTimeMs: number
  onSeek: (ms: number) => void
  state?: 'loading' | 'empty' | 'error'   // P2-9: 4-state 대응
  className?: string
}

export const VideoDock = ({ streamingUrl, feedbacks, currentTimeMs, onSeek, state, className }: VideoDockProps) => (
  <StickyRail className={className}>
    {state === 'loading' && <VideoDockSkeleton />}
    {state === 'error'   && <VideoDockError />}
    {state === 'empty'   && <VideoDockEmpty />}
    {!state && (
      <>
        {/* xl: aspect-video 예약으로 CLS 방지 (P2-11) */}
        <div className="aspect-video w-full bg-foreground/5 xl:aspect-video">
          <VideoPlayer src={streamingUrl} fallback={fallbackUrl} />
        </div>
        {/* lg 미만 sticky 모드: placeholder box로 CLS 방지 */}
        <div className="h-[25vh] md:h-[30vh] xl:hidden relative">
          {/* 비디오 로드 전 placeholder */}
        </div>
        <TimelineBar
          feedbacks={feedbacks}
          currentTimeMs={currentTimeMs}
          onSeek={onSeek}
          className="transition-transform duration-[var(--duration-normal)] ease-[var(--ease-decel)]"
        />
      </>
    )}
  </StickyRail>
)
```

**반응형 동작** (StickyRail `col` prop으로 제어):
- `xl+`: `col-span-4 sticky top-[var(--utility-bar-height)]`
- `lg`: `col-span-4 sticky top-[var(--utility-bar-height)]`
- `md`: `sticky top-0 w-full h-[30vh]`
- `sm`: `sticky top-0 w-full h-[25vh]`

**z-index 계층**: `z-10` (UtilityBar `z-20` 아래)

**CLS 대응 (P2-11)**:
- xl 3-pane: `aspect-video` 명시로 레이아웃 공간 예약
- lg 미만 sticky 모드: `h-[25vh] md:h-[30vh]` + 비디오 로드 전 placeholder box
- `max-h-[calc(100vh-var(--utility-bar-height))] overflow-y-auto` (StickyRail에서 처리) → viewport 잘림 방지

### 5.5 `<UtilityBar>`

**용도**: 전 페이지 헤더 대체. 시각 높이 44px, 모바일 56px. 터치 hitbox는 44×44px 이상 보장.

```tsx
interface UtilityBarProps {
  chapter?: string      // 예: "FEEDBACK · Q3 of 8"
  actions?: React.ReactNode
  className?: string
}

export const UtilityBar = ({ chapter, actions, className }: UtilityBarProps) => (
  <header
    className={cn(
      'sticky top-0 z-20 w-full border-b border-foreground/8',
      // anti-slop(리뷰 반영): backdrop-blur 사용 금지 — 불투명 배경 + hairline border로 정직하게 분리
      'bg-background',
      'h-11 md:h-11',                              // 44px
      'flex items-center justify-between',
      'px-4 md:px-8',
      className,
    )}
    role="banner"
  >
    {/* 챕터 표시 */}
    <span className="font-tabular text-[11px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
      {chapter}
    </span>
    {/* 우측 액션 — 각 버튼 최소 44×44px hitbox */}
    <div className="flex items-center gap-1">
      {actions}
    </div>
  </header>
)
```

**모바일 규칙**: `sm`에서 챕터 표시를 두 번째 줄로 스택, 전체 bar 높이 56px (`h-14`).

### 5.6 `<ChapterMarker>` (P1-5 크기 축소 — 위계 역전 해소)

**변경 근거**: 기존 `text-[48px] font-bold` 숫자는 F-pattern 첫 fixation을 장식 요소가 독점하는 구조였다. 실제 사용자 과업(질문 내용 파악)에서 숫자는 참조 정보일 뿐이다. 숫자를 over-line caption으로 격하하고, 질문 제목을 `display-lg`로 승격해 진짜 위계를 부여한다.

**용도**: 섹션 간 구분. 숫자는 상단 over-line caption(11px), 질문 제목이 `display-lg(40–48px)`로 주인공. hairline `border-t`. 카드 박스 없이 읽기 흐름 분절.

```tsx
interface ChapterMarkerProps {
  index: number          // 1-based (01, 02 ...)
  title: string          // 질문 제목 — display-lg로 표시 (필수)
  label?: string         // 카테고리 over-line 라벨 (선택)
  className?: string
}

export const ChapterMarker = ({ index, title, label, className }: ChapterMarkerProps) => (
  <div className={cn('pt-12 pb-6 border-t border-foreground/8 animate-fade-in', className)}>
    {/* over-line: 숫자 + 카테고리 — 11px uppercase caption으로 격하 */}
    <div className="flex items-center gap-2 mb-3">
      <span
        className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-accent-editorial select-none"
        aria-hidden="true"
      >
        {String(index).padStart(2, '0')}
      </span>
      {label && (
        <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
          {label}
        </span>
      )}
    </div>
    {/* 질문 제목: display-lg (40–48px) — 진짜 위계의 주인공 */}
    <h2 className="text-[2.5rem] md:text-[3rem] font-bold leading-[1.10] tracking-[-0.02em] text-foreground">
      {title}
    </h2>
  </div>
)
```

**Before/After 비교**:
- Before: 숫자 `48px bold` (F-pattern 낭비) + 카테고리 라벨 `14px`
- After: 숫자 `11px uppercase caption` (over-line) + 질문 제목 `40–48px bold` (display-lg 승격)

---

## 6. Component Specs (리파인)

### 6.1 Button Variants

현재 파일: `frontend/src/components/ui/button-variants.ts:10`

**Before**:
```ts
default: 'bg-primary text-primary-foreground shadow-lg shadow-primary/20 border-t border-white/10 hover:bg-primary/90 active:scale-[0.98]',
cta: 'bg-primary text-primary-foreground shadow-[0_10px_20px_-5px_rgba(0,0,0,0.1)] ...',
```

**After** (섀도우 재설계 — signal 토큰 기반):
```ts
default:
  'bg-primary text-primary-foreground shadow-sm hover:bg-primary/90 active:scale-[0.98] transition-colors duration-[var(--duration-fast)]',
cta:
  'bg-primary text-primary-foreground shadow-md hover:shadow-lg hover:bg-primary/90 active:scale-[0.98] transition-[color,box-shadow] duration-[var(--duration-fast)]',
// cta는 Home Hero CTA 1개에만 사용 — 다른 페이지 금지
ghost:
  'bg-transparent text-foreground/70 hover:text-foreground hover:bg-foreground/6 active:scale-[0.98]',
```

> **수정 이유**: `duration-[--duration-fast]`는 CSS 변수를 직접 참조하는 잘못된 문법. Tailwind arbitrary value에서 CSS 변수는 반드시 `var()` 래퍼 포함 — `duration-[var(--duration-fast)]`.

**사용 영역 제한**:
- `cta` variant: Home Hero CTA button 단 1개
- `default` (primary fill): 각 페이지당 primary action 최대 1개
- `ghost`: UtilityBar 내부 아이콘 버튼 전용 (nav 영역)

### 6.2 Card (Elevation 3단, 사용 영역 제한)

**L3 기조**: 카드는 "의미 있는 분리가 필요한 영역"에만 유지.

| Elevation | Shadow | Radius | 허용 영역 |
|-----------|--------|--------|-----------|
| `card-flat` | none + `border border-foreground/8` | `radius-sm` (8px) | 기본값 (단순 구분) |
| `card-raised` | `shadow-sm` | `radius-md` (12px) | Selection wizard 옵션, review 카드 |
| `card-elevated` | `shadow-md` | `radius-md` (12px) | Dashboard 통계 묶음, dialog 내부 |

**사용 금지 영역**: `ReadingColumn` 내부, Hero 섹션, ChapterMarker 인접 영역, 테이블 row.

### 6.3 Badge

**Before**: `rounded-full bg-muted px-2 py-0.5` (interview-feedback-page.tsx:53)

**After**: shadcn `<Badge>` variant 통일
```tsx
// variant="outline" — 인터뷰 타입 태그
<Badge variant="outline" className="font-tabular text-xs">
  {label}
</Badge>
```
**pill 정정**: `rounded-pill` (`9999px`) 유지, 단 크기 제한 → `text-xs`, `py-0.5 px-2` 고정.

### 6.4 SelectionCard (P2-12 — Merge → Compose 재분류)

**재분류 근거**: `step-tech-stack`(tag input), `step-duration`(slider), `step-position`(단일 선택)은 인터랙션 패턴이 달라 단일 `SelectionCard` props로 수용 불가. `<SelectionCard>`는 시각적 primitive만 담당하고, 각 step wrapper가 내부에서 composition한다.

#### `<SelectionCard>` — 시각적 primitive만

```tsx
// frontend/src/components/ui/selection-card.tsx (신규)
interface SelectionCardProps {
  label: string
  description?: string
  selected?: boolean
  disabled?: boolean
  onClick?: () => void
  children?: React.ReactNode   // 내부 커스텀 인터랙션 슬롯
}

// state별 스타일:
// neutral:  card-flat + text-foreground/60
// active:   card-raised + border-accent-editorial + text-foreground
// disabled: opacity-40 + pointer-events-none
```

#### Step wrapper — SelectionCard composition 예시

```tsx
// step-position.tsx — 단일 선택 (SelectionCard 직접 사용)
<SelectionCard label="백엔드" selected={position === 'backend'} onClick={() => setPosition('backend')} />

// step-tech-stack.tsx — tag input (SelectionCard + 내부 tag input 슬롯)
<SelectionCard label="기술 스택 선택" selected={stack.length > 0}>
  <TagInput value={stack} onChange={setStack} placeholder="예: TypeScript, Spring Boot" />
</SelectionCard>

// step-duration.tsx — slider (SelectionCard + 내부 Slider 슬롯)
<SelectionCard label="면접 시간" selected>
  <Slider min={10} max={60} step={5} value={[duration]} onValueChange={([v]) => setDuration(v)} />
</SelectionCard>
```

`step-interview-type.tsx`, `step-level.tsx`는 단일/다중 선택이므로 `<SelectionCard>` 직접 사용 (wrapper 유지, 내부 UI만 교체).

### 6.5 Empty / Loading / Error State 규칙 (P2-9 신설)

모든 structural primitive는 4-state(default / loading / empty / error)를 정의한다. 일러스트레이션 없이 copy + action으로 처리한다.

#### 공통 원칙

| State | 색상 | 동작 금지 | 필수 요소 |
|-------|------|-----------|-----------|
| `loading` | `muted-foreground/40` skeleton | signal-warning 표시 금지 | `animate-pulse` skeleton |
| `empty` | `muted-foreground/60` copy | 일러스트레이션 금지 | 짧은 안내 copy + 작은 action link |
| `error` | `signal-warning` | 자동 retry 금지 | signal-warning 아이콘 + retry 버튼 |

#### Primitive별 4-state 규칙

**`<ReadingColumn>`**
```tsx
// loading
<div className="space-y-4 animate-pulse">
  <div className="h-10 w-3/4 rounded bg-muted-foreground/20" />  {/* 질문 제목 skeleton */}
  <div className="h-4 w-full rounded bg-muted-foreground/10" />
  <div className="h-4 w-5/6 rounded bg-muted-foreground/10" />
</div>
// empty: 피드백 생성 전 상태
<p className="text-sm text-muted-foreground">아직 피드백이 없습니다. <a className="underline">면접을 시작</a>해보세요.</p>
// error
<div className="flex items-center gap-2 text-signal-warning text-sm">
  <AlertTriangle className="w-4 h-4" /> 피드백을 불러오지 못했습니다.
  <button className="underline ml-2" onClick={retry}>다시 시도</button>
</div>
```

**`<VideoDock>` / `<StickyRail>`** — `state` prop으로 분기 (`VideoDockProps.state?: 'loading' | 'empty' | 'error'`)
```tsx
// loading: aspect-video 유지하여 CLS 방지
<div className="aspect-video w-full animate-pulse bg-muted-foreground/10 rounded" />
// empty
<div className="aspect-video w-full flex items-center justify-center text-sm text-muted-foreground">
  영상이 없습니다.
</div>
// error
<div className="aspect-video w-full flex flex-col items-center justify-center gap-2 text-signal-warning">
  <AlertTriangle className="w-5 h-5" />
  <span className="text-sm">영상을 불러오지 못했습니다.</span>
  <button className="text-sm underline" onClick={retry}>다시 시도</button>
</div>
```

**`<StickyOutline.Desktop>` / `<StickyOutline.TabBar>`**
```tsx
// loading: 항목 수 기반 skeleton
{Array.from({ length: 5 }).map((_, i) => (
  <div key={i} className="h-7 rounded animate-pulse bg-muted-foreground/10 mx-2" />
))}
// empty: 질문 없음
<p className="px-2 text-xs text-muted-foreground">질문 목록이 없습니다.</p>
// error: outline 자체 실패는 드물므로 silent — error boundary로 위임
```

**`<PageGrid>`**: 4-state는 하위 primitive에 위임, PageGrid 자체는 상태 없음.

---

## 7. Page Specs (Tier 3)

### 7.1 interview-feedback-page (Priority 1)

**현재 파일**: `frontend/src/pages/interview-feedback-page.tsx:267`

**Before**:
```
┌─────────────────────────────────────────┐
│  InterviewInfoBar (max-w-6xl mx-auto)   │
├─────────────────────────────────────────┤
│  max-w-6xl mx-auto px-4                 │
│  ┌──────────────────┬──────────────────┐ │
│  │   lg:w-[60%]     │   lg:w-[40%]     │ │
│  │   VideoPlayer    │   FeedbackPanel  │ │
│  │   TimelineBar    │                  │ │
│  │   QuestionList   │                  │ │
│  └──────────────────┴──────────────────┘ │
└─────────────────────────────────────────┘
```

**After (xl: 3-pane editorial — P1-6 scan-first `2+6+4`)**:
```
┌─────────────────────────────────────────────────────────────────┐
│ <UtilityBar> chapter="FEEDBACK · Q3 of 8"  [bookmark] [share]  │
├──────┬────────────────────────────┬────────────────────────────┤
│ col-2│          col-6             │        col-4               │
│      │  <ReadingColumn>           │  <VideoDock>               │
│  01  │  <ChapterMarker            │  aspect-video              │
│  02  │   index={1}                │  VideoPlayer               │
│  03  │   title="질문 내용"         │  TimelineBar               │
│  04  │   label="기술 면접" />      │  (sticky                   │
│      │                            │   top-[var(               │
│      │  피드백 본문 (body-lg 1.65) │   --utility-bar-height)])  │
│ Sticky│  max-w-[55ch]             │                            │
│ top  │  hairline 구분             │                            │
│      │  <ChapterMarker index={2}  │                            │
└──────┴────────────────────────────┴────────────────────────────┘
```

**반응형 recipe**:
| Breakpoint | 레이아웃 |
|------------|----------|
| `sm` | single col, VideoDock sticky-top 25vh, StickyOutline → MobileSheet |
| `md` | single col, VideoDock sticky-top 30vh, StickyOutline → MobileSheet |
| `lg` | 2-pane (col-8 + col-4), StickyOutline → TabBar |
| `xl` | 3-pane (**col-2 + col-6 + col-4**) — scan-first 비율 |

**변경 요소 체크리스트**:
- [x] L1: `max-w-6xl mx-auto` 제거 → `<PageGrid>` 교체
- [x] L2: 3-pane editorial 적용 (xl+)
- [x] L3: `QuestionSetSection` 박스 (`rounded-4xl bg-surface`) → `<ReadingColumn>` + `<ChapterMarker>` 교체
- [x] L4: `InterviewInfoBar` → `<UtilityBar chapter="...">` 교체
- [x] L5: `space-y-6` 균등 → 챕터 간 96px, 단락 간 24px 차등
- [x] L6: `QuestionList`(좌) + `FeedbackPanel`(우) 고정 → `<StickyOutline>` + `<ReadingColumn>` + `<VideoDock>` 분리

**Hero 섹션 제거**: 첫 ChapterMarker가 바로 시작. 메타 정보(날짜, 포지션, 시간)는 UtilityBar로 이전.

### 7.2 interview-page (Priority 1)

**현재 파일**: `frontend/src/pages/interview-page.tsx:147`

**Before**:
```
┌─────────────────────────────────────────┐
│  flex h-screen flex-col bg-studio-bg    │
│  ┌─────────────────────────────────────┐│
│  │  AI Interviewer Tile (full-screen)  ││
│  │  ring-2 ring-meet-green/red         ││
│  │  animate-rec-pulse (glow)           ││
│  │  Question overlay (bottom-center)   ││
│  └─────────────────────────────────────┘│
│  [하단 controls — Google Meet 스타일]   │
└─────────────────────────────────────────┘
```

**After (극장식 몰입)**:
```
┌─────────────────────────────────────────────────────┐
│  bg-interview-stage h-screen flex overflow-hidden   │
│  ┌────────────────────────────────┬────────────────┐│
│  │  AI Interviewer (full-bleed)   │  Right Rail    ││
│  │  배경: warm off-black          │  col-3 (lg+)   ││
│  │  ring 효과 제거                 │  질문 display  ││
│  │  glow 제거                      │  타이머 (tnum) ││
│  │  상단 좌측: [● REC] 라벨        │  진행 인디케이터││
│  │  (signal-record dot 고정 표시)  │  ──────────── ││
│  │                                │  [종료] [일시중지]│
│  │  Bottom caption: 질문 텍스트   │  항상 visible  ││
│  └────────────────────────────────┴────────────────┘│
│  [모바일 < md]: 하단 고정 bar 56px (탭 타겟 보장)   │
└─────────────────────────────────────────────────────┘
```

**반응형 recipe**:
| Breakpoint | 레이아웃 |
|------------|----------|
| `sm` | 비디오 full-bleed, 하단 고정 bar 56px (질문 + 종료/일시중지) |
| `md` | 비디오 full-bleed, 우측 rail narrow (col-4) |
| `lg+` | 비디오 full-bleed, 우측 rail col-3 |

**컨트롤 발견성 규칙 (중요)**:
- 종료·일시중지 버튼은 **항상 visible** — hover에 숨기지 않음
- 우측 rail 하단에 고정 pill (lg+), 모바일은 하단 bar 56px 높이
- 각 버튼 hitbox 최소 **44×44px** (WCAG 2.5.5 Target Size)
- `animate-rec-pulse` (glow ring) **삭제** → `--signal-record` dot(`●`) + "REC" 텍스트 라벨로 교체
- `ring-2 ring-meet-green/red` **삭제** → audio indicator는 subtle waveform으로만
- `meet-green`, `meet-red` 색상 토큰 **삭제** → `--signal-record`, `--signal-warning` 사용

**변경 요소 체크리스트**:
- [x] L3: `rounded-lg overflow-hidden` 메인 타일 → 완전 full-bleed (radius 제거)
- [x] L4: 하단 controls bar → 우측 rail (lg+) / 하단 56px bar (sm)
- [x] L5: 질문 overlay `bottom-12` → rail 내 상단 섹션 (독립 공간)
- [x] L6: Google Meet 복제 레이아웃 → 극장식 비대칭

### 7.3 interview-setup-page (Priority 1)

**현재 파일**: `frontend/src/pages/interview-setup-page.tsx:16`

**Before**:
```
┌─────────────────────────────────────────┐
│  max-w-2xl mx-auto 중앙 정렬            │
│  ProgressBar (상단 가로 바)             │
│  ┌───────────────────────────────────┐  │
│  │  Step 컨텐츠 (step-*.tsx)         │  │
│  └───────────────────────────────────┘  │
│  [이전] ──────────────── [다음]         │
└─────────────────────────────────────────┘
```

**After (4+8 split)**:
```
┌─────────────────────────────────────────────────────────────────┐
│  <UtilityBar chapter="SETUP · 02 / 05">                        │
├──────────────────────────┬──────────────────────────────────────┤
│  col-span-4 sticky        │  col-span-8                          │
│  "당신의 프로필"           │  현재 스텝 (<SelectionCard> 그리드) │
│                            │                                      │
│  01 포지션 (done)          │  <ChapterMarker index={2}           │
│  ▶ 02 면접 유형 (active)   │    label="면접 유형 선택" />        │
│  ○ 03 경력 수준             │                                      │
│  ○ 04 기술 스택             │  [기술 면접] [인성 면접] ...         │
│  ○ 05 시간 설정             │  (<SelectionCard> variant 그리드)   │
│                            │                                      │
│  ┌──────────────────────┐  │  [→ 다음 단계 (텍스트 링크)]        │
│  │  선택 누적 표시        │  │  (버튼 아님, underline link)        │
│  │  포지션: 백엔드        │  │                                      │
│  └──────────────────────┘  │                                      │
└──────────────────────────┴──────────────────────────────────────┘
```

**반응형 recipe**:
| Breakpoint | 레이아웃 |
|------------|----------|
| `sm` | single col, 상단 tab bar (목차 → 가로 스크롤 탭) |
| `md` | single col + 상단 tab bar |
| `lg+` | 4+8 split |

**변경 요소 체크리스트**:
- [x] L1: `max-w-2xl mx-auto` → `<PageGrid>` 4+8 split
- [x] L2: 설문지 형태 → editorial 진행 (좌측 sticky 목차)
- [x] L3: step-* 박스형 카드 → `<SelectionCard>` 통합 (card-raised)
- [x] L4: `ProgressBar` (setup-progress-bar.tsx) 제거 → `<UtilityBar chapter>` + 좌측 목차 하이라이트
- [x] L5: 하단 네비 `[이전][다음]` → 우측 영역 하단 인라인 텍스트 링크
- [x] L6: 스텝별 화면이 같은 템플릿 → 좌측 목차 + 우측 콘텐츠 구조로 차별화

### 7.4 home-page (Priority 2 — 간략)

**현재 파일**: `frontend/src/pages/home-page.tsx:48`

| 섹션 | Before | After |
|------|--------|-------|
| HeroSection | `max-w-5xl mx-auto` 중앙 | 7+5 split, 좌 anchor copy + 우 제품 스크린샷 |
| PainPoints/Journey | 같은 폭 순차 스택 | numbered chapter (2+7+3), hairline |
| MetricsSection | 카드형 수치 | 박스 없는 **초대형 숫자 갤러리** (display-xl, Fraunces) |
| CTASection | 중앙 정렬 | full-width **좌 정렬 manifesto** |

### 7.5 dashboard-page (Priority 2 — 간략)

**현재 파일**: `frontend/src/pages/dashboard-page.tsx:17`

- `StatsCards` 박스 → 활자 갤러리 (`display-xl` 숫자 + caption 라벨, 박스 없음)
- 테이블 `rounded-*` border 제거 → hairline row (`border-b border-foreground/8`)
- 8+4 split: 좌 8-col 메인 + 우 4-col 활동 타임라인

### 7.6 Priority 3 (토큰 승계만)

`about`, `privacy`, `faq`, `guide` 페이지는 **wireframe 생략**. 아래 두 가지만 적용:
1. `--background`, `--foreground` 토큰 자동 승계 (CSS 변수 교체 시 자동 반영)
2. `font-sans` Pretendard, `font-tabular` 클래스 필요 부분 추가

---

## 8. 이미지 자산 정책

### 필요 자산 5종

| # | 자산 | 크기 | 용도 |
|---|------|------|------|
| 1 | `feedback-3pane-light.png` / `feedback-3pane-dark.png` | 2400×1500 @2x | Home Hero 우 5-col 제품 목업 |
| 2 | `feedback-timeline-closeup.png` | 800×600 @2x | 피드백 UI 시연 클로즈업 |
| 3 | `interview-theater-preview.png` | 1920×1080 @2x | 극장식 몰입 모드 면접 화면 |
| 4 | `dashboard-headline.png` | 1600×1000 @2x | 대시보드 초대형 숫자 헤드라인 |
| 5 | `section-pain-points.png` / `section-journey.png` | 1200×800 @2x 각 | Home 섹션 기능 증빙 |

### 제작 방법 — **옵션 A (Playwright 자동 캡처) 권장**

- Phase B/C 완료 시마다 Playwright 스크립트(`e2e/capture-mockups.ts`)로 자동 재생성
- 목업 데이터는 `fixtures/mockup-interview.json` (실명 없는 더미)
- 초기 Home 랜딩 작업 전에는 **옵션 B (Figma 임시)** 사용 가능, Phase B 완료 후 A로 교체

### 자산 관리

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

- 파일명: `{page}-{variant}-{theme}.{ext}`
- 해상도: @2x PNG 기본, GIF/WebM 5MB 이하
- 템플릿:
```html
<picture>
  <source media="(min-width: 1280px)" srcset="/mockups/feedback-3pane-light.png 2x">
  <img src="/mockups/feedback-3pane-light.png" alt="Rehearse 피드백 3-pane 화면" loading="lazy" decoding="async">
</picture>
```

---

## 9. DESIGN.md 관계 매트릭스

| 항목 | 기존 DESIGN.md | 본 리디자인 | 관계 | DESIGN.md 수정 방법 |
|------|----------------|------------|------|---------------------|
| Cal.com 기반 모노크롬 철학 | ✓ | ✓ 유지 | 승계 | 유지 |
| Charcoal `#242424` primary | ✓ | warm off-black `#14130f`로 미세 조정 | 개선 | §2 Color Palette 값 교체 |
| Pretendard 본문 | ✓ | ✓ 유지 | 승계 | 유지 |
| Cal Sans display | ✓ | **제거** (리뷰 반영 — src 사용처 0건, Cal.com 잔향 제거) | 제거 | §3 Typography에서 Cal Sans 레퍼런스 삭제 |
| Fraunces serif | ✗ | **영문 display 단일 폰트로 승격** (헤드라인·숫자 마커 전담) | 신규 + 승격 | §3 Typography를 Pretendard + Fraunces + JetBrains Mono 3-폰트 시스템으로 재작성 |
| Aceternity (Plan 04 제정) | ✓ 히어로 1개 제한 | 동일 정책 승계 — 히어로 외 신규 도입 금지 | 승계 | 유지 |
| Inter body | ✓ (DESIGN.md 원본) | 이미 Pretendard로 교체됨 | 이미 개선됨 | 불필요 |
| 멀티레이어 섀도우 | ✓ | 사용 영역 제한, 5단계 재정의 | 축소·재정의 | §6 Elevation 테이블 교체 |
| Product screenshots primary | ✓ | 자산 정책 구체화 | 확장 | §8 Asset Policy 섹션 append |
| 레이아웃 시스템 | ✗ | 12-col asymmetric grid, 3-pane, hairline | 신규 | `## Layout System` 섹션 append |
| Semantic color 분리 | ✗ | accent/warning/record/success | 신규 | `## Semantic Color` 섹션 append |
| 반응형 매트릭스 | ✗ | breakpoint별 recipe | 신규 | `## Responsive Strategy` 섹션 append |

**DESIGN.md 수정 전략**: 교체 아님, **append + 부분 개정**. 색·타이포 섹션 값 수정 + 하단 3개 섹션 신설.

---

## 10. 기존 컴포넌트 마이그레이션 매트릭스

### Priority 1 대상 (Phase B/C에서 완료)

#### `components/feedback/` (13개)

| 파일 | 판정 | 근거 |
|------|------|------|
| `video-player.tsx` | **Keep** | `<VideoDock>` 내부에 그대로 삽입 |
| `timeline-bar.tsx` | **Keep** | `<VideoDock>` 내부 위치 이동만 |
| `question-list.tsx` | **Replace** | `<StickyOutline>` items prop으로 대체. 하단 props 매핑 테이블 참조 (P2-17) |
| `feedback-panel.tsx` | **Refactor** | `<ReadingColumn>` 내부로 이동, 박스 제거 |
| `coaching-card.tsx` | **Refactor** | card-flat elevation으로 shadow 축소 |
| `accuracy-issues.tsx` | **Keep** | 토큰 값 자동 승계 |
| `bookmark-toggle-button.tsx` | **Keep** | hitbox 44px 확인 필요 |
| `content-tab.tsx` | **Keep** | 토큰 승계 |
| `delivery-tab.tsx` | **Keep** | 토큰 승계 |
| `structured-comment.tsx` | **Keep** | `ReadingColumn` 내부에서 그대로 사용 |
| `level-badge.tsx` | **Refactor** | `<Badge variant="outline">` 통합 |
| `format-feedback-level.ts` | **Keep** | 로직 파일 — 변경 없음 |
| `review-coach-mark.tsx` | **Drop** | tutorial-ring/nudge 애니메이션 삭제 대상. 대체: feedback-page 최초 방문 시 ReadingColumn 상단 1회성 dismissible callout (`localStorage: rehearse.feedbackOnboarded` flag 기반). 온보딩 기능 자체를 제거하는 것이 아님 (P2-13) |

#### `question-list.tsx` → `StickyOutline` props 매핑 (P2-17)

| 현재 `question-list.tsx` props/내부 값 | `StickyOutline.Desktop` / `OutlineItem` props | 비고 |
|--------------------------------------|----------------------------------------------|------|
| `questions[].id` | `items[].id` | 1:1 매핑 |
| `questions[].index` (1-based) | `items[].index` | 1:1 매핑 |
| `questions[].content` (질문 본문) | `items[].label` | 60자 내외 truncate 필요 |
| `questions[].category` | `items[].meta?.category` | `meta` 확장 슬롯 수용 |
| `currentQuestionId` | `activeId` | 1:1 매핑 |
| `onSelect(id)` callback | `onSelect(id)` | 1:1 매핑 |
| (없음) | `items[].hasIssue?` | 피드백 이슈 여부 — 호출부에서 `feedbacks` 분석 후 주입 |

누락 필드(`category` 등)는 `items[].meta?: Record<string, unknown>` 확장 슬롯으로 수용한다.

#### `components/interview/` (15개)

| 파일 | 판정 | 근거 |
|------|------|------|
| `interviewer-avatar.tsx` | **Keep** | 극장식 레이아웃에 그대로 사용 |
| `video-preview.tsx` | **Keep** | 위치 이동만 (right rail → full-bleed 오버레이) |
| `interview-controls.tsx` | **Refactor** | 우측 rail 하단 고정 pill (항상 visible) |
| `interview-timer.tsx` | **Refactor** | `font-tabular` 클래스 추가, rail에 배치 |
| `finishing-overlay.tsx` | **Keep** | 토큰 승계 |
| `upload-recovery-dialog.tsx` | **Keep** | 토큰 승계 |
| `question-card.tsx` | **Replace** | rail 내 plain text display로 대체 |
| `question-card-skeleton.tsx` | **Drop** | question-card 제거에 따름 |
| `question-display.tsx` | **Refactor** | rail 내 `body-lg` 스타일로 재작성 |
| `audio-waveform.tsx` | **Keep** | meet-green → subtle monochrome waveform |
| `camera-test-row.tsx` | **Keep** | 토큰 승계 |
| `device-test-section.tsx` | **Keep** | 토큰 승계 |
| `device-test-status.ts` | **Keep** | 로직 파일 |
| `device-test-utils.tsx` | **Keep** | 로직 파일 |
| `mic-test-row.tsx` | **Keep** | 토큰 승계 |
| `speaker-test-row.tsx` | **Keep** | 토큰 승계 |

#### `components/setup/` (5+3개)

| 파일 | 판정 | 근거 |
|------|------|------|
| `step-duration.tsx` | **Compose** | wrapper 유지, 내부에서 `<SelectionCard>` + Slider composition (P2-12) |
| `step-interview-type.tsx` | **Compose** | wrapper 유지, 내부에서 `<SelectionCard>` 단일/다중 선택 |
| `step-level.tsx` | **Compose** | wrapper 유지, 내부에서 `<SelectionCard>` 단일 선택 |
| `step-position.tsx` | **Compose** | wrapper 유지, 내부에서 `<SelectionCard>` 직접 사용 |
| `step-tech-stack.tsx` | **Compose** | wrapper 유지, 내부에서 `<SelectionCard>` + TagInput composition (P2-12) |
| `setup-progress-bar.tsx` | **Drop** | `<UtilityBar chapter>` + 목차 하이라이트로 대체 |
| `setup-navigation.tsx` | **Replace** | 인라인 텍스트 링크로 대체 |
| `resume-upload.tsx` | **Keep** | 토큰 승계 |

### Priority 2/3 대상

`components/home/*` (9개), `components/review/*` — **Phase D에서 확정**. 현재는 토큰 자동 승계만.

---

## 11. Implementation Roadmap (4-Phase)

### Phase A — 토큰 교체 + DESIGN.md 개정 (예상 1–2일)

**수정 파일**:
- `frontend/src/index.css` — `--background`, `--foreground` off-neutral 교체, semantic color 4종 추가, motion 토큰 추가
- `frontend/tailwind.config.js` — shadow 재정의, `glow-pulse`/`rec-pulse`/`ripple`/`tutorial-ring`/`tutorial-nudge` keyframe 삭제, `grid-cols-12` 추가
- `DESIGN.md` — 색·타이포 섹션 값 수정 + Layout/Semantic Color/Responsive 섹션 append

**PR 분할**: 1개 PR (`refactor(fe): Phase A — 디자인 토큰 재정의`)

**Gate (P1-7 정량 기준으로 대체)**:

다음 3개 정량 기준을 **모두** 통과해야 Phase B 진입 가능. 주관 평가(사용자 피드백)는 Phase B gate로 이동.

| 기준 | 도구 | 통과 조건 |
|------|------|-----------|
| axe-core violations | `@axe-core/cli` | **= 0** |
| WCAG 4.5:1 contrast 재측정 | `@axe-core/cli` + 수동 | §3.1 수치 기준 **전 항목 통과** |
| vitest snapshot 회귀 | `vitest run` | **0건** |

시각 회귀 스크린샷 전후 비교는 추가 참고 자료로만 활용 (gate 조건 아님).

---

### Phase B — feedback-page 3-pane editorial (예상 3–5일)

**Primitive 도입 순서**: `PageGrid` → `ReadingColumn` → `StickyOutline` → `VideoDock` → `ChapterMarker` → `UtilityBar`

**수정 파일**:
- `frontend/src/components/layout/page-grid.tsx` (신규)
- `frontend/src/components/layout/reading-column.tsx` (신규)
- `frontend/src/components/layout/sticky-outline/index.tsx` (신규 — compound export)
- `frontend/src/components/layout/sticky-outline/desktop.tsx` (신규)
- `frontend/src/components/layout/sticky-outline/tab-bar.tsx` (신규)
- `frontend/src/components/layout/sticky-outline/mobile-sheet.tsx` (신규)
- `frontend/src/components/layout/sticky-outline/types.ts` (신규)
- `frontend/src/components/layout/sticky-rail.tsx` (신규 — 순수 layout primitive)
- `frontend/src/components/layout/chapter-marker.tsx` (신규)
- `frontend/src/components/layout/utility-bar.tsx` (신규)
- `frontend/src/components/feedback/video-dock.tsx` (신규 — feedback composite, layout/ 아님)
- `frontend/src/components/ui/selection-card.tsx` (신규)
- `frontend/src/pages/interview-feedback-page.tsx` — 3-pane 재배치
- `frontend/src/components/feedback/question-list.tsx` → StickyOutline으로 교체
- `frontend/src/components/feedback/review-coach-mark.tsx` → Drop (dismissible callout로 대체)

**PR 분할**:
1. `feat(fe): layout primitives 6개 신설 (PageGrid/ReadingColumn/StickyOutline/VideoDock/ChapterMarker/UtilityBar)`
2. `refactor(fe): feedback-page 3-pane editorial 전환`

**Gate**: 성공 기준 §12.3 기술 지표 + §12.1 고유성 테스트 통과.

---

### Phase C — interview-page 극장식 몰입 (예상 2–3일)

**수정 파일**:
- `frontend/src/pages/interview-page.tsx` — 극장식 레이아웃, rail 분리
- `frontend/src/components/interview/interview-controls.tsx` — 항상 visible pill
- `frontend/src/components/interview/question-display.tsx` — rail 내 재배치
- `frontend/src/components/interview/interview-timer.tsx` — font-tabular 추가

**PR 분할**: 1개 PR (`refactor(fe): interview-page 극장식 몰입 모드 전환`)

**Gate**: QA 테스트 (컨트롤 발견성 검증) + Phase B 토큰 회귀 확인.

---

### Phase D — 나머지 페이지 + a11y 재검증 (예상 3–5일)

**수정 파일**: `interview-setup-page.tsx`, `home-page.tsx`, `dashboard-page.tsx`, `components/setup/*`, `components/home/*`

**PR 분할**:
1. `refactor(fe): setup-page 4+8 split + SelectionCard 통합`
2. `refactor(fe): home-page 섹션 레이아웃 차별화`
3. `refactor(fe): dashboard-page 8+4 split + 활자 헤드라인`

**Gate**: 전체 성공 기준 재측정 (§12 전 항목).

---

## 12. 성공 기준

1. **"Rehearse 고유성 테스트"**: 각 페이지 스크린샷에서 로고·카피를 가렸을 때, Rehearse 고유 레이아웃 요소(3-pane feedback, 챕터 마커, 극장식 rail)가 1개 이상 식별 가능
2. **사용자 평가 A/B**: 리디자인 전후 스크린샷 5명 제시, "진지함·전문성" (1–7점) 평균 **+1.5 이상** 상승
3. **기술 지표**:
   - LCP ≤ 2.5s (이미지 자산 도입 후에도)
   - CLS < 0.1 (sticky/dock 도입 후에도)
   - axe-core violations = 0
   - Lighthouse accessibility ≥ 95
4. **레이아웃 원칙 준수율**: L1–L6 체크리스트, Priority 1 페이지 **6/6 통과**, Priority 2는 **4/6 이상**

---

## 13. Verification Plan

### 자동화

```bash
# axe-core 스캔 (Phase B/C/D 완료 시마다)
npx @axe-core/cli http://localhost:5173/interview/{id}/feedback --tags wcag2a,wcag2aa

# Playwright 시각 회귀
npx playwright test --project=visual-regression

# Lighthouse CI
npx lhci autorun --config=.lighthouserc.json
```

### 테스트 전략 2-tier (P2-18)

sticky/scroll 동작 테스트는 테스트 종류별 책임을 분리한다.

#### Tier 1 — RTL 순수 렌더 테스트 (vitest + @testing-library/react)

**책임**: breakpoint별 conditional mount 검증. DOM 렌더 여부만 확인.

```ts
// sticky-outline.test.tsx 예시
describe('StickyOutline breakpoint conditional mount', () => {
  it('xl 이상: Desktop만 렌더, TabBar 숨김', () => {
    mockUseBreakpoint({ xl: true, lg: true })
    render(<StickyOutline.Desktop items={mockItems} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('navigation', { name: '질문 목차' })).toBeInTheDocument()
  })

  it('lg 미만: MobileSheet trigger 노출', () => {
    mockUseBreakpoint({ xl: false, lg: false })
    render(<StickyOutline.MobileSheet items={mockItems} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('button', { name: '목차' })).toBeInTheDocument()
  })
})
```

#### Tier 2 — Playwright component test

**책임**: 실제 sticky 동작, scroll 연동, IntersectionObserver activeId 추적 검증. jsdom으로는 재현 불가한 레이아웃 동작만 담당.

```ts
// sticky-outline.spec.ts 예시
test('스크롤 시 activeId가 IntersectionObserver로 업데이트됨', async ({ page }) => {
  await page.goto('/test/feedback-page-fixture')
  await page.evaluate(() => window.scrollTo(0, 800))
  await expect(page.locator('[aria-current="true"]')).toContainText('02')
})

test('StickyRail이 viewport 내 sticky 유지됨', async ({ page }) => {
  await page.goto('/test/feedback-page-fixture')
  const railTop = await page.locator('[data-testid="sticky-rail"]').boundingBox()
  await page.evaluate(() => window.scrollTo(0, 600))
  const railTopAfter = await page.locator('[data-testid="sticky-rail"]').boundingBox()
  expect(railTopAfter?.y).toBeLessThanOrEqual(railTop?.y ?? 0 + 44)
})
```

**분리 원칙**: Tier 1은 `vitest run`에 포함 (CI fast lane). Tier 2는 `playwright test --project=component` (CI slow lane, PR merge 전 실행).

### Self-Check 체크리스트 (frontend-design-rules.md 8항 + 플랜 Verification 18항)

#### frontend-design-rules.md 8항

- [ ] primary 색상이 purple/indigo인가? → Amber/Ochre/Red/Sage만 사용
- [ ] 폰트가 Inter/Roboto인가? → Pretendard + optional Fraunces만 사용
- [ ] hero 아래 3-column icon grid가 있는가? → Recipe A/B/C/D로 교체
- [ ] 모든 카드에 backdrop-blur가 있는가? → UtilityBar 1개에만
- [ ] dark mode에 pure black/white를 쓰는가? → off-black/off-white 확인
- [ ] `transition-all`을 남발했는가? → 필요 property만 명시
- [ ] lorem ipsum / 영문 placeholder가 남아있는가? → 한국어 컨텍스트로 교체
- [ ] 이 화면이 다른 SaaS로도 쓸 수 있는가? → ChapterMarker/3-pane/rail로 차별화

#### 플랜 Verification 18항 (원본) + 리뷰 반영 18항 체크리스트

**원본 18항**

- [x] "Why" 섹션이 사용자 피드백 + 실측 5개 원인에 근거
- [x] `frontend-design-rules.md` self-check 8항 체크리스트 포함
- [x] 레이아웃 원칙 L1–L6 전부 스펙에 반영
- [x] 각 Priority 1 페이지에 Before ASCII + After ASCII + 컬럼 점유 수치 + 반응형 recipe
- [x] 모든 토큰 변경에 Before/After 값 + 적용 위치 (파일:line)
- [x] Semantic color 4종 분리 정의 + 충돌 방지 규칙
- [x] Layout primitive 6개 props + 반응형 fallback + 용법 코드
- [x] Phase B primitive 도입 순서 명시 (PageGrid/ReadingColumn 먼저)
- [x] 터치 타겟 44px 이상 보장 규칙 (utility bar, interview controls)
- [x] 이미지 자산 정책: 5종 + 제작 방법(A/B/C) 결정 + 관리 규칙
- [x] DESIGN.md 관계 매트릭스 (승계/개선/축소/확장/신규) 완비
- [x] 성공 기준 4항 (고유성 테스트 / A/B 평가 / 기술 지표 / 원칙 준수율)
- [x] 마이그레이션 매트릭스 Priority 1 대상 Keep/Refactor/Compose/Replace/Drop 판정 완료
- [x] Fraunces/영문 serif optional 명시 + 한국어 대체 위계 전략 (§3.2)
- [x] Interview 컨트롤 발견성 규칙 (항상 visible, 모바일 56px bar, ring/pulse 삭제)
- [x] 4-Phase Roadmap에 phase별 gate 존재 (Phase A gate = 정량 3기준)
- [x] WCAG 4.5:1 contrast 수치 포함 (§3.1 대비 수치)
- [x] "이름만 바꾸면 다른 SaaS로 쓸 수 있는가?" 테스트 → ChapterMarker/3-pane으로 차별화 확보

**리뷰 반영 18항 (P0×4 + P1×4 + P2×10)**

- [x] P0-1: Tailwind `theme.extend.colors` hsl(var(--...)) 매핑 코드 스니펫 포함 (§3.1)
- [x] P0-1: `duration-[var(--duration-fast)]` 올바른 arbitrary value 문법 (§3.5, §6.1)
- [x] P0-2: StickyOutline 3종 compound 분할 (Desktop/TabBar/MobileSheet), 타입 인터페이스 각각 정의 (§5.3)
- [x] P0-3: `--utility-bar-height` CSS 변수 신설, mobile override, top-[44px] 전면 치환 (§3.1, §4.3)
- [x] P0-4: `<StickyRail>` layout primitive 분리, `<VideoDock>` feedback composite으로 재구조 (§5.4, §10 Phase B)
- [x] P1-5: ChapterMarker 숫자 11px over-line caption으로 격하, 질문 제목 display-lg(40–48px) 승격 (§5.6, §7.1)
- [x] P1-6: 3-pane 비율 `2+7+3` → `2+6+4`, ReadingColumn `max-w-[55ch]` + `leading-1.65`, `hasIssue` prop (§4.3, §5.2, §5.3)
- [x] P1-7: Phase A gate 정량 기준 3개 (axe-core=0, WCAG 재측정, vitest 회귀 0건) (§11)
- [x] P1-8: `--interview-stage` semantic 토큰, `bg-interview-stage` 치환, 350ms crossfade 규칙, 몰입 리플레이 모드 예약 (§3.1, §3.5, §7.2)
- [x] P2-9: §6.5 Empty/Loading/Error State 섹션 신설, primitive별 4-state, `VideoDockProps.state?` (§6.5)
- [x] P2-10: Motion 사용 예시 3가지 + `prefers-reduced-motion` 전역 override (§3.5)
- [x] P2-11: VideoDock CLS 대응 — `aspect-video` 예약, placeholder box, `max-h overflow-y-auto` (§5.4)
- [x] P2-12: SelectionCard Merge → Compose 재분류, step wrapper 유지 + composition 예시 (§6.4, §10)
- [x] P2-13: `review-coach-mark` Drop 대체 — dismissible callout + `localStorage rehearse.feedbackOnboarded` (§10)
- [x] P2-14: Font loading 전략 — `font-display: optional`, Google Fonts `&text=` subset, `index.html` 위치 (§3.2)
- [x] P2-15: `font-tabular` 필수 적용 컴포넌트 5개 목록 (§3.2)
- [x] P2-16: IntersectionObserver `rootMargin: "-44px 0px -80% 0px"` 권장 (§5.3)
- [x] P2-17: `question-list.tsx` → StickyOutline props 매핑 테이블, `meta?` 확장 슬롯 (§10)
- [x] P2-18: 테스트 전략 2-tier (Tier1 RTL 렌더 + Tier2 Playwright scroll/sticky) (§13)

---

## 14. 2026-04-18 Designer Critique 반영 로그

본 스펙은 senior designer 관점의 2차 review를 거쳐 P0 5건이 추가 반영되었다.

| # | 문제 | 수정 |
|---|------|------|
| D-1 | Cal Sans + Fraunces + Pretendard 3-폰트 햇지 구조 (§3.2 196줄 "섹션 내 혼용 금지" 룰이 스멜) | **Cal Sans 제거, Fraunces 영문 display 단일화** — `tailwind.config.js` `fontFamily.display`를 Fraunces 스택으로 재포인트. 실제 `src/**/*.tsx`에서 `font-display` 사용처 0건 확인, 마이그레이션 비용 실효 없음. (§1.5 / §3.2 / §9) |
| D-2 | `--accent-editorial #b8741a`(ochre)와 `--signal-warning #d4a017`(ochre) 색상 충돌, "8px 인접 금지" 룰은 스멜 | **accent-editorial을 테라코타 `#a65131`(hue 13°)로 재지정** — signal-warning(hue 43°)과 색상환 ≥30° 이격. 인접 금지 룰 폐기. (§3.1 / `index.css`) |
| D-3 | "editorial 롱폼 리딩" 선언과 §4.3·§5.2의 scan-first 축소(`55ch`, `leading-1.65`, VideoDock 3→4)가 모순 | **editorial의 의미를 "타이포그래피 위계의 정확함"으로 재정의** — long-form 독서 프레이밍 제거, scan-first 전환을 명시적 논지로 격상. (§1 목표 + 신규 "Editorial vs Scan" 섹션) |
| D-4 | `.dark` 블록에 semantic `-bg` 토큰 누락 — light의 95% L tint가 다크 `#14130f` 위에서 과발광 | **dark 카운터파트 8종 추가** (`accent-editorial`, `signal-record/warning/success` × `-bg`) + `--interview-stage` 다크값. 테마 기본값 "light-default, system-following" 명시. (§3.1 / `index.css`) |
| D-5 | UtilityBar / StickyOutline.TabBar의 `backdrop-blur-sm`이 프로젝트 anti-slop rule(`glassmorphism 금지`) 위반 + 모션 인벤토리 3건에 불과 | **backdrop-blur 제거 → `bg-background + border-b`로 대체**. 모션 인벤토리 3→6종 확장(Modal, Toast, Focus Ring 추가). page-enter 부재를 "의도된 조용함"으로 명시. (§3.5 / §5.3 TabBar / §5.5 UtilityBar / `utility-bar.tsx` / `tab-bar.tsx`) |

### 반영되지 않은 리뷰 제안

- **§10 Share/OG 이미지 정책**: Priority 2 (Home·Dashboard) 작업 시점에 별도 스펙으로 분리 예정. 본 스펙에서는 §8 Asset Policy의 제품 스크린샷 정책 범위 유지.
- **UtilityBar offline state / ChapterMarker locked-upcoming state**: 기능 스펙 영역이므로 해당 기능 도입 시 각 페이지 스펙에서 별도 정의.
