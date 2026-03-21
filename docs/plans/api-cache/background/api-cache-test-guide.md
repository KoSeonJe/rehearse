# Rehearse — 캐싱 최적화 Before/After 토큰 사용량 테스트 가이드

> 이 문서는 `rehearse-question-caching-design-v2.md`에 기술된 최적화를 적용한 전후의
> API 호출 횟수, 토큰 사용량, 비용, 응답 시간을 **정량적으로 측정**하기 위한 테스트 가이드이다.

---

## 1. 측정 대상 메트릭

### 1.1 Claude API 응답의 usage 필드

모든 Claude API 응답에는 `usage` 객체가 포함된다. 이 필드를 반드시 로깅하여 수집한다.

```json
{
  "usage": {
    "input_tokens": 50,              // 캐시 미적용 입력 토큰 (cache breakpoint 이후)
    "output_tokens": 300,             // 출력 토큰
    "cache_creation_input_tokens": 1200, // 캐시에 새로 저장된 입력 토큰 (cache write)
    "cache_read_input_tokens": 0      // 캐시에서 읽은 입력 토큰 (cache hit)
  }
}
```

**총 입력 토큰 계산:**
```
total_input_tokens = input_tokens + cache_creation_input_tokens + cache_read_input_tokens
```

**비용 계산 (Sonnet 4 기준, $3/1M input, $15/1M output):**
```
input_cost  = (input_tokens × $3.00
            + cache_creation_input_tokens × $3.75   // 1.25x
            + cache_read_input_tokens × $0.30)      // 0.1x
            / 1,000,000

output_cost = output_tokens × $15.00 / 1,000,000

total_cost  = input_cost + output_cost
```

**비용 계산 (Haiku 4.5 기준, $0.80/1M input, $4.00/1M output):**
```
input_cost  = (input_tokens × $0.80
            + cache_creation_input_tokens × $1.00
            + cache_read_input_tokens × $0.08)
            / 1,000,000

output_cost = output_tokens × $4.00 / 1,000,000
```

### 1.2 수집해야 할 메트릭 전체 목록

| 메트릭 | 단위 | 수집 방법 |
|---|---|---|
| API 호출 횟수 | 회 | 호출 카운터 |
| input_tokens | 토큰 | API 응답 usage |
| output_tokens | 토큰 | API 응답 usage |
| cache_creation_input_tokens | 토큰 | API 응답 usage |
| cache_read_input_tokens | 토큰 | API 응답 usage |
| 총 비용 | USD | 위 공식으로 계산 |
| 응답 시간 (latency) | ms | 호출 시작~응답 완료 |
| pool 히트/미스 | boolean | pool 조회 성공 여부 |

---

## 2. 로깅 인프라 구현

### 2.1 API 호출 래퍼 — 메트릭 자동 수집

모든 Claude API 호출에 아래 래퍼를 적용하여 메트릭을 자동 수집한다.

```java
@Component
@Slf4j
public class ClaudeApiMetricsCollector {

    private final List<ApiCallMetric> metrics = new CopyOnWriteArrayList<>();

    public record ApiCallMetric(
        String testScenario,        // 테스트 시나리오 ID
        String callType,            // INITIAL_QUESTION, FOLLOW_UP_REALTIME, BATCH 등
        String model,               // claude-sonnet-4, claude-haiku-4-5 등
        int inputTokens,
        int outputTokens,
        int cacheCreationTokens,
        int cacheReadTokens,
        long latencyMs,
        double costUsd,
        LocalDateTime timestamp
    ) {}

    /**
     * Claude API 호출을 감싸고 메트릭을 자동 수집한다.
     */
    public <T> T callWithMetrics(
            String testScenario,
            String callType,
            Supplier<ClaudeApiResponse<T>> apiCall) {

        long start = System.currentTimeMillis();
        ClaudeApiResponse<T> response = apiCall.get();
        long latency = System.currentTimeMillis() - start;

        var usage = response.getUsage();
        double cost = calculateCost(response.getModel(), usage);

        ApiCallMetric metric = new ApiCallMetric(
            testScenario, callType, response.getModel(),
            usage.getInputTokens(), usage.getOutputTokens(),
            usage.getCacheCreationInputTokens(), usage.getCacheReadInputTokens(),
            latency, cost, LocalDateTime.now()
        );

        metrics.add(metric);

        log.info("[METRIC] scenario={} type={} model={} in={} out={} cache_write={} cache_read={} latency={}ms cost=${}", 
            testScenario, callType, response.getModel(),
            usage.getInputTokens(), usage.getOutputTokens(),
            usage.getCacheCreationInputTokens(), usage.getCacheReadInputTokens(),
            latency, String.format("%.6f", cost));

        return response.getBody();
    }

    /**
     * 시나리오별 집계 리포트를 출력한다.
     */
    public Map<String, ScenarioSummary> summarize() {
        return metrics.stream()
            .collect(Collectors.groupingBy(
                ApiCallMetric::testScenario,
                Collectors.collectingAndThen(Collectors.toList(), this::aggregate)));
    }

    public record ScenarioSummary(
        int totalCalls,
        int totalInputTokens,
        int totalOutputTokens,
        int totalCacheWriteTokens,
        int totalCacheReadTokens,
        long avgLatencyMs,
        double totalCostUsd
    ) {}
}
```

### 2.2 Pool 히트/미스 로깅

```java
@Slf4j
public class CacheableQuestionProvider {

    public List<Question> provide(String cacheKey, int requiredCount) {
        boolean poolSufficient = isPoolSufficient(cacheKey, requiredCount);

        log.info("[POOL] cacheKey={} required={} poolSize={} hit={}",
            cacheKey, requiredCount,
            questionPoolRepository.countByCacheKeyAndIsActiveTrue(cacheKey),
            poolSufficient);

        if (poolSufficient) {
            // pool에서 제공 — Claude 호출 0회
            return selectFromPool(cacheKey, requiredCount);
        } else {
            // Claude 호출 — metricsCollector를 통해 자동 수집됨
            return callClaudeAndSaveToPool(cacheKey, requiredCount);
        }
    }
}
```

---

## 3. 테스트 시나리오

### 3.1 시나리오 매트릭스

총 6개 시나리오로 Before/After를 비교한다.
각 시나리오를 **3회 반복 실행**하여 평균값을 사용한다.

| ID | 시나리오 | 면접 유형 | 질문 수 | 목적 |
|---|---|---|---|---|
| S1 | Cacheable Only — Pool 비어있음 | BACKEND:JUNIOR:JAVA_SPRING | 5 | Lazy 축적 첫 호출 측정 |
| S2 | Cacheable Only — Pool 충분 | BACKEND:JUNIOR:JAVA_SPRING | 5 | 캐시 히트 시 절감량 측정 |
| S3 | Fresh Only | BACKEND:JUNIOR:RESUME_BASED | 5 | 이력서 기반, prompt caching 효과 측정 |
| S4 | Mixed (Cacheable + Fresh) | JAVA_SPRING + RESUME_BASED | 6 (3+3) | 병렬 처리 + 부분 캐싱 효과 측정 |
| S5 | 후속 질문 — PREPARED | (S2 면접의 질문에 대해) | 5 | 사전 생성 후속 질문, Claude 호출 0회 확인 |
| S6 | 후속 질문 — REALTIME | (S3 면접의 질문에 대해) | 5 | Haiku + 입력 슬리밍 효과 측정 |

### 3.2 시나리오별 테스트 상세

#### S1: Cacheable — Pool 비어있음 (Cold Start)

```
사전 조건: question_pool 테이블이 비어있거나, 해당 cache_key에 질문이 없는 상태
입력: Position=BACKEND, Level=JUNIOR, InterviewType=[JAVA_SPRING], 질문 수=5

기대 동작 (Before): Claude API 1회 호출
기대 동작 (After):  Claude API 1회 호출 + pool에 저장 (첫 호출은 동일)

측정 포인트:
  - 초기 질문 생성의 input_tokens, output_tokens
  - prompt caching 적용 시 cache_creation_input_tokens
  - pool에 저장된 질문 수 확인
```

#### S2: Cacheable — Pool 충분 (Warm Cache)

```
사전 조건: S1을 실행하여 pool에 질문이 15개 이상 축적된 상태
입력: Position=BACKEND, Level=JUNIOR, InterviewType=[JAVA_SPRING], 질문 수=5

기대 동작 (Before): Claude API 1회 호출 (매번 새로 생성)
기대 동작 (After):  Claude API 0회 (pool에서 제공)

측정 포인트:
  - API 호출 횟수 = 0 확인
  - 응답 시간 (DB 조회만, 수십ms 이하 기대)
  - 토큰 사용량 = 0 확인
```

#### S3: Fresh Only (이력서 기반)

```
사전 조건: 테스트용 이력서 텍스트 준비 (일관된 측정을 위해 고정)
입력: Position=BACKEND, Level=JUNIOR, InterviewType=[RESUME_BASED], 이력서=고정 텍스트, 질문 수=5

기대 동작 (Before): Claude API 1회 (시스템 프롬프트 + 이력서 전문 매번 전송)
기대 동작 (After):  Claude API 1회 (시스템 프롬프트 + 이력서 prompt cached)

측정 포인트:
  - 1차 호출: cache_creation_input_tokens (cache write)
  - 2차 호출 (동일 이력서): cache_read_input_tokens (cache hit)
  - 모범답안 분리 전후 output_tokens 비교
```

#### S4: Mixed (Cacheable + Fresh 병렬)

```
사전 조건: JAVA_SPRING pool 충분, 이력서 준비
입력: InterviewType=[JAVA_SPRING, RESUME_BASED], 질문 수=6 (3+3)

기대 동작 (Before): Claude API 1회 (전체 6개 한 번에 생성)
기대 동작 (After):  JAVA_SPRING → pool 0회, RESUME_BASED → Claude 1회 (3개만 생성)

측정 포인트:
  - API 호출 횟수: Before 1회 vs After 1회 (같지만 토큰 절반)
  - output_tokens: Before ~2500 (6개) vs After ~750 (3개만 생성)
  - 전체 응답 시간 (병렬 처리 효과 확인)
```

#### S5: 후속 질문 — PREPARED

```
사전 조건: S2에서 생성된 면접, 각 질문에 preparedFollowUps가 저장된 상태
입력: 각 메인 질문에 대해 모의 사용자 답변 (고정 텍스트)

기대 동작 (Before): Claude API 5회 (매 질문마다 후속 질문 생성)
기대 동작 (After):  Claude API 0회 (prepared_follow_up 테이블에서 제공)

측정 포인트:
  - API 호출 횟수: Before 5회 vs After 0회
  - 총 토큰: Before ~10,000 vs After 0
  - 응답 시간: Before 수초×5 vs After 수십ms×5
```

#### S6: 후속 질문 — REALTIME (Haiku)

```
사전 조건: S3에서 생성된 면접 (이력서 기반)
입력: 각 메인 질문에 대해 모의 사용자 답변 (고정 텍스트)

기대 동작 (Before): Sonnet 5회 (전체 시스템 프롬프트 + 이력서 전문 매번)
기대 동작 (After):  Haiku 5회 (축소 프롬프트 + 이력서 섹션만 + prompt caching)

측정 포인트:
  - 모델 변경 효과: Sonnet vs Haiku 비용 차이
  - 입력 슬리밍 효과: 입력 토큰 ~1500 → ~500
  - prompt caching 효과: 2번째~5번째 호출의 cache_read_input_tokens
  - 출력 토큰 제한 효과: ~300 → ~150
```

---

## 4. 테스트 실행 방법

### 4.1 테스트 데이터 준비

```java
public class CachingTestFixtures {

    // 모든 테스트에서 동일하게 사용할 고정 데이터
    public static final String TEST_RESUME_TEXT = """
        [경력]
        - 토스 Backend 개발자 (2024.07 ~ 현재)
          - 결제 시스템 MSA 전환, TPS 3,000 달성
          - Spring WebFlux 기반 비동기 처리 파이프라인 구축
        
        [프로젝트]
        - 띵동: 대학교 동아리 플랫폼, MAU 5,000
          - Next.js SSR 마이그레이션 (LCP 3.1s → 1s)
          - Lambda@Edge 이미지 최적화
        
        [기술 스택]
        Java, Spring Boot, JPA, MySQL, AWS, Docker, Kubernetes
        """;

    // S5, S6에서 사용할 모의 사용자 답변
    public static final Map<String, String> MOCK_USER_ANSWERS = Map.of(
        "GC 설명", "Java GC는 Young Generation과 Old Generation으로 나뉘어 관리됩니다. Minor GC와 Major GC가 있고...",
        "Spring Bean 라이프사이클", "@PostConstruct로 초기화하고 @PreDestroy로 정리합니다...",
        "프로젝트 성능 최적화", "LCP를 3.1초에서 1초로 줄였습니다. SSR 마이그레이션과 이미지 최적화를 진행했고..."
    );
}
```

### 4.2 테스트 실행 클래스

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CachingOptimizationBenchmarkTest {

    @Autowired private ClaudeApiMetricsCollector metricsCollector;
    @Autowired private InterviewCreationService interviewService;
    @Autowired private FollowUpQuestionService followUpService;
    @Autowired private QuestionPoolRepository questionPoolRepository;

    // ============================================================
    // Before 시나리오 — 최적화 적용 전 (기존 로직)
    // ============================================================

    @Test @Order(1)
    void before_S1_cacheable_cold() {
        // pool을 사용하지 않고, 항상 Claude를 호출하는 기존 로직
        interviewService.createInterviewLegacy(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(JAVA_SPRING), 5));
    }

    @Test @Order(2)
    void before_S2_cacheable_warm() {
        // 기존 로직은 pool이 있든 없든 항상 Claude 호출
        interviewService.createInterviewLegacy(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(JAVA_SPRING), 5));
    }

    @Test @Order(3)
    void before_S3_fresh() {
        interviewService.createInterviewLegacy(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(RESUME_BASED), 5,
                TEST_RESUME_TEXT));
    }

    @Test @Order(4)
    void before_S5_followup() {
        // 기존: 매 질문마다 Sonnet으로 후속 질문 생성
        Interview interview = getLatestInterview();
        for (Question q : interview.getQuestions()) {
            followUpService.generateFollowUpLegacy(q, MOCK_USER_ANSWERS.get(q.getCategory()));
        }
    }

    // ============================================================
    // After 시나리오 — 최적화 적용 후
    // ============================================================

    @Test @Order(10)
    void after_S1_cacheable_cold() {
        questionPoolRepository.deleteAllByCacheKey("BACKEND:JUNIOR:JAVA_SPRING");
        interviewService.createInterview(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(JAVA_SPRING), 5));
    }

    @Test @Order(11)
    void after_S2_cacheable_warm() {
        // S1에서 pool이 축적된 상태 — Claude 호출 0회 기대
        interviewService.createInterview(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(JAVA_SPRING), 5));
    }

    @Test @Order(12)
    void after_S3_fresh() {
        interviewService.createInterview(
            InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(RESUME_BASED), 5,
                TEST_RESUME_TEXT));
    }

    @Test @Order(13)
    void after_S4_mixed() {
        interviewService.createInterview(
            InterviewCreateRequest.of(BACKEND, JUNIOR,
                List.of(JAVA_SPRING, RESUME_BASED), 6, TEST_RESUME_TEXT));
    }

    @Test @Order(14)
    void after_S5_followup_prepared() {
        Interview interview = getLatestCacheableInterview();
        for (Question q : interview.getQuestions()) {
            followUpService.generateFollowUp(q, MOCK_USER_ANSWERS.get(q.getCategory()));
        }
    }

    @Test @Order(15)
    void after_S6_followup_realtime() {
        Interview interview = getLatestFreshInterview();
        for (Question q : interview.getQuestions()) {
            followUpService.generateFollowUp(q, MOCK_USER_ANSWERS.get(q.getCategory()));
        }
    }

    // ============================================================
    // 리포트 출력
    // ============================================================

    @Test @Order(99)
    void printReport() {
        metricsCollector.summarize().forEach((scenario, summary) -> {
            System.out.printf("""
                === %s ===
                호출 횟수:    %d
                입력 토큰:    %d (cache_write: %d, cache_read: %d)
                출력 토큰:    %d
                평균 레이턴시: %dms
                총 비용:      $%.6f
                ===========================
                """,
                scenario, summary.totalCalls(),
                summary.totalInputTokens(), summary.totalCacheWriteTokens(),
                summary.totalCacheReadTokens(), summary.totalOutputTokens(),
                summary.avgLatencyMs(), summary.totalCostUsd());
        });
    }
}
```

---

## 5. 기대 결과 및 비교 템플릿

### 5.1 결과 기록 테이블

각 시나리오 실행 후 아래 테이블을 채운다. (3회 평균값)

#### 초기 질문 생성

| 시나리오 | 구분 | API 호출 | input_tokens | cache_write | cache_read | output_tokens | latency(ms) | cost(USD) |
|---|---|---|---|---|---|---|---|---|
| S1 Cold | Before | | | | | | | |
| S1 Cold | After | | | | | | | |
| S2 Warm | Before | | | | | | | |
| S2 Warm | After | 0 (기대) | 0 | 0 | 0 | 0 | <50 | $0 |
| S3 Fresh | Before | | | | | | | |
| S3 Fresh | After | | | | | | | |
| S4 Mixed | Before | | | | | | | |
| S4 Mixed | After | | | | | | | |

#### 후속 질문 생성 (질문 5개 합산)

| 시나리오 | 구분 | API 호출 | 모델 | input_tokens | cache_write | cache_read | output_tokens | latency(ms) | cost(USD) |
|---|---|---|---|---|---|---|---|---|---|
| S5 PREPARED | Before | 5 | Sonnet | | | | | | |
| S5 PREPARED | After | 0 (기대) | - | 0 | 0 | 0 | 0 | <50 | $0 |
| S6 REALTIME | Before | 5 | Sonnet | | | | | | |
| S6 REALTIME | After | 5 | Haiku | | | | | | |

### 5.2 기대 절감량 (추정)

| 시나리오 | 호출 절감 | 입력 토큰 절감 | 출력 토큰 절감 | 비용 절감 | 레이턴시 절감 |
|---|---|---|---|---|---|
| S2 (Warm Cache) | 100% (1→0) | 100% | 100% | 100% | ~99% (수초→수십ms) |
| S3 (Fresh + Caching) | 0% (1→1) | 10-30% (prompt cache) | 40-50% (모범답안 분리) | 30-40% | 0-10% |
| S4 (Mixed) | 0% (1→1) | 50-60% (절반만 생성) | 50-60% | 50-60% | 10-30% (병렬) |
| S5 (PREPARED) | 100% (5→0) | 100% | 100% | 100% | ~99% |
| S6 (REALTIME Haiku) | 0% (5→5) | 60-70% (슬리밍) | 50% (출력 제한) | 70-80% (Haiku) | 20-40% |

---

## 6. 검증 체크리스트

### 6.1 기능 정확성 검증

테스트 중 비용 절감뿐 아니라 **질문 품질이 유지되는지** 반드시 확인한다.

```
□ S2: pool에서 가져온 질문이 JAVA_SPRING 범위에 맞는지 확인
□ S2: 카테고리(JVM, Collection, Spring Core 등)가 고르게 분포되는지 확인
□ S3: 모범답안 분리 후에도 evaluationCriteria가 충분히 구체적인지 확인
□ S4: cacheable(3개) + fresh(3개) 합산 시 question_order가 올바른지 확인
□ S5: PREPARED 후속 질문이 사용자 답변과 맥락이 맞는지 확인
□ S6: Haiku 후속 질문의 품질이 Sonnet과 비교하여 수용 가능한지 확인
□ Stampede: 동시 10명 요청 시 pool에 중복 질문이 쌓이지 않는지 확인
□ 트랜잭션: 병렬 처리 중 한쪽 실패 시 다른 쪽에 영향이 없는지 확인
```

### 6.2 성능 검증

```
□ S2 응답 시간이 100ms 미만인지 확인
□ S4 전체 응답 시간이 fresh 유형 단독 호출 시간과 유사한지 확인 (병렬 효과)
□ S6에서 prompt caching이 실제로 동작하는지 (2번째 호출부터 cache_read > 0)
□ ORDER BY RAND()가 pool 200개 기준으로 10ms 미만에 실행되는지 확인
```

### 6.3 비용 검증

```
□ S1 After에서 pool에 질문이 저장되었는지 DB 확인
□ S2 After에서 Claude API 호출이 실제로 0회인지 로그 확인
□ S5 After에서 Claude API 호출이 실제로 0회인지 로그 확인
□ S6 After에서 model 필드가 "claude-haiku-4-5-20251001"인지 확인
□ 전체 시나리오의 비용 합계가 Before 대비 70% 이상 절감되는지 확인
```

---

## 7. Stampede 방어 테스트

### 7.1 동시 요청 테스트

```java
@Test
void stampede_test() throws Exception {
    // pool을 비운다
    questionPoolRepository.deleteAllByCacheKey("BACKEND:JUNIOR:JAVA_SPRING");

    int concurrentRequests = 10;
    ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
    CountDownLatch latch = new CountDownLatch(1);
    List<Future<Interview>> futures = new ArrayList<>();

    for (int i = 0; i < concurrentRequests; i++) {
        futures.add(executor.submit(() -> {
            latch.await(); // 모든 스레드가 동시에 시작
            return interviewService.createInterview(
                InterviewCreateRequest.of(BACKEND, JUNIOR, List.of(JAVA_SPRING), 5));
        }));
    }

    latch.countDown(); // 동시 실행

    List<Interview> results = futures.stream()
        .map(f -> { try { return f.get(30, TimeUnit.SECONDS); } catch (Exception e) { return null; } })
        .filter(Objects::nonNull)
        .toList();

    // 검증
    int apiCallCount = metricsCollector.getMetrics().stream()
        .filter(m -> m.testScenario().equals("stampede") && m.callType().equals("INITIAL_QUESTION"))
        .toList().size();

    long poolSize = questionPoolRepository.countByCacheKeyAndIsActiveTrue("BACKEND:JUNIOR:JAVA_SPRING");

    System.out.printf("동시 요청: %d, API 호출: %d, pool 크기: %d%n",
        concurrentRequests, apiCallCount, poolSize);

    // 기대: API 호출이 1회 (stampede 방어)
    // 실제로는 인메모리 락의 타이밍에 따라 1~2회 가능
    assertThat(apiCallCount).isLessThanOrEqualTo(2);
    assertThat(results).hasSize(concurrentRequests); // 모든 요청이 성공
}
```

---

## 8. Prompt Caching 효과 검증

### 8.1 연속 호출로 cache hit 확인

```java
@Test
void prompt_caching_verification() {
    // 동일한 시스템 프롬프트로 2회 연속 호출
    // 1차: cache_creation > 0, cache_read = 0
    // 2차: cache_creation = 0, cache_read > 0

    var response1 = claudeApiClient.createQuestions(
        BACKEND, JUNIOR, RESUME_BASED, 3, TEST_RESUME_TEXT);
    var usage1 = response1.getUsage();

    // 5분 TTL 내에 2차 호출
    var response2 = claudeApiClient.createQuestions(
        BACKEND, JUNIOR, RESUME_BASED, 3, TEST_RESUME_TEXT);
    var usage2 = response2.getUsage();

    System.out.printf("""
        1차 호출: input=%d, cache_write=%d, cache_read=%d
        2차 호출: input=%d, cache_write=%d, cache_read=%d
        """,
        usage1.getInputTokens(), usage1.getCacheCreationInputTokens(), usage1.getCacheReadInputTokens(),
        usage2.getInputTokens(), usage2.getCacheCreationInputTokens(), usage2.getCacheReadInputTokens());

    // 2차 호출에서 cache_read > 0 이면 prompt caching 동작 확인
    assertThat(usage2.getCacheReadInputTokens()).isGreaterThan(0);
    assertThat(usage2.getCacheCreationInputTokens()).isEqualTo(0);
}
```

---

## 9. 최종 리포트 템플릿

모든 테스트 완료 후 아래 형식으로 최종 리포트를 작성한다.

```markdown
# 캐싱 최적화 Before/After 결과 리포트

## 실행 환경
- 날짜: YYYY-MM-DD
- 모델: claude-sonnet-4-20250514, claude-haiku-4-5-20251001
- pool 상태: (시딩 여부, pool 크기)

## 요약

| 구분 | Before 합계 | After 합계 | 절감율 |
|---|---|---|---|
| API 호출 | N회 | N회 | N% |
| 입력 토큰 | N | N | N% |
| 출력 토큰 | N | N | N% |
| 비용 | $N.NNNNNN | $N.NNNNNN | N% |
| 평균 레이턴시 | Nms | Nms | N% |

## 시나리오별 상세
(위 5.1 테이블 채운 것 첨부)

## 품질 검증 결과
(6.1 체크리스트 통과 여부)

## 특이사항 및 발견
- ...
```
