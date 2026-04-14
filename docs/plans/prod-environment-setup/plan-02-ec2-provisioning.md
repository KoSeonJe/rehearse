# Plan 02: prod EC2 인스턴스 프로비저닝

> 상태: Draft
> 작성일: 2026-04-12

## Why

dev와 물리적으로 분리된 prod 런타임이 필요하다. 동일 EC2에 두 환경을 띄우면 포트·볼륨·certbot·IAM 자격이 얽혀 장애 전파 위험이 크다. 사용자 결정사항(EC2 Docker MySQL 유지, t4g.small ARM64)에 따라 dev와 스펙이 동일하지만 완전히 독립된 인스턴스를 신규 생성한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| EC2 인스턴스 | 신규 생성 (AWS 콘솔) |
| IAM Role `rehearse-prod-ec2-role` | 신규 생성 |
| IAM Policy `rehearse-prod-ec2-policy` | 신규 생성 |
| SG `rehearse-prod-sg` | 신규 생성 |
| Key Pair `rehearse-prod-key.pem` | 신규 생성, `~/.ssh/`에 저장 |
| Elastic IP | 신규 할당 + 인스턴스 연결 |
| `docs/architecture/infrastructure-status.md` | prod EC2 항목 추가 |
| `~/rehearse/backend/.env.prod` (EC2 내) | 수동 업로드 |
| `~/rehearse/backend/gcp-credentials.json` (EC2 내) | 수동 업로드 |

## 상세

### EC2 사양

| 항목 | 값 |
|---|---|
| 이름 | `rehearse-prod-api` |
| AMI | Ubuntu 22.04 LTS (Canonical, ARM64) — ap-northeast-2 최신 |
| 인스턴스 유형 | `t4g.small` (2 vCPU / 2 GiB / ARM64 Graviton2) |
| 루트 볼륨 | gp3 30 GiB, IOPS 3000, 암호화 ON |
| 키 페어 | `rehearse-prod-key` (신규, `.pem` 발급 즉시 `~/.ssh/rehearse-prod-key.pem` 저장, `chmod 400`). **백업 필수**: 1Password `Rehearse Prod` vault에 업로드, 팀 공유 볼트에도 동일 저장. Key 분실 시 EC2 SSH 복구 불가 → AMI 재생성 필요(다운타임 큼) |
| VPC / Subnet | 기본 VPC, public subnet (ap-northeast-2a) |
| Public IP | Auto-assign OFF (Elastic IP로 고정) |
| 종료 보호 | ON (`DisableApiTermination: true`) |
| 상세 모니터링 | OFF (비용 절감, CloudWatch 기본 메트릭만) |

### Elastic IP

- 신규 할당, 인스턴스에 연결
- 할당 후 IP 값 기록 → plan-08 가비아 DNS A 레코드 대상 (apex `rehearse.co.kr` + `api.rehearse.co.kr` 양쪽)

### Security Group `rehearse-prod-sg`

| 방향 | 포트 | 소스 | 용도 |
|---|---|---|---|
| Inbound | 22/tcp | 관리자 IP CIDR (개인 IP/32) | SSH 관리 |
| Inbound | 80/tcp | 0.0.0.0/0 | HTTP (certbot 발급 + redirect) |
| Inbound | 443/tcp | 0.0.0.0/0 | HTTPS |
| Inbound | 8080/tcp | **차단** | 내부 docker expose, 외부 노출 금지 |
| Outbound | ALL | 0.0.0.0/0 | Lambda/S3/OAuth provider/ECR 호출 |

dev와 차이: dev는 8080이 Lambda 직접 접근용으로 열려 있었으나(`API_SERVER_URL=http://54.180.188.135:80`), prod는 Lambda도 `https://api.rehearse.co.kr` 경유하므로 8080 노출 불필요.

### IAM Role `rehearse-prod-ec2-role`

`AssumeRolePolicyDocument`: EC2 service principal
**연결 정책** (관리형 + 커스텀):
- `AmazonEC2ContainerRegistryReadOnly` (관리형) — ECR `docker pull`
- `CloudWatchAgentServerPolicy` (관리형) — 로그/메트릭 (plan-14에서 사용)
- 커스텀 `rehearse-prod-s3-rw`:
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
        "Resource": [
          "arn:aws:s3:::rehearse-videos-prod",
          "arn:aws:s3:::rehearse-videos-prod/*"
        ]
      }
    ]
  }
  ```

**주의**: dev Role과 권한 경계 분리. prod Role이 `rehearse-videos-dev` 접근 금지, 역도 금지.

### 초기 설정 스크립트 (SSH 접속 후 수동 실행)

```bash
# 1. Docker 설치
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker

# 2. Docker Compose plugin 확인
docker compose version

# 3. AWS CLI v2 (ARM64)
curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install && rm -rf aws awscliv2.zip
aws sts get-caller-identity  # IAM Role 인증 확인

# 4. 작업 디렉토리
mkdir -p ~/rehearse/backend/nginx
cd ~/rehearse/backend

# 5. ECR 로그인 (Role 기반)
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin 776735194358.dkr.ecr.ap-northeast-2.amazonaws.com

# 6. 타임존
sudo timedatectl set-timezone Asia/Seoul
```

### 수동 업로드 파일 (로컬 → EC2 scp)

```bash
# .env.prod (prod 전용 시크릿 포함)
scp -i ~/.ssh/rehearse-prod-key.pem .env.prod ubuntu@<EIP>:~/rehearse/backend/.env

# GCP credentials (TTS)
scp -i ~/.ssh/rehearse-prod-key.pem gcp-credentials-prod.json ubuntu@<EIP>:~/rehearse/backend/gcp-credentials.json

# docker-compose.prod.yml + nginx.prod.conf (plan-06 산출물 — 초기 1회는 수동, 이후 GitHub Actions)
scp -i ~/.ssh/rehearse-prod-key.pem backend/docker-compose.prod.yml ubuntu@<EIP>:~/rehearse/backend/docker-compose.yml
scp -i ~/.ssh/rehearse-prod-key.pem backend/nginx/nginx.prod.conf ubuntu@<EIP>:~/rehearse/backend/nginx/nginx.conf
```

**주의**: `docker-compose.prod.yml`을 EC2에서는 `docker-compose.yml`로 리네임해 compose 기본 파일 규약 준수.

### 비용 추정 (월)

- t4g.small on-demand: ~$15
- gp3 30GB: ~$3
- Elastic IP (인스턴스 연결 상태): $0 (미연결 시 $4)
- 데이터 전송(아웃바운드): 사용량 기반
- **소계**: ~$18/월

## 담당 에이전트

- Implement: `devops-engineer` — AWS 콘솔/CLI 프로비저닝
- Review: `architect-reviewer` — IAM 권한 경계, SG 규칙

## 검증

- `ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@<EIP>` 접속 성공
- `docker --version` 및 `docker compose version` 출력 확인
- `aws sts get-caller-identity` → prod Role ARN 반환
- `aws s3 ls s3://rehearse-videos-prod --profile ec2-role` 성공, `rehearse-videos-dev` 접근 시 AccessDenied
- `docker login` ECR 성공
- SG inbound 22/80/443만 노출 확인, 8080 폐쇄 확인
- `progress.md` Task 2 → Completed
