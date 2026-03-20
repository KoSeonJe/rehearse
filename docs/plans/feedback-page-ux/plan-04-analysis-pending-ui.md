# Plan 04: 분석 대기 상태 UI 개선 [parallel]

> 상태: Draft
> 작성일: 2026-03-20

## Why

`analysisStatus !== 'COMPLETED'`일 때 `animate-pulse` 빈 박스만 표시되어 사용자가 현재 무슨 일이 일어나고 있는지 알 수 없다. 분석 파이프라인의 단계(STT → 언어분석 → 비언어분석 → 종합)를 시각적으로 보여줘야 한다. (이슈 #4)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-feedback-page.tsx` | QuestionSetSection의 분석 대기/진행 UI 개선 |

## 상세

### 현재 코드 (lines 73-89)

```tsx
// analysisStatus !== 'COMPLETED'일 때
<div className="rounded-2xl bg-surface p-8 text-center animate-pulse">
  <p className="text-sm font-bold text-text-tertiary">분석이 아직 완료되지 않았습니다</p>
</div>
```

### 변경: 상태별 분기

`AnalysisStatus` 값에 따라 다른 UI 표시:

```
PENDING / PENDING_UPLOAD → "영상 업로드 대기 중"
ANALYZING → "분석 진행 중" + 단계 표시
SKIPPED → "이 질문세트는 건너뛰었습니다"
FAILED → (기존 FAILED UI 유지)
COMPLETED → (기존 피드백 UI 유지)
```

### ANALYZING 상태 UI

`AnalysisProgress` 타입을 활용한 단계 표시:

```
STARTED → 분석 시작
EXTRACTING → 영상 추출 중
STT_PROCESSING → 음성 인식 중
VERBAL_ANALYZING → 언어 분석 중
NONVERBAL_ANALYZING → 비언어 분석 중
FINALIZING → 종합 정리 중
```

`QuestionSetStatusResponse`의 `analysisProgress` 필드를 폴링으로 가져오되, 현재 페이지에서는 `QuestionSetData`의 `analysisStatus`만 사용. 상태가 `ANALYZING`이면 Character 컴포넌트 + 진행 메시지를 표시.

```tsx
// ANALYZING
<div className="rounded-2xl bg-surface p-8 text-center space-y-4">
  <Character mood="thinking" size={80} className="mx-auto" />
  <div>
    <p className="text-sm font-semibold text-text-primary">분석이 진행 중이에요</p>
    <p className="text-xs text-text-tertiary mt-1">영상을 분석하고 피드백을 생성하고 있습니다</p>
  </div>
  <div className="h-1 w-32 bg-accent/20 rounded-full mx-auto overflow-hidden">
    <div className="h-full bg-accent animate-progress-loading" />
  </div>
</div>
```

### PENDING / PENDING_UPLOAD

```tsx
<div className="rounded-2xl bg-surface p-8 text-center space-y-3">
  <p className="text-sm font-semibold text-text-secondary">영상 업로드를 기다리고 있어요</p>
  <p className="text-xs text-text-tertiary">면접 영상이 업로드되면 자동으로 분석이 시작됩니다</p>
</div>
```

### SKIPPED

```tsx
<div className="rounded-2xl bg-surface p-8 text-center">
  <p className="text-sm font-semibold text-text-tertiary">이 질문세트는 건너뛰었습니다</p>
</div>
```

## 담당 에이전트

- Implement: `frontend` — 분석 상태 UI 분기
- Review: `designer` — 대기 상태 UX

## 검증

- `PENDING` 상태에서 업로드 대기 메시지 표시
- `ANALYZING` 상태에서 Character + 프로그레스 바 표시
- `SKIPPED` 상태에서 건너뛰기 메시지 표시
- `FAILED` 상태는 기존 UI 유지
- `progress.md` 상태 업데이트 (Task 4 → Completed)
