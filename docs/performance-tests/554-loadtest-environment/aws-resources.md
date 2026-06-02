# loadtest 환경 — AWS 리소스 대장

> 554-loadtest-environment 세션(2026-06-02)에서 생성. region `ap-northeast-2` / account `776735194358`.
> **terminate = 사용자만** (destructive). start/stop 으로 비용 통제.

## 생성된 리소스 (이 세션 신규)

### EC2 인스턴스 2대

| 역할 | Instance ID | Type | 용도 | private IP (불변) | SG | 비고 |
|------|-------------|------|------|------------------|-----|------|
| 부하 (EC2#1) | `i-07ca3e9cda9c81ba0` | **t4g.medium** (2026-06-02 small→medium 업사이징) | app + MySQL + WireMock + node_exporter (docker, 앱만 native jar) | `172.31.14.143` | `sg-0b5a7915bdb248607` | OOM 해소 (idle available 1.9GB) |
| 관측 (EC2#2) | `i-0df37cc60c287b2bf` | t4g.small | Prometheus + Grafana (docker) | `172.31.12.190` | `sg-0c988c0e70ccdb142` | EC2#1 private IP scrape |

- 공통: AMI arm64 AL2023, key `rehearse-key`(`~/.ssh/rehearse-key.pem`), subnet `subnet-01702bb29691cb32b`(2a public), VPC `vpc-0902ab0bef9eb644f`, EBS gp3 20GB.
- **public IP = stop/start 시 변동** → 재기동 후 `config.env` LOADTEST_SSH_HOST / OBS_SSH_HOST + Prometheus scrape target 갱신 필요. (Prometheus 는 EC2#1 **private IP** scrape 라 불변.)

### 보안그룹 2개

| SG ID | 이름 | 인바운드 (전부 0.0.0.0/0 — 사용자 결정, ISP IP 로테이션 회피) |
|-------|------|------|
| `sg-0b5a7915bdb248607` | rehearse-loadtest-sg | 22(SSH) / 8080(app) / 9100(node_exporter scrape) |
| `sg-0c988c0e70ccdb142` | rehearse-observability-sg | 22(SSH) / 3000(Grafana) / 9091(Prometheus) |

## 재사용 리소스 (신규 생성 아님 — 삭제 금지)
- keypair `rehearse-key` (dev EC2 공용)
- subnet `subnet-01702bb29691cb32b`, VPC `vpc-0902ab0bef9eb644f` (dev/prod 공용)

## 운영

```bash
source backend/src/test/k6/infra/config.env
# 시작 (실험 시 둘 다)
aws ec2 start-instances --region ap-northeast-2 --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf
# 중지 (사용 후 — EBS 비용만 남김)
aws ec2 stop-instances --region ap-northeast-2 --instance-ids i-07ca3e9cda9c81ba0 i-0df37cc60c287b2bf
```

- 비용: t4g.small ~$0.021/hr×2 = ~$0.042/hr (running). 중지 시 EBS gp3 20GB×2 ≈ ~$3.2/mo 만.

## 해소 — EC2#1 OOM (사이징, 2026-06-02 완료)

**EC2#1 t4g.medium(4GB) 업사이징 완료** — small(2GB)에서 idle available 19MB → mysql 쿼리 시 호스트 행이던 문제 해소. medium idle available 1921MB. vCPU 2개 불변(thread/connection 실험 변수 보존). 절차: `stop → modify-instance-attribute --instance-type t4g.medium → start` 실행 완료. baseline.md §2/§3.2 반영.

## smoke 검증 결과 (2026-06-02)
- A1~A7 PASS (이전 세션, small): 앱 배포 / 관측 분리 scrape(private IP) / Grafana 데이터 / WireMock stub(claude 3s·whisper 1s) 전부 정상.
- **A8~A10 PASS (medium 업사이징 후 재검증)**: A8 idle available 1921MB / A9 mysql 쿼리 후 호스트 안정(SSH+HTTP+Prometheus up) / A10 k6 저부하 exit 0. baseline.md §3.2 기록.

## 관련 known issue
- `seed-data.sql:38` `interview_type='CS'` = 폐기된 enum (현 `CS_FUNDAMENTAL`) → 30s 스케줄러 `No enum constant` ERROR 누적. backend 위임 (별도). 환경 차단 아님.
- dev/prod actuator/prometheus 무인증 노출 → `docs/plans/554-loadtest-environment/known-issues.md` KI-1 (별도 보안 PR 보류).
