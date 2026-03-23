# Plan 06: Frontend 음성 특성 피드백 UI + 프로그레스 변경

> 상태: Draft
> 작성일: 2026-03-23

## Why

Gemini가 생성하는 음성 특성 피드백(톤 자신감, 감정, 말빠르기, 필러워드)을 사용자에게 보여줘야 한다. 또한 AnalysisProgress 단계가 변경되므로 프로그레스 UI도 수정해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-analysis-page.tsx` | `PROGRESS_STEPS` 배열 수정 (6단계 → 4단계) |
| 피드백 뷰어 컴포넌트 | 음성 특성 피드백 영역 추가 |

## 상세

### 프로그레스 UI 변경

현재 하드코딩된 6단계:
```typescript
const PROGRESS_STEPS = [
  { key: 'STARTED', label: '준비', fullLabel: '분석 준비 중' },
  { key: 'EXTRACTING', label: '영상', fullLabel: '영상 처리 중' },
  { key: 'STT_PROCESSING', label: '음성', fullLabel: '음성 인식 중' },
  { key: 'VERBAL_ANALYZING', label: '언어', fullLabel: '언어 분석 중' },
  { key: 'NONVERBAL_ANALYZING', label: '비언어', fullLabel: '비언어 분석 중' },
  { key: 'FINALIZING', label: '생성', fullLabel: '피드백 생성 중' },
]
```

변경 후 4단계:
```typescript
const PROGRESS_STEPS = [
  { key: 'STARTED', label: '준비', fullLabel: '분석 준비 중' },
  { key: 'EXTRACTING', label: '추출', fullLabel: '영상 처리 중' },
  { key: 'ANALYZING', label: '분석', fullLabel: 'AI가 답변을 분석 중' },
  { key: 'FINALIZING', label: '생성', fullLabel: '종합 피드백 생성 중' },
]
```

**하위 호환**: 기존 progress 값(`STT_PROCESSING` 등)이 오면 `ANALYZING`으로 매핑하는 폴백 로직 추가.

### 피드백 뷰어 음성 특성 영역

기존 비언어 분석 영역 옆에 음성 특성 피드백 영역 추가:

- 톤 자신감 점수 (0-100, 게이지 또는 바)
- 감정 레이블 (자신감/긴장/평온/불안 — 배지)
- 말 빠르기 (빠름/적절/느림)
- 필러워드 목록 + 횟수
- 음성 종합 코멘트

## 담당 에이전트

- Implement: `frontend` — 컴포넌트, 상태 관리
- Review: `designer` — UI/UX 일관성
- Review: `code-reviewer` — 코드 품질

## 검증

- 프로그레스 바가 4단계로 정상 표시
- 기존 progress 값 호환성 (STT_PROCESSING → ANALYZING 매핑)
- 음성 특성 피드백 영역이 비언어 피드백 옆에 표시
- 음성 데이터 없는 기존 피드백에서 에러 없이 렌더링
- `progress.md` 상태 업데이트 (Task 6 → Completed)
