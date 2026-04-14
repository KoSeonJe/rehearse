# Phase C — 사용자 수동 작업 체크리스트

> 작성일: 2026-04-14
> 선행: Phase A (코드 변경) 완료, Phase B (AWS CLI 인프라 생성) 완료
> 후행: Phase D (컷오버) 는 본 체크리스트 전 항목 Done 이후 재개

Phase B에서 제가 AWS 리소스를 전부 구축했습니다. 그러나 **외부 서비스(가비아/Google/GitHub/Anthropic/OpenAI/GCP)**는 AWS CLI로 조작 불가능하므로 **사용자가 직접 수행**해야 합니다. 아래 각 항목을 순서대로 완료한 뒤 제게 "Phase C 완료"라고 알려주세요.

---

## 준비: 제 쪽 산출물 위치

| 자원 | 값 / 경로 |
|---|---|
| prod EC2 EIP | `43.201.187.118` |
| prod CloudFront 도메인 | `d22w460y8zm2vk.cloudfront.net` |
| ACM validation CNAME (Name) | `_2d670c7363ee5697964562e9c0b44f22.www.rehearse.co.kr` |
| ACM validation CNAME (Value) | `_d2c9e9f2b3c34e1bde8170dfe09e3708.jkddzztszm.acm-validations.aws` |
| SSH pem | `~/.ssh/rehearse-prod-key.pem` |
| `.env.prod.template` | `/tmp/rehearse-prod/.env.prod.template` |
| 생성된 시크릿 | `.claude.local.md`의 "Prod 리소스 > 시크릿" 섹션 |

---

## C1. 가비아 DNS 레코드 등록 (4건)

가비아 콘솔 → 내 도메인 → `rehearse.co.kr` → DNS 관리 → 레코드 추가.

| 호스트 | 타입 | 값 | TTL |
|---|---|---|---|
| `_2d670c7363ee5697964562e9c0b44f22.www` | CNAME | `_d2c9e9f2b3c34e1bde8170dfe09e3708.jkddzztszm.acm-validations.aws.` | 300 |
| `@` (apex, 빈 값) | A | `43.201.187.118` | 300 |
| `www` | CNAME | `d22w460y8zm2vk.cloudfront.net.` | 300 |
| `api` | A | `43.201.187.118` | 300 |

**주의**:
- 가비아 UI에서 호스트 필드는 도메인 접미사 없이 입력 (`api`만, `api.rehearse.co.kr` 아님)
- apex는 `@` 또는 빈 값
- ACM validation CNAME은 값 끝의 `.` 유지 (가비아 자동 추가하면 중복 없이 1개)
- TTL 300으로 설정 (컷오버 전 전파 속도 확보)
- `dev.rehearse.co.kr`, `api-dev.rehearse.co.kr` 레코드는 **절대 건드리지 말 것** (dev 회귀)

**검증**: 등록 후 5~10분 대기, 아래 명령으로 전파 확인

```bash
dig +short rehearse.co.kr            # 43.201.187.118 반환해야 함
dig +short www.rehearse.co.kr        # d22w460y8zm2vk.cloudfront.net 계열 반환
dig +short api.rehearse.co.kr        # 43.201.187.118 반환
```

---

## C2. Google Cloud Console — prod OAuth Client 발급

1. <https://console.cloud.google.com/apis/credentials> 접속
2. 기존 Rehearse 프로젝트 선택 (신규 프로젝트 생성 비권장, consent screen 심사 이력 유지)
3. `Create Credentials` → `OAuth 2.0 Client ID` → `Web application`
4. Name: **Rehearse Production**
5. **Authorized JavaScript origins**:
   - `https://rehearse.co.kr`
   - `https://www.rehearse.co.kr`
6. **Authorized redirect URIs**:
   - `https://api.rehearse.co.kr/login/oauth2/code/google`
7. `Create` → Client ID / Client Secret 기록

**주의**: dev OAuth Client에 `rehearse.co.kr` 계열 URI가 들어가지 않도록 확인 (dev는 `dev.rehearse.co.kr` 전용으로 유지).

---

## C3. GitHub — prod OAuth App 발급

1. <https://github.com/settings/developers> → `OAuth Apps` → `New OAuth App`
2. Application name: **Rehearse Production**
3. Homepage URL: `https://rehearse.co.kr`
4. Authorization callback URL: `https://api.rehearse.co.kr/login/oauth2/code/github`
5. `Register application` → Client ID 기록 → `Generate a new client secret` → Secret 기록

**주의**: 팀 공유 시 Organization 계정 또는 공용 관리자 계정으로 발급 (퇴사/이직 리스크 회피).

---

## C4. Anthropic / OpenAI — prod API 키 발급

### Anthropic
- <https://console.anthropic.com/> → Settings → API Keys → Create Key
- Name: `Rehearse Production`
- 키 기록 후 저장

### OpenAI
- <https://platform.openai.com/api-keys> → Create new secret key
- Name: `Rehearse Production`
- 키 기록

**주의**: dev 키를 그대로 prod에 쓰면 쿼터/로그가 혼재되므로 **반드시 prod 전용 키 발급**.

---

## C5. Google Cloud TTS — `gcp-credentials-prod.json`

이미 dev에서 사용 중인 GCP 프로젝트의 Service Account를 재사용하되 키 파일만 새로 다운로드하거나, prod 전용 서비스 계정을 새로 발급.

1. Google Cloud Console → IAM & Admin → Service Accounts
2. 기존 `rehearse-tts` 계정 사용 → Keys → Add Key → JSON 다운로드
3. 로컬 파일명: `gcp-credentials-prod.json`

---

## C6. `.env.prod` 최종 작성

1. `/tmp/rehearse-prod/.env.prod.template`을 복사해서 `/tmp/rehearse-prod/.env.prod`로 저장
2. `__FILL_*__` placeholder를 C2·C3·C4에서 발급받은 값으로 교체:
   ```
   GITHUB_CLIENT_ID=<C3 값>
   GITHUB_CLIENT_SECRET=<C3 값>
   GOOGLE_CLIENT_ID=<C2 값>
   GOOGLE_CLIENT_SECRET=<C2 값>
   CLAUDE_API_KEY=<C4 Anthropic>
   OPENAI_API_KEY=<C4 OpenAI>
   ```
3. DB / JWT / INTERNAL 관련 값은 이미 템플릿에 채워져 있음 (1Password 백업 필수)

---

## C7. prod EC2에 파일 업로드 (scp)

EIP `43.201.187.118` 로 scp 업로드.

```bash
# .env 업로드 (파일명 .env 로 리네임)
scp -i ~/.ssh/rehearse-prod-key.pem /tmp/rehearse-prod/.env.prod ubuntu@43.201.187.118:~/rehearse/backend/.env
# GCP 서비스 계정 키 업로드
scp -i ~/.ssh/rehearse-prod-key.pem ~/Downloads/gcp-credentials-prod.json ubuntu@43.201.187.118:~/rehearse/backend/gcp-credentials.json
# docker-compose.prod.yml (rename 후 업로드)
scp -i ~/.ssh/rehearse-prod-key.pem /Users/koseonje/dev/devlens/backend/docker-compose.prod.yml ubuntu@43.201.187.118:~/rehearse/backend/docker-compose.yml
# nginx.prod.conf / nginx.cert-init.conf
scp -i ~/.ssh/rehearse-prod-key.pem /Users/koseonje/dev/devlens/backend/nginx/nginx.prod.conf ubuntu@43.201.187.118:~/rehearse/backend/nginx/
scp -i ~/.ssh/rehearse-prod-key.pem /Users/koseonje/dev/devlens/backend/nginx/nginx.cert-init.conf ubuntu@43.201.187.118:~/rehearse/backend/nginx/
```

**사전 작업**: EC2 접속 후 디렉토리 생성 필요

```bash
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@43.201.187.118
mkdir -p ~/rehearse/backend/nginx
exit
```

또한 첫 접속 시 Docker 등 초기 설치가 안 되어 있으므로 아래 실행:

```bash
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@43.201.187.118 << 'EOF'
set -e
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
sudo timedatectl set-timezone Asia/Seoul
# AWS CLI v2 ARM
curl -s "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip && sudo ./aws/install && rm -rf aws awscliv2.zip
aws --version
aws sts get-caller-identity
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | sudo docker login --username AWS --password-stdin 776735194358.dkr.ecr.ap-northeast-2.amazonaws.com
EOF
```

---

## C8. Lambda prod 환경변수의 OpenAI / Gemini 키 교체

Phase B에서 Lambda를 생성할 때 OpenAI/Gemini 키는 `__FILL_PROD__` placeholder로 넣어두었습니다. C4에서 발급한 prod 키로 교체:

```bash
aws lambda update-function-configuration \
  --function-name rehearse-analysis-prod \
  --environment "Variables={S3_BUCKET=rehearse-videos-prod,USE_GEMINI=true,FFPROBE_PATH=/opt/bin/ffprobe,FFMPEG_PATH=/opt/bin/ffmpeg,GEMINI_MODEL=gemini-2.5-flash,API_SERVER_URL=https://api.rehearse.co.kr,INTERNAL_API_KEY=5b1c623c641b6a5de702ec036abf6d96c2f59851ea0304aa64bc73dc782dc3b3,GEMINI_API_KEY=<YOUR_PROD_GEMINI>,OPENAI_API_KEY=<YOUR_PROD_OPENAI>}"
```

**또는** 이 단계를 제게 말해주시면 `state_write`나 직접 실행으로 교체해드립니다.

---

## C9. GitHub Repository — Environments + Secrets 설정

1. `https://github.com/<owner>/<repo>/settings/environments` 접속
2. `New environment` → name: `production`
3. **Required reviewers**: 본인 (또는 팀원 1명 이상) 추가 — 자동 배포 방지
4. **Environment secrets** 추가:

| Key | Value |
|---|---|
| `AWS_REGION` | `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID` | (IAM user 또는 root 키; 보안상 prod 전용 IAM user 권장이지만 사용자 결정에 따라 root도 가능) |
| `AWS_SECRET_ACCESS_KEY` | 상동 |
| `ECR_REGISTRY` | `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com` |
| `S3_BUCKET_NAME` | `rehearse-frontend-prod` |
| `CLOUDFRONT_DISTRIBUTION_ID` | `E2UWW3KP4S5VOV` |
| `EC2_HOST` | `43.201.187.118` |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_SSH_KEY` | `~/.ssh/rehearse-prod-key.pem` 의 내용 전체 (`-----BEGIN RSA PRIVATE KEY-----` 부터 `-----END RSA PRIVATE KEY-----` 까지) |

---

## Phase C 완료 보고 시 제게 전달해주실 것

`Phase C 완료` 라고 말씀해주시면서 아래 중 **제가 대신 수행할 단계를 위임**하시면 됩니다:

- C8 Lambda env 교체 (키만 알려주시면 CLI로 수행 가능)
- Phase D 재개 신호

Phase D에서 제가 할 일:
1. ACM Issued 확인 → CloudFront distribution에 `www.rehearse.co.kr` alternate domain + ACM 인증서 바인딩
2. EC2 SSH로 certbot 최초 발급 (api + apex 멀티 도메인) — 이 단계는 제가 SSH로 원격 실행 가능
3. Flyway 최초 마이그레이션
4. 프론트 빌드 + S3 sync + CloudFront invalidation
5. 공인 Health check + E2E 스모크

---

## Rollback (비상시)

- 가비아 DNS 레코드를 전부 삭제하면 prod 트래픽이 차단됨 (dev는 계속 동작)
- prod EC2 중지: `aws ec2 stop-instances --instance-ids i-08c7eb8711b295401`
- CloudFront 비활성: `aws cloudfront get-distribution-config` 후 Enabled=false로 update
- Phase D 실패 시 plan-13 Rollback Decision Tree (Case A~E) 참조
