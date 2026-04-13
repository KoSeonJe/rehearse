# Plan 07: CacheableQuestionProvider + FreshQuestionProvider 테스트 `[blocking]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 1 완성. CacheableQuestionProvider의 캐시 히트/미스 분기, stampede 보호, 카테고리 필터링 검증. FreshQuestionProvider의 AI 호출 + 결과 트렁케이션 검증.

## 의존성

- 선행: Plan 06 (QuestionPoolService 테스트 완료)
- 후행: Plan 09 (QuestionGenerationService가 두 Provider에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../questionpool/service/CacheableQuestionProviderTest.java` | 신규 생성 (~12 tests) |
| `src/test/.../questionpool/service/FreshQuestionProviderTest.java` | 신규 생성 (~6 tests) |

## 상세

### CacheableQuestionProviderTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `QuestionPoolService`, `QuestionRepository`, `AiClient`, `QuestionGenerationLock`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `provide_poolSufficient_returnsCachedQuestions` | 캐시 히트 | Pool에서 즉시 반환, AI 미호출 |
| 2 | `provide_poolInsufficient_callsAiAndReturnsPool` | 캐시 미스 | AI 호출 후 Pool 저장 |
| 3 | `provide_stampedeLock_rechecksAfterAcquire` | Lock 획득 후 재확인 | 이중 확인 통과 시 AI 미호출 |
| 4 | `provide_aiFailure_propagatesException` | AI 에러 | 예외 전파, Lock 해제 |
| 5 | `provide_csFundamental_appliesCategoryFilter` | CS_FUNDAMENTAL 타입 | CsSubTopic → 카테고리 변환 |
| 6 | `provide_nonCsFundamental_noCategoryFilter` | 일반 타입 | 카테고리 필터 null |
| 7 | `provide_nullUserId_skipsUserFiltering` | userId null | usedPoolIds 조회 스킵 |
| 8 | `provide_withUserId_excludesUsedQuestions` | userId 존재 | 기사용 질문 제외 |
| 9 | `provide_lockReleasedOnException` | AI 예외 발생 | finally에서 Lock release 호출 |
| 10 | `provide_lockReleasedOnSuccess` | 정상 처리 | finally에서 Lock release 호출 |
| 11 | `provide_filteredGeneration_appliesCategoryFilter` | AI 결과에 필터 적용 | 카테고리 일치 항목만 반환 |
| 12 | `provide_emptyCsSubTopics_usesAllCategories` | 빈 csSubTopics | 필터 없이 전체 사용 |

### FreshQuestionProviderTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `AiClient`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `provide_normalGeneration_returnsAll` | 정확한 수량 생성 | 전체 반환 |
| 2 | `provide_excessGeneration_truncatesToRequired` | 초과 생성 | required 수만큼 잘라서 반환 |
| 3 | `provide_underGeneration_returnsAll` | 부족 생성 | 있는 만큼 반환 |
| 4 | `provide_nullCsSubTopics_convertsToEmptySet` | csSubTopics null | 빈 Set으로 변환 |
| 5 | `provide_nullResumeText_passesNull` | resumeText null | null 그대로 전달 |
| 6 | `provide_emptyTypes_passesEmptySet` | types 빈 Set | 빈 Set 전달 |

## 담당 에이전트

- Implement: `test-engineer` — 2개 파일 작성
- Review: `qa` — stampede 보호 시나리오 정확성

## 검증

- [ ] `./gradlew test --tests "CacheableQuestionProviderTest"` 통과
- [ ] `./gradlew test --tests "FreshQuestionProviderTest"` 통과
- [ ] Lock acquire/release `then().should()` 검증 포함
- [ ] `progress.md` 상태 업데이트 (Plan 07 → Completed)
