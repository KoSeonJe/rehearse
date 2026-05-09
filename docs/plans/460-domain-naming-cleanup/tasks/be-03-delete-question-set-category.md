# BE Task 03 — `QuestionSetCategory` 삭제 + `InterviewType` 통합

## 목적

`QuestionSetCategory` (12개 enum) 삭제. `QuestionSet.category` 필드 타입을 `InterviewType` 으로 교체. 동일 12값 두 enum 양쪽 활성 상태 해소. **DDL/DML 0** (값 100% 일치 + `@Enumerated(STRING)` 유지).

## 변경 파일

### 삭제
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionSetCategory.java`

### 임포트 / 타입 교체 (main)
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionSet.java`
  - `private QuestionSetCategory category;` → `private InterviewType category;` (`@Enumerated(STRING)` 유지)
- `backend/src/main/java/com/rehearse/api/domain/question/repository/QuestionSetRepository.java`
  - 메서드 파라미터 타입 `QuestionSetCategory` → `InterviewType` 일괄 (전체 메서드 시그니처 확인)
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionSetAssembler.java`
  - 임포트 + 사용 타입 갱신
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionPersister.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricFamily.java` (사용 타입 정리)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java` (사용 타입 정리)
- `backend/src/main/java/com/rehearse/api/domain/question/dto/AnswerResponse.java`
  - **`from(QuestionAnswer, QuestionSetCategory)` → `from(QuestionAnswer, InterviewType)` 시그니처는 Task 7 영역.** 본 task 는 임포트 정리만.
  - **결정**: AnswerResponse.from() 시그니처 변경은 **Task 3 에서 동시 처리** (QuestionSetCategory 삭제 시 호출처 즉시 교체 필요 — 분리 시 컴파일 RED).
  - `InternalQuestionSetService.java:88` 호출처도 `questionSet.getCategory()` 반환 타입 자동 호환 (`InterviewType`).
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` (사용 시 임포트 정리)

### 테스트 갱신
- `backend/src/test/java/com/rehearse/api/global/support/TestFixtures.java` — `QuestionSet` 빌더 / 팩토리 `InterviewType` 사용
- `backend/src/test/java/com/rehearse/api/domain/question/**` (다수)
- `backend/src/test/java/com/rehearse/api/domain/feedback/**` (다수)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/*Test.java`
- `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandlerTest.java`
- `backend/src/test/java/com/rehearse/api/domain/integrity/V40IntegrityPatchTest.java`
- `backend/src/test/java/com/rehearse/api/domain/reviewbookmark/**`

## 핵심 변경 (요지)

- DB 컬럼 = `question_set.category VARCHAR(50)`. 저장 문자열 = `InterviewType.name()` (12값 100% 일치).
- `@Enumerated(EnumType.STRING)` 유지 → 직렬화 호환.
- **CacheStrategy 노출 수용** (handoff 사용자 결정 — YAGNI). `questionSet.getCategory().getCacheStrategy()` 호출 가능 상태로 둠. 발생 시 별도 처리.

## 트레이드오프 재확인

- T2 (RubricCategory rename) **후** 진행 — `RubricFamily` / `RubricLoader` 가 두 enum 동시 변경 영향 → T2 먼저 안정화 후 T3.
- `AnswerResponse.from()` 시그니처 변경 = **본 task 동시 수행** 필수 (분리 시 빌드 RED). Task 7 은 JSON 키 / 필드명 (`rubricCategory`) 변경 전담.

## 테스트

- `./gradlew test --tests "com.rehearse.api.domain.question.*" "com.rehearse.api.domain.feedback.*" "com.rehearse.api.domain.resume.*" "com.rehearse.api.domain.interview.service.FollowUpTransactionHandlerTest"`
- Repository 테스트 = Testcontainers + Flyway 자동 실행 → `question_set.category` 컬럼 호환 자동 검증.

## 완료 기준

- [ ] `grep -rn "QuestionSetCategory" backend/src` = 0 (main + test 모두)
- [ ] `QuestionSet.category : InterviewType` 타입 + `@Enumerated(STRING)` 유지
- [ ] `AnswerResponse.from(QuestionAnswer, InterviewType)` 시그니처
- [ ] `InternalQuestionSetServiceTest`, `QuestionSetServiceTest`, `RubricLoaderTest` GREEN
- [ ] DDL/DML 변경 없음 (Flyway V 신규 파일 0)
- [ ] `./gradlew test` GREEN

## 의존

- 선행: T2 (RubricCategory rename — RubricFamily / RubricLoader 안정화 후).
- 후행: T7 (AnswerResponse JSON 키 — `from()` 시그니처는 본 task 에서 변경 완료).

## 커밋

```
refactor(BE): QuestionSetCategory 삭제 + QuestionSet.category 타입을 InterviewType 으로 통합
```

## 위험 / 메모

- **CacheStrategy 노출**: 사용자 결정 = 수용 (YAGNI). 호출처 0건 → 발생 시 별도 처리 (handoff).
- **마이그 검증**: Testcontainers 부팅 시 Flyway 자동 적용 → `question_set` 테이블 + 기존 row 호환 자동 보장. 별도 DDL 추가 0.
