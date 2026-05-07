# API: 면접 목록 / 단건 / 통계 조회

> Endpoints (4개 묶음):
> - `GET /api/v1/interviews?page&size` — 목록 (페이지네이션)
> - `GET /api/v1/interviews/{id}` — 단건 (내부 ID)
> - `GET /api/v1/interviews/by-public-id/{publicId}` — 단건 (외부 UUID)
> - `GET /api/v1/interviews/stats` — 사용자 통계
>
> Action: 사용자가 자기가 생성한 면접 세션을 조회한다 (목록 / 단건 / 카운트 통계).
> 관련 테이블: `interview` (read) / `question_set` + `question` + `question_answer` (read, join)
> 관련 외부 의존: 없음

---

## 입력 — 목록 (`GET /`)

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| query | `page` | int | default 0 | 0-base |
| query | `size` | int | default 20, clamped 1..100 | 페이지 크기 |

## 입력 — 단건 (`GET /{id}` / `GET /by-public-id/{publicId}`)

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `id` 또는 `publicId` | Long / UUID | required | 식별자 |
| header | `Authorization` | Bearer | required | JWT |

## 입력 — 통계 (`GET /stats`)

JWT 만 필요. 입력 없음.

---

## 출력 (200) — 목록

페이지네이션 (`Page<InterviewListResponse>`):

| 필드 | 타입 | 의미 |
|------|------|------|
| `content[].id` | Long | 면접 PK |
| `content[].publicId` | string | UUID |
| `content[].position` / `positionDetail` / `interviewTypes` / `csSubTopics` / `durationMinutes` | — | 메타 |
| `content[].status` | enum | `READY` / `IN_PROGRESS` / `COMPLETED` |
| `content[].answerCount` | Long | 답변 수 (분석 FAILED 인 question_set 제외) |
| `content[].createdAt` | datetime | 생성 시각 |
| `pageable` / `totalElements` / `totalPages` / … | Spring `Page` | 표준 |

## 출력 (200) — 단건

`InterviewResponse` (모든 메타 + `questionSets` 풀 detail).

## 출력 (200) — 통계

| 필드 | 타입 | 의미 |
|------|------|------|
| `totalCount` | Long | 사용자가 생성한 면접 총수 |
| `completedCount` | Long | `status = COMPLETED` 카운트 |
| `thisWeekCount` | Long | 월요일 00:00 (`Asia/Seoul`) 이후 생성된 면접 수 |

## 출력 (4xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 401 | — | JWT 부재 / 만료 |
| 403 | `INTERVIEW_008 FORBIDDEN` | 단건 조회 시 본인 소유 아님 (`Interview.validateOwner`) |
| 404 | `INTERVIEW_001 NOT_FOUND` | 단건 조회 시 ID / publicId 불일치 |

---

## 흐름

### 1. 목록 (`GET /`)
1. `size` clamp: `Math.min(Math.max(size, 1), 100)` (Controller)
2. `interviewRepository.findAllByUserId(userId, PageRequest.of(page, safeSize))` — `WHERE user_id = ? ORDER BY created_at DESC, id DESC`
3. `interviewIds` 추출 → `countAnswersByInterviewIds` 1쿼리로 답변 수 집계 (N+1 회피)
   - JPQL: `QuestionAnswer` JOIN `question` JOIN `question_set` GROUP BY `question_set.interview.id`
   - WHERE: `question_set.fileMetadata IS NULL OR fileMetadata.status <> FAILED` (FAILED 분석 제외)
4. `Page<Interview>` → `Page<InterviewListResponse>` 매핑 (answerCount 합류)

### 2. 단건 (`GET /{id}`)
1. `InterviewFinder.findById(id)` — `findByIdWithElementCollections` (`@EntityGraph` interviewTypes / csSubTopics fetch)
2. `interview.validateOwner(userId)` — `userId != null && != requester` → 403
3. `questionSetRepository.findByInterviewIdWithQuestions(id)` — questionSets + questions 단일 fetch
4. `InterviewResponse.from(interview, questionSets)`

### 3. 단건 by public-id
1. `InterviewFinder.findByPublicId(publicId)` — `@EntityGraph` 동일
2. 이후 동일 (validateOwner → questionSets fetch → 매핑)

### 4. 통계
1. `countByUserId` — 총수
2. `countByUserIdAndStatus(COMPLETED)` — 완료수
3. 주간 시작점 = `LocalDate.now(ZoneId.of("Asia/Seoul")).with(DayOfWeek.MONDAY).atStartOfDay()`
4. `countByUserIdAndCreatedAtAfter(weekStart)`
5. 응답 빌드

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| `user_id = NULL` 인 레거시 row | `validateOwner` 가 NULL 무시 → 누구나 조회 가능. ⚠️ 정책-코드 갭 (Issue #404 추적: 1번 ❓TODO 보류) |
| `size > 100` | 100 으로 강제 clamp |
| `size = 0` | 1 로 강제 clamp |
| 빈 `interviewIds` | `countAnswersByInterviewIds` 호출 스킵 (N+1 회피 + empty IN 방지) |
| 다른 사용자 publicId 추측 접근 | `validateOwner` 가 403 — UUID 보호로 사실상 차단 |
| 통계 기준 타임존이 사용자 로컬과 다름 | Asia/Seoul 고정 (서버 정책) — ❓TODO 다중 타임존 |
| `INTERVIEW_001` vs `INTERVIEW_008` 우선순위 | finder 가 NOT_FOUND 먼저 발생 → 권한 체크 도달 안 함 |

---

## 관찰성

- **로그**: 본 액션 단독 INFO 로그 없음 (조회 빈도 높음). ERROR 만 GlobalExceptionHandler 단계에서.
- **메트릭**: `http_server_requests_seconds{uri="/api/v1/interviews"}` (Spring Boot Actuator 기본).
- **알람**: 별도 없음.

---

## 성능 / 쿼리 노트

- 목록: `findAllByUserId` 1쿼리 + `countAnswersByInterviewIds` 1쿼리 + count query 1쿼리 = **총 3 쿼리** (페이지당, N과 무관)
- 단건: `findByIdWithElementCollections` (EntityGraph 로 ElementCollection 동시 fetch) + `findByInterviewIdWithQuestions` = **총 2 쿼리**
- `default_batch_fetch_size: 100` (`application.yml:16`) — ElementCollection lazy loading 시 batch
- `@BatchSize(size = 100)` — `interviewTypes` / `csSubTopics`

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.question.repository.QuestionSetRepository` | questionSets fetch | calls — `findByInterviewIdWithQuestions` |
| `com.rehearse.api.domain.question.dto.QuestionSetResponse` | 단건 응답 변환 | calls — `from(QuestionSet)` |
| `org.springframework.data.domain.Page` / `Pageable` / `PageRequest` | 페이지네이션 | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (소유자 / status)
- 임계값:
  - `size` clamp 1..100 (`InterviewController:52`)
  - 통계 주간 시작 = `Asia/Seoul` MONDAY 00:00 (`InterviewQueryService:67-69`)
- ❓TODO(사용자 확인) — Issue #404 비스코프 (보류):
  - `Asia/Seoul` 고정 — 다중 타임존 사용자 정책 미정
  - `user_id = NULL` 레거시 row 조회 정책 — backfill / 차단 / 허용 미정
