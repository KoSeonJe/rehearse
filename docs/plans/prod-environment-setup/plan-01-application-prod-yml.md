# Plan 01: `application-prod.yml` 설정 보강

> 상태: Draft
> 작성일: 2026-04-12

## Why

현재 `backend/src/main/resources/application-prod.yml`(1~69줄)은 최소한의 datasource / JPA / AWS / CORS 설정만 포함되어 있고, **OAuth2 client, JWT, Flyway, server, spring.servlet.multipart, OAuth2SuccessHandler가 의존하는 필드가 전부 누락**되어 있다. 이 상태로 `SPRING_PROFILES_ACTIVE=prod` 기동 시 다음 장애가 발생한다:

- **OAuth 로그인 불가**: `spring.security.oauth2.client.registration.{github,google}` 미설정 → Spring Security 컨텍스트 생성 실패
- **JWT 발급 불가**: `jwt.secret` 미주입 → `JwtService` NPE
- **Flyway 미실행**: prod DB 최초 기동 시 스키마 생성 안 됨, `ddl-auto: validate`로 인해 부팅 실패
- **OAuth https callback 깨짐**: `server.forward-headers-strategy` 미설정 → Nginx `X-Forwarded-Proto` 무시, `redirect_uri`가 `http://`로 생성 (dev-domain-restore에서 학습한 이슈)
- **영상 업로드 413**: `spring.servlet.multipart.max-request-size` 미설정 → 기본 1MB 제한

requirements.md의 Evidence 섹션에서 검증한 내용 그대로, `application-dev.yml` 수준까지 설정을 끌어올려 prod 프로파일이 단독 기동 가능하도록 한다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `backend/src/main/resources/application-prod.yml` | OAuth/JWT/Flyway/server/multipart 설정 추가 |
| `backend/.env.example` | prod 환경변수 샘플 키 추가 (주석으로 prod 전용 표시) |

## 상세

### 추가 항목 (dev.yml과 동일 구조)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user,user:email
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email,profile

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    # 주의: baseline-version은 절대 설정하지 말 것.
    # 빈 prod DB에서 baseline-version을 설정하면 V1을 건너뛰는 심각한 버그 발생.
    # baseline-on-migrate: true는 빈 DB에서는 무해(그냥 V1부터 시작).

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 604800000

server:
  port: 8080
  forward-headers-strategy: framework
```

### prod 전용 값 (dev와 차이)

- `spring.datasource.hikari.maximum-pool-size: 30` (유지, dev 기본 10)
- `spring.datasource.hikari.minimum-idle: 10` (유지)
- `spring.datasource.hikari.connection-timeout: 3000` (유지)
- `spring.datasource.hikari.max-lifetime: 1200000` (유지)
- `spring.datasource.hikari.leak-detection-threshold: 30000` (유지)
- `spring.jpa.hibernate.ddl-auto: validate` (강제, dev와 동일)
- `spring.jpa.show-sql: false` (유지)
- `logging.level.com.rehearse.api: INFO` (유지)
- `logging.level.org.flywaydb: INFO` (신규 추가)
- **OAuth scope는 dev와 동일**, provider에 등록한 prod client secret으로만 분기

### 확인해야 할 dev.yml 설정 (복사 여부)

| dev 설정 | prod 복사? | 비고 |
|---|---|---|
| `resilience4j.ratelimiter.*` | 필요 시 검토 | `application.yml`(base)에서 이미 정의되므로 prod 고유값 필요 없으면 생략 |
| `spring.threads.virtual.enabled` | 기본값 유지 | `application.yml`에서 `false` |
| `management.endpoints.web.exposure.include` | 기본값 유지 | `application.yml`에서 `health,info` 공개 |

### 검증 전 크로스체크

1. `application.yml`(base) → `application-prod.yml`(override) 병합 규칙 재확인
2. `OAuth2SuccessHandler.java:23,46-58`이 `${app.frontend-url}`을 참조하는지 재확인 — 이미 `app.frontend-url: ${FRONTEND_URL:https://rehearse.co.kr}` 기본값 존재
3. `CorsConfig.java:31` `setMaxAge(3600L)` — prod에서도 1시간 유지 (분리된 값 불필요)

## 담당 에이전트

- Implement: `backend` — `application-prod.yml` 수정
- Review: `architect-reviewer` — dev/prod 설정 일관성, 프로파일 오버라이드 규칙

## 검증

- `./gradlew test -Dspring.profiles.active=prod` (실행 가능 여부만 확인, SpringBootTest ContextLoad 검증)
- prod `.env.prod` 샘플과 교차 검증: 모든 `${VAR}` placeholder가 `.env.prod`에 존재하는지
- `grep -E '\${[A-Z_]+}' backend/src/main/resources/application-prod.yml` → 누락 변수 체크
- dev yml과 side-by-side diff로 의도적 차이(hikari pool, logging level)만 남았는지 확인
- `progress.md` Task 1 → Completed
