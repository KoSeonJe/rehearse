# Plan 05: 타임라인 색상 인코딩 통일 [parallel]

> 상태: Draft
> 작성일: 2026-03-20

## Why

타임라인 바의 Legend에 타입 색상(원본=accent, 후속=blue)과 점수 색상(80+=green, 50~79=yellow, ~49=red) 두 가지 인코딩이 동시에 표시되지만, 실제 바 세그먼트는 점수 색상(`getScoreColor`)만 사용한다. 타입 색상(`ANSWER_TYPE_COLORS`)은 Legend에만 있고 바에는 적용되지 않아 혼란. (이슈 #7)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/feedback/timeline-bar.tsx` | 색상 인코딩 통일 — 점수 기반 단일 체계 |

## 상세

### 방안: 점수 색상 단일 체계 + 타입은 border로 구분

- 바 세그먼트 색상: 점수 기반 유지 (green/yellow/red/gray)
- FOLLOWUP 타입: `border-2 border-dashed border-blue-400`로 시각 구분
- Legend에서 `ANSWER_TYPE_COLORS` 제거, 타입은 "점선=후속질문"으로 표시

### 변경 코드

```tsx
// 바 세그먼트
<button
  className={`absolute top-1 bottom-1 rounded-md transition-all cursor-pointer ${
    isActive ? 'ring-2 ring-accent ring-offset-1 z-10' : 'hover:brightness-110'
  } ${getScoreColor(fb)} ${
    fb.questionType === 'FOLLOWUP' ? 'border-2 border-dashed border-blue-400' : ''
  }`}
  ...
/>
```

### Legend 변경

```
[━━━] 80+   [━━━] 50~79   [━━━] ~49   [┅┅┅] 후속질문
```

- ANSWER_TYPE_COLORS의 "원본" 항목 제거 (기본이 원본)
- "후속" 항목은 dashed border 스타일로 표시

## 담당 에이전트

- Implement: `frontend` — 타임라인 색상 로직 수정
- Review: `designer` — 색상 체계 일관성

## 검증

- MAIN 타입 세그먼트: 점수 색상만 적용, border 없음
- FOLLOWUP 타입 세그먼트: 점수 색상 + dashed border
- Legend가 점수 색상 3개 + 후속질문 dashed 패턴으로 표시
- `progress.md` 상태 업데이트 (Task 5 → Completed)
