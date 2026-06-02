# 실험2 — 스레드 병목: 가상스레드 검증

> 측정 환경: 554 loadtest (EC2#1 t4g.medium 측정 + EC2#2 관측). 환경 자산 = `../baseline.md`, `../aws-resources.md`.
> 원본 지시서: `docs/local/thread.md` / 증명 대상: `docs/local/resume.md`.
> 상태: **계획**. 🔗 **베이스라인 = 실험1의 B(트랜잭션 분리 적용 상태)** — 반드시 분리 적용 후 측정.

---

## 1. 목적 & 가설

AI 꼬리질문 생성 API는 외부 AI 호출이 **요청 처리 스레드를 점유**해, 고부하에서 톰캣 스레드 풀이 포화되고 큐잉이 발생한다. 이 실험은 **가상스레드를 도입하면 스레드 포화로 인한 응답 지연이 해소되는지**를 비교 측정한다. 고부하(100rps)에서 드러나는 병목이다.

**증명 대상 (resume.md 18, 24행)**:
> 100rps 기준 스레드 풀 포화로 큐잉이 발생해 **p95 응답시간이 16.2초**가 되었습니다.
> 가상스레드 도입 후, 100rps 기준 **p95 응답시간이 16.2초 → 3.1초로 80% 단축**되었습니다.

**가설**: B(플랫폼 스레드)는 외부 호출 동안 요청 스레드가 묶여, 동시 요청이 톰캣 풀(200)을 초과하면 큐 대기로 p95가 급등한다. C(가상스레드)는 I/O 대기 중 캐리어 스레드를 놓아줘 대량 동시성을 수용, p95가 외부 호출 시간에 수렴한다.

## 2. 진실성 원칙

- 코드 구조(VirtualThread executor, `DelegatingSecurityContextExecutor` 래핑)는 이미 실제 코드와 일치 확인됨(`AsyncConfig.vtExecutor`). 검증 대상은 **부하 수치**다.
- "16.2초 → 3.1초"는 본 실험 실측으로 채운다. 측정 안 한 수치 미기재.
- 베이스라인 톰캣 스레드 풀 크기 명시 없으면 p95 해석 불가 → §4 고정.

## 3. 비교군

| | 구성 | 설명 |
|---|---|---|
| **B (베이스라인)** | 트랜잭션 분리 O + **플랫폼 스레드** | 외부 호출이 요청(톰캣) 스레드를 점유. 고부하 시 풀(200) 포화 → 큐 대기 |
| **C (가상스레드)** | 트랜잭션 분리 O + **가상스레드** | AI 호출을 `vtExecutor`(`Executors.newVirtualThreadPerTaskExecutor`)에 offload, 컨트롤러는 `CompletableFuture` async 반환 → I/O 대기 중 캐리어 반납 |

> 두 비교군 모두 실험1의 B(트랜잭션 분리)가 적용된 상태다. 차이는 스레드 모델뿐.

## 4. 고정 변수 (B/C 동일)

| 변수 | 값 |
|------|----|
| EC2 인스턴스 | t4g.medium (2 vCPU, 4GB) |
| HikariCP pool | 10 (실험1과 동일, 트랜잭션 분리 적용) |
| **톰캣 스레드 풀** | `server.tomcat.threads.max: 200` (`application-loadtest.yml:3`) ← p95 해석 근거 |
| **외부 AI = WireMock 고정 지연** | STT `1000ms` + 꼬리질문 생성 `3000ms` ≈ **요청당 외부 ~4초** |
| 부하 | 100rps, 3분 유지 |
| 반복 | 최소 3회, 중앙값 |

## 5. 측정 지표 → resume 수치 매핑 (핵심)

| resume 수치 | 메트릭 | 측정 방법 | 결과 매핑 |
|------------|--------|----------|----------|
| p95 16.2초→3.1초 | `http_server_requests_seconds` p95 (Micrometer) | 100rps 정상상태 p95. B vs C | B=[~16s] / C=[~3s] |
| (증명) 스레드 포화 | `tomcat_threads_busy` | B 에서 max(200)에 붙는지 / C 는 낮게 유지 | B=200 / C≪200 |
| (증명) 처리량 유지 | throughput (완료 req/s, k6 + `http_server_requests_seconds_count`) | C 에서 목표 rps 유지되는지 | B<100 / C≈100 |
| (보조) CPU/heap | `node_cpu_seconds_total`, `jvm_memory_used_bytes` | VT 도입이 자원 폭주 부르지 않는지 | B≈C |

Grafana 패널 `tomcat.threads`/`HTTP p95·p99`/`throughput` 모두 존재 → 추가 불필요.

**산술 점검 (방어 근거)**: 100rps × 외부 ~4초 = 동시 ~400건 in-flight. 톰캣 200(B) → ~200건 큐 대기 → 누적 지연으로 p95 급등. C 는 캐리어 반납으로 ~400건 수용 → p95 ≈ 외부 호출 시간(~4초)에 수렴. **이 산술이 측정값과 맞아야 방어된다.** (resume 외부 "평균 3초"·p95 "3.1초" vs 본 환경 mock ~4초 → C p95 ~4초대 예상. 결과 표에 실제 지연 명시, 필요 시 STT 제외로 ~3초 정렬)

> **⚠️ CPU 교란 통제 (최대 위험)**: t4g.medium 2vCPU 에서 100rps + C 의 ~400 동시성 + WireMock(co-located, 400 동시 응답 생성) + MySQL + JVM 이 같은 2코어 경합. C 의 p95 가 "외부 4초 수렴"이 아니라 **CPU 큐 대기로 부풀면** "스레드 모델 차이" 단일 변수 비교가 오염된다(면접 "그거 CPU 부족 아니냐" 반박 취약). 통제: (1) B/C 둘 다 `node_cpu`·`process_cpu_usage`·WireMock CPU 기록 → **C 의 host CPU peak 이 saturation(>90%) 닿으면** "C 의 p95 는 CPU-bound 라 스레드 효과 과소측정" 명시. (2) 100rps 단일이 아니라 **50→75→100rps 스윕**으로 "B 꺾이고 C 유지되는 변곡점(스레드 한계)"과 "C 도 꺾이는 점(CPU 한계)"을 분리 관찰. (3) WireMock 을 측정호스트 밖으로 옮길지 = baseline §0 co-location 결정과 trade-off (사용자 판단).

## 6. 필요 구현 (구현명세)

현재 코드 = **C(가상스레드 offload)만 존재** (`InterviewController:98 CompletableFuture.supplyAsync(vtExecutor)`, `AsyncConfig.vtExecutor` 가상 고정). B(플랫폼 스레드) 비교군 토글 신규 필요.

| # | 구현 | 위치 | 담당 |
|---|------|------|------|
| 1 | B/C 토글 — ⚠️ **단일 메서드 플래그 분기 불가**. 컨트롤러 반환타입이 `CompletableFuture<ResponseEntity>` 고정(`InterviewController:90`) → 항상 async dispatch. B(동기, 톰캣 스레드 점유)는 반환 `ResponseEntity` 라야 함 → **반환타입 다른 두 컨트롤러 빈** `@ConditionalOnProperty(VIRTUAL_THREAD_ENABLED)` 로 스위칭 (`SyncInterviewController`=동기 직접호출 / `AsyncInterviewController`=현 supplyAsync+vtExecutor) | `InterviewController` / `AsyncConfig` | `backend` |
| 2 | `spring.threads.virtual.enabled=false`(톰캣 스레드 전용) — **이것만으론 부족**. `vtExecutor` 빈은 yml 과 독립적으로 항상 가상스레드(`AsyncConfig:19`) → #1 의 동기 컨트롤러(vtExecutor 미호출)가 반드시 동반돼야 "플랫폼 스레드 점유" 성립 | `application-loadtest.yml` | `backend` |
| 3 | **pinning 점검** — 외부 호출 스택(`AudioTurnAnalysisService`, `WhisperService`, RestClient, resilience4j)의 `synchronized` 블로킹이 캐리어 고정 시 C 효과 소실. **측정 전 호출스택 `synchronized` grep 사전 식별** + `-Djdk.tracePinnedThreads=full` 확인. 발견 시 (a) `ReentrantLock` 교체 가능하면 minimal fix, (b) 라이브러리 내부면 한계로 명기 | JVM opts (`config.env` `LOADTEST_JAVA_OPTS`) | `backend`/`sre-engineer` |
| 4 | ✅ **구현 완료** — k6 `audio` 더미 webm 파트 + JWT 헤더 (`_auth.js` 공유 헬퍼, sub"1"/role"USER"). B/C 토글 코드(#1·#2)만 잔여 | `test-b-virtual-thread.js` + `_auth.js` | done |

> ⚠️ **측정 포인트 주의** (thread.md): 컨트롤러가 `CompletableFuture` 반환(async servlet) → C 에서 톰캣 스레드가 실제로 풀리는지 확인. B 가 동기 실행으로 톰캣 스레드를 외부 호출 내내 점유해야 `tomcat.threads.busy` 포화가 관측됨. 면접 단골 질문 — 답변 준비.

## 7. 부하 시나리오 + 공통 블로커

- **부하**: `constant-arrival-rate` 100rps, 3분 유지. B/C 각 3회, 중앙값.
- **k6**: `backend/src/test/k6/test-b-virtual-thread.js` 재사용 (stage4 = 100rps; 100rps 단일 stage 3분으로 좁혀도 됨). `audio` 파트 + JWT 헤더 추가.
- **공통 블로커 해결** (실험1 §7 과 동일, 선결):
  - **[P0] seed user_id**: interview `user_id` NULL → `validateOwner` 전건 `NOT_FOUND`. seed 에 user 행 + interview.user_id + JWT subject 일치.
  - **[P0] OpenAI mock**: follow-up = OpenAI primary. loadtest yml base-url override + `openai-api` ratelimiter + `/v1/chat/completions` stub 없으면 실제 OpenAI 직격.
  - JWT: `jwt.secret`(HS256) 토큰(subject=seed user_id) → k6 `setup()` 헤더.
  - seed: `mysql rehearse_loadtest < seed-data.sql`.

## 8. 산출물 포맷 (resume 빈칸 채움)

```
100rps, 톰캣 스레드 200개, 외부 지연 ~4초, 인스턴스 t4g.medium (트랜잭션 분리 적용), 반복 3회 중앙값
                  B(플랫폼 스레드)   C(가상스레드)
API p95           ___ ms             ___ ms
API p99           ___ ms             ___ ms
톰캣 busy 피크     ___ (=max?)        ___
처리량            ___ req/s          ___ req/s
```

## 9. 실행 절차

1. `source infra/config.env` → `bash infra/start-ec2.sh`
2. B 회차: `VIRTUAL_THREAD_ENABLED=false` + 동기 실행 + `LOADTEST_JAVA_OPTS` 에 `-Djdk.tracePinnedThreads=full` 배포 / C 회차: `VIRTUAL_THREAD_ENABLED=true` (현 구조) 배포
3. seed 적재 + JWT 토큰 발급(§7)
4. **k6 실행 위치 = EC2#2(또는 별도 인스턴스)에서 EC2#1 사설IP** `172.31.14.143:8080` (측정호스트 CPU 잠식 방지 — 실험2 CPU 교란 직결). EC2#1 자체 실행 금지. `k6 run --env BASE_URL=http://172.31.14.143:8080 test-b-virtual-thread.js` (100rps 또는 50→75→100 스윕)
5. 워밍업 1회 버림 + 본 3회, 정상상태 60s 윈도우 추출, `dropped_iterations`=0 확인. B/C 둘 다 host/JVM CPU 동시 기록
6. `collect-metrics.sh` 스냅샷 + EC2#2 Grafana API p95 / tomcat.threads.busy / CPU 패널 캡처

## 10. Verification

- B/C 각 3회 측정 + §8 표 중앙값 채움.
- B: `tomcat_threads_busy` max(200) 포화 + p95 급등(수~수십초). C: busy ≪ 200 + p95 ≈ 외부 호출 시간.
- 산술(100rps×~4초, 톰캣 200 → ~200 큐 대기)과 측정 정합.
- pinning 로그(`tracePinnedThreads`)에 캐리어 고정 미발생 (VT 효과 유효 확인).
