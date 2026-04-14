# Plan 14: Observability + 운영 Runbook

> 상태: Draft
> 작성일: 2026-04-12

## Why

prod 런칭 후 "동작한다" 이상의 운영 역량이 필요하다. 현재 dev 환경은 모니터링·알람·백업·인시던트 대응 절차가 수립되어 있지 않아 prod 그대로 적용 시 다음 리스크가 발생한다:

- 장애 감지 지연 (사용자가 제보 전까지 파악 불가)
- Lambda 실패/쿼터 초과 누락
- EC2 디스크 풀·OOM 미탐지
- DB 백업 부재 → 데이터 손실 시 복구 불가
- 장애 대응 절차 미숙지 → MTTR 증가

MVP 단계에서는 과도한 도구 도입을 피하고 **CloudWatch 기본 알람 + mysqldump 자동 백업 + 일일 점검 체크리스트** 수준으로 시작한다. 별건으로 Grafana/Datadog 이관 가능.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| CloudWatch 알람 (SNS 연동) | 신규 생성 |
| SNS Topic `rehearse-prod-alerts` | 신규 생성 |
| EC2 cron: `mysqldump` S3 업로드 | 신규 |
| `docs/guides/prod-runbook.md` | 신규 작성 (장애 대응 절차) |
| `docs/guides/prod-db-backup-restore.md` | 신규 작성 (plan-12 연동) |
| `docs/guides/rollback-runbook.md` | 신규 작성 (plan-05 연동) |

## 상세

### 1. SNS Topic `rehearse-prod-alerts`

| 설정 | 값 |
|---|---|
| Name | `rehearse-prod-alerts` |
| Region | `ap-northeast-2` |
| Subscription | email (관리자) + Slack webhook (Lambda bridge 또는 AWS Chatbot) |

Slack 연동은 AWS Chatbot으로 가장 간단. 별도 Lambda 불필요.

### 2. CloudWatch 알람 (초기 세트)

| 알람명 | 메트릭 | 조건 | Action |
|---|---|---|---|
| `rehearse-prod-lambda-analysis-errors` | `AWS/Lambda` `Errors` (`FunctionName=rehearse-analysis-prod`) | 5분 3회 이상 | SNS |
| `rehearse-prod-lambda-convert-errors` | 동일, `rehearse-convert-prod` | 5분 3회 이상 | SNS |
| `rehearse-prod-lambda-analysis-duration` | `Duration` p95 | 5분 평균 > 800000ms (13분, timeout 근접) | SNS |
| `rehearse-prod-ec2-cpu` | `AWS/EC2` `CPUUtilization` | 5분 평균 > 80% 연속 3회 | SNS |
| `rehearse-prod-ec2-status-check` | `StatusCheckFailed` | 1회 이상 | SNS |
| `rehearse-prod-ec2-disk` | `AWS/CWAgent` `disk_used_percent` | > 80% | SNS (CWAgent 필요) |
| `rehearse-prod-ec2-mem` | `AWS/CWAgent` `mem_used_percent` | > 85% | SNS (CWAgent 필요) |
| `rehearse-prod-alb-5xx-proxy` *(대안: 외부 uptime)* | (LB 없음) | — | Uptime Robot / BetterStack 등 외부 모니터링으로 대체 |
| `rehearse-prod-eventbridge-dlq` | SQS `ApproximateNumberOfMessagesVisible` (`rehearse-event-dlq-prod`) | > 0 | SNS |

**CloudWatch Agent 설치** (EC2 디스크/메모리 메트릭 수집):
```bash
# prod EC2에서
sudo apt install amazon-cloudwatch-agent
sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/arm64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb

# config.json 작성 (mem, disk, swap 메트릭)
sudo vi /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```
EC2 IAM Role에 `CloudWatchAgentServerPolicy` 이미 부여 (plan-02).

**외부 Uptime Monitoring**: 무료 티어 Uptime Robot 또는 BetterStack.
- `https://rehearse.co.kr` — 1분 간격
- `https://api.rehearse.co.kr/actuator/health` — 1분 간격
- 실패 시 Slack + email 알림

### 3. DB 자동 백업 + Nginx 주간 Reload

**cron** (`ubuntu` 사용자, `crontab -e`):
```cron
# 매일 03:30 KST (UTC 18:30) — DB 백업
30 18 * * * /home/ubuntu/rehearse/backend/scripts/db-backup.sh >> /var/log/rehearse/db-backup.log 2>&1

# 매주 월요일 04:00 KST (UTC 일요일 19:00) — nginx reload로 갱신된 인증서 반영
# (certbot 컨테이너가 renew한 인증서는 nginx 재시작 없이 인식되지 않음)
0 19 * * 0 cd /home/ubuntu/rehearse/backend && docker compose exec -T nginx nginx -s reload >> /var/log/rehearse/nginx-reload.log 2>&1
```

**Nginx reload 주기 선택 이유**:
- Let's Encrypt 인증서 유효기간 90일, certbot은 30일 이내 진입 시 자동 renew → 최악 60일 내 renew 발생
- 주간 reload로 최대 7일 지연 수용 → 만료 여유(30일) 충분
- 일간 reload는 과도(불필요한 연결 드롭), 월간은 위험

**로그 디렉토리 준비**:
```bash
sudo mkdir -p /var/log/rehearse
sudo chown ubuntu:ubuntu /var/log/rehearse
```

`~/rehearse/backend/scripts/db-backup.sh`:
```bash
#!/bin/bash
set -euo pipefail

TIMESTAMP=$(date -u +%Y%m%d-%H%M%S)
BACKUP_FILE="/tmp/rehearse-prod-${TIMESTAMP}.sql.gz"
S3_BUCKET="rehearse-videos-prod"
S3_PREFIX="db-backups"
CNF_FILE=$(mktemp)
trap 'rm -f "${CNF_FILE}" "${BACKUP_FILE}"' EXIT

cd /home/ubuntu/rehearse/backend
set -a; source .env; set +a

# credentials를 임시 defaults-extra-file로 기록 (ps aux 노출 방지)
umask 077
cat > "${CNF_FILE}" <<EOF
[client]
user=root
password=${DB_ROOT_PASSWORD}
EOF

# 컨테이너 안에 임시 파일 복사 후 mysqldump 실행
docker cp "${CNF_FILE}" rehearse-db:/tmp/.my.cnf
docker exec rehearse-db sh -c "chmod 600 /tmp/.my.cnf && \
  mysqldump --defaults-extra-file=/tmp/.my.cnf \
    --single-transaction --routines --triggers rehearse" \
  | gzip > "${BACKUP_FILE}"
docker exec rehearse-db rm -f /tmp/.my.cnf

aws s3 cp "${BACKUP_FILE}" "s3://${S3_BUCKET}/${S3_PREFIX}/rehearse-prod-${TIMESTAMP}.sql.gz" \
  --storage-class STANDARD_IA
```

**보안 개선 요지**:
- `-p"${PASS}"` 인자 사용 안 함 → `ps aux` / CloudWatch container insights에서 비밀번호 노출 없음
- 임시 cnf 파일은 `umask 077` + `trap`으로 확실히 정리
- 컨테이너 내부에도 `chmod 600` 적용 후 실행 직후 삭제

**S3 Lifecycle** (`rehearse-videos-prod/db-backups/` prefix):
- 7일 지난 파일 → Glacier Instant Retrieval
- 90일 지난 파일 → Glacier Deep Archive
- 365일 지난 파일 → Expire

**복구 절차** (`docs/guides/prod-db-backup-restore.md`):
```bash
aws s3 cp s3://rehearse-videos-prod/db-backups/<file>.sql.gz /tmp/
gunzip /tmp/<file>.sql.gz
docker exec -i rehearse-db mysql -u root -p"${DB_ROOT_PASSWORD}" rehearse < /tmp/<file>.sql
```

### 4. 일일 점검 체크리스트 (`docs/guides/prod-runbook.md`)

**매일 오전 10시** (수동 또는 cron + 리포트):
- [ ] Uptime Robot 지난 24h 99.9% 이상
- [ ] Prod EC2 `/actuator/health` 200
- [ ] Lambda 에러 카운트 < 10 (24h)
- [ ] CloudWatch 알람 상태 OK
- [ ] DB 어제자 백업 파일 S3 존재 확인
- [ ] S3 `rehearse-videos-prod` 사이즈 증가 추이 (이상 증가 탐지)
- [ ] EC2 디스크 사용률 < 70%
- [ ] 주요 OAuth provider 상태 확인 (Google/GitHub status page)

### 5. 인시던트 템플릿

`docs/guides/prod-runbook.md`에 포함:

```markdown
## Incident #N — YYYY-MM-DD HH:MM

**Severity**: P0 | P1 | P2 | P3
**Status**: Detecting | Mitigating | Monitoring | Resolved
**Summary**: (한 줄)

### Timeline
- HH:MM — 최초 감지 (source)
- HH:MM — 원인 파악
- HH:MM — 완화 조치
- HH:MM — 해소

### Impact
- 영향 사용자 수
- 영향 기능
- 기간

### Root Cause
...

### Action Items
- [ ] 단기 수정
- [ ] 장기 개선 (플랜 생성)

### Postmortem
```

### 6. 주요 장애 시나리오별 1차 대응

**Backend 5xx 급증**:
1. `/actuator/health` 확인
2. `docker compose logs backend --tail 200`
3. 최근 배포 확인 → `git log --oneline main -5`
4. 롤백 필요 시 plan-05 rollback 절차

**Lambda 분석 실패 급증**:
1. CloudWatch Logs `/aws/lambda/rehearse-analysis-prod` 확인
2. 쿼터 초과 의심 시 OpenAI 대시보드
3. `lambda/lambda-safe-deploy.sh --env prod --rollback analysis`

**EC2 디스크 가득**:
1. `df -h` → `/var/lib/docker` 대용량 확인
2. `docker system prune -af --volumes` **신중히** (MySQL 볼륨 제외 확인)
3. 오래된 이미지 정리

**DB 연결 불가**:
1. `docker compose ps db` → healthy?
2. `docker compose logs db --tail 100`
3. 최악의 경우 백업에서 복구

### 7. 비용 모니터링

AWS Budgets 알람:
- 월 $75 도달 시 SNS 알림 (상한 $50 + 50% 버퍼)
- 서비스별 breakdown 주간 리포트

## 담당 에이전트

- Implement: `devops-engineer` — CloudWatch / SNS / 알람 / cron / runbook 작성
- Review: `architect-reviewer` — 알람 임계값 적절성, 백업 정책

## 검증

- SNS topic + subscription 확인 → 테스트 알림 수신
- 각 CloudWatch 알람 `OK` 상태 진입
- CloudWatch Agent 설치 후 `mem_used_percent` 메트릭 수집 확인
- DB 백업 cron 1회 수동 실행 → S3 객체 존재 확인
- 복구 절차 dry-run: 백업 파일을 로컬 Docker에서 restore 성공
- `docs/guides/prod-runbook.md`, `prod-db-backup-restore.md`, `rollback-runbook.md` 3개 문서 존재
- `progress.md` Task 14 → Completed
