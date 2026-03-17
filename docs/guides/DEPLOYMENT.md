# Rehearse — 배포 운영 가이드

> 최종 업데이트: 2026-03-16

## 개요

- **Frontend**: S3 + CloudFront (React SPA)
- **Backend**: EC2 t3.micro + Docker Compose (Spring Boot + MySQL)
- **CI/CD**: GitHub Actions (develop 브랜치 push 시 자동 배포)
- **이미지 레지스트리**: Amazon ECR

## 아키텍처

```
[GitHub Actions]
    ├── FE: build → S3 sync → CloudFront invalidation
    └── BE: docker build → ECR push → SSH → compose up

[CloudFront] → [S3] React SPA
[EC2 :80]   → Docker: backend (8080) + mysql (3306)
```

---

## AWS 리소스 생성 (순서대로)

### 1. EC2 인스턴스

- **리전**: ap-northeast-2 (서울)
- **AMI**: Ubuntu 24.04 LTS
- **타입**: t3.micro
- **스토리지**: 20GB gp3
- **키페어**: .pem 다운로드 → 안전 보관
- **보안 그룹**: 22(SSH), 80(HTTP) 인바운드

### 2. Elastic IP

- EC2 > Elastic IP > 새 주소 할당 → EC2에 연결
- EC2 실행 + 연결 상태면 무료

### 3. ECR 리포지토리

- 이름: `rehearse-backend`
- 리전: ap-northeast-2
- 태그 변경 가능성: Mutable

### 4. S3 버킷

- 이름: `rehearse-frontend-dev` (원하는 이름)
- 리전: ap-northeast-2
- **퍼블릭 액세스 모두 차단** (CloudFront OAC만 접근)

### 5. CloudFront 배포

- Origin: S3 버킷
- Origin Access: **OAC** 생성
- 기본 루트 객체: `index.html`
- 에러 페이지: 403/404 → `/index.html` (200 응답, SPA 라우팅)
- S3 버킷 정책에 CloudFront OAC 정책 추가

### 6. IAM 사용자 (CI/CD)

- 이름: `github-actions-deployer`
- 정책: `AmazonS3FullAccess`, `CloudFrontFullAccess`, `AmazonEC2ContainerRegistryFullAccess`
- 액세스 키 생성 (CLI용)

---

## EC2 초기 세팅

```bash
ssh -i your-key.pem ubuntu@{ELASTIC_IP}

# Docker 설치
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker ubuntu
exit  # 재접속으로 그룹 적용

ssh -i your-key.pem ubuntu@{ELASTIC_IP}

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
   - Backend 테스트 + Frontend 빌드 (병렬)
   - 둘 다 성공 시 배포:
     - FE: S3 sync → CloudFront 캐시 무효화
     - BE: Docker 이미지 빌드 → ECR push → EC2 SSH → compose pull → up
3. 헬스 체크: `curl http://{EC2_HOST}/actuator/health`

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

---

## 월 예상 비용

| 리소스 | 비용 |
|--------|------|
| EC2 t3.micro | ~$7.5 |
| Elastic IP | $0 (연결+실행 시) |
| ECR | ~$0.05 |
| S3 + CloudFront | ~$1 |
| **합계** | **~$8.5** |

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
