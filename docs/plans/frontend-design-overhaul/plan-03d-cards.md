# Plan 03d: Card 컴포넌트 shadcn 교체

> 상태: Draft
> 작성일: 2026-04-17

## Why

`selection-card.tsx`와 페이지 내 반복되는 카드 패턴(대시보드 리스트, feature 블록, 리뷰 항목 등)이 각기 다른 padding/radius/shadow를 사용. shadcn `Card` compound로 통일하고 DESIGN.md 토큰 기반 shadow/radius로 수렴.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/selection-card.tsx` | shadcn `Card` 래핑 또는 교체 |
| `frontend/src/components/dashboard/**/*.tsx` | 카드 패턴 교체 |
| `frontend/src/components/home/**/*.tsx` | feature 카드 교체 |
| `frontend/src/components/review/**/*.tsx` | 리뷰 카드 교체 |

## 상세

### 1. shadcn Card 설치

```bash
cd frontend
npx shadcn@latest add card
```

### 2. 매핑

| 기존 | shadcn |
|------|--------|
| 타이틀 + 본문 카드 | `Card > CardHeader > CardTitle + CardContent` |
| 타이틀 + 본문 + 액션 | `... + CardFooter` |
| 선택 가능 카드(`selection-card.tsx`) | `Card` + 선택 상태 variant (클릭 핸들러 유지) |

### 3. Shadow / Radius 정렬

- 기존 `shadow-toss`, `rounded-card(20px)` 사용처 정리
- **유지/제거 판정 근거는 Plan 01 Audit의 카드 인벤토리 결과를 따름** (`docs/design-audit.md`의 "컴포넌트 중복" + "하드코딩 spacing/radius" 섹션)
  - Audit 결과가 없으면 본 Phase 진입 금지 — Phase 1 재개 후 진행
- DESIGN.md 기준 `rounded-lg` (`var(--radius)`) + 가벼운 shadow 기본값
- hover shadow는 interactive 카드에만

### 4. 제약

- 카드 내부 Button/Input은 Phase 3a/3b 결과 그대로 사용
- `onClick` 핸들러 유지, 카드 선택 상태 로직 유지

## 담당 에이전트

- Implement: `frontend` — 카드 교체
- Review: `designer` — shadow/radius/typography가 DESIGN.md 토큰 기반인지

## 검증

- `npm run lint/build/test` green
- 수동 스모크: 대시보드 리스트, 홈 feature, review-list 클릭 동작
- 시각 점검: 카드 간 spacing/radius 일관성
- `progress.md` Task 3d → Completed

## 체크포인트

변경 파일 수 + 시각 회귀 여부 보고 → 사용자 승인 후 Phase 3e 진입.
