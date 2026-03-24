# Plan 07: 부하테스트 — TX 분리 + VT 도입 전후 비교

> 상태: Completed
> 작성일: 2026-03-23

## Why

트랜잭션 분리와 VT 도입의 실제 효과를 정량적으로 측정하여, 커넥션 점유 시간 단축과 동시 처리량 향상을 증명한다.

## 테스트 도구

- **k6** (Grafana k6) — JavaScript 기반 부하테스트 도구
- **WireMock** — 외부 API stub (일관된 지연 시뮬레이션)

## 테스트 시나리오

### 시나리오 A: 트랜잭션 분리 전후 DB 커넥션 점유 비교

- **대상 API**: `POST /api/interviews/{id}/follow-up`
- **외부 API stub**: WireMock (Claude 2초 지연, Whisper 1초 지연)
- **측정 지표**:
  - HikariCP active connections (`/actuator/metrics/hikaricp.connections.active`)
  - 커넥션 점유 시간 (leak-detection-threshold 로그)
  - p50/p95/p99 응답 시간
- **비교 조건**:
  1. 분리 전: 기존 `@Transactional` 단일 메서드 (별도 브랜치에서 테스트)
  2. 분리 후: `FollowUpTransactionHandler` 3-phase 구조
- **부하**: 동시 사용자 10 / 20 / 50명, 각 30초 유지

### 시나리오 B: Virtual Thread 도입 전후 동시성 비교

- **대상 API**: `POST /api/interviews/{id}/follow-up`
- **외부 API stub**: WireMock (동일 지연)
- **측정 지표**:
  - 최대 동시 처리 요청 수
  - p50/p95/p99 응답 시간
  - 에러율 (429, 503, timeout)
  - JVM 스레드 수 (Actuator `/actuator/metrics/jvm.threads.live`)
- **비교 조건**:
  1. Platform Thread: `spring.threads.virtual.enabled=false` (Tomcat 200 스레드)
  2. Virtual Thread: `spring.threads.virtual.enabled=true`
- **부하**: Ramp-up 10 → 50 → 100 → 200 동시 사용자, 각 단계 30초

### 시나리오 C: RateLimiter 동작 검증 (스트레스)

- **대상**: Claude API RateLimiter (20 req/s)
- **부하**: 50 req/s 지속 60초
- **측정**: 429 응답 비율, 정상 처리 비율, `RequestNotPermitted` 발생 시점

## 환경

- 로컬 개발 환경 (Docker MySQL 또는 H2)
- WireMock으로 외부 API stub (일관된 지연 시뮬레이션)
- HikariCP pool-size: 10 (의도적으로 작게 설정하여 병목 조기 발견)
- Actuator metrics 엔드포인트 활성화

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/test/k6/follow-up-load-test.js` | k6 부하테스트 스크립트 (시나리오 A/B/C) |
| `backend/src/test/k6/wiremock-stubs/` | WireMock stub 설정 (Claude, Whisper 지연) |
| `backend/src/test/resources/application-loadtest.yml` | 부하테스트 전용 프로필 |

## 실행 순서

1. WireMock stub 설정 + application-loadtest.yml 생성
2. k6 스크립트 작성
3. 시나리오 A 실행 (TX 분리 전 → 분리 후)
4. 시나리오 B 실행 (Platform Thread → Virtual Thread)
5. 시나리오 C 실행 (RateLimiter 스트레스)
6. 결과 수집 → plan-08 문서화

## 담당 에이전트

- Implement: `test-engineer` — 테스트 스크립트 + 환경 구성
- Review: `architect-reviewer` — 테스트 시나리오 유의미성 검증

## 검증

- k6 스크립트 실행 가능 확인
- WireMock stub 정상 동작 확인
- 각 시나리오 결과 데이터 수집 완료
- `progress.md` 상태 업데이트 (Task 7 → Completed)
