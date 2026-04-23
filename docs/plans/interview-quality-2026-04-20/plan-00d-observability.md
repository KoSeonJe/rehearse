# Plan 00d: Observability (Phase 0) `[parallel:00c]`

> 상태: Draft
> 작성일: 2026-04-20
> 수정일: 2026-04-23 (Smoke Eval 섹션 전면 삭제 — A/B 측정 인프라 축소 결정. Micrometer/APM 부분만 유지)
> 주차: W2 후반 (2일)
> 해결 RC: RC4(W1-W3 회귀 방어 공백 + APM 부재)

## Why

plan-01~07 배포 중 턴당 LLM 호출 수가 1→3~4회로 증가하는데 APM 메트릭 없이 배포하면 비용/지연 문제를 사후에만 감지할 수 있다. Micrometer 표준 태그를 모든 신규 호출에 의무화하고 최소 대시보드(호출 수/지연/실패율/캐시 히트율/Fallback 발동률)를 준비함으로써 배포 중 회귀를 빠르게 감지한다.

회귀 감지를 위한 자동화 Judge(Smoke Eval, J1 Judge, 골든셋)는 2026-04-23 결정에 따라 이 스프린트에서 도입하지 않는다. 회귀 판단은 `MANUAL_AB_PROTOCOL.md` 의 수동 비교 방식으로 대체한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` | 신규. Micrometer Timer + Counter 래퍼 |
| `backend/src/main/resources/application.yml` | `management.metrics.export.prometheus.enabled: true` 확인 및 활성화 |
| `docs/plans/interview-quality-2026-04-20/OBSERVABILITY.md` | 신규. Grafana 쿼리 예시 + Alert 임계치 |

## 상세

### Micrometer 표준 (RC4 핵심)

`AiCallMetrics.java`:
```java
@Component
public class AiCallMetrics {
    private final MeterRegistry registry;

    public <T> T recordChat(String callType, String model, String provider, Supplier<ChatResponse> call) {
        Timer.Sample sample = Timer.start(registry);
        ChatResponse resp = null;
        try {
            resp = call.get();
            return (T) resp;
        } finally {
            sample.stop(Timer.builder("rehearse.ai.call.duration")
                .tag("call.type", callType)
                .tag("model", model)
                .tag("provider", provider)
                .tag("cache.hit", String.valueOf(resp != null && resp.cacheHit()))
                .tag("fallback", String.valueOf(resp != null && resp.fallbackUsed()))
                .register(registry));
            if (resp != null) {
                registry.counter("rehearse.ai.call.tokens.input", "call.type", callType).increment(resp.inputTokens());
                registry.counter("rehearse.ai.call.tokens.output", "call.type", callType).increment(resp.outputTokens());
                registry.counter("rehearse.ai.call.tokens.cached", "call.type", callType).increment(resp.cachedTokens());
            }
        }
    }
}
```

plan-00b의 `ResilientAiClient.chat()` 내부에서 `aiCallMetrics.recordChat(...)` 감쌈 → 모든 신규 호출 자동 태깅.

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

1. 로컬 `/actuator/prometheus` 에서 `rehearse_ai_call_duration_seconds` + `rehearse_runtime_state_cache_{hits,misses,evictions}` (plan-00c Caffeine) 메트릭 노출 확인
2. plan-00b의 `chat()` 호출 1회에 대해 Timer + Counter 모두 기록되는지 통합 테스트
3. OBSERVABILITY.md의 쿼리가 문법적으로 유효(Grafana 임시 import해서 에러 없음)
4. `progress.md` 00d → Completed
