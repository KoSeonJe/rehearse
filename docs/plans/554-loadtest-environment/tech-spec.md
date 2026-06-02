# Tech Spec — loadtest 영속 부하테스트 환경 구축

> **작성자**: Staff Engineer (메인 세션)
> **product-spec (소비자 문서)**: `docs/local/a-btest.md` (실험0 영상분리) / `docs/local/connection.md` (실험1 커넥션) / `docs/local/thread.md` (실험2 스레드)
> **답하는 질문**: dev/prod 처럼 관리되는 `loadtest` 환경을 어떻게 영속 구축하나 (EC2 + 프로파일 + 관측 스택 + Mock AI + 배포 + start/stop)
> **승인 게이트**: ★ 사용자 명시 승인 후 implement 진입 ★
> **실행 에이전트**: `sre-engineer` (관측 스택 / Mock AI / k6 러너 / start-stop / 프로파일 조정 / EC2 기동) + 일부 `backend` (프로파일/exposure 설정 보강)

---

## Scope (이 spec의 경계)

**In-scope** — 3 실험이 공유하는 **영속 환경 기반**:
- 전용 EC2 `t4g.small` (ARM64) — 관리형, start/stop 가능, terminate 는 사용자만.
- `loadtest` Spring 프로파일 EC2 적용 + 베이스라인 설정값 고정·노출 (HikariCP / Tomcat thread / JVM heap).
- 관측 스택: Prometheus + Grafana + node_exporter (docker-compose, ARM64) + scrape config.
- Mock AI provider (고정 지연) — 변수 통제용.
- k6 러너 + 배포 메커니즘 + start/stop 운영 스크립트.

**Out-of-scope** — 각 실험이 별도 tech-spec + 브랜치로 진행:
- 실험0 영상분리 A 비교군 인라인 코드 (`feat/load-comparison`).
- 실험1 커넥션 A 비교군 (외부호출 트랜잭션 안 복원, `perf/connection-bottleneck`).
- 실험2 스레드 B/C 토글 측정 (`perf/thread-bottleneck`).
- 실제 부하 주입 / 측정 / 결과 문서화 (환경 위에서 실험별 수행).

> 본 환경 완성 후 실험0/1/2 를 **하나씩 fresh 컨텍스트**로 순차 실행 (실험1 B = 실험2 baseline 체인).

## Why → Goal (1줄 미러)

3종 성능 실험(영상분리/커넥션/스레드)을 **공정·재현 가능하게** 돌릴 dev/prod 급 관리 환경이 없음 → 측정 스택·Mock·베이스라인 설정을 고정한 영속 `loadtest` 환경을 1회 구축해 모든 실험이 재사용.

## Evidence

- **측정 스택 3문서 공통**: a-btest §2 / connection.md §2 / thread.md §2 모두 동일 — `[k6] → [Spring Actuator/Micrometer] → [Prometheus] → [Grafana]` + node_exporter. → **1회 구축 후 공유** 가 설계 의도(thread.md §2 "중복 셋업 불필요" 명시).
- **관측 의존성 일부 보유**: `backend/build.gradle.kts:31-32` — actuator + micrometer-registry-prometheus 존재.
- **exposure 갭 (확인됨)**: `backend/src/test/resources/application-loadtest.yml` 의 `management.endpoints.web.exposure.include = health, metrics, info` — **`prometheus` 미포함**. 3문서가 요구하는 `prometheus` 엔드포인트 미노출 상태 → 보강 필요.
- **loadtest 프로파일 기존 보유**: `application-loadtest.yml` — VT toggle(`VIRTUAL_THREAD_ENABLED`), HikariCP max=10/min=5, Resilience4j rate limit 8req/s, 외부 API → `localhost:9090`(Mock). EC2 단일 호스트 배치 시 localhost 구성 그대로 유효.
- **Mock AI 기존 자산 (두 갈래)**: compose 관리 **WireMock**(`docker-compose-loadtest.yml:19-25`, 9090, `wiremock-stubs/mappings/` — claude-followup `fixedDelayMilliseconds:3000` / whisper-stt `1000`) + 수동 실행 `mock-server.py`(`MOCK_DELAY` 기본 2s). → **WireMock 채택**(사용자 결정): compose-native + claude 3초 이미 충족. `mock-server.py` = dead 처리.
- **k6 기존 자산**: `follow-up-load-test.js` (시나리오 A/B/C — TX분리/VT/RateLimiter), `test-a-tx-separation.js`, `test-b-virtual-thread.js`. connection/thread 실험 부하 스크립트 이미 존재 → 환경 위에서 재사용.
- **사용자 발화 (결정 근거)**:
  - "부하테스트 서버 일회성 말고 dev, prod 처럼 loadtest 로 관리" → 영속 관리 환경.
  - "connection, thread 도 부하테스트 대상" → 환경은 3실험 공통 기반.
  - 인스턴스 = t4g.small, start/stop 가능, A 비교군 코드는 브랜치 전용.
- **추정/미확인**:
  - t4g.small = ARM64(Graviton2) / 2 vCPU / 2GB RAM → 모든 docker 이미지 arm64 태그, JVM arm64, (영상실험 시) ffmpeg arm64.
  - 2GB RAM → JVM heap + 관측 스택 컨테이너 공존 시 빠듯. heap 상한 + 컨테이너 메모리 제한 명시 필요.
  - 단일 호스트 배치(EC2 1대에 app+mysql+mock+관측) = 커넥션/스레드 경합 실측에 오히려 적합(실험 목적과 정합).

## Trade-offs

### Trade-off 1 — 환경 영속성 (채택: 관리형 영속 EC2 + start/stop)

#### Option A (채택): 영속 t4g.small, start/stop 로 비용 통제
- 장점: 3실험 재사용, 베이스라인 설정 1회 고정 → 실험 간 일관성. dev/prod 와 동일 운영 모델.
- 단점: 인스턴스 상시 보유(중지 상태 EBS 비용 소액). start/stop 운영 필요.
- 사유: 사용자 명시 + 3실험 공유 + 체인(실험1 B = 실험2 baseline) → 환경 재구축 반복 낭비 제거.

#### Option B (폐기): 실험마다 일회성 기동·폐기
- 폐기 사유: 베이스라인 설정 재고정 반복 → 실험 간 수치 비교 신뢰도 하락 + 셋업 중복.

### Trade-off 2 — 컴포넌트 배치 (채택: 단일 EC2 co-location)

#### Option A (채택): app + MySQL + Mock AI + 관측 스택 모두 EC2 1대 docker
- 장점: 커넥션/스레드/CPU 경합을 단일 호스트에서 실측 — 실험 목적과 정합. 네트워크 변수 제거.
- 단점: 관측 스택 자체가 호스트 자원 소비 → node_exporter 로 보정 + 컨테이너 메모리 제한.
- 사유: connection.md/thread.md 핵심 = "단일 서버 자원 경합". 분산 배치 시 측정 의미 희석.

#### Option B (폐기): MySQL=RDS / 관측=별도 호스트
- 폐기 사유: 비용·셋업 증가 + 단일 호스트 경합 측정 목적 훼손.

### Trade-off 3 — Mock AI 표준 (채택: WireMock, compose-native)

#### Option A (채택): WireMock 단일 표준 (claude stub 3000ms 기설정)
- 장점: compose 가 이미 와이어링(`docker-compose-loadtest.yml:19-25`), `wiremock-stubs/mappings/claude-followup.json` = `fixedDelayMilliseconds: 3000`(3초) 이미 충족. co-location compose env 와 네이티브. 수동 프로세스 0.
- 단점: 지연 변경 = stub JSON 편집(단 이미 3초라 거의 불변). `mock-server.py` 의존 제거(dead 처리).
- 사유: env 가 compose 기반(Trade-off 2) → compose 관리 WireMock 이 자연스러움 + 3문서 "외부 고정 지연 Mock" 원칙 충족. 실측 변수 1개(구조)만 남김.

#### Option B (폐기): mock-server.py(Python) 표준
- 폐기 사유: compose 밖 수동 실행 + 기본 2초(3초 아님) + co-location 통일성 깨짐. 사용자 결정으로 WireMock 채택.

## Architecture

```
[전용 EC2 t4g.small — ARM64, 관리형 loadtest 환경]
┌─────────────────────────────────────────────────────────┐
│ Spring Boot (profile=loadtest)  :8080                    │
│   - actuator/prometheus (HikariCP·Tomcat·JVM 메트릭)      │
│   - 베이스라인 고정: HikariCP pool / Tomcat thread / -Xmx  │
│        │                                                 │
│ docker-compose (arm64):                                  │
│   - MySQL 8.0          :3306  (rehearse_loadtest)        │
│   - WireMock (claude 3s/whisper 1s) :9090                │
│   - node_exporter      :9100  (호스트 CPU/메모리)         │
│   - Prometheus  :9091 (127.0.0.1 바인딩, scrape app+node)│
│   - Grafana     :3000 (127.0.0.1 바인딩, 대시보드)        │
└─────────────────────────────────────────────────────────┘
        ▲ k6 부하 (로컬 또는 EC2 내)        ▲ Grafana 스크린샷=포폴
        │                                  │
   [실험0/1/2 가 이 환경 위에서 브랜치별 배포 → 측정]

배포 흐름:  feat/perf 브랜치 빌드(jar, arch-neutral) → EC2 scp → arm64 JVM 실행
start/stop: aws ec2 start-instances / stop-instances (terminate 금지 — 사용자만)
```

## Data Model

**스키마 변경 없음.** 환경 구축은 인프라/설정만. MySQL(loadtest) 은 기존 Flyway 마이그레이션(`classpath:db/migration`) 그대로 적용(`baseline-on-migrate=true` — 신규 빈 DB 면 무효과, 기존 데이터 재사용 대비 안전망).

## API Contract

**신규 엔드포인트 없음.** 환경은 기존 actuator 엔드포인트만 노출 보강:
- `GET /actuator/prometheus` — **신규 노출** (exposure include 에 `prometheus` 추가). Prometheus scrape 대상.
- `GET /actuator/health`, `/actuator/metrics/{name}` — 기존 유지.

> 실험별 부하 엔드포인트(`POST /follow-up`, `POST /api/loadtest/inline-process` 등)는 각 실험 tech-spec 소관.

## 베이스라인 고정값 (3실험 해석 근거 — 반드시 노출·기록)

| 설정 | 값 | 근거 |
|------|-----|------|
| EC2 | t4g.small (ARM64, 2vCPU, 2GB) | 사용자 결정 |
| HikariCP pool | max=10 / min=5 | 기존 loadtest.yml. connection.md 핵심 해석 근거 |
| Tomcat thread pool | 200 (기본 명시) | thread.md p95 해석 근거 |
| JVM heap | `-Xmx768m` (아래 검산표 참조) | 추정 — implement 시 검산 |
| Mock AI 지연 | WireMock stub `fixedDelayMilliseconds` — claude 3000ms / whisper 1000ms | connection/thread 변수 통제 |
| VT toggle | `VIRTUAL_THREAD_ENABLED` (기본 true) | thread.md B/C 전환 |
| 외부 API rate limit | 8 req/s (기존) | 기존 loadtest.yml |

> 이 표는 모든 실험 결과 문서 헤더에 복사 — "수치가 공중에 뜨지 않게"(connection.md §0).

### 2GB 메모리 배분 검산 (합 ≤ 2048MB)

| 컴포넌트 | mem_limit | 비고 |
|---------|-----------|------|
| JVM app (`-Xmx768m`) | ~960MB | heap 768 + metaspace/스택/native ~190 |
| MySQL 8.0 | 384MB | `--innodb-buffer-pool-size=128M` 등 경량 |
| WireMock | 256MB | |
| Prometheus | 192MB | `--storage.tsdb.retention.time=2h` 경량 |
| Grafana | 128MB | |
| node_exporter | 32MB | |
| **합** | **~1952MB** | OS 여유 ~96MB |

> 빠듯함 → **관측 스택(Prometheus/Grafana)을 측정 PC(로컬)로 분리** 하는 fallback 우선 검토. implement 시 `docker stats` 실측 후 확정. node_exporter 만 EC2 상주(호스트 메트릭 필수).

## Verification (완료 판정)

- [ ] **EC2 기동**: t4g.small(arm64 AL2023) 기동 + start/stop 스크립트 동작(`aws ec2 start/stop-instances`).
- [ ] **app 배포·구동**: loadtest 프로파일 jar EC2 구동 + `GET /actuator/health` 200.
- [ ] **prometheus 엔드포인트**: `GET /actuator/prometheus` 200 + HikariCP/Tomcat/JVM 메트릭 노출 확인(exposure 보강 반영).
- [ ] **관측 스택 기동**: prometheus+grafana+node_exporter compose `up` (arm64) → Prometheus 에서 app target + node target 둘 다 `UP`.
- [ ] **Grafana 대시보드**: 호스트 CPU(node_exporter) / JVM heap / HikariCP pending·active / Tomcat busy / http p95 패널 데이터 수신.
- [ ] **Mock AI**: `localhost:9090` WireMock 호출 시 claude 3초 / whisper 1초 지연 확인(stub `fixedDelayMilliseconds`).
- [ ] **메모리 검산**: 전 컨테이너 `up` 후 `docker stats` + `free -m` → 합 ≤ 2048MB & OOMKilled 0. **초과/OOMKilled 발생 시 관측 스택 로컬 분리 fallback 전환**(트리거: 가용 메모리 < 100MB 또는 컨테이너 OOMKilled 1회).
- [ ] **k6 smoke**: 기존 follow-up-load-test.js 1개 시나리오 저부하 1회 정상 실행(환경 검증용, 본 측정 아님).
- [ ] **k6 threshold 회귀**: WireMock 3초 지연이 기존 follow-up-load-test.js threshold(http_req_duration p95 등) 가정과 충돌 없는지 확인 — 충돌 시 threshold 실험별 조정 메모.
- [ ] **베이스라인 기록**: 위 고정값 표 + 메모리 검산표가 docs/performance-tests/554-loadtest-environment/ 에 기록.
- [ ] **회귀(운영 무영향)**: develop 무변경 — 환경 자산은 별도 위치(설정 보강 PR 제외).

## Pre / Post State

### Pre (현재)
- 전용 부하 EC2 없음. loadtest = 로컬 docker mysql + 수동 mock(k6 README).
- 관측 스택(prometheus/grafana/node_exporter) compose 없음 (WireMock 만 compose 존재).
- `application-loadtest.yml` exposure 에 `prometheus` 미포함.
- Mock 두 갈래 병존(WireMock 3s/1s + mock-server.py 2s).

### Post (구현 후)
- 영속 t4g.small EC2 + start/stop 스크립트.
- 관측 스택 compose(arm64) + prometheus.yml scrape config + Grafana 대시보드 정의.
- exposure 에 `prometheus` 추가(loadtest 프로파일 한정).
- WireMock 단일 표준(claude 3s/whisper 1s) + `mock-server.py` dead 처리.
- 베이스라인 고정값 표 + 메모리 검산표 문서화.
- 3실험이 이 환경 위에서 브랜치 배포 → 측정 가능 상태.
- develop 런타임 동작 무변경(설정은 loadtest 프로파일/test 리소스 한정).

## 위험 / 마이그레이션 / 롤백

- **위험 1 (2GB 메모리 압박)**: app heap + MySQL + Mock + Prometheus + Grafana + node_exporter 공존 → OOM. → 컨테이너별 `mem_limit` 명시 + JVM `-Xmx768m` + Grafana/Prometheus 경량 설정. implement 시 합산 검증. 초과 시 관측 스택을 측정 PC(로컬)로 분리하는 fallback 명시.
- **위험 2 (ARM 호환)**: 모든 이미지 arm64 태그(mysql:8.0 / prom/prometheus / grafana/grafana / prom/node-exporter 모두 arm64 지원). JVM arm64. ffmpeg(영상실험 한정) arm64.
- **위험 3 (비용)**: EC2 상시 ON 비용 → start/stop 스크립트 측정 시에만 기동. **EC2 최초 생성 = 비용 발생 → 사용자 승인 게이트(아래)**.
- **위험 4 (exposure 보안)**: `prometheus` 엔드포인트 노출은 **loadtest 프로파일 한정** — dev/prod 프로파일 미적용 확인(actuator 무인증 노출 = A05 misconfiguration 회피).
- **마이그레이션**: 없음(스키마 무변경).
- **롤백**: 환경 = EC2 stop/terminate(사용자) + compose down(볼륨 보존). 설정 보강 PR = revert. 운영 영향 0.

## 분기 결정

- [x] **인프라/설정 중심 — sre-engineer 주도 + backend 일부 보강**:
  - **Task 1 — `backend`**: `application-loadtest.yml` exposure 에 `prometheus` 추가 + 베이스라인 설정값(Tomcat thread, `-Xmx` 주입 지점) 명시. loadtest 프로파일 한정. (소규모 설정 PR)
  - **Task 2 — `sre-engineer`**: 관측 스택 compose(arm64: prometheus+grafana+node_exporter) + prometheus.yml(app+node scrape) + Grafana 대시보드 JSON.
  - **Task 3 — `sre-engineer`**: WireMock 을 Mock 표준으로 확정 — claude stub 3000ms 유지 확인 + compose 와이어링 정리 + `mock-server.py` dead 처리(README 참조 제거). (지연 변경 = stub JSON)
  - **Task 4 — `sre-engineer`**: EC2 t4g.small 프로비저닝 스크립트(arm64 AL2023 + docker) + start/stop 스크립트 + 배포(jar scp/구동) 스크립트.
  - **Task 5 — `sre-engineer`**: 환경 smoke 검증(actuator/prometheus + 관측 target UP + Mock 지연 + k6 1회) + 베이스라인 표 문서화.

### 실행 주체 / 승인 게이트 (Blocking)
- **EC2 최초 생성·배포 = sre-engineer 실행, 단 2단계 게이트**: ① 프로비저닝/배포 스크립트 작성 + dry-run/plan 출력 → ② 사용자 명시 확인(비용 발생) → ③ `aws ec2 run-instances`/`start-instances`/scp apply 실행. sre-engineer 의 "mutating 허용(terminate 제외)" 범위(사용자 결정). **terminate = 사용자만**(destructive).
- app 배포(loadtest EC2 jar 구동) = test 환경이므로 sre-engineer 수행 범위(운영 prod 배포 아님).
- 본 결정 반영 위해 `.claude/agents/sre-engineer.md` 의 "배포=위임" 문구를 "loadtest 전용 EC2 mutating 허용(terminate/prod 제외)" 으로 명확화(별도 소규모 편집).
- 사용자 검토: 베이스라인 고정값 표 승인 = 모든 후속 실험 수치 해석 기준 → 승인 게이트.
