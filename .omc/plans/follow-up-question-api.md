# Feature Specification: 후속 질문 생성 API

> **문서 ID**: PLAN-003
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P0 (Must-have)
> **의존성**: PLAN-001 (면접 세션 생성), PLAN-002 (면접 진행) 완료

---

## Overview

### 문제 정의

면접이 진행 중일 때 사용자의 답변을 분석하여 더 깊이 있는 후속 질문을 실시간으로 생성해야 한다. 단순한 초기 질문뿐 아니라 답변에 따라 동적으로 면접을 진행함으로써 실제 면접의 흐름과 유사한 경험을 제공한다.

### 솔루션 요약

사용자가 질문에 대한 답변을 완료하면 프론트엔드가 백엔드에 다음 정보를 전송한다:
- 원본 질문 내용
- 사용자 답변 (STT 변환된 텍스트)
- 비언어 분석 요약 (선택사항)

백엔드의 Claude API 호출을 통해 4가지 유형의 후속 질문(심화/명확화/반론/적용) 중 하나를 생성하고, 생성 이유를 함께 반환한다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 면접의 현실감을 높이는 핵심 기능 |
| **Effort** | Low - Claude API 호출 + 간단한 DTO 매핑 |
| **결론** | **P0** - High Impact, Low Effort. 즉시 개발 |

---

## Target Users

- **주니어 개발자**: 동적 질문을 통해 실제 면접에 더 가까운 경험 습득
- **경력 개발자**: 시스템 설계 면접의 심화 질문 대비
- **면접 준비생 전체**: 자신의 답변을 기반한 맞춤형 피드백 체험

---

## User Stories

### US-1: 후속 질문 요청

**As a** 면접 진행 중인 사용자
**I want to** 내 답변을 바탕으로 후속 질문이 생성되길 원한다
**So that** 면접관의 추가 질문에 대비하며 더 깊이 있는 답변을 연습할 수 있다

**Acceptance Criteria:**
- [ ] 답변 완료 시 자동으로 후속 질문 생성 요청이 전송된다
- [ ] 로딩 중 사용자에게 "후속 질문을 생성하고 있습니다" 메시지가 표시된다
- [ ] 후속 질문이 3초 이내에 생성된다 (Claude API 응답 기준)
- [ ] 후속 질문 생성 실패 시 사용자에게 에러 메시지가 표시되고 다음 질문으로 진행 가능하다
- [ ] 후속 질문이 없으면 다음 질문 버튼으로 바로 진행할 수 있다

**Priority:** P0
**Notes:**
- 후속 질문은 선택사항이므로 생성 실패가 면접 진행을 방해하지 않는다
- 비언어 분석 데이터(nonVerbalSummary)는 선택적이다

---

### US-2: 후속 질문 표시

**As a** 후속 질문이 생성된 면접자
**I want to** 후속 질문이 화면에 명확하게 표시되길 원한다
**So that** 추가 질문의 의도를 파악하고 신중하게 답변할 수 있다

**Acceptance Criteria:**
- [ ] 후속 질문 타입(심화/명확화/반론/적용)이 배지로 표시된다
- [ ] 후속 질문 내용이 주 질문 아래에 카드 형태로 표시된다
- [ ] 후속 질문이 생성된 이유가 작은 텍스트로 표시된다
- [ ] 후속 질문은 다음 질문과는 별개로, 현재 질문에 대한 추가 질문임이 명확하다

**Priority:** P0

---

## Scope

### In Scope

1. **Backend**
   - 후속 질문 생성 엔드포인트 (`POST /api/v1/interviews/{id}/follow-up`)
   - Claude API와의 연동 서비스 확장
   - 후속 질문 생성 로직 (4가지 타입 분류)
   - 요청/응답 DTO + Validation

2. **Frontend**
   - 후속 질문 생성 뮤테이션 훅 (`useFollowUpQuestion`)
   - QuestionDisplay 컴포넌트 확장 (후속 질문 표시)
   - InterviewPage에서 자동 요청 로직
   - 로딩/에러 상태 UI

3. **UX**
   - 후속 질문 로딩 인디케이터
   - 후속 질문 표시 카드
   - 실패 시 재시도 없이 다음 진행 가능

### Out of Scope

| 항목 | 사유 |
|------|------|
| 사용자 선호 후속 질문 타입 설정 | P2. MVP에서는 AI가 최적의 타입을 선택 |
| 후속 질문 답변 저장 | P2. MVP에서는 원본 질문 답변만 저장 |
| 후속 질문 히스토리 조회 | P2. 백로그 |
| 후속 질문 수동 스킵 | 다음 질문으로 진행 시 자동 스킵 |

---

## Backend API Specification

### API-1: 후속 질문 생성

```
POST /api/v1/interviews/{id}/follow-up
```

**Path Parameters:**
- `id` (Long): 면접 세션 ID

**Request Body:**
```json
{
  "questionContent": "HashMap과 TreeMap의 차이점을 설명해주세요.",
  "answerText": "HashMap은 해시 함수를 사용해서 O(1)에 접근하고, TreeMap은 Red-Black 트리를 사용해서 O(log n)에 접근합니다.",
  "nonVerbalSummary": "eye_contact_lost: 시선이 3초간 이탈됨, slouch_detected: 2회 감지"
}
```

**Validation:**
- `questionContent`: NotBlank, 최대 1000자
- `answerText`: NotBlank, 최대 5000자
- `nonVerbalSummary`: Optional

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "question": "좋은 설명이네요. 그럼 HashMap의 충돌(collision) 해결 방식을 설명해주시겠어요?",
    "reason": "답변에서 기본 개념은 맞지만, 실제 구현의 세부사항(충돌 처리)을 더 깊이 있게 파악하기 위해 심화 질문을 제시합니다.",
    "type": "DEEP_DIVE"
  },
  "message": null
}
```

**Response Fields:**
- `question` (String): 생성된 후속 질문
- `reason` (String): 이 질문을 생성한 이유
- `type` (String): 후속 질문 타입 (DEEP_DIVE, CLARIFICATION, CHALLENGE, APPLICATION)

**Error Cases:**
- 400: Validation 실패 (빈 questionContent, 빈 answerText)
- 409: 진행 중(IN_PROGRESS)이 아닌 면접에서 요청
- 502: Claude API 호출 실패
- 504: Claude API 타임아웃 (30초 초과)

---

## Implementation Details

### 후속 질문 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| **DEEP_DIVE** | 답변 내용의 깊이를 더하는 질문 | "좋은 답변이네요. 그럼 XXX의 세부 구현은 어떻게 되나요?" |
| **CLARIFICATION** | 모호한 부분을 명확히 하는 질문 | "XXX가 정확히 무엇을 의미하나요?" |
| **CHALLENGE** | 답변의 주장에 반박하는 질문 | "그런데 YYY의 경우는 어떻게 되나요?" |
| **APPLICATION** | 실무에 적용하는 시나리오 질문 | "실무에서는 이를 어떻게 활용할 수 있을까요?" |

### Claude API Prompt Design

```
System Prompt:
- 역할: 한국 IT 기업의 시니어 개발자 면접관
- 원본 질문과 사용자 답변을 분석하여 후속 질문 생성
- 후속 질문 타입 선택: DEEP_DIVE (가장 빈번) > CHALLENGE > CLARIFICATION > APPLICATION
- 출력 형식: JSON

User Prompt:
- 원본 질문: {questionContent}
- 사용자 답변: {answerText}
- 비언어 분석: {nonVerbalSummary} (선택사항)
- 요청: 위 답변에 대한 후속 질문 1개 생성 (type, question, reason 포함)
```

**출력 형식 (Claude에게 요청할 JSON):**
```json
{
  "question": "후속 질문 내용",
  "reason": "이 질문을 선택한 이유",
  "type": "DEEP_DIVE"
}
```

---

## Data Model

### 새로운 엔티티 없음

후속 질문은 DB에 저장하지 않고, 현재 세션의 Zustand 스토어에만 저장된다.
(향후 후속 질문 히스토리 기능 추가 시 FollowUpQuestion 엔티티 생성 가능)

---

## Frontend Components & Hooks

### useFollowUpQuestion Hook

```typescript
export const useFollowUpQuestion = () => {
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number
      data: FollowUpRequest
    }) =>
      apiClient.post<ApiResponse<FollowUpResponse>>(
        `/api/v1/interviews/${id}/follow-up`,
        data,
      ),
  })
}
```

**사용 위치:** `interview-page.tsx`의 `handleStopAnswer` 함수

### QuestionDisplay Component 확장

```typescript
interface QuestionDisplayProps {
  question: Question
  currentIndex: number
  totalCount: number
  followUp?: FollowUpResponse       // NEW
  isFollowUpLoading?: boolean       // NEW
}
```

**기능:**
- `isFollowUpLoading` 표시 중일 때 로딩 스피너 표시
- `followUp` 데이터가 있을 때 배지(타입) + 질문 + 이유 표시

### Zustand Store 확장

```typescript
interface InterviewStore {
  // 기존 필드...
  followUpQuestions: Map<number, FollowUpResponse>  // NEW
  isFollowUpLoading: boolean                         // NEW

  addFollowUpQuestion: (questionIndex: number, followUp: FollowUpResponse) => void
  setFollowUpLoading: (loading: boolean) => void
}
```

---

## Flow Diagram

```
[답변 완료]
    │
    │ handleStopAnswer() 호출
    ▼
[후속 질문 요청 시작]
    │
    │ followUpMutation.mutate({
    │   id: interview.id,
    │   data: { questionContent, answerText, nonVerbalSummary }
    │ })
    ▼
[로딩 중: isFollowUpLoading = true]
    │
    │ QuestionDisplay에서 로딩 상태 표시
    ▼
[API 응답]
    │
    ├─ 성공: followUpQuestions에 저장, 화면 갱신
    │
    └─ 실패: 에러 메시지 표시, 다음 질문으로 진행 가능
```

---

## Technical Constraints

1. **후속 질문은 선택사항**: 생성 실패가 면접 진행을 막지 않음
2. **비언어 분석은 선택적**: nonVerbalSummary 없이도 후속 질문 생성 가능
3. **Claude API 타임아웃**: 30초 (Spring WebClient 사용)
4. **메모리 효율성**: 후속 질문은 DB 저장 없이 클라이언트 스토어에서만 관리
5. **응답 래퍼**: 기존 ApiResponse<T> 패턴 사용

---

## Dependencies

| 의존성 | 타입 | 상태 | 설명 |
|--------|------|------|------|
| Claude API | 외부 서비스 | 필요 | 후속 질문 생성 |
| InterviewService | 내부 | 완료 | 기존 서비스 활용 |
| AiClient | 내부 | 확장 필요 | generateFollowUpQuestion 메서드 추가 |
| TanStack Query | 프론트엔드 | 완료 | useMutation 활용 |
| Zustand | 프론트엔드 | 완료 | 후속 질문 상태 저장 |

---

## Integration Points

1. **InterviewPage**: 답변 완료 시 후속 질문 자동 요청
2. **QuestionDisplay**: 후속 질문 표시 및 로딩 상태 UI
3. **Zustand Store**: 후속 질문 데이터 관리
4. **InterviewService**: 진행 상태 검증

---

## Success Criteria

- [x] 후속 질문 API 엔드포인트 구현 및 동작
- [x] Claude API를 통한 질문 생성 로직 구현
- [x] 4가지 타입(DEEP_DIVE, CLARIFICATION, CHALLENGE, APPLICATION) 분류
- [x] 프론트엔드에서 자동 요청 및 표시
- [x] 로딩/에러 상태 UI 구현
- [x] 질문별로 후속 질문 저장 (Map 구조)
- [x] 생성 실패 시에도 면접 진행 가능
