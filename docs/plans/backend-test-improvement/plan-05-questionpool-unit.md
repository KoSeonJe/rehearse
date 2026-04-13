# Plan 05: QuestionPool Entity + KeywordMatcher + QuestionGenerationLock 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

QuestionPool 도메인 테스트 0%. 가장 기초적인 Entity/유틸/동시성 클래스부터 시작하여 Phase 1 토대 구축. 이 클래스들은 외부 의존성이 없어 순수 Unit 테스트로 빠르게 검증 가능.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: Plan 06 (QuestionPoolService가 이 클래스들에 의존)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../questionpool/entity/QuestionPoolTest.java` | 신규 생성 (~7 tests) |
| `src/test/.../questionpool/service/KeywordMatcherTest.java` | 신규 생성 (~9 tests) |
| `src/test/.../questionpool/service/QuestionGenerationLockTest.java` | 신규 생성 (~7 tests) |

## 상세

### QuestionPoolTest (순수 Unit, Mock 없음)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `create_validParams_returnsActivePool` | 정상 파라미터 | isActive=true, 필드 매핑 정확 |
| 2 | `create_nullCacheKey_throwsException` | cacheKey null | IllegalArgumentException |
| 3 | `create_blankCacheKey_throwsException` | cacheKey 빈 문자열 | IllegalArgumentException |
| 4 | `create_nullContent_throwsException` | content null | IllegalArgumentException |
| 5 | `create_blankContent_throwsException` | content 빈 문자열 | IllegalArgumentException |
| 6 | `create_nullableFields_acceptsNull` | ttsContent, category 등 null | 정상 생성 |
| 7 | `deactivate_setsIsActiveFalse` | 활성 상태에서 비활성화 | isActive=false |

### KeywordMatcherTest (순수 Unit, Mock 없음)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `matches_nullKeywords_returnsFalse` | keywords null | false |
| 2 | `matches_nullAnswer_returnsFalse` | answer null | false |
| 3 | `matches_emptyKeywords_returnsFalse` | 빈 리스트 | false |
| 4 | `matches_emptyAnswer_returnsFalse` | 빈 문자열 | false |
| 5 | `matches_belowThreshold_returnsFalse` | 매칭 수 < threshold | false |
| 6 | `matches_meetsThreshold_returnsTrue` | 매칭 수 >= threshold | true |
| 7 | `matches_caseInsensitive_returnsTrue` | 대소문자 무시 매칭 | true |
| 8 | `matches_substringMatch_counted` | 부분 문자열도 매칭 | 카운트 반영 |
| 9 | `matches_thresholdExceedsSize_returnsFalse` | threshold > keywords.size() | false |

### QuestionGenerationLockTest (동시성 테스트 포함)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `acquire_newKey_returnsLockedLock` | 새 키 | locked 상태의 ReentrantLock |
| 2 | `acquire_sameKey_returnsSameLock` | 같은 키 2회 | 동일 Lock 인스턴스 |
| 3 | `acquire_differentKeys_returnsDifferentLocks` | 다른 키 | 다른 Lock 인스턴스 |
| 4 | `release_unlocksLock` | release 호출 | isLocked=false |
| 5 | `acquire_concurrentSameKey_onlyOneProceeds` | 2 스레드 동시 acquire | 하나만 진행, CountDownLatch |
| 6 | `acquire_afterRelease_canReacquire` | release 후 재획득 | 정상 획득 |
| 7 | `release_alreadyUnlocked_noException` | 이미 해제된 lock | IllegalMonitorStateException 또는 무시 |

## 담당 에이전트

- Implement: `test-engineer` — 3개 파일 작성
- Review: `qa` — 엣지 케이스 누락 확인, 동시성 테스트 안정성

## 검증

- [ ] `./gradlew test --tests "QuestionPoolTest"` 통과
- [ ] `./gradlew test --tests "KeywordMatcherTest"` 통과
- [ ] `./gradlew test --tests "QuestionGenerationLockTest"` 통과
- [ ] QuestionGenerationLockTest에 CountDownLatch 기반 동시성 테스트 1개 이상
- [ ] 3개 파일 모두 Mock 0개 (순수 Java)
- [ ] `progress.md` 상태 업데이트 (Plan 05 → Completed)
