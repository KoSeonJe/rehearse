# Rehearse Backend API 명세서

> 최종 업데이트: 2026-03-16

**관련 문서**: [ERD (Entity Relationship Diagram)](erd.md)

## Base URL

```
/api/v1
```

---

## 공통 응답 형식

### 성공 응답 — `ApiResponse<T>`

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 항상 `true` |
| `data` | `T` | 응답 데이터 |
| `message` | `String?` | 선택적 메시지 |

### 에러 응답 — `ErrorResponse`

```json
{
  "success": false,
  "status": 400,
  "code": "INTERVIEW_001",
  "message": "면접 세션을 찾을 수 없습니다.",
  "errors": null,
  "timestamp": "2026-03-16T14:30:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 항상 `false` |
| `status` | `int` | HTTP 상태 코드 |
| `code` | `String` | 도메인별 에러 코드 |
| `message` | `String` | 에러 메시지 |
| `errors` | `List<FieldError>?` | 유효성 검증 실패 시 필드별 오류 |
| `timestamp` | `LocalDateTime` | 에러 발생 시각 |

#### FieldError (유효성 검증 오류)

```json
{
  "field": "position",
  "value": "",
  "reason": "must not be null"
}
```

---

## Interview API

### 1. 면접 세션 생성

```
POST /api/v1/interviews
Content-Type: multipart/form-data
```

**Request Parts**

| Part | 타입 | 필수 | 설명 |
|------|------|------|------|
| `request` | `CreateInterviewRequest` (JSON) | O | 면접 설정 정보 |
| `resumeFile` | `MultipartFile` | X | 이력서 파일 (PDF) |

**CreateInterviewRequest**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `position` | `Position` | O | `@NotNull` | 포지션 |
| `positionDetail` | `String` | X | `@Size(max=100)` | 포지션 상세 |
| `level` | `InterviewLevel` | O | `@NotNull` | 경력 레벨 |
| `interviewTypes` | `List<InterviewType>` | O | `@NotEmpty` | 면접 유형 목록 |
| `csSubTopics` | `List<String>` | X | — | CS 세부 주제 |
| `durationMinutes` | `Integer` | O | `@NotNull @Min(5) @Max(120)` | 면접 시간(분) |

**Response** — `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "position": "BACKEND",
    "positionDetail": "Spring Boot 기반 서버 개발",
    "level": "JUNIOR",
    "interviewTypes": ["CS_FUNDAMENTAL", "JAVA_SPRING"],
    "csSubTopics": ["운영체제", "네트워크"],
    "status": "READY",
    "durationMinutes": 30,
    "questions": [
      {
        "id": 1,
        "content": "Spring IoC 컨테이너에 대해 설명해주세요.",
        "category": "JAVA_SPRING",
        "order": 1
      }
    ],
    "createdAt": "2026-03-16T14:30:00"
  }
}
```

---

### 2. 면접 상세 조회

```
GET /api/v1/interviews/{id}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 면접 세션 ID |

**Response** — `200 OK`

`InterviewResponse` (위 생성 응답과 동일 구조)

---

### 3. 면접 상태 변경

```
PATCH /api/v1/interviews/{id}/status
Content-Type: application/json
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 면접 세션 ID |

**Request Body — `UpdateStatusRequest`**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `status` | `InterviewStatus` | O | `@NotNull` | 변경할 상태 |

**상태 전이 규칙**

```
READY → IN_PROGRESS → COMPLETED
```

- `READY` → `IN_PROGRESS`만 허용
- `IN_PROGRESS` → `COMPLETED`만 허용
- `COMPLETED` → 전이 불가

**Response** — `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "IN_PROGRESS"
  }
}
```

---

### 4. 후속 질문 생성

```
POST /api/v1/interviews/{id}/follow-up
Content-Type: application/json
```

> 면접 상태가 `IN_PROGRESS`일 때만 호출 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 면접 세션 ID |

**Request Body — `FollowUpRequest`**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `questionContent` | `String` | O | `@NotBlank` | 현재 질문 내용 |
| `answerText` | `String` | O | `@NotBlank` | 사용자 답변 |
| `nonVerbalSummary` | `String` | X | — | 비언어 분석 요약 |
| `previousExchanges` | `List<FollowUpExchange>` | X | — | 이전 대화 기록 |

**FollowUpExchange**

| 필드 | 타입 | 설명 |
|------|------|------|
| `question` | `String` | 이전 질문 |
| `answer` | `String` | 이전 답변 |

**Response** — `200 OK`

```json
{
  "success": true,
  "data": {
    "question": "그렇다면 DI를 사용했을 때의 테스트 용이성은 어떻게 달라지나요?",
    "reason": "IoC/DI 이해도를 심화 확인하기 위해",
    "type": "DEEP_DIVE"
  }
}
```

---

## Feedback API

### 5. AI 피드백 생성

```
POST /api/v1/interviews/{interviewId}/feedbacks
Content-Type: application/json
```

> 면접 상태가 `COMPLETED`일 때만 호출 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `interviewId` | `Long` | 면접 세션 ID |

**Request Body — `GenerateFeedbackRequest`**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `answers` | `List<AnswerData>` | O | `@NotEmpty @Valid` | 답변 데이터 목록 |

**AnswerData**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `questionIndex` | `Integer` | O | `@NotNull` | 질문 순서 (0-based) |
| `questionContent` | `String` | O | `@NotBlank` | 질문 내용 |
| `answerText` | `String` | O | `@NotBlank` | 답변 내용 |
| `nonVerbalSummary` | `String` | X | — | 비언어 분석 요약 |
| `voiceSummary` | `String` | X | — | 음성 분석 요약 |

**Response** — `201 Created`

```json
{
  "success": true,
  "data": {
    "interviewId": 1,
    "feedbacks": [
      {
        "id": 1,
        "timestampSeconds": 45.2,
        "category": "CONTENT",
        "severity": "SUGGESTION",
        "content": "IoC 컨테이너의 동작 원리를 더 구체적으로 설명하면 좋겠습니다.",
        "suggestion": "BeanFactory와 ApplicationContext의 차이를 언급해보세요."
      }
    ],
    "totalCount": 5
  }
}
```

---

### 6. 피드백 조회

```
GET /api/v1/interviews/{interviewId}/feedbacks
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `interviewId` | `Long` | 면접 세션 ID |

**Response** — `200 OK`

`FeedbackListResponse` (위 생성 응답과 동일 구조)

---

## Report API

### 7. 종합 리포트 조회

```
GET /api/v1/interviews/{interviewId}/report
```

> 피드백이 1개 이상 존재해야 조회 가능. 없으면 AI가 리포트를 생성 후 반환.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `interviewId` | `Long` | 면접 세션 ID |

**Response** — `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "interviewId": 1,
    "overallScore": 72,
    "summary": "전반적으로 기본 개념에 대한 이해는 있으나, 심화 질문에서 구체성이 부족합니다.",
    "strengths": [
      "Spring 핵심 개념(IoC, DI)에 대한 기본 이해가 좋습니다.",
      "답변 시 논리적인 구조를 갖추고 있습니다."
    ],
    "improvements": [
      "구체적인 사례나 코드 예시를 들어 설명하는 연습이 필요합니다.",
      "비언어적 표현(시선, 자세)에 더 신경 쓰면 좋겠습니다."
    ],
    "feedbackCount": 5
  }
}
```

---

## Enum 정의

### Position (포지션)

| 값 | 설명 |
|----|------|
| `BACKEND` | 백엔드 |
| `FRONTEND` | 프론트엔드 |
| `DEVOPS` | 데브옵스 |
| `DATA_ENGINEER` | 데이터 엔지니어 |
| `FULLSTACK` | 풀스택 |

### InterviewLevel (경력 레벨)

| 값 | 설명 |
|----|------|
| `JUNIOR` | 주니어 |
| `MID` | 미드 |
| `SENIOR` | 시니어 |

### InterviewType (면접 유형)

| 값 | 설명 | 대상 |
|----|------|------|
| `CS_FUNDAMENTAL` | CS 기초 | 공통 |
| `BEHAVIORAL` | 인성/행동 면접 | 공통 |
| `RESUME_BASED` | 이력서 기반 | 공통 |
| `JAVA_SPRING` | Java/Spring | 백엔드 |
| `SYSTEM_DESIGN` | 시스템 설계 | 백엔드 |
| `FULLSTACK_JS` | 풀스택 JS | 풀스택 |
| `REACT_COMPONENT` | React 컴포넌트 | 프론트엔드 |
| `BROWSER_PERFORMANCE` | 브라우저 성능 | 프론트엔드 |
| `INFRA_CICD` | 인프라/CI·CD | 데브옵스 |
| `CLOUD` | 클라우드 | 데브옵스 |
| `DATA_PIPELINE` | 데이터 파이프라인 | 데이터 |
| `SQL_MODELING` | SQL/모델링 | 데이터 |

### InterviewStatus (면접 상태)

| 값 | 설명 | 전이 가능 |
|----|------|-----------|
| `READY` | 준비 완료 | → `IN_PROGRESS` |
| `IN_PROGRESS` | 진행 중 | → `COMPLETED` |
| `COMPLETED` | 완료 | 전이 불가 |

### FeedbackCategory (피드백 카테고리)

| 값 | 설명 |
|----|------|
| `VERBAL` | 언어적 표현 |
| `NON_VERBAL` | 비언어적 표현 |
| `CONTENT` | 답변 내용 |

### FeedbackSeverity (피드백 심각도)

| 값 | 설명 |
|----|------|
| `INFO` | 정보 |
| `WARNING` | 경고 |
| `SUGGESTION` | 제안 |

---

## 에러 코드 목록

### Interview 도메인

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `INTERVIEW_001` | 404 | 면접 세션을 찾을 수 없습니다. |
| `INTERVIEW_002` | 409 | 잘못된 상태 전이입니다. |
| `INTERVIEW_003` | 409 | 진행 중인 면접에서만 후속 질문을 생성할 수 있습니다. |

### Feedback 도메인

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `FEEDBACK_001` | 409 | 완료된 면접에서만 피드백을 생성할 수 있습니다. |
| `FEEDBACK_002` | 500 | 답변 데이터 직렬화에 실패했습니다. |

### Report 도메인

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `REPORT_001` | 409 | 피드백이 없어 리포트를 생성할 수 없습니다. |

### AI 도메인

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `AI_001` | 502 | AI 요청에 실패했습니다. |
| `AI_002` | 502 | AI 서비스가 일시적으로 불안정합니다. |
| `AI_003` | 502 | AI 응답이 비어있습니다. |
| `AI_004` | 504 | AI 서비스 호출 시간이 초과되었습니다. |
| `AI_005` | 502 | AI 응답을 파싱할 수 없습니다. |
| `AI_006` | 502 | AI 피드백을 생성할 수 없습니다. |

### 글로벌 에러

| 상황 | HTTP | 설명 |
|------|------|------|
| 유효성 검증 실패 | 400 | `errors` 필드에 필드별 오류 포함 |
| 파라미터 타입 불일치 | 400 | 잘못된 타입의 파라미터 전달 |
| 알 수 없는 서버 오류 | 500 | 예상치 못한 내부 오류 |
