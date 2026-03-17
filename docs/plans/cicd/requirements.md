# CI/CD + AWS 개발 서버 배포 — 요구사항 정의

> 상태: In Progress
> 작성일: 2026-03-16

## Why

포트폴리오 시연과 실제 환경 검증을 위해 AWS 개발 서버 배포가 필요하다.
현재 Docker/CI/CD 인프라가 전혀 없으므로 처음부터 구축한다.

## 배포 목표

- develop 브랜치 머지 시 자동 배포
- Frontend: S3 + CloudFront (CDN)
- Backend: EC2 + Docker Compose (backend + mysql)
- DB 마이그레이션: Flyway

## 인프라 아키텍처

```
[GitHub Actions - develop 머지 트리거]
    ├── Frontend: npm build → S3 업로드 → CloudFront 캐시 무효화
    └── Backend: docker build → ECR push → SSH → docker compose pull → up -d

[사용자 브라우저]
    ├── CloudFront URL → [S3] React SPA
    └── VITE_API_URL  → [EC2 t3.micro :80]
                            ├── docker: backend (ECR 이미지, 80:8080)
                            └── docker: mysql (3306)
```

## AWS 리소스 + 예상 비용

| 리소스 | 스펙 | 월 비용 |
|--------|------|---------|
| EC2 | t3.micro | ~$7.5 |
| Elastic IP | EC2 연결 시 무료 | $0 |
| ECR | ~500MB | ~$0.05 |
| S3 + CloudFront | 소량 트래픽 | ~$1 |
| **합계** | | **~$8.5** |

## 환경변수

### Backend (backend/.env)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`
- `CLAUDE_API_KEY`, `CORS_ALLOWED_ORIGINS`
- `SPRING_PROFILES_ACTIVE=dev`
- `ECR_REGISTRY`

### Frontend (빌드 시)
- `VITE_API_URL=http://{ELASTIC_IP}`

### GitHub Secrets
- `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
- `ECR_REGISTRY`, `S3_BUCKET_NAME`, `CLOUDFRONT_DISTRIBUTION_ID`

## CORS 정책
- `CORS_ALLOWED_ORIGINS`: CloudFront 도메인 (https://xxx.cloudfront.net)

## 보안 요구사항
- EC2 보안 그룹: 22(SSH), 80(HTTP) 만 허용
- S3: 퍼블릭 차단, CloudFront OAC만 접근
- Docker: non-root 사용자 실행
- 환경변수: .env 파일 git 미추적
