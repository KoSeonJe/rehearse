# Plan 01: 피드백 카드 → 코칭 카드 재설계 [blocking]

> 상태: Draft
> 작성일: 2026-03-20

## Why

피드백 카드의 핵심 정보(점수, 코멘트)가 `isAnalyzed` + null 체크로 숨겨져 있어 대부분의 카드가 트랜스크립트만 표시한다. 정보 계층이 역전되어 가장 actionable한 정보(종합 코멘트, 점수)가 가장 아래에 작게 표시되고, 가장 덜 actionable한 정보(트랜스크립트)가 가장 크게 표시된다. (이슈 #1, #2, #6)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/feedback/feedback-panel.tsx` | FeedbackCard 컴포넌트 전면 재설계 |

## 상세

### 1. 정보 계층 재배치

**현재 순서**: 시간 → 질문 → 트랜스크립트(큼) → 점수(작음) → 코멘트(작음) → 모범답변
**변경 순서**: 시간+타입 → 질문 → **점수 요약 블록(큼)** → **종합 코멘트(큼)** → 트랜스크립트(접힘) → 모범답변(접힘)

### 2. 점수 요약 블록 신설

`ScoreBadge`를 개선하여 `ScoreBar` 컴포넌트로 교체:

```
언어 72/100 [━━━━━━━░░░] 보통
```

- 숫자 + 프로그레스 바 + 라벨(우수/보통/미흡)
- 색상: >=80 → success "우수", >=50 → yellow "보통", <50 → error "미흡"
- 바는 Tailwind gradient (`bg-gradient-to-r`)
- 비언어 점수(시선/자세/표정)는 한 줄 가로 배치 유지

### 3. 조건부 렌더링 정책 변경

**현재**: `isAnalyzed`가 false이면 점수/코멘트 섹션 전체를 숨김
**변경**: 항상 섹션 구조를 표시하되, 데이터가 없으면 "분석 대기 중" 플레이스홀더

```tsx
// Before (숨김)
{feedback.isAnalyzed && (
  <div className="space-y-3">...</div>
)}

// After (항상 표시)
<div className="space-y-3">
  {feedback.verbalScore !== null ? (
    <ScoreBar label="언어" score={feedback.verbalScore} />
  ) : (
    <div className="text-xs text-text-tertiary">언어 분석 대기 중</div>
  )}
</div>
```

### 4. 트랜스크립트 접기

트랜스크립트를 기본 접힘 상태로 변경. 토글 버튼으로 펼침.
- 버튼 라벨: "답변 텍스트 보기" / "답변 텍스트 접기"
- 모범답변 토글과 동일한 패턴

### 5. 타이포그래피 계층 정상화 (이슈 #8 부분 적용)

FeedbackCard 내부:
- 시간 배지: `text-[10px] font-bold` (기존 font-black → bold)
- 질문: `text-sm font-semibold` (기존 text-xs font-bold)
- 점수 라벨: `text-xs font-medium`
- 점수 숫자: `text-sm font-black` (강조 유지)
- 코멘트: `text-sm font-normal leading-relaxed`
- 토글 버튼: `text-xs font-semibold text-accent`

## 담당 에이전트

- Implement: `frontend` — FeedbackCard 컴포넌트 재설계, ScoreBar 컴포넌트
- Review: `designer` — 정보 계층, 토스 디자인 토큰 준수
- Review: `code-reviewer` — 코드 품질, 접근성

## 검증

- 빌드 성공 (`npm run build`)
- `isAnalyzed: false`일 때 점수 섹션에 플레이스홀더 표시 확인
- `verbalScore: null`일 때 "분석 대기 중" 표시 확인
- 트랜스크립트 토글 정상 동작
- 점수 라벨이 "우수/보통/미흡"으로 표시되는지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
