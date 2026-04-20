# Rehearse — 배포 운영 가이드

> 최종 업데이트: 2026-03-20

## 개요

- **Frontend**: S3 + CloudFront (React SPA)
- **Backend**: EC2 t3.small + Docker Compose (Spring Boot + MySQL)
- **Analysis**: Lambda (Python 3.12) — Analysis + Convert
- **Storage**: S3 (영상 업로드/변환)
- **Event**: EventBridge (S3 → Lambda 트리거)
- **Video**: MediaConvert (WebM → MP4)
- **CI/CD**: GitHub Actions (develop 브랜치 push 시 자동 배포)
- **이미지 레지스트리**: Amazon ECR

## 아키텍처

```
[GitHub Actions]
    ├── FE: build → S3 sync → CloudFront invalidation
    └── BE: docker build → ECR push → SSH → compose up

[CloudFront] → [S3] React SPA
[EC2 :80]   → Docker: backend (8080) + mysql (3306)

[S3 rehearse-videos-dev]
    └── PutObject (videos/*.webm)
        → EventBridge (rehearse-video-uploaded-dev)
            ├── rehearse-convert-dev (WebM→MP4)
            └── rehearse-analysis-dev (Whisper+GPT-4o→피드백)
```

---

## AWS 리소스 생성 (순서대로)

### 1. EC2 인스턴스

- **리전**: ap-northeast-2 (서울)
- **AMI**: Ubuntu 24.04 LTS
- **타입**: t3.small (2 vCPU, 2GB RAM)
- **스토리지**: 20GB gp3
- **키페어**: `rehearse-key.pem`
- **보안 그룹**: `sg-082751d93d0991dd3`

| 포트 | 프로토콜 | 소스 | 용도 |
|------|----------|------|------|
| 22 | TCP | 0.0.0.0/0 | SSH |
| 80 | TCP | 0.0.0.0/0 | BE API (Docker 80→8080) |
| 8080 | TCP | 0.0.0.0/0 | Lambda → BE 직접 접근 |

### 2. Elastic IP

- EC2 > Elastic IP > 새 주소 할당 → EC2에 연결
- Public IP: `54.180.188.135`

### 3. ECR 리포지토리

- 이름: `rehearse-backend`
- 리전: ap-northeast-2
- URI: `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com/rehearse-backend`

### 4. S3 버킷

| 버킷 | 용도 |
|------|------|
| Frontend용 S3 | 정적 빌드 파일 (CloudFront OAC) |
| `rehearse-videos-dev` | 영상 업로드/변환 (Presigned URL) |

**rehearse-videos-dev 디렉토리 구조:**
```
rehearse-videos-dev/
├── videos/{interviewId}/qs_{questionSetId}.webm   ← 원본 녹화
├── videos/{interviewId}/qs_{questionSetId}.mp4    ← MediaConvert 변환
├── analysis-backup/{id}/qs_{qsId}.json            ← 분석 실패 시 백업
└── layers/ffmpeg-layer.zip                         ← Lambda Layer 소스
```

### 5. CloudFront 배포

- Origin: S3 버킷 (Frontend)
- Origin Access: **OAC** 생성
- Distribution: `d2n8xljv54hfw0.cloudfront.net`
- 기본 루트 객체: `index.html`
- 에러 페이지: 403/404 → `/index.html` (200 응답, SPA 라우팅)

### 6. EventBridge

| 항목 | 값 |
|------|-----|
| Rule | `rehearse-video-uploaded-dev` |
| Pattern | S3 PutObject → `videos/*.webm` |
| Targets | rehearse-convert-dev, rehearse-analysis-dev (병렬) |

### 7. Lambda

#### rehearse-convert-dev (WebM → MP4 변환)

| 항목 | 값 |
|------|-----|
| Runtime | Python 3.12 |
| Memory | 256 MB |
| Timeout | 300초 (5분) |

**환경변수:** `API_SERVER_URL`, `INTERNAL_API_KEY`, `S3_BUCKET`, `MEDIACONVERT_ENDPOINT`, `MEDIACONVERT_ROLE`

#### rehearse-analysis-dev (AI 분석 파이프라인)

| 항목 | 값 |
|------|-----|
| Runtime | Python 3.12 |
| Memory | 2048 MB |
| Timeout | 900초 (15분) |
| Layer | `ffmpeg-static:1` (FFmpeg 7.x, 56MB) |

**환경변수:** `API_SERVER_URL`, `INTERNAL_API_KEY`, `S3_BUCKET`, `OPENAI_API_KEY`, `FFMPEG_PATH=/opt/bin/ffmpeg`, `FFPROBE_PATH=/opt/bin/ffprobe`

### 8. MediaConvert

| 항목 | 값 |
|------|-----|
| Input | WebM (VP9 + Opus/Vorbis) |
| Output | MP4 (H.264 HIGH + AAC 128kbps) |
| MOOV Placement | PROGRESSIVE_DOWNLOAD (faststart) |
| Rate Control | QVBR (Quality Level 7, MaxBitrate 5Mbps) |

### 9. IAM

| 역할 | 정책 |
|------|------|
| `github-actions-deployer` | S3FullAccess, CloudFrontFullAccess, ECRFullAccess |
| `rehearse-lambda-execution` | LambdaBasicExecution, S3 Get/Put/Head, MediaConvert Create/Get/Describe, iam:PassRole |
| `rehearse-mediaconvert-role` | S3 Get/Put (`rehearse-videos-dev/*`) |

---

## EC2 초기 세팅

```bash
ssh -i rehearse-key.pem ubuntu@54.180.188.135

# Docker 설치
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu
exit  # 재접속으로 그룹 적용

ssh -i rehearse-key.pem ubuntu@54.180.188.135

# AWS CLI
sudo apt install -y awscli
aws configure  # Access Key, Secret Key, Region(ap-northeast-2) 입력

# 프로젝트 클론
git clone https://github.com/KoSeonJe/rehearse.git ~/rehearse
cd ~/rehearse/backend

# 환경변수 파일 생성
cp .env.example .env
nano .env  # 값 입력

# ECR 로그인 + 최초 실행
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin {ECR_REGISTRY}
docker compose --env-file .env up -d
```

---

## Docker Compose 구성

| 컨테이너 | 이미지 | 포트 | 네트워크 |
|-----------|--------|------|----------|
| rehearse-backend | ECR/rehearse-backend:latest | 80→8080 | backend_default |
| rehearse-db | mysql:8.0 | 3306→3306 | backend_default |

---

## GitHub Secrets 등록

GitHub repo > Settings > Secrets and variables > Actions

| Secret | 값 | 출처 |
|--------|-----|------|
| `EC2_HOST` | Elastic IP | AWS 콘솔 |
| `EC2_USERNAME` | `ubuntu` | 고정 |
| `EC2_SSH_KEY` | .pem 파일 내용 전체 | 키페어 |
| `AWS_ACCESS_KEY_ID` | IAM Access Key | IAM |
| `AWS_SECRET_ACCESS_KEY` | IAM Secret Key | IAM |
| `AWS_REGION` | `ap-northeast-2` | 고정 |
| `ECR_REGISTRY` | ECR URI (계정번호.dkr.ecr...) | ECR |
| `S3_BUCKET_NAME` | S3 버킷 이름 | S3 |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront 배포 ID | CloudFront |

---

## 배포 흐름

1. `develop` 브랜치에 push/merge
2. GitHub Actions 자동 실행:
   - **변경 감지**: `dorny/paths-filter`로 backend/frontend 변경 여부 판별
   - Backend: 테스트 (Gradle + H2) → Docker 빌드 → ECR push → EC2 SSH → compose pull → up
   - Frontend: 빌드 (Node + Vite) → S3 sync → CloudFront 캐시 무효화
3. 헬스 체크: `curl http://{EC2_HOST}/actuator/health`

> Lambda는 CI/CD에 포함되지 않음. AWS 콘솔에서 직접 배포.

---

## 환경변수 관리

### EC2 (`~/rehearse/backend/.env`)
```
DB_URL, DB_USERNAME, DB_PASSWORD, DB_ROOT_PASSWORD
SPRING_PROFILES_ACTIVE=dev
CLAUDE_API_KEY
OPENAI_API_KEY
INTERNAL_API_KEY
CORS_ALLOWED_ORIGINS
ECR_REGISTRY
AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION
```

### Lambda 환경변수
```
API_SERVER_URL=http://54.180.188.135:80
INTERNAL_API_KEY
S3_BUCKET=rehearse-videos-dev
OPENAI_API_KEY          (analysis만)
MEDIACONVERT_ENDPOINT   (convert만)
MEDIACONVERT_ROLE       (convert만)
FFMPEG_PATH=/opt/bin/ffmpeg     (analysis만)
FFPROBE_PATH=/opt/bin/ffprobe   (analysis만)
```

---

## 운영 명령어

### EC2에서 로그 확인
```bash
docker compose --env-file .env logs -f backend
docker compose --env-file .env logs -f db
```

### 수동 재시작
```bash
docker compose --env-file .env restart backend
```

### 수동 배포 (ECR에서 최신 이미지)
```bash
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin {ECR_REGISTRY}
docker compose --env-file .env pull backend
docker compose --env-file .env up -d
```

### DB 접속
```bash
docker compose --env-file .env exec db mysql -u rehearse -p rehearse
```

### Lambda 로그 확인
```bash
aws logs tail /aws/lambda/rehearse-analysis-dev --follow
aws logs tail /aws/lambda/rehearse-convert-dev --follow
```

---

## 월 예상 비용

| 리소스 | 비용 |
|--------|------|
| EC2 t3.small | ~$15 |
| Elastic IP | $0 (연결+실행 시) |
| ECR | ~$0.05 |
| S3 (영상 + Frontend) | ~$1.25 |
| CloudFront | ~$1 |
| Lambda (100회 분석) | ~$1 |
| MediaConvert (100건) | ~$2 |
| **합계** | **~$20/월** |

---

## 트러블슈팅

### Backend 시작 실패
```bash
docker compose --env-file .env logs backend | tail -50
```

### MySQL 연결 실패
- `docker compose --env-file .env ps` 로 db 상태 확인
- 헬스체크 통과 전 backend가 시작되면 자동 재시작됨

### Flyway 마이그레이션 실패
- 로그에서 `FlywayException` 확인
- `flyway_schema_history` 테이블 확인 후 수동 조치

### CORS 에러
- `.env`의 `CORS_ALLOWED_ORIGINS`가 CloudFront 도메인과 일치하는지 확인
- `https://` 프로토콜 포함 필수

### Lambda 분석 실패
- CloudWatch 로그 확인: `/aws/lambda/rehearse-analysis-dev`
- 분석 재시도: `POST /api/v1/interviews/{id}/question-sets/{qsId}/retry-analysis`
- FFmpeg 경로 확인: 환경변수 `FFMPEG_PATH=/opt/bin/ffmpeg`

### MediaConvert 변환 실패
- CloudWatch 로그 확인: `/aws/lambda/rehearse-convert-dev`
- MediaConvert 콘솔에서 Job 상태 확인
- IAM `rehearse-mediaconvert-role`의 S3 권한 확인

---

## Dev 인스턴스 자동 Start/Stop 스케줄러

비용 절감을 위해 dev EC2(`i-0c7d5af781b430b85`)는 EventBridge Scheduler로 자동 start/stop 됩니다.

- **Stop**: 매일 KST 02:00 (`rehearse-dev-stop`)
- **Start**: 매일 KST 10:00 (`rehearse-dev-start`)
- **리소스 정의**: `infra/dev-instance-scheduler.yaml` (CloudFormation)
- **Stack**: `rehearse-dev-scheduler` (region `ap-northeast-2`)

### 배포 / 재배포
```bash
aws cloudformation deploy \
  --template-file infra/dev-instance-scheduler.yaml \
  --stack-name rehearse-dev-scheduler \
  --capabilities CAPABILITY_NAMED_IAM \
  --region ap-northeast-2
```

### 스케줄 해제 (전체 롤백)
```bash
aws cloudformation delete-stack --stack-name rehearse-dev-scheduler --region ap-northeast-2
```

### 긴급 수동 start (stop 상태에서)
```bash
aws ec2 start-instances --instance-ids i-0c7d5af781b430b85 --region ap-northeast-2
```

### 일시적으로 스케줄 비활성화
콘솔 또는 CLI로 개별 스케줄 disable:
```bash
aws scheduler update-schedule --name rehearse-dev-stop \
  --state DISABLED --flexible-time-window Mode=OFF \
  --schedule-expression "cron(0 2 * * ? *)" \
  --schedule-expression-timezone Asia/Seoul \
  --target '{"Arn":"arn:aws:scheduler:::aws-sdk:ec2:stopInstances","RoleArn":"arn:aws:iam::776735194358:role/rehearse-dev-scheduler-role","Input":"{\"InstanceIds\":[\"i-0c7d5af781b430b85\"]}"}' \
  --region ap-northeast-2
```

### 주의
- **GitHub Actions dev 배포는 10:00–02:00 구간에만 실행**해야 함. 그 외 시간대엔 인스턴스가 stopped라 SSH 실패
- EIP는 유지되므로 start 후 IP 변경 없음
- prod 인스턴스(`i-08c7eb8711b295401`)는 스케줄러 대상이 **아님** (24/7 운영)
