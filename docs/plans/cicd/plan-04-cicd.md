# Plan 04: GitHub Actions CI/CD

> 상태: Completed
> 작성일: 2026-03-16

## Why

develop 브랜치 머지 시 수동 배포 없이 자동으로 FE/BE 모두 배포.

## 생성 파일

| 파일 | 설명 |
|------|------|
| `.github/workflows/deploy-dev.yml` | CI/CD 파이프라인 |

## 흐름

```
develop push 트리거
  ├── [parallel] backend-test: gradlew test
  └── [parallel] frontend-build: npm ci → lint → build
         ↓ (둘 다 성공)
      deploy:
        ├── frontend: S3 sync + CloudFront invalidation
        └── backend: ECR build+push → SSH → compose pull → up -d → health check
```

## 담당 에이전트
- Implement: `devops`
- Review: `deployment-engineer`

## 검증
- GitHub Actions 워크플로우 YAML 문법 검증
- develop push 시 파이프라인 정상 실행
- `progress.md` 상태 업데이트 (Task 5 → Completed)
