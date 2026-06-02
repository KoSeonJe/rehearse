# Handoff — 554-loadtest-environment

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 (2026-06-02)
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: 환경 구축 완료 (smoke A1~A7 PASS). A8~A10 OOM 차단으로 미완.
- 브랜치: `refactor/be-session-feedback-comment-input` (무관 브랜치 위에 작업됨 — 별도 loadtest 브랜치 분리 필요)
- 관련 PR: 없음 (전 자산 uncommitted)
- 빌드: 앱 배포 정상 / A8~A10 미진행
- 테스트: smoke A1~A7 PASS / A8~A10 OOM 차단

## 다음 세션 시작점

1. **EC2#1 인스턴스 타입 변경 결정** (사용자 확인 필요 — 미결 블로커 참고)
2. 타입 변경 후 재시작 → A8~A10 재검증 → baseline.md 실측값 채움
3. 별도 loadtest 브랜치 생성 + 환경 자산 전체 커밋 + PR

- 다음 작업: EC2#1 t4g.medium 업사이징 후 재배포 → smoke A8~A10
- 참조: `docs/plans/554-loadtest-environment/tech-spec.md`, `docs/performance-tests/554-loadtest-environment/aws-resources.md`
- 첫 명령 (인스턴스 재개):
  ```bash
  source backend/src/test/k6/infra/config.env
  aws ec2 start-instances --region ap-northeast-2 --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf
  # start 후 public IP 변동 → config.env LOADTEST_SSH_HOST / OBS_SSH_HOST 갱신
  # Prometheus 는 EC2#1 private IP(172.31.14.143) scrape → 불변
  ```

## 미해결 질문 / Blocker

### [Blocker] EC2#1 OOM — 인스턴스 타입 업사이징

EC2#1 t4g.small(2GB)에서 app(-Xms768m, 809MB) + MySQL8(606MB) + WireMock(106MB) = ~1721MB.
idle 여유 19MB → MySQL 쿼리 시 호스트 행. A8~A10(메모리 실측/k6 smoke) 차단.

- 옵션 A (추천): t4g.medium(4GB)으로 업사이징. vCPU 동일 2개 유지 → thread/connection 실험변수 불변, OOM만 제거. 절차: stop → `aws ec2 modify-instance-attribute --instance-type t4g.medium` → start → baseline.md EC2 행 + tech-spec 갱신.
- 옵션 B: t4g.small 유지 + swap 추가 + MySQL 메모리 튜닝. 부하 중 swap = latency 왜곡 위험으로 실험 신뢰도 저하 우려.

### [별도 작업 — 환경 차단 아님] seed-data.sql interview_type 폐기 enum

`seed-data.sql`의 `interview_type='CS'`가 폐기된 enum(현행 `CS_FUNDAMENTAL`). 30초 스케줄러 ERROR 누적 중. backend 위임 필요 (별도 브랜치/PR).

## 컨텍스트 메모

- **결정 — 단일호스트 → 관측 분리**: 원래 tech-spec Trade-off 2는 단일 호스트 유지였으나, 관측 스택(Prometheus+Grafana) OOM 기여로 EC2#2 분리로 변경. EC2#2(`i-0df37cc60c287b2bf`)가 관측 전담. 측정 시스템(앱+MySQL+WireMock+node_exporter)은 EC2#1 단일 유지.
- **결정 — SG 0.0.0.0/0**: ISP IP 로테이션으로 allowlist 유지 불가 → 사용자 결정으로 전체 개방. 프로덕션 SG와 무관한 독립 리소스.
- **Prometheus scrape 고정값**: EC2#1 private IP `172.31.14.143`는 VPC 내 고정 → start/stop 후에도 불변. public IP만 갱신 필요.
- **WireMock 딜레이 기준**: claude 3000ms / whisper 1000ms 확인 완료. `mock-server.py` deprecated, WireMock 표준화됨.
- **브랜치 주의**: 환경 자산 전체가 무관 브랜치(`refactor/be-session-feedback-comment-input`) 위에 uncommitted 상태. 커밋 전 반드시 `git checkout -b feat/554-loadtest-environment` 신규 브랜치 분리.
- **환경**: EC2 둘 다 현재 stopped(과금 EBS만). 작업 시작 시 start 필요.

## 참고 명령

```bash
# 인스턴스 시작
source backend/src/test/k6/infra/config.env
aws ec2 start-instances --region ap-northeast-2 \
  --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf

# start 후 IP 갱신 (public IP 변동)
aws ec2 describe-instances --region ap-northeast-2 \
  --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf \
  --query 'Reservations[].Instances[].[InstanceId,PublicIpAddress,PrivateIpAddress,State.Name]' \
  --output table

# EC2#1 업사이징 (stopped 상태에서)
aws ec2 modify-instance-attribute --region ap-northeast-2 \
  --instance-id i-07ca3e9cda9c81ba0 \
  --instance-type '{"Value":"t4g.medium"}'

# 앱 배포 (config.env 갱신 후)
bash backend/src/test/k6/infra/deploy-app.sh

# 인스턴스 정지
aws ec2 stop-instances --region ap-northeast-2 \
  --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf
```

## 참조 파일

| 파일 | 용도 |
|------|------|
| `docs/plans/554-loadtest-environment/tech-spec.md` | 설계 전반 (Trade-off / 검증 기준) |
| `docs/performance-tests/554-loadtest-environment/aws-resources.md` | EC2/SG/VPC 상세 리소스 대장 |
| `docs/performance-tests/554-loadtest-environment/baseline.md` | 고정값표 + smoke A1~A7 기록 |
| `docs/plans/554-loadtest-environment/known-issues.md` | 알려진 이슈 목록 |
| `backend/src/test/k6/infra/` | 인프라 스크립트 (provision/start/stop/deploy-app 등) |
| `backend/src/test/k6/observability/` | Prometheus + Grafana + node_exporter compose |

---

업데이트: 2026-06-02 (세션 종료)
