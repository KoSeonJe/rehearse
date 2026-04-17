# Plan 07: 토큰 정리 최종 마무리 (Group A)

> 상태: Draft
> 작성일: 2026-04-18

## Why

Phase 6 클린업이 `violet-legacy`/`#6366F1`는 제거했지만 실측 grep 결과 **arbitrary radius 20+건**, **`bg-white` 10+건**, **`character.tsx` `transition-all` 1건**이 잔존한다. 이들이 남아 있으면 디자인 토큰 drift가 다시 발생하고 `frontend-design-rules.md` self-check 항목 6(`transition-all` 남용)과 토큰 정합성 기준을 통과하지 못한다. 본 태스크는 merge 전 최종 잔여 정리이다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/tailwind.config.js` | `borderRadius` 스케일 정의(`2xl: 20px`, `3xl: 24px`, `4xl: 32px`) |
| `frontend/src/pages/home-page.tsx` | sticky header `bg-white/80` → `bg-background/80` |
| `frontend/src/pages/interview-ready-page.tsx` | `rounded-[24px]` → `rounded-3xl` (2건) |
| `frontend/src/pages/interview-analysis-page.tsx` | `rounded-[24px]` → `rounded-3xl` (2건) |
| `frontend/src/pages/interview-feedback-page.tsx` | `rounded-[32px]` → `rounded-4xl`, `rounded-[24px]` → `rounded-3xl` |
| `frontend/src/components/home/hero-section.tsx` | `rounded-[32px]` → `rounded-4xl`, `rounded-[24px]` → `rounded-3xl`, `bg-white/80` → `bg-background/80` |
| `frontend/src/components/home/journey-section.tsx` | `rounded-[32px]` → `rounded-4xl`, `rounded-[24px]` → `rounded-3xl`, 내부 `bg-white` → `bg-card` |
| `frontend/src/components/home/dev-tailored-section.tsx` | `rounded-[32px]` → `rounded-4xl`, `rounded-[20px]` → `rounded-2xl` |
| `frontend/src/components/home/metrics-section.tsx` | `rounded-[24px]` → `rounded-3xl` |
| `frontend/src/components/home/video-feedback-section.tsx` | `rounded-[20px]` → `rounded-2xl` (타임라인 점 `bg-white` 유지 — 시각 강조 목적) |
| `frontend/src/components/setup/step-{position,level,interview-type,duration,tech-stack}.tsx` | `rounded-[20px]` → `rounded-2xl` (5파일) |
| `frontend/src/components/setup/setup-navigation.tsx` | `rounded-[24px]` → `rounded-3xl` (3건) |
| `frontend/src/components/setup/resume-upload.tsx` | `bg-white` → `bg-card` (삭제 버튼) |
| `frontend/src/components/interview/{camera,mic,speaker}-test-row.tsx` | `rounded-[20px]` → `rounded-2xl` (3파일) |
| `frontend/src/components/dashboard/delete-confirm-dialog.tsx` | `bg-white rounded-card shadow-toss-lg` → `bg-card rounded-lg shadow-md` |
| `frontend/src/components/dashboard/service-feedback-modal.tsx` | 동일 패턴 정리 |
| `frontend/src/components/ui/login-modal.tsx` | `rounded-[28px] bg-white shadow-toss-lg` → `rounded-3xl bg-card shadow-md`, OAuth 버튼 `bg-white` → `bg-card`, 내부 `TODO(plan-05)` 주석 제거 |
| `frontend/src/components/ui/button-variants.ts` | secondary/outline variant `bg-white ... hover:bg-background` → `bg-background ... hover:bg-muted` |
| `frontend/src/components/ui/character.tsx` | `transition-all duration-500` → `transition-[transform,opacity] duration-500` |

## 상세

### 보존 원칙 (수정하지 않음)

- `frontend/src/pages/interview-page.tsx` 및 `components/interview/{interview-controls,upload-recovery-dialog,finishing-overlay}.tsx` — 스튜디오 Google Meet 다크 UI (Plan 03h 의도 보존)
- `components/ui/login-modal.tsx:64-81` — OAuth 브랜드 컬러(GitHub `#24292e`, Google SVG)
- `components/ui/character.tsx:32,39` — `#F59E0B`(confused) / `#EF4444`(recording) semantic mood
- `components/setup/step-tech-stack.tsx:54`, `step-level.tsx:57` — `bg-white/20` (다크 배경 위 선택 강조)

### Tailwind radius 스케일 정의

```js
// frontend/tailwind.config.js (추가/수정)
theme: {
  extend: {
    borderRadius: {
      '2xl': '20px',
      '3xl': '24px',
      '4xl': '32px',
    },
  },
},
```

- 기존 shadcn 기본 `rounded-lg`(8px), `rounded-md`(6px)는 유지
- `rounded-card` 커스텀 토큰은 사용처 0으로 정리 후 Phase 8에서 제거 여부 재평가

## 담당 에이전트

- Implement: `frontend` — 치환 작업 + Tailwind 스케일 정의
- Review: `code-reviewer` — 토큰 정합, 의도 보존 라인 오염 여부, 다크모드 영향
- Review: `designer` — 시각 회귀(라운드 값 변동으로 카드 느낌 변화 확인)

## 검증

- `grep -rE "rounded-\[(20|24|28|32)px\]" frontend/src` → **0건**
- `grep -rnE "bg-white(?![/\d])" frontend/src` → 의도 잔존 라인만 (타임라인 점, `bg-white/20` 등)
- `grep -rn "transition-all" frontend/src/components/ui/character.tsx` → **0건**
- `grep -rn "TODO(plan-05)" frontend/src` → **0건**
- `cd frontend && npm run lint && npm run build && npm run test` 모두 green
- 홈/셋업/피드백/분석/대시보드 페이지 수동 smoke (라이트/다크) — 라운드/배경 차이 인지 가능하나 레이아웃 깨짐 없음
- `progress.md` Task 7 → Completed
