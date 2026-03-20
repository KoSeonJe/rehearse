# Plan 03: 비디오 우선 레이아웃 [parallel]

> 상태: Draft
> 작성일: 2026-03-20

## Why

현재 `lg:grid-cols-2` 50/50 분할에서 비디오가 너무 작아 면접 영상 확인이 어렵다. 피드백 페이지의 핵심은 영상을 보며 피드백을 확인하는 것이므로 비디오가 더 커야 한다. (이슈 #5)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-feedback-page.tsx` | 그리드 레이아웃 변경 (50/50 → 비디오 우선) |

## 상세

### 레이아웃 변경

```tsx
// Before
<div className="grid grid-cols-1 gap-6 lg:grid-cols-2">

// After — 비디오 상단, 피드백 하단 (스택 레이아웃)
<div className="space-y-6">
  {/* Video + Timeline: full width */}
  <div className="space-y-4">
    <VideoPlayer ... />
    <TimelineBar ... />
  </div>

  {/* Feedback Panel: full width, max-height with scroll */}
  <div className="max-h-[500px] overflow-y-auto">
    <FeedbackPanel ... />
  </div>
</div>
```

### 근거

- 비디오가 전체 너비를 사용하면 면접자의 표정/제스처 확인 용이
- 타임라인도 넓어져 구간 클릭이 더 정확해짐
- 세로 스택은 모바일과 데스크톱 레이아웃 차이가 줄어들어 반응형 일관성 향상
- 피드백 패널은 `max-h-[500px]`로 스크롤 영역 유지

### 기존 lg:max-h-[500px] 제거

```tsx
// Before
<div className="lg:max-h-[500px] lg:overflow-y-auto">

// After (lg 프리픽스 불필요, 항상 스크롤)
<div className="max-h-[500px] overflow-y-auto">
```

## 담당 에이전트

- Implement: `frontend` — 레이아웃 변경
- Review: `designer` — 비디오 비율, 스크롤 UX

## 검증

- 비디오가 전체 너비로 표시됨
- 타임라인 바가 비디오 바로 아래에 위치
- 피드백 패널이 스크롤 가능하고 max-height 적용됨
- 모바일/데스크톱 모두 레이아웃 정상
- `progress.md` 상태 업데이트 (Task 3 → Completed)
