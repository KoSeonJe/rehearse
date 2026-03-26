# Plan 03: 설정 정리 + 로그 정리

> 상태: Draft
> 작성일: 2026-03-26

## Why

OpenAI Client와 서킷브레이커가 올바르게 동작하려면 설정 파일에 OpenAI 프로퍼티, CircuitBreaker 설정, RateLimiter 설정이 추가되어야 한다. 또한 로그 메시지에서 "Claude" 하드코딩을 정리하여 multi-provider 구조에 맞게 일관성을 확보한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/application.yml` | CB + openai-api RateLimiter 설정 추가 |
| `backend/src/main/resources/application-dev.yml` | openai model 설정 추가 |
| `backend/src/main/resources/application-prod.yml` | openai + CB prod 설정 추가 |
| `backend/src/main/java/.../questionpool/service/FreshQuestionProvider.java` | 로그 "Claude" → "AI" |
| `backend/src/main/java/.../questionpool/service/CacheableQuestionProvider.java` | 로그 "Claude" → "AI" |

## 상세

### application.yml (기본 설정)

기존 `claude-api` RateLimiter는 유지 (ClaudeApiClient fallback용). 새로 `openai-api` 추가:

```yaml
# 추가할 설정
openai:
  api-key: ${OPENAI_API_KEY:}
  model: ${OPENAI_MODEL:gpt-4o-mini}

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
        recordExceptions:
          - com.rehearse.api.global.exception.BusinessException
          - com.rehearse.api.infra.ai.exception.RetryableApiException
          - org.springframework.web.client.RestClientException
  ratelimiter:
    instances:
      openai-api:           # 추가
        limitForPeriod: 50
        limitRefreshPeriod: 1s
        timeoutDuration: 120s
      claude-api:           # 기존 유지 (fallback용)
        limitForPeriod: 50
        limitRefreshPeriod: 1s
        timeoutDuration: 120s
```

### application-dev.yml

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  model: ${OPENAI_MODEL:gpt-4o-mini}
# claude 블록 유지 (fallback용)
```

### application-prod.yml

```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  model: ${OPENAI_MODEL:gpt-4o-mini}
# claude 블록 유지 (fallback용)

resilience4j:
  circuitbreaker:
    instances:
      openai-api:
        # prod는 기본값과 동일 (필요 시 오버라이드)
        waitDurationInOpenState: 60s
  ratelimiter:
    instances:
      openai-api:
        limitForPeriod: 30
        limitRefreshPeriod: 1s
        timeoutDuration: 60s
      claude-api:
        limitForPeriod: 20
        limitRefreshPeriod: 1s
        timeoutDuration: 60s
```

### 로그 메시지 정리

- `FreshQuestionProvider.java`: "Claude 호출 완료" → "AI 호출 완료"
- `CacheableQuestionProvider.java`: "Claude 호출" → "AI 호출"

## 담당 에이전트

- Implement: `backend` — 설정 파일 + 로그 정리
- Review: `code-reviewer` — 설정 값 정합성, 환경별 오버라이드 검증

## 검증

- `./gradlew build` 성공
- dev 프로필 실행 시 CircuitBreaker 빈 정상 로드 (로그 확인)
- Actuator `/actuator/circuitbreakers` 엔드포인트 응답 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
