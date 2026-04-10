# Plan 01: 사용자별 중복 방지 로직 구현

> 상태: Draft
> 작성일: 2026-04-10

## Why

동일 사용자가 반복 면접 시 이전에 받은 질문을 다시 받을 수 있다. 현재 시스템은 `POOL_SUFFICIENCY_MULTIPLIER = 3` (3배수 휴리스틱)으로 다양성을 보장하려 하지만, 사용자별 추적이 없어 실제 중복이 발생한다. 기존 테이블 관계로 기출을 추적하고, 미사용 질문이 부족해지면 AI 생성으로 풀을 자동 확장한다.

## 현재 구현 분석

### 전체 호출 체인 (userId가 빠져있는 부분 표시)

```
InterviewCreationService.createInterview(userId, request, resumeFile)
  └─ Interview 저장 (userId 포함)
  └─ QuestionGenerationRequestedEvent 발행 ← ❌ userId 미포함
       └─ QuestionGenerationEventHandler.handleQuestionGenerationEvent(event)
            └─ QuestionGenerationService.generateQuestions(interviewId, ...) ← ❌ userId 없음
                 └─ CacheableQuestionProvider.provide(position, level, techStack, type, count, csSubTopics) ← ❌ userId 없음
                      └─ QuestionPoolService.isPoolSufficient(cacheKey, requiredCount, categoryFilter)
                      └─ QuestionPoolService.selectFromPool(cacheKey, requiredCount, categoryFilter)
```

### 현재 "부족" 판단 (QuestionPoolService:27-29)

```java
private static final int POOL_SUFFICIENCY_MULTIPLIER = 3;

public boolean isPoolSufficient(String cacheKey, int requiredCount) {
    long activeCount = questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey);
    return activeCount >= (long) requiredCount * POOL_SUFFICIENCY_MULTIPLIER;
}
// 예: 5개 필요 → 풀에 15개 이상이면 "충분"
// 문제: 사용자가 이미 15개를 전부 받았어도 "충분"으로 판단
```

### 현재 선택 로직 (QuestionPoolService:60-101)

`selectWithCategoryDistribution()` — 카테고리별 라운드로빈으로 셔플 후 선택. 랜덤이지만 **사용자별 필터링 없음**.

### 현재 AI 생성 트리거 (CacheableQuestionProvider:46-88)

풀이 부족하면 `generateWithStampedeProtection()` → `ReentrantLock`으로 동시 호출 방지 → AI 생성 → 풀에 저장(soft cap 200 미만일 때) → 결과에서 선택.

---

## 변경 설계

### "부족" 판단 기준 변경

```
기존: activeCount >= requiredCount * 3 (풀 전체 기준, 사용자 무관)
변경: (activeCount - userUsedCount) >= requiredCount * 1.5 (사용자별 미사용 기준 + 다양성 여유)
```

- 3배수 multiplier를 1.5배수로 축소. 사용자별 필터링이 주 다양성 보장이지만, 미사용 질문이 딱 requiredCount개만 남으면 셔플/분배의 다양성이 사라지므로 최소 여유 배수를 유지한다.
- `requiredCount * 1.5` 이상 미사용 질문이 있으면 "충분".
- 미사용 질문이 부족하면 AI 생성으로 풀 확장 후 선택.

### userId 전달 경로 수정

```
InterviewCreationService.createInterview(userId, request, resumeFile)
  └─ QuestionGenerationRequestedEvent 발행 ← ✅ userId 추가
       └─ QuestionGenerationEventHandler.handleQuestionGenerationEvent(event)
            └─ QuestionGenerationService.generateQuestions(..., userId) ← ✅ userId 추가
                 └─ provideCacheableQuestions(..., userId) ← ✅ userId 추가
                      └─ CacheableQuestionProvider.provide(..., userId) ← ✅ userId 추가
                           └─ QuestionRepository.findUsedQuestionPoolIdsByUserId(userId)
                           └─ QuestionPoolService.selectFromPool(..., usedPoolIds) ← ✅ 기출 제외
```

---

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `QuestionGenerationRequestedEvent.java` | `userId` 필드 추가 |
| `InterviewCreationService.java` | 이벤트 발행 시 `userId` 전달 |
| `QuestionGenerationEventHandler.java` | `event.getUserId()` → `generateQuestions()` 전달 |
| `QuestionGenerationService.java` | `generateQuestions()` + `provideCacheableQuestions()` 에 userId 파라미터 추가 |
| `CacheableQuestionProvider.java` | `provide()` 에 userId 파라미터 추가, 충분성 체크 변경 |
| `QuestionPoolService.java` | `selectFromPool()` + `isPoolSufficientForUser()` 오버로드 추가 |
| `QuestionRepository.java` | 사용자별 기출 question_pool_id 조회 쿼리 추가 |

## 상세

### 1. QuestionGenerationRequestedEvent에 userId 추가

```java
// 기존 필드들 + 추가
private final Long userId;  // ← 신규
```

### 2. InterviewCreationService에서 userId 전달

```java
// 기존 코드 (54행):
eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(
        saved.getId(),
        // ... 기존 파라미터 ...
        request.getTechStack(),
        userId  // ← 추가
));
```

### 3. QuestionGenerationEventHandler에서 userId 전달

```java
questionGenerationService.generateQuestions(
        event.getInterviewId(), event.getPosition(), event.getLevel(),
        event.getInterviewTypes(), event.getCsSubTopics(),
        event.getResumeText(), event.getDurationMinutes(), event.getTechStack(),
        event.getUserId());  // ← 추가
```

### 4. QuestionGenerationService에 userId 전달

```java
// generateQuestions() 시그니처에 userId 추가
public void generateQuestions(Long interviewId, Position position,
                              InterviewLevel level, List<InterviewType> interviewTypes,
                              List<String> csSubTopics, String resumeText,
                              Integer durationMinutes, TechStack techStack,
                              Long userId) {  // ← 추가
    // ...
    // provideCacheableQuestions 호출 시 userId 전달
    CompletableFuture<List<QuestionSet>> cacheableFuture = CompletableFuture.supplyAsync(() ->
            provideCacheableQuestions(interviewId, position, level, effectiveTechStack,
                    cacheableTypes, csSubTopics, userId),  // ← 추가
            virtualExecutor
    ).orTimeout(60, TimeUnit.SECONDS);
}

// provideCacheableQuestions에도 userId 추가
private List<QuestionSet> provideCacheableQuestions(
        Long interviewId, Position position, InterviewLevel level,
        TechStack techStack, Map<InterviewType, Integer> typeDistribution,
        List<String> csSubTopics, Long userId) {  // ← 추가
    // ...
    List<QuestionPool> poolQuestions = cacheableProvider.provide(
            position, level, techStack, type, count, csSubTopics, userId);  // ← 추가
}
```

### 5. QuestionRepository에 기출 조회 쿼리 추가

```java
@Query("SELECT DISTINCT q.questionPool.id FROM Question q " +
       "JOIN q.questionSet qs JOIN qs.interview i " +
       "WHERE i.userId = :userId AND q.questionPool IS NOT NULL " +
       "AND q.questionPool.cacheKey = :cacheKey")
Set<Long> findUsedQuestionPoolIdsByUserIdAndCacheKey(
        @Param("userId") Long userId, @Param("cacheKey") String cacheKey);
```

> Interview 엔티티의 userId는 `Long userId` 단순 필드 (확인 완료).
> cacheKey 필터를 추가하여 해당 풀 범위의 기출만 조회 — 면접을 많이 본 사용자도 불필요한 데이터 로딩 없음.

### 6. CacheableQuestionProvider 변경

```java
public List<QuestionPool> provide(Position position, InterviewLevel level,
                                  TechStack techStack, InterviewType type,
                                  int requiredCount, List<String> csSubTopics,
                                  Long userId) {  // ← 추가

    String cacheKey = QuestionCacheKeyGenerator.generate(position, level, techStack, type);
    List<String> categoryFilter = toCategoryFilter(csSubTopics);

    // userId로 해당 cacheKey 범위의 기출 조회 (null이면 빈 Set → 기존 로직과 동일)
    Set<Long> usedPoolIds = (userId != null)
            ? questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(userId, cacheKey)
            : Set.of();

    if (questionPoolService.isPoolSufficientForUser(cacheKey, requiredCount, categoryFilter, usedPoolIds)) {
        log.info("[CACHE] pool 히트 (사용자 기출 제외): cacheKey={}, userId={}", cacheKey, userId);
        return questionPoolService.selectFromPool(cacheKey, requiredCount, categoryFilter, usedPoolIds);
    }

    log.info("[CACHE] 미사용 질문 부족, AI 호출: cacheKey={}, userId={}", cacheKey, userId);
    return generateWithStampedeProtection(cacheKey, position, level, techStack, type,
            requiredCount, csSubTopics, categoryFilter, usedPoolIds);
}
```

### 7. QuestionPoolService 오버로드 추가

```java
public boolean isPoolSufficientForUser(String cacheKey, int requiredCount,
        List<String> categoryFilter, Set<Long> usedPoolIds) {
    List<QuestionPool> candidates;
    if (categoryFilter == null || categoryFilter.isEmpty()) {
        candidates = questionPoolRepository.findByCacheKeyAndIsActiveTrue(cacheKey);
    } else {
        candidates = questionPoolRepository
                .findByCacheKeyAndIsActiveTrueAndCategoryIn(cacheKey, categoryFilter);
    }
    long unusedCount = candidates.stream()
            .filter(qp -> !usedPoolIds.contains(qp.getId()))
            .count();
    return unusedCount >= (long) Math.ceil(requiredCount * 1.5);
}

public List<QuestionPool> selectFromPool(String cacheKey, int requiredCount,
        List<String> categoryFilter, Set<Long> usedPoolIds) {
    List<QuestionPool> candidates;
    if (categoryFilter == null || categoryFilter.isEmpty()) {
        candidates = questionPoolRepository.findByCacheKeyAndIsActiveTrue(cacheKey);
    } else {
        candidates = questionPoolRepository
                .findByCacheKeyAndIsActiveTrueAndCategoryIn(cacheKey, categoryFilter);
    }
    List<QuestionPool> unused = candidates.stream()
            .filter(qp -> !usedPoolIds.contains(qp.getId()))
            .collect(Collectors.toCollection(ArrayList::new));
    return selectWithCategoryDistribution(unused, requiredCount);
}
```

### 8. generateWithStampedeProtection에 usedPoolIds 전달

```java
private List<QuestionPool> generateWithStampedeProtection(
        String cacheKey, ..., Set<Long> usedPoolIds) {

    ReentrantLock lock = questionGenerationLock.acquire(cacheKey);
    try {
        // lock 후 재확인 (다른 스레드가 이미 생성했을 수 있음)
        if (questionPoolService.isPoolSufficientForUser(cacheKey, requiredCount, categoryFilter, usedPoolIds)) {
            return questionPoolService.selectFromPool(cacheKey, requiredCount, categoryFilter, usedPoolIds);
        }

        // AI 생성
        List<GeneratedQuestion> generated = aiClient.generateQuestions(request);
        List<QuestionPool> allGenerated = questionPoolService.convertAndCacheIfEligible(cacheKey, generated);

        // 새로 생성된 질문은 이 사용자가 본 적 없으므로 바로 선택 가능
        // categoryFilter 적용 후 선택
        // ... 기존 로직 유지
    } finally {
        questionGenerationLock.release(cacheKey, lock);
    }
}
```

---

## 엣지케이스

| 케이스 | 처리 |
|--------|------|
| userId가 null (비로그인/게스트) | usedPoolIds = Set.of() → 기존 로직과 동일 (전체 풀에서 랜덤) |
| 사용자가 풀의 모든 질문을 소진 | AI 생성 트리거 → 새 질문 풀에 추가 후 선택 |
| AI 생성 실패 | 기존 예외 처리 로직 유지 (에러 전파) |
| 풀이 soft cap(200)에 도달한 상태에서 확장 필요 | AI 생성은 하되 DB 저장 생략(기존 로직), 생성된 질문은 해당 면접에서 사용 |
| 동시에 같은 사용자가 면접 생성 | 기존 ReentrantLock(per cacheKey)으로 stampede 방지 |

## 담당 에이전트

- Implement: `backend` — 이벤트, Service, Repository 전체 수정
- Review: `architect-reviewer` — 레이어링, userId 전달 경로 일관성, 쿼리 성능

## 검증

- 동일 유저가 2번째 면접 생성 시 이전 질문이 제외되는지 단위 테스트
- userId가 null인 경우 기존 로직과 동일하게 동작하는지 테스트
- 풀 소진 시 AI 생성이 트리거되는지 테스트
- 기출 조회 쿼리 성능 확인 (EXPLAIN)
- 기존 테스트 (`InterviewCreationServiceTest`, `InterviewServiceTest`) 깨지지 않는지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
