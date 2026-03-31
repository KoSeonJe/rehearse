# Plan 01: 면접 목록/통계/삭제 API

> 상태: Draft
> 작성일: 2026-03-31

## Why

대시보드 UI를 구성하려면 면접 목록, 통계, 삭제 API가 필요하다. 현재는 개별 면접 조회만 가능하고 목록 조회 엔드포인트가 없다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/.../interview/controller/InterviewController.java` | GET 목록, GET 통계, DELETE 엔드포인트 추가 |
| `backend/src/.../interview/service/InterviewService.java` | 목록 조회, 통계 조회, 삭제 로직 |
| `backend/src/.../interview/repository/InterviewRepository.java` | userId 기반 조회 쿼리, 통계 쿼리 추가 |
| `backend/src/.../interview/dto/InterviewListResponse.java` | 목록 응답 DTO (신규) |
| `backend/src/.../interview/dto/InterviewStatsResponse.java` | 통계 응답 DTO (신규) |
| `backend/src/.../interview/entity/Interview.java` | 삭제 가능 상태 검증 메서드 추가 |
| `backend/src/.../interview/entity/InterviewStatus.java` | `isDeletable()` 메서드 추가 |
| `backend/src/test/.../interview/...` | 각 API 통합 테스트 |

## 상세

### GET /api/v1/interviews — 내 면접 목록

```java
// 응답 구조 (페이지네이션)
{
  "data": {
    "content": [
      {
        "id": 1,
        "publicId": "uuid",
        "position": "BACKEND",
        "positionDetail": "Spring Boot 백엔드",
        "interviewTypes": ["CS", "PROJECT"],
        "csSubTopics": ["네트워크", "운영체제"],
        "durationMinutes": 30,
        "status": "COMPLETED",
        "questionCount": 12,
        "createdAt": "2026-03-30T14:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

- `questionCount`: JPQL에서 `LEFT JOIN questionSets → questions`로 COUNT
- 정렬: `createdAt DESC` 고정
- 기본 size: 20

### GET /api/v1/interviews/stats — 통계

```java
{
  "data": {
    "totalCount": 10,
    "completedCount": 7,
    "thisWeekCount": 2
  }
}
```

- `thisWeekCount`: 이번 주 월요일 00:00 ~ 현재 기준 `createdAt` 필터

### DELETE /api/v1/interviews/{id} — 면접 삭제

- 소유권 검증 (`validateOwner`)
- COMPLETED 상태 삭제 불가 → `BusinessException`
- READY / IN_PROGRESS만 삭제 가능 (InterviewStatus에 `isDeletable()` 추가)
- 연관 데이터 cascade 삭제: QuestionSet, Question, QuestionSetFile 등
- S3 오브젝트는 삭제하지 않음 (READY/IN_PROGRESS는 업로드 전이므로 S3 데이터 없음)

## 담당 에이전트

- Implement: `backend` — API 엔드포인트 + 서비스 로직 + 레포지토리 쿼리
- Review: `architect-reviewer` — 레이어링, 쿼리 효율성, cascade 삭제 안전성

## 검증

- 통합 테스트: 목록 조회 (본인 면접만 반환), 통계 정확성, 삭제 성공/실패 케이스
- 소유권 미일치 시 403 반환 확인
- COMPLETED 삭제 시도 시 400 반환 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
