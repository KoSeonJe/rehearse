# Feature Specification: 면접 세션 생성 + AI 질문 생성

> **문서 ID**: PLAN-001
> **작성일**: 2026-03-10
> **작성자**: Planner
> **상태**: Draft
> **우선순위**: P0 (Must-have)

---

## Overview

### 문제 정의

Rehearse의 모든 기능(영상 녹화, 비언어 분석, AI 피드백)은 "면접 세션"이 존재해야 동작한다. 면접 세션 없이는 어떤 핵심 기능도 사용할 수 없으므로, 이것이 MVP의 첫 번째 구현 대상이다.

### 솔루션 요약

사용자가 직무/레벨/면접 유형을 선택하면 백엔드가 Claude API를 호출하여 맞춤형 면접 질문 5개를 생성하고, 사용자는 생성된 질문을 확인한 후 면접을 시작할 수 있다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 이 기능 없이는 플랫폼의 어떤 기능도 작동하지 않음 |
| **Effort** | Medium - Backend API + Claude API 연동 + Frontend 3개 페이지 |
| **결론** | **P0** - High Impact, Medium Effort. 즉시 개발 |

---

## Target Users

- **주니어 개발자**: 취업 준비 중, CS 기초 면접 연습이 필요한 사용자
- **경력 개발자**: 이직 준비 중, 시스템 설계 면접 대비가 필요한 사용자
- **부트캠프 수료생**: 면접 경험이 부족하여 BQ 연습이 필요한 사용자

---

## User Stories

### US-1: 면접 세션 설정

**As a** 면접 준비 중인 개발자
**I want to** 직무, 레벨, 면접 유형을 선택하여 면접 세션을 생성하고 싶다
**So that** 나의 수준과 목적에 맞는 맞춤형 면접을 준비할 수 있다

**Acceptance Criteria:**
- [ ] 직무 입력 필드가 있다 (텍스트 입력, 예: "백엔드 개발자", "프론트엔드 개발자")
- [ ] 레벨 선택이 있다 (주니어 / 미드 / 시니어, 라디오 또는 셀렉트)
- [ ] 면접 유형 선택이 있다 (CS 기초 / 시스템 설계 / Behavioral, 라디오 또는 카드 선택)
- [ ] 직무가 비어있으면 제출 버튼이 비활성화된다
- [ ] 레벨 미선택 시 제출 버튼이 비활성화된다
- [ ] 면접 유형 미선택 시 제출 버튼이 비활성화된다
- [ ] "질문 생성하기" 버튼을 누르면 API를 호출하고 로딩 상태를 표시한다

**Priority:** P0
**Notes:**
- 직무는 자유 텍스트 입력 (드롭다운 아님). MVP 단순화를 위해 사전 정의된 목록을 두지 않는다
- 레벨 enum: `JUNIOR`, `MID`, `SENIOR`
- 면접 유형 enum: `CS`, `SYSTEM_DESIGN`, `BEHAVIORAL`

---

### US-2: AI 질문 생성

**As a** 면접 세션을 생성한 사용자
**I want to** AI가 나의 직무/레벨에 맞는 면접 질문을 자동으로 생성해주길 원한다
**So that** 실제 면접과 유사한 수준의 질문으로 연습할 수 있다

**Acceptance Criteria:**
- [ ] 세션 생성 요청 시 Claude API를 통해 질문 5개가 생성된다
- [ ] 각 질문에는 content(질문 내용), category(세부 카테고리), order(순서)가 포함된다
- [ ] CS 기초 선택 시 자료구조/알고리즘/OS/네트워크/DB 관련 질문이 생성된다
- [ ] 시스템 설계 선택 시 스케일링/아키텍처/트레이드오프 관련 질문이 생성된다
- [ ] Behavioral 선택 시 STAR 기반 경험 질문이 생성된다
- [ ] 질문 생성이 10초 이내에 완료된다 (Claude API 응답 기준)
- [ ] Claude API 호출 실패 시 사용자에게 에러 메시지를 표시하고 재시도 버튼을 제공한다

**Priority:** P0
**Notes:**
- Claude API 호출은 반드시 백엔드(Spring Boot)에서만 실행 (CLAUDE.md 규칙)
- 모델: claude-sonnet-4-20250514
- 질문 수: 5개 고정 (MVP)
- 각 질문에는 내부적으로 평가 루브릭(evaluationCriteria)도 함께 생성하여 저장한다 (후속 피드백 생성 시 사용)

---

### US-3: 면접 대기 화면 (질문 확인 + 면접 시작)

**As a** AI 질문이 생성된 사용자
**I want to** 생성된 질문 목록을 미리 확인하고, 준비가 되면 면접을 시작하고 싶다
**So that** 어떤 질문이 나올지 파악하고 심리적으로 준비할 수 있다

**Acceptance Criteria:**
- [ ] 생성된 5개 질문이 순서대로 카드 형태로 표시된다
- [ ] 각 질문 카드에 번호, 카테고리, 질문 내용이 표시된다
- [ ] "면접 시작" 버튼이 있다
- [ ] "면접 시작" 버튼 클릭 시 면접 세션 상태가 `IN_PROGRESS`로 변경된다
- [ ] "질문 다시 생성" 버튼이 있어 새로운 질문 세트를 받을 수 있다
- [ ] "뒤로 가기"로 설정 화면으로 돌아갈 수 있다

**Priority:** P0
**Notes:**
- "면접 시작" 클릭 후 실제 면접 진행 화면(녹화/MediaPipe)은 별도 기능 스펙에서 다룬다
- 이번 스펙에서는 상태를 `IN_PROGRESS`로 변경하는 API 호출까지만 포함

---

### US-4: 면접 세션 조회

**As a** 이전에 면접 세션을 생성한 사용자
**I want to** 특정 면접 세션의 상세 정보를 조회하고 싶다
**So that** 생성된 질문과 세션 상태를 확인할 수 있다

**Acceptance Criteria:**
- [ ] 세션 ID로 면접 세션을 조회할 수 있다
- [ ] 응답에 세션 상태, 질문 목록, 설정 정보(직무/레벨/유형), 생성일시가 포함된다
- [ ] 존재하지 않는 세션 ID 조회 시 404 에러를 반환한다

**Priority:** P0
**Notes:**
- 면접 대기 화면 진입 시, 질문 목록 새로고침 시 사용

---

## Scope

### In Scope

1. **Backend**
   - 면접 세션 생성 API (`POST /api/v1/interviews`)
   - 면접 세션 조회 API (`GET /api/v1/interviews/{id}`)
   - 면접 세션 상태 변경 API (`PATCH /api/v1/interviews/{id}/status`)
   - Claude API 연동 서비스 (질문 생성)
   - 면접 세션 엔티티 + 질문 엔티티 (JPA)
   - 요청/응답 DTO + Validation

2. **Frontend**
   - 면접 설정 페이지 (`/interview/setup`)
   - 면접 대기 페이지 (`/interview/{id}/ready`)
   - TanStack Query 훅 (세션 생성, 조회)
   - Zustand 스토어 확장 (세션 설정 상태)
   - 로딩/에러 상태 UI

3. **데이터**
   - InterviewSession 테이블
   - Question 테이블

### Out of Scope

| 항목 | 사유 |
|------|------|
| 이력서 파일 업로드/파싱 | MVP 1차 범위 외. 직무/레벨 텍스트 입력으로 대체 |
| 후속 질문 (동적 생성) | 면접 진행 중 실시간 생성. 별도 기능 스펙 |
| 영상 녹화 / MediaPipe / STT | 면접 진행 기능. 별도 기능 스펙 |
| AI 피드백 / 종합 리포트 | 면접 종료 후 기능. 별도 기능 스펙 |
| 사용자 인증/로그인 | 별도 기능 스펙. 이번에는 인증 없이 동작 |
| 면접 세션 목록 조회 / 삭제 | P2. 백로그 |
| 질문 수 설정 (3/5/7개) | P2. MVP에서는 5개 고정 |
| 면접 유형 복수 선택 | P2. MVP에서는 단일 선택 |

---

## Page Flow (Designer 참고)

```
[홈 페이지]  "/"
    │
    │  "새 면접 시작" 버튼 클릭
    ▼
[면접 설정 페이지]  "/interview/setup"
    │
    │  직무 입력 + 레벨 선택 + 유형 선택
    │  → "질문 생성하기" 버튼 클릭
    │  → POST /api/v1/interviews 호출
    │  → 로딩 스피너 표시 (질문 생성 중...)
    │
    ▼
[면접 대기 페이지]  "/interview/{id}/ready"
    │
    │  생성된 질문 5개 카드로 표시
    │
    ├── "질문 다시 생성" → POST /api/v1/interviews (새 세션)
    ├── "뒤로 가기" → "/interview/setup"
    │
    │  "면접 시작" 버튼 클릭
    │  → PATCH /api/v1/interviews/{id}/status (IN_PROGRESS)
    │
    ▼
[면접 진행 페이지]  "/interview/{id}/session"  ← 별도 기능 스펙
```

### 페이지별 핵심 요소

**면접 설정 페이지 (`/interview/setup`)**
- 직무 텍스트 입력 (placeholder: "예: 백엔드 개발자")
- 레벨 선택 (주니어 / 미드 / 시니어) - 카드 또는 라디오
- 면접 유형 선택 (CS 기초 / 시스템 설계 / Behavioral) - 카드 형태, 각각 간단한 설명 포함
- "질문 생성하기" 버튼 (primary, 하단 고정)
- 로딩 오버레이 (질문 생성 중 상태)

**면접 대기 페이지 (`/interview/{id}/ready`)**
- 상단: 세션 정보 요약 (직무, 레벨, 유형)
- 중앙: 질문 카드 리스트 (1~5번)
- 하단: "면접 시작" 버튼 (primary) + "질문 다시 생성" 버튼 (secondary)

---

## Backend API Specification

### API-1: 면접 세션 생성

```
POST /api/v1/interviews
```

**Request Body:**
```json
{
  "position": "백엔드 개발자",
  "level": "JUNIOR",
  "interviewType": "CS"
}
```

**Validation:**
- `position`: NotBlank, 최대 100자
- `level`: NotNull, enum (JUNIOR, MID, SENIOR)
- `interviewType`: NotNull, enum (CS, SYSTEM_DESIGN, BEHAVIORAL)

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "position": "백엔드 개발자",
    "level": "JUNIOR",
    "interviewType": "CS",
    "status": "READY",
    "questions": [
      {
        "id": 1,
        "content": "HashMap과 TreeMap의 차이점과 각각의 시간 복잡도를 설명해주세요.",
        "category": "자료구조",
        "order": 1
      },
      {
        "id": 2,
        "content": "프로세스와 스레드의 차이점을 설명하고, 멀티스레딩의 장단점을 말씀해주세요.",
        "category": "운영체제",
        "order": 2
      }
    ],
    "createdAt": "2026-03-10T14:30:00"
  },
  "message": null
}
```

**Error Cases:**
- 400: Validation 실패 (빈 직무, 잘못된 enum 값)
- 502: Claude API 호출 실패
- 504: Claude API 타임아웃 (30초 초과)

---

### API-2: 면접 세션 조회

```
GET /api/v1/interviews/{id}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "position": "백엔드 개발자",
    "level": "JUNIOR",
    "interviewType": "CS",
    "status": "READY",
    "questions": [
      {
        "id": 1,
        "content": "HashMap과 TreeMap의 차이점...",
        "category": "자료구조",
        "order": 1
      }
    ],
    "createdAt": "2026-03-10T14:30:00"
  },
  "message": null
}
```

**Error Cases:**
- 404: 존재하지 않는 세션 ID

---

### API-3: 면접 세션 상태 변경

```
PATCH /api/v1/interviews/{id}/status
```

**Request Body:**
```json
{
  "status": "IN_PROGRESS"
}
```

**Validation:**
- `status`: NotNull, enum (IN_PROGRESS, COMPLETED)
- 상태 전이 규칙: READY -> IN_PROGRESS -> COMPLETED (역방향 불가)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "IN_PROGRESS"
  },
  "message": null
}
```

**Error Cases:**
- 404: 존재하지 않는 세션 ID
- 409: 잘못된 상태 전이 (예: COMPLETED -> IN_PROGRESS)

---

## Data Model

### InterviewSession 엔티티

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long (PK) | AUTO_INCREMENT | 세션 식별자 |
| position | VARCHAR(100) | NOT NULL | 직무 (예: 백엔드 개발자) |
| level | ENUM | NOT NULL | JUNIOR, MID, SENIOR |
| interview_type | ENUM | NOT NULL | CS, SYSTEM_DESIGN, BEHAVIORAL |
| status | ENUM | NOT NULL, DEFAULT 'READY' | READY, IN_PROGRESS, COMPLETED |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | NOT NULL | 수정일시 |

### Question 엔티티

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long (PK) | AUTO_INCREMENT | 질문 식별자 |
| interview_session_id | Long (FK) | NOT NULL | 세션 참조 |
| content | TEXT | NOT NULL | 질문 내용 |
| category | VARCHAR(50) | NOT NULL | 세부 카테고리 (예: 자료구조, OS) |
| question_order | INT | NOT NULL | 질문 순서 (1~5) |
| evaluation_criteria | TEXT | NULL | AI가 생성한 평가 기준 (내부용, API 미노출) |
| created_at | DATETIME | NOT NULL | 생성일시 |

**관계:** InterviewSession 1:N Question (cascade)

---

## Technical Constraints

1. **Claude API 호출은 백엔드에서만** - 프론트엔드에서 직접 호출 금지 (API Key 노출 방지, CLAUDE.md 규칙)
2. **Claude API 타임아웃**: 30초 (Spring WebClient 또는 RestClient 사용)
3. **H2 In-Memory**: 로컬 개발 환경에서 사용, 운영은 MySQL 8.0
4. **응답 래퍼**: 기존 `ApiResponse<T>` 클래스 사용 (`success`, `data`, `message`)
5. **에러 처리**: 기존 `GlobalExceptionHandler` + `BusinessException` 패턴 활용
6. **프론트엔드 라우팅**: react-router-dom 사용 (기존 app.tsx 구조 확장)
7. **서버 상태**: TanStack Query 사용 (useMutation으로 세션 생성, useQuery로 세션 조회)

---

## Dependencies

| 의존성 | 타입 | 상태 | 설명 |
|--------|------|------|------|
| Claude API Key | 외부 서비스 | 필요 | `ANTHROPIC_API_KEY` 환경변수 설정 필요 |
| Spring Boot 기본 구조 | 내부 | 완료 | Controller/Service/Repository 패턴, ApiResponse, GlobalExceptionHandler 존재 |
| Frontend 기본 구조 | 내부 | 완료 | Vite + React Router + Zustand + ApiClient 존재 |
| interview.ts 타입 | 내부 | 확장 필요 | 기존 타입에 position, level, interviewType 필드 추가 필요 |

---

## Claude API Prompt Design (Backend 참고)

### 질문 생성 프롬프트 구조

```
System Prompt:
- 역할: 한국 IT 기업의 시니어 개발자 면접관
- 면접 유형별 평가 기준 명시
- 출력 형식: JSON 배열

User Prompt:
- 직무: {position}
- 레벨: {level}
- 면접 유형: {interviewType}
- 요청: 위 조건에 맞는 면접 질문 5개 + 각 질문별 평가 기준 생성
```

**출력 형식 (Claude에게 요청할 JSON):**
```json
{
  "questions": [
    {
      "content": "질문 내용",
      "category": "세부 카테고리명",
      "order": 1,
      "evaluationCriteria": "이 질문에서 평가할 핵심 포인트"
    }
  ]
}
```

---

## Frontend State Management (Frontend 참고)

### Zustand Store 확장

기존 `useInterviewStore`에 세션 설정 관련 상태를 추가하거나, 별도 `useSessionSetupStore`를 만든다.

```
필요한 클라이언트 상태:
- position: string (직무 입력값)
- level: 'JUNIOR' | 'MID' | 'SENIOR' | null (선택된 레벨)
- interviewType: 'CS' | 'SYSTEM_DESIGN' | 'BEHAVIORAL' | null (선택된 유형)
```

### TanStack Query 훅

```
- useCreateInterview: useMutation (POST /api/v1/interviews)
- useInterview: useQuery (GET /api/v1/interviews/{id})
- useUpdateInterviewStatus: useMutation (PATCH /api/v1/interviews/{id}/status)
```

---

## Self-Verify Checklist

- [x] 모든 유저 스토리에 수용 기준 3개 이상 있는가: US-1(7개), US-2(7개), US-3(6개), US-4(3개)
- [x] In/Out Scope가 명시되어 있는가: In Scope 3영역, Out of Scope 8항목
- [x] 모호한 요구사항이 남아있지 않은가: 직무 입력 방식, 질문 수, enum 값 등 모두 확정
- [x] 우선순위와 근거가 명시되어 있는가: P0, Impact/Effort 매트릭스 제시
- [x] API 엔드포인트 초안이 있는가: 3개 엔드포인트 + Request/Response 정의
- [x] 페이지 흐름이 정의되어 있는가: 3개 페이지 + 네비게이션 흐름도
- [x] 기존 코드베이스 패턴과 일관성 있는가: ApiResponse, GlobalExceptionHandler, interview.ts 타입 확인
