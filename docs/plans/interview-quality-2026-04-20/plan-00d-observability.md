# Plan 00d: Observability (Phase 0) `[parallel:00c]`

> 상태: Completed (2026-04-24, S3c)
> 산출물: `OBSERVABILITY.md` + `AiCallMetrics` (Timer + 4 Counter) + prometheus registry 의존성
> 작성일: 2026-04-20
> 수정일:
> - 2026-04-23 — Smoke Eval 섹션 전면 삭제 (A/B 측정 인프라 축소 결정). Micrometer/APM 부분만 유지
> - 2026-04-24 (S3c) — 실구현(2-arg `recordChat` + 응답 후추출)에 맞춰 본문 코드/파일 목록 갱신. 토큰 Counter 4 종(input/output/cached.read/cached.write) 확정. prometheus registry 의존성 명시
> 완료일: 2026-04-24
> 주차: W2 후반 (2일)
> 해결 RC: RC4(W1-W3 회귀 방어 공백 + APM 부재)

## Why

plan-01~07 배포 중 턴당 LLM 호출 수가 1→3~4회로 증가하는데 APM 메트릭 없이 배포하면 비용/지연 문제를 사후에만 감지할 수 있다. Micrometer 표준 태그를 모든 신규 호출에 의무화하고 최소 대시보드(호출 수/지연/실패율/캐시 히트율/Fallback 발동률)를 준비함으로써 배포 중 회귀를 빠르게 감지한다.

회귀 감지를 위한 자동화 Judge(Smoke Eval, J1 Judge, 골든셋)는 2026-04-23 결정에 따라 이 스프린트에서 도입하지 않는다. 회귀 판단은 `MANUAL_AB_PROTOCOL.md` 의 수동 비교 방식으로 대체한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | `runtimeOnly("io.micrometer:micrometer-registry-prometheus")` 추가 — Spring Boot 3.x 는 actuator 만으로는 `/actuator/prometheus` 노출 안 함 |
| `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` | Micrometer Timer + 4 Counter 래퍼 (S2 에서 Timer 만 선행 머지 → S3c 에서 Counter 4 종 추가) |
| `backend/src/main/resources/application.yml` | `management.endpoints.web.exposure.include: health,info,prometheus` + `metrics.export.prometheus.enabled: true` (선행 머지됨) |
| `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` | `chat()` 에서 `aiCallMetrics.recordChat()` 래핑 (선행 머지됨, L67) |
| `backend/src/main/java/com/rehearse/api/config/RuntimeCacheConfig.java` | `CaffeineCacheMetrics.monitor()` 로 `rehearse.runtime.state.*` 노출 (선행 머지됨, plan-00c S3) |
| `backend/src/test/java/com/rehearse/api/infra/ai/AiCallMetricsTest.java` | `SimpleMeterRegistry` 기반 단위 테스트. Timer 태그 6 종 + Counter 4 종 + 0 토큰/예외 경로 검증 |
| `docs/plans/interview-quality-2026-04-20/OBSERVABILITY.md` | Grafana 쿼리 + Alert 임계치 + 배포 회귀 체크리스트 |

## 상세

### Micrometer 표준 (RC4 핵심)

**시그니처 확정 (2026-04-24 S3c)**: 초안의 `recordChat(callType, model, provider, Supplier)` 4-arg 대신 **`recordChat(String callType, Callable<ChatResponse> callable)` 2-arg** 로 구현.

- 근거: `ResilientAiClient` 는 OpenAI primary → Claude fallback 패턴. **호출 전엔 최종 provider/model 이 확정되지 않는다** → `ChatResponse` 수신 후 `response.provider()` / `response.model()` 에서 후추출하는 편이 정확.
- `ChatResponse.Usage` 필드는 OpenAI prompt caching / Claude cache_control 스펙에 맞춰 4 필드: `inputTokens` / `outputTokens` / `cacheReadTokens` / `cacheWriteTokens`.

`AiCallMetrics.java` (요약):
```java
@Component
@RequiredArgsConstructor
public class AiCallMetrics {
    public static final String TIMER_NAME = "rehearse.ai.call.duration";
    public static final String TOKENS_INPUT = "rehearse.ai.call.tokens.input";
    public static final String TOKENS_OUTPUT = "rehearse.ai.call.tokens.output";
    public static final String TOKENS_CACHED_READ = "rehearse.ai.call.tokens.cached.read";
    public static final String TOKENS_CACHED_WRITE = "rehearse.ai.call.tokens.cached.write";

    private final MeterRegistry meterRegistry;

    public ChatResponse recordChat(String callType, Callable<ChatResponse> callable) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success", provider = "unknown", model = "unknown",
               cacheHit = "unknown", fallback = "false";
        ChatResponse response = null;
        try {
            response = callable.call();
            provider = response.provider();
            model = response.model();
            cacheHit = String.valueOf(response.cacheHit());
            fallback = String.valueOf(response.fallbackUsed());
            return response;
        } catch (Exception e) {
            outcome = "failure";
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        } finally {
            sample.stop(Timer.builder(TIMER_NAME)
                .tag("call.type", callType).tag("model", model).tag("provider", provider)
                .tag("cache.hit", cacheHit).tag("fallback", fallback).tag("outcome", outcome)
                .register(meterRegistry));
            if (response != null && response.usage() != null) {
                recordTokenUsage(callType, provider, model, response.usage()); // 4 Counter
            }
        }
    }
    // incrementCounter: amount <= 0 이면 미등록 (무의미 시리즈 방지)
}
```

`ResilientAiClient.chat()` L67:
```java
return aiCallMetrics.recordChat(request.callType(), () -> doChat(request));
```

### OBSERVABILITY.md — 최소 대시보드 쿼리 예시 (Grafana/PromQL)

```
# 턴당 LLM 호출 수 증가 확인 (P95)
histogram_quantile(0.95, sum(rate(rehearse_ai_call_duration_seconds_bucket[5m])) by (le, call_type))

# Fallback 발동률
sum(rate(rehearse_ai_call_duration_seconds_count{fallback="true"}[5m])) /
sum(rate(rehearse_ai_call_duration_seconds_count[5m]))

# 캐시 히트율
sum(rate(rehearse_ai_call_duration_seconds_count{cache_hit="true"}[5m])) /
sum(rate(rehearse_ai_call_duration_seconds_count[5m]))

# 분당 토큰 사용량 (비용 추정)
sum(rate(rehearse_ai_call_tokens_input[1m])) by (call_type)

# Runtime State 캐시 히트율 (plan-00c Caffeine — 추가 2026-04-22)
sum(rate(rehearse_runtime_state_cache_hits[5m])) /
(sum(rate(rehearse_runtime_state_cache_hits[5m])) + sum(rate(rehearse_runtime_state_cache_misses[5m])))

# Runtime State 캐시 eviction 추이 (세션 만료 패턴 파악)
sum(rate(rehearse_runtime_state_cache_evictions[5m]))
```

**plan-00c Caffeine 메트릭 노출 검증** (2026-04-22 추가):
- `InterviewRuntimeStateStore` 는 `CaffeineCacheMetrics.monitor()` 로 Micrometer 바인딩 완료
- `/actuator/prometheus` 에서 `rehearse_runtime_state_cache_hits`, `..._misses`, `..._evictions` 세 메트릭 노출 확인 필요 (본 plan 검증 시 항목 추가)

### Alert 임계치 (Out of Scope: 실제 설정 — 문서만)
- Fallback 발동률 > 5% 5분 지속 → OpenAI 장애 의심
- p95 latency > 8s → 성능 회귀
- 캐시 히트율 < 50% (L1 대상 호출) → 캐시 정책 깨짐

## 담당 에이전트

- Implement: `devops-engineer` + `backend` — Micrometer 통합, Actuator 설정
- Review: `sre-engineer` — 메트릭 표준 적절성, Alert 임계치 합리성

## 검증

1. `./gradlew test --tests "AiCallMetricsTest"` — SimpleMeterRegistry 기반 Timer 6 태그 + Counter 4 종 (success/failure/cache/0-token/누적) 단위 테스트 그린
2. 로컬 `./gradlew bootRun` → `curl -s localhost:8080/actuator/prometheus | grep rehearse_ai_call` 노출 확인 (`_duration_seconds_*` + `_tokens_{input,output,cached_read,cached_write}_total` + `rehearse_runtime_state_cache_*`)
3. `ResilientAiClient.chat()` 호출 1 회 후 Timer `count=1` + 4 Counter 값 비(非)영 검증 (통합 테스트 또는 수동)
4. OBSERVABILITY.md 쿼리 구문 유효성 확인 (PromQL 파서 통과)
5. `progress.md` 00d → Completed
