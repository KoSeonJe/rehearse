# Plan 00d: Observability + Eval Smoke (Phase 0) `[parallel:00c]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W2 후반 (2일)
> 해결 RC: RC4(W1-W3 회귀 방어 공백 + APM 부재)

## Why

plan-10(Eval Harness)은 W7(기존 W4에서 재조정)에야 완성. 그 사이 plan-01~07 배포 중 회귀가 발생해도 **사용자 컴플레인이 유일한 감지 경로**. 또한 턴당 LLM 호출 수가 1→3~4회로 증가하는데 APM 메트릭 없이 배포하면 비용/지연 문제를 사후 감지.

**근본 해결**: (a) Eval의 **smoke subset**(골든셋 5개 + J1 Judge 1개)을 W2에 미리 만들어 매 plan 머지 전 로컬 회귀 체크, (b) Micrometer 표준 태그(REMEDIATION.md 참조)를 모든 신규 호출에 의무화, (c) 최소 대시보드(호출 수/지연/실패율/캐시 히트율/Fallback 발동률).

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `eval/golden-sets/smoke/gs_s01_happy_path.yaml` ~ `gs_s05_clarify.yaml` | 신규 smoke 5개 (happy 3 + clarify 1 + give_up 1) |
| `eval/judges/j1-followup-relevance.txt` | 신규. plan-10에서 최종화, 여기선 초안 |
| `eval/scripts/smoke.py` | 신규. 골든셋 5개 × J1 실행 후 summary 출력. 실패 시 exit 1 |
| `eval/README.md` | 신규. 사용법 |
| `eval/requirements.txt` | 신규. `openai`, `anthropic`, `pyyaml` |
| `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` | 신규. Micrometer Timer + Counter 래퍼 |
| `backend/src/main/resources/application.yml` | `management.metrics.export.prometheus.enabled: true` 확인 및 활성화 |
| `docs/plans/interview-quality-2026-04-20/OBSERVABILITY.md` | 신규. Grafana 쿼리 예시 + Alert 임계치 |
| `.github/workflows/eval-smoke.yml` (선택, 미구현도 가능) | **미작성** — 로컬 실행 우선, CI 자동화는 Out of Scope |

## 상세

### Smoke 골든셋 (5개)

각 케이스는 plan-10의 본 골든셋 스키마와 호환. 단 **단일 턴 시나리오**만 포함 (실행 시간 ≤ 2분 목표).

```yaml
# eval/golden-sets/smoke/gs_s01_happy_path.yaml
case_id: gs_s01
category: concept
coverage_class: happy_path
initial_question:
  content: "Spring @Transactional이 언제 롤백되는지 설명해주세요"
  referenceType: CONCEPT
user_turns:
  - turn: 1
    user_utterance: "RuntimeException에서 롤백됩니다"
    expected_intent: ANSWER
    expected_behavior:
      follow_up_should: ["Checked exception", "unstated assumption"]
      follow_up_should_not: ["무관한 관점 점프"]
```

### J1 Judge 초안 (plan-10에서 완성)
```
# eval/judges/j1-followup-relevance.txt (초안)
당신은 꼬리질문이 사용자 답변과 얼마나 잘 연결되는지 평가합니다.
3차원 (relevance / depth_appropriateness / conversational_flow) 각 1-5점.
JSON 출력:
{"relevance": {"score": 4, "reasoning": "..."}, ..., "overall": 3.7}
```

### smoke.py 실행
```bash
cd eval && python scripts/smoke.py --backend-url http://localhost:8080
# 출력:
# gs_s01: J1 overall 4.2 ✓
# gs_s02: J1 overall 3.8 ✓ (threshold 3.5)
# ...
# Summary: 5/5 passed (avg 4.0)
# Exit 0 if all >= 3.5, else 1
```

각 plan(01, 02, 03 등) 머지 전 수동으로 실행 → 리그레션 있으면 PR 차단(수동). 자동 CI는 Out of Scope.

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
```

### Alert 임계치 (Out of Scope: 실제 설정 — 문서만)
- Fallback 발동률 > 5% 5분 지속 → OpenAI 장애 의심
- p95 latency > 8s → 성능 회귀
- 캐시 히트율 < 50% (L1 대상 호출) → 캐시 정책 깨짐

## 담당 에이전트

- Implement: `test-engineer` — Smoke eval 스크립트
- Implement: `devops-engineer` + `backend` — Micrometer 통합, Actuator 설정
- Review: `sre-engineer` — 메트릭 표준 적절성, Alert 임계치 합리성

## 검증

1. `eval/scripts/smoke.py` 실행 시간 ≤ 2분, 5/5 통과
2. 로컬 `/actuator/prometheus` 에서 `rehearse_ai_call_duration_seconds` 메트릭 노출 확인
3. plan-00b의 `chat()` 호출 1회에 대해 Timer + Counter 모두 기록되는지 통합 테스트
4. OBSERVABILITY.md의 쿼리가 문법적으로 유효(Grafana 임시 import해서 에러 없음)
5. Smoke eval 스크립트 README만 보고 제3자 실행 가능
6. `progress.md` 00d → Completed
