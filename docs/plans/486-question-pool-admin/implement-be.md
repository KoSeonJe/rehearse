# Implement (Backend) — Question Pool Admin

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ 사용자 명시 승인 후 시작 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 의 요청/응답 schema 확정 여부 확인.

- [ ] Endpoint 경로 / 메서드 합의됨
- [ ] Request / Response schema 합의됨
- [ ] Error 코드 매핑 합의됨
- [ ] `referenceType`은 현 스키마 부재로 제외하는 데 합의됨

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

## Phase / Step 개요

| Phase | 제목 | 구현 | 의존 |
|-------|------|------|------|
| 1 | DTO + Repository 검색 기반 | `backend` | Phase 0 |
| 2 | Service 조회/생성 | `backend` | Phase 1 |
| 3 | Controller + 인증/validation | `backend` | Phase 2 |
| 4 | Repository 통합 테스트 | `backend` | Phase 1 |
| 5 | Verification | `backend` | Phase 1-4 |

## Phase 1: DTO + Repository 검색 기반

- **구현**: 어드민 API request/response 타입과 동적 검색을 위한 repository 확장.

### 변경 파일

- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/AdminQuestionPoolSearchCondition.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/AdminQuestionPoolResponse.java`
- Create: `backend/src/main/java/com/rehearse/api/domain/question/dto/CreateQuestionPoolRequest.java`
- Modify: `backend/src/main/java/com/rehearse/api/domain/question/repository/QuestionPoolRepository.java`

### 핵심 로직

- `AdminQuestionPoolSearchCondition`:
  - fields: `cacheKey`, `category`, `Boolean isActive`, `keyword`
- `AdminQuestionPoolResponse.from(QuestionPool)`:
  - entity field를 API field로 그대로 매핑
- `CreateQuestionPoolRequest`:
  - `@NotBlank cacheKey`, `@Size(max = 255)`
  - `@NotBlank content`
  - `@Size(max = 100) category`
- `QuestionPoolRepository`:
  - `extends JpaRepository<QuestionPool, Long>, JpaSpecificationExecutor<QuestionPool>`

### Verification

- 컴파일 단계에서 DTO accessor와 repository 상속 확인.

## Phase 2: Service 조회/생성

- **구현**: `AdminQuestionPoolService`가 검색 specification과 생성 트랜잭션을 담당.

### 변경 파일

- Create: `backend/src/main/java/com/rehearse/api/domain/question/service/AdminQuestionPoolService.java`
- Create: `backend/src/test/java/com/rehearse/api/domain/question/service/AdminQuestionPoolServiceTest.java`

### 핵심 로직

- `search(condition, pageable)`:
  - `PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(DESC, "createdAt").and(Sort.by(DESC, "id")))`
  - specification:
    - blank 문자열은 조건에서 제외
    - `cacheKey`, `category`는 case-insensitive partial match
    - `isActive`는 exact
    - `keyword`는 `content` 또는 `bestAnswer` partial match
- `create(request)`:
  - `QuestionPool.create(request.cacheKey(), request.content(), request.ttsContent(), request.category(), request.bestAnswer())`
  - `questionPoolRepository.save(pool)`
  - `AdminQuestionPoolResponse.from(saved)`

### Verification

- `./gradlew test --tests "com.rehearse.api.domain.question.service.AdminQuestionPoolServiceTest"`
- 검증 케이스:
  - 생성 시 repository save 호출
  - 검색 시 Page 응답을 response DTO로 매핑

## Phase 3: Controller + 인증/validation

- **구현**: `/api/v1/admin/question-pools` GET/POST endpoint 추가.

### 변경 파일

- Create: `backend/src/main/java/com/rehearse/api/domain/question/controller/AdminQuestionPoolController.java`
- Create: `backend/src/test/java/com/rehearse/api/domain/question/controller/AdminQuestionPoolControllerTest.java`

### 핵심 로직

- Controller:
  - `@RequestMapping("/api/v1/admin/question-pools")`
  - `@Value("${app.admin.password}")`
  - private `validateAdminPassword(String password)`에서 `AdminErrorCode.INVALID_PASSWORD` 사용
- GET:
  - header optional read
  - params: `page`, `size`, `cacheKey`, `category`, `isActive`, `keyword`
  - size clamp: `Math.min(Math.max(size, 1), 100)`
  - return `ApiResponse.ok(service.search(condition, PageRequest.of(page, safeSize)))`
- POST:
  - `@Valid @RequestBody CreateQuestionPoolRequest`
  - return `ApiResponse.ok(service.create(request))`

### Verification

- `./gradlew test --tests "com.rehearse.api.domain.question.controller.AdminQuestionPoolControllerTest"`
- 검증 케이스:
  - GET header 없음/오류 401
  - GET 성공 200
  - GET size clamp
  - GET filter param condition 전달
  - POST validation 400
  - POST 성공 응답 field 확인

## Phase 4: Repository 통합 테스트

- **구현**: MySQL Testcontainers 기반으로 어드민 필터 동작 검증.

### 변경 파일

- Create: `backend/src/test/java/com/rehearse/api/domain/question/repository/QuestionPoolRepositoryTest.java`

### 핵심 로직

- `@DataJpaTest` + `AbstractMySqlContainerTest` 패턴 사용.
- 샘플 `QuestionPool` 저장:
  - active CS 운영체제 row
  - active CS 네트워크 row
  - inactive behavioral row
- 검증:
  - `cacheKey` partial match
  - `category` partial match
  - `isActive=false`
  - `keyword`가 content 또는 bestAnswer에 match

### Verification

- `./gradlew test --tests "com.rehearse.api.domain.question.repository.QuestionPoolRepositoryTest"`

## Phase 5: Backend Verification

- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.controller.AdminQuestionPoolControllerTest"`
- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.service.AdminQuestionPoolServiceTest"`
- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.repository.QuestionPoolRepositoryTest"`
- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.*"`

## FE 와 통합 시점

- API contract 변경이 필요하면 구현 전에 `tech-spec.md` 갱신 후 사용자 재승인.
- BE 구현 완료 후 FE는 실제 endpoint로 연결한다.

## 리뷰 게이트 (MANDATORY)

- [ ] 구현 완료 직후 backend review 수행
- [ ] 컨벤션 위반 0건 (`backend/AGENTS.md` + backend rules)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치
