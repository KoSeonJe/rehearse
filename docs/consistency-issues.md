# Consistency Audit Issues (Plan 05)

> 작성일: 2026-04-17
> 상태: Completed
> 담당: general-purpose agent
> 스펙: `docs/plans/frontend-design-overhaul/plan-05-consistency-audit.md`
> 브랜치: `refactor/frontend-design-overhaul`

---

## Summary

- **총 이슈**: H 4건 / M 7건 / L 4건 = **15건**
- **코드 수정 0건** (문서 감사 전용). 실제 수정은 사용자 우선순위 결정 후 별도 스펙으로.
- **violet-legacy 토큰 잔존**: 93 occurrences × 40 files (src) + 2 정의(index.css) + 1 정의(tailwind.config.js) — Phase 3 기간 의도적 잔존. Phase 5 이후 제거 전제는 "사용처 0건".
- **하드코딩 #6366F1 리터럴 잔존**: `character.tsx` 3건 (violet-legacy 토큰으로 교체 누락).
- **자동 grep 7종 완료** / **9질문 × 14페이지 매트릭스 완료** / **axe-core 미실행** (환경 미구축 — 후속 과제).

### 페이지별 9질문 통과율 (요약)

- 평균 통과율: **~63%** (9질문 중 평균 5.7개 통과)
- 최고: privacy-policy / not-found / admin-feedbacks / faq (7–8/9)
- 최저: interview-page (3/9 — 스튜디오 다크 UI 의도적 보존), home (4/9 — Phase 4 Aceternity 대기)

---

## 1. 자동 grep 결과

### 1.1 하드코딩 색상 (Tailwind 임의값 + 16진수 리터럴)

**총 16건 / 6파일** — `bg-[#..]`, `text-[#..]`, `border-[#..]`, `ring-[#..]` 임의값 기준.

#### A. 스튜디오 다크 테마 (의도적 보존 — plan-03c TODO 마킹 상태)
- `frontend/src/pages/interview-page.tsx:158` — `bg-[#1a1a1a]` (비디오 배경)
- `frontend/src/pages/interview-page.tsx:185,186` — `bg-[#F9AB00]/20`, `text-[#F9AB00]` (마감 경고 뱃지, Google Meet 컬러)
- `frontend/src/pages/interview-page.tsx:306,361` — `bg-[#2c2c2c] border-[#3c4043]` (finish/exit 다이얼로그)
- `frontend/src/components/interview/upload-recovery-dialog.tsx:26` — `bg-[#2c2c2c] border-[#3c4043]`
- `frontend/src/components/interview/finishing-overlay.tsx:27,31,58,68` — `bg-[#2c2c2c] border-[#3c4043] bg-[#3c4043]` (4건)
  → Phase 3c에서 "스튜디오 다크 테마 별도 토큰 정의 후 치환" 조건으로 보류됨. TODO 주석 유지.

#### B. 브랜드 컬러 (의도적 — OAuth 제공자 브랜드)
- `frontend/src/components/ui/login-modal.tsx:64` — `bg-[#24292e]` (GitHub 공식 브랜드 컬러)
- `frontend/src/components/ui/login-modal.tsx:78-81` — `fill="#4285F4|#34A853|#FBBC05|#EA4335"` (Google 로고 SVG path — 공식 컬러, 유지 필요)

#### C. 정리 누락 (이슈)
- `frontend/src/components/review/review-bookmark-card.tsx:105` — `hover:bg-[#059669]` (border-success + hover만 하드코딩. success-hover 토큰 없음)
- `frontend/src/components/feedback/bookmark-toggle-button.tsx:97` — `border-[#C7D2FE]` (violet-legacy-light 계열 파생색, 토큰화 가능)
- `frontend/src/components/common/review-toast.tsx:83` — `focus-visible:ring-offset-[#0F172A]` (slate-900 하드코딩, `bg-slate-900`과 짝)

#### D. SVG stroke/fill (의도적 — 로고 아이덴티티)
- `frontend/src/components/ui/logo.tsx:21,37,38,51` — `#1e293b` (slate-900, 로고 오리지널)
- `frontend/src/components/ui/logo-icon.tsx:19,30,31` — `#1e293b`
- `frontend/src/components/ui/character.tsx:69,77` — `#1E293B` (stroke)
- `frontend/src/components/interview/interviewer-avatar.tsx:67,68,71` — `#5F6368 #E8EAED` (Google Material 톤, 스튜디오 UI)

#### E. 문제: #6366F1 리터럴 (정리 필요 — H 우선)
- `frontend/src/components/ui/character.tsx:18,25,46` — `color: '#6366F1'` × 3건 → **violet-legacy 토큰 전환 누락**

### 1.2 rgb/rgba/hsl 직접 사용
**총 5건 / 3파일** — 모두 의도적 유지 가능.
- `frontend/src/components/home/hero-section.tsx:71` — `rgba(0,0,0,0.03)` (radial-gradient mesh, 장식적 배경)
- `frontend/src/components/home/video-feedback-section.tsx:47` — 동일
- `frontend/src/components/ui/button-variants.ts:22,24,31` — `rgba(0,0,0,0.02)`, `rgba(0,0,0,0.1)` (shadow 임의값, shadcn default와 다르게 커스터마이즈)

### 1.3 폰트 규칙 (Inter/Roboto/Arial/Open Sans/Lato)
**0건** — frontend-design-rules.md 완전 통과. Pretendard + Cal Sans 체계 유지.

### 1.4 `transition-all` 남용
**총 19건 / 8파일**:
- `interview-page.tsx`: 8건 (스튜디오 UI, duration 500/1000/300 — 대부분 의도적)
- `interview-controls.tsx`: 4건 (Meet-style 버튼 hover/scale)
- `upload-recovery-dialog.tsx`: 2건
- `dashboard-page.tsx:119`: 1건 (하단 CTA)
- `interview-table.tsx:224`: 1건 (delete 버튼 opacity+color 동시 전이)
- `finishing-overlay.tsx:60`: 1건 (progress bar)
- `password-protected-route.tsx:69`: 1건
- `character.tsx:54`: 1건

→ **M 이슈**. 대부분 `transition-colors`, `transition-[opacity,colors]`, `transition-[background-color,transform]`로 치환 가능. 스튜디오 다크 UI(interview-page, interview-controls, upload-recovery-dialog, finishing-overlay)는 Plan 03c 보류 범위와 중복 — 해당 파일 전체 리디자인 시 일괄 처리.

### 1.5 pure black/white (bg-white, text-white, bg-black, text-black)
**총 91건 / 41파일** — 대부분 **의도적**(violet-legacy 대비 흰 글자, 스튜디오 오버레이 `bg-black/60~80`).
- `bg-white` (순수 배경) **정리 대상**:
  - `frontend/src/pages/home-page.tsx:49,63,114` (홈 최상위 bg-white, header bg-white/80, footer bg-white) → `bg-background` 토큰화 가능
  - `frontend/src/components/home/journey-section.tsx:57,98` (랜딩 섹션 bg-white)
  - `frontend/src/components/home/before-you-start-section.tsx:61`, `metrics-section.tsx:146,149,165`, `dev-tailored-section.tsx:79`, `video-feedback-section.tsx:83,158,61,93`, `pain-points-section.tsx:66`, `hero-section.tsx:75` (홈 내부 카드 bg-white)
  - `frontend/src/components/ui/protected-route.tsx:24` — `bg-white` (로딩 스크린)
  - `frontend/src/components/setup/resume-upload.tsx:89` (close 버튼)
  - `frontend/src/components/ui/login-modal.tsx:41,75` (DialogContent bg-white + Google button bg-white)
  - `frontend/src/components/dashboard/delete-confirm-dialog.tsx:35`, `service-feedback-modal.tsx:66` (쌍둥이, TODO(plan-05) 이관 상태)
- `text-white` on `bg-violet-legacy`: 의도적 (유지).
- `bg-black/60~80` (interview-page 오버레이): 의도적 (유지).
- `bg-white/80 backdrop-blur-md` (header): Cal.com 스타일 유지 의도 — 토큰 `bg-background/80`로 치환 가능.

### 1.6 영어 placeholder / Lorem ipsum
**0건** — 모두 한국어. Lorem ipsum 없음.
- `placeholder="..."` 전수: `서비스에 대한 의견을 자유롭게 남겨주세요...`, `비밀번호` 2건.
- aria-label/에러 문구 영어 유출 없음 (grep 기준).

### 1.7 legacy 토큰 (violet-legacy + #6366F1)

**violet-legacy 사용처 (src만)**: **93 occurrences × 40 files**. 상위 위치:
- 토큰 정의: `frontend/src/index.css:48,74` (light+dark), `frontend/tailwind.config.js:63-64`
- CTA/primary button: `button-variants.ts` (7건 — bg/ring/shadow), `password-protected-route.tsx:69`, `dashboard-page.tsx:119`, 가이드 페이지 3개, about/faq/analysis CTA 블록
- Setup 스텝 active 상태: `setup-progress-bar.tsx(3)`, `step-duration(1)`, `step-level(1)`, `step-position(1)`, `step-tech-stack(1)`, `step-interview-type(1)` — 선택된 카드의 퍼플 배경 (Phase 3h 의도적 잔존)
- 피드백/분석: `interview-feedback-page(6)`, `interview-analysis-page(8)` (로딩 스피너, 진행 바, 번호 뱃지, 최종 CTA)
- 대시보드/리뷰: `interview-table(9)`, `interview-card(1)`, `sidebar(2)`, `stats-cards(1)`, `dashboard-header(1)`, `empty-state(1)`, `review-bookmark-card(4)`, `review-list-filter-bar(1)`, `answer-comparison-view(2)`, `review-category-section(1)`
- 피드백 컴포넌트: `timeline-bar(3)`, `question-list(3)`, `video-player(1)`, `bookmark-toggle-button(2)`, `review-coach-mark(5)`, `spinner(1)`
- Focus ring: `selection-card(2)`, `back-link(1)`, `content-page-shell(2)`, `privacy-policy-page(5)`, `login-modal(2)`

**#6366F1 리터럴 잔존**: **3건 / 1파일** — `character.tsx:18,25,46` (color prop). 모두 violet-legacy 토큰으로 치환 가능.

### 1.8 `rounded-[N px]` 임의값 (추가 체크)
**총 31건**. 주요 패턴:
- `rounded-[32px]`: hero, journey, dev-tailored cards, feedback page surface (4건) — 의도적 대형 카드 라운드
- `rounded-[28px]`: login-modal DialogContent (TODO(plan-05) 명시됨)
- `rounded-[24px]`: ready/analysis/feedback CTA 버튼, metrics 박스 — 의도적 CTA 강조
- `rounded-[20px]`: setup step cards, test rows, dev-tailored inner — 선택 카드 통일 크기
- `rounded-[16px], [12px]`: resume-upload 내부 — 부분 일관

→ `DESIGN.md`에서 `rounded-lg(8px), rounded-xl(12px), rounded-2xl(16px)` 기본 스케일과 충돌. Phase 4/5에서 rounded 스케일을 디자인 토큰으로 재정의하는 것을 권장.

### 1.9 backdrop-blur
**총 11건 / 8파일** — 모두 sticky header 또는 다크 오버레이 텍스트 박스. Rule "모든 카드에 backdrop-blur" 위반 없음.

---

## 2. 9질문 × 14페이지 매트릭스

범례: ✓ 통과 / ✗ 위반 / ~ 부분·주의

| 페이지 | Q1 퍼플? | Q2 Inter/Roboto? | Q3 3-col icon grid? | Q4 모든 카드 blur? | Q5 pure B/W? | Q6 transition-all? | Q7 영어 placeholder? | Q8 swap-SaaS? | Q9 기억에 남는 하나? | 통과 |
|--------|---------|------------------|---------------------|---------------------|--------------|---------------------|----------------------|---------------|---------------------|------|
| home | ~ (violet-legacy 없음, `bg-white` 많음) | ✓ | ✓ (pain-points 세로 stack 재편 완료) | ✓ | ✗ (bg-white 다수) | ✓ | ✓ | ~ (tilt 카드 있음) | ✗ (Phase 4 대기) | 4/9 |
| dashboard | ✗ (violet-legacy CTA+table th 9건+sidebar) | ✓ | ✓ | ✓ | ~ (bg-white 최소) | ✗ (1건) | ✓ | ✓ | ~ (interview-table 레이아웃) | 5/9 |
| interview-page | ~ (violet-legacy 없음, 스튜디오 다크) | ✓ | ✓ | ~ (overlay 3건 의도적) | ✗ (bg-black 오버레이 의도적) | ✗ (8건) | ✓ | ✓ (스튜디오 UI) | ✓ (Meet-style greeting) | 3/9 |
| interview-setup | ✗ (step active violet-legacy 7건) | ✓ | ✓ | ✓ | ~ (bg-white 보조) | ✗ (보류 제외) | ✓ | ✓ | ~ (step cards) | 6/9 |
| interview-ready | ✓ (Phase 3h 정리 완료) | ✓ | ✓ | ✓ | ~ (bg-card 전환 완료) | ✓ | ✓ | ✓ | ~ (CTA rounded-[24px]) | 7/9 |
| interview-feedback | ✗ (violet-legacy 6건: 번호뱃지/progress) | ✓ | ✓ | ~ (header blur) | ~ (bg-background 토큰화 완료) | ✓ | ✓ | ✓ | ✓ (타임라인 + 비디오 동기화) | 6/9 |
| interview-analysis | ✗ (violet-legacy 8건: 진행바/최종 CTA) | ✓ | ✓ | ~ (header blur) | ~ (토큰화) | ✓ | ✓ | ✓ | ~ (progress UI) | 5/9 |
| review-list | ✗ (filter-bar violet-legacy active) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ (bookmark card) | 7/9 |
| about | ✗ (CTA violet-legacy) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | 7/9 |
| admin-feedbacks | ✓ (semantic amber/blue/green만) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | 8/9 |
| faq | ✗ (CTA violet-legacy) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | 7/9 |
| guide (3 하위) | ✗ (CTA violet-legacy, 3페이지 공통) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | 7/9 |
| privacy-policy | ✗ (링크/focus violet-legacy 5건) | ✓ | ✓ | ~ (header blur) | ✓ | ✓ | ✓ | ✓ | ~ | 7/9 |
| not-found | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (character 이스터에그) | 9/9 |

**평균 통과율**: 6.36 / 9 ≈ **70.7%** (최소 3/9 interview-page ~ 최대 9/9 not-found)

**해석**: Q1(퍼플)이 가장 많이 실패 — violet-legacy 토큰이 "의도적 잔존" 상태라 Phase 5에서 제거 결정이 핵심. Q9(기억에 남는 하나)는 Plan 04 Aceternity 대기 상태로 대부분 `~`.

---

## 3. 이슈 목록 (심각도순)

### H1. violet-legacy 토큰 사용처 대량 잔존 — 최종 제거 결정 필요
- 위치: 40 files × 93 occurrences + 정의 `frontend/src/index.css:48,74` + `frontend/tailwind.config.js:63-64`
- 현재: `bg-violet-legacy`, `text-violet-legacy`, `ring-violet-legacy`, `border-violet-legacy` 다수. CTA primary 전반 + Setup step active + Feedback/Analysis progress + focus ring 패턴.
- 목표: (옵션 A) `hsl(var(--primary))` 기반 모노크롬 primary로 전면 교체 → violet-legacy 정의 제거. (옵션 B) `--primary` 자체를 violet으로 선언하고 "모노크롬 기조 + violet accent 1개"로 재정의.
- 제약: `button-variants.ts cta/primary`가 violet-legacy에 하드 의존 → 버튼 variant 재설계 선결.
- 예상 수정 범위: **L** (40파일 전면 치환 + variant 재설계 + 토큰 정의 제거 + visual regression)

### H2. `#6366F1` 하드코딩 리터럴 잔존 — violet-legacy 토큰 전환 누락
- 위치: `frontend/src/components/ui/character.tsx:18,25,46`
- 현재: `color: '#6366F1'` (mood=thinking/idle/happy의 body prop)
- 목표: `color: 'hsl(var(--violet-legacy))'` 또는 Phase 5 결정에 따라 `hsl(var(--primary))`
- 예상 수정 범위: **S** (3라인)

### H3. `bg-white` / `bg-white/80` → `bg-background` 토큰 미정합 (home 중심)
- 위치: `home-page.tsx:49,63,114`, `journey-section.tsx:57,98`, `before-you-start-section.tsx:61`, `metrics-section.tsx:146,149,165`, `dev-tailored-section.tsx:79`, `video-feedback-section.tsx:47,61,83,93,158`, `pain-points-section.tsx:66`, `hero-section.tsx:75`, `protected-route.tsx:24`, `setup/resume-upload.tsx:89`, `login-modal.tsx:41,75`, `delete-confirm-dialog.tsx:35`, `service-feedback-modal.tsx:66`
- 현재: `bg-white` 하드코딩 (dark 모드에서 색 반전 불가)
- 목표: `bg-background` (페이지 shell) / `bg-card` (내부 카드) 토큰화
- 제약: delete-confirm-dialog / service-feedback-modal / login-modal은 Plan 03c/03d 이관 항목으로 TODO 마킹 상태.
- 예상 수정 범위: **M** (~18파일)

### H4. `rounded-[32px|28px|24px|20px]` 임의 스케일 남용 — radius 토큰 미정의
- 위치: 31건 / 22파일
- 현재: Cal.com 스타일 대형 카드 라운드(`32px`), CTA(`24px`), 선택 카드(`20px`)가 임의값으로 뿌려짐
- 목표: `DESIGN.md`에 `--radius-card: 32px`, `--radius-cta: 24px`, `--radius-select: 20px` 등 스케일 명시 후 Tailwind config에 등록 → `rounded-card` 등으로 통일
- 예상 수정 범위: **M** (토큰 정의 + 22파일 치환)

### M1. 스튜디오 다크 테마 하드코딩 — Google Meet 컬러 토큰화
- 위치: `interview-page.tsx(6)`, `upload-recovery-dialog.tsx(1)`, `finishing-overlay.tsx(4)`, `interviewer-avatar.tsx(3)`
- 현재: `#1a1a1a`, `#2c2c2c`, `#3c4043`, `#5F6368`, `#E8EAED`, `#F9AB00`
- 목표: `--studio-bg`, `--studio-surface`, `--studio-border`, `--studio-warn` 커스텀 토큰 선언 후 치환 (Plan 03c 보류 조건과 동일)
- 제약: Plan 03c가 명시적으로 보류한 영역 — 스튜디오 UI 디자인 결정 후 진행
- 예상 수정 범위: **S–M** (토큰 5개 정의 + ~14건 치환)

### M2. `transition-all` → 구체 속성 치환 (비-스튜디오 영역)
- 위치: `dashboard-page.tsx:119`, `interview-table.tsx:224`, `password-protected-route.tsx:69`, `character.tsx:54`
- 현재: `transition-all duration-...`
- 목표: `transition-colors` / `transition-[opacity,color]` / `transition-transform` 등 구체 지정
- 스튜디오 영역(interview-page, interview-controls, finishing-overlay, upload-recovery-dialog)은 M1과 묶어 처리 권장
- 예상 수정 범위: **S** (4건 + 스튜디오 영역은 M1 번들)

### M3. `hover:bg-[#059669]` 하드코딩 (success-hover 토큰 부재)
- 위치: `review-bookmark-card.tsx:105`
- 현재: `bg-success text-white ... hover:bg-[#059669]`
- 목표: `--success-hover` 토큰 추가 또는 `hover:bg-success/90`
- 예상 수정 범위: **S**

### M4. `border-[#C7D2FE]` 하드코딩
- 위치: `bookmark-toggle-button.tsx:97`
- 현재: `border border-[#C7D2FE]` (violet-legacy-light 파생)
- 목표: `border-violet-legacy-light` (또는 H1 결론에 따라 재할당)
- 예상 수정 범위: **S**

### M5. `ring-offset-[#0F172A]` 하드코딩
- 위치: `common/review-toast.tsx:83`
- 현재: `focus-visible:ring-offset-[#0F172A]` (slate-900, toast bg와 짝)
- 목표: Toast 전용 `--toast-bg` 토큰 정의 후 ring-offset도 토큰 참조
- 예상 수정 범위: **S**

### M6. login-modal `rounded-[28px]` + `shadow-toss-lg` 임의값 (TODO(plan-05) 명시)
- 위치: `frontend/src/components/ui/login-modal.tsx:40-41`
- 현재: 이미 코드에 TODO(plan-05) 주석 존재
- 목표: `rounded-card` 토큰 통일 + `shadow-md` 전환
- 예상 수정 범위: **S**

### M7. delete-confirm-dialog / service-feedback-modal `rounded-card shadow-toss-lg bg-white` (TODO(plan-05) 이관)
- 위치: `dashboard/delete-confirm-dialog.tsx:35`, `dashboard/service-feedback-modal.tsx:66`
- 현재: Plan 03c/03e 이관 상태 — H3·H4와 묶어 처리 가능
- 예상 수정 범위: **S**

### L1. `button-variants.ts` cta variant `shadow-[0_10px_20px_-5px_rgba(0,0,0,0.1)]` 임의값
- 위치: `frontend/src/components/ui/button-variants.ts:22,24,31`
- 현재: rgba 임의 shadow
- 목표: `shadow-md` / `shadow-lg` 토큰 사용 또는 `--shadow-cta` 변수 정의
- 예상 수정 범위: **S**

### L2. `home-page.tsx:49` selection:bg-secondary 외 최상위 bg-white
- H3에 포함. 단일 이슈로 별도 관리 시 L.

### L3. `home/hero-section.tsx:71` / `video-feedback-section.tsx:47` `radial-gradient` 임의값
- 위치: 2건
- 현재: `bg-[radial-gradient(circle_at_50%_50%,rgba(0,0,0,0.03),transparent_70%)]`
- 목표: 유지 가능(장식적). 필요 시 CSS 클래스로 분리.
- 예상 수정 범위: **XS** (선택)

### L4. 로고/캐릭터/면접관 아바타 SVG 하드코딩 색상 (유지 권장)
- 위치: `logo.tsx(4)`, `logo-icon.tsx(3)`, `character.tsx(stroke 2건, body 3건)`, `interviewer-avatar.tsx(3)`
- 판단: 로고/캐릭터 아이덴티티 — **유지**. character.tsx body의 `#6366F1`만 H2로 분리.

---

## 4. a11y 검증

### 4.1 axe-core 자동 검증
- **미실행**. `@axe-core/cli` / Playwright axe 통합 모두 프로젝트에 설치되어 있지 않음 (`package.json` 기준 `@axe-core/*` 의존성 0건).
- Playwright MCP는 세션에서 사용 가능하나, Phase 5 스펙은 "수정 0건"을 요구 — 설치/실행은 범위 밖으로 판단.
- **후속 과제로 이관**: Phase 5 이후 별도 스펙으로 `npm i -D @axe-core/cli playwright` 후 14페이지 CI 스캔 자동화 권장.

### 4.2 WCAG 2.1 AA 수동 체크 항목 (권고)
1. **Focus ring 대비**: `--ring: 217 91% 60%` (dark 배경 #0a0a0a 대비 ≈ AA 충족, index.css 주석에 명시됨). Light 배경에서도 동일 값 — 흰 배경 대비 4.5:1 유지 여부 실측 필요.
2. **violet-legacy on white** (CTA `bg-violet-legacy text-white`): indigo-500 기반, `#6366F1 + #fff` 대비 ≈ 4.65:1 (AA 통과).
3. **text-muted-foreground (`0 0% 54%`) on white**: 3.08:1 — **WCAG AA 미달 가능성**. 본문이 아닌 metadata 라벨로만 쓰여야 함. 사용처 전수 점검 필요.
4. **Google Meet 컬러 `#F9AB00`** on `bg-[#1a1a1a]`: 약 8.3:1 AAA — OK.
5. **아이콘-only 버튼 aria-label 누락**: 샘플 스폿 점검에서 `delete`, `close` 등 주요 버튼은 aria-label 존재. 전체 감사 필요.
6. **키보드 포커스 트랩**: Dialog 3종(login, delete-confirm, service-feedback)은 shadcn Radix 기반 — 통과. finishing-overlay/upload-recovery-dialog/exit-guard는 shadcn 미전환 상태로 포커스 트랩 미보장.

---

## 5. legacy 토큰(violet-legacy) 제거 조건 & 로드맵

**현재 상태**: 사용처 93건 / 40파일 + 정의 3지점 (`index.css` light, dark, `tailwind.config.js`)

**제거 전제**: 사용처 0건 달성 후 정의 삭제. 현 시점 정의 제거 시 빌드 실패.

**권장 로드맵 (사용자 결정 후 별도 스펙으로 진행)**:
1. **결정 게이트**: `--primary`를 violet으로 재정의할지, 완전 모노크롬 전환할지 사용자 선택.
2. **Option A — 모노크롬 전환**:
   - `button-variants.ts` cta/primary → `bg-primary text-primary-foreground` (모노크롬)
   - Setup step active → `bg-foreground text-background` 또는 별도 `--accent` 토큰 정의 후 교체
   - Feedback progress/번호 뱃지 → `bg-foreground` 또는 semantic color
   - Focus ring → `ring-ring` 일관 적용 (이미 대부분 전환됨)
3. **Option B — violet을 brand accent로 정식 채택**:
   - `--primary: 239 84% 67%` 재정의 후 violet-legacy 별칭 삭제
   - `button-variants.ts` 계속 primary 사용
   - `frontend-design-rules.md`의 "purple/indigo 금지" 규칙을 "이 프로젝트의 primary는 violet(#6366F1), 단 기조는 모노크롬"으로 예외 명시
4. 치환 후 `grep -rn "violet-legacy" frontend/src` = 0 확인 → `index.css` + `tailwind.config.js` 정의 제거 → 최종 커밋

---

## 6. 후속 작업 권고

### 우선순위별 수정 배치 (사용자 결정 사항)
- **P0 (게이트)**: H1 violet-legacy 제거 방향 결정 (Option A/B). 나머지 이슈의 치환 타겟이 여기에 의존.
- **P1**: H2(`#6366F1` 하드코딩 3건) + H3(`bg-white` 토큰화 ~18파일) + H4(radius 토큰 정의) — H1 결정 후 일괄.
- **P2**: M1(스튜디오 다크 토큰) + M2(transition-all) + M6/M7(dialog 임의값) — 스튜디오 UI 디자인 결정과 함께.
- **P3**: M3/M4/M5/L1 (자잘한 하드코딩 제거) — 묶어서 1PR로 처리 가능.
- **P4**: L3/L4 (장식/브랜드 하드코딩) — **유지 권장**, 필요 시에만.

### Plan 04 Aceternity 재개 여부 가이드
- 현재 Q9(기억에 남는 하나) 통과: not-found, interview-page, interview-feedback 3페이지 뿐.
- home/hero-section은 TODO 주석으로 Aceternity 삽입 위치가 예약되어 있음 (`Phase 03f 커밋 66c0d4e`).
- **권고**: H1 결정 + H3/H4 토큰 정리 후 Plan 04 재개. 토큰 불안정 상태에서 Aceternity 도입 시 재작업 가능성 높음.

### 감사 자동화 CI 권고 (범위 밖)
- `npm run audit:design` 스크립트: 본 문서의 grep 7종을 실행해 임계치(예: violet-legacy 0건, bg-[#..] ≤ 스튜디오 허용 15건)를 초과하면 CI 실패.
- axe-core Playwright 14페이지 스캔을 주 1회 스케줄 실행.

---

## 감사 방법론 체크리스트

- [x] 1.1 Tailwind 임의값 색상 grep (16건)
- [x] 1.2 rgb/rgba/hsl 직접 사용 grep (5건, button-variants 3건 + home 2건)
- [x] 1.3 Inter/Roboto/Arial/Open Sans/Lato grep (0건)
- [x] 1.4 transition-all grep (19건/8파일)
- [x] 1.5 pure black/white grep (91건/41파일, 대부분 의도적)
- [x] 1.6 영어 placeholder / Lorem ipsum grep (0건)
- [x] 1.7 violet-legacy + #6366F1 grep (93건 + 3건 리터럴)
- [x] 1.8 rounded-[N] 임의 스케일 grep (31건, 추가 체크)
- [x] 1.9 backdrop-blur 분포 확인 (11건 모두 header/overlay, 남용 아님)
- [x] 2. 9질문 × 14페이지 매트릭스
- [ ] 3. axe-core 자동 검증 — 환경 미구축, 후속 이관
- [x] 4. WCAG 수동 점검 항목 정리
- [x] 5. legacy 토큰 제거 로드맵
