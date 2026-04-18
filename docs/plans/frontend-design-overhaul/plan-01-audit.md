# Plan 01: 프론트엔드 디자인 상태 진단 (Audit)

> 상태: Draft
> 작성일: 2026-04-17

## Why

손대기 전에 전수 인벤토리와 DESIGN.md 대비 gap을 수치화하지 않으면 리팩토링 중 우선순위가 흐려지고 드리프트가 재발생한다. Phase 2 이후 모든 의사결정의 근거 자료를 마련한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/design-audit.md` | 색상/폰트/컴포넌트/spacing 인벤토리 + DESIGN.md gap 테이블 |

## 상세

### 1. 인벤토리 수집

- **색상**: `frontend/src/**/*.{ts,tsx,css}` 전수 grep
  - `#[0-9a-fA-F]{3,8}`, `rgb(`, `rgba(`, `hsl(` 모두 추출 → 빈도 집계
  - Tailwind 클래스 내 `bg-*`, `text-*`, `border-*` 커스텀값 포함
- **폰트**: `font-family` 선언, `font-*` 클래스, Google Fonts import, `index.html` preload
- **컴포넌트 중복**: `frontend/src/components/ui/` 13개 + 페이지 내 인라인 `<button>`, `<input>`, 카드 패턴 빈도
- **하드코딩 spacing/radius**: `p-[`, `m-[`, `w-[`, `h-[`, `gap-[`, `rounded-[` 임의값 빈도

### 2. Gap 분석 (현재 vs DESIGN.md)

템플릿:

| 항목 | 현재 값 | 목표(DESIGN.md) | 차이 | 영향 파일 수 | 우선순위 |
|------|---------|-----------------|------|-------------|---------|
| accent primary | `#6366F1` | Charcoal `#242424` | 퍼플 제거 | ? | H |
| border-radius (button) | `24px` | Cal 기준 scale | 재정의 | ? | M |
| font display | (없음) | Cal Sans | 추가 | 홈 hero | M |
| dark mode bg | `#202124` | `#0a0a0a` 범위 | 조정 | studio 사용처 | L |
| ... | | | | | |

### 3. 페이지별 카드

14개 페이지 각각에 대해:
- 주요 섹션 목록
- 사용 중인 UI primitive
- DESIGN.md 위반 포인트 요약
- 예상 리팩토링 난이도 (S/M/L)

대상: `home`, `dashboard`, `interview-page`, `interview-setup`, `interview-ready`, `interview-feedback`, `interview-analysis`, `review-list`, `about`, `admin-feedbacks`, `faq`, `guide`, `privacy-policy`, `not-found`

### 4. 컴포넌트 교체 후보

shadcn 매핑 테이블:

| 기존 | shadcn | 비고 |
|------|--------|------|
| `button.tsx` | `Button` | 3a |
| `text-input.tsx` | `Input` | 3b |
| `login-modal.tsx` | `Dialog` | 3c |
| `selection-card.tsx` | `Card` | 3d |
| ... | | |

## 담당 에이전트

- Implement: `designer` — 인벤토리 + gap 분석 작성
- Review: `code-reviewer` — 14 페이지 누락 여부, 집계 정확성

## 검증

- `docs/design-audit.md` 생성됨
- 14 페이지 모두 페이지 카드 존재
- 색상/폰트/컴포넌트/spacing 4개 인벤토리 섹션 모두 채워짐
- 우선순위 H/M/L 분류 완료
- **코드 수정 0건** (이 Plan은 진단 전용)
- `progress.md` Task 1 → Completed

## 체크포인트

사용자가 audit 결과 리뷰 후 Phase 2 우선순위 확정 → 다음 Plan 진입 승인.
