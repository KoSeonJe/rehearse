# Feature Specification: 종합 리포트 페이지

> **문서 ID**: PLAN-009
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P1 (High Impact)

---

## Overview

### 문제 정의

종합 리포트 API가 제공하는 점수, 요약, 강점, 개선점을 사용자가 직관적으로 이해할 수 있는 페이지가 필요하다. 점수를 시각화하고, 강점과 개선점을 카드 형태로 명확하게 구분하여 표시해야 한다.

### 솔루션 요약

React 페이지로 종합 리포트를 표시한다. 상단에 점수 카드(원형 진행 바), 가운데에 요약문, 하단에 강점/개선점을 2열 그리드로 표시한다. TanStack Query `useReport()` 훅으로 API 데이터를 페칭하고, 로딩/에러 상태를 Character 컴포넌트로 표시한다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 사용자가 최종 결과를 시각적으로 이해하는 핵심 페이지 |
| **Effort** | Low - 기존 컴포넌트 조합, 새로운 로직 없음 |
| **결론** | **P1** - 종합 리포트 API 완료 후 즉시 개발 |

---

## Frontend Page

### 페이지: `interview-report-page.tsx`

**경로**: `/interview/:id/report`

**목적**: 종합 리포트 조회 및 표시

**주요 기능**:
- `useReport(id)` 훅으로 `GET /api/v1/interviews/{id}/report` 호출
- 로딩 상태: Character mood="thinking" + "리포트를 생성하고 있습니다..." 메시지
- 에러 상태: Character mood="confused" + "리포트를 불러올 수 없습니다" + 홈 링크
- 성공 상태: 헤더 + 점수 카드 + 요약 섹션 + 강점/개선점 섹션

**Props**: 없음 (URL 파라미터 `id`에서 인터뷰 ID 추출)

**레이아웃**:

```
┌─────────────────────────────────┐
│ [Logo] Rehearse | 종합 리포트      │ (헤더)
├─────────────────────────────────┤
│                                 │
│         [점수 카드]              │ (중앙 정렬)
│       75 / 우수                  │
│      ▓▓▓▓▓▓▓▓░░                 │
│                                 │
├─────────────────────────────────┤
│                                 │
│  [종합 평가 섹션]                │
│  "전반적으로 좋은 답변이었습니다..." │
│  (피드백 8개 기반)               │
│                                 │
├─────────────────────────────────┤
│                                 │
│  [강점]          [개선 포인트]   │ (2열)
│  1. 깊이 있는... 1. 속도 조절... │
│  2. 명확한...     2. 예시 제시... │
│  3. 긍정적...     3. 자세 개선... │
│                                 │
├─────────────────────────────────┤
│  [다음 단계 섹션]                │
│  "타임스탬프 리뷰를 확인하세요"  │
│  [타임스탬프 리뷰 보기] 버튼     │
│                                 │
└─────────────────────────────────┘
```

**헤더 버튼**:
- 좌측: 로고 + "Rehearse" + "종합 리포트" (sm 이상에서만)
- 우측: "피드백 리뷰" 버튼 (secondary, sm 이상) + "홈으로" 버튼 (primary)

---

## Frontend Components

### 컴포넌트: `score-card.tsx`

**목적**: 종합 점수를 시각적으로 표시

**Props**:
```typescript
interface ScoreCardProps {
  score: number  // 0-100
}
```

**기능**:
- 상단: "종합 점수" 라벨
- 중앙: 점수 숫자 (text-5xl, bold)
- 점수에 따라 색상 변경:
  - 80-100: success (초록색, "우수")
  - 60-79: info (파란색, "양호")
  - 40-59: warning (주황색, "보통")
  - 0-39: error (빨간색, "개선 필요")
- 레이블: 점수 아래 (색상 맞춤)
- 진행 바: 하단 (width: `${score}%`, 색상 맞춤)

**스타일**:
- 배경: surface (흰색)
- 테두리: border (회색)
- padding: 32px (p-8)
- 중앙 정렬 (flex flex-col items-center)

---

### 컴포넌트: `improvement-list.tsx`

**목적**: 강점/개선점 리스트를 카드 형태로 표시

**Props**:
```typescript
interface ImprovementListProps {
  title: string                        // "강점" 또는 "개선 포인트"
  items: string[]                      // 3개 문자열 배열
  variant: 'strength' | 'improvement'
}
```

**기능**:
- 제목: variant별 색상
  - strength: success (초록색)
  - improvement: warning (주황색)
- 배경: variant별 light 색상
  - strength: success-light + border-success/20
  - improvement: warning-light + border-warning/20
- 항목: 순번 배지 + 텍스트
  - 배지: 동그란 원 (h-5 w-5), 텍스트 중앙 정렬, 순번 (1, 2, 3)
  - 배지 배경: variant별 색상
  - 텍스트: text-primary, 왼쪽 정렬

**레이아웃**:
```
┌─────────────────────┐
│ 강점                │ (제목, 초록색)
├─────────────────────┤
│ ① 자료구조에 대한...│
│                     │
│ ② 명확한 문제 분석  │
│                     │
│ ③ 긍정적인 태도 유지│
│                     │
└─────────────────────┘
```

---

## Custom Hook: `use-report.ts`

**목적**: 종합 리포트 데이터 페칭

```typescript
export const useReport = (interviewId: string) => {
  return useQuery({
    queryKey: ['report', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewReport>>(
        `/api/v1/interviews/${interviewId}/report`,
      ),
    enabled: !!interviewId,
  })
}
```

**반환값**:
```typescript
{
  data?: ApiResponse<InterviewReport>
  isLoading: boolean
  isError: boolean
  error?: Error
}
```

**동작**:
- TanStack Query의 `useQuery` 래퍼
- `queryKey: ['report', interviewId]`로 캐싱
- `enabled: !!interviewId` (ID 있을 때만 자동 호출)

---

## Data Types

### `InterviewReport`

```typescript
interface InterviewReport {
  id: number
  interviewId: number
  overallScore: number           // 0-100
  summary: string                // 3-4문장
  strengths: string[]            // 3개 문자열
  improvements: string[]         // 3개 문자열
  feedbackCount: number          // 피드백 개수
}
```

---

## Page Flow

```
[피드백 리뷰 페이지]
       ↓
    [링크 클릭]
       ↓
[종합 리포트 페이지] ← 이 페이지
       │
       ├── "피드백 리뷰" 버튼 → [피드백 리뷰 페이지]
       └── "홈으로" 버튼 → [홈 페이지]
```

---

## 색상 시스템

**점수별 색상** (ScoreCard):

| 범위 | 라벨 | 텍스트 | 진행 바 |
|------|------|--------|--------|
| 80-100 | 우수 | text-success | bg-success |
| 60-79 | 양호 | text-info | bg-info |
| 40-59 | 보통 | text-warning | bg-warning |
| 0-39 | 개선 필요 | text-error | bg-error |

**카드 색상** (ImprovementList):

| Variant | 제목색 | 배경색 | 테두리색 | 배지색 |
|---------|-------|--------|---------|--------|
| strength | text-success | bg-success-light | border-success/20 | bg-success |
| improvement | text-warning | bg-warning-light | border-warning/20 | bg-warning |

---

## 반응형 디자인

**데스크톱 (md 이상)**:
- 강점/개선점: 2열 그리드 (`grid-cols-2`)

**태블릿/모바일 (md 미만)**:
- 강점/개선점: 1열 스택 (`grid-cols-1`)

**컨테이너**:
- `max-w-3xl` (최대 너비 768px)
- `mx-auto` (중앙 정렬)
- 좌우 패딩: 모바일 16px, 데스크톱 auto (px-4 sm:px-6)

---

## 주요 구현 결정사항

1. **로딩 상태**
   - Character mood="thinking" (생각하는 표정)
   - 텍스트: "리포트를 생성하고 있습니다..."

2. **에러 상태**
   - Character mood="confused" (당황한 표정)
   - 텍스트: "리포트를 불러올 수 없습니다"
   - 버튼: "홈으로 돌아가기" (secondary)

3. **점수 시각화**
   - 진행 바 (width: `${score}%`)로 점수 비율 표시
   - 색상: 점수 범위에 따라 동적 변경
   - 레이블: 숫자 아래에 등급 명시

4. **강점/개선점 순번**
   - 배지 (1, 2, 3) 사용
   - 숫자 중앙 정렬 (font-bold, text-white)

5. **섹션 간 여백**
   - 점수 카드 - 요약: 24px (space-y-6)
   - 요약 - 강점/개선: 24px
   - 강점/개선 - 다음단계: 24px

6. **API 캐싱**
   - TanStack Query queryKey로 자동 캐싱
   - 같은 인터뷰 재방문 시 캐시 활용

---

## 상태 전이

**리포트 페이지 생명주기**:

1. **마운트**:
   - useReport 호출 → API 요청 (loading 상태)

2. **로딩 중**:
   - Character mood="thinking" + 로딩 메시지

3. **성공**:
   - ScoreCard + 요약 + ImprovementList 렌더

4. **에러**:
   - Character mood="confused" + 에러 메시지 + 홈 링크

5. **언마운트**:
   - 자동으로 TanStack Query 캐시 유지

---

## 테스트 전략 (QA 참고)

- [x] 페이지 로드: useReport API 호출
- [x] 로딩 상태: Character mood="thinking" 표시
- [x] 성공 상태: 점수, 요약, 강점/개선점 모두 표시
- [x] 에러 상태: 에러 메시지 + 홈 버튼
- [x] 점수 색상: 80이상→초록, 60-79→파란, 40-59→주황, 0-39→빨강
- [x] 반응형: 모바일/태블릿/데스크톱 모두 정상 표시
- [x] 버튼 동작: "피드백 리뷰", "홈으로" 이동 정상
- [x] 캐싱: 재방문 시 로딩 없음

---

## 의존성

| 의존성 | 상태 | 설명 |
|--------|------|------|
| `react-router-dom` | 기존 | URL 파라미터, 네비게이션 |
| `@tanstack/react-query` | 기존 | 서버 상태 |
| `LogoIcon` | 기존 | 헤더 로고 |
| `Button` | 기존 | 헤더/푸터 버튼 |
| `Character` | 기존 | 로딩/에러 상태 표시 |
| Backend API | 완료 | GET /api/v1/interviews/{id}/report |

---

## 완료 기준

- [x] interview-report-page.tsx (페이지)
- [x] score-card.tsx (점수 카드)
- [x] improvement-list.tsx (강점/개선점)
- [x] use-report.ts (TanStack Query 훅)
- [x] 로딩/에러 상태 UI
- [x] 반응형 디자인
- [x] 색상 시스템 적용
- [x] API 통합 테스트

---

## 완료된 작업

**Frontend 구현 완료 (PR #9)**:
- interview-report-page.tsx
- score-card.tsx
- improvement-list.tsx
- use-report.ts
