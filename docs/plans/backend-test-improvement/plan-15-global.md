# Plan 15: GlobalExceptionHandler + GlobalRateLimiterFilter 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 5. 횡단 관심사 검증. 예외 응답 형식(ErrorResponse 구조) 보장, Rate Limiter의 동시성 제어 검증. 모든 API의 에러 응답 포맷이 이 핸들러를 통과하므로, 형식이 깨지면 전체 프론트엔드에 영향.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: 없음

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../global/exception/GlobalExceptionHandlerTest.java` | 신규 생성 (~14 tests) |
| `src/test/.../global/config/GlobalRateLimiterFilterTest.java` | 신규 생성 (~11 tests) |

## 상세

### GlobalExceptionHandlerTest

테스트 유형: Unit (핸들러 메서드 직접 호출, mock 예외 전달)
Mock: `HttpServletRequest`

**HandleMethodArgumentNotValid**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `handleMethodArgumentNotValid_returns400WithFieldErrors` | 단일 필드 에러 | 400 + FieldError 포함 |
| 2 | `handleMethodArgumentNotValid_multipleErrors_returnsAll` | 복수 필드 에러 | 모든 에러 포함 |
| 3 | `handleMethodArgumentNotValid_nullRejectedValue_handled` | rejectedValue null | 정상 처리 |

**HandleTypeMismatch**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 4 | `handleTypeMismatch_returns400` | 타입 불일치 | 400 + TYPE_MISMATCH |

**HandleRateLimited**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 5 | `handleRateLimited_returns429` | RequestNotPermitted | 429 TOO_MANY_REQUESTS |

**HandleBusinessException**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 6 | `handleBusinessException_4xxStatus_mapsCorrectly` | 4xx ErrorCode | 해당 HTTP 상태 |
| 7 | `handleBusinessException_5xxStatus_mapsCorrectly` | 5xx ErrorCode | 해당 HTTP 상태 |
| 8 | `handleBusinessException_containsErrorCode` | 에러 코드 | ErrorResponse에 code 포함 |
| 9 | `handleBusinessException_containsMessage` | 에러 메시지 | ErrorResponse에 message 포함 |

**HandleGenericException**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 10 | `handleException_returns500` | NullPointerException 등 | 500 INTERNAL_SERVER_ERROR |
| 11 | `handleException_hidesInternalMessage` | 내부 에러 | 일반 메시지만 노출 |

**응답 구조**

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 12 | `allHandlers_returnErrorResponseFormat` | 모든 핸들러 | ErrorResponse 구조 일관 |
| 13 | `allHandlers_includeRequestUri` | URI 포함 | path 필드에 URI |
| 14 | `allHandlers_includeTimestamp` | 타임스탬프 | timestamp 필드 존재 |

### GlobalRateLimiterFilterTest

테스트 유형: Unit
Mock: `FilterChain`, `HttpServletRequest`, `HttpServletResponse` (+ `StringWriter` for response body)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `doFilter_limitZero_passesThrough` | limit=0 (비활성) | chain.doFilter 호출, 필터링 없음 |
| 2 | `doFilter_permitAvailable_executesChain` | permit 있음 | chain.doFilter 호출 |
| 3 | `doFilter_noPermit_returns503` | permit 없음 | 503 SERVICE_UNAVAILABLE |
| 4 | `doFilter_noPermit_returnsJsonBody` | 503 응답 바디 | JSON 형식 에러 |
| 5 | `doFilter_noPermit_setsContentTypeJson` | 503 헤더 | Content-Type: application/json |
| 6 | `doFilter_releasesPermitAfterSuccess` | 정상 요청 후 | permit 반환 |
| 7 | `doFilter_releasesPermitAfterException` | chain 예외 후 | permit 반환 (finally) |
| 8 | `doFilter_concurrentRequests_limitsCorrectly` | 동시 요청 | limit 초과 시 503, CountDownLatch |
| 9 | `doFilter_fairSemaphore_fifo` | 공정 세마포어 | FIFO 순서 |
| 10 | `doFilter_filterOrder_highestPrecedence` | 필터 순서 | HIGHEST_PRECEDENCE |
| 11 | `doFilter_negativeLimitDisabled` | limit=-1 | 비활성 (passthrough) |

## 담당 에이전트

- Implement: `test-engineer` — 2개 파일 작성
- Review: `qa` — 동시성 테스트 안정성, 에러 응답 JSON 구조

## 검증

- [ ] `./gradlew test --tests "GlobalExceptionHandlerTest"` 통과
- [ ] `./gradlew test --tests "GlobalRateLimiterFilterTest"` 통과
- [ ] GlobalRateLimiterFilterTest에 CountDownLatch 기반 동시성 테스트 포함
- [ ] ErrorResponse 구조 일관성 검증 포함
- [ ] `progress.md` 상태 업데이트 (Plan 15 → Completed)
