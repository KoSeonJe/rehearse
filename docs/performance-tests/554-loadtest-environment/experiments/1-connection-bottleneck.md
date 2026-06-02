# 실험1 — 커넥션 병목: 트랜잭션 분리 검증

> 측정 환경: 554 loadtest (EC2#1 t4g.medium 측정 + EC2#2 관측). 환경 자산 = `../baseline.md`, `../aws-resources.md`.
> 원본 지시서: `docs/local/connection.md` / 증명 대상: `docs/local/resume.md`.
> 상태: **계획**. 🔗 본 실험의 **B(분리) 상태가 곧 실험2의 시작 베이스라인**이다.

---

## 1. 목적 & 가설

AI 꼬리질문 생성 API는 외부 AI 호출이 **DB 트랜잭션 안에서** 일어나면, 응답을 기다리는 수 초 내내 DB 커넥션을 점유한다. 이 실험은 **외부 호출을 트랜잭션 밖으로 분리하면 커넥션 점유가 실제로 해소되는지**를 A/B 비교로 검증한다. 저부하(10rps)에서도 드러나는 병목이라 부하를 크게 줄 필요 없다.

**증명 대상 (resume.md 19, 25행)**:
> 외부 API 응답을 기다리는 동안에도 DB 커넥션을 점유하여, 부하 테스트 시 **10rps에서 커넥션 타임아웃으로 인한 에러율이 30.6%**까지 치솟았습니다.
> 트랜잭션 분리 후, 10rps 기준 **DB 커넥션 대기가 30개 → 0개**로 해소되었으며, **에러율이 30.6% → 1% 미만**으로 개선되었습니다.

**가설**: A(외부 호출이 트랜잭션 안)는 커넥션이 외부 호출 시간만큼 점유돼, pool(10)을 초과한 요청이 대기·타임아웃된다. B(분리)는 커넥션이 짧은 쿼리 순간에만 점유돼 대기가 사라진다.

## 2. 진실성 원칙

- 코드 구조(트랜잭션 3단계 분리)는 이미 실제 코드와 일치 확인됨(`FollowUpService:38 @Transactional(NOT_SUPPORTED)` = B). 검증 대상은 **부하 수치**다.
- "30→0, 30.6%→1%"는 본 실험 실측으로 채운다. 측정 안 한 수치 미기재.
- 베이스라인 설정값(pool 크기·외부 지연)을 명시하지 않으면 수치가 공중에 뜬다 → §4 고정.

## 3. 비교군

| | 구성 | 설명 |
|---|---|---|
| **A (베이스라인)** | 외부 호출이 `@Transactional` 안 | 조회·외부호출·저장이 한 트랜잭션 → 커넥션을 외부 호출 내내 점유 |
| **B (분리, 현 구조)** | 외부 호출을 트랜잭션 밖으로 | `loadFollowUpContext`(readOnly, 짧음) → 분석/생성(트랜잭션 밖) → `saveFollowUpResult`(tx, 짧음). 커넥션은 쿼리 순간만 점유 (`FollowUpService` + `FollowUpTransactionHandler`) |

## 4. 고정 변수 (A/B 동일)

| 변수 | 값 |
|------|----|
| EC2 인스턴스 | t4g.medium (2 vCPU, 4GB) |
| **HikariCP pool** | `maximum-pool-size: 10` (`application-loadtest.yml:17`) ← 수치 해석 핵심 |
| connection-timeout | `3000ms` (`application-loadtest.yml:19`) ← 타임아웃 에러 임계 |
| Tomcat 스레드 | `max: 200` (`application-loadtest.yml:3`) |
| **외부 AI = WireMock 고정 지연** | STT `1000ms`(`whisper-stt.json`) + 꼬리질문 생성 `3000ms`(`claude-followup.json`) ≈ **요청당 외부 ~4초** |
| 부하 | 10rps, 3분 유지 |
| 반복 | 최소 3회, 중앙값 |

## 5. 측정 지표 → resume 수치 매핑 (핵심)

| resume 수치 | 메트릭 | 측정 방법 | 결과 매핑 |
|------------|--------|----------|----------|
| 커넥션 대기 30→0 | `hikaricp_connections_pending` (Micrometer) | 10rps 정상상태 pending peak. A vs B | A=[~30] / B=[0] |
| 에러율 30.6%→<1% | `hikaricp_connections_timeout` 카운터 / 전체 요청 (+ k6 `error_rate`) | 커넥션 획득 타임아웃 비율. A vs B | A=[~30%] / B=[<1%] |
| (보조) acquire 대기 | `hikaricp_connections_acquire` 타이머 p95 | 커넥션 획득 지연. A vs B | A≫B |
| (보조) active | `hikaricp_connections_active` | 활성 커넥션 (A=10 풀포화) | A=10 / B≈낮음 |
| (보조) API p95 | `http_server_requests_seconds` p95 | 참고 | A>B |

> **메트릭 보강 확인**: HikariCP `acquire`/`timeout` 메트릭이 Micrometer HikariCP 바인딩으로 노출되는지 측정 전 확인. 미노출 시 보강 필요. `pending`/`active` 는 노출 확인됨(Grafana 패널 존재).

**산술 점검 (방어 근거)**: 10rps × 외부 ~4초 = 동시 ~40건 in-flight. pool 10 → A 에서 ~30건이 대기로 쌓임 → resume "대기 30" 과 정합. connection-timeout 3000ms < 점유 ~4초 → 대기 요청이 타임아웃 → 높은 에러율. **이 산술이 측정값과 맞아야 방어된다.** (resume 의 외부 "평균 3초"와 본 환경 mock ~4초 차이 → 결과 표에 실제 지연 명시)
> **주의 — pending 은 정적 30 이 아니라 동적**: connection-timeout(3000ms) 으로 대기 요청이 타임아웃돼 빠져나가면 pending 이 30 에 고정되지 않고 회전한다. 측정 시 **pending 평균 + peak 둘 다 기록**하고, resume "30→0" 은 측정 peak 중앙값으로 채우되 산술과 차이나면 "타임아웃 회전으로 실측 pending=N" 주석.

## 6. 필요 구현 (구현명세)

현재 코드 = **B(분리)만 존재**. A 비교군(외부 호출 트랜잭션 안) 토글 신규 필요.

| # | 구현 | 위치 | 담당 |
|---|------|------|------|
| 1 | A 토글 — 외부 호출(analyze + write)을 한 트랜잭션 안에서 실행하는 분기. 플래그 `FOLLOWUP_TX_INLINE` 또는 `@ConditionalOnProperty` 별도 빈 (`InlineTxFollowUpService`) | `FollowUpService` / `application-loadtest.yml` | `backend` |
| 2 | ✅ **구현 완료** — k6 `audio` 더미 webm 파트(EBML magic bytes → `AudioValidator.isWebm()` 통과) + JWT 헤더. 공유 헬퍼 `backend/src/test/k6/_auth.js`(`issueLoadTestJwt`=sub"1"/role"USER", `dummyWebmBytes`) | `test-a-tx-separation.js` + `_auth.js` | done |
| 3 | A 토글 코드(`InlineTxFollowUpService` + handling_mode)만 잔여 — 실험 착수 시 구현 | `backend` | |

- **A 구현 방식 (⚠️ 한 줄 `@Transactional` 아님)**: 현 DB 작업은 `generateFollowUp` 본문이 아니라 **별도 빈** `FollowUpTransactionHandler` 의 `loadFollowUpContext`(readOnly)·`saveFollowUpResult`(write)·`publishAnswerAnalysisCompletedEvent` 에 분산돼 있다(`FollowUpTransactionHandler.java:41,103,151`). `generateFollowUp` 에 `@Transactional(REQUIRED)` 만 걸어도 본문에 직접 쿼리가 0줄이라 "외부호출 동안 커넥션 점유"가 보장되지 않는다. → A 는 **신규 별도 빈 `InlineTxFollowUpService`** 의 단일 public 메서드에서 `loadContext→analyze(외부)→write(외부)→save` 를 직접 EntityManager/Repository 로 수행하는 흐름으로 구현 + `application-loadtest.yml` 에 `spring.jpa.properties.hibernate.connection.handling_mode=DELAYED_ACQUISITION_AND_HOLD` 명시(첫 쿼리 후 트랜잭션 끝까지 커넥션 보유 강제 = A 의 본질). `@ConditionalOnProperty(FOLLOWUP_TX_INLINE)` 로 B 와 전환.
- **self-invocation 금지**: A 인라인 트랜잭션은 반드시 별도 빈의 public 진입점. 같은 클래스 private 메서드에 `@Transactional` 부착 시 AOP 프록시 우회로 무효화됨(현 `FollowUpService`→`FollowUpTransactionHandler` 는 서로 다른 빈이라 안전, 신규 빈도 동일 원칙 준수).
- **이벤트는 A/B 차이와 무관**: `AnswerAnalysisCompletedEvent` 발행은 있으나 main 측 `@TransactionalEventListener` 구독처가 없음(테스트만 구독). A/B 차이는 **트랜잭션 경계뿐** — 이벤트를 커넥션 점유 검증 요소로 거론하지 말 것.

## 7. 부하 시나리오 + 공통 블로커

- **부하**: `constant-arrival-rate` 10rps, 3분 유지. A/B 각 3회, 중앙값.
- **k6**: `backend/src/test/k6/test-a-tx-separation.js` 재사용. 현재 stage 10/20/30rps → **10rps 단일 stage 3분**으로 조정(resume 기준). `audio` 파트 + JWT 헤더 추가.
- **공통 블로커 해결 (선결, 미해소 시 측정 불가)**:
  - **[P0] seed user_id**: `seed-data.sql` interview 의 `user_id` 가 NULL → `Interview.validateOwner`(`Interview.java:173-178`)가 전건 `NOT_FOUND` 차단. JWT 만으론 안 됨. → seed 에 `users` 행 + `interview.user_id` 채움 + **JWT subject 를 그 user_id 와 일치**. (JWT 필터 자체는 DB 조회 없지만, 소유자 검증이 interview.user_id 대조)
  - **[P0] OpenAI mock**: follow-up 실제 호출 = OpenAI(primary). loadtest yml 에 openai base-url override + `openai-api` ratelimiter + `/v1/chat/completions` WireMock stub 없으면 **실제 OpenAI 직격**(비용/비결정/보안). → 선결 수정 필요(`application-loadtest.yml` + `wiremock-stubs/`).
  - JWT: loadtest yml `jwt.secret`(HS256) 토큰 발급(subject=seed user_id, role) → k6 `setup()` 헤더 주입.
  - seed: `mysql rehearse_loadtest < seed-data.sql`.

## 8. 산출물 포맷 (resume 빈칸 채움)

```
10rps, 커넥션 풀 10개, 외부 지연 ~4초(STT 1s + 생성 3s), 인스턴스 t4g.medium, 반복 3회 중앙값
                       A(트랜잭션 내)   B(분리)
커넥션 대기 피크         ___ 개          ___ 개
커넥션 타임아웃 에러율   ___ %           ___ %
acquire p95             ___ ms          ___ ms
API p95                 ___ ms          ___ ms
```

## 9. 실행 절차

1. `source infra/config.env` → `bash infra/start-ec2.sh`
2. A 회차: `FOLLOWUP_TX_INLINE=true` 배포 / B 회차: `false` 배포 (`infra/deploy-app.sh`)
3. seed 적재 + JWT 토큰 발급(§7)
4. **k6 실행 위치 = EC2#2(또는 동일 VPC 별도 인스턴스)에서 EC2#1 사설IP** `172.31.14.143:8080` 로 부하 (측정호스트 CPU 잠식·인터넷 RTT 제거). EC2#1 자체 실행 금지. `k6 run --env BASE_URL=http://172.31.14.143:8080 test-a-tx-separation.js` (10rps 3분)
5. **측정 전** `curl :8080/actuator/prometheus | grep hikaricp_connections_timeout` 로 timeout/acquire 시리즈 실재 확인(미노출 시 보강). 워밍업 1회 버림 + 본 3회, 정상상태 60s 윈도우만 추출, k6 `dropped_iterations`=0 확인
6. `collect-metrics.sh` 스냅샷 + EC2#2 Grafana HikariCP 패널 캡처

## 10. Verification

- A/B 각 3회 측정 + §8 표 중앙값 채움.
- A: pending peak ≈ 산술(~30) ± 합리 범위, timeout 에러율 유의(>수십%). B: pending 0, 에러율 <1%.
- 산술(10rps×~4초, pool 10 → ~30 대기)과 측정 pending 정합.
- B 상태 그대로 실험2 베이스라인으로 인계.
