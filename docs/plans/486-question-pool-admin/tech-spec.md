# Tech Spec — Question Pool Admin

> **작성자**: 구현 agent
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 구현 진입 ★

---

## Why → Goal (1줄 미러)

운영자가 DB 직접 조회 없이 `question_pool`의 질문/모범답안/cacheKey/category를 확인하고 필요한 질문을 직접 추가할 수 있게 한다.

## Evidence

- 현재 구조:
  - `QuestionPool` 엔티티는 `cacheKey`, `content`, `ttsContent`, `category`, `bestAnswer`, `isActive`, `createdAt`를 가진다.
  - `QuestionPoolRepository`는 현재 active cacheKey 조회만 제공하므로 어드민 검색용 동적 조건 조회가 없다.
  - `QuestionPoolService`는 질문 생성 경로의 풀 선택/저장을 담당한다.
  - 기존 어드민 피드백 API는 `X-Admin-Password` 헤더로 인증한다.
  - FE는 `PasswordProtectedRoute`에서 `sessionStorage['admin-password']`를 저장하고 어드민 API 호출 시 헤더로 전달한다.
- 사용자 발화:
  - "question pool에 있는 질문, 답변, 캐시키 등을 조회하고 질문을 추가할 수 있는 어드민 페이지"
- 추정 / 미확인 가정:
  - 이슈의 `referenceType` 요구는 현재 실코드 `question_pool` 엔티티에 컬럼이 없어 이번 MVP에서는 제외한다.
  - `best_answer` DB 컬럼은 JPA naming 전략으로 `bestAnswer` 필드와 매핑된다고 본다.

## Trade-offs

### Option A (채택): 기존 스키마 기반 조회/생성
- 장점:
  - 마이그레이션 없이 빠르게 운영 기능을 제공한다.
  - 기존 질문 생성 경로와 데이터 모델을 건드리지 않아 회귀 위험이 낮다.
  - 추가된 row는 기존 `findByCacheKeyAndIsActiveTrue` 경로에서 즉시 사용 가능하다.
- 단점:
  - `referenceType`은 이번 화면에 포함할 수 없다.
  - `(cache_key, content)` dedup 부재는 그대로 남는다.
- 사유:
  - 사용자가 요청한 핵심은 "조회와 질문 추가"이며, 스키마 정책 변경은 #407 성격의 별도 작업이다.

### Option B (폐기): `reference_type` 포함 스키마 확장
- 장점:
  - 문서의 `reference_type`과 API 응답을 정합시킬 수 있다.
- 단점:
  - Flyway 마이그레이션, 기존 생성 경로 매핑, seed 데이터 정책까지 확장된다.
  - 질문 풀 관리 UI의 MVP 범위를 넘는다.
- 폐기 사유:
  - 이슈 #486의 운영 가치에 비해 데이터 모델 변경 리스크가 크다.

### Option C (폐기): seed SQL 관리 화면만 제공
- 장점:
  - 런타임 API 설계가 단순하다.
- 단점:
  - 운영자가 실제 DB 상태를 확인할 수 없고 배포 없이 추가할 수 없다.
- 폐기 사유:
  - "조회하고 질문을 추가"라는 요구를 충족하지 못한다.

## Architecture

```
[Admin Page /admin/question-pool]
  ├─ useAdminQuestionPools(filters, page, size)
  │    → GET /api/v1/admin/question-pools
  └─ useCreateAdminQuestionPool()
       → POST /api/v1/admin/question-pools

[AdminQuestionPoolController]
  ├─ password header validation
  └─ [AdminQuestionPoolService]
       ├─ search: QuestionPoolRepository + Specification
       └─ create: QuestionPool.create(cacheKey, content, ttsContent, category, bestAnswer) + save
```

## Data Model

DB 스키마 변경 없음.

기존 `question_pool` 컬럼만 사용한다.

| API field | Entity field | DB column |
|-----------|--------------|-----------|
| `id` | `id` | `id` |
| `cacheKey` | `cacheKey` | `cache_key` |
| `content` | `content` | `content` |
| `ttsContent` | `ttsContent` | `tts_content` |
| `category` | `category` | `category` |
| `bestAnswer` | `bestAnswer` | `best_answer` |
| `isActive` | `isActive` | `is_active` |
| `createdAt` | `createdAt` | `created_at` |

## API Contract

공통 인증:

- Header: `X-Admin-Password: {password}`
- 비밀번호 누락/불일치: `401 ADMIN_001`

### Endpoint: 목록 조회

`GET /api/v1/admin/question-pools`

#### Query Params

| name | type | required | default | note |
|------|------|----------|---------|------|
| `page` | number | no | `0` | 0-based |
| `size` | number | no | `20` | 1~100으로 clamp |
| `cacheKey` | string | no | none | 부분 일치, trim 후 blank면 무시 |
| `category` | string | no | none | 부분 일치, trim 후 blank면 무시 |
| `isActive` | boolean | no | none | true/false |
| `keyword` | string | no | none | `content` 또는 `bestAnswer` 부분 검색 |

#### Response 200

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
        "content": "프로세스와 스레드의 차이를 설명해주세요.",
        "ttsContent": "프로세스와 스레드의 차이를 설명해 주세요.",
        "category": "운영체제",
        "bestAnswer": "프로세스는 독립된 주소 공간을 가지고 스레드는 같은 프로세스의 주소 공간을 공유합니다.",
        "isActive": true,
        "createdAt": "2026-05-16T10:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "size": 20,
    "number": 0
  }
}
```

### Endpoint: 단건 생성

`POST /api/v1/admin/question-pools`

#### Request

```json
{
  "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
  "content": "프로세스와 스레드의 차이를 설명해주세요.",
  "ttsContent": "프로세스와 스레드의 차이를 설명해 주세요.",
  "category": "운영체제",
  "bestAnswer": "프로세스는 독립된 주소 공간을 가지고 스레드는 같은 프로세스의 주소 공간을 공유합니다."
}
```

Validation:

- `cacheKey`: required, non-blank, max 255
- `content`: required, non-blank
- `ttsContent`: optional
- `category`: optional, max 100
- `bestAnswer`: optional

#### Response 200

```json
{
  "success": true,
  "data": {
    "id": 2,
    "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
    "content": "프로세스와 스레드의 차이를 설명해주세요.",
    "ttsContent": "프로세스와 스레드의 차이를 설명해 주세요.",
    "category": "운영체제",
    "bestAnswer": "프로세스는 독립된 주소 공간을 가지고 스레드는 같은 프로세스의 주소 공간을 공유합니다.",
    "isActive": true,
    "createdAt": "2026-05-16T10:35:00"
  }
}
```

### Error

- 400: validation 실패 (`VALIDATION_ERROR`)
- 401: 관리자 비밀번호 누락/불일치 (`ADMIN_001`)
- 500: 예상 밖 서버 오류

## Backend Design

### Files

- Create: `backend/src/main/java/com/rehearse/api/domain/question/controller/AdminQuestionPoolController.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/AdminQuestionPoolSearchCondition.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/AdminQuestionPoolResponse.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/CreateQuestionPoolRequest.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/service/AdminQuestionPoolService.java`
- Modify: `backend/src/main/java/com/rehearse/api/domain/question/repository/QuestionPoolRepository.java`
- Create: `backend/src/test/java/com/rehearse/api/domain/question/controller/AdminQuestionPoolControllerTest.java`
- Create: `backend/src/test/java/com/rehearse/api/domain/question/service/AdminQuestionPoolServiceTest.java`
- Create: `backend/src/test/java/com/rehearse/api/domain/question/repository/QuestionPoolRepositoryTest.java`

### Repository

`QuestionPoolRepository`에 `JpaSpecificationExecutor<QuestionPool>`를 추가한다.

어드민 검색 조건은 service 내부 private method 또는 별도 specification builder로 시작한다. 조건이 단순하므로 별도 파일은 만들지 않는다.

검색 정책:

- `cacheKey`: `lower(cacheKey) like %lower(trim(value))%`
- `category`: `lower(category) like %lower(trim(value))%`
- `isActive`: exact match
- `keyword`: `lower(content) like %lower(trim(keyword))% OR lower(bestAnswer) like %lower(trim(keyword))%`
- sort: `createdAt DESC`, 동률 `id DESC`

### Service

`AdminQuestionPoolService` 책임:

- `Page<AdminQuestionPoolResponse> search(AdminQuestionPoolSearchCondition condition, Pageable pageable)`
- `AdminQuestionPoolResponse create(CreateQuestionPoolRequest request)`

생성 시 `QuestionPool.create(request.cacheKey(), request.content(), request.ttsContent(), request.category(), request.bestAnswer())`를 사용한다. 중복 방지는 이번 범위가 아니다.

### Controller

기존 `AdminFeedbackController`와 동일하게 컨트롤러에서 `X-Admin-Password` 헤더를 검증한다.

페이지 사이즈는 기존 어드민 피드백 API와 동일하게 1~100으로 보정한다.

## Frontend Design

### Files

- Create: `frontend/src/types/question-pool.ts`
- Create: `frontend/src/hooks/use-admin-question-pool.ts`
- Create: `frontend/src/pages/admin-question-pool-page.tsx`
- Modify: `frontend/src/app.tsx`
- Modify: `frontend/tests/a11y/pages.spec.tsx`
- Create: `frontend/src/pages/__tests__/admin-question-pool-page.test.tsx`

### Page

`/admin/question-pool`는 기존 `PasswordProtectedRoute` 아래에 추가한다.

레이아웃:

- 상단: 제목, 총 개수, 새 질문 추가 버튼
- 필터 영역: cacheKey input, category input, 활성 상태 select, keyword input, 검색/초기화 버튼
- 데스크탑: 테이블 뷰
- 모바일: 카드 리스트
- 생성: Dialog 기반 단건 추가 폼

### Hook

`useAdminQuestionPools(filters, page, size)`:

- queryKey: `['admin-question-pools', filters, page, size]`
- `URLSearchParams`로 query string 구성
- header: `X-Admin-Password`

`useCreateAdminQuestionPool()`:

- POST 후 `admin-question-pools` query invalidate

## Verification (완료 판정)

- [ ] Backend controller:
  - 비밀번호 누락/불일치 401
  - 목록 기본 page/size 적용
  - size 1~100 clamp
  - 필터 query가 condition으로 전달됨
  - 생성 request validation 실패 400
  - 생성 성공 200 + 응답 필드 확인
- [ ] Backend service:
  - 검색 조건별 specification이 repository 호출에 사용됨
  - 생성 시 `QuestionPool.create`와 save 호출
- [ ] Backend repository:
  - MySQL Testcontainers에서 `cacheKey`, category, `isActive`, keyword 필터가 동작
- [ ] Frontend:
  - 목록 로딩/빈 상태/데이터 상태 렌더링
  - 필터 입력 후 검색 시 hook query 변경
  - 생성 폼 제출 성공 후 목록 invalidate
  - a11y smoke에 admin question pool 페이지 추가
- [ ] Commands:
  - `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.*"`
  - `cd frontend && npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`
  - `cd frontend && npm run test -- tests/a11y/pages.spec.tsx`
  - 필요 시 `cd frontend && npm run lint`

## Pre / Post State

### Pre (현재)
- `question_pool`은 내부 질문 생성 경로에서만 조회/저장된다.
- 운영자용 목록/생성 HTTP API가 없다.
- FE 어드민 화면은 서비스 피드백 조회만 있다.

### Post (구현 후)
- `GET /api/v1/admin/question-pools`로 운영자가 풀을 조회한다.
- `POST /api/v1/admin/question-pools`로 운영자가 풀 row를 추가한다.
- `/admin/question-pool`에서 조회/필터/생성을 수행한다.

## 위험 / 마이그레이션 / 롤백

- 위험:
  - 관리자 비밀번호가 sessionStorage에 저장되는 기존 패턴을 그대로 사용한다. 신규 위험은 아니지만 보안 수준은 낮다.
  - dedup 부재로 동일 `cacheKey + content` 중복 row 생성 가능성이 있다.
  - `reference_type`은 실코드에 없으므로 화면에서 제공하지 않는다.
- 마이그레이션 전략:
  - DB 스키마 변경 없음.
  - 기존 질문 생성 경로 변경 없음.
- 롤백 시나리오:
  - FE route 제거와 BE admin controller/service 제거만으로 기능 비활성화 가능.
  - 생성된 `question_pool` row는 필요 시 운영 SQL로 비활성화한다.

## 분기 결정

- [ ] 단일 영역 → `implement.md` 1개
- [x] BE+FE 동시 → `implement-be.md` + `implement-fe.md` (API contract 합의 후 병렬)
- [ ] BE 선행 강제 (강결합) → `implement-be.md` 머지 후 `implement-fe.md`
