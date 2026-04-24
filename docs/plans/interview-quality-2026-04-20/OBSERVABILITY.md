# Observability — AI 호출 + Runtime State 대시보드 표준

> 상태: Completed
> 작성일: 2026-04-24 (plan-00d S3b 실행)
> 범위: AI 호출 Micrometer 메트릭 표준 + Caffeine Runtime State 캐시 메트릭 + Grafana/PromQL 쿼리 레퍼런스 + Alert 임계치 가이드
> 구현 상태: **코드는 이미 머지됨** (S2 — PR #336 AiCallMetrics / S3 — PR #338 RuntimeCacheConfig)
> 본 문서: 메트릭 계약 정의 + 대시보드 쿼리 + 회귀 감지 체크리스트 (문서 전용)

## 배경

plan-01~07 배포 중 턴당 LLM 호출 수가 1→3~4 회로 증가한다. APM 메트릭 없이 배포하면 비용·지연·fallback 사고를 **사후에만** 감지할 수 있다. Micrometer 표준 태그를 모든 신규 호출에 의무화하고 Grafana 쿼리 레퍼런스를 확정해야 배포 중 회귀를 1 분 단위로 감지할 수 있다.

자동화 Judge (Smoke Eval / 골든셋 / J1 Judge) 는 2026-04-23 결정으로 스프린트 범위에서 제외되었다. 회귀 판단은 `MANUAL_AB_PROTOCOL.md` 수동 비교로 대체하고, 본 문서의 정량 메트릭이 **정량 감지 단일 소스**다.

## 노출 메트릭

### 1. AI 호출 Timer — `rehearse.ai.call.duration`

구현 위치: `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java`
호출 지점: `ResilientAiClient.chat():67` — 모든 `chat(ChatRequest)` 호출이 본 Timer 로 자동 래핑

| 태그 | 값 | 출처 |
|------|-----|------|
| `call.type` | `ChatRequest.callType()` (예: `question_generation`, `follow_up`, `intent_classifier`, `feedback_synthesizer`) | 호출부에서 지정 |
| `model` | 실제 호출된 모델 ID (예: `gpt-4o-mini`, `claude-3-5-sonnet-latest`) | `ChatResponse.model()` |
| `provider` | `openai` / `claude` / fallback 시 전환된 provider | `ChatResponse.provider()` |
| `cache.hit` | `true` / `false` — `ChatResponse.cacheHit()` | OpenAI prompt cache / Claude cache_control |
| `fallback` | `true` / `false` — primary 실패 후 fallback 동작 여부 | `ChatResponse.fallbackUsed()` |
| `outcome` | `success` / `failure` | try-catch |

### 2. AI 호출 토큰 Counter (3 종)

| Counter | 태그 | 용도 |
|---------|-----|------|
| `rehearse.ai.call.tokens.input` | `call.type` | 입력 토큰 합계 (비용 추정) |
| `rehearse.ai.call.tokens.output` | `call.type` | 출력 토큰 합계 |
| `rehearse.ai.call.tokens.cached` | `call.type` | 캐시 히트 토큰 (OpenAI `prompt_tokens_details.cached_tokens`) |

> **참고**: 2026-04-24 현재 `AiCallMetrics.recordChat()` 은 Timer 만 기록한다. Counter 3 종은 plan-00d 본문 스펙. 추가 구현은 plan-00d 수정 PR 또는 plan-04 Context Engineering PR 에서 `ChatResponse.Usage` 파싱과 함께 기록 (현재는 `Usage.empty()` 반환 경우 존재 — S2 HANDOFF.md 미해결 이월 참조).

### 3. Runtime State 캐시 (Caffeine) — `rehearse.runtime.state.*`

구현 위치: `backend/src/main/java/com/rehearse/api/config/RuntimeCacheConfig.java:23`

```java
CaffeineCacheMetrics.monitor(registry, cache, "rehearse.runtime.state");
```

노출되는 Prometheus 메트릭 (Micrometer → Prometheus 스네이크 케이스 변환):

| Prometheus 메트릭 | 의미 |
|-------------------|------|
| `rehearse_runtime_state_cache_hits_total` | 히트 카운트 |
| `rehearse_runtime_state_cache_misses_total` | 미스 카운트 |
| `rehearse_runtime_state_cache_evictions_total` | eviction 카운트 (2h idle TTL / max 10,000) |
| `rehearse_runtime_state_cache_size` | 현재 캐시 엔트리 수 |
| `rehearse_runtime_state_cache_puts_total` | put 카운트 |

### 4. Spring Boot 기본 메트릭 (무료)

`management.endpoints.web.exposure.include: health, info, prometheus` (`application.yml`) 로 활성.

- `http_server_requests_seconds_*` — HTTP 레이턴시
- `jvm_memory_used_bytes` / `jvm_gc_*` — JVM 건강성
- `hikaricp_connections_*` — DB 커넥션 풀

## Grafana / PromQL 쿼리 레퍼런스

### A. 턴당 LLM 호출 p95 레이턴시 (call type 별)

```promql
histogram_quantile(
  0.95,
  sum(rate(rehearse_ai_call_duration_seconds_bucket[5m])) by (le, call_type)
)
```

### B. Fallback 발동률 (5 분 이동 평균)

```promql
sum(rate(rehearse_ai_call_duration_seconds_count{fallback="true"}[5m]))
/
sum(rate(rehearse_ai_call_duration_seconds_count[5m]))
```

### C. 캐시 히트율 (OpenAI prompt cache / Claude cache_control 합산)

```promql
sum(rate(rehearse_ai_call_duration_seconds_count{cache_hit="true"}[5m]))
/
sum(rate(rehearse_ai_call_duration_seconds_count[5m]))
```

### D. 분당 토큰 사용량 (비용 추정)

```promql
sum(rate(rehearse_ai_call_tokens_input_total[1m])) by (call_type)
sum(rate(rehearse_ai_call_tokens_output_total[1m])) by (call_type)
```

### E. Runtime State 캐시 히트율

```promql
sum(rate(rehearse_runtime_state_cache_hits_total[5m]))
/
(
  sum(rate(rehearse_runtime_state_cache_hits_total[5m]))
  + sum(rate(rehearse_runtime_state_cache_misses_total[5m]))
)
```

### F. Runtime State eviction 추이 (세션 만료 패턴)

```promql
sum(rate(rehearse_runtime_state_cache_evictions_total[5m]))
```

### G. 실패율 (outcome = failure)

```promql
sum(rate(rehearse_ai_call_duration_seconds_count{outcome="failure"}[5m]))
/
sum(rate(rehearse_ai_call_duration_seconds_count[5m]))
```

## Alert 임계치 가이드 (실제 설정 Out of Scope)

Prometheus Alertmanager 에 설정할 룰 초안. 실제 설정은 인프라 PR 에서.

| Alert | 조건 | 의심 원인 |
|-------|------|-----------|
| `AiCallFallbackHigh` | Query B 값 > 0.05, 5 분 지속 | OpenAI 장애 / API 키 문제 |
| `AiCallLatencyHigh` | Query A (p95) > 8 초, 5 분 지속 | 프롬프트 크기 급증 / 모델 교체 리그레션 |
| `AiCallFailureHigh` | Query G > 0.02, 5 분 지속 | JSON 파싱 실패 / 쿼터 초과 |
| `PromptCacheMiss` | Query C < 0.5 (L1 캐시 대상 call.type 한정), 10 분 지속 | 캐시 정책 깨짐 (프롬프트 구조 변경) |
| `RuntimeStateEvictionSpike` | Query F > 10/s, 5 분 지속 | 세션 트래픽 급증 / TTL 부적절 |

## 배포 중 회귀 감지 체크리스트 (plan-01~ 롤아웃 시)

매 plan PR 머지 후 배포 시 아래 순서로 확인:

1. **배포 직후 10 분**
   - [ ] Query A p95 값이 이전 배포 대비 **+2 초 이내**
   - [ ] Query G 실패율이 **이전 베이스라인 × 2 이하**
2. **배포 후 1 시간**
   - [ ] Query B fallback 발동률이 **5 % 미만** 유지
   - [ ] Query D 토큰 사용량이 **이전 대비 +50 % 이내** (plan-01~03 배포 시 호출 수 3~4 배 증가 예상 — 예외적 허용 범위 기록 필요)
3. **배포 후 1 일**
   - [ ] Query C 캐시 히트율 **L1 cacheable call.type 에서 70 % 이상**
   - [ ] `MANUAL_AB_PROTOCOL.md` 3~5 건 정성 비교 수행
4. **회귀 발견 시**
   - ECR 이전 태그로 롤백 (Runtime flag 없음 — 2026-04-23 결정)
   - Caffeine 세션 스토어 캐시 퍼지 (`/actuator/caches` 또는 pod 재시작)

## 검증 스냅샷 (수동 확인 가이드)

로컬에서 본 문서 메트릭 노출을 검증하려면:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'

# 별도 터미널
curl -s localhost:8080/actuator/prometheus \
  | grep -E "^rehearse_ai_call_duration_seconds|^rehearse_runtime_state_cache"
```

**예상 출력 (메트릭 노출 시작 전)**:
- `rehearse_runtime_state_cache_*` 는 bootRun 직후 노출됨 (캐시 빈 생성 시점)
- `rehearse_ai_call_duration_seconds_*` 는 첫 `ResilientAiClient.chat()` 호출 후 노출 (예: `POST /api/interviews/{id}/questions` 1 회 트리거 필요)

**확인 포인트**:
- `rehearse_ai_call_duration_seconds_bucket{call_type="...",le="..."}` — 히스토그램 버킷 존재
- `rehearse_ai_call_duration_seconds_count{call_type="...",cache_hit="...",fallback="...",outcome="..."}` — 7 태그 전부 채워짐
- `rehearse_runtime_state_cache_hits_total` / `_misses_total` / `_evictions_total` / `_size` 4 종 노출

> **Note**: 본 PR 에서는 로컬 bootRun 실행 권한 제한으로 라이브 출력 캡처 미첨부. 코드 상 `AiCallMetrics.java:41~48` + `RuntimeCacheConfig.java:23` 로 노출이 보장되어 있으며, PR 머지 후 스테이징에서 1 회 curl 스냅샷을 캡처해 본 섹션에 부록으로 추가할 것.

## 향후 확장 (Out of Scope)

- Counter 3 종 (`tokens.input/output/cached`) 실제 구현 — plan-00d 스펙. S2 에서 `AiCallMetrics.recordChat()` 가 Timer 만 기록. 2026-04 현재 `ChatResponse.Usage.empty()` 반환 경로 있음 → plan-04 Context Engineering PR 에서 Usage 파싱과 함께 Counter 기록 추가 권장
- Grafana 대시보드 JSON — 본 문서 쿼리 기반 대시보드 파일은 인프라 레포에서 별도 관리
- Alertmanager 룰 YAML — 임계치 가이드 표를 룰 YAML 로 변환하는 작업은 SRE 스프린트 별건

## 참고

- 마스터 플랜: `/Users/koseonje/.claude/plans/interview-quality-jolly-flask.md`
- 상세 스펙: `plan-00d-observability.md`
- 관련 구현 PR: S2 (#336 AiClient 범용화 + AiCallMetrics), S3 (#338 Session State Store + Caffeine 메트릭)
- 수동 비교 프로토콜: `MANUAL_AB_PROTOCOL.md`
- Spring Boot Actuator 문서: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics
- Micrometer Prometheus: https://micrometer.io/docs/registry/prometheus
