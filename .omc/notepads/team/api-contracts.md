# Backend <-> Frontend API 계약서

> 백엔드와 프론트엔드 간 API 인터페이스를 정의합니다.
> 변경 시 양측 합의 후 업데이트합니다.

---

## 공통 규칙

- **Base URL**: `/api/v1`
- **인증**: JWT Bearer Token (Authorization 헤더) — 면접 세션 API는 현재 인증 없이 동작
- **응답 형식**: JSON
- **성공 응답 형식**:
  ```json
  {
    "success": true,
    "data": { ... },
    "message": null
  }
  ```
- **에러 응답 형식**:
  ```json
  {
    "success": false,
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "상세 에러 메시지",
    "errors": [{ "field": "...", "value": "...", "reason": "..." }],
    "timestamp": "2026-03-10T12:00:00"
  }
  ```
- **페이지네이션**: `?page=0&size=20&sort=createdAt,desc`

---

## API 엔드포인트

### 면접 세션 (Interview Session)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/interviews` | 면접 세션 생성 + AI 질문 생성 | ✅ 구현 완료 |
| GET | `/api/v1/interviews/{id}` | 면접 세션 조회 | ✅ 구현 완료 |
| PATCH | `/api/v1/interviews/{id}/status` | 면접 세션 상태 변경 | ✅ 구현 완료 |

### AI 질문 생성 (Question Generation)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/interviews/{id}/follow-up` | 후속 질문 생성 | 📋 미구현 |

### AI 피드백 (Feedback)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/interviews/{id}/feedback` | 피드백 생성 요청 | 📋 미구현 |
| GET | `/api/v1/interviews/{id}/feedback` | 피드백 조회 | 📋 미구현 |

### 리포트 (Report)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| GET | `/api/v1/interviews/{id}/report` | 종합 리포트 조회 | 📋 미구현 |

---

## DTO 정의

### POST /api/v1/interviews — 면접 세션 생성

**Auth**: 불필요 (MVP)

**Request Body:**
```json
{
  "position": "백엔드 개발자",
  "level": "JUNIOR",
  "interviewType": "CS"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| position | string | O | NotBlank, 최대 100자 |
| level | enum | O | JUNIOR, MID, SENIOR |
| interviewType | enum | O | CS, SYSTEM_DESIGN, BEHAVIORAL |

**Response 201:**
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
      }
    ],
    "createdAt": "2026-03-10T14:30:00"
  },
  "message": null
}
```

**Error Cases:**
- 400 `VALIDATION_ERROR`: 입력값 검증 실패
- 502 `AI_001~AI_005`: Claude API 호출/파싱 실패
- 504 `AI_004`: Claude API 타임아웃

---

### GET /api/v1/interviews/{id} — 면접 세션 조회

**Auth**: 불필요 (MVP)

**Path Parameter:**
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 면접 세션 ID |

**Response 200:**
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
        "content": "질문 내용",
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
- 404 `INTERVIEW_001`: 면접 세션을 찾을 수 없습니다

---

### PATCH /api/v1/interviews/{id}/status — 상태 변경

**Auth**: 불필요 (MVP)

**Path Parameter:**
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 면접 세션 ID |

**Request Body:**
```json
{
  "status": "IN_PROGRESS"
}
```

| 필드 | 타입 | 필수 | 검증 |
|------|------|------|------|
| status | enum | O | IN_PROGRESS, COMPLETED |

**상태 전이 규칙:**
- READY -> IN_PROGRESS (면접 시작)
- IN_PROGRESS -> COMPLETED (면접 종료)
- 역방향 전이 불가

**Response 200:**
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
- 404 `INTERVIEW_001`: 면접 세션을 찾을 수 없습니다
- 409 `INTERVIEW_002`: 잘못된 상태 전이

---

## 에러 코드 체계

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| VALIDATION_ERROR | 400 | 입력값 검증 실패 |
| TYPE_MISMATCH | 400 | 요청 파라미터 타입 불일치 |
| INTERVIEW_001 | 404 | 면접 세션을 찾을 수 없음 |
| INTERVIEW_002 | 409 | 잘못된 상태 전이 |
| AI_001 | 502 | Claude API 클라이언트 에러 |
| AI_002 | 502 | Claude API 서버 에러 |
| AI_003 | 502 | Claude API 빈 응답 |
| AI_004 | 504 | Claude API 타임아웃 |
| AI_005 | 502 | Claude 응답 JSON 파싱 실패 |
