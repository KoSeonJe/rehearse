# Virtual Thread Migration — 부하테스트 결과 보고서

> 테스트일: 2026-03-24

## 테스트 환경

| 항목 | 값 |
|------|-----|
| OS | macOS Darwin 23.5.0 (arm64) |
| JVM | Java 21.0.4 |
| Spring Boot | 3.4.3 |
| HikariCP | Phase 0: 5.x (기본), Phase 1-2: 6.2.1 |
| DB | Docker MySQL 8.0 |
| HikariCP pool-size | 10 |
| 외부 API | Python Mock Server (Phase 0: 3s 지연, Phase 1-2: 2s 지연) |
| 테스트 데이터 | 면접 5,000건 (각 질문세트 1건, 질문 1건) |
| 부하 패턴 | k6 ramp-up: 10→50→100→200→300→500 VUs (총 ~5분) |
| 메트릭 수집 | 1초 간격 Actuator 폴링 (active/pending connections, live threads) |
| 대상 API | `POST /api/v1/interviews/{id}/follow-up` (answerText 직접 전달) |
| 처리량 기준 | **실질 처리량** = 성공(2xx) 요청수 / 테스트시간. 500 에러는 제외 |
| 에러율 기준 | 500번대 서버 에러만 카운트 (400 비즈니스, 429 rate limit 제외) |

---

# Part 1: 현실 시나리오 (외부 API RateLimiter 8 req/s)

> GPT-4o Tier 1 기준 (500 RPM = ~8 req/s). 실 서비스 운영 환경을 반영한 테스트.

## Phase 0: 원본 코드

**코드 상태**: VT 마이그레이션 이전 (`c1d5f87`)
- `generateFollowUp()`이 `@Transactional` 내부에서 외부 API 호출 (3s 지연)
- Platform Thread (Tomcat 200), RateLimiter 없음

### 병목

```
@Transactional 시작 (DB 커넥션 획득)
  → DB 조회 (~20ms)
  → 외부 API 호출 (~3s)  ← 커넥션 점유 중!
  → DB 저장 (~20ms)
@Transactional 종료 (커넥션 반환)
// 총 커넥션 점유: ~3s/요청
```

pool-size 10에서 커넥션 1개당 3s 점유 → 최대 ~3 req/s.

### 결과

| 지표 | 값 |
|------|-----|
| 총 요청 | 9,950 |
| 성공 | 820 (8%) |
| **인프라 에러율** | **91.74%** |
| **실질 처리량** | **~2.8 req/s** |
| Peak active connections | **10 (= pool-size, 상시 포화)** |
| Peak pending connections | **190** |
| Peak live threads | 215 |

**결론**: 동시 10명 수준에서 이미 pool 포화 → 서비스 장애.

---

## Phase 1: TX 분리 + RateLimiter 8 req/s

**변경점**: 3-phase TX 분리 (커넥션 점유 3s → ~40ms) + 외부 API RL 8 req/s

### 결과

| 지표 | Phase 0 | Phase 1 | 변화 |
|------|---------|---------|------|
| 성공 | 820 | **2,191** | **2.7배 증가** |
| 인프라 에러율 | 91.74% | **1.48%** | **90%p 감소** |
| 실질 처리량 | 2.8 | **~7.0 req/s** | 2.5배 증가 |
| Peak active | 10 (상시) | 10 (순간) | 점유 시간 극감 |
| Peak pending | 190 | **0** | **완전 해소** |
| Peak threads | 215 | 215 | PT 동일 |
| p95 응답 | 6.73s | 57.97s | RL 대기 큐 영향 |

**결론**: 커넥션 병목 완전 해소. 처리량이 RL 한도(8 req/s)에 도달하여 천장.

---

## Phase 2: +Virtual Thread + RateLimiter 8 req/s

**변경점**: VT 활성화 + HikariCP 6.2.1

### 결과

| 지표 | Phase 1 (PT) | Phase 2 (VT) | 변화 |
|------|-------------|-------------|------|
| 성공 | 2,191 | 2,177 | 동일 |
| 인프라 에러율 | 1.48% | 1.75% | 동일 |
| 실질 처리량 | 7.0 | **7.2 req/s** | 동일 |
| Peak pending | 0 | **1** | 안정 |
| **Peak threads** | **215** | **33** | **85% 감소** |

**결론**: RL 8 req/s가 천장이므로 PT/VT 처리량 차이 없음. VT 효과는 스레드 수 절감(215→33)에 한정. **외부 API 한도가 병목이면 VT의 동시성 이점이 발현되지 않음.**

---

## Part 1 요약

| Phase | 실질 처리량 | 인프라 에러율 | Peak pending | Peak threads |
|-------|-----------|-------------|-------------|-------------|
| 0: 원본 | 2.8 req/s | 91.74% | 190 | 215 |
| 1: TX분리+RL | **7.0 req/s** | **1.48%** | **0** | 215 |
| 2: +VT+RL | 7.2 req/s | 1.75% | 1 | **33** |

**핵심**:
- TX 분리가 핵심 개선 (에러율 91% → 1.5%)
- RL 8 req/s 환경에서 VT는 처리량 이점 없음 (스레드 절감만)
- Tier 업그레이드 시 VT 효과가 드러날 것으로 예상

---

# Part 2: 스레드 병목 검증 (외부 API RateLimiter 없음)

> RL을 제거하여 외부 API 천장 없이 PT/VT 동시성을 직접 비교.

## Phase 0: 원본 코드 (RL 없음)

| 지표 | 값 |
|------|-----|
| 성공 | 804 (8%) |
| **인프라 에러율** | **91.64%** |
| **실질 처리량** | **~2.7 req/s** |
| Peak active | **10 (상시 포화)** |
| Peak pending | **190** |
| Peak threads | 215 |

**결론**: Part 1과 동일 — `@Transactional` 내 3s 커넥션 점유로 pool 포화.

---

## Phase 1: TX 분리 + Platform Thread (RL 없음)

| 지표 | Phase 0 | Phase 1 | 변화 |
|------|---------|---------|------|
| 성공 | 804 | **9,246** | **11.5배 증가** |
| 인프라 에러율 | 91.64% | **14.93%** | **77%p 감소** |
| 실질 처리량 | 2.7 | **~31.1 req/s** | **11.5배 증가** |
| Peak active | 10 (상시) | 10 (순간) | 점유 극감 |
| Peak pending | 190 | **125** | 감소 |
| Peak threads | 215 | **215** | PT 200 + 데몬 |

**결론**: TX 분리만으로 처리량 11.5배 증가. 에러 14.93%는 500 VUs 부하에서 Tomcat 200 스레드 포화 + DB pool 경쟁.

---

## Phase 2: TX 분리 + Virtual Thread (RL 없음)

| 지표 | Phase 1 (PT) | Phase 2 (VT) | 변화 |
|------|-------------|-------------|------|
| 성공 | 9,246 | 9,206 | 동일 |
| 인프라 에러율 | 14.93% | 16.03% | 유사 |
| 실질 처리량 | 31.1 | **31.0 req/s** | 동일 |
| Peak pending | 125 | **218** | 증가 |
| **Peak threads** | **215** | **52** | **76% 감소** |

### 분석: 왜 VT에서 처리량이 증가하지 않는가?

1. **mock 서버가 병목**: Python mock 서버가 500개 동시 커넥션을 처리하는 데 한계. PT/VT 모두 mock 서버의 처리 용량(~31 req/s)에서 수렴.
2. **VT pending 증가(125→218)**: VT는 Tomcat 200 스레드 제한 없이 모든 요청을 동시에 받아들여, 더 많은 요청이 DB 커넥션을 기다림.
3. **스레드 효율은 명확**: 동일 처리량에서 스레드 76% 절감 (215→52). 메모리와 컨텍스트 스위칭 비용 감소.

---

## Part 2 요약

| Phase | 실질 처리량 | 인프라 에러율 | Peak pending | Peak threads |
|-------|-----------|-------------|-------------|-------------|
| 0: 원본 | 2.7 req/s | 91.64% | 190 | 215 |
| 1: TX분리+PT | **31.1 req/s** | **14.93%** | **125** | 215 |
| 2: TX분리+VT | 31.0 req/s | 16.03% | 218 | **52** |

---

# 종합 결론

## 각 개선의 효과

| 개선 | 효과 | 필수 여부 |
|------|------|----------|
| **TX 분리** | 처리량 11배 증가, 에러율 91%→15% | **필수** — 커넥션 pool 고갈 해소 |
| **RateLimiter** | 외부 API 과금/ban 방지, 시스템 안정화 | **필수** — 비즈니스 보호 |
| **Virtual Thread** | 스레드 76~85% 절감, 처리량 동일 | **선제 적용** — 현 단계 효과 미미 |

## VT는 왜 선제 적용하는가?

현재 외부 API 한도(GPT-4o Tier 1: 8 req/s)에서는 Tomcat 200 스레드가 포화되지 않아 VT의 동시성 이점이 드러나지 않음. 그러나:

1. **도입 비용이 거의 없음**: `spring.threads.virtual.enabled=true` + HikariCP 6.x 업그레이드
2. **API Tier 업그레이드 시 즉시 효과**: Tier 5(167 req/s)나 DeepSeek(무제한)으로 전환하면 PT 200 스레드가 병목이 되며, VT가 이를 해소
3. **되돌리기 쉬움**: 설정 한 줄로 PT 복귀 가능

## 외부 API Rate Limit 참고 (후속 질문 생성용)

| Provider | Model | 최고 Tier RPM | req/s | VT 필요 시점 |
|----------|-------|-------------|-------|-------------|
| OpenAI | GPT-4o | Tier 5: 10,000 | ~167 | Tier 4+ (67 req/s) |
| OpenAI | GPT-4o-mini | Tier 5: 30,000 | ~500 | Tier 3+ |
| Gemini | 2.5 Flash | Tier 3: 4,000 | ~67 | Tier 2+ |
| DeepSeek | R1/V3 | 제한 없음 | ∞ | 즉시 |

> VT가 실질적으로 필요해지는 시점: 외부 API 한도 > ~100 req/s (동시 200명 × 2s 지연)

