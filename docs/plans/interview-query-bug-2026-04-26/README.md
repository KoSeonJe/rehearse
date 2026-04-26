# Interviews 조회 버그 분석 & 수정 플랜

> Status: Completed (2026-04-26)
> Branch: `fix/interview-query-response-leak`

> **Note (2026-04-26 리뷰 후)**: 본 PR 은 권한 체크(IDOR), 페이지네이션 tie-break, 단건 publicId 경로의 EntityGraph 보강을 처리한다. user 가 보고한 "interviewTypes 응답 오염" 증상은 H3(LazyInit) 으로 부분 설명되나 직접 재현은 미완. 추가 진단(3 endpoint raw JSON 비교, DB row 직조회, Hibernate SQL 로그)이 별도 PR 로 필요.

## Why

User 보고: 면접 생성 시 `interviewTypes = [CS_FUNDAMENTAL]` 만 선택했는데, 조회 응답에는 `EXPERIENCE`, `LANGUAGE_FRAMEWORK` 까지 포함되어 노출됨. DB `interview_interview_types` 테이블에는 `CS_FUNDAMENTAL` 한 행만 저장 → **저장 단계 정상, 조회/직렬화 단계에서 다른 enum 값이 응답에 포함됨**.

본 플랜은 메인 이슈를 해결하면서, 코드 리뷰 중 발견된 인접 버그 3종을 함께 정리한다.

1. **메인**: interviewTypes 응답 오염
2. `getInterviewByPublicId` 권한 체크 0 (User confirm: "본인만 조회해야 함" → IDOR)
3. `findAllByUserId` 페이지네이션 tie-break 누락 (`createdAt DESC` 단일 정렬 → 동일 ms 면접 페이지 경계서 중복/누락)
4. FE `useUpdateInterviewStatus` / `useRetryQuestions` 가 목록·stats 캐시 invalidate 누락

## Goal

- 메인 이슈 재현 + 근본 원인 확정 + 수정 + 회귀 테스트
- 인접 버그 3종 동시 수정 (BE 3개 한 PR, FE 1개 별 PR — BE/FE 분리 룰)

---

## 증상 / 경로

- **재현 경로 후보**:
  - `GET /api/v1/interviews/{id}` → `InterviewQueryService.getInterview` → `InterviewFinder.findByIdAndValidateOwner` → `findByIdWithElementCollections` (EntityGraph 적용)
  - `GET /api/v1/interviews/by-public-id/{publicId}` → `InterviewQueryService.getInterviewByPublicId` → `findByPublicId` (**EntityGraph 미적용** → lazy 로드)
  - `GET /api/v1/interviews` (목록) → `InterviewService.getInterviews` → `findAllByUserId` (EntityGraph 미적용)

- **FE 표시 경로**: `interview.interviewTypes` 그대로 map (interview-card / interview-table / interview-feedback-page / interview-ready-page). FE 가 enum 전체로 보강하는 코드 없음 → **응답 오염은 BE 단계**.

---

## 가설 (우선순위 순)

### H1. EntityGraph 두 ElementCollection 동시 fetch 의 cartesian product
`InterviewRepository:18` `@EntityGraph(attributePaths = {"interviewTypes", "csSubTopics"})` 가 두 ElementCollection 을 한 SELECT 로 join → cartesian product. Set 이라 dedupe 되지만, 특정 Hibernate 버전에서 hydrate 오류 사례 보고. 단, 다른 면접의 type 이 섞일 시나리오는 직접 설명 안 됨.

### H2. 영속성 컨텍스트 collection merge 이슈
같은 트랜잭션에서 여러 Interview 를 로드하는 흐름 중 ElementCollection Set 인스턴스가 합쳐지거나 stale snapshot 노출 가능. `Interview.getInterviewTypes()` 가 `Collections.unmodifiableSet(interviewTypes)` 반환 → 내부 필드 직접 노출.

### H3. 단건/공개 조회 경로 EntityGraph 누락 + 비동기 트랜잭션 race
`findByPublicId` 는 derived query, lazy 로드. `generateFollowUp` 가 `CompletableFuture.supplyAsync(..., vtExecutor)` 사용 → 트랜잭션 경계에서 collection 오염 가능.

### H4. Question 생성 / 재시도 시 interviewTypes 가 덮어써짐 (가능성 낮)
코드상 interviewTypes 추가/수정 메서드 없음. Flyway 백필 SQL 만 확인하면 됨.

### H5. CreateInterviewRequest 직렬화 단계 default 추가 (가능성 낮)
코드상 default 채움 없음.

---

## 재현 / 진단 결과 (2026-04-26 확정)

### 가설 확정

`@DataJpaTest` failing test 4개 작성 후 실행 결과:

- **H3 확정**: `findByPublicId`는 EntityGraph 없는 derived query → `entityManager.clear()` 이후 `LazyInitializationException`. 실 서비스에서도 `@Transactional(readOnly=true)` 경계 밖(또는 session 닫힌 후) 접근 시 컬렉션 로드 실패.
- **findAllByUserId 경로도 동일 문제**: EntityGraph 없이 `@BatchSize`만으로는 세션이 유효한 트랜잭션 안에서만 동작. 실제 서비스(`InterviewService.getInterviews`)는 `@Transactional(readOnly=true)` 내에서 접근하므로 `@BatchSize`가 정상 동작하지만, 경계 주의 필요.
- **H2 (영속성 컨텍스트 오염) 미발생**: 동일 영속성 컨텍스트에 다른 타입 조합 Interview 먼저 로드 후 CS_FUNDAMENTAL 단일 타입 Interview 조회 시 오염 없음 — 테스트 통과.
- **H1 (EntityGraph cartesian product) 미발생**: `findByIdWithElementCollections` 경로는 정상 동작.

### 적용한 수정

1. `InterviewRepository.findByPublicId` — `@EntityGraph(attributePaths = {"interviewTypes", "csSubTopics"})` 추가 (H3 수정)
2. `InterviewRepository.findAllByUserId` — `ORDER BY i.createdAt DESC, i.id DESC` (Task B tie-break)
3. `InterviewQueryService.getInterviewByPublicId(publicId, userId)` — 시그니처에 `userId` 추가 + `interview.validateOwner(userId)` 호출 (Task A IDOR)
4. `InterviewController.getInterviewByPublicId` — `@AuthenticationPrincipal Long userId` 추가

### 추가된 테스트

- `InterviewRepositoryTest`: 6개 → 11개 (+5)
  - `findByIdWithElementCollections_singleType_returnsExactMatch`
  - `findByPublicId_singleType_returnsExactMatch`
  - `findByIdWithElementCollections_persistenceContextPollutionScenario`
  - `findAllByUserId_singleType_returnsExactTypes`
  - `findAllByUserId_sameCreatedAt_noDuplicateWithTieBreak`
- `InterviewQueryServiceTest`: 2개 → 4개 (+2)
  - `getInterviewByPublicId_owner_success`
  - `getInterviewByPublicId_otherUser_forbidden`
- `InterviewControllerTest`: 기존 + `GetInterviewByPublicId` Nested (+3)
  - `getInterviewByPublicId_owner_success`
  - `getInterviewByPublicId_otherUser_forbidden`
  - `getInterviewByPublicId_unauthenticated_unauthorized`

### 전체 테스트 결과

852 tests, 0 failed, 1 skipped — BUILD SUCCESSFUL

---

## 재현 / 진단 단계 (Task 1 구현 전 필수)

1. **응답 페이로드 직접 확인** (3개 endpoint 비교) → 오염 위치 좁히기
2. **DB 직조회**: `SELECT * FROM interview_interview_types WHERE interview_id = ?`
3. **Hibernate SQL 로깅**: `application-local.yml` 에 `org.hibernate.SQL=DEBUG` 한시 활성
4. **Failing test 작성**: `@DataJpaTest` 로 단일 type 면접 만들고 응답 verify

---

## Tasks

### Task 1 — interviewTypes 응답 오염 수정 (메인)
- **Implement**: `backend` — 재현 결과에 따라
  - H1/H3 시: EntityGraph 분리 또는 제거 후 `@BatchSize` 활용
  - H2 시: getter defensive copy (`new HashSet<>(interviewTypes)`)
- **Files**: `repository/InterviewRepository.java:18`, `entity/Interview.java:116`, `service/InterviewQueryService.java`, `service/InterviewService.java:118,155`
- **Review**: `architect-reviewer` (트랜잭션·fetch 전략), `code-reviewer`

### Task 2 — publicId 권한 체크
- **Implement**: `backend`
  - `InterviewController:72-76` `getInterviewByPublicId` 에 `@AuthenticationPrincipal Long userId` 추가
  - `InterviewQueryService:29-33` 에서 `interview.validateOwner(userId)`
- **Review**: `code-reviewer` (auth 회귀)

### Task 3 — 페이지네이션 tie-break
- **Implement**: `backend` — `InterviewRepository:26` `ORDER BY i.createdAt DESC, i.id DESC`
- **Review**: `code-reviewer`

### Task 4 — FE 캐시 무효화 보강 (별 PR — BE 머지 후)
- **Implement**: `frontend`
  - `frontend/src/hooks/use-interviews.ts:121-125` `useUpdateInterviewStatus.onSuccess` 에 `['interviews', 'list']`, `['interviews', 'stats']` invalidate 추가
  - `use-interviews.ts:137-141` `useRetryQuestions.onSuccess` 동일
- **Review**: `code-reviewer`

### Task 5 [parallel] — 회귀 테스트
- **Implement**: `test-engineer`
  - BE: `@DataJpaTest` 단일 type 응답 일치성 회귀
  - BE: `getInterviewByPublicId` 권한 테스트 (`@WebMvcTest`)
  - BE: tie-break 검증 (createdAt 동일 + size=1)
- **Review**: `qa`

---

## 핵심 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/controller/InterviewController.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewQueryService.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewFinder.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/repository/InterviewRepository.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/InterviewResponse.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/InterviewListResponse.java`
- `frontend/src/hooks/use-interviews.ts`

---

## 검증

- 진단 단계 1~3 dev/local 선행
- `./gradlew test --tests "com.rehearse.api.domain.interview.*"`
- 통합 (수동):
  1. 새 면접 (interviewTypes=`[CS_FUNDAMENTAL]`) 생성
  2. 3 endpoint 응답 `interviewTypes` 모두 `["CS_FUNDAMENTAL"]` 확인
  3. 타 유저로 publicId 호출 → 403
  4. createdAt 동일 면접 2개 + size=1 페이징 중복/누락 0
  5. status update 후 대시보드 즉시 반영
- FE: `npm run lint && npm run test && npm run build`
- **Post-impl review (필수)**: Task 1·2 머지 전 `architect-reviewer` + `code-reviewer` 병렬 실행

---

## Follow-up (별 PR)

1. **`Interview.validateOwner(Long)` null-safety 결함** — `this.userId == null` 일 때 모든 호출을 통과시킴. 현재는 `SecurityConfig` 의 인증 필터가 1차 가드라 실 익스플로잇 경로 X. 별도 PR 로 `userId == null || !userId.equals(this.userId)` 형태로 강화 필요.
2. **`InterviewRepository.findAllByUserId` EntityGraph 미적용은 의도적 결정** — 페이지 결과 × ElementCollection cartesian 폭발 회피 목적. `@BatchSize(100)` + `@Transactional(readOnly=true)` 안에서만 동작 보장. 트랜잭션 경계 외에서 직렬화하는 코드 추가 시 LazyInit 재발 가능 — ADR 또는 `docs/architecture/` 노트로 1줄 기록 권장.
3. **메인 이슈 진짜 root cause 진단** — dev 환경에서 user 보고 면접의 publicId 로 (a) 3 endpoint raw JSON 응답 dump, (b) `interview_interview_types` SELECT 직조회, (c) `org.hibernate.SQL=DEBUG` + `org.hibernate.orm.jdbc.bind=TRACE` 로그 캡처. evidence 확보 후 별도 fix PR.
4. **publicId 응답 코드 정책 명시** — 존재하지만 권한 없음 = 403 (현재). UUID 추측 방지 차원에서 404 통합 검토 가능. UUID v4 random성 충분 → 현재 정책 유지 권장. 의사결정 기록만 본 README 또는 보안 설계 문서에 1줄.
