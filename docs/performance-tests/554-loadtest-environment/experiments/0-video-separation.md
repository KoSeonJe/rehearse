# 실험0 — 영상 처리 책임 분리 검증

> 측정 환경: 554 loadtest (EC2#1 t4g.medium 측정 + EC2#2 관측). 환경 자산 = `../baseline.md`, `../aws-resources.md`.
> 원본 지시서: `docs/local/a-btest.md` / 증명 대상: `docs/local/resume.md`.
> 상태: **계획**. ⚠️ 본 실험은 A 비교군 구현 비용이 실험1/2 보다 훨씬 큼 → **실험1·2 완료 후 별도 진행 권장**.

---

## 1. 목적 & 가설

devlens 는 처음부터 무거운 작업(영상 변환·분석)을 앱 서버(EC2) 밖으로 분리해 설계했다. 이 실험은 **그 설계 판단이 옳았는지 사후 검증**한다 — "EC2 단일 서버에서 다 처리했다면?"을 실제 구현·측정해 분리 효과를 숫자로 증명한다.

**증명 대상 (resume.md 12-13행)**:
> 영상을 앱 서버를 거치지 않고 S3로 직접 올려, 단일 처리 시 영상 적재로 발생하던 JVM 힙 사용량을 **[X]MB → [Y]MB**로 낮췄습니다.
> 변환 작업 동안 일반 API 응답 시간(p99)이 단일 처리 **[A]ms → 분리 [B]ms**, 호스트 CPU 피크가 **[C]% → [D]%**로 줄었습니다.

**가설**: 모놀리식(A)은 영상 바이트가 JVM 힙에 적재되고 ffmpeg 변환이 호스트 CPU를 점유해, 변환 중 무관한 일반 API 응답(p99)이 함께 느려진다. 분리(B)는 영상이 JVM·호스트를 거치지 않아 일반 트래픽이 격리된다.

## 2. 진실성 원칙

- 운영 장애를 겪은 게 아니다. **A(모놀리식)는 검증용으로 새로 구현한 비교군**이다.
- 서사 = "설계 판단을 직접 구현·측정해 검증했다" (≠ "서버가 죽어서 고쳤다").
- 측정하지 않은 수치는 쓰지 않는다. resume 빈칸은 본 실험 실측값으로만 채운다.

## 3. 비교군

| | 구성 | 설명 |
|---|---|---|
| **A (모놀리식)** | EC2 단일 서버 인라인 처리 | 영상을 백엔드가 multipart 수신 → `ProcessBuilder` ffmpeg 변환 + 분석 직접 호출. 영상 바이트가 JVM·호스트 경유 |
| **B (분리, 현 구조)** | 변환·분석을 서버 밖 워커로 분리 | 브라우저 → presigned URL 로 S3 직접 업로드(`AwsS3Service`), EventBridge → Lambda 변환·분석. EC2 는 조율만 |

## 4. 고정 변수 (A/B 동일)

| 변수 | 값 |
|------|----|
| EC2 인스턴스 | t4g.medium (ARM64 Graviton2, 2 vCPU, 4GB) — A/B 동일 |
| JVM heap | `-Xmx768m -Xms768m` (`config.env.example:25`) |
| 영상 샘플 | **S3 원본 버킷 중 최대 크기 영상 1개** (A/B 동일본). 선정: `aws s3 ls s3://<원본버킷> --recursive --human-readable \| sort -k3 -h \| tail -1` |
| 일반 조회 API | `GET /api/v1/interviews/{id}` (변환과 무관한 가벼운 조회, JWT 필요) |
| 동시 변환 건수 N | **N=1 우선**(변환 1건만으로도 일반 API p99 X→Y = 현실적 단일 시나리오). N=3 은 "동시 다발 악화" 보조 (2vCPU 에선 N=3 시 A 가 CPU 200% 포화로 자명하게 이겨 비교 설득력 약화) |
| 일반 GET 부하 | 예: 20rps (baseline 대비 측정 가능 수준 고정) |
| 계측 | A/B 동일 Actuator/node_exporter (측정 오버헤드 상쇄) |
| 반복 | 최소 3회, 중앙값 |

## 5. 측정 지표 → resume 수치 매핑 (핵심)

| resume 수치 | 메트릭 | 측정 방법 | 결과 매핑 |
|------------|--------|----------|----------|
| JVM 힙 [X]→[Y]MB | `jvm_memory_used_bytes{area="heap"}` (Micrometer) | 변환 1건 처리 중 heap peak. A(영상 JVM 경유) vs B(미경유) | A=[X] / B=[Y] |
| 일반 API p99 [A]→[B]ms | `http_server_requests_seconds`(uri=`/api/v1/interviews/{id}`) p99 | 변환 N건 진행 중 같은 GET 부하 p99. A(CPU/스레드 경합) vs B | A=[A] / B=[B] |
| 호스트 CPU 피크 [C]→[D]% | `node_cpu_seconds_total` → `100*(1-idle rate)` (node_exporter) | 변환 중 호스트 CPU peak. A(ffmpeg 인라인) vs B | A=[C] / B=[D] |
| (보조) 에러율/타임아웃 | k6 `http_req_failed` + custom `error_rate` | 동시 N건 종료 시 일반 API 실패율 | A vs B |

Grafana 패널은 `../../observability/grafana/dashboards/loadtest-overview.json` 에 호스트 CPU / JVM Heap / HTTP p99 모두 존재 → 추가 패널 불필요. 측정 구간 캡처가 곧 포폴 그래프.

## 6. 필요 구현 (구현명세) — ⚠️ 비용 가장 큼

현재 코드 = **B(분리)만 존재**. 백엔드에 ffmpeg/인라인 분석 전무 (`grep ffmpeg\|ProcessBuilder backend/src/main` = 0건). A 비교군 신규 구현 필요.

| # | 구현 | 위치 | 담당 |
|---|------|------|------|
| 1 | multipart 영상 업로드 엔드포인트 (`POST /api/v1/interviews/{id}/video`, `@RequestPart MultipartFile`) | 신규 `controller/VideoUploadController.java` | `backend` |
| 2 | 인라인 변환·분석 서비스 (`ProcessBuilder` ffmpeg + 분석 직접 호출) | 신규 `service/InlineVideoProcessingService.java` | `backend` |
| 3 | A/B 토글 플래그 `USE_INLINE_VIDEO` (`@ConditionalOnProperty`) | `application-loadtest.yml` | `backend` |
| 4 | EC2#1 에 ffmpeg 바이너리 설치 | `infra/deploy-app.sh` 또는 `user-data.sh` (`dnf install -y ffmpeg`) | `backend`/`sre-engineer` |
| 5 | 분석 호출은 기존 WireMock 재사용 (변수 통제: 실제 Gemini/Vision 대신 고정 지연 mock) | `wiremock-stubs/` | `backend` |

**⚠️ 재검토 정정 — 실제 규모 = 미니프로젝트 (실험1/2 "토글 추가" 수준 아님)**:
- **`AudioTurnAnalysisService` 재사용 불가**: 이 서비스는 **음성(audio) 답변 전사+분석 전용 포트**(`AudioTurnAnalyzer`, 입력 `MultipartFile audio`). 실험0 이 필요한 **영상 변환(ffmpeg) + 비언어(Gemini/Vision) 분석** 경로는 백엔드에 0줄(전부 `lambda/analysis`). → 신규 작성 = (a) `ProcessBuilder` ffmpeg 변환 래퍼, (b) 변환 산출물→분석 입력 어댑터, (c) 고정지연 mock 분석 호출, (d) `MultipartFile.getBytes()` 힙 적재 인라인 서비스.
- **측정 목적 = 힙·CPU 점유 증명** → 분석은 실제 LLM 불필요. 고정지연 mock + 더미 바이트 처리로 충분.
- **[P0] multipart 10MB 한도**: 전 프로필 `spring.servlet.multipart.max-file-size` 10MB(loadtest yml 은 미설정→Spring 기본 파일 1MB). 영상 수십~수백MB → 즉시 `MaxUploadSizeExceededException`. → loadtest yml 한도 상향(예: 500MB) **또는** multipart 우회하고 "EC2 로컬 파일 직접 처리" 경로만 측정(권장, 한도 무관). 단 '힙 적재' 측정 의도면 `getBytes()` 명시적 힙 로딩 코드 필요.
- **[보안 A03] `ProcessBuilder`**: ffmpeg 호출은 `infra/video/adapter` 포트 구현 배치(app service 직접 호출 회피). 입출력 경로 = **서버 생성 임시파일만**(사용자 입력 경로 ProcessBuilder 전달 금지). 검증용이라도 보안 룰 예외 없음.
- ffmpeg: 시스템 바이너리 + `ProcessBuilder`(Lambda 변환과 동일 도구). EC2#1 에 `dnf install ffmpeg` 선설치.
- 영상: S3 최대 원본 1개(A/B 동일본). A = 로컬 파일 직접 처리(권장) 또는 multipart 한도 상향 후 업로드. B = 정상 presigned→S3→Lambda.

## 7. 부하 시나리오

- **baseline**: 변환 없이 일반 GET API 에 일정 RPS(예: 20rps, 2분) → 평소 p99 기준선.
- **under-load**: 영상 변환 N건(기본 3건) 동시 트리거 + 동시에 같은 GET 부하 → "변환 중" 구간 p99·CPU·heap·에러율.
- A/B 각각 동일 시나리오 3회 반복, 중앙값.
- **k6 스크립트**: 신규 필요 (`video-load-test.js` — GET 부하 + 변환 트리거). 기존 follow-up 스크립트는 부적합. JWT 헤더 주입 필수(§9).

## 8. 산출물 포맷 (resume 빈칸 채움)

```
영상 샘플: <파일명/크기>, 동시 변환 N=<>건, 인스턴스 t4g.medium, 반복 3회 중앙값
변환 작업 중 일반 API 응답:
  A(모놀리식):  p99 ___ ms,  에러율 ___ %
  B(분리):      p99 ___ ms,  에러율 ___ %
호스트 CPU 피크:  A ___ % / B ___ %
JVM 힙 피크:      A ___ MB / B ___ MB
```

## 9. 실행 절차

1. `source backend/src/test/k6/infra/config.env` → `bash infra/start-ec2.sh` (EC2 기동)
2. A 구현 배포 + ffmpeg 설치 → `USE_INLINE_VIDEO=true`(A) / `false`(B) 로 각 회차 배포 (`infra/deploy-app.sh`)
3. **[P0] seed 적재 (user_id 포함)**: `mysql rehearse_loadtest < seed-data.sql`. interview `user_id` 가 NULL 이면 GET `/api/v1/interviews/{id}` 도 `validateOwner` 전건 차단(`Interview.java:173-178`) → seed 에 user 행 + interview.user_id 필수
4. **JWT 주입**: `jwt.secret`(HS256) 토큰 발급(**subject=seed user_id**, role) → k6 `setup()` `Authorization: Bearer` 헤더. JWT 필터는 토큰 파싱만이나 소유자 검증이 interview.user_id 대조 → seed user_id 일치 필수
5. S3 최대 영상 선정 → A/B 동일본 사용 (A 는 로컬 직접 처리 권장)
6. baseline → under-load k6 실행, `collect-metrics.sh` 로 Actuator/Prometheus 스냅샷
7. EC2#2 Grafana(`:3000`) 에서 측정 구간 캡처

## 10. Verification

- 워밍업 1회 버림 + 본 3회 측정 + 정상상태 윈도우 추출 → 중앙값 §8 표 채움.
- JVM heap: A > B 유의차 (영상 바이트 적재 증명). 호스트 CPU: A peak ≫ B (ffmpeg 점유 증명).
- 변환 중 일반 API p99: A > B (경합 증명).
- 측정값이 "분리 설계가 일반 트래픽을 격리한다"는 가설과 정합.
