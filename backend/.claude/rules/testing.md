# Backend Testing Rule

## 핵심 원칙

1. **테스트만 보고 도메인 정책 파악 가능해야 한다** — `@DisplayName` 한국어 + `@Nested` 시나리오 그룹.
2. **내가 만든 코드 Mock 자제** — 외부 API (HTTP / SDK) 만 Mock. 내부 Service / Repository / 도메인 = 실제 주입.
3. **피라미드 유지** — Domain Unit 多, E2E 少.
4. **Support 추상 클래스 재사용** — 매 클래스 환경 셋업 반복 금지.
5. **AssertJ 강제** + **`verify(...)` 최소화** (반환값 / 상태 검증 우선).

## 카테고리

| 카테고리 | 대상 | Support | Spring 컨텍스트 | 비중 |
|---------|------|---------|----------------|------|
| E2E (RestAssured) | API 플로우 | `E2ESupport` | `@SpringBootTest(RANDOM_PORT)` | ≤5% |
| Service Integration | 서비스 비즈니스 | `ServiceIntegrationSupport` | `@SpringBootTest` | 20-25% |
| Infra Integration | 외부 어댑터 (HTTP/SDK) | `InfraIntegrationSupport` | 부분 컨텍스트 | 5-10% |
| Domain Unit | VO / Entity / 순수 도메인 | `DomainUnitSupport` | 없음 | ≥60% |
| Repository | 직접 작성 쿼리만 | `RepositorySupport` | `@DataJpaTest` + Testcontainers | 5-10% |
| Smoke | 부팅 가드 | (1개 고정) | `@SpringBootTest` | — |
| ArchUnit | 구조 / 계층 룰 | (별도) | — | — |

Support 위치: `backend/src/test/java/com/rehearse/api/support/`. Fixtures: `support/fixtures/TestFixtures.java`.

## E2E 룰

- API 당 **성공 1 건만**.
- 외부 API 포함 시 **Mock 1 + Live 1**.
- Live = `@Disabled` 기본 + `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`. 신규 / 외부 호출 로직 수정 시만 활성.
- 인증: Support 가 JWT 발급 + 헤더 주입 헬퍼 제공.

## Service Integration 룰

- 외부 API 만 Mock. 내가 만든 Service / Repository = **실제 주입**.

## DB Cleanup 정책

| 카테고리 | 정리 방식 |
|---------|----------|
| E2E | **TRUNCATE in `@BeforeEach`** (Support 제공) |
| Service Integration | **TRUNCATE in `@BeforeEach`** (Support 제공) |
| Infra Integration (DB 사용 시) | **TRUNCATE in `@BeforeEach`** |
| Repository (`@DataJpaTest`) | 자동 롤백 허용 (슬라이스 테스트 / 단일 트랜잭션) |
| Domain Unit | 해당 없음 (DB 미사용) |

**`@Transactional` 롤백 cleanup 금지 (E2E / Service / Infra)** — 실 commit 동작 차이 + 비동기 / 이벤트 / 멀티 트랜잭션 깨짐.

## Infra Integration 룰

- 어댑터 단독 검증 (헤더 / 페이로드 / Retry / Timeout / Response 파싱).
- 2-tier: Mock 통합 (default ON, WireMock 또는 SDK Mock) + Live 통합 (`@Disabled` 기본, 키 부재 시 자동 skip).

## Domain Unit 룰

- VO / Entity 메서드 검증.
- 도메인 서비스가 Repository 의존 시 Repository 실제 주입 우선 (Mock 도 허용 — 기존 Mockist 패턴).

## Repository 룰

- **직접 작성 쿼리만** (`@Query`, QueryDSL, Native). Spring Data 기본 메서드 테스트 X.
- Testcontainers (mysql) + Flyway 자동 적용.

## 결정 가이드 — Service Integration vs Domain Unit

```
의존성 = VO / Entity 만               → Domain Unit
+ Repository 1개                      → Domain Unit (Repository 실제 주입)
+ 다른 도메인 Service 호출            → Service Integration
+ 외부 API 호출                       → Service Integration (외부 API 만 Mock)
```

## 네이밍

- `@DisplayName` 한국어 필수, 도메인 정책 한 줄 표현. 예: `"이력서 PDF 가 10MB 초과하면 업로드 실패한다"`.
- 메서드명: `<예상결과>_when_<조건>` 또는 `should_<예상결과>_when_<조건>`.
- `@Nested` 로 시나리오 그룹.

## 데이터

- `TestFixtures` 강제. 신규 도메인 = `TestFixtures` 에 팩토리 추가. 자체 빌더 / 임의 객체 금지.
- Entity / VO / DTO **Mock 금지** — 실제 객체.

## 외부 API 키

- 환경변수 (`OPENAI_API_KEY` 등). 코드 / yml 하드코딩 금지.
- Live 테스트는 키 부재 시 자동 skip.

## 안티 패턴 (즉시 반려)

- 내가 만든 Service / Repository Mock.
- E2E 케이스 다수 (성공 1 + Mock/Live 외 추가).
- `verify(...)` 위주 구현 추적.
- Entity / VO / DTO Mock.
- LLM / timestamp 비결정적 출력 snapshot.
- `@DisplayName` 누락 또는 영문 / 무의미 이름.
- Support 우회 (어노테이션 / 셋업 반복).
- E2E / Service / Infra Integration 에서 `@Transactional` 롤백 cleanup (TRUNCATE 사용).
- API 키 코드 / yml 노출.
