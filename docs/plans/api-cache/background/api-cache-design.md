# Rehearse — AI 면접 질문 캐싱 및 비용 최적화 설계서 v2

> 이 문서는 Rehearse(AI 모의 면접 플랫폼)의 면접 질문 생성에서 Claude API 호출 횟수와 토큰 비용을 최소화하기 위한 전체 설계를 기술한다.
> 구현 담당자(Claude Code 포함)는 이 문서를 기준으로 코드를 작성한다.

---

## 1. 프로젝트 컨텍스트

### 1.1 기술 스택

- Backend: Java 21 + Spring Boot 3.x + JPA + MySQL
- 질문 생성: Claude API (claude-sonnet-4-20250514)
- 답변 분석: OpenAI API (Whisper STT + GPT-4o) — AWS Lambda
- 향후: 사용자 인증(로그인) 기능 추가 예정

### 1.2 현재 면접 생성 플로우

1. 사용자가 면접 조건 설정 (Position, Level, InterviewType[], csSubTopics[], 이력서 PDF, 면접 시간)
2. Backend → Claude API 호출 → 질문 목록 생성 (1회)
3. 면접 진행 중 → 후속 질문도 Claude API 호출 (질문당 1회)

### 1.3 핵심 문제

- 매 면접 생성 시 Claude API를 호출하지만, 이력서 비의존 유형은 같은 조건이면 누가 요청하든 비슷한 질문이 생성됨
- 후속 질문도 매번 호출하지만, 기술 지식 질문의 꼬리 질문은 사전 생성이 가능함
- 시스템 프롬프트가 매 호출마다 동일하게 전송되어 입력 토큰이 낭비됨

---

## 2. InterviewType 분류 체계

### 2.1 Cacheable vs Fresh 분류

모든 최적화의 기반이 되는 핵심 분류이다.

```java
public enum CacheStrategy {
    CACHEABLE,  // 이력서/경험에 의존하지 않음 → DB pool에서 제공 가능
    FRESH       // 이력서/경험에 의존 → 항상 Claude 실시간 생성 필요
}
```

| InterviewType | CacheStrategy | 근거 |
|---|---|---|
| CS_FUNDAMENTAL | CACHEABLE | 보편적 CS 지식 질문 |
| JAVA_SPRING | CACHEABLE | 기술 스택 기초 질문 |
| SYSTEM_DESIGN | CACHEABLE | 설계 역량 질문 |
| REACT_COMPONENT | CACHEABLE | 프론트 기술 질문 |
| BROWSER_PERFORMANCE | CACHEABLE | 브라우저 성능 질문 |
| INFRA_CICD | CACHEABLE | 인프라/CI-CD 질문 |
| CLOUD | CACHEABLE | 클라우드 기술 질문 |
| DATA_PIPELINE | CACHEABLE | 데이터 파이프라인 질문 |
| SQL_MODELING | CACHEABLE | SQL/모델링 질문 |
| FULLSTACK_JS | CACHEABLE | 풀스택 JS 질문 |
| RESUME_BASED | FRESH | 이력서 내용 기반 맞춤 질문 |
| BEHAVIORAL | FRESH | 경험 기반 질문 |

### 2.2 후속 질문(Follow-Up) 전략 분류

메인 질문의 성격에 따라 후속 질문 생성 방식이 달라진다.
이 분류는 InterviewType 단위가 아니라 **개별 질문 단위**로 적용한다.

```java
public enum FollowUpStrategy {
    PREPARED,  // 사전 생성 가능 — 정답이 있는 기술 질문의 꼬리 질문
    REALTIME   // 실시간 생성 필요 — 사용자 답변 내용에 따라 달라지는 꼬리 질문
}
```

**분류 기준:**

- PREPARED: "Java GC 설명하세요" → 사용자가 뭐라고 답하든 "G1 vs ZGC 차이는?", "GC 튜닝 경험은?" 등으로 파고들 수 있음
- REALTIME: "이 프로젝트에서 본인 역할을 설명하세요" → "DB 설계했습니다" vs "API 개발했습니다"에 따라 후속 질문이 완전히 달라짐

**결정 규칙:**

- FRESH 유형(RESUME_BASED, BEHAVIORAL)의 모든 질문 → REALTIME
- CACHEABLE 유형의 질문 → 기본 PREPARED (Claude가 초기 생성 시 함께 생성)
- CACHEABLE 유형이지만 경험을 묻는 질문 (예: "Spring 트랜잭션 관리를 프로젝트에서 어떻게 했나요?") → REALTIME
    - 이 판단은 초기 질문 생성 시 Claude에게 `followUpStrategy` 태그를 함께 출력하도록 요청하여 결정

---

## 3. 질문 Pool 캐싱 시스템

### 3.1 캐시 키 설계

```
{position}:{level}:{interviewType}[:{sorted(csSubTopics)}]
```

예시:
- `BACKEND:JUNIOR:JAVA_SPRING`
- `BACKEND:JUNIOR:CS_FUNDAMENTAL:DATA_STRUCTURE,NETWORK`
- `FRONTEND:MID:REACT_COMPONENT`

csSubTopics는 CS_FUNDAMENTAL 유형에서만 사용하며, 알파벳 순 정렬하여 키 일관성을 보장한다.

### 3.2 DB 스키마

```sql
-- 질문 Pool 테이블
CREATE TABLE question_pool (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key           VARCHAR(255) NOT NULL,
    content             TEXT NOT NULL,           -- 질문 텍스트
    category            VARCHAR(100),            -- 질문 카테고리 (JVM, Network 등)
    question_order      INT,                     -- 원래 생성 시 순서
    evaluation_criteria TEXT NOT NULL,            -- 평가 기준
    model_answer        TEXT,                    -- 모범 답안 (nullable, 분리 생성 시)
    reference_type      VARCHAR(50),             -- MODEL_ANSWER 또는 GUIDE
    follow_up_strategy  VARCHAR(20) NOT NULL DEFAULT 'PREPARED',  -- PREPARED 또는 REALTIME
    quality_score       DECIMAL(3,2) DEFAULT NULL, -- 품질 점수 (향후 피드백 기반)
    is_active           BOOLEAN DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_cache_key_active (cache_key, is_active),
    INDEX idx_created_at (created_at)
);

-- PREPARED 후속 질문 테이블
CREATE TABLE prepared_follow_up (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_pool_id    BIGINT NOT NULL,         -- 부모 메인 질문 FK
    content             TEXT NOT NULL,            -- 후속 질문 텍스트
    trigger_condition   VARCHAR(255),            -- 선택 조건 설명 (예: "GC 종류를 나열한 경우")
    display_order       INT NOT NULL DEFAULT 0,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (question_pool_id) REFERENCES question_pool(id) ON DELETE CASCADE,
    INDEX idx_question_pool_id (question_pool_id)
);

-- 사용자별 질문 출제 이력 (로그인 기능 구현 후 활성화)
CREATE TABLE user_question_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    question_pool_id    BIGINT NOT NULL,
    served_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_user_question (user_id, question_pool_id),
    INDEX idx_user_id (user_id)
);
```

### 3.3 Pool 충분성 기준

```java
private static final int POOL_SUFFICIENCY_MULTIPLIER = 3;
private static final int POOL_SOFT_CAP = 200;

public boolean isPoolSufficient(String cacheKey, int requiredCount) {
    long activeCount = questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey);
    return activeCount >= requiredCount * POOL_SUFFICIENCY_MULTIPLIER;
}

public boolean shouldSaveToPool(String cacheKey) {
    long activeCount = questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey);
    return activeCount < POOL_SOFT_CAP;
}
```

- 필요 질문 수 × 3 이상이면 pool에서 제공
- 미만이면 Claude 호출 → 사용자에게 제공 + pool에 저장 (Lazy 축적)
- 캐시 키당 soft cap: 200개 (초과 시 더 이상 적재하지 않음)

### 3.4 Pool에서 질문 조회 — 카테고리 분산 보장

pool에서 랜덤 선택 시 특정 category(예: JVM)에 질문이 몰리는 것을 방지한다.
애플리케이션 레벨에서 카테고리별 분산 셔플링을 적용한다.

```sql
-- pool에서 충분한 후보를 가져온다 (필요 수의 3배)
SELECT * FROM question_pool
WHERE cache_key = :cacheKey AND is_active = TRUE
ORDER BY RAND()
LIMIT :candidateCount;
```

> soft cap이 200개이므로 ORDER BY RAND()의 성능 이슈는 없다.

```java
/**
 * 후보 질문 목록에서 카테고리가 고르게 분포되도록 선택한다.
 * Round-robin 방식으로 각 카테고리에서 하나씩 번갈아 선택.
 */
public List<QuestionPool> selectWithCategoryDistribution(
        List<QuestionPool> candidates, int requiredCount) {

    // 카테고리별로 그룹핑
    Map<String, Queue<QuestionPool>> byCategory = candidates.stream()
        .collect(Collectors.groupingBy(
            QuestionPool::getCategory,
            Collectors.toCollection(LinkedList::new)));

    List<QuestionPool> result = new ArrayList<>();
    List<String> categories = new ArrayList<>(byCategory.keySet());
    Collections.shuffle(categories); // 카테고리 순서도 랜덤화

    int categoryIdx = 0;
    while (result.size() < requiredCount && !byCategory.isEmpty()) {
        String cat = categories.get(categoryIdx % categories.size());
        Queue<QuestionPool> queue = byCategory.get(cat);

        if (queue != null && !queue.isEmpty()) {
            result.add(queue.poll());
        } else {
            byCategory.remove(cat);
            categories.remove(cat);
            if (categories.isEmpty()) break;
        }
        categoryIdx++;
    }
    return result;
}
```

**로그인 후 — 이전 출제 질문 제외:**

```sql
SELECT qp.* FROM question_pool qp
WHERE qp.cache_key = :cacheKey AND qp.is_active = TRUE
  AND qp.id NOT IN (
      SELECT uqh.question_pool_id
      FROM user_question_history uqh
      WHERE uqh.user_id = :userId
  )
ORDER BY RAND()
LIMIT :candidateCount;
```

### 3.5 Cache Stampede 방어

서비스 초기에 pool이 비어있는 상태에서 동시 요청이 들어오면, 여러 스레드가 동시에 Claude API를 호출하여 중복 질문을 pool에 밀어넣는 문제가 발생할 수 있다.

**방어 전략: 인메모리 락 (단일 인스턴스 기준)**

```java
@Component
public class CacheableQuestionProvider {

    // 캐시 키별 진행 중인 Claude 호출을 추적
    private final ConcurrentHashMap<String, CompletableFuture<List<QuestionPool>>> inFlightRequests
        = new ConcurrentHashMap<>();

    public List<QuestionPool> provide(String cacheKey, int requiredCount) {
        // 1. pool 충분성 확인
        if (isPoolSufficient(cacheKey, requiredCount)) {
            return selectFromPool(cacheKey, requiredCount);
        }

        // 2. pool 부족 → Claude 호출, 동일 키에 대해 하나의 호출만 허용
        CompletableFuture<List<QuestionPool>> future = inFlightRequests.computeIfAbsent(
            cacheKey,
            key -> CompletableFuture.supplyAsync(() -> {
                try {
                    List<QuestionPool> generated = callClaudeAndSaveToPool(key, requiredCount);
                    return generated;
                } finally {
                    inFlightRequests.remove(key);
                }
            })
        );

        // 3. 첫 번째 요청이든 후속 요청이든 동일한 Future의 결과를 공유
        List<QuestionPool> generated = future.join();

        // 4. 결과에서 필요한 수만큼 선택 (여러 요청이 공유하므로 각자 다른 서브셋 선택)
        return selectWithCategoryDistribution(generated, requiredCount);
    }
}
```

**설계 판단:**
- 현재 스택에 Redis가 없으므로 분산 락(Redisson 등)은 도입하지 않는다.
- 단일 인스턴스 배포 기준으로 `ConcurrentHashMap`의 `computeIfAbsent` 원자성만으로 충분하다.
- 다중 인스턴스 배포 시에도, 최악의 경우 인스턴스 수만큼 중복 호출이 발생하지만 soft cap(200개)이 있으므로 pool이 넘치지는 않는다.

### 3.6 Pool 고갈 시 Fallback

같은 사용자가 동일 캐시 키로 반복 면접을 봐서 미출제 질문이 부족한 경우:

1. 부족한 수만큼 Claude API를 호출하여 새 질문 생성
2. 이때 기존 pool의 질문 목록(content 필드)을 Claude 프롬프트에 "이미 출제된 질문 목록"으로 포함하여 중복 방지
3. 생성된 질문은 사용자에게 제공 + pool에 저장

---

## 4. 면접 생성 플로우 (최적화 후)

### 4.1 전체 아키텍처

```
InterviewCreationService
│
├── 요청에서 interviewTypes를 CACHEABLE / FRESH로 분류
│
├── 유형별 필요 질문 수 계산 (전체 질문 수를 유형 수로 균등 배분)
│
├── CacheableQuestionProvider (병렬 실행)
│   ├── pool 충분 → DB에서 카테고리 분산 선택 (수십ms)
│   └── pool 부족 → Claude 호출 → 제공 + pool 저장 (stampede 방어 적용)
│
├── FreshQuestionProvider (병렬 실행)
│   └── 항상 Claude 호출 (이력서 + 조건 기반)
│
└── 결과 합산 → question_order 재배정 → 면접 생성 완료
```

### 4.2 질문 수 배분 로직

```java
public Map<InterviewType, Integer> distributeQuestionCount(
        List<InterviewType> types, int totalQuestions) {
    int base = totalQuestions / types.size();
    int remainder = totalQuestions % types.size();

    Map<InterviewType, Integer> distribution = new LinkedHashMap<>();
    for (int i = 0; i < types.size(); i++) {
        distribution.put(types.get(i), base + (i < remainder ? 1 : 0));
    }
    return distribution;
}
```

### 4.3 병렬 처리 및 트랜잭션 경계

cacheable 유형(DB 조회, 수십ms)과 fresh 유형(Claude API, 수초)을 CompletableFuture로 동시 실행한다.

**트랜잭션 설계 원칙:**
- Spring의 `@Transactional`은 ThreadLocal 기반이므로, CompletableFuture 워커 스레드로 전파되지 않는다.
- 따라서 각 Provider 내부에서 독립적인 트랜잭션을 사용하거나, 비동기 작업 완료 후 메인 스레드에서 일괄 저장한다.

```java
@Service
public class InterviewCreationService {

    // 이 메서드 자체는 @Transactional을 걸지 않는다.
    // 병렬 작업 완료 후 메인 스레드에서 일괄 저장한다.
    public Interview createInterview(InterviewCreateRequest request) {

        Map<InterviewType, Integer> distribution = distributeQuestionCount(
            request.getTypes(), calculateQuestionCount(request.getInterviewMinutes()));

        Map<CacheStrategy, Map<InterviewType, Integer>> grouped = distribution.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> CacheStrategyConfig.getStrategy(e.getKey()),
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        // 병렬 실행 — 각 Provider는 자체 트랜잭션으로 DB 접근
        CompletableFuture<List<Question>> cacheableFuture =
            CompletableFuture.supplyAsync(() ->
                cacheableProvider.provide(grouped.getOrDefault(CACHEABLE, Map.of())));

        CompletableFuture<List<Question>> freshFuture =
            CompletableFuture.supplyAsync(() ->
                freshProvider.provide(
                    grouped.getOrDefault(FRESH, Map.of()),
                    request.getResumeText()));

        // 메인 스레드에서 합산 및 일괄 저장
        List<Question> allQuestions = Stream.concat(
            cacheableFuture.join().stream(),
            freshFuture.join().stream()
        ).collect(Collectors.toList());

        // question_order 재배정
        IntStream.range(0, allQuestions.size())
            .forEach(i -> allQuestions.get(i).setOrder(i + 1));

        return interviewRepository.save(
            Interview.create(request, allQuestions));
    }
}
```

```java
@Service
public class CacheableQuestionProvider {

    // 독립 트랜잭션: 워커 스레드에서 실행되므로 자체 트랜잭션 필요
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Question> provide(Map<InterviewType, Integer> distribution) {
        // pool 조회 및 필요 시 Claude 호출 + pool 저장
    }
}
```

**예외 처리:**

```java
// Claude API 호출 타임아웃 및 실패 시 처리
CompletableFuture<List<Question>> freshFuture =
    CompletableFuture.supplyAsync(() -> freshProvider.provide(...))
        .orTimeout(30, TimeUnit.SECONDS)
        .exceptionally(ex -> {
            log.error("Fresh question generation failed", ex);
            // Fallback: cacheable pool에서 범용 질문으로 대체하거나 빈 리스트 반환
            return List.of();
        });
```

---

## 5. 후속 질문 생성 플로우 (최적화 후)

### 5.1 전략 분기

```
면접 진행 중 후속 질문 요청
│
├── 메인 질문의 followUpStrategy 확인
│
├── PREPARED인 경우
│   ├── prepared_follow_up 테이블에서 후보 조회
│   ├── triggerCondition 기반 규칙 분기로 선택 (아래 5.2 참고)
│   └── Claude 호출 0회
│
└── REALTIME인 경우
    ├── Claude API 호출 (Haiku 모델 사용)
    ├── 입력: 축소된 프롬프트 (아래 5.3 참고)
    └── 출력: 꼬리 질문 1개 + 평가기준
```

### 5.2 PREPARED 후속 질문 선택 방식

사전 생성된 후속 질문(2~3개) 중 하나를 선택한다.

**기본 전략 — triggerCondition 규칙 기반 (Claude 호출 0회)**

triggerCondition은 Claude가 초기 질문 생성 시 함께 만든 간단한 조건 설명이다.
서버에서는 사용자 답변의 단순한 특성(길이, 키워드 포함 여부)으로 매칭한다.

```java
public PreparedFollowUp selectFollowUp(
        List<PreparedFollowUp> candidates, String userAnswer) {

    // 1. triggerCondition 기반 매칭 시도
    for (PreparedFollowUp candidate : candidates) {
        if (matchesTriggerCondition(candidate.getTriggerCondition(), userAnswer)) {
            return candidate;
        }
    }

    // 2. 매칭 실패 시 display_order 순서대로 제공
    return candidates.stream()
        .min(Comparator.comparingInt(PreparedFollowUp::getDisplayOrder))
        .orElseThrow();
}

private boolean matchesTriggerCondition(String condition, String answer) {
    // 단순 규칙 기반 — 키워드 존재 여부, 답변 길이 등
    // 예: condition = "GC 종류를 나열한 경우"
    //     → answer에 "G1", "ZGC", "CMS", "Serial" 중 2개 이상 포함되면 매칭
    // 복잡한 NLP가 아닌, Claude가 생성한 조건에 대한 간단한 규칙 매핑
}
```

**설계 판단:**
- 후속 질문 후보가 2~3개뿐인 상황에서 LLM API를 호출하는 것은 비효율적이다.
  네트워크 레이턴시(200~500ms)가 면접 중 매 질문마다 추가되면 UX에 영향을 줌.
- triggerCondition 매칭이 실패해도 display_order fallback이 있으므로 면접이 중단되지 않음.
- 향후 고도화 시 Haiku 분류 호출(옵션 B)로 전환 가능하나, MVP에서는 불필요.

### 5.3 REALTIME 후속 질문 — 비용 최소화

호출을 없앨 수 없으므로 호출당 비용을 최소화한다.

**모델 다운그레이드:**
- 후속 질문 생성에는 Haiku 사용 (Sonnet 대비 토큰당 비용 대폭 절감)
- 이미 컨텍스트(메인 질문 + 사용자 답변)가 주어진 상태에서 꼬리 질문 1개를 만드는 작업이므로 Haiku로 충분

**입력 토큰 최소화:**

```
AS-IS (현재 추정):
  시스템 프롬프트       ~600 토큰
  이력서 전문           ~500-2000 토큰
  현재 질문 + 답변      ~200 토큰
  합계                  ~1,300-2,800 토큰

TO-BE (최적화 후):
  후속 질문 전용 시스템 프롬프트  ~100 토큰
  이력서 관련 섹션만 발췌         ~100-300 토큰
  현재 질문 + 답변 + 평가기준     ~200 토큰
  합계                           ~400-600 토큰
```

후속 질문 전용 시스템 프롬프트 예시:
```
당신은 기술 면접관입니다. 아래 질문과 지원자의 답변을 보고,
답변에서 부족하거나 더 깊이 확인해야 할 부분을 파고드는 꼬리 질문 1개를 생성하세요.
JSON 형식으로 응답: {"content": "질문 텍스트", "evaluationCriteria": "평가 기준"}
```

**출력 토큰 제한:**
- max_tokens: 200
- 후속 질문 1개 + 평가기준 한 줄만 출력

**Prompt Caching 적용 (같은 면접 세션 내):**
- 같은 면접에서 후속 질문을 여러 번 호출할 때, 시스템 프롬프트 + 이력서 섹션 부분에 cache_control 설정
- 1번째 호출: cache write (1.25x)
- 2번째 이후: cache hit (0.1x)
- 면접 한 세션(~30분)은 5분 TTL 내에 진행되므로 캐시 유지

---

## 6. API 비용 최적화 전략

### 6.1 Anthropic Prompt Caching

**적용 대상:** Claude API를 직접 호출하는 모든 경우

**방법:**
- 시스템 프롬프트(면접관 역할 + 출제 가이드 + JSON 스키마)에 `cache_control: {"type": "ephemeral"}` 설정
- 최소 캐싱 토큰: 1,024 (claude-sonnet-4 기준)
- 현재 시스템 프롬프트 ~600토큰 → 유형별 출제 가이드 + JSON 스키마를 합쳐 1,024토큰 이상으로 구성

**비용 효과:**
- cache write: 기본 입력 가격의 1.25x
- cache hit: 기본 입력 가격의 0.1x (90% 절감)
- 5분 내 동일 prefix 요청이 2회 이상이면 이득

**구현 시 주의:**
- 시스템 프롬프트는 모든 요청에서 동일해야 캐시 히트 발생
- 유저 프롬프트(동적 부분)는 시스템 프롬프트 뒤에 배치
- tool_choice나 이미지 포함 여부가 변경되면 캐시 무효화됨

### 6.2 Batch API로 Pool 사전 축적

**목적:** 서비스 초기 또는 pool 부족 시 저렴하게 대량 질문 생성

**Batch API 특징:**
- 입출력 토큰 모두 50% 할인
- Prompt Caching과 할인 중첩 가능 (최대 입력 토큰 95% 절감)
- 비동기 처리, 대부분 1시간 내 완료
- 배치당 최대 100,000 요청

**적용 시나리오:**

```
1. 서비스 초기 시딩 (1회성)
   - 주요 캐시 키 조합에 대해 질문 사전 생성
   - cacheable 유형 10개 × Position 5개 × Level 3개 = 최대 150개 키
   - 키당 15-20개 질문 → 총 2,250-3,000 요청
   - Batch 1회로 처리 가능
   - 주의: Anthropic 계정의 티어별 일일 Batch 토큰 한도를 사전 확인할 것

2. 야간 보충 배치 (주기적)
   - pool 부족 캐시 키 탐지 쿼리:
     SELECT cache_key, COUNT(*) as cnt
     FROM question_pool
     WHERE is_active = TRUE
     GROUP BY cache_key
     HAVING cnt < (필요 질문 수 × 3);
   - 부족한 키에 대해 Batch API로 질문 생성
```

**Batch 요청 구성 예시:**

```java
BatchRequest request = BatchRequest.builder()
    .customId("BACKEND:JUNIOR:JAVA_SPRING_batch_001")
    .params(MessageParams.builder()
        .model("claude-sonnet-4-20250514")
        .maxTokens(3000)
        .system(SYSTEM_PROMPT)  // cache_control 포함
        .messages(List.of(
            Message.user("Position: BACKEND, Level: JUNIOR, Type: JAVA_SPRING, 질문 수: 15")
        ))
        .build())
    .build();
```

### 6.3 모델 티어링

| 용도 | 모델 | 근거 |
|---|---|---|
| 초기 질문 생성 (높은 품질) | Sonnet 4 | 구조화된 질문 세트 + 평가기준 + 후속 질문 |
| 후속 질문 생성 (REALTIME) | Haiku | 컨텍스트 주어진 상태에서 꼬리 질문 1개 생성 |
| Pool 사전 축적 배치 | Sonnet 4 (Batch) | 50% 할인 적용, 품질 유지 필요 |

### 6.4 출력 토큰 절감 — 모범답안 분리 생성

**현재:** 초기 질문 생성 시 질문 + 평가기준 + 모범답안을 한 번에 생성 (출력 ~2,000-3,000토큰)

**최적화:** 모범답안(modelAnswer)을 면접 생성 시점에서 분리

```
초기 질문 생성 시 출력:
  - content (질문 텍스트)
  - category
  - evaluationCriteria
  - followUpStrategy (PREPARED/REALTIME)
  - preparedFollowUps (PREPARED인 경우)
  → 출력 토큰: ~1,000-1,500

모범답안 생성 시점:
  - 면접 완료 후, 피드백 단계에서 생성
  - 면접 중도 포기 시 생성 비용 0
  - OpenAI Lambda 피드백 파이프라인에서 함께 처리 가능
```

**효과:** 초기 질문 생성 출력 토큰 40-50% 절감 + 중도 포기 시 모범답안 비용 0

### 6.5 면접 시간 기반 질문 수 동적 조절

면접 시간 옵션에 따라 질문 수 상한을 제한하여 불필요한 생성을 방지:

```java
public int calculateQuestionCount(Integer interviewMinutes, int typeCount) {
    if (interviewMinutes == null) {
        return Math.min(typeCount * 2, 8); // 기본값
    }
    // 질문당 평균 3-5분 소요 가정
    int maxQuestions = interviewMinutes / 4;
    return Math.max(3, Math.min(maxQuestions, 10));
}
```

---

## 7. 캐시 무효화 전략

### 7.1 InterviewType별 갱신 주기

면접 질문은 "틀린 데이터"가 아닌 "오래된 데이터"이므로, 공격적 무효화보다는 주기적 갱신이 적합하다.

| 갱신 주기 | InterviewType | 근거 |
|---|---|---|
| 12개월 | CS_FUNDAMENTAL, SQL_MODELING | 기초 CS 지식은 거의 변하지 않음 |
| 6개월 | JAVA_SPRING, SYSTEM_DESIGN, REACT_COMPONENT, BROWSER_PERFORMANCE, FULLSTACK_JS | 프레임워크/언어 업데이트 주기 |
| 3개월 | CLOUD, INFRA_CICD, DATA_PIPELINE | 클라우드/인프라 기술 변화가 빠름 |

### 7.2 무효화 방식

- 삭제가 아닌 soft-deprecate: `is_active = false`로 마킹
- 갱신 대상 질문은 created_at 기준으로 필터링
- 새 질문은 Batch API로 보충

```sql
-- 예: CLOUD 유형 3개월 이상 된 질문 비활성화
UPDATE question_pool
SET is_active = FALSE
WHERE cache_key LIKE '%:CLOUD'
  AND created_at < DATE_SUB(NOW(), INTERVAL 3 MONTH);
```

---

## 8. 이력서 기반 유형의 부분 최적화

FRESH 유형도 완전히 최적화 불가능한 것은 아니다.

### 8.1 이력서 텍스트 Prompt Caching

같은 사용자가 이력서를 바꾸지 않고 여러 번 면접을 생성하는 경우, 이력서 텍스트를 prompt caching 대상으로 포함:

```java
// 시스템 프롬프트 + 이력서 텍스트를 합쳐서 cache breakpoint 설정
Message systemWithResume = Message.builder()
    .role("system")
    .content(SYSTEM_PROMPT + "\n\n[이력서]\n" + resumeText)
    .cacheControl(CacheControl.ephemeral())
    .build();
```

### 8.2 BEHAVIORAL 유형 하이브리드

BEHAVIORAL 질문 중 범용적인 것은 pool에 넣고, 이력서 맞춤 질문만 fresh 생성:

- 범용 예시: "협업 중 갈등 해결 경험", "실패 경험과 교훈", "리더십 발휘 경험"
- 이력서 맞춤 예시: "이력서에 적힌 X 프로젝트에서 어려웠던 점"

```java
// BEHAVIORAL 유형의 질문 수 배분
int totalBehavioral = distribution.get(BEHAVIORAL);
int genericCount = totalBehavioral / 2;     // 절반은 pool에서
int customCount = totalBehavioral - genericCount; // 나머지는 Claude 생성

// 별도 캐시 키: BEHAVIORAL:GENERIC:{level}
String genericCacheKey = "BEHAVIORAL:GENERIC:" + level;
```

---

## 9. 비용 효과 추정

### 9.1 현재 비용 (면접 1회, 질문 6개 기준)

| 항목 | 호출 수 | 입력 토큰 | 출력 토큰 |
|---|---|---|---|
| 초기 질문 생성 | 1회 | ~800 | ~2,500 |
| 후속 질문 생성 | 6회 | ~1,500 × 6 = 9,000 | ~300 × 6 = 1,800 |
| **합계** | **7회** | **~9,800** | **~4,300** |

### 9.2 최적화 후 비용 — Cacheable 유형만 면접

| 항목 | 호출 수 | 입력 토큰 | 출력 토큰 |
|---|---|---|---|
| 초기 질문 생성 | 0회 (pool) | 0 | 0 |
| 후속 질문 (PREPARED) | 0회 | 0 | 0 |
| **합계** | **0회** | **0** | **0** |

### 9.3 최적화 후 비용 — Mixed 면접 (cacheable 4개 + fresh 2개)

| 항목 | 호출 수 | 입력 토큰 | 출력 토큰 |
|---|---|---|---|
| cacheable 질문 | 0회 (pool) | 0 | 0 |
| fresh 질문 생성 | 1회 (Sonnet) | ~800 (prompt cached) | ~600 (모범답안 분리) |
| 후속 질문 PREPARED (4개) | 0회 | 0 | 0 |
| 후속 질문 REALTIME (2개) | 2회 (Haiku) | ~500 × 2 = 1,000 | ~150 × 2 = 300 |
| **합계** | **3회** | **~1,800** | **~900** |

**절감율: 호출 57% 감소, 입력 토큰 82% 감소, 출력 토큰 79% 감소**

---

## 10. 구현 우선순위

### Phase 1 — 핵심 캐싱 (즉시)

1. DB 스키마 생성 (question_pool, prepared_follow_up)
2. CacheStrategy enum 및 InterviewType 분류 매핑
3. CacheableQuestionProvider 구현 (pool 조회 + stampede 방어 + fallback Claude 호출)
4. FreshQuestionProvider 구현 (기존 로직 분리)
5. InterviewCreationService에서 cacheable/fresh 분기 + 병렬 처리 (트랜잭션 경계 주의)
6. Lazy 축적: Claude 응답을 pool에 저장하는 로직
7. 카테고리 분산 셔플링 로직

### Phase 2 — 후속 질문 최적화

8. FollowUpStrategy enum 및 질문별 태그 시스템
9. 초기 질문 생성 프롬프트 수정: followUpStrategy + preparedFollowUps 출력 포함
10. prepared_follow_up 테이블 저장/조회 로직
11. 후속 질문 분기 로직 (PREPARED → triggerCondition 규칙 + display_order fallback / REALTIME → Haiku 호출)
12. REALTIME 후속 질문 프롬프트 최소화 + 모델 다운그레이드

### Phase 3 — 비용 고도화

13. Anthropic Prompt Caching 적용 (시스템 프롬프트 1,024토큰 이상 구성 + cache_control)
14. 모범답안 분리 생성 (초기 생성에서 제외 → 피드백 단계로 이동)
15. Batch API 초기 시딩 스크립트
16. 야간 pool 보충 배치 스케줄러 (Spring @Scheduled 또는 별도 배치)

### Phase 4 — 품질 및 사용자 경험 (로그인 후)

17. user_question_history 테이블 활성화
18. Pool 조회 시 이전 출제 질문 제외 로직
19. 질문 품질 피드백 수집 → quality_score 반영
20. 캐시 무효화 스케줄러 (InterviewType별 갱신 주기)
21. Pool 고갈 시 fallback 로직 (중복 방지 프롬프트 포함 Claude 호출)

---

## 부록 A. Claude API 호출 시 프롬프트 구조

### A.1 초기 질문 생성 (최적화 후)

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1500,
  "temperature": 0.9,
  "system": [
    {
      "type": "text",
      "text": "당신은 기술 면접관입니다... [면접관 역할 + 유형별 출제 가이드 + JSON 스키마, 1024토큰 이상]",
      "cache_control": {"type": "ephemeral"}
    }
  ],
  "messages": [
    {
      "role": "user",
      "content": "Position: BACKEND\nLevel: JUNIOR\nType: JAVA_SPRING\n질문 수: 4\n\n각 질문에 followUpStrategy(PREPARED/REALTIME)를 포함하고, PREPARED인 경우 preparedFollowUps(2-3개)도 함께 생성하세요."
    }
  ]
}
```

### A.2 초기 질문 생성 — 기대 응답 구조

```json
{
  "questions": [
    {
      "content": "Spring Bean의 라이프사이클에 대해 설명하세요",
      "category": "Spring Core",
      "evaluationCriteria": "Bean 생성부터 소멸까지의 과정을 이해하고 있는지",
      "followUpStrategy": "PREPARED",
      "preparedFollowUps": [
        {
          "content": "@PostConstruct와 InitializingBean의 차이는 무엇인가요?",
          "triggerCondition": "라이프사이클 단계를 설명한 경우"
        },
        {
          "content": "커스텀 Bean 스코프를 정의해본 경험이 있나요?",
          "triggerCondition": "Scope 관련 내용을 언급하지 않은 경우"
        }
      ]
    },
    {
      "content": "본인 프로젝트에서 Spring 트랜잭션 전파 속성을 어떻게 활용했나요?",
      "category": "Spring Transaction",
      "evaluationCriteria": "트랜잭션 전파 속성의 실제 적용 경험",
      "followUpStrategy": "REALTIME",
      "preparedFollowUps": null
    }
  ]
}
```

### A.3 REALTIME 후속 질문 생성

```json
{
  "model": "claude-haiku-4-5-20251001",
  "max_tokens": 200,
  "temperature": 0.7,
  "system": "당신은 기술 면접관입니다. 아래 질문과 지원자의 답변을 보고, 답변에서 부족하거나 더 깊이 확인해야 할 부분을 파고드는 꼬리 질문 1개를 생성하세요.\nJSON 형식: {\"content\": \"질문\", \"evaluationCriteria\": \"평가기준\"}",
  "messages": [
    {
      "role": "user",
      "content": "[이력서 관련 섹션]\n띵동 프로젝트: 대학교 동아리 플랫폼, MAU 5000, SSR 마이그레이션...\n\n[메인 질문]\n띵동 프로젝트에서 성능 최적화를 어떻게 진행했나요?\n\n[지원자 답변]\nLCP를 3.1초에서 1초로 줄였습니다. Next.js SSR로 마이그레이션하고...\n\n[평가기준]\n성능 병목 분석과 최적화 전략의 구체성"
    }
  ]
}
```

---

## 부록 B. 핵심 클래스 구조 (참고용)

```
com.rehearse.interview.question
├── domain
│   ├── QuestionPool.java           // question_pool 엔티티
│   ├── PreparedFollowUp.java       // prepared_follow_up 엔티티
│   ├── UserQuestionHistory.java    // user_question_history 엔티티
│   ├── CacheStrategy.java          // enum: CACHEABLE, FRESH
│   └── FollowUpStrategy.java       // enum: PREPARED, REALTIME
│
├── repository
│   ├── QuestionPoolRepository.java
│   ├── PreparedFollowUpRepository.java
│   └── UserQuestionHistoryRepository.java
│
├── service
│   ├── InterviewCreationService.java       // 면접 생성 오케스트레이터 (트랜잭션 경계 관리)
│   ├── CacheableQuestionProvider.java      // pool 조회 + stampede 방어 + fallback
│   ├── FreshQuestionProvider.java          // Claude 실시간 호출
│   ├── FollowUpQuestionService.java        // 후속 질문 분기 처리 (PREPARED/REALTIME)
│   ├── QuestionPoolService.java            // pool 저장/조회/카테고리 분산 선택
│   └── QuestionPoolReplenishScheduler.java // 야간 보충 배치
│
├── client
│   ├── ClaudeApiClient.java                // Claude API 호출 (prompt caching 포함)
│   └── ClaudeBatchClient.java              // Batch API 호출
│
└── config
    ├── CacheStrategyConfig.java            // InterviewType → CacheStrategy 매핑
    └── CacheInvalidationConfig.java        // InterviewType별 갱신 주기 설정
```

---

## 부록 C. v1 → v2 변경 이력

| 항목 | v1 | v2 | 변경 근거 |
|---|---|---|---|
| Cache Stampede 방어 | 없음 | ConcurrentHashMap 기반 인메모리 락 추가 (3.5항) | 서비스 초기 동시 요청 시 중복 Claude 호출 방지. Redis 분산 락은 현재 스택에 Redis가 없으므로 오버엔지니어링 |
| @Transactional 전파 | 미언급 | 병렬 처리 시 트랜잭션 경계 명시 (4.3항) | CompletableFuture 워커 스레드에서 Spring ThreadLocal 트랜잭션이 전파되지 않는 문제 |
| 카테고리 분산 | 없음 | Round-robin 셔플링 로직 추가 (3.4항) | pool에서 랜덤 선택 시 특정 카테고리에 질문이 몰리는 문제 방지 |
| ORDER BY RAND() 주의 문구 | "수만 개가 되면 성능 이슈" | 삭제 | soft cap 200개이므로 성능 이슈 없음 |
| PREPARED 후속 질문 선택 | 옵션 A 권장 + 옵션 B, C 병기 | 옵션 A 단일 권장 + fallback display_order | 후보 2~3개에 LLM 호출은 비효율적. triggerCondition 규칙 매칭 실패 시 display_order fallback으로 충분 |
| Batch API 토큰 한도 | 미언급 | 티어별 한도 사전 확인 주의 추가 (6.2항) | 초기 시딩 시 계정 한도 초과 가능성 |
| 다국어 캐시 키 | 없음 | 미반영 | YAGNI. 한국어 MVP 단계에서 시기상조. 향후 필요 시 캐시 키 마이그레이션 용이 |
