# 프론트엔드 디자인 상태 진단 (Audit)

> 작성일: 2026-04-17
> 상태: Completed
> 담당: designer agent

---

## 1. 인벤토리

### 1.1 색상

**하드코딩 hex 색상 — 총 64건, 16개 파일**

| 파일 | 건수 | 주요 값 |
|------|------|---------|
| `components/home/pain-points-section.tsx` | 11 | `#6366F1` (SVG stroke/fill) ×11 (대소문자 혼용 포함) |
| `components/feedback/feedback-panel.tsx` | 13 | `bg-[#...]`, `text-[#...]` 혼재 |
| `components/feedback/review-coach-mark.tsx` | 6 | `#6366F1` ring/gradient + `#8B5CF6` gradient |
| `components/ui/character.tsx` | 7 | `#6366F1` ×3 (color prop 하드코딩) |
| `components/ui/login-modal.tsx` | 5 | `#24292e` (GitHub), 퍼플 ring |
| `components/review/review-list-filter-bar.tsx` | 6 | `#6366F1` focus ring ×3, `#E2E8F0`, `#334155` |
| `components/feedback/question-list.tsx` | 3 | `#6366F1` bg, text, badge |
| `components/feedback/bookmark-toggle-button.tsx` | 3 | `#6366F1` hover/ring ×2 |
| `components/ui/logo.tsx` | 4 | 브랜드 색상 |
| `components/ui/logo-icon.tsx` | 3 | 브랜드 색상 |
| `pages/interview-page.tsx` | 5 | `#1a1a1a`, `#F9AB00` (경고), `#2c2c2c`, `#3c4043` |
| `components/interview/finishing-overlay.tsx` | 4 | studio 계열 |
| `components/home/hero-section.tsx` | 2 | `to-indigo-50/30` |
| `components/home/video-feedback-section.tsx` | 1 | `to-indigo-50/40` |
| `components/dashboard/interview-table.tsx` | 2 | `bg-indigo-100`, `bg-violet-100` |

**퍼플/인디고/바이올렛 하드코딩 집계**

| 패턴 | 건수 | 위치 |
|------|------|------|
| `#6366F1` (literal, 대소문자 무시) | 28 | pain-points ×11, character ×3, question-list ×3, bookmark-toggle ×2, review-coach-mark ×6 (ring ×3 + gradient ×1 + focus-ring ×2), review-list-filter-bar ×3 |
| `#8B5CF6` (violet) | 3 | review-coach-mark gradient |
| `indigo-` Tailwind class | 3 | hero-section, video-feedback-section, interview-table |
| `violet-` Tailwind class | 3 | dev-tailored-section ×2, interview-table ×1 |
| `purple-` Tailwind class | 0 | — |

**총 퍼플 계열 하드코딩: 37건, 8개 파일** (실측 `grep -rni "#6366F1" frontend/src` = 28 + 인디고/바이올렛 Tailwind class 9)

**rgba/rgb/hsl — 총 6건, 3개 파일**

| 파일 | 건수 |
|------|------|
| `components/home/hero-section.tsx` | 1 |
| `components/ui/button.tsx` | 4 |
| `components/home/video-feedback-section.tsx` | 1 |

**Tailwind 임의값 색상(`bg-[`, `text-[`, `border-[`) — 총 152건, 40개 파일**

상위 핫스팟:

| 파일 | 건수 |
|------|------|
| `components/feedback/feedback-panel.tsx` | 13 |
| `components/home/video-feedback-section.tsx` | 17 |
| `components/review/answer-comparison-view.tsx` | 8 |
| `components/feedback/content-tab.tsx` | 8 |
| `components/feedback/delivery-tab.tsx` | 11 |
| `components/feedback/question-list.tsx` | 8 |
| `pages/interview-page.tsx` | 11 |
| `components/review/review-list-filter-bar.tsx` | 5 |
| `components/review/review-bookmark-card.tsx` | 5 |
| `components/feedback/coaching-card.tsx` | 4 |

### 1.2 폰트

| 선언 위치 | 값 |
|-----------|-----|
| `index.css` `@import` | `cdn.jsdelivr.net/gh/orioncactus/pretendard` (Variable, dynamic-subset) |
| `tailwind.config.js` `fontFamily.sans` | `['Pretendard Variable', 'Pretendard', '-apple-system', ...]` |
| `tailwind.config.js` `fontFamily.mono` | `['JetBrains Mono', 'Consolas', 'monospace']` |
| `index.html` preload | 없음 (CDN import만 있음) |
| Google Fonts import | 없음 |

`font-mono` 클래스 사용 패턴 (label/badge 용도로 페이지 전체 남용):
- `font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent` — 섹션 레이블 패턴으로 19곳에서 동일하게 반복 (pain-points, video-feedback, metrics, dev-tailored, journey, before-you-start, faq-section, step-duration/position/tech-stack/level/interview-type 등)

**DESIGN.md 목표 대비**: Cal Sans Display 폰트 미도입. `font-sans`가 Pretendard로 고정되어 있어 Display / Body 계층 분리 없음. `font-mono`가 JetBrains Mono로 설정되어 있어 코드 맥락에선 적합하나, 레이블 레이아웃에서 남용 중.

### 1.3 컴포넌트 중복

**`components/ui/` 13개 커스텀 primitive 현황**

| 컴포넌트 | 주요 의존 | 비고 |
|----------|-----------|------|
| `button.tsx` | Spinner | accent/rounded-button 직접 참조 |
| `text-input.tsx` | — | rounded-button 사용 |
| `login-modal.tsx` | Logo, BetaBadge | rounded-[28px] 하드코딩, shadow-toss-lg |
| `selection-card.tsx` | — | rounded-card, accent-light |
| `skeleton.tsx` | — | 단순 |
| `spinner.tsx` | — | 단순 |
| `back-link.tsx` | — | 단순 |
| `beta-badge.tsx` | — | bg-\[...] 임의값 |
| `logo.tsx` | — | SVG, 하드코딩 색상 4건 |
| `logo-icon.tsx` | — | SVG, 하드코딩 색상 3건 |
| `character.tsx` | — | SVG, `#6366F1` 3건 |
| `password-protected-route.tsx` | — | 라우팅 로직, UI 없음 |
| `protected-route.tsx` | — | 라우팅 로직, UI 없음 |

**페이지 내 인라인 `<button>` 패턴 (컴포넌트 미사용)**

`<Button>` 컴포넌트를 import하지 않고 `<button className="...rounded-button bg-accent...">` 패턴이 최소 12개 페이지/컴포넌트에서 발견됨 (dashboard-page, interview-ready-page, interview-feedback-page, interview-analysis-page 등). 스타일 드리프트의 핵심 원인.

### 1.4 하드코딩 spacing/radius

**임의값(`p-[`, `m-[`, `gap-[`, `w-[`, `h-[`, `rounded-[`) — 총 49건, 23개 파일**
(집계 기준: 위 6개 패턴 합산. `rounded-[` 단독은 32건/20파일 — 실측 `grep -rn "rounded-\[" frontend/src`)

상위 핫스팟:

| 파일 | 건수 | 예시 |
|------|------|------|
| `pages/interview-page.tsx` | 4 | `w-[300px]`, `w-[320px]`, `h-[320px]`, `max-w-2xl` |
| `pages/interview-feedback-page.tsx` | 5 | `rounded-[32px]`, `lg:w-[60%]`, `lg:w-[40%]` |
| `pages/interview-analysis-page.tsx` | 2 | `rounded-[24px]` ×2 |
| `pages/interview-ready-page.tsx` | 2 | `rounded-[24px]` ×2 |
| `components/home/hero-section.tsx` | 3 | `rounded-[24px]`, `h-[...]` |
| `components/home/video-feedback-section.tsx` | 3 | `rounded-[20px]` |
| `components/setup/resume-upload.tsx` | 3 | `h-[...]`, `w-[...]` |
| `components/home/metrics-section.tsx` | 2 | 임의 spacing |

**`rounded-card`(20px) 사용**: 9건 (selection-card.tsx 내부 + 페이지들)
**`rounded-button`(24px) 사용**: 55건 이상, 30개 파일 (전체 인터랙티브 요소에 일괄 적용)
**`transition-all` 남용**: 66건, 30개 파일 — DESIGN.md/frontend-design-rules.md 모두 금지

---

## 2. Gap 분석 (현재 vs DESIGN.md)

| 항목 | 현재 값 | DESIGN.md 목표 | 영향 파일 수 | 우선순위 |
|------|---------|----------------|-------------|---------|
| `accent.DEFAULT` | `#6366F1` (Electric Violet) | Charcoal `#242424` (또는 모노크롬 accent 없음) | 40+ | **H** |
| `accent.hover` | `#4F46E5` | 없음 (불투명도 0.7 hover) | 10+ | **H** |
| `accent.light` | `#EEF2FF` (연보라) | 없음 (불필요) | 8 | **H** |
| `tutorial-ring` keyframe | `rgba(99,102,241,...)` 퍼플 | 제거 또는 모노크롬 ring | 1 (tailwind.config.js) | **H** |
| `review-coach-mark` gradient | `from-[#6366F1] to-[#8B5CF6]` | 모노크롬 대체 | 1 | **H** |
| `character.tsx` fill color | `#6366F1` ×3 | neutral/charcoal | 1 | **H** |
| `pain-points-section.tsx` SVG stroke | `#6366F1` ×8 | `currentColor` 또는 `#242424` | 1 | **H** |
| `fontFamily.sans` | Pretendard (body only) | Pretendard(한글) + Cal Sans(Display hero) | 전체 | **M** |
| Display font 미도입 | 없음 | Cal Sans (Google Fonts) | hero, 섹션 헤딩 | **M** |
| `borderRadius.button` | `24px` | Cal scale 6–8px | 30개 파일 | **M** |
| `borderRadius.card` | `20px` | Cal scale 8–12px | 9개 파일 | **M** |
| `boxShadow.toss` | `0 8px 16px rgba(0,0,0,0.04)` | Cal Level 2 multi-layer shadow | 13개 파일 | **M** |
| `boxShadow.toss-lg` | `0 16px 32px rgba(0,0,0,0.08)` | Cal Level 2 또는 Level 3 | 5개 파일 | **M** |
| `background` 토큰 | `#F1F5F9` (Slate-100) | `#ffffff` (pure white) | 전체 | **M** |
| `text.primary` | `#0F172A` | `#242424` Charcoal | 전체 | **M** |
| `text.secondary` | `#334155` | `#898989` Mid Gray | 전체 | **M** |
| `transition-all` 남용 | 66건 | `transition-colors`/`transition-transform` 개별 지정 | 30개 파일 | **M** |
| `studio.bg` | `#202124` | off-black `#0a0a0a`~`#141414` 범위 | interview-page | **L** |
| `font-mono` 레이블 패턴 | JetBrains Mono 19곳 남용 | Inter 또는 전용 label 토큰 | 19곳 | **L** |
| CSS variable 레이어 | 없음 | `:root` CSS custom properties (light/dark) | 전체 | **M** |
| shadcn/ui | 미도입 | Phase 2 기준 primitive 표준 | 전체 | **M** |
| `meet.green`/`meet.red` | `#00AC47`, `#EA4335` | 유지 (interview 전용 semantic) | interview-page | **L** |
| `indigo-50/30`, `indigo-50/40` gradient | hero, video-feedback bg | 제거 → flat white or subtle gray | 2 | **H** |
| `bg-indigo-100 text-indigo-700` | interview-table badge | 모노크롬 badge 대체 | 1 | **H** |
| `bg-violet-100 text-violet-700` | interview-table badge | 모노크롬 badge 대체 | 1 | **H** |
| `bg-violet-100`/`text-violet-600` | dev-tailored-section | 모노크롬 대체 | 1 | **H** |

---

## 3. 페이지 카드 (14개)

### 3.1 home-page (`/`)

**주요 섹션**
- sticky 헤더: Logo + BetaBadge + 로그인/로그아웃 버튼
- HeroSection: CTA 버튼, 제품 프리뷰 영역
- PainPointsSection: `#6366F1` SVG 아이콘 ×3개 세트 (3-column 아이콘 그리드 패턴)
- VideoFeedbackSection: 제품 데모 영상 영역, `indigo-50/40` 그라데이션 배경
- MetricsSection, DevTailoredSection, JourneySection, BeforeYouStartSection, FaqSection, CtaSection
- footer: 링크, GitHub, 이메일

**사용 UI primitive**: Logo, BetaBadge, (Button 컴포넌트 미사용 — 인라인 `<button>`)

**DESIGN.md 위반**
- PainPointsSection: 3-column 아이콘 그리드 (frontend-design-rules 금지 패턴)
- SVG stroke `#6366F1` 하드코딩 8건
- `font-mono text-[10px] text-accent` 섹션 레이블 19회 반복
- `indigo-50/40` 그라데이션 배경
- `selection:bg-accent/10` — accent 퍼플 노출
- `focus-visible:ring-accent` 전체 (퍼플 focus ring)
- `rounded-[24px]` 히어로 프리뷰 카드

**난이도**: L — 섹션 수 많음 + SVG 색상 직접 교체 + 3-column 레이아웃 구조 변경 필요

---

### 3.2 dashboard-page (`/dashboard`)

**주요 섹션**
- AppShell 래핑 (사이드바 + 헤더 포함)
- 인사 섹션 (데스크탑)
- StatsCards: 통계 3개 카드
- InterviewTable (데스크탑) / InterviewList (모바일)
- ServiceFeedbackModal ×2

**사용 UI primitive**: (없음 — AppShell/DashboardHeader/StatsCards 등 도메인 컴포넌트)

**DESIGN.md 위반**
- `bg-accent text-white rounded-button` 인라인 버튼 ×2 ("+ 새 면접")
- `text-accent hover:bg-accent-light` ("피드백 보내기")
- `rounded-button`(24px) → Cal scale 8px 필요
- `transition-all cursor-pointer` — transition-all 남용

**난이도**: S — 인라인 버튼 교체 + accent 토큰 교체로 자동 반영됨

---

### 3.3 interview-page (`/interview/:id/conduct`)

**주요 섹션**
- 로딩 상태: studio-bg 전체 화면
- 메인 Video Area: AI 면접관 타일 (Google Meet PIP 레이아웃)
- InterviewerAvatar (중앙), 질문 오버레이 (하단)
- User Video PIP (우하단)
- Bottom Control Bar: InterviewTimer + InterviewControls + 종료 버튼
- Finish Dialog (modal)
- FinishingOverlay, UploadRecoveryDialog
- 이탈 가드 Dialog

**사용 UI primitive**: (없음 — 모두 인라인 또는 interview 전용 컴포넌트)

**DESIGN.md 위반**
- `bg-[#1a1a1a]`, `bg-[#2c2c2c]`, `border-[#3c4043]` 하드코딩 (studio 토큰 있으나 직접 하드코딩 혼재)
- `rounded-2xl bg-[#2c2c2c]` dialog — studio 토큰 미사용
- `transition-all` 8건
- `bg-black/60`, `bg-black/70` — pure black 사용
- `text-[13px]`, `text-[11px]`, `text-[10px]` 임의 텍스트 사이즈 5건
- `w-[300px]`, `w-[320px]`, `h-[320px]` 임의 크기 3건

**난이도**: M — interview 전용 스튜디오 다크 UI로 DESIGN.md 적용 범위 제한적, 주로 토큰 정리와 하드코딩 교체

---

### 3.4 interview-setup-page (`/interview/setup`)

**주요 섹션**
- 헤더: Logo + BackLink
- SetupProgressBar (5단계)
- Step 컴포넌트 5개: StepPosition, StepTechStack, StepLevel, StepDuration, StepInterviewType
- SetupNavigation (하단 CTA 버튼)

**사용 UI primitive**: Logo, BackLink

**DESIGN.md 위반**
- 모든 Step 내부: `font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent` 섹션 레이블 패턴
- `bg-accent/10 text-accent` 선택된 상태 배지
- SetupNavigation 내 `<button>` 인라인 — Button 컴포넌트 미사용
- `rounded-[24px]` submit 버튼 (interview-ready에서 확인)
- `transition-all` 3건

**난이도**: M — Step 컴포넌트 5개 전수 + accent 토큰 일괄 교체 후 자동 반영

---

### 3.5 interview-ready-page (`/interview/:id/ready`)

**주요 섹션**
- 헤더: Logo + BackLink
- 섹션 레이블 + 대제목 ("장치를 확인하고 시작하세요.")
- 면접 설정 태그 바 (포지션, 레벨, 타입, 시간)
- 질문 생성 상태 카드
- DeviceTestSection (카메라/마이크/스피커 3개 테스트 행)
- 시작 버튼 (대형 CTA)

**사용 UI primitive**: Logo, BackLink, Character, (Button 미사용 — 인라인)

**DESIGN.md 위반**
- `h-16 rounded-[24px] bg-accent font-black text-white shadow-lg shadow-accent/20` — accent+24px radius 조합
- `rounded-[24px]` 버튼 ×2
- `text-accent` 섹션 레이블 + 스피너 + 태그
- `bg-accent/10 text-accent` 태그 배지
- `font-mono text-[10px]` 레이블 패턴
- `h-5 w-5 animate-spin border-accent` 로더
- `transition-all` 3건

**난이도**: M — 주요 CTA가 accent에 강하게 결합되어 있어 토큰 교체 후 세부 검수 필요

---

### 3.6 interview-feedback-page (`/interview/:publicId/feedback`)

**주요 섹션**
- sticky 헤더: Logo + 대시보드 링크
- InterviewInfoBar: 면접 정보 요약 (포지션, 타입, 시간, 날짜)
- 히어로 섹션: 타이틀 + 설명
- QuestionSetSection ×N (각각 비디오+타임라인+질문목록+피드백패널)
- 로딩/에러 상태 (Character 컴포넌트)

**사용 UI primitive**: Logo, Character

**DESIGN.md 위반**
- `rounded-full bg-accent text-white` 섹션 번호 뱃지
- `bg-accent/10 px-2 py-0.5 text-xs text-accent` 인터뷰 타입 배지
- `rounded-[32px]` 카드 1건
- `h-full bg-accent animate-progress-loading` 로더 바
- `font-mono text-[10px] text-accent` 섹션 레이블
- `transition-all` 5건 (QuestionSetSection 포함)
- `rounded-[24px] bg-accent` 버튼 (에러/빈상태 CTA)
- ReviewCoachMark: `from-[#6366F1] to-[#8B5CF6]` gradient 버튼/배지 (최대 위반)

**난이도**: L — ReviewCoachMark의 퍼플 gradient 전면 교체 + QuestionSetSection 구조 복잡

---

### 3.7 interview-analysis-page (`/interview/:publicId/analysis`)

**주요 섹션**
- sticky 헤더: Logo (accent 배경 아이콘)
- 모범답변 섹션: ModelAnswerSection ×N
- 분석 상태 플로팅 UI (AnalysisStatusFloat)
- 로딩/완료/스킵 상태별 분기 화면

**사용 UI primitive**: Logo, Character

**DESIGN.md 위반**
- `bg-accent shadow-lg shadow-accent/20` 헤더 Logo 래퍼
- `rounded-[24px] bg-accent` 버튼 ×3
- `border-accent border-t-transparent` 스피너
- `font-bold text-accent` 분석 진행 레이블
- `bg-accent` progress bar, 완료 배지
- `font-mono text-[10px] text-accent` 레이블
- `transition-all` 6건

**난이도**: M — AnalysisStatusFloat 단독 분리 가능, accent 교체로 대부분 해결

---

### 3.8 review-list-page (`/reviews`)

**주요 섹션**
- AppShell 래핑
- ReviewListHeader: 북마크 총 수
- ReviewListFilterBar: 상태/포지션/타입 필터
- ReviewCategorySection ×N (카테고리별 북마크 카드 목록)
- ReviewEmptyState

**사용 UI primitive**: (없음)

**DESIGN.md 위반**
- ReviewListFilterBar: `focus-visible:ring-[#6366F1]` ×3건 하드코딩
- `border-[#E2E8F0]`, `text-[#334155]` 하드코딩 (토큰 있으나 직접 하드코딩)
- `rounded-xl` select dropdown (Cal scale 준수 가능)
- `transition-all` 6건 (filter-bar 포함)

**난이도**: S — focus ring 교체 + 하드코딩 2~3개 토큰화로 완료

---

### 3.9 about-page (`/about`)

**주요 섹션**
- ContentPageShell 래핑 (헤더/푸터 공통)
- h1 타이틀
- Section ×4: 우리가 만든 이유, 차별점, 누구를 위해, 앞으로의 방향
- CTA 블록: `bg-gray-50 border-border/50` + accent 버튼

**사용 UI primitive**: (없음 — ContentPageShell 사용)

**DESIGN.md 위반**
- `bg-accent text-white rounded-md` CTA 버튼 (인라인 `<Link>`)
- `bg-gray-50` 하드코딩 배경 (surface 토큰 미사용)
- `hover:opacity-90` — 토스/AI 슬롭 hover 패턴

**난이도**: S — CTA 버튼 1개 교체, gray-50 토큰화

---

### 3.10 admin-feedbacks-page (`/admin/feedbacks`)

**주요 섹션**
- 전체 bg-background 레이아웃
- 헤더: h1 타이틀 + 총 수
- 데스크탑: 테이블 뷰 (shadow-toss 카드 + 행)
- 모바일: 카드 뷰 (shadow-toss 카드)
- 페이지네이션

**사용 UI primitive**: Spinner

**DESIGN.md 위반**
- `bg-surface rounded-2xl border border-border shadow-toss` — shadow-toss 교체 대상
- `shadow-toss` ×2건 (테이블/카드 컨테이너)
- `bg-blue-100 text-blue-700` SourceBadge (자동)
- `bg-green-100 text-green-700` SourceBadge (자발적)
- `text-amber-400` 별점 — 시맨틱 색상 (유지 가능하나 검토 필요)
- `rounded-2xl` — Cal scale 12px 해당, 허용 범위 내

**난이도**: S — shadow-toss 교체 + badge 색상 검토

---

### 3.11 faq-page (`/faq`)

**주요 섹션**
- ContentPageShell 래핑
- h1 타이틀 + 설명
- FAQ 목록 (10개 Q&A, border-b 구분)
- 하단 CTA 블록

**사용 UI primitive**: (없음)

**DESIGN.md 위반**
- `bg-accent text-white rounded-md` CTA 버튼
- `bg-gray-50` 배경 하드코딩
- `hover:opacity-90` — AI 슬롭 hover 패턴

**난이도**: S — about-page와 동일 패턴

---

### 3.12 guide (폴더 — 3개 SEO 페이지)

`/guide/ai-mock-interview`, `/guide/developer-interview-prep`, `/guide/resume-based-interview`

**주요 섹션**
- ContentPageShell 래핑
- `prose prose-neutral` Tailwind Typography 플러그인 사용
- 아티클 본문 (h1, h2, p, ul 등)

**사용 UI primitive**: (없음)

**DESIGN.md 위반**
- `prose prose-neutral` — Tailwind Typography의 기본 링크 색상이 accent와 충돌 가능
- ContentPageShell 내 CTA 버튼이 accent 사용 중인 경우 연쇄 위반
- 기본 prose 타이포그래피가 DESIGN.md 스케일과 불일치

**난이도**: S — ContentPageShell CTA 일괄 교체 + prose 커스터마이징

---

### 3.13 privacy-policy-page (`/privacy`)

**주요 섹션**
- sticky 헤더: Logo + BetaBadge + "← 홈으로" 링크
- 제목 + 시행일자
- 베타 서비스 고지 (amber 배너)
- 목차 nav
- 본문 Section ×10개
- `<code>` 인라인 코드 블록

**사용 UI primitive**: Logo, BetaBadge

**DESIGN.md 위반**
- `focus-visible:ring-accent` ×3건 (헤더 링크, 목차 앵커)
- `hover:text-accent` 목차 링크 hover
- `border-amber-200 bg-amber-50 text-amber-800` 베타 고지 배너 — 시맨틱이지만 accent와 별도 하드코딩
- `font-mono` code inline — 유지 가능

**난이도**: S — focus ring + hover 색상 교체

---

### 3.14 not-found-page (`/404`)

**주요 섹션**
- 전체 화면 중앙 정렬 레이아웃
- "404" 텍스트
- h1 제목
- 설명 텍스트
- "홈으로 돌아가기" 링크 버튼

**사용 UI primitive**: (없음)

**DESIGN.md 위반**
- 없음 — `bg-white text-text-primary`, `rounded-xl border border-border`로 비교적 clean
- CTA가 `<Link>` 버튼으로 인라인 구현 (Button 컴포넌트 미사용)
- 특별한 비주얼 요소 없음 — DESIGN.md 기준에서 허용 가능

**난이도**: S — 최소 수정 대상

---

## 4. 컴포넌트 → shadcn 매핑

| 기존 컴포넌트 | shadcn 매핑 | Phase | 판단 | 사유 |
|--------------|-------------|-------|------|------|
| `button.tsx` | `Button` | 3a | 교체 | 4개 variant 모두 shadcn `Button` + `variant` prop으로 수렴. accent 토큰 교체 후 자동 반영 |
| `text-input.tsx` | `Input` | 3b | 교체 | 단순 wrapper, shadcn `Input` + label 패턴으로 대체 가능 |
| `login-modal.tsx` | `Dialog` | 3c | 교체 | `rounded-[28px]`, `shadow-toss-lg` 하드코딩. shadcn Dialog로 포털/접근성 표준화 |
| `selection-card.tsx` | (커스텀 유지) | — | 유지 | radio 역할 카드로 shadcn에 직접 대응 없음. accent 토큰 교체 후 스타일만 수정 |
| `skeleton.tsx` | `Skeleton` | 3a | 교체 | shadcn Skeleton이 동일 기능 제공 |
| `spinner.tsx` | (유지) | — | 유지 | 단순 spin 애니메이션. shadcn에 Spinner 없음 (Loader2 아이콘으로 대체 가능하나 현재 유지) |
| `back-link.tsx` | (유지) | — | 유지 | 라우팅+아이콘 조합 단순 컴포넌트. 교체 효익 없음 |
| `beta-badge.tsx` | (유지) | — | 유지 | 브랜드 전용 뱃지. 토큰 교체 후 스타일 정리로 충분 |
| `logo.tsx` | (유지) | — | 유지 | SVG 브랜드 자산. 내부 하드코딩 색상만 교체 |
| `logo-icon.tsx` | (유지) | — | 유지 | 위 동일 |
| `character.tsx` | (유지) | — | 유지 | SVG 마스코트. `#6366F1` fill → charcoal/neutral 교체만 필요 |
| `password-protected-route.tsx` | (유지) | — | 유지 | 라우팅 로직만, UI 없음 |
| `protected-route.tsx` | (유지) | — | 유지 | 라우팅 로직만, UI 없음 |

**Phase 3 교체 대상**: button, text-input, login-modal, skeleton (4개)
**토큰 교체 후 스타일 수정**: selection-card, beta-badge, logo, logo-icon, character (5개)
**변경 불필요**: spinner, back-link, password-protected-route, protected-route (4개)

---

## 5. 우선순위 요약

### H (즉시 처리 — 규칙 위반)

1. `accent.DEFAULT: #6366F1` → `#242424` (또는 CSS var `--color-accent`) — tailwind.config.js 1건 교체로 40+ 파일 자동 반영
2. `accent.hover: #4F46E5` → 삭제 또는 `#111111`
3. `accent.light: #EEF2FF` → 삭제 또는 `rgba(0,0,0,0.05)`
4. `tutorial-ring` keyframe `rgba(99,102,241,...)` → 중립 ring으로 교체
5. `pain-points-section.tsx` SVG `#6366F1` ×8 → `currentColor` 또는 `#242424`
6. `character.tsx` fill `#6366F1` ×3 → neutral
7. `review-coach-mark.tsx` `from-[#6366F1] to-[#8B5CF6]` gradient → 모노크롬
8. `review-list-filter-bar.tsx` `ring-[#6366F1]` ×3 → CSS var
9. `review-coach-mark.tsx:274` `ring-[#6366F1]/70` + `:324`·`:350` `focus-visible:ring-[#6366F1]` → 모노크롬 ring
10. `interview-table.tsx` `bg-indigo-100/violet-100` badge → 모노크롬
11. `dev-tailored-section.tsx` `bg-violet-100/text-violet-600` → 모노크롬
12. `hero-section.tsx` `to-indigo-50/30`, `video-feedback-section.tsx` `to-indigo-50/40` → 제거

### M (Phase 2~3 처리)

1. `fontFamily`: Cal Sans Display 추가 (Google Fonts) — hero/섹션 헤딩에 적용
2. `borderRadius.button` 24px → 8px (Cal scale)
3. `borderRadius.card` 20px → 12px (Cal scale)
4. `boxShadow.toss`/`toss-lg` → Cal Level 2 multi-layer shadow
5. `background` 토큰 `#F1F5F9` → `#ffffff`
6. `text.primary` `#0F172A` → `#242424`
7. `text.secondary` `#334155` → `#898989`
8. CSS variable `:root` 레이어 신설 (light/dark 대응)
9. shadcn/ui 설치 + Button/Input/Dialog/Skeleton 교체
10. `transition-all` 66건 → `transition-colors`/`transition-transform` 개별 지정
11. 인라인 `<button>` 12개 페이지 → `<Button>` 컴포넌트 통일
12. `font-mono text-[10px] text-accent` 레이블 패턴 19곳 → 디자인 토큰 기반 `<SectionLabel>` 컴포넌트화

### L (Phase 3 후반~선택)

1. `studio.bg: #202124` → `#0f1011` (off-black 범위)
2. `meet.green`/`meet.red` — 유지 (interview 전용 semantic 색상)
3. `font-mono` 레이블 남용 정리 (Cal Sans Display 도입 후 재검토)
4. guide 폴더 prose 타이포그래피 DESIGN.md 스케일 맞춤
5. about/faq `bg-gray-50` → surface 토큰

---

## 6. Phase 2 진입 권고

Audit 결과를 종합하면 **Phase 2(토큰 + shadcn init)가 모든 후속 Phase의 전제**이며, 다음 순서로 진입을 권고한다.

### Phase 2 작업 범위 (확정)

1. **CSS variable 레이어 신설** (`frontend/src/index.css`)
   - `:root` 에 DESIGN.md 기반 light 토큰 정의
   - `prefers-color-scheme: dark` 에 dark 토큰 정의 (studio 계열 포함)

2. **tailwind.config.js 전면 교체**
   - `accent.DEFAULT` `#6366F1` → `hsl(var(--color-accent))`
   - `borderRadius.button` 24px → 8px
   - `borderRadius.card` 20px → 12px
   - `boxShadow.toss`/`toss-lg` → Cal Level 2/3 shadow
   - `background` → `#ffffff`
   - `text.primary` → `#242424`
   - `text.secondary` → `#898989`

3. **퍼플 즉시 제거 (H 항목)** — tailwind.config 교체와 동시 진행
   - `tutorial-ring` keyframe 색상 교체
   - `pain-points-section.tsx` SVG stroke → `currentColor`
   - `character.tsx` fill → neutral
   - `interview-table.tsx` badge → 모노크롬
   - `dev-tailored-section.tsx` badge → 모노크롬

4. **shadcn/ui 초기화** (`npx shadcn@latest init`)
   - CSS variable 방식 선택
   - Button, Input, Dialog, Skeleton 컴포넌트 추가

### 기대 효과

- `accent` 토큰 교체 1건으로 **40+ 파일의 퍼플 색상 자동 제거**
- `borderRadius.button` 교체로 **30개 파일 radius 자동 수정**
- shadcn 도입으로 **4개 커스텀 primitive 삭제 → 생태계 표준화**
- H 항목 11건 해소 → frontend-design-rules.md 셀프체크 1~4번 통과 기반 마련

### 주의사항

- `review-coach-mark.tsx`의 `from-[#6366F1] to-[#8B5CF6]` gradient는 tailwind.config accent 교체만으로 해결 안 됨 — 별도 수동 수정 필요 (Phase 3a 또는 Phase 2 말)
- `interview-page.tsx`의 studio 다크 UI는 DESIGN.md 적용 범위 외 — `studio.*` 토큰 유지하되 off-black 범위만 조정 (L 항목)
- guide 폴더 `prose` 클래스는 Tailwind Typography 플러그인 설치 여부 확인 후 처리
