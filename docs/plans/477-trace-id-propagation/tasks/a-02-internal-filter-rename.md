# Task A2 — InternalApiKeyFilter 헤더명 / MDC key rename

> **PR**: PR-A
> **영역**: BE
> **선행 Task**: A1 (filter chain 순서 결정)

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/global/config/InternalApiKeyFilter.java` — **변경**. 헤더명 `X-Correlation-Id` → `X-Trace-Id`. MDC key `correlationId` → `traceId`. 부재 시 WARN + UUID **MDC fallback** (sunset 없음, 즉시 단일화).
- `backend/src/test/java/com/rehearse/api/global/config/InternalApiKeyFilterTest.java` — **신규 또는 확장**. 헤더 rename 회귀 / MDC fallback / 패턴 위반.

### 용어 분리 (Required)

본 plan 에서 "fallback" 은 두 종류가 존재. 혼동 금지:

| 종류 | 정의 | 본 plan 적용 |
|------|------|-------------|
| **헤더 dual-pattern fallback** | `X-Correlation-Id` 와 `X-Trace-Id` 둘 다 인식 (transitional) | **없음** — 즉시 단일화 (`X-Trace-Id` 만 인식). 사용자 명시 결정 |
| **MDC UUID fallback** | 헤더 부재 / 패턴 위반 시 UUID 자체 생성 후 MDC 적재 + WARN | **있음** — 요청 reject 하지 않고 정상 처리 (운영 가시성 유지) |

본 task 의 `WARN + UUID fallback` 은 **MDC UUID fallback** 만 의미. 헤더 dual-pattern 인식 코드 작성 금지.

---

## 핵심 로직

```java
// 변경 전
String correlationId = req.getHeader("X-Correlation-Id");
if (correlationId == null) {
    correlationId = UUID.randomUUID().toString();
}
MDC.put("correlationId", correlationId);

// 변경 후
String raw = req.getHeader("X-Trace-Id");
String traceId;
if (raw != null && VALID.matcher(raw).matches()) {
    traceId = raw;
} else {
    traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    log.warn("X-Trace-Id 헤더 부재 또는 패턴 위반 - fallback uuid 사용: path={}, given={}",
        req.getRequestURI(), raw);
}
MDC.put("traceId", traceId);
try {
    chain.doFilter(req, res);
} finally {
    MDC.remove("traceId");
}
```

VALID 정규식 = A1 의 `TraceIdFilter` 와 동일 (`[a-zA-Z0-9-]{8,32}`). 공통 상수화 검토 가능 (옵션) — 단순화 우선 시 각자 보유 허용.

---

## 테스트

**카테고리**: Domain Unit

```java
class InternalApiKeyFilterTest {
    @Test @DisplayName("X-Trace-Id 헤더 있으면 MDC 에 그대로 적재")
    void traceId_inHeader_isPropagatedToMdc() { ... }

    @Test @DisplayName("X-Trace-Id 헤더 부재 시 WARN + UUID fallback")
    void fallback_uuid_with_warn_when_headerMissing() { ... }

    @Test @DisplayName("X-Trace-Id 헤더 패턴 위반 시 WARN + UUID fallback")
    void fallback_uuid_with_warn_when_headerPatternInvalid() { ... }

    @Test @DisplayName("filter 종료 후 MDC traceId 가 제거된다")
    void clears_mdc_after_filterChain() { ... }

    // 기존 X-Internal-Api-Key 검증 케이스는 유지
}
```

---

## 의존

- 선행: A1 (filter chain 순서)
- 외부: SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.global.config.InternalApiKeyFilterTest"`
- 통과 기준: 모든 케이스 green
- 관찰 가능 동작: dev Lambda safe-deploy 후 콜백 호출 1건 → BE stdout 로그에 동일 traceId 출력 (Lambda 송신 헤더와 일치)

---

## 커밋 메시지 (예상)

```
refactor(BE): InternalApiKeyFilter 헤더명 X-Trace-Id 로 단일화
```
