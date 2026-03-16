# CI/CD 배포 구축 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 비고 |
|---|--------|------|------|
| 0 | 문서 구조 생성 | Completed | |
| 1 | Flyway + 마이그레이션 + dev-server 프로필 | Completed | |
| 2 | Backend Dockerfile | Completed | |
| 3 | Docker Compose | Completed | |
| 4 | .gitignore 업데이트 | Completed | |
| 5 | GitHub Actions CI/CD | Completed | |
| 6 | 배포 가이드 + 환경변수 예시 | Completed | |

## 진행 로그

### 2026-03-16
- Task 0~6 전체 구현 완료
- 생성 파일: V1__init_schema.sql, application-dev-server.yml, Dockerfile, .dockerignore, docker-compose.yml, deploy-dev.yml, .env.dev-server.example, DEPLOYMENT.md
- 수정 파일: build.gradle.kts (Flyway 의존성), .gitignore (Docker 패턴)
