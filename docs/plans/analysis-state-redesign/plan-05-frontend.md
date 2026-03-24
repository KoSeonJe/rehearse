# Plan 05: FE 타입 + 폴링 + UI 수정

> 상태: Draft
> 작성일: 2026-03-24

## Why

Backend 상태 모델이 변경되면 FE의 타입 정의, 폴링 응답 처리, 상태 조건 분기, 진행률 UI가 모두 영향받는다. AnalysisProgress 제거 + PARTIAL 상태 추가 + convertStatus 신규 필드에 맞춰 수정한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/types/interview.ts` | AnalysisStatus 확장 (EXTRACTING, ANALYZING, FINALIZING, PARTIAL 추가). FileStatus 축소 (CONVERTING, CONVERTED 제거). AnalysisProgress 타입 제거. ConvertStatus 타입 추가. QuestionSetStatusResponse 타입 수정 |
| `frontend/src/hooks/use-question-sets.ts` | 폴링 응답 타입 변경 (analysisProgress 제거, convertStatus/isVerbalCompleted/isNonverbalCompleted/fullyReady 추가) |
| `frontend/src/pages/interview-analysis-page.tsx` | PROGRESS_STEPS 재작성 (AnalysisStatus 기반). LEGACY_ANALYZING_KEYS 제거. 상태 조건 분기에 PARTIAL 추가. 재시도 조건에 PARTIAL 추가 |
| `frontend/src/pages/interview-feedback-page.tsx` | statusConfig에 PARTIAL/EXTRACTING/FINALIZING 추가. 피드백 조회 조건에 PARTIAL 추가 |
| `frontend/src/components/feedback/feedback-panel.tsx` | 영향 최소 (TimestampFeedback 구조 변경 없음) |
| `frontend/src/stores/interview-store.ts` | QuestionSetData 타입에 analysisStatus 소스 변경 확인 (BE 응답에서 자동 매핑) |

## 상세

### types/interview.ts 변경

```typescript
// 변경 전
export type AnalysisStatus = 'PENDING' | 'PENDING_UPLOAD' | 'ANALYZING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
export type FileStatus = 'PENDING' | 'UPLOADED' | 'CONVERTING' | 'CONVERTED' | 'FAILED'
export type AnalysisProgress = 'STARTED' | 'EXTRACTING' | ... | 'FAILED'

// 변경 후
export type AnalysisStatus = 'PENDING' | 'PENDING_UPLOAD' | 'EXTRACTING' | 'ANALYZING' | 'FINALIZING' | 'COMPLETED' | 'PARTIAL' | 'FAILED' | 'SKIPPED'
export type ConvertStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type FileStatus = 'PENDING' | 'UPLOADED' | 'FAILED'
// AnalysisProgress 삭제

export interface QuestionSetStatusResponse {
  id: number
  analysisStatus: AnalysisStatus
  convertStatus: ConvertStatus          // 신규
  fileStatus: FileStatus | null
  isVerbalCompleted: boolean             // 신규
  isNonverbalCompleted: boolean          // 신규
  fullyReady: boolean                    // 신규
  failureReason: string | null
}
```

### interview-analysis-page.tsx 변경

```typescript
// 변경 전: AnalysisProgress 기반 단계
const PROGRESS_STEPS = [
  { key: 'STARTED', label: '분석 준비 중' },
  { key: 'EXTRACTING', label: '영상 처리 중' },
  { key: 'ANALYZING', label: 'AI 분석 중' },
  { key: 'FINALIZING', label: '피드백 생성 중' },
]

// 변경 후: AnalysisStatus 기반 단계
const PROGRESS_STEPS = [
  { key: 'PENDING_UPLOAD', label: '업로드 대기 중' },
  { key: 'EXTRACTING', label: '영상 처리 중' },
  { key: 'ANALYZING', label: 'AI 분석 중' },
  { key: 'FINALIZING', label: '피드백 생성 중' },
]
```

상태 조건 분기 변경:
```typescript
// 변경 전
s?.analysisStatus === 'COMPLETED' || s?.analysisStatus === 'SKIPPED'

// 변경 후 (PARTIAL도 완료로 취급, 폴링 중단 조건)
const isTerminal = (s) => ['COMPLETED', 'PARTIAL', 'FAILED', 'SKIPPED'].includes(s?.analysisStatus)
// allTerminal이면 폴링 중단
```

재시도 조건 변경:
```typescript
// 변경 전
statuses[idx]?.analysisStatus === 'FAILED' || statuses[idx]?.fileStatus === 'FAILED'

// 변경 후
statuses[idx]?.analysisStatus === 'FAILED' || statuses[idx]?.analysisStatus === 'PARTIAL' || statuses[idx]?.convertStatus === 'FAILED'
```

### interview-feedback-page.tsx 변경

```typescript
// statusConfig에 PARTIAL 추가
const statusConfig = {
  ...
  PARTIAL: { subtitle: '일부 분석 완료', icon: AlertIcon, color: 'amber' },
  EXTRACTING: { subtitle: '영상 처리 중', icon: LoadingIcon, color: 'blue' },
  FINALIZING: { subtitle: '피드백 생성 중', icon: LoadingIcon, color: 'blue' },
}

// 피드백 조회 조건에 PARTIAL 추가
const shouldFetchFeedback = analysisStatus === 'COMPLETED' || analysisStatus === 'PARTIAL'
```

## 담당 에이전트

- Implement: `frontend` — 타입 정의, 훅, 페이지 수정
- Review: `code-reviewer` — 상태 조건 분기 누락 확인

## 검증

- 타입 빌드 에러 없는지 확인 (`npm run build`)
- 분석 대기 UI: EXTRACTING → ANALYZING → FINALIZING 진행률 표시
- PARTIAL 상태: "일부 분석 완료" 표시 + 재시도 버튼 활성화
- COMPLETED: 피드백 정상 조회
- FAILED: 재시도 버튼 활성화
- `progress.md` 상태 업데이트 (Task 5 → Completed)
