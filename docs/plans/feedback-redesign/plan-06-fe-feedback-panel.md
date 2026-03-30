# Plan 06: FE 피드백 패널 리디자인

> 상태: Draft
> 작성일: 2026-03-30

## Why

피드백 구조가 변경됨에 따라 FE 피드백 패널도 리디자인이 필요하다. 탭 분리(답변 내용 / 전달력), 점수 제거 후 라벨 배지, 이모지+불릿 코멘트, 기술 피드백 카드를 반영해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/feedback/feedback-panel.tsx` | 탭 UI 추가, 카드 내부 구조 전면 변경 |
| `frontend/src/components/feedback/content-tab.tsx` | 신규 — 답변 내용 분석 탭 컴포넌트 |
| `frontend/src/components/feedback/delivery-tab.tsx` | 신규 — 전달력 분석 탭 컴포넌트 |
| `frontend/src/components/feedback/level-badge.tsx` | 신규 — 3단계 라벨 배지 공통 컴포넌트 |
| `frontend/src/components/feedback/structured-comment.tsx` | 신규 — 이모지+불릿 코멘트 파싱/렌더링 컴포넌트 |
| `frontend/src/components/feedback/accuracy-issues.tsx` | 신규 — 기술 정확성 오류 목록 컴포넌트 |
| `frontend/src/components/feedback/coaching-card.tsx` | 신규 — 코칭 카드 컴포넌트 |
| `frontend/src/types/interview.ts` | TimestampFeedback 타입 변경 |

## 상세

### 1. 탭 구조

```
┌─────────────────┬─────────────────┐
│  답변 내용 분석  │  전달력 분석     │
└─────────────────┴─────────────────┘
```

- 탭 전환 시 동일 피드백 카드의 다른 관점을 보여줌
- 기본 선택 탭: "답변 내용 분석"

### 2. 답변 내용 분석 탭 (`content-tab.tsx`)

```
┌─────────────────────────────────────┐
│ ✓ JPA 영속성 컨텍스트 개념을 정확히  │
│   설명했습니다                        │
│ △ 1차 캐시와 지연 로딩의 관계 설명이  │
│   빠졌습니다                         │
│ → 영속성 컨텍스트의 생명주기와        │
│   트랜잭션 범위를 연결해서 설명해보세요 │
├─────────────────────────────────────┤
│ ⚠ 기술 오류 (있을 때만 표시)          │
│ ┌─────────────────────────────────┐ │
│ │ "기본 격리 수준이 SERIALIZABLE"   │ │
│ │ → 기본값은 DB의 기본 격리 수준을  │ │
│ │   따릅니다 (MySQL: REPEATABLE    │ │
│ │   READ)                         │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ 💡 코칭                             │
│ 구조: 개념→원리→실무적용 순서로...    │
│ 개선: 실제 장애 사례를 덧붙이면...    │
└─────────────────────────────────────┘
```

### 3. 전달력 분석 탭 (`delivery-tab.tsx`)

```
┌─────────────────────────────────────┐
│ 비언어                               │
│ [좋음] 시선  [보통] 자세  자신감 표정  │
│ ✓ 카메라를 안정적으로 응시했습니다     │
│ △ 답변 후반부에 자세가 기울어졌습니다  │
│ → 의자 등받이에 기대지 말고...        │
├─────────────────────────────────────┤
│ 음성                                 │
│ [적절] 속도  [보통] 자신감  평온 감정  │
│ 필러워드: [음] [어] [그]              │
│ ✓ 말 속도가 적절합니다               │
│ △ 문장 시작 시 "음"이 반복됩니다      │
│ → 짧은 호흡 후 바로 시작하는 연습을... │
└─────────────────────────────────────┘
```

### 4. LevelBadge 컴포넌트

```tsx
const LEVEL_STYLES = {
  GOOD: 'bg-green-50 text-green-600',
  AVERAGE: 'bg-yellow-50 text-yellow-600',
  NEEDS_IMPROVEMENT: 'bg-red-50 text-red-600',
}
const LEVEL_LABELS = {
  GOOD: '좋음',
  AVERAGE: '보통',
  NEEDS_IMPROVEMENT: '개선 필요',
}
```

### 5. StructuredComment 컴포넌트

이모지+불릿 포맷 파싱:
```tsx
// "✓ 잘한 점\n△ 보완할 점\n→ 개선 방법" → 3줄로 렌더링
// ✓ → 초록색, △ → 주황색, → → 파란색
```

### 6. 하위 호환

- `accuracyIssues`, `coaching` 필드가 없는 기존 데이터: 해당 섹션 숨김
- `eyeContactLevel` 등이 null이고 `eyeContactScore`가 있으면: score 기반 라벨 변환하여 표시
- `comment`가 이모지+불릿 포맷이 아닌 기존 텍스트: 그대로 텍스트 표시

## 담당 에이전트

- Implement: `frontend` — 탭 UI, 컴포넌트 구현
- Review: `code-reviewer` — 코드 품질, 타입 안전성
- Review: `designer` — UI/UX 일관성, 디자인 토큰 준수

## 검증

- 답변 내용 / 전달력 탭 전환이 정상 동작하는지 확인
- 기존 데이터(score 있음, level 없음)에서 fallback이 정상 동작하는지 확인
- 기술 오류 목록이 기술 유형에서만 표시되는지 확인
- 이모지+불릿 코멘트가 올바르게 파싱/렌더링되는지 확인
- 반응형(모바일) 레이아웃 확인
- FE lint + build 통과 (`Frontend CI`)
- `progress.md` 상태 업데이트 (Task 6 → Completed)
