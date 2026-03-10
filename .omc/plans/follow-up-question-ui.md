# Feature Specification: 후속 질문 UI

> **문서 ID**: PLAN-005
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P0 (Must-have)
> **의존성**: PLAN-003 (후속 질문 생성 API) 완료

---

## Overview

### 문제 정의

후속 질문이 API를 통해 생성되고 있지만, 프론트엔드에서 이를 사용자에게 명확하고 시각적으로 표시해야 한다. 후속 질문의 타입(심화/명확화/반론/적용)을 사용자가 이해할 수 있도록 하고, 로딩 상태와 질문 내용을 구분되게 표시해야 한다.

### 솔루션 요약

면접 진행 페이지의 QuestionDisplay 컴포넌트를 확장하여:
1. 원본 질문을 상단에 표시
2. 후속 질문 생성 중 로딩 인디케이터 표시
3. 후속 질문이 준비되면 타입 배지, 질문 내용, 생성 이유를 카드로 표시
4. 각 질문별로 후속 질문을 저장하는 Zustand 스토어 확장

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 사용자가 후속 질문을 실제로 볼 수 있어야 기능이 완성됨 |
| **Effort** | Low - UI 컴포넌트 작은 확장 + 스토어 상태 관리 |
| **결론** | **P0** - High Impact, Low Effort |

---

## Target Users

- **모든 면접 사용자**: 후속 질문을 명확하게 이해하고 답변 준비
- **시각적 학습자**: 타입 배지와 색상을 통한 직관적 이해

---

## User Stories

### US-1: 후속 질문 로딩 상태 표시

**As a** 면접 중인 사용자
**I want to** 후속 질문이 생성되고 있음을 시각적으로 알 수 있기를 원한다
**So that** 서버가 처리 중임을 이해하고 인내심을 갖고 기다릴 수 있다

**Acceptance Criteria:**
- [ ] 로딩 중에 회전하는 스피너가 표시된다
- [ ] "후속 질문을 생성하고 있습니다..." 메시지가 표시된다
- [ ] 로딩 상태의 배경색이 정보 색상(info)으로 구분된다
- [ ] 로딩 상태는 최대 10초 이상 표시되지 않는다 (타임아웃)

**Priority:** P0

---

### US-2: 후속 질문 표시

**As a** 후속 질문이 생성된 면접자
**I want to** 후속 질문이 명확하게 표시되기를 원한다
**So that** 추가 질문의 의도를 파악하고 신중하게 답변할 수 있다

**Acceptance Criteria:**
- [ ] 후속 질문 타입이 배지로 표시된다 (예: "심화", "명확화", "반론", "적용")
- [ ] 배지의 색상이 정보 색상(info)으로 통일된다
- [ ] 후속 질문 내용이 원본 질문 아래에 카드 형태로 표시된다
- [ ] 후속 질문이 생성된 이유가 작은 텍스트로 표시된다
- [ ] 후속 질문 카드의 배경색이 정보 색상의 연한 버전(info-light)이다

**Priority:** P0

---

### US-3: 질문별 후속 질문 관리

**As a** 여러 질문에 답변하는 면접자
**I want to** 각 질문별로 별도의 후속 질문을 가질 수 있기를 원한다
**So that** 질문을 전환해도 이전 질문의 후속 질문이 유지된다

**Acceptance Criteria:**
- [ ] 질문 1의 후속 질문이 생성되었을 때, 질문 2로 이동해도 유지된다
- [ ] 질문 1로 돌아오면 생성된 후속 질문이 다시 표시된다
- [ ] 같은 질문으로 돌아와도 후속 질문이 중복 요청되지 않는다
- [ ] Map 구조를 사용하여 `Map<questionIndex, FollowUpResponse>` 형태로 저장된다

**Priority:** P0

---

## Scope

### In Scope

1. **Frontend Components**
   - QuestionDisplay 컴포넌트 확장 (후속 질문 표시)
   - 로딩 상태 스피너 UI
   - 후속 질문 카드 UI
   - 타입 배지 (심화/명확화/반론/적용)

2. **Frontend State Management**
   - Zustand Store에 followUpQuestions Map 추가
   - addFollowUpQuestion 액션
   - setFollowUpLoading 액션

3. **Frontend Logic**
   - InterviewPage에서 후속 질문 자동 요청
   - 답변 완료 시 후속 질문 생성 로직
   - 생성 실패 시 에러 처리

### Out of Scope

| 항목 | 사유 |
|------|------|
| 후속 질문 시각화 (그래프/차트) | P2. MVP에서는 텍스트 표시만 |
| 후속 질문 음성 재생 | P2. TTS 기능 |
| 후속 질문 다시 생성 | P2. 1회 생성 후 선택지 없음 |
| 후속 질문 거부/스킵 UI | 다음 질문으로 자동 진행 가능 |

---

## Component Specification

### QuestionDisplay Component

**Location:** `frontend/src/components/interview/question-display.tsx`

**Props:**
```typescript
interface QuestionDisplayProps {
  question: Question
  currentIndex: number
  totalCount: number
  followUp?: FollowUpResponse       // NEW
  isFollowUpLoading?: boolean       // NEW
}
```

**Structure:**
```
┌─────────────────────────────────────┐
│ 원본 질문 카드                         │
│ ┌─────────────────────────────────┐ │
│ │ [1] 1 / 5  [자료구조]            │ │
│ │ "HashMap과 TreeMap의 차이점...  │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
        ↓
┌─────────────────────────────────────┐
│ 후속 질문 로딩 상태 (선택적)          │
│ ┌─────────────────────────────────┐ │
│ │ [스피너] 후속 질문을 생성 중...   │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
        ↓
┌─────────────────────────────────────┐
│ 후속 질문 카드 (선택적)               │
│ ┌─────────────────────────────────┐ │
│ │ [후속 질문] [심화]               │ │
│ │ "좋은 설명이네요. HashMap의...   │ │
│ │ "이 질문을 선택한 이유..."        │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

**Rendering Logic:**

```typescript
const QuestionDisplay = ({
  question,
  currentIndex,
  totalCount,
  followUp,        // FollowUpResponse | undefined
  isFollowUpLoading // boolean | undefined
}: QuestionDisplayProps) => {
  return (
    <div className="space-y-3">
      {/* 1. 원본 질문 카드 (항상 표시) */}
      <div className="rounded-card border border-border bg-surface p-6">
        {/* 질문 메타정보 */}
        {/* 질문 내용 */}
      </div>

      {/* 2. 후속 질문 로딩 상태 (isFollowUpLoading === true일 때만) */}
      {isFollowUpLoading && (
        <div className="rounded-card border border-info/30 bg-info-light p-4">
          {/* 로딩 스피너 + 메시지 */}
        </div>
      )}

      {/* 3. 후속 질문 카드 (followUp이 있고 로딩 중이 아닐 때) */}
      {followUp && !isFollowUpLoading && (
        <div className="rounded-card border border-info/30 bg-info-light p-5">
          {/* 타입 배지 + 질문 + 이유 */}
        </div>
      )}
    </div>
  )
}
```

**Styling Details:**

원본 질문 카드:
- 배경: `bg-surface`
- 테두리: `border-border`
- 패딩: `p-6`
- 둥글기: `rounded-card`

후속 질문 로딩 상태:
- 배경: `bg-info-light` (연한 파란색)
- 테두리: `border-info/30` (투명도 30%)
- 패딩: `p-4`

후속 질문 카드:
- 배경: `bg-info-light`
- 테두리: `border-info/30`
- 패딩: `p-5`

타입 배지:
- 배경: `bg-info/10` (투명도 10%)
- 텍스트: `text-info`

---

## Type Label Mapping

```typescript
const FOLLOW_UP_TYPE_LABELS: Record<string, string> = {
  DEEP_DIVE: '심화',
  CLARIFICATION: '명확화',
  CHALLENGE: '반론',
  APPLICATION: '적용',
}
```

**사용 위치:**
```typescript
<span className="rounded-badge bg-info/10 px-2.5 py-0.5 text-xs font-medium text-info">
  {FOLLOW_UP_TYPE_LABELS[followUp.type] ?? followUp.type}
</span>
```

---

## Store Specification

### Zustand Store Extension

**Location:** `frontend/src/stores/interview-store.ts`

**New Fields:**
```typescript
interface InterviewStore {
  // 기존 필드...

  // NEW: 후속 질문 관리
  followUpQuestions: Map<number, FollowUpResponse>
  isFollowUpLoading: boolean

  // NEW: 액션
  addFollowUpQuestion: (questionIndex: number, followUp: FollowUpResponse) => void
  setFollowUpLoading: (loading: boolean) => void
}
```

**Implementation:**
```typescript
const useInterviewStore = create<InterviewStore>((set) => ({
  // 기존 상태...

  followUpQuestions: new Map(),
  isFollowUpLoading: false,

  addFollowUpQuestion: (questionIndex, followUp) =>
    set((state) => {
      const newMap = new Map(state.followUpQuestions)
      newMap.set(questionIndex, followUp)
      return { followUpQuestions: newMap }
    }),

  setFollowUpLoading: (loading) =>
    set({ isFollowUpLoading: loading }),
}))
```

---

## Integration with InterviewPage

**Location:** `frontend/src/pages/interview-page.tsx`

### Step 1: 후속 질문 뮤테이션 준비

```typescript
const followUpMutation = useFollowUpQuestion()
```

### Step 2: 답변 완료 시 후속 질문 요청

```typescript
const handleStopAnswer = useCallback(() => {
  // 1. STT/레코더 중지
  stopRecording()
  stt.stop()
  recorder.pause()

  // 2. 현재 답변 텍스트 수집
  const currentAnswer = useInterviewStore.getState().answers[currentQuestionIndex]
  const answerText = currentAnswer?.transcripts
    .filter((t) => t.isFinal)
    .map((t) => t.text)
    .join(' ')

  // 3. 후속 질문 요청
  if (answerText && interview) {
    setFollowUpLoading(true)
    followUpMutation.mutate(
      {
        id: interview.id,
        data: {
          questionContent: questions[currentQuestionIndex].content,
          answerText,
        },
      },
      {
        onSuccess: (res) => {
          addFollowUpQuestion(currentQuestionIndex, res.data)
          setFollowUpLoading(false)
        },
        onError: () => {
          setFollowUpLoading(false)
        },
      },
    )
  }
}, [/* 의존성 */])
```

### Step 3: QuestionDisplay에 Props 전달

```typescript
<QuestionDisplay
  question={currentQuestion}
  currentIndex={currentQuestionIndex}
  totalCount={questions.length}
  followUp={followUpQuestions.get(currentQuestionIndex)}
  isFollowUpLoading={isFollowUpLoading}
/>
```

---

## Data Flow

```
[질문 표시]
    │
    ├─ question: 원본 질문
    ├─ currentIndex: 0
    └─ totalCount: 5
        │
        ▼
[원본 질문 카드 렌더링]
    │
    │ 사용자가 "답변 시작" → "답변 완료" 클릭
    ▼
[handleStopAnswer 호출]
    │
    ├─ STT/레코더 중지
    ├─ 답변 텍스트 수집
    ├─ setFollowUpLoading(true)
    └─ followUpMutation.mutate(...)
        │
        ▼
[QuestionDisplay 리렌더링]
    │
    ├─ isFollowUpLoading = true
    └─ 로딩 카드 표시
        │
        ▼
[API 응답 (3초 후)]
    │
    ├─ addFollowUpQuestion(0, followUp)
    ├─ setFollowUpLoading(false)
    │
    ▼
[QuestionDisplay 리렌더링]
    │
    ├─ followUpQuestions.get(0) = followUp
    ├─ isFollowUpLoading = false
    └─ 후속 질문 카드 표시
```

---

## Testing Scenarios

### Scenario 1: 정상 후속 질문 생성
- 질문 1 답변 완료
- 로딩 상태 표시
- 3초 후 후속 질문 카드 표시
- 질문 2로 이동
- 질문 1로 돌아옴
- 저장된 후속 질문이 다시 표시됨

### Scenario 2: 후속 질문 생성 실패
- 질문 1 답변 완료
- 로딩 상태 표시
- API 에러 응답
- 로딩 상태 해제, 후속 질문 미표시
- 다음 질문으로 진행 가능 (에러가 면접을 막지 않음)

### Scenario 3: 모든 질문에 후속 질문
- 5개 질문 모두 답변
- 각 질문별로 독립적인 후속 질문 저장
- 질문 간 이동 시 각각의 후속 질문이 표시됨

---

## Accessibility & UX

1. **로딩 스피너**: 명확한 시각적 피드백 제공
2. **배지 색상**: 타입을 색상으로 구분 가능하지만, 텍스트 레이블도 함께 제공
3. **텍스트 대비**: info 색상(파란색)이 배경(흰색)과 충분한 명도 대비
4. **반응형**: 모바일에서도 카드 너비 100% 유지

---

## Browser Compatibility

- 최신 Chrome/Firefox/Safari 지원
- `Map` 자료구조는 ES6+ 지원 필수
- Tailwind CSS 기본 클래스만 사용 (커스텀 클래스 없음)

---

## Success Criteria

- [x] QuestionDisplay 컴포넌트에 followUp, isFollowUpLoading Props 추가
- [x] 로딩 상태 UI 구현
- [x] 후속 질문 카드 UI 구현
- [x] 타입 레이블 매핑 구현
- [x] Zustand Store 확장 (Map 구조)
- [x] InterviewPage에서 후속 질문 자동 요청
- [x] 질문 전환 시 후속 질문 유지
- [x] 생성 실패 시에도 면접 진행 가능
- [x] 모바일 반응형 디자인
