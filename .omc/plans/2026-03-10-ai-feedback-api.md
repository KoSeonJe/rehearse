# Feature Specification: AI 피드백 생성 API

> **문서 ID**: PLAN-004
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P0 (Must-have)
> **의존성**: PLAN-001 (면접 세션 생성), PLAN-002 (면접 진행) 완료

---

## Overview

### 문제 정의

면접이 완료되면 사용자의 모든 답변(STT, 비언어 분석, 음성 분석)을 수집하여 Claude API를 통해 종합적인 AI 피드백을 생성해야 한다. 각 피드백은 타임스탬프를 포함하여 이후 비디오 플레이어에서 정확한 시점에 동기화되어 표시된다.

### 솔루션 요약

면접이 COMPLETED 상태로 변경되면 프론트엔드가 모든 답변 데이터를 백엔드로 전송한다:
- 각 질문의 STT 변환 텍스트
- 비언어 분석 요약 (시선, 표정, 자세)
- 음성 분석 요약 (음량, 속도, 침묵)

백엔드가 이 정보를 Claude API로 분석하여 다음을 생성한다:
- 피드백 항목 (언어 피드백, 비언어 피드백)
- 각 피드백별 타임스탬프
- 심각도 수준 (CRITICAL, WARNING, MINOR)
- 구체적인 개선 제안

모든 피드백은 DB에 저장되고, 이후 타임스탬프 피드백 리뷰 페이지에서 재생되는 비디오와 동기화된다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | Critical - DevLens의 핵심 가치인 "타임스탬프 피드백" 제공 |
| **Effort** | Medium - Claude API 호출 + 다수 DTO 매핑 |
| **결론** | **P0** - Critical Impact. 즉시 개발 |

---

## Target Users

- **모든 면접 사용자**: 종합적인 AI 피드백을 통해 약점 파악
- **비언어 피드백 필요 사용자**: 시선, 표정, 자세에 대한 실시간 분석
- **음성 피드백 필요 사용자**: 말하기 속도, 음량, 침묵에 대한 분석

---

## User Stories

### US-1: 피드백 요청

**As a** 면접을 완료한 사용자
**I want to** 내 면접에 대한 AI 피드백을 자동으로 생성받고 싶다
**So that** 면접 과정 전체를 분석하고 개선점을 파악할 수 있다

**Acceptance Criteria:**
- [ ] 면접 상태가 COMPLETED로 변경될 때 피드백 생성 자동 요청
- [ ] 모든 답변 데이터(STT, 비언어, 음성)가 함께 전송된다
- [ ] 로딩 상태 UI가 표시되며 "AI 피드백 생성 중" 메시지 표시
- [ ] 피드백 생성이 평균 5~10초 내 완료된다 (Claude API 응답 기준)
- [ ] 생성된 피드백 개수가 최소 3개 이상이다 (과다 피드백 최대 20개)
- [ ] 피드백 생성 실패 시 재시도 버튼이 제공된다

**Priority:** P0

---

### US-2: 피드백 조회

**As a** 이전 면접의 피드백을 보고 싶은 사용자
**I want to** 이미 생성된 피드백을 조회할 수 있길 원한다
**So that** 언제든지 다시 피드백을 확인할 수 있다

**Acceptance Criteria:**
- [ ] GET /api/v1/interviews/{id}/feedbacks로 피드백 조회 가능
- [ ] 응답에 모든 피드백이 타임스탬프 순서대로 정렬되어 포함된다
- [ ] 각 피드백에는 카테고리, 심각도, 내용, 제안이 포함된다
- [ ] 피드백이 없으면 빈 배열이 반환된다 (500 에러 아님)

**Priority:** P0

---

## Scope

### In Scope

1. **Backend**
   - 피드백 생성 엔드포인트 (`POST /api/v1/interviews/{id}/feedbacks`)
   - 피드백 조회 엔드포인트 (`GET /api/v1/interviews/{id}/feedbacks`)
   - Claude API를 통한 피드백 생성 로직
   - 답변 데이터 저장 (InterviewAnswer 엔티티)
   - 피드백 데이터 저장 (Feedback 엔티티)
   - 요청/응답 DTO + Validation

2. **Frontend**
   - 피드백 생성 뮤테이션 훅 (`useGenerateFeedback`)
   - 피드백 조회 쿼리 훅 (`useFeedbacks`)
   - InterviewCompletePage에서 자동 요청 및 결과 표시
   - 로딩/성공/에러 상태 UI

3. **Database**
   - InterviewAnswer 테이블 (각 질문의 답변 저장)
   - Feedback 테이블 (타임스탬프 피드백)

### Out of Scope

| 항목 | 사유 |
|------|------|
| 실시간 피드백 (면접 중 생성) | 면접 종료 후 생성. 면접 중 실시간은 P2 |
| 사용자 피드백 튜닝 (좋아요/싫어요) | P2. 반복학습 데이터 수집 |
| 피드백 기반 개선 과제 추천 | P2. MVP에서는 피드백만 제공 |
| 피드백 내보내기 (PDF/이미지) | P2. 백로그 |

---

## Backend API Specification

### API-1: 피드백 생성

```
POST /api/v1/interviews/{id}/feedbacks
```

**Path Parameters:**
- `id` (Long): 면접 세션 ID

**Request Body:**
```json
{
  "answers": [
    {
      "questionIndex": 0,
      "questionContent": "HashMap과 TreeMap의 차이점을 설명해주세요.",
      "answerText": "HashMap은 O(1) 접근, TreeMap은 O(log n) 접근합니다.",
      "nonVerbalSummary": "eye_contact_lost: 2회, slouch: 1회",
      "voiceSummary": "speaking_rate_fast: 5회(0.5초씩), silence: 3회(0.3초씩)"
    },
    {
      "questionIndex": 1,
      "questionContent": "GC가 무엇인가요?",
      "answerText": "Garbage Collection은...",
      "nonVerbalSummary": null,
      "voiceSummary": "normal"
    }
  ]
}
```

**Validation:**
- `answers`: NotEmpty (최소 1개), 최대 10개
- 각 AnswerData:
  - `questionIndex`: NotNull, 0 이상
  - `questionContent`: NotBlank
  - `answerText`: NotBlank
  - `nonVerbalSummary`: Optional
  - `voiceSummary`: Optional

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "interviewId": 1,
    "feedbacks": [
      {
        "id": 101,
        "timestampSeconds": 15.5,
        "category": "LINGUISTIC",
        "severity": "WARNING",
        "content": "답변의 구조를 먼저 설명하고 세부사항을 덧붙이는 방식으로 개선할 수 있습니다.",
        "suggestion": "먼저 '크게 2가지로 나누어 설명하겠습니다'라는 도입문을 추가하세요."
      },
      {
        "id": 102,
        "timestampSeconds": 42.3,
        "category": "NON_VERBAL",
        "severity": "MINOR",
        "content": "답변 중간에 시선이 이탈되었습니다.",
        "suggestion": "면접관이나 카메라와 시선을 맞추며 대답하세요."
      },
      {
        "id": 103,
        "timestampSeconds": 60.0,
        "category": "VOICE",
        "severity": "CRITICAL",
        "content": "말씀하시는 속도가 너무 빨라서 이해하기 어렵습니다.",
        "suggestion": "1초에 3~4 단어 수준으로 천천히 말씀해주세요."
      }
    ],
    "totalCount": 3
  },
  "message": null
}
```

**Response Fields:**
- `feedbacks[].id`: 피드백 고유 ID
- `feedbacks[].timestampSeconds`: 비디오 재생 중 동기화될 시점 (초)
- `feedbacks[].category`: LINGUISTIC, NON_VERBAL, VOICE, BEHAVIORAL
- `feedbacks[].severity`: CRITICAL, WARNING, MINOR
- `feedbacks[].content`: 피드백 내용
- `feedbacks[].suggestion`: 개선 제안
- `totalCount`: 생성된 피드백 개수

**Preconditions:**
- 면접 상태가 COMPLETED여야 함 (409: 그 외 상태)
- 인터뷰가 존재해야 함 (404: 없을 때)

**Error Cases:**
- 400: Validation 실패 (빈 answers, 빈 answerText 등)
- 404: 존재하지 않는 면접 ID
- 409: 면접 상태가 COMPLETED가 아님
- 502: Claude API 호출 실패
- 504: Claude API 타임아웃 (30초 초과)

---

### API-2: 피드백 조회

```
GET /api/v1/interviews/{id}/feedbacks
```

**Path Parameters:**
- `id` (Long): 면접 세션 ID

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "interviewId": 1,
    "feedbacks": [
      {
        "id": 101,
        "timestampSeconds": 15.5,
        "category": "LINGUISTIC",
        "severity": "WARNING",
        "content": "답변의 구조를...",
        "suggestion": "먼저 '크게 2가지로...'라는 도입문을 추가하세요."
      }
    ],
    "totalCount": 3
  },
  "message": null
}
```

**Error Cases:**
- 404: 존재하지 않는 면접 ID
- (피드백이 없어도 빈 배열 반환 - 에러 아님)

---

## Data Model

### InterviewAnswer 엔티티

각 질문에 대한 사용자 답변을 저장한다. (각 질문별 1개 레코드)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long (PK) | AUTO_INCREMENT | 답변 식별자 |
| interview_id | Long (FK) | NOT NULL | 세션 참조 |
| question_index | INT | NOT NULL | 질문 순서 (0부터 시작) |
| question_content | TEXT | NOT NULL | 질문 내용 |
| answer_text | TEXT | NOT NULL | STT 변환된 답변 텍스트 |
| non_verbal_summary | TEXT | NULL | 비언어 분석 요약 |
| voice_summary | TEXT | NULL | 음성 분석 요약 |
| created_at | DATETIME | NOT NULL | 생성일시 |

**관계:** Interview 1:N InterviewAnswer (cascade delete)

### Feedback 엔티티

AI가 생성한 개별 피드백을 저장한다. (면접당 평균 5~10개)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long (PK) | AUTO_INCREMENT | 피드백 식별자 |
| interview_id | Long (FK) | NOT NULL | 세션 참조 |
| timestamp_seconds | DOUBLE | NOT NULL | 비디오 재생 시점 (초) |
| category | ENUM | NOT NULL | LINGUISTIC, NON_VERBAL, VOICE, BEHAVIORAL |
| severity | ENUM | NOT NULL | CRITICAL, WARNING, MINOR |
| content | TEXT | NOT NULL | 피드백 내용 |
| suggestion | TEXT | NULL | 개선 제안 |
| created_at | DATETIME | NOT NULL | 생성일시 |

**관계:** Interview 1:N Feedback (cascade delete)

### Enum: FeedbackCategory

```java
enum FeedbackCategory {
  LINGUISTIC,    // 언어 표현, 문법, 설명 구조
  NON_VERBAL,    // 시선, 표정, 자세, 제스처
  VOICE,         // 음량, 속도, 음정, 침묵
  BEHAVIORAL     // 신뢰성, 태도, 참여도
}
```

### Enum: FeedbackSeverity

```java
enum FeedbackSeverity {
  CRITICAL,   // 즉시 개선 필요
  WARNING,    // 중요한 개선점
  MINOR       // 참고 수준
}
```

---

## Frontend Components & Hooks

### useGenerateFeedback Hook

```typescript
export const useGenerateFeedback = () => {
  return useMutation({
    mutationFn: ({
      interviewId,
      data,
    }: {
      interviewId: number
      data: GenerateFeedbackRequest
    }) =>
      apiClient.post<ApiResponse<FeedbackListResponse>>(
        `/api/v1/interviews/${interviewId}/feedbacks`,
        data,
      ),
  })
}
```

**사용 위치:** `interview-complete-page.tsx`에서 자동 호출

### useFeedbacks Hook

```typescript
export const useFeedbacks = (interviewId: string) => {
  return useQuery({
    queryKey: ['feedbacks', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<FeedbackListResponse>>(
        `/api/v1/interviews/${interviewId}/feedbacks`,
      ),
    enabled: !!interviewId,
  })
}
```

**사용 위치:** `interview-review-page.tsx`에서 피드백 로드

### InterviewCompletePage Flow

```typescript
useEffect(() => {
  if (!id || generateFeedback.isPending || generateFeedback.isSuccess) return
  if (answers.length === 0) return

  // 1. 모든 답변을 AnswerData 배열로 변환
  const answerDataList: AnswerData[] = answers.map((answer, index) => ({
    questionIndex: index,
    questionContent: questions[index]?.content,
    answerText: answer.transcripts.filter(t => t.isFinal).map(t => t.text).join(' '),
    nonVerbalSummary: answer.nonVerbalEvents.map(e => `${e.type}: ${e.data.description}`).join(', '),
    voiceSummary: answer.voiceEvents.map(e => `${e.type}(${e.duration}ms)`).join(', '),
  }))

  // 2. 피드백 생성 요청
  generateFeedback.mutate({
    interviewId,
    data: { answers: answerDataList },
  })
}, [id])
```

### InterviewCompletePage UI States

1. **isPending**: "AI 피드백 생성 중" + 로딩 스피너
2. **isSuccess**: "피드백 생성 완료" + 타임스탬프 리뷰/종합 리포트 버튼
3. **isError**: "피드백 생성 실패" + 홈으로 돌아가기 버튼

---

## Claude API Prompt Design

### System Prompt

```
역할: 개발자 면접 전문 평가자
목표: 사용자의 면접 성과를 객관적으로 분석하고 구체적인 개선안 제시

평가 기준:
- 언어 (LINGUISTIC): 설명의 명확성, 논리적 구조, 정확한 용어 사용
- 비언어 (NON_VERBAL): 시선 접촉, 자세, 제스처, 얼굴 표정
- 음성 (VOICE): 말하기 속도, 음량, 음정 변화, 침묵 관리
- 행동 (BEHAVIORAL): 신뢰도, 자신감, 질문 이해도

출력 형식: JSON 배열 (질문별 또는 전체 평가)
```

### User Prompt

```
다음은 개발자 면접 기록입니다.

{
  "answers": [
    {
      "questionIndex": 0,
      "questionContent": "질문 내용",
      "answerText": "사용자 답변",
      "nonVerbalSummary": "비언어 분석 데이터",
      "voiceSummary": "음성 분석 데이터"
    }
  ]
}

위 면접을 분석하여 다음 형식의 JSON 피드백 배열을 생성하세요:
- 피드백 당 1개 항목
- 각 항목은 timestampSeconds(비디오 시점), category, severity, content, suggestion 포함
- 최소 3개, 최대 20개 피드백
- timestampSeconds는 질문 순서와 예상 답변 시간을 고려한 현실적인 값

출력 형식:
{
  "feedbacks": [
    {
      "timestampSeconds": 15.5,
      "category": "LINGUISTIC",
      "severity": "WARNING",
      "content": "피드백 내용",
      "suggestion": "개선 제안"
    }
  ]
}
```

---

## Flow Diagram

```
[면접 완료: 상태 COMPLETED로 변경]
    │
    ▼
[InterviewCompletePage 진입]
    │
    │ useEffect에서 모든 답변 데이터 수집
    │ (STT, 비언어, 음성)
    ▼
[피드백 생성 요청]
    │ generateFeedback.mutate({
    │   interviewId,
    │   data: { answers: AnswerData[] }
    │ })
    ▼
[로딩 중: isPending = true]
    │ "AI 피드백 생성 중" UI 표시
    ▼
[API 응답]
    │
    ├─ 성공 (201):
    │   └─ feedbacks 저장, 캐시 무효화
    │   └─ isSuccess = true
    │   └─ "피드백 생성 완료" UI
    │   └─ 타임스탬프 리뷰/종합 리포트 네비게이션
    │
    └─ 실패:
        └─ isError = true
        └─ 에러 메시지 표시
        └─ 홈으로 돌아가기 버튼
```

---

## Technical Constraints

1. **피드백은 면접 완료 후에만 생성**: 상태 검증 필수
2. **Claude API 타임아웃**: 30초
3. **응답 정규화**: JSON 파싱 실패 시 500 에러 (구체적 에러 메시지 포함)
4. **타임스탬프 정확성**: 비디오 플레이어와 동기화를 위해 현실적인 값 필요
5. **메모리 효율성**: 피드백은 DB에 영구 저장

---

## Dependencies

| 의존성 | 타입 | 상태 | 설명 |
|--------|------|------|------|
| Claude API | 외부 서비스 | 필요 | 피드백 생성 |
| InterviewRepository | 내부 | 완료 | 면접 조회 |
| AiClient | 내부 | 확장 필요 | generateFeedback 메서드 추가 |
| FeedbackRepository | 내부 | 생성 필요 | Feedback 엔티티 저장/조회 |
| InterviewAnswerRepository | 내부 | 생성 필요 | InterviewAnswer 엔티티 저장 |
| ObjectMapper | 내부 | 필요 | 답변 JSON 직렬화 |
| TanStack Query | 프론트엔드 | 완료 | useMutation/useQuery 활용 |

---

## Integration Points

1. **InterviewService**: 상태 검증
2. **InterviewPage**: 완료 상태 변경 시 CompletePage로 네비게이션
3. **InterviewCompletePage**: 피드백 생성 요청 및 UI 상태 관리
4. **InterviewReviewPage**: 피드백 조회 및 타임스탬프 동기화

---

## Success Criteria

- [x] 피드백 생성 API 구현 및 정상 동작
- [x] InterviewAnswer 엔티티 생성 및 저장
- [x] Feedback 엔티티 생성 및 저장
- [x] Claude API를 통한 피드백 생성
- [x] 4가지 카테고리(LINGUISTIC, NON_VERBAL, VOICE, BEHAVIORAL) 분류
- [x] 3가지 심각도(CRITICAL, WARNING, MINOR) 적용
- [x] 현실적인 타임스탬프 계산
- [x] 프론트엔드에서 자동 요청 및 결과 표시
- [x] 로딩/성공/에러 상태 UI 구현
- [x] 피드백 조회 API 구현
- [x] 면접 COMPLETED 상태 검증
