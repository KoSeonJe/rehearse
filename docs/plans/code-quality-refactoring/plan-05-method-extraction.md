# Plan 05: 장문 메서드 분리 및 트랜잭션 컨벤션 통일

> 상태: Draft
> 작성일: 2026-04-13

## Why

30줄 이상의 서비스 메서드가 5개 존재하여 가독성이 떨어진다. QuestionPoolService는 동일 로직의 3중 오버로딩이 있어 유지보수 비용이 높다. ReviewBookmarkService는 `@Transactional` 컨벤션을 따르지 않는다.

## 전제조건: 테스트 확인

| 대상 | 테스트 | 상태 |
|------|--------|------|
| QuestionPoolService | QuestionPoolServiceTest | ✅ |
| QuestionGenerationService | QuestionGenerationServiceTest | ✅ |
| ReviewBookmarkService | ReviewBookmarkServiceTest | ✅ |

모든 대상에 테스트 존재. 즉시 리팩토링 가능.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `domain/interview/generation/pool/service/QuestionPoolService.java` | PoolSelectionCriteria record 생성, 오버로딩 6개 → 2개 통합 |
| `domain/interview/generation/pool/service/CacheableQuestionProvider.java` | PoolSelectionCriteria 사용으로 호출부 변경 |
| `domain/interview/generation/pool/service/FreshQuestionProvider.java` | PoolSelectionCriteria 사용으로 호출부 변경 |
| `domain/interview/generation/service/QuestionGenerationService.java` | generateQuestions()에서 calculateDistribution(), joinParallelResults() 추출 |
| `domain/reviewbookmark/service/ReviewBookmarkService.java` | @Transactional(readOnly=true) 클래스 기본값, write 메서드에 @Transactional |

## 상세

### Task 5.1: QuestionPoolService 오버로딩 통합 [parallel]

```java
// 신규 record
public record PoolSelectionCriteria(
    String cacheKey,
    int requiredCount,
    List<String> categoryFilter,  // nullable
    Set<Long> usedPoolIds         // nullable
) {}

// 3개 → 1개
public boolean isPoolSufficient(PoolSelectionCriteria criteria) { ... }
public List<QuestionPool> selectFromPool(PoolSelectionCriteria criteria) { ... }
```

### Task 5.2: selectWithCategoryDistribution() 메서드 분리 [parallel]

42줄 메서드에서 `groupAndShuffleByCategory()` 추출 → round-robin 로직만 ~15줄 유지

### Task 5.3: ReviewBookmarkService @Transactional 수정 [parallel]

```java
// Before
@Transactional
public class ReviewBookmarkService { ... }

// After
@Transactional(readOnly = true)
public class ReviewBookmarkService {
    @Transactional
    public ReviewBookmarkResponse create(...) { ... }
    @Transactional
    public void delete(...) { ... }
    @Transactional
    public ReviewBookmarkResponse updateStatus(...) { ... }
}
```

### Task 5.4: QuestionGenerationService 장문 메서드 정리 [parallel]

generateQuestions() (49줄)에서 추출:
- `calculateDistribution()` — 유형별 질문 수 배분 (58~66줄)
- `joinParallelResults()` — CompletableFuture join + 에러 처리 (80~89줄)

## 담당 에이전트

- Implement: `backend` — 메서드 추출, 오버로딩 통합, 어노테이션 수정
- Implement (테스트): `test-engineer` — QuestionPoolServiceTest PoolSelectionCriteria 사용 변경
- Review: `architect-reviewer` — API 설계 (PoolSelectionCriteria)
- Review: `code-reviewer` — 동작 보존, 컨벤션 준수

## 검증

- `./gradlew test` — 전체 통과
- 30줄 초과 서비스 메서드: 0개 확인
- QuestionPoolService public 메서드: isPoolSufficient 1개 + selectFromPool 1개 확인
- `progress.md` 상태 업데이트 (Task 5 → Completed)
