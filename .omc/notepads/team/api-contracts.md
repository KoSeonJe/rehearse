# Backend ↔ Frontend API 계약서

> 백엔드와 프론트엔드 간 API 인터페이스를 정의합니다.
> 변경 시 양측 합의 후 업데이트합니다.

---

## 공통 규칙

- **Base URL**: `/api/v1`
- **인증**: JWT Bearer Token (Authorization 헤더)
- **응답 형식**: JSON
- **에러 응답 형식**:
  ```json
  {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "상세 에러 메시지",
    "timestamp": "2026-03-10T12:00:00Z"
  }
  ```
- **페이지네이션**: `?page=0&size=20&sort=createdAt,desc`

---

## API 엔드포인트

### 면접 세션 (Interview Session)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/interviews` | 면접 세션 생성 | 📋 미구현 |
| GET | `/api/v1/interviews/{id}` | 면접 세션 조회 | 📋 미구현 |
| PUT | `/api/v1/interviews/{id}` | 면접 세션 수정 | 📋 미구현 |

### AI 질문 생성 (Question Generation)

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/interviews/{id}/questions` | 이력서 기반 질문 생성 | 📋 미구현 |
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

_(API 구현 시 요청/응답 DTO 구조를 여기에 기록)_
