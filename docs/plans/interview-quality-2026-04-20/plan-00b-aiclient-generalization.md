# Plan 00b: AiClient Generalization (Phase 0) `[blocking]`

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W1 후반 (3-4일)
> 해결 RC: RC1(AiClient 도메인 결합), RC7(Feature flag runtime), RC6의 JSON 파싱 폴백

## Why

현재 `AiClient` 인터페이스는 `generateQuestions / generateFollowUpQuestion / generateFollowUpWithAudio` 3개 도메인 메서드로 **고정**. 후속 plan이 요구하는 신규 호출이 최소 9종(Intent Classifier, Answer Analyzer, Follow-up v3, Dialogue Compactor, Resume Extractor, Resume Planner, Playground Responder, Chain Interrogator, Rubric Scorer, Feedback Synthesizer) — 인터페이스를 9번 쪼개는 것은 지속 가능하지 않다.

**근본 해결은 인터페이스 범용화**: "메시지 리스트 + 호출 메타(model/temperature/max_tokens/cache_policy/response_format)" 를 받아 JSON 결과를 반환하는 `chat(ChatRequest)` 메서드를 추가. 기존 3개 메서드는 이 위의 얇은 어댑터로 보존 → 회귀 0, 신규는 자유.

동시에 C3(모델 선택), M5(Fallback 캐시 정책), JSON 파싱 실패 폴백을 **이 인터페이스 내부에서 한 번에 해결**.

## 생성/수정 파일

### 수정
| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java` | 범용 `chat(ChatRequest): ChatResponse` 메서드 추가. 기존 3개 메서드는 default method로 새 chat 호출 위임 |
| `backend/src/main/java/com/rehearse/api/infra/ai/OpenAiClient.java` | `chat()` 구현. 모델 오버라이드(`ChatRequest.model`) 지원. 자동 Prompt Caching 활용 위한 메시지 순서 보장 |
| `backend/src/main/java/com/rehearse/api/infra/ai/ClaudeApiClient.java` | `chat()` 구현. `cache_control: ephemeral` 마킹 지원(메시지별 flag) |
| `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` | Primary 실패 시 Fallback에서도 `ChatRequest` 그대로 재시도. **Fallback 경로는 cache_policy.allow_miss=true** 로 degrade 허용 |
| `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` | `chat()` stub |
| `backend/src/main/java/com/rehearse/api/infra/ai/AiResponseParser.java` | 범용 JSON 파서 + `parseWithRetry()` — 스키마 실패 시 1회 재호출(prompt에 "previous response had invalid JSON, fix it" 프리픽스) |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatRequest.java` | 신규 record |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatResponse.java` | 신규 record (content + usage + cache_read + provider + fallback_used) |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatMessage.java` | 신규. `role(SYSTEM/USER/ASSISTANT)`, `content`, `cacheControl` |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/CachePolicy.java` | 신규. `provider_cache(auto/explicit/none)`, `allow_miss(bool)` |
| `backend/src/main/java/com/rehearse/api/config/AiFeatureProperties.java` | 신규 `@ConfigurationProperties("rehearse.features")` + `@RefreshScope` — runtime 갱신 지원 |
| `backend/src/main/resources/application.yml` | `management.endpoints.web.exposure.include` 에 `refresh` 추가 (Actuator) |
| `backend/build.gradle.kts` | `spring-cloud-starter` (refresh scope만, Config Server 아님) 최소 의존성 추가 |

### 신규 테스트
| 파일 | 작업 |
|------|------|
| `backend/src/test/java/com/rehearse/api/infra/ai/AiClientChatTest.java` | 범용 chat 계약 테스트 (Mock provider) |
| `backend/src/test/java/com/rehearse/api/infra/ai/ResilientAiClientFallbackTest.java` | Fallback 시 캐시 정책 degrade 검증 |
| `backend/src/test/java/com/rehearse/api/infra/ai/AiResponseParserRetryTest.java` | JSON 파싱 실패 재호출 검증 |

## 상세

### ChatRequest 설계
```java
public record ChatRequest(
    List<ChatMessage> messages,
    String modelOverride,            // null이면 application.yml 기본값
    Double temperature,
    Integer maxTokens,
    CachePolicy cachePolicy,
    ResponseFormat responseFormat,   // JSON_OBJECT | TEXT
    String callType                  // Micrometer 태그용
) {}
```

### 모델 선택 해결 (C3)
- application.yml의 `openai.model` 은 default (GPT-4o-mini 유지)
- plan-05 Resume Extractor는 `ChatRequest.modelOverride = "gpt-4o"` 명시
- plan-00b에서 이 오버라이드를 provider 호출 시점에 반영하도록 `OpenAiClient.chat()` 구현

### Fallback 캐시 정책 해결 (M5)
```java
// ResilientAiClient.chat(ChatRequest req):
try {
    return openAiClient.chat(req);
} catch (...) {
    ChatRequest fallbackReq = req.withCachePolicy(
        req.cachePolicy().allowMiss(true)   // Claude 캐시 콜드 미스 수용
    );
    return claudeApiClient.chat(fallbackReq);
}
```
Latency 증가는 수용하되 **max-context-tokens 상한은 동일**하게 적용(plan-04의 Compactor가 이미 했음).

### JSON 파싱 폴백 (Missing)
`AiResponseParser.parseWithRetry()`:
1. 1차 파싱 시도
2. 스키마 불일치 시 1회 재호출 — 원래 system prompt 뒤에 `"이전 응답은 스키마를 위반했습니다. 다음 필드가 필수: [...]. 다시 생성하세요."` 주입
3. 2차 실패 시 `AiResponseParseException` 던짐 → 호출측이 기존 경로로 degrade

### Feature Flag runtime 변경 (RC7)
Spring Cloud Config Server 도입 없이 **`@RefreshScope` + Actuator `/actuator/refresh`** 로 제한적 runtime 변경.
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
```
`POST /actuator/refresh` 호출 시 `@RefreshScope` bean 재생성 → `rehearse.features.*` 값 갱신. 로컬/스테이징에서 서버 재시작 없이 flag 토글 가능.

### Micrometer 태그 (RC4 선행 일부)
`chat()` 내부에서 `Timer.builder("rehearse.ai.call.duration").tag("call.type", req.callType()).tag("model", ...).tag("provider", ...).tag("cache.hit", ...).tag("fallback", ...)` 자동 기록.

### 기존 3개 메서드 보존 (회귀 방지)
```java
default QuestionGenerationResponse generateQuestions(QuestionGenerationRequest req) {
    ChatRequest chatReq = QuestionGenerationAdapter.toChat(req);
    ChatResponse resp = chat(chatReq);
    return QuestionGenerationAdapter.fromChat(resp);
}
```
기존 caller(QuestionGenerationService 등) 전혀 수정 불필요 → 회귀 범위 0.

## 담당 에이전트

- Implement: `backend-architect` — 인터페이스 재설계, 어댑터 패턴, Spring Cloud refresh 통합
- Review: `architect-reviewer` — SOLID(O/L/I 특히), 기존 caller 영향도
- Review: `code-reviewer` — 예외 처리 체인, JSON 파싱 재시도 로직, 테스트 커버리지

## 검증

1. `./gradlew test` 전체 통과 (기존 테스트 0개 깨짐)
2. 신규 테스트 3종(AiClientChatTest/ResilientAiClientFallbackTest/AiResponseParserRetryTest) 모두 통과
3. 기존 `FollowUpService` / `QuestionGenerationService` 호출 경로가 새 `chat()` 경유하도록 내부 리팩토링 — 외부 동작 동일
4. Micrometer 태그가 `rehearse.ai.call.duration_seconds{call.type="generate_questions",...}` 형태로 노출(로컬 `/actuator/prometheus` 확인)
5. `curl -X POST localhost:8080/actuator/refresh` 실행 시 `rehearse.features.intent-classifier.enabled` 값 변경이 즉시 반영
6. modelOverride 테스트: `ChatRequest.modelOverride="gpt-4o"` 시 실제 OpenAI 요청에 해당 model이 들어가는지(WireMock 또는 통합 테스트)
7. Fallback 시 `ChatResponse.fallbackUsed = true` 반환, `cacheHit = false` 허용
8. `progress.md` 00b → Completed
