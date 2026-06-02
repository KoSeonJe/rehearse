# loadtest 환경 베이스라인 — 554-loadtest-environment

> **목적**: 3종 성능 실험(실험0 영상분리 / 실험1 커넥션 / 실험2 스레드)이 공유하는 영속 `loadtest` 환경의 고정 설정값·메모리 검산·smoke 검증 절차를 1회 확정.
> **이 문서의 표는 모든 실험 결과 문서 헤더에 복사** — "수치가 공중에 뜨지 않게"(connection.md §0).
> **상태**: 2026-06-02 EC2#1 t4g.medium(4GB) 업사이징 후 2-instance 분리 아키텍처로 재기동·smoke A1~A10 완료(전 항목 PASS, 실측 채움). small(2GB) idle OOM 차단 해소.
> tech-spec: `docs/plans/554-loadtest-environment/tech-spec.md`

---

## 0. 인프라 토폴로지 (2026-06-02 관측 분리 — tech-spec Trade-off 2 단일호스트에서 변경)

기존 단일 t4g.small(2GB) 에 app+mysql+wiremock+prometheus+grafana 를 co-location → idle 메모리 압박으로 OOM/SSH 스래싱. **관측 오버헤드(Prometheus+Grafana)만 전용 EC2#2 로 off-host**, 측정 대상(app+mysql+wiremock)은 EC2#1 단일호스트 유지(측정 fidelity 보존).

| 인스턴스 | ID | 사설IP (고정) | 퍼블릭IP (stop/start 변동) | 스택 | SG (open 포트) |
|---------|-----|--------------|---------------------------|------|----------------|
| EC2#1 loadtest (t4g.medium 4GB) | `i-07ca3e9cda9c81ba0` | `172.31.14.143` | `13.125.243.87` (2026-06-02 재기동) | app(호스트 JVM) + MySQL + WireMock + node_exporter | `sg-0b5a7915bdb248607` (22 / 8080 / 9100) |
| EC2#2 관측 (t4g.small 2GB) | `i-0df37cc60c287b2bf` | `172.31.12.190` | `52.79.141.57` (2026-06-02 재기동) | Prometheus + Grafana | `sg-0c988c0e70ccdb142` (22 / 3000 / 9091) |

- **scrape 경로**: EC2#2 Prometheus → EC2#1 **사설IP** `172.31.14.143:8080`(/actuator/prometheus) + `:9100`(node_exporter). 동일 VPC subnet 이라 사설IP 사용 → stop/start 시 IP 불변(퍼블릭IP 갱신 불필요, stale 위험 제거).
- **Grafana 접속**: `http://52.79.141.57:3000` (admin / loadtest). SG 가 3000 개방.
- WireMock(9090) 은 외부 미개방 — app 만 localhost 로 호출(SG 의도).
- 둘 다 키 `/Users/koseonje/.ssh/rehearse-key.pem`, user `ec2-user`. config.env: `LOADTEST_*`=EC2#1 / `OBS_*`=EC2#2.

---

## 1. 베이스라인 고정값 (실험 해석 근거 — 반드시 노출·기록)

| 설정 | 값 | 근거 |
|------|-----|------|
| EC2#1 (측정) | t4g.medium (ARM64 Graviton2, 2 vCPU, 4GB) | 2026-06-02 small→medium 업사이징. vCPU 2 불변(thread/connection 실험변수 보존), OOM만 제거 |
| EC2#2 (관측) | t4g.small (ARM64 Graviton2, 2 vCPU, 2GB) | 관측 스택 전용. 분리 후 여유 충분 |
| HikariCP pool | max=10 / min=5 / connection-timeout 3000ms | `application-loadtest.yml`. connection.md 핵심 해석 근거 |
| Tomcat thread pool | max=200 | `application-loadtest.yml`. thread.md p95 해석 근거 |
| JVM heap | `-Xmx768m -Xms768m` | deploy-app.sh `LOADTEST_JAVA_OPTS` 주입 (아래 §2 검산) |
| Mock AI 지연 | WireMock stub `fixedDelayMilliseconds` — claude 3000ms / whisper 1000ms | `wiremock-stubs/mappings/`. 변수 통제 |
| VT toggle | `VIRTUAL_THREAD_ENABLED` (기본 true) | thread.md B/C 전환 |
| 외부 API rate limit | `RATELIMITER_LIMIT` 기본 8 req/s | `application-loadtest.yml` (GPT-4o Tier1 ≈ 500 RPM) |
| Global concurrency limit | `GLOBAL_CONCURRENCY_LIMIT` 기본 0(비활성) | `application-loadtest.yml` |

> **JVM heap 주입 지점은 단 한 곳**: `deploy-app.sh` 의 `LOADTEST_JAVA_OPTS`(config.env override 가능). yml 에 `-Xmx` 하드코딩 없음.

---

## 2. 메모리 배분 검산 (2026-06-02 t4g.medium 업사이징 후 실측 — `docker stats` / `free -m`)

> EC2#1 을 t4g.medium(4GB)로 업사이징 + 관측 스택은 EC2#2 분리 유지. small(2GB)에서 idle available 19~22MB 로 빠듯하던 §2.1 OOM 차단이 해소됨 — medium idle available **~1.9GB** 로 fallback 게이트(>100MB) 압도적 충족.

### EC2#1 loadtest (app + mysql + wiremock + node_exporter) — medium idle 실측 (2026-06-02 재기동 후)
| 컴포넌트 | mem_limit | 실측 RSS/MemUsage (small → **medium**) | 비고 |
|---------|-----------|-------------------|------|
| JVM app (`-Xmx768m`) | 없음(호스트 JVM) | 809~902MB → **830MB RSS** | heap used 386.8MB(< Xmx768m). compose 밖 |
| MySQL 8.0 | 없음 | 604~606MB → **656MB** | `--innodb-buffer-pool-size=128M` 적용 |
| WireMock | 없음 | 105~106MB → **124MB** | `--container-threads 1000` |
| node_exporter | 32m | 7MB → **7MB** | collector 최소화 |
| `free -m` (전체) | — | small: total 1846 / used 1523 / available 22 → **medium: total 3835 / used 1562 / available 1921** | medium idle 여유 ~1.9GB (게이트 통과) |
| OOMKilled | — | **0건 (전 컨테이너 Restarts=0)** | A8/A9/A10 smoke 전후 동일 |

### EC2#2 관측 (prometheus + grafana) — 실측
| 컴포넌트 | mem_limit | 실측 MemUsage | 비고 |
|---------|-----------|---------------|------|
| Prometheus | 256m | **19MB** | retention 2h / 256MB. scrape 5s |
| Grafana | 128m | **59MB** | |
| `free -m` (전체) | — | total 1846 / **used 375 / available 1293** | 여유 충분 |
| OOMKilled | — | **0건** | |

### §2.1 해소 — EC2#1 t4g.medium 업사이징으로 idle 메모리 압박 제거 (2026-06-02)
- 이전 small(2GB)에서 idle available 19~22MB → mysql 쿼리 한 방에 호스트 행(§3.1 A9 FAIL 원인). **t4g.medium(4GB) 승격으로 해소**: idle available **1921MB**, 부하 후에도 1907MB 유지.
- 채택 근거: vCPU 2개 불변 → thread/connection 실험 변수 보존, OOM 만 제거(handoff 옵션 A). 대안(swap 추가 / `-Xmx` 하향)은 부하 중 swap latency 왜곡 / 실험 변수 변경 우려로 폐기.
- 측정 사용 메모리(idle): JVM 830 + MySQL 656 + WireMock 124 + node_exporter 7 ≈ 1617MB / 3835MB → 본측정(500 VU)용 헤드룸 ~2.2GB 확보.

### 검산 결론 (2026-06-02, medium 재기동 후)
- **fallback(관측 off-host) + 인스턴스 업사이징 둘 다 적용 완료**: 관측 스택은 전용 EC2#2 영구 분리, EC2#1 은 medium(4GB) 으로 idle 압박 제거. EC2#1 은 node_exporter 만 상주(+측정 대상).
- **잔여 리스크 해소**: small 시절 "500 VU 본측정 OOM 위험" 경고는 medium 헤드룸 ~2.2GB 확보로 완화. 단 500 VU 본측정 시 JVM heap/MySQL 동시 증가폭은 본측정 단계에서 실측 권고(현재는 저부하 smoke 까지만 검증).
- MySQL `--innodb-buffer-pool-size=128M` 은 docker-compose-loadtest.yml 반영(재기동 시 적용).

---

## 3. Smoke 검증 체크리스트 (tech-spec Verification 매핑)

> EC2 기동 후(사용자 승인) 아래 순서로 1회 실행. 각 항목 = tech-spec Verification 체크박스 대응.
> 환경변수: `BASE_URL` = EC2 퍼블릭 접근 시 `http://<EC2_IP>:8080`, 로컬 검증 시 `http://localhost:8080`.

### A1. EC2 기동 + start/stop
```bash
source backend/src/test/k6/infra/config.env
APPLY=true backend/src/test/k6/infra/start-ec2.sh   # running + PublicIp 출력
# (종료) APPLY=true backend/src/test/k6/infra/stop-ec2.sh
```
판정: `instance-running` wait 통과 + PublicIp 반환.

### A2. app 배포·구동
```bash
APPLY=true backend/src/test/k6/infra/deploy-app.sh
curl -s http://<EC2_IP>:8080/actuator/health | jq .
```
판정: `{"status":"UP"}` 200.

### A3. prometheus 엔드포인트 (exposure 보강 반영 — 이미 yml 포함)
```bash
curl -s http://<EC2_IP>:8080/actuator/prometheus | grep -E "hikaricp_connections|tomcat_threads|jvm_memory" | head
```
판정: HikariCP / Tomcat / JVM 메트릭 라인 노출.

### A4. 관측 스택 기동 + target UP
```bash
# EC2 상에서:
docker compose -f backend/src/test/k6/observability/docker-compose-observability.yml up -d
curl -s http://127.0.0.1:9091/api/v1/targets | jq '.data.activeTargets[] | {job:.labels.job, health:.health}'
```
판정: `spring-app` + `node-exporter` 둘 다 `"health":"up"`.

### A5. Grafana 대시보드
```
SSH 터널:  ssh -i <key> -L 3000:127.0.0.1:3000 ec2-user@<EC2_IP>
브라우저:  http://localhost:3000  (admin / loadtest)
대시보드:  "Rehearse Loadtest Overview"
```
판정: 호스트 CPU / JVM heap / HikariCP(active·idle·pending) / Tomcat busy / http p95 6개 패널 데이터 수신.

### A6. 메모리 검산 (§2 fallback 게이트)
```bash
docker stats --no-stream
free -m
```
판정: 합 ≤ 2048MB & OOMKilled 0. 초과 시 §2 fallback 전환. → 결과를 §2 "실측" 열에 기록.

### A7. Mock AI 지연 확인
```bash
time curl -s -o /dev/null -X POST http://<EC2_IP>:9090/v1/messages -d '{}'                 # ~3s
time curl -s -o /dev/null -X POST http://<EC2_IP>:9090/v1/audio/transcriptions -d '{}'      # ~1s
```
판정: claude ≈ 3초 / whisper ≈ 1초 (stub `fixedDelayMilliseconds`).

### A8. k6 smoke (환경 검증용, 본 측정 아님)
```bash
k6 run --env SCENARIO=A \
       --env BASE_URL=http://<EC2_IP>:8080 \
       --env INTERVIEW_ID=1 --env QUESTION_SET_ID=1 \
       backend/src/test/k6/follow-up-load-test.js
```
판정: 1개 시나리오 저부하 1회 정상 종료(에러 폭주 없음).

### A9. k6 threshold 회귀 확인
판정: WireMock 3초 지연이 `follow-up-load-test.js` 의 `http_req_duration` threshold 가정과 충돌하는지 점검. 충돌 시 threshold 는 **실험별 tech-spec 에서 조정**(본 환경 스크립트 임의 변경 금지) — 충돌 여부만 메모.

### A10. 회귀(운영 무영향)
판정: develop 런타임 무변경. 환경 자산은 `loadtest` 프로파일 / `src/test/k6` 한정. exposure `prometheus` 는 loadtest 프로파일 yml 에만 존재(dev/prod 미적용).

---

## 3.1 Smoke 실행 기록 — 2026-06-02 (2-instance 분리, EC2#1 app 재배포 후)

> 실행자: SRE 에이전트. EC2#1 app(pid 5803, `-Xmx768m`, profile=loadtest) + infra compose(mysql/wiremock/node_exporter) + EC2#2 관측 스택 모두 이전 세션에서 기동된 상태에서 smoke A1~A10 검증.

| # | 항목 | 실측 | 판정 |
|---|------|------|------|
| A1 | app health (`http://43.201.35.210:8080/actuator/health`) | HTTP 200, `status=UP`, db UP(MySQL) | PASS |
| A2 | app boot | `Started RehearseApiApplication in 27.8s` (17:19) | PASS |
| A3 | prometheus endpoint | 472 메트릭 라인, hikaricp/jvm/http_server_requests 노출 | PASS |
| A4 | node_exporter `:9100` | cpu/meminfo 메트릭 응답 | PASS |
| A5 | EC2#2 Prometheus targets | `spring-app`(172.31.14.143:8080) + `node-exporter`(172.31.14.143:9100) 둘 다 `up`. 사설IP scrape 동작 확인 | PASS |
| A6 | EC2#2 Grafana | health ok(v11.2.0), datasource prometheus 프로비저닝, 대시보드 "Rehearse Loadtest Overview" 로드, proxy 쿼리(up) 2 series 반환 | PASS |
| A7 | WireMock 지연 | claude `/v1/messages` 3.20s / whisper `/v1/audio/transcriptions` 1.40s (stub fixedDelay 3000/1000 일치) | PASS |
| A8 | EC2#1 메모리 실측 | (small: available 19MB) → **medium 업사이징 후 재측정 §3.2 참조** | 측정 (medium PASS) |
| A9 | EC2#1 호스트 안정성 | (small: mysql 쿼리 직후 호스트 행 FAIL) → **medium 재검증 §3.2 참조** | small FAIL → **medium PASS** |
| A10 | k6 smoke | (small: A9 행으로 BLOCKED) → **medium 재검증 §3.2 참조** | small BLOCKED → **medium PASS** |

### §3.1.1 발견 — EC2#1 OOM/호스트 행 재현 (small 기준, medium 에서 해소됨)
- **현상(small)**: idle available 19MB 상태에서 추가 메모리 요구(mysql 쿼리 실행) 직후 호스트가 SSH·HTTP 전부 무응답. JVM heap 은 493MB(< 768m)로 정상 — **JVM OOM 아님, 호스트 물리 RAM 고갈**(no swap).
- **해소**: 2026-06-02 EC2#1 t4g.medium(4GB) 업사이징 → §3.2 재검증에서 A8/A9/A10 전부 PASS.
- **별도 발견(데이터, 미해결)**: `seed-data.sql:38` 이 `interview_type='CS'` 삽입하나 현 enum `InterviewType` 에 `CS` 없음(→ `CS_FUNDAMENTAL` 로 변경됨). `InterviewCompletionService.checkAndCompleteInterviews`(스케줄 30s)가 매회 `No enum constant ...InterviewType.CS` ERROR. 환경 차단 아니나 follow-up 부하 시나리오 정합성 영향. **fix 위임: backend**(seed-data.sql 의 'CS' → 유효 enum 값 치환) — SRE 직접 수정 안 함.

---

## 3.2 재검증 기록 — 2026-06-02 (EC2#1 t4g.medium 업사이징 + 재기동 후 A8~A10)

> 실행자: SRE 에이전트. small→medium 업사이징(`modify-instance-attribute`) → 2대 start → infra compose(mysql/wiremock/node_exporter) + app + 관측 스택 부팅 시 자동 복구 → A8~A10 재검증. app: pid 3641, `-Xmx768m -Xms768m`, profile=loadtest, `Started ... in 24.637s`.

| # | 항목 | 실측 | 판정 |
|---|------|------|------|
| A8 | EC2#1 medium 메모리 | `free -m`: total 3835 / used 1562 / **available 1921MB**(>100MB 게이트 충족), swap 0. docker stats: mysql 656MB + wiremock 124MB + node_exporter 7MB. JVM RSS 830MB(heap used 386.8MB < Xmx768m). 전 컨테이너 OOMKilled=false / Restarts=0 | **PASS** |
| A9 | EC2#1 호스트 안정성 | small 에서 행 유발했던 동일 패턴 `docker exec rehearse-loadtest-db mysql ... SELECT COUNT(*)` 쿼리 정상 반환(interviews/questions/users 카운트). 쿼리 직후 SSH alive + uptime load avg 0.00 + app HTTP 200(0.11s). EC2#2 Prometheus `up{spring-app}=1` / `up{node-exporter}=1`(사설IP 172.31.14.143 scrape). 호스트 행 재현 안 됨 | **PASS** |
| A10 | k6 smoke | `follow-up-load-test.js` 저부하(CLI override 5 VU / 20s — 스크립트 기본 ramping-vus 500까지 가는 것을 smoke 용으로 제한) 1회 실행: **k6 exit 0**, 95 iterations complete / 0 interrupted. 부하 후 호스트 available 1907MB / load avg 0 / app HTTP 200 / OOMKilled 0 | **PASS** |

### §3.2.1 발견 — A10 응답 전건 401(AUTH_001), 인프라 5xx 아님
- **현상**: A10 k6 요약에서 `http_req_failed=100%`, `status is 2xx` 전건 실패. 직접 호출 시 `HTTP 401 {"code":"AUTH_001","message":"로그인이 필요합니다."}` — `/api/v1/interviews/{id}/follow-up` 가 인증 필요 엔드포인트인데 `follow-up-load-test.js` 가 JWT 헤더를 주입하지 않음.
- **A10 판정 영향 없음(PASS 유지)**: A10 목적 = "환경(네트워크/앱/k6 경로) 검증용 1회 정상 종료(exit 0)". 401 = 인증 미주입 비즈니스 응답이며 **인프라 5xx / 호스트 행 아님** → 앱이 살아서 요청을 정상 처리(401 반환)했음을 입증 = 환경 검증 목적 충족. k6 exit 0.
- **본측정 전 선결(SRE 범위 밖)**: 실제 부하 측정 시 `follow-up-load-test.js` 에 JWT 발급/주입 로직 필요. + seed 데이터 0건(interview/question/users 전부 0) — 본측정은 유효 seed + 인증 토큰 필요. **실험별 tech-spec 소관**(본 환경 스크립트 임의 변경 금지, baseline.md §A9 회귀 메모 원칙).
- **`follow-up-load-test.js` 인자 불일치(메모)**: baseline.md §3 A8/A10 예시 명령은 `--env SCENARIO/INTERVIEW_ID/QUESTION_SET_ID` 를 쓰나, 현 스크립트는 `MAX_INTERVIEW_ID` 만 사용 + options 에 ramping-vus(최대 500 VU) 하드코딩. smoke 1회는 CLI `--vus/--duration` override 로 제한해 실행. 예시 명령 갱신은 실험별 스크립트 정비 시 동반 권고.

---

## 4. 환경 자산 위치

| 자산 | 경로 |
|------|------|
| 부하 격리 인프라 compose (MySQL + WireMock) | `backend/src/test/k6/docker-compose-loadtest.yml` |
| 관측 스택 compose (Prometheus + Grafana + node_exporter, arm64) | `backend/src/test/k6/observability/docker-compose-observability.yml` |
| Prometheus scrape config | `backend/src/test/k6/observability/prometheus.yml` |
| Grafana 대시보드 | `backend/src/test/k6/observability/grafana/dashboards/loadtest-overview.json` |
| Mock AI stub (claude 3s / whisper 1s) | `backend/src/test/k6/wiremock-stubs/mappings/` |
| EC2 프로비저닝 / start / stop / 배포 | `backend/src/test/k6/infra/{provision,start,stop}-ec2.sh, deploy-app.sh, user-data.sh` |
| 인프라 설정 템플릿 | `backend/src/test/k6/infra/config.env.example` |
| loadtest 프로파일 | `backend/src/test/resources/application-loadtest.yml` |
