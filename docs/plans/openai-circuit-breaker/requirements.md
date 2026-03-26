# GPT-4o-mini 마이그레이션 + 서킷브레이커 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-26

## Why

### 1. Why? — 어떤 문제를 해결하는가?

질문 생성 / 후속 질문 생성은 서비스의 핵심 로직이지만 외부 API(Claude)에 100% 의존한다. 현재 두 가지 문제가 있다:

1. **Rate Limit**: Claude API의 rate limit으로 동시 요청 처리에 병목 발생. MVP 단계에서도 면접 생성 실패 사례 확인.
2. **단일 장애점(SPOF)**: Claude API 장애 시 질문 생성 + 후속 질문 모두 불가 → 서비스 전체 마비. 현재는 수동 재시도(3회) 외에 장애 대응 메커니즘 없음.

### 2. Goal — 구체적인 결과물과 성공 기준

| 지표 | 현재 | 목표 |
|------|------|------|
| Primary AI | Claude (sonnet/haiku) | GPT-4o-mini (rate limit 여유) |
| 호출 실패 대응 | 3회 재시도 후 실패 → 에러 | 1회 재시도 (총 2회) 후 실패 → Claude fallback |
| 장애 대응 (CB) | 없음 | 30% 실패율 → OPEN → OpenAI 시도 생략, Claude 직행 |
| 이중 장애 시 | 500 에러 | 503 SERVICE_UNAVAILABLE + 사용자 친화 메시지 |
| 복구 감지 | N/A | 60초마다 자동 확인, 성공 시 즉시 복귀 |

### 3. Evidence — 근거 데이터

**Rate Limit 문제:**
- `ClaudeApiClient.java:126-128` — 429 응답 처리 로직 존재 → 실제 rate limit 발생 확인
- `application-prod.yml` — claude-api RateLimiter 20 req/s 설정 → Anthropic tier 제한에 맞춤
- OpenAI GPT-4o-mini: Tier 1 기준 500 RPM, Tier 2 기준 5000 RPM → 여유 충분

**장애 대응 부재:**
- `ClaudeApiClient.java:118-178` — 수동 3회 재시도가 유일한 방어 장치
- 3회 모두 실패 시 즉시 `BusinessException(TIMEOUT)` → 사용자에게 에러 노출
- 동시에 여러 요청이 같은 장애 API를 계속 호출 → 불필요한 리소스 소모

**서킷브레이커 효과 (일반론):**
- OPEN 상태에서 즉시 실패(fast-fail) → 장애 API에 불필요한 호출 방지
- fallback으로 서비스 연속성 보장
- HALF_OPEN으로 자동 복구 감지 → 수동 개입 불필요

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 대안 제외 이유 |
|------|------|----------------|
| GPT-4o-mini (Primary) | Claude 유지 + 티어 업그레이드 | 비용 증가, rate limit 근본 해결 안됨 |
| Claude (Fallback) | fallback 없이 에러 반환 | 핵심 로직이므로 서비스 연속성 중요 |
| Resilience4j CircuitBreaker | Spring Cloud CircuitBreaker | Resilience4j 이미 사용 중(RateLimiter), 동일 스택 유지 |
| Composite AiClient 패턴 | @Primary/@Qualifier 분리 | Composite가 서킷브레이커 로직을 한 곳에 캡슐화, 서비스 계층 변경 불필요 |
| 30% failureRate | 50% / 70% | 하단 심층 분석 참조 |
| 재시도 1회 (총 2회) | 재시도 3회 (총 4회) | 빠른 fallback 전환 우선. 장애 시 3회 재시도는 시간 낭비 |

---

## 아키텍처

### 변경 전

```
[Service Layer] → AiClient(interface)
                    ↓
              ClaudeApiClient (유일한 구현체)
                    ↓
              Claude API (단일 의존)
```

### 변경 후

```
[Service Layer] → AiClient(interface)
                    ↓
              ResilientAiClient (@Primary, 서킷브레이커 오케스트레이션)
                    ├─ @CircuitBreaker → OpenAiClient (GPT-4o-mini, Primary)
                    └─ fallback       → ClaudeApiClient (Claude, Fallback)
```

- **ResilientAiClient**: `@Primary` `AiClient` 구현체. 서킷브레이커 + fallback 로직 담당
- **OpenAiClient**: OpenAI Chat Completions API 호출. 독립 클래스 (AiClient 미구현)
- **ClaudeApiClient**: 기존 코드 그대로 유지. 독립 클래스 (AiClient 미구현으로 변경)
- **서비스 계층**: `AiClient` 인터페이스만 의존 → **변경 없음**

### 2단계 Fallback 전략

```
요청 → [RateLimiter] → [CircuitBreaker]
                              │
                    ┌─────────┴─────────┐
                  CLOSED              OPEN
                    │                   │
              OpenAiClient         (OpenAI 시도 생략)
              (1회 재시도, 총 2회)       │
                    │                   │
               실패 시                  │
                    ↓                   ↓
              ClaudeApiClient ← ─ ─ Claude 직행
              (per-call fallback)
```

**1단계 — Per-call Fallback (CB CLOSED 상태):**
- OpenAI 호출 (최대 2회: 원래 1회 + 재시도 1회)
- 2회 모두 실패 → 즉시 Claude fallback
- CB에 "실패 1회" 기록

**2단계 — Circuit Breaker OPEN (30% 도달):**
- OpenAI 시도 자체를 생략 → Claude 직행
- 장애 API에 불필요한 호출 방지 (fast-fail)

---

## 서킷브레이커 수치 타당성 분석

### 도메인 특성 요약

| 요소 | 질문 생성 | 후속 질문 |
|------|----------|----------|
| 동기/비동기 | 비동기 (이벤트) | 동기 (실시간, 사용자 대기) |
| 응답 시간 | 5-30초 | 2-5초 |
| 실패 시 영향 | FAILED 상태, 재시도 API 있음 | 면접 흐름 중단, 사용자에게 에러 |
| 트래픽 규모 | MVP <10 동시 사용자 | MVP <10 동시 사용자 |
| 면접 1회당 호출 | 질문 생성 1회 | 후속 질문 0~16회 |

### 파라미터 설정값

```yaml
resilience4j:
  circuitbreaker:
    instances:
      openai-api:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 30
        slowCallDurationThreshold: 30s
        slowCallRateThreshold: 80
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

### 파라미터별 근거

#### slidingWindowType: COUNT_BASED

| 옵션 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| COUNT_BASED | 호출 수 기준이라 저트래픽에서도 의미 있는 통계 | 시간 개념 없음 | **채택** |
| TIME_BASED | 시간 단위 평가, 트래픽 변동 반영 | MVP 60초 윈도우에 호출 2-3건이면 통계적 무의미 | 제외 |

MVP 사용 패턴: 면접 1회 = 질문 5-8개 + 후속 0-16개가 **한 번에 몰림** → 시간 기반보다 호출 수 기반이 적합.

#### slidingWindowSize: 10

- 면접 1회 호출량: 질문 생성 1회 + 후속 질문 0~16회 ≈ 최대 17회
- 윈도우 10 = 약 1회 면접 세션 분량으로 판단 가능

| 크기 | 장애 감지 속도 | false positive 리스크 | 선택 |
|------|-------------|---------------------|------|
| 5 | 매우 빠름 (2회 실패 = 40%) | 높음 — 네트워크 지터에 취약 | 제외 |
| **10** | **적절 (3회 실패 = 30%)** | **낮음** | **채택** |
| 20 | 느림 — MVP에서 수분~수십분 | 매우 낮음 | 제외 |

#### minimumNumberOfCalls: 5

윈도우가 절반(5회) 이상 차야 평가 시작. 서비스 시작 직후 1-2회 실패로 즉시 OPEN 되는 것 방지.

#### failureRateThreshold: 30% — 심층 분석

**핵심 전제:**
- 서킷브레이커가 보는 "1회 실패" = 재시도 1회 포함 총 2회 시도 후 최종 실패
- CB에 1회 실패가 기록되려면 실제로 2회의 raw API 호출이 실패해야 함
- 단, 1회 실패 시 per-call fallback으로 이미 Claude가 응답 → **사용자는 영향 없음**

| CB 실패 횟수 (10회 윈도우) | CB 실패율 | 실제 raw 실패 횟수 (최소) | 사용자 영향 |
|--------------------------|---------|------------------------|-----------|
| 2회 | 20% | 4회 | 없음 (Claude fallback 성공) |
| **3회** | **30%** | **6회** | **없음 (CB OPEN → Claude 직행으로 전환)** |
| 5회 | 50% | 10회 | 없음 (이미 OPEN 상태) |

**30%가 적절한 이유:**

1. **per-call fallback이 1차 방어선**: OpenAI 실패 → 즉시 Claude 응답. 사용자는 어떤 AI가 응답했는지 모름. false positive로 CB가 열려도 사용자 영향 제로.

2. **CB OPEN = 최적화**: CB의 역할은 "장애 AI에 불필요한 시도를 줄이는 것". 30% 도달 = 6+ raw 실패 = 일시적 지터가 아닌 지속적 문제. 이 시점에서 OpenAI 시도를 생략하면 응답 시간 단축.

3. **50%/70%로 올리면**: CB가 OPEN되기 전에 더 많은 요청이 "OpenAI 실패 → Claude fallback" 경로를 타야 함. 이 경로는 OpenAI 호출 시간(타임아웃 포함) + Claude 호출 시간이 합산되어 **응답 지연**이 발생.

| 임계값 | OPEN까지 필요한 최종 실패 수 | 그동안 지연되는 요청 수 | 선택 |
|--------|--------------------------|---------------------|------|
| **30%** | **3회 (=6 raw)** | **3건** | **채택 — 빠른 전환, 지연 최소화** |
| 50% | 5회 (=10 raw) | 5건 | 5건이 지연됨. 불필요 |
| 70% | 7회 (=14 raw) | 7건 | 7건이 지연됨. 너무 느림 |

**결론: 30% 채택 근거**
1. per-call fallback이 있으므로 CB OPEN의 false positive 비용이 거의 없음 (Claude로 가든 OpenAI로 가든 사용자는 동일한 서비스)
2. CB OPEN의 진짜 이점 = 장애 API 호출 시간 절약 (응답 지연 방지)
3. 30%면 3회 최종 실패(6 raw)로 빠르게 전환 → 나머지 요청의 지연 방지
4. 60초 후 HALF_OPEN에서 자동 복구 시도 → OpenAI 정상이면 바로 복귀

#### slowCallDurationThreshold: 30s

| 작업 | 정상 범위 | 30초 초과 의미 |
|------|----------|--------------|
| 질문 생성 (gpt-4o-mini) | 3-15초 예상 | 확실한 지연 — 타임아웃(60초) 임박 |
| 후속 질문 (gpt-4o-mini) | 1-3초 예상 | 심각한 이상 |

gpt-4o-mini는 Claude sonnet/haiku보다 빠를 것으로 예상 → 30초는 충분한 여유.

#### slowCallRateThreshold: 80%

질문 생성이 원래 느린 작업(3-15초)이므로 일부 slow call은 정상. 80% 이상이 30초 초과 = 서비스 전체가 비정상적으로 느린 상태.

#### waitDurationInOpenState: 60s

| 대기 시간 | 복구 확인 빈도 | 장점 | 단점 |
|----------|-------------|------|------|
| 30초 | 30초마다 | 빠른 복구 감지 | 복구 안된 API에 너무 자주 시도 |
| **60초** | **60초마다** | **적절한 균형** | 최대 60초 지연 |
| 120초 | 2분마다 | 보수적 | 복구됐는데 Claude에서 오래 머무름 |

OpenAI 장애 이력: 대부분 5-30분 내 복구. 60초 간격이면 복구 후 최대 1분 내 감지.

#### permittedNumberOfCallsInHalfOpenState: 3

| 허용 호출 수 | 판단 신뢰도 | HALF_OPEN 지속 시간 |
|------------|-----------|-------------------|
| 1 | 낮음 — 우연한 성공/실패 구분 불가 | 매우 짧음 |
| **3** | **적절 — 2/3 성공 = 67% 신뢰** | **짧음** |
| 5 | 높음 | MVP 트래픽에서 오래 지속 |

### Fallback 전략 (2단계)

```
[CB CLOSED — 정상 상태]
  └─ OpenAiClient 호출 (1회 재시도, 총 2회)
       ├─ 성공 → 응답 반환
       └─ 실패 → Claude fallback → 응답 반환
                  (CB에 실패 1회 기록)

[CB OPEN — 30% 실패율 도달]
  └─ OpenAI 시도 생략 → Claude 직행
       ├─ 성공 → 응답 반환
       └─ 실패 → BusinessException(SERVICE_UNAVAILABLE, 503)

[CB HALF_OPEN — 60초 후 복구 확인]
  └─ OpenAiClient 3회 시도
       ├─ 실패율 30% 미만 → CLOSED 복귀 (OpenAI 정상)
       └─ 실패율 30% 이상 → OPEN 유지 (다시 60초 대기)
```

**핵심**: CB CLOSED 상태에서도 OpenAI 실패 시 Claude가 즉시 응답 → 사용자는 장애를 인지하지 못함.
CB OPEN은 "OpenAI 호출 시간 낭비 방지"를 위한 최적화 역할.

- fallback 호출(Claude)은 CB 통계에 포함하지 않음 (별도 API)
- 이중 장애(OpenAI + Claude 동시 장애)는 극히 드문 상황 → 503 에러 반환

---

## Scope

### In
- OpenAI GPT-4o-mini Client 구현 (Chat Completions API)
- Resilience4j CircuitBreaker 설정 (Retry는 수동 1회 재시도)
- ResilientAiClient (Composite 패턴, fallback 오케스트레이션)
- ClaudeApiClient 리팩토링 (AiClient → 독립 클래스)
- AiResponseParser 리네임 (ClaudeResponseParser → 범용)
- 설정 파일 업데이트 (openai + circuitbreaker)
- AiErrorCode.SERVICE_UNAVAILABLE 추가
- GlobalExceptionHandler CB 예외 핸들러

### Out
- Claude API 코드 삭제 (fallback으로 유지)
- 프롬프트 템플릿 변경 (모델 무관, 그대로 사용)
- 프론트엔드 변경 (백엔드만 변경)
- 운영 환경 배포
- 부하 테스트

## 제약조건

- Java 21, Spring Boot 3.4.3, Gradle Kotlin DSL
- Resilience4j 2.2.0 (spring-boot3 + ratelimiter 이미 사용 중)
- `AiClient` 인터페이스 변경 불가 (서비스 계층 영향 최소화)
- 프롬프트 빌더는 모델 무관 (재사용)
- 두 API 키 모두 비어있으면 MockAiClient 활성화 유지
