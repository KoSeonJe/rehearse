# Plan 09: QuestionGenerationService + EventHandler + InterviewFinder 테스트 `[blocking]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 2 완성. 질문 생성 파이프라인의 핵심 오케스트레이터(`QuestionGenerationService`), 비동기 이벤트 핸들러, Finder 패턴 검증. 이 파이프라인은 면접 생성의 핵심 플로우이며, 장애 시 사용자가 질문을 받지 못하는 치명적 결과를 초래한다.

## 의존성

- 선행: Plan 07 (Provider 테스트), Plan 08 (TransactionHandler + Distribution 테스트)
- 후행: 없음 (Phase 2 최종)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../interview/service/QuestionGenerationServiceTest.java` | 신규 생성 (~15 tests) |
| `src/test/.../interview/service/QuestionGenerationEventHandlerTest.java` | 신규 생성 (~7 tests) |
| `src/test/.../interview/service/InterviewFinderTest.java` | 신규 생성 (~8 tests) |

## 상세

### QuestionGenerationServiceTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `QuestionGenerationTransactionHandler`, `CacheableQuestionProvider`, `FreshQuestionProvider`, `QuestionCountCalculator`
주의: `virtualExecutor`는 직접 실행하는 동기 Executor(`Runnable::run`)로 대체하여 테스트

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `generateQuestions_cacheableAndFresh_createsAllQuestionSets` | 혼합 타입 | 양쪽 Provider 호출 |
| 2 | `generateQuestions_cacheableOnly_skipsFreshCall` | cacheable만 | FreshProvider 미호출 |
| 3 | `generateQuestions_freshOnly_skipsCacheableCall` | fresh만 | CacheableProvider 미호출 |
| 4 | `generateQuestions_callsStartGeneration_beforeProcessing` | 순서 검증 | startGeneration 먼저 호출 |
| 5 | `generateQuestions_callsSaveResults_afterProcessing` | 순서 검증 | saveResults 마지막 호출 |
| 6 | `generateQuestions_futureTimeout_throwsException` | 60초 초과 | 예외 발생 |
| 7 | `generateQuestions_exceptionInProvider_callsFailGeneration` | Provider 예외 | failGeneration 호출 |
| 8 | `generateQuestions_reordersQuestions_sequentially` | 다수 QuestionSet | orderIndex 순차 재배정 |
| 9 | `generateQuestions_defaultTechStack_whenNull` | techStack null | Position 기반 기본값 |
| 10 | `generateQuestions_createsCorrectQuestionSetStructure` | 정상 | QuestionSet에 Question 포함 |
| 11 | `generateQuestions_behavioralType_setsBehavioralPerspective` | BEHAVIORAL 타입 | FeedbackPerspective.BEHAVIORAL |
| 12 | `generateQuestions_resumeType_setsResumeBasedPerspective` | RESUME_BASED 타입 | FeedbackPerspective.RESUME_BASED |
| 13 | `generateQuestions_technicalType_setsTechnicalPerspective` | TECHNICAL 타입 | FeedbackPerspective.TECHNICAL |
| 14 | `generateQuestions_parsesReferenceType` | 유효한 referenceType | enum 파싱 정확 |
| 15 | `generateQuestions_categoryResolution_forFreshQuestions` | fresh 질문 | 카테고리 올바르게 매핑 |

### QuestionGenerationEventHandlerTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `QuestionGenerationService`, `QuestionGenerationTransactionHandler`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `handle_success_delegatesToService` | 정상 이벤트 | generateQuestions 호출 |
| 2 | `handle_success_passesAllEventParams` | 파라미터 전달 | 모든 필드 정확 전달 |
| 3 | `handle_exception_callsFailGeneration` | 서비스 예외 | failGeneration 호출 |
| 4 | `handle_exceptionWithCause_extractsCauseMessage` | cause 있는 예외 | cause.getMessage() 사용 |
| 5 | `handle_exceptionWithoutCause_usesDirectMessage` | cause 없는 예외 | e.getMessage() 사용 |
| 6 | `handle_nullMessage_usesDefaultErrorMessage` | message null | "알 수 없는 오류" |
| 7 | `handle_failGenerationAlsoFails_logsAndSwallows` | 이중 장애 | 로그만, 추가 예외 없음 |

### InterviewFinderTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `InterviewRepository`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `findById_exists_returnsInterview` | 존재 | Interview 반환 |
| 2 | `findById_notFound_throwsBusinessException` | 미존재 | NOT_FOUND 예외 |
| 3 | `findByIdAndValidateOwner_validOwner_returnsInterview` | 소유자 일치 | 정상 반환 |
| 4 | `findByIdAndValidateOwner_notFound_throwsException` | 미존재 | NOT_FOUND 예외 |
| 5 | `findByIdAndValidateOwner_invalidOwner_throwsException` | 소유자 불일치 | 권한 예외 |
| 6 | `findByPublicId_exists_returnsInterview` | 존재 | Interview 반환 |
| 7 | `findByPublicId_notFound_throwsBusinessException` | 미존재 | NOT_FOUND 예외 |
| 8 | `findById_usesElementCollectionQuery` | 쿼리 확인 | findByIdWithElementCollections 호출 |

## 담당 에이전트

- Implement: `test-engineer` — 3개 파일 작성
- Review: `architect-reviewer` — Mock 경계가 아키텍처 레이어와 일치하는지 검증

## 검증

- [ ] `./gradlew test --tests "QuestionGenerationServiceTest"` 통과
- [ ] `./gradlew test --tests "QuestionGenerationEventHandlerTest"` 통과
- [ ] `./gradlew test --tests "InterviewFinderTest"` 통과
- [ ] QuestionGenerationServiceTest에서 `virtualExecutor` 대신 동기 Executor 사용
- [ ] `progress.md` 상태 업데이트 (Plan 09 → Completed)
