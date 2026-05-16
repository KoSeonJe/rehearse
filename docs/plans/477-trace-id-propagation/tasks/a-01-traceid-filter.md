# Task A1 — TraceIdFilter 신설 + Security filter chain 등록

> **PR**: PR-A
> **영역**: BE
> **선행 Task**: 없음

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/global/config/TraceIdFilter.java` — **신규**
- `backend/src/main/java/com/rehearse/api/global/security/config/SecurityConfig.java:73-76` — **변경** (filter chain 등록 — `JwtAuthenticationFilter` 앞에 `TraceIdFilter` 삽입)
- `backend/src/test/java/com/rehearse/api/global/config/TraceIdFilterTest.java` — **신규** (Domain Unit)

> **참고**: tech-spec.md §Architecture 의 컴포넌트 변경 맵에서 위치 = `global/config/` (`InternalApiKeyFilter` 와 동거).

---

## 핵심 로직

`OncePerRequestFilter` 상속. 인증 진입점 (사용자 노출 `/api/v1/**`) 가드. `/api/internal/**` 은 `InternalApiKeyFilter` 가 담당하므로 본 filter 의 `shouldNotFilter` 에서 제외.

```java
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";
    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9-]{8,32}");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String raw = req.getHeader(HEADER);
        String traceId;
        if (raw == null) {
            traceId = generate();
        } else if (VALID.matcher(raw).matches()) {
            traceId = raw;
        } else {
            traceId = generate();
            log.warn("X-Trace-Id 헤더 패턴 위반 - fallback uuid 사용: path={}, given={}", req.getRequestURI(), raw);
        }
        MDC.put(MDC_KEY, traceId);
        // 응답 헤더 동봉 여부 = tech-spec.md §미확인 항목 #5 결정 대기
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        // /api/internal/** 은 InternalApiKeyFilter 가 담당
        return req.getRequestURI().startsWith("/api/internal/");
    }

    private static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

Security filter chain 등록 — 기존 `SecurityConfig.java:73-76` 의 `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` 앞에 `TraceIdFilter` 를 **명시적 순서**로 추가:

```java
// SecurityConfig.java (변경부)
http
    // ... 기존 설정 ...
    .addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class)   // 신규 — JWT 인증 이전에 MDC 적재
    .addFilterBefore(
        new JwtAuthenticationFilter(jwtTokenProvider),
        UsernamePasswordAuthenticationFilter.class
    );
```

- `traceIdFilter` = `@Component` 빈 주입 (`SecurityConfig` 생성자 `@RequiredArgsConstructor`).
- 순서 = `TraceIdFilter` → `JwtAuthenticationFilter` → `UsernamePasswordAuthenticationFilter`. 인증 실패 / 예외 경로에도 MDC traceId 가 적재된 상태로 로그 출력되도록 **인증보다 앞**.
- `InternalApiKeyFilter` 는 `/api/internal/**` 전용. `TraceIdFilter.shouldNotFilter` 가 `/api/internal/**` 제외 → 두 filter 충돌 없음.

**검증 방식**: 신규 통합 테스트 부재 (filter chain 출력 자동 검증 도구 없음). 다음으로 갈음:
- `TraceIdFilterTest` 5 케이스 (Domain Unit) — filter 자체 동작
- 부팅 직후 stdout 에 `o.s.s.web.DefaultSecurityFilterChain` 출력되는 filter 체인에 `TraceIdFilter` 등장 1회 수동 확인 (PR-A 머지 후 dev 배포 시점)

---

## 테스트

**카테고리**: Domain Unit (`DomainUnitSupport` 미사용 — jakarta servlet mock 만)

```java
class TraceIdFilterTest {
    @Test @DisplayName("X-Trace-Id 헤더 있으면 MDC 에 그대로 적재")
    void traceId_inHeader_isPropagatedToMdc() { ... }

    @Test @DisplayName("X-Trace-Id 헤더 없으면 16자 UUID 생성 후 MDC 적재")
    void generates_traceId_when_headerMissing() { ... }

    @Test @DisplayName("X-Trace-Id 헤더 패턴 위반 시 WARN + UUID fallback")
    void fallback_uuid_when_headerPatternInvalid() { ... }

    @Test @DisplayName("/api/internal 경로는 본 filter 가 처리하지 않는다")
    void shouldNotFilter_internalPath() { ... }

    @Test @DisplayName("filter 종료 후 MDC traceId 가 제거된다")
    void clears_mdc_after_filterChain() { ... }
}
```

---

## 의존

- 선행: 없음
- 외부: Spring Security `OncePerRequestFilter`, SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.global.config.TraceIdFilterTest"`
- 통과 기준: 5 케이스 green. MDC clear 검증.
- 관찰 가능 동작: 로컬 부트런 + `curl -H "X-Trace-Id: abc12345" http://localhost:8080/api/v1/users/me` → 서버 로그에 `[abc12345]` 출력.

---

## 커밋 메시지 (예상)

```
feat(BE): TraceIdFilter 신설 + 사용자 API 진입 traceId MDC 적재
```
