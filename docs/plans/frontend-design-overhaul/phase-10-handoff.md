# Plan 10 Handoff — 다음 세션 이어받기 가이드

> **Status**: Phase A 완료 · PR open · Phase B 대기
> **Updated**: 2026-04-18
> **Author**: Claude Opus 4.7 (1M) — 자율 디자이너 모드

---

## 0. 이 문서는 무엇인가

사용자가 "디자인을 잘 몰라서 너가 디자이너로써 판단해서 다 자동으로 작업해줘. 컨텍스트 30-40 남으면 정리하고 새 세션에서 이어가고"라고 지시함. 이 문서는 **다음 세션의 Claude가 앞에서부터 읽고 그대로 실행 가능**하도록 구성된다.

---

## 1. 프로젝트 한눈에

**Rehearse**: 개발자 취준생 대상 AI 모의면접 플랫폼. 프로젝트 루트 `/Users/koseonje/dev/devlens`.

**진행 중인 작업**: 프론트엔드 디자인 전면 개편.
- Plan 01~09: 기반 정비 (퍼플 제거, shadcn 도입, 토큰 정리) — **완료**
- Plan 10: 디자인 언어 "Quiet Rigor" (editorial 3-pane, semantic color) — **Phase A 완료, B~D 대기**

**마스터 스펙**: `/Users/koseonje/dev/devlens/.omc/plans/2026-04-18-product-ui-redesign.md` (1,594줄). 이 문서를 반드시 먼저 읽어라.

---

## 2. 브랜치 / PR / 커밋

- **브랜치**: `refactor/frontend-design-overhaul`
- **Base**: `develop`
- **PR**: https://github.com/KoSeonJe/rehearse/pull/329 (open, CI 결과 확인 필요)
- **Phase A 커밋**:
  - `ca6443e refactor(fe): Plan 10 Phase A — Quiet Rigor 토큰 교체 + DESIGN.md 개정`
  - `8a3fd05 docs: Plan 10 마스터 스펙`
- 이전 누적: Plan 01~09 10+ 커밋 (동일 PR에 포함)

**이 PR이 머지되기 전에 Phase B에 착수하지 마라** — Phase B는 이 토큰 레이어 위에 primitive·페이지를 쌓는다.

---

## 3. 컨텍스트 복원 순서 (새 세션 Claude가 해야 할 일)

새 세션 시작 시 다음 파일을 **이 순서로** 읽어라:

1. 이 문서 (Phase 10 Handoff)
2. `/Users/koseonje/dev/devlens/CLAUDE.md` (프로젝트 규칙)
3. `/Users/koseonje/dev/devlens/.claude/rules/frontend-design-rules.md` (AI slop 금지)
4. `/Users/koseonje/dev/devlens/DESIGN.md` (Phase A 개정된 최신본, Layout System 섹션 포함)
5. `/Users/koseonje/dev/devlens/.omc/plans/2026-04-18-product-ui-redesign.md` (Plan 10 마스터 스펙 — §4 Grid / §5 Primitives / §7.1 feedback 3-pane 집중)
6. `/Users/koseonje/dev/devlens/docs/plans/frontend-design-overhaul/requirements.md` (Plan 01~09 맥락)
7. `/Users/koseonje/.claude/projects/-Users-koseonje-dev-devlens/memory/MEMORY.md` (auto memory 인덱스)

**auto memory**는 `/Users/koseonje/.claude/projects/-Users-koseonje-dev-devlens/memory/`에 저장됨:
- `user_profile.md` — 사용자는 디자인 비전공, 자율 판단 선호
- `feedback_autonomous.md` — 디자인 작업은 자율 판단, 확인 최소화
- `feedback_context_budget.md` — 30-40% 남으면 핸드오프
- `project_design_overhaul.md` — 프로젝트 상태
- `reference_plan_location.md` — 플랜 파일 위치

---

## 4. Phase A 완료 상태 (되돌리지 말 것)

### 변경된 파일

| 파일 | 변화 |
|------|------|
| `frontend/src/index.css` | warm off-neutral 팔레트, semantic color 4종, layout/motion 토큰 |
| `frontend/tailwind.config.js` | semantic color 매핑, serif 폰트 키, 5단계 섀도우, @deprecated keyframe 유지 |
| `frontend/index.html` | Fraunces preload 주석 (Phase B 활성화) |
| `DESIGN.md` | Color/Typography 부분 개정 + Layout System / Responsive Strategy / Asset Policy 섹션 신설 |

### 검증 결과
- ESLint ✓ / tsc ✓ / vitest 38 passed, snapshot 회귀 0
- WCAG: light 17.8:1, dark 17.4:1, muted 4.53:1

### 주의사항
- **Cal Sans 폰트**는 유지됨 (Plan 01~09 승계). 제거 금지
- **shadcn Card**는 제거하지 말고 "사용 영역 제한" 방향으로 (dashboard·dialog·selection에는 유지)
- `rec-pulse`, `tutorial-ring`, `tutorial-nudge`, `ripple` keyframe은 `@deprecated`로 존치. Phase C/D에서 **해당 컴포넌트와 함께** 제거할 것:
  - `frontend/src/pages/interview-page.tsx:159` — `animate-rec-pulse`
  - `frontend/src/components/feedback/review-coach-mark.tsx:274,339` — `animate-tutorial-ring/nudge`
  - `frontend/src/components/interview/interviewer-avatar.tsx:22,26,30,39,47` — `ripple`, `meet-green/red`

---

## 5. Phase B 시작점 (다음 세션 첫 작업)

### 5.1 게이트 확인 (시작 전 필수)

- [ ] PR #329 CI 결과 확인 (`gh pr checks 329`)
- [ ] 사용자가 "토큰 반영 OK, Phase B 진행해" 확인 (또는 PR merged)
- [ ] 로컬 `git pull origin refactor/frontend-design-overhaul` 최신화

### 5.2 Phase B 범위

**목표**: `feedback-page` 3-pane editorial 전환.

**대상 파일**:
- `frontend/src/pages/interview-feedback-page.tsx` (현재 `max-w-6xl` 중앙 + 60/40 하드 스플릿)
- `frontend/src/components/feedback/*` (13개, 마이그레이션 매트릭스는 스펙 §10 참조)

**신규 primitive 도입 순서** (스펙 §5 기반, 리뷰에서 지적된 순서 엄수):
1. **`frontend/src/components/layout/page-grid.tsx`** — 12-col asymmetric grid wrapper (가장 기반)
2. **`frontend/src/components/layout/reading-column.tsx`** — `max-w-[55ch]` long-form 컨테이너
3. **`frontend/src/components/layout/sticky-rail.tsx`** — 순수 sticky 배치 (col + offset만 담당)
4. **`frontend/src/components/layout/chapter-marker.tsx`** — 숫자(24px tabular) + hairline + title slot
5. **`frontend/src/components/layout/utility-bar.tsx`** — 얇은 헤더 (44/56px, `top-[var(--utility-bar-height)]`)
6. **`frontend/src/components/layout/sticky-outline/`** — compound 패턴
   - `sticky-outline.desktop.tsx` (xl+: col-span-2 sticky)
   - `sticky-outline.tab-bar.tsx` (lg: 상단 horizontal tab)
   - `sticky-outline.mobile-sheet.tsx` (md/sm: Radix Sheet bottom-sheet)
   - `index.ts` — `useBreakpoint()` 훅으로 분기
7. **`frontend/src/components/feedback/video-dock.tsx`** (layout이 아닌 feedback 도메인) — `StickyRail + VideoPlayer + TimelineBar` 컴포지션

**feedback-page 전환**:
- `max-w-6xl mx-auto` → `<PageGrid>` 12-col
- `QuestionSetSection` 카드 → `<ChapterMarker>` + `<ReadingColumn>` + `<VideoDock>` 조합
- `videoRef`는 Zustand slice 또는 context lift (3-pane 간 공유)
- `QuestionList` → `<StickyOutline>` Replace (스펙 §10 매트릭스 참조)
- `ReviewCoachMark` → **Drop** (localStorage `rehearse.feedbackOnboarded` 기반 dismissible callout으로 대체)

### 5.3 예상 공수 (리뷰에서 재추정): 5-7일. 단일 세션으론 불가능, Phase B 자체도 분할 권장.

**Phase B 세부 분할 제안**:
- B.1 — primitive 6종 생성 + 단위 테스트 (스펙 §13 tier1 RTL) → 별도 PR
- B.2 — feedback-page 3-pane 적용 + 마이그레이션 매트릭스 실행 → 별도 PR

### 5.4 검증 Gate (Phase B 완료 조건)

- ESLint / tsc / vitest 통과
- vitest-axe feedback-page 0 violations
- 스펙 성공 기준 §12.3 기술 지표: LCP ≤ 2.5s, CLS < 0.1, Lighthouse a11y ≥ 95
- sticky top 오프셋이 `--utility-bar-height`로 통일되어 모바일 56px에서도 안 잘림
- StickyOutline Desktop/TabBar/MobileSheet 3종 breakpoint 전환 확인

---

## 6. Phase C, D 예고 (Phase B 완료 후)

### Phase C — interview-page 극장식 몰입 모드
- `frontend/src/pages/interview-page.tsx` 재설계
- Google Meet 복제 컨트롤 바 제거, 비디오 풀블리드 + 우측 rail
- `rec-pulse`, `meet-green/red`, `ripple` keyframe과 해당 사용처 **함께** 제거
- 컨트롤 항상 visible (모바일 56px 고정 bar)

### Phase D — 나머지 페이지 일괄
- setup (4+8 split), home (섹션별 차등), dashboard (8+4 + 활자 헤드라인)
- review-list, about/privacy/faq/guide
- **이모지 제거** (🚀 ✨ 💡 🎯 — `frontend-design-rules.md` 금지)
- `ReviewCoachMark` 완전 제거 + 대체 온보딩
- a11y 재검증 (vitest-axe 14 페이지)

---

## 7. 자주 쓸 명령어

```bash
cd /Users/koseonje/dev/devlens/frontend
npm run dev       # 로컬 확인
npm run lint
npm run test
npm run build
npx tsc --noEmit

# PR 상태
gh pr checks 329
gh pr view 329

# 원격 최신화
git pull origin refactor/frontend-design-overhaul
```

---

## 8. 결정 요지 (왜 이 방향인지 한 장)

- **"Quiet Rigor"** — Linear + Readwise + Arc 참고. 면접의 진지함 + 개발자의 정밀함을 editorial 타이포·hairline·여백으로 증명
- **승계**: Cal.com 모노크롬, Cal Sans, shadcn 23개 primitive, Aceternity 히어로 1개 정책
- **추가**: Semantic color 4종, 12-col asymmetric grid, editorial 3-pane, ChapterMarker
- **되돌리기 금지**: Plan 01~09에서 제거한 violet-legacy, 해체한 커스텀 UI, rename한 토큰
- **사용자 피드백 기반**: "캐주얼 → 진지·세련"은 색이 아니라 **레이아웃 구조** 문제

---

## 9. 질문이 생기면

사용자는 디자인 비전공. 구현 디테일·토큰 값·컴포넌트 API 같은 것은 **스스로 판단해서 진행**하고 결과 보고.
방향성이 흔들리는 이슈(예: "3-pane 말고 다른 구조")만 사용자에게 확인.

Plan 01~09 의도가 불확실하면 `docs/plans/frontend-design-overhaul/plan-0N-*.md` 해당 문서 직접 열어볼 것.
