# Feature Specification: 피드백 요청 연동 + 완료 페이지

> **문서 ID**: PLAN-006
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P0 (Must-have)
> **의존성**: PLAN-002 (면접 진행), PLAN-004 (AI 피드백 API) 완료

---

## Overview

### 문제 정의

면접이 모든 질문에 대해 완료되면 사용자를 새로운 페이지로 안내하여 자동으로 피드백을 생성하도록 요청해야 한다. 피드백 생성 중 로딩 상태를 표시하고, 완료 후 리뷰 페이지와 리포트 페이지로 네비게이션할 수 있어야 한다.

### 솔루션 요약

면접 진행 페이지에서 "면접 종료" 버튼을 누르면:
1. 면접 상태가 COMPLETED로 변경된다
2. InterviewCompletePage로 자동 이동한다
3. 모든 답변 데이터가 자동으로 수집되고 피드백 생성 API로 전송된다
4. 로딩 중에는 AI 아바타와 함께 "피드백 생성 중" 메시지가 표시된다
5. 완료되면 "타임스탬프 피드백 리뷰" 및 "종합 리포트" 버튼이 표시된다
6. 실패 시 에러 메시지와 함께 홈으로 돌아가기 버튼이 표시된다

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | Critical - 면접 종료와 피드백 생성의 연결점 |
| **Effort** | Low - 기존 훅과 API 연동만 필요 |
| **결론** | **P0** - Critical Impact, Low Effort |

---

## Target Users

- **모든 면접 사용자**: 면접 완료 후 자동으로 피드백을 받는 경험
- **경험 부족한 사용자**: 아바타와 메시지를 통한 편안한 UI

---

## User Stories

### US-1: 면접 종료 및 피드백 요청

**As a** 모든 질문 답변을 완료한 면접자
**I want to** "면접 종료" 버튼을 누르고 자동으로 피드백을 받고 싶다
**So that** 바로 피드백을 통해 개선점을 파악할 수 있다

**Acceptance Criteria:**
- [ ] InterviewPage에서 "면접 종료" 버튼이 마지막 질문(5/5)에서만 표시된다
- [ ] 버튼 클릭 시 면접 상태가 COMPLETED로 변경된다
- [ ] InterviewCompletePage로 자동 이동한다
- [ ] 페이지 진입 시 자동으로 피드백 생성 요청이 시작된다 (사용자 입력 불필요)
- [ ] 요청은 1회만 실행된다 (중복 요청 없음)

**Priority:** P0

---

### US-2: 피드백 생성 로딩 상태

**As a** 피드백이 생성되고 있는 사용자
**I want to** 로딩 중임을 명확하게 알 수 있기를 원한다
**So that** 서버가 처리 중임을 이해하고 인내심을 갖고 기다릴 수 있다

**Acceptance Criteria:**
- [ ] 페이지 진입 시 "AI 피드백 생성 중" 메시지가 표시된다
- [ ] AI 아바타(Character 컴포넌트)가 "thinking" 표정으로 표시된다
- [ ] 진행 바(progress bar)가 애니메이션되며 진행 상황을 표시한다
- [ ] 로딩 상태는 5~10초 정도 지속된다
- [ ] 로딩 중에는 다른 상호작용이 불가능하다 (버튼 비활성화)

**Priority:** P0

---

### US-3: 피드백 생성 완료

**As a** 피드백이 생성된 사용자
**I want to** 피드백이 완료되었음을 알고 다음 단계로 진행하고 싶다
**So that** 피드백 리뷰 또는 리포트를 볼 수 있다

**Acceptance Criteria:**
- [ ] 피드백 생성 완료 시 AI 아바타가 "happy" 표정으로 변경된다
- [ ] "피드백 생성 완료" 메시지와 생성된 피드백 개수가 표시된다
- [ ] "타임스탬프 피드백 리뷰" 버튼(primary)이 표시된다
- [ ] "종합 리포트 보기" 버튼(secondary)이 표시된다
- [ ] 두 버튼 모두 클릭하면 각각의 페이지로 이동한다

**Priority:** P0

---

### US-4: 피드백 생성 실패 처리

**As a** 피드백 생성이 실패한 사용자
**I want to** 명확한 에러 메시지를 받고 싶다
**So that** 무엇이 문제인지 이해하고 홈으로 돌아갈 수 있다

**Acceptance Criteria:**
- [ ] 피드백 생성 API 실패 시 AI 아바타가 "confused" 표정으로 변경된다
- [ ] "피드백 생성 실패" 메시지가 표시된다
- [ ] 에러 메시지에 "다시 시도해주세요"가 포함된다
- [ ] "홈으로 돌아가기" 버튼이 표시되어 홈(/경로로 이동한다
- [ ] 에러가 면접 데이터 손실을 야기하지 않는다

**Priority:** P0

---

## Scope

### In Scope

1. **Frontend Pages**
   - InterviewCompletePage 구현
   - useEffect를 통한 자동 피드백 요청
   - 3가지 상태(로딩/성공/실패) UI 구현

2. **Frontend Components**
   - Character 컴포넌트 (아바타 감정 표현)
   - Button 컴포넌트 (navigation)
   - 진행 바 (progress bar)

3. **Frontend Hooks**
   - useGenerateFeedback 활용
   - 상태 동기화

4. **Data Flow**
   - InterviewPage → InterviewCompletePage 네비게이션
   - 면접 상태 COMPLETED로 변경
   - 답변 데이터 수집 및 변환

### Out of Scope

| 항목 | 사유 |
|------|------|
| 피드백 생성 재시도 UI | P2. 현재는 홈으로 돌아가기만 제공 |
| 피드백 부분 생성 | 모든 답변이 완료되어야 생성 |
| 백그라운드 피드백 생성 | 면접 종료 후 페이지 진입 시에만 요청 |
| 피드백 생성 진행률 표시 | 전체 로딩 바만 표시 |

---

## Page Specification

### InterviewCompletePage

**Location:** `frontend/src/pages/interview-complete-page.tsx`

**Route:** `/interview/{id}/complete`

**Layout:**
```
┌──────────────────────────────┐
│  중앙 정렬 컨테이너 (max-w-md)  │
│                              │
│  [AI 아바타 (100px)]         │
│                              │
│  [제목 텍스트]               │
│  [부제목 텍스트]             │
│                              │
│  [로딩 바 또는 버튼들]       │
│                              │
└──────────────────────────────┘
```

**Responsive Design:**
- Mobile: `px-4 sm:px-6`
- 최대 폭: `max-w-md` (448px)
- 수직 정렬: 중앙 (flexbox)

---

### State 1: 피드백 생성 중 (isPending)

**UI Components:**
```typescript
{generateFeedback.isPending && (
  <>
    <Character mood="thinking" size={100} className="mx-auto" />
    <div className="space-y-2">
      <h1 className="text-2xl font-bold text-text-primary">AI 피드백 생성 중</h1>
      <p className="text-sm text-text-secondary">
        면접 답변을 분석하고 있습니다. 잠시만 기다려주세요.
      </p>
    </div>
    <div className="mx-auto h-1.5 w-48 overflow-hidden rounded-full bg-border">
      <div className="h-full animate-pulse rounded-full bg-accent" style={{ width: '60%' }} />
    </div>
  </>
)}
```

**Visual Details:**
- 아바타: `mood="thinking"`, `size={100}`
- 제목: `text-2xl font-bold text-text-primary`
- 부제목: `text-sm text-text-secondary`
- 진행 바: `h-1.5 w-48`, `animate-pulse`, 60% 채움

---

### State 2: 피드백 생성 완료 (isSuccess)

**UI Components:**
```typescript
{generateFeedback.isSuccess && (
  <>
    <Character mood="happy" size={100} className="mx-auto" />
    <div className="space-y-2">
      <h1 className="text-2xl font-bold text-text-primary">피드백 생성 완료</h1>
      <p className="text-sm text-text-secondary">
        {generateFeedback.data.data.totalCount}개의 피드백이 생성되었습니다.
      </p>
    </div>
    <div className="space-y-3">
      <Button
        variant="primary"
        fullWidth
        onClick={handleViewReview}
      >
        타임스탬프 피드백 리뷰
      </Button>
      <Button
        variant="secondary"
        fullWidth
        onClick={handleViewReport}
      >
        종합 리포트 보기
      </Button>
    </div>
  </>
)}
```

**Visual Details:**
- 아바타: `mood="happy"`, `size={100}`
- 피드백 개수 표시: `generateFeedback.data.data.totalCount`
- 버튼 1: `variant="primary"`, `fullWidth`
- 버튼 2: `variant="secondary"`, `fullWidth`
- 버튼 간격: `space-y-3`

**Navigation Handlers:**
```typescript
const handleViewReview = () => {
  navigate(`/interview/${id}/review`)
}

const handleViewReport = () => {
  navigate(`/interview/${id}/report`)
}
```

---

### State 3: 피드백 생성 실패 (isError)

**UI Components:**
```typescript
{generateFeedback.isError && (
  <>
    <Character mood="confused" size={100} className="mx-auto" />
    <div className="space-y-2">
      <h1 className="text-2xl font-bold text-text-primary">피드백 생성 실패</h1>
      <p className="text-sm text-text-secondary">
        피드백 생성 중 오류가 발생했습니다. 다시 시도해주세요.
      </p>
    </div>
    <Button
      variant="secondary"
      onClick={() => navigate('/')}
    >
      홈으로 돌아가기
    </Button>
  </>
)}
```

**Visual Details:**
- 아바타: `mood="confused"`, `size={100}`
- 제목: `text-2xl font-bold text-text-primary`
- 부제목: `text-sm text-text-secondary`
- 버튼: `variant="secondary"`, 홈으로 이동

---

## Implementation Details

### useEffect 피드백 요청 로직

**Location:** `frontend/src/pages/interview-complete-page.tsx`

```typescript
const { id } = useParams<{ id: string }>()
const navigate = useNavigate()
const interviewId = Number(id)

const { questions, answers } = useInterviewStore()
const generateFeedback = useGenerateFeedback()

useEffect(() => {
  // 1. 조건 검증
  if (!id || generateFeedback.isPending || generateFeedback.isSuccess || generateFeedback.isError) {
    return
  }
  if (answers.length === 0) {
    return
  }

  // 2. 답변 데이터 변환
  const answerDataList: AnswerData[] = answers.map((answer, index) => {
    const question = questions[index]
    const answerText = answer.transcripts
      .filter((t) => t.isFinal)
      .map((t) => t.text)
      .join(' ')

    const nonVerbalSummary = answer.nonVerbalEvents.length > 0
      ? answer.nonVerbalEvents.map((e) => `${e.type}: ${e.data.description}`).join(', ')
      : undefined

    const voiceSummary = answer.voiceEvents.length > 0
      ? answer.voiceEvents.map((e) => `${e.type}(${e.duration}ms)`).join(', ')
      : undefined

    return {
      questionIndex: index,
      questionContent: question?.content ?? '',
      answerText: answerText || '(답변 없음)',
      nonVerbalSummary,
      voiceSummary,
    }
  })

  // 3. 피드백 생성 요청
  generateFeedback.mutate({
    interviewId,
    data: { answers: answerDataList },
  })
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [id])
```

**Key Points:**
- `generateFeedback.isPending/isSuccess/isError` 체크로 중복 요청 방지
- `answers.length === 0` 체크로 데이터 부재 시 요청 방지
- STT 결과(transcripts)에서 최종 결과만 필터링
- 비언어/음성 분석이 없으면 `undefined`로 전달 (선택적)

---

## Data Transformation

### Answer → AnswerData 변환

```typescript
interface Answer {
  transcripts: TranscriptSegment[]
  nonVerbalEvents: NonVerbalEvent[]
  voiceEvents: VoiceEvent[]
}

interface AnswerData {
  questionIndex: number
  questionContent: string
  answerText: string
  nonVerbalSummary?: string
  voiceSummary?: string
}

// 변환 로직
const answerText = answer.transcripts
  .filter((t) => t.isFinal)  // 최종 인식 결과만
  .map((t) => t.text)
  .join(' ')  // 공백으로 연결

const nonVerbalSummary = answer.nonVerbalEvents.length > 0
  ? answer.nonVerbalEvents
      .map((e) => `${e.type}: ${e.data.description}`)
      .join(', ')
  : undefined

const voiceSummary = answer.voiceEvents.length > 0
  ? answer.voiceEvents
      .map((e) => `${e.type}(${e.duration}ms)`)
      .join(', ')
  : undefined
```

---

## Flow Diagram

```
[면접 진행 페이지]
    │
    │ 마지막 질문(5/5) 답변 완료
    ▼
[InterviewControls]
    │
    │ "면접 종료" 버튼 표시 (마지막 질문에서만)
    │ 클릭
    ▼
[handleFinishInterview 호출]
    │
    ├─ STT/오디오 분석 중지
    ├─ MediaRecorder 정지
    ├─ 비디오 Blob 저장
    ├─ completeInterview() - Zustand 상태 변경
    ├─ updateStatus.mutate({ status: 'COMPLETED' })
    ├─ mediaStream 중지
    └─ navigate('/interview/{id}/complete')
        │
        ▼
[InterviewCompletePage 진입]
    │
    │ useEffect 트리거
    │ (id가 있고, answers 있고, 상태 체크)
    ▼
[피드백 요청 시작]
    │
    ├─ answers → AnswerData[] 변환
    └─ generateFeedback.mutate({ interviewId, data })
        │
        ▼
[generateFeedback.isPending = true]
    │
    └─ UI: "AI 피드백 생성 중" + 아바타(thinking)
        │
        ▼ (API 응답 5~10초 후)
        │
        ├─ 성공 (201 Created):
        │   │
        │   ├─ generateFeedback.isSuccess = true
        │   ├─ generateFeedback.data = FeedbackListResponse
        │   │
        │   └─ UI: "피드백 생성 완료" + 아바타(happy)
        │       + "타임스탐프 피드백 리뷰" 버튼
        │       + "종합 리포트 보기" 버튼
        │
        └─ 실패:
            │
            ├─ generateFeedback.isError = true
            │
            └─ UI: "피드백 생성 실패" + 아바타(confused)
                + "홈으로 돌아가기" 버튼
```

---

## Button Navigation

### 타임스탬프 피드백 리뷰 버튼
- 텍스트: "타임스탬프 피드백 리뷰"
- 변형: `variant="primary"`
- 네비게이션: `/interview/{id}/review`
- 목적: 비디오 플레이어와 타임스탬프 피드백 동기화

### 종합 리포트 보기 버튼
- 텍스트: "종합 리포트 보기"
- 변형: `variant="secondary"`
- 네비게이션: `/interview/{id}/report`
- 목적: 종합 분석 리포트 표시

### 홈으로 돌아가기 버튼 (에러 시)
- 텍스트: "홈으로 돌아가기"
- 변형: `variant="secondary"`
- 네비게이션: `/`
- 목적: 에러 상태에서 사용자를 안전한 상태로 복구

---

## Error Handling

### API 에러 케이스

1. **400 Bad Request**: 답변 데이터 형식 오류
2. **404 Not Found**: 면접이 존재하지 않음
3. **409 Conflict**: 면접 상태가 COMPLETED가 아님
4. **502 Bad Gateway**: Claude API 호출 실패
5. **504 Gateway Timeout**: Claude API 타임아웃

모든 경우 `isError` 상태로 전환되고, 사용자에게 일반적인 에러 메시지를 표시한다.

---

## UX Considerations

1. **자동 요청**: 사용자가 버튼을 누를 필요 없음 (더 자연스러운 흐름)
2. **아바타 감정**: 상태를 시각적으로 표현 (thinking → happy/confused)
3. **진행 바**: 로딩 중 진행 상황을 시각적으로 피드백
4. **명확한 메시지**: 다음 단계가 무엇인지 분명함
5. **에러 복구**: 에러 시 홈으로 돌아갈 수 있는 경로 제공

---

## Browser Compatibility

- 최신 Chrome/Firefox/Safari 지원
- 비동기 API 및 Promise 지원 필수
- Tailwind CSS 기본 클래스만 사용

---

## Success Criteria

- [x] InterviewCompletePage 구현
- [x] useEffect를 통한 자동 피드백 요청
- [x] 중복 요청 방지 (isPending/isSuccess/isError 체크)
- [x] 답변 데이터 수집 및 변환
- [x] 3가지 상태(로딩/성공/실패) UI 구현
- [x] Character 컴포넌트를 활용한 아바타 표정
- [x] 진행 바 애니메이션
- [x] 네비게이션 버튼 구현
- [x] 에러 처리 및 홈으로 돌아가기
- [x] 반응형 디자인
- [x] 접근성 고려 (명확한 텍스트, 색상 외 표시)
