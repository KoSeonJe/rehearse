# Plan 08: QuestionDistribution VO + QuestionGenerationTransactionHandler 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 2 시작. QuestionDistribution은 질문 분배 수학 로직(나머지 처리), TransactionHandler는 질문 생성의 트랜잭션 경계를 검증.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: Plan 09 (QuestionGenerationService가 두 클래스에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../interview/vo/QuestionDistributionTest.java` | 신규 생성 (~10 tests) |
| `src/test/.../interview/service/QuestionGenerationTransactionHandlerTest.java` | 신규 생성 (~10 tests) |

## 상세

### QuestionDistributionTest (순수 Unit, Mock 없음)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `create_evenSplit_distributesEqually` | 6/2 타입 | 3, 3 |
| 2 | `create_unevenSplit_distributesRemainder` | 7/2 타입 | 4, 3 |
| 3 | `create_singleType_getsAll` | 1 타입 | 전체 할당 |
| 4 | `create_manyTypes_distributesAll` | 7/3 타입 | 3, 2, 2 |
| 5 | `getCacheableTypes_filtersCorrectly` | cacheable/fresh 혼합 | cacheable만 반환 |
| 6 | `getFreshTypes_filtersCorrectly` | cacheable/fresh 혼합 | fresh만 반환 |
| 7 | `sumOfAllTypes_equalsTotalCount` | 임의 분배 | cacheable + fresh = total |
| 8 | `create_remainderGoesToFirstTypes` | 나머지 분배 | 첫 N개 타입에 +1 |

### QuestionGenerationTransactionHandlerTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `InterviewRepository`, `QuestionSetRepository`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `startGeneration_success_callsStartQuestionGeneration` | 정상 | 상태 전이 호출 |
| 2 | `startGeneration_notFound_throwsBusinessException` | 면접 없음 | NOT_FOUND 예외 |
| 3 | `saveResults_success_assignsInterviewAndSaves` | 정상 | interview 할당 + saveAll |
| 4 | `saveResults_notFound_throwsBusinessException` | 면접 없음 | NOT_FOUND 예외 |
| 5 | `saveResults_multipleQuestionSets_savesAll` | 여러 QuestionSet | 전부 저장 |
| 6 | `saveResults_callsCompleteQuestionGeneration` | 정상 | 완료 상태 전이 |
| 7 | `failGeneration_found_callsFailQuestionGeneration` | 면접 존재 | 실패 상태 전이 + reason |
| 8 | `failGeneration_notFound_doesNotThrow` | 면접 없음 | 예외 없이 무시 (ifPresent) |

## 담당 에이전트

- Implement: `test-engineer` — 2개 파일 작성
- Review: `qa` — 나머지 분배 수학 검증, 트랜잭션 경계

## 검증

- [ ] `./gradlew test --tests "QuestionDistributionTest"` 통과
- [ ] `./gradlew test --tests "QuestionGenerationTransactionHandlerTest"` 통과
- [ ] QuestionDistributionTest에 Mock 0개
- [ ] `progress.md` 상태 업데이트 (Plan 08 → Completed)
