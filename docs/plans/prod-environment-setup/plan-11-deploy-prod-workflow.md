# Plan 11: `.github/workflows/deploy-prod.yml` 작성

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 05 (ECR 태그 전략), Plan 06 (docker-compose.prod.yml), Plan 10 (Environments)

## Why

prod 자동 배포 파이프라인이 필요하다. 기존 `deploy-dev.yml`을 템플릿으로 복제하되 다음 차이를 반영한다:

- 트리거: `main` 브랜치 push (develop→main merge 후)
- `environment: production` — required reviewer gate 통과해야 실제 배포 job 실행
- ECR 태그: `:prod-<sha>` + `:prod` 듀얼 푸시
- `docker-compose.prod.yml` 대상 SCP
- Frontend build env: `VITE_API_URL=https://api.rehearse.co.kr`
- Health check URL: `https://api.rehearse.co.kr/actuator/health`
- CloudFront invalidation: prod distribution ID

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `.github/workflows/deploy-prod.yml` | 신규 작성 |
| `.github/workflows/deploy-dev.yml` | `environment: development` 추가 (plan-10에서 이미 반영, 재확인) |

## 상세

### 워크플로우 구조

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

concurrency:
  group: deploy-prod
  cancel-in-progress: false   # dev와 동일 원칙: paths-filter 누락 방지

env:
  AWS_REGION: ${{ secrets.AWS_REGION }}

jobs:
  changes:
    name: Detect Changes
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
      infra: ${{ steps.filter.outputs.infra }}
      nginx: ${{ steps.filter.outputs.nginx }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
            frontend:
              - 'frontend/**'
            nginx:
              - 'backend/nginx/**'
            infra:
              - 'backend/docker-compose.prod.yml'
              - 'backend/nginx/**'
              - '.github/workflows/**'
              - 'backend/.env*'

  backend-test:
    name: Backend Test
    needs: changes
    if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('backend/**/*.gradle*', 'backend/gradle-wrapper.properties') }}
          restore-keys: gradle-
      - name: Run tests
        run: ./gradlew test --no-daemon
        env:
          SPRING_PROFILES_ACTIVE: test

  frontend-build:
    name: Frontend Build
    needs: changes
    if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.infra == 'true'
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - name: Build
        run: npm run build
        env:
          VITE_API_URL: https://api.rehearse.co.kr
      - uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: frontend/dist
          retention-days: 1

  deploy:
    name: Deploy to Production
    environment: production     # ← required reviewer gate
    needs: [changes, backend-test, frontend-build]
    if: always() && !failure() && !cancelled()
    runs-on: ubuntu-24.04-arm
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      # --- Frontend: S3 + CloudFront ---
      - name: Download frontend artifact
        if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.infra == 'true'
        uses: actions/download-artifact@v4
        with:
          name: frontend-dist
          path: frontend/dist

      - name: Deploy frontend to S3 (assets with long cache)
        if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.infra == 'true'
        run: |
          aws s3 sync frontend/dist s3://${{ secrets.S3_BUCKET_NAME }} \
            --delete \
            --exclude "index.html" \
            --cache-control "public, max-age=31536000, immutable"

      - name: Deploy index.html (no-cache)
        if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.infra == 'true'
        run: |
          aws s3 cp frontend/dist/index.html s3://${{ secrets.S3_BUCKET_NAME }}/index.html \
            --cache-control "no-cache, no-store, must-revalidate" \
            --content-type "text/html"

      - name: Invalidate CloudFront cache
        if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.infra == 'true'
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"

      # --- Backend: ECR + EC2 ---
      - name: Login to ECR
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Docker Buildx
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        uses: docker/setup-buildx-action@v3

      - name: Build and push backend image (prod tags)
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        working-directory: backend
        run: |
          docker buildx build \
            --cache-from type=gha \
            --cache-to type=gha,mode=max \
            -t ${{ secrets.ECR_REGISTRY }}/rehearse-backend:prod-${{ github.sha }} \
            -t ${{ secrets.ECR_REGISTRY }}/rehearse-backend:prod \
            --push .

      - name: Copy docker-compose.prod.yml and nginx config to EC2
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          source: "backend/docker-compose.prod.yml,backend/nginx/nginx.prod.conf"
          target: "~/rehearse"
          overwrite: true

      - name: Rename compose & nginx files on EC2
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/rehearse/backend
            mv -f docker-compose.prod.yml docker-compose.yml
            mv -f nginx/nginx.prod.conf nginx/nginx.conf

      - name: Deploy backend to EC2
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/rehearse/backend
            aws ecr get-login-password --region ${{ secrets.AWS_REGION }} | \
              docker login --username AWS --password-stdin ${{ secrets.ECR_REGISTRY }}
            docker compose --env-file .env stop backend
            docker compose --env-file .env pull backend
            docker compose --env-file .env up -d backend
            echo "Waiting for backend to start..."
            # 주의: backend 이미지(eclipse-temurin jre-alpine)에는 curl/wget 미포함 가능.
            # → host에서 컨테이너 IP로 직접 HTTP 요청 (docker exec + curl 의존성 회피)
            BACKEND_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' rehearse-backend)
            for i in $(seq 1 12); do
              sleep 5
              if curl -sf "http://${BACKEND_IP}:8080/actuator/health" > /dev/null 2>&1; then
                echo "Health check passed (attempt $i)"
                exit 0
              fi
              echo "Attempt $i/12: not ready yet..."
            done
            echo "Health check failed after 60s"
            docker compose --env-file .env logs backend --tail 80
            exit 1

      - name: Verify public endpoint
        if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.infra == 'true'
        run: |
          for i in $(seq 1 6); do
            if curl -sf https://api.rehearse.co.kr/actuator/health > /dev/null; then
              echo "Public health check passed (attempt $i)"
              exit 0
            fi
            sleep 10
          done
          echo "Public health check failed"
          exit 1

      - name: Restart Nginx on config change
        if: needs.changes.outputs.nginx == 'true'
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd ~/rehearse/backend
            docker compose restart nginx
```

### `deploy-dev.yml`과의 차이 요약

| 항목 | dev | prod |
|---|---|---|
| `on.push.branches` | `[develop]` | `[main]` |
| `environment:` | `development` (plan-10에서 추가) | `production` (+required reviewer) |
| `paths-filter infra` | `docker-compose.yml` | `docker-compose.prod.yml` |
| `VITE_API_URL` | `https://api-dev.rehearse.co.kr` | `https://api.rehearse.co.kr` |
| ECR tags | `:latest` | `:prod-<sha>` + `:prod` |
| SCP source | `docker-compose.yml,nginx/nginx.conf` | `docker-compose.prod.yml,nginx/nginx.prod.conf` |
| Rename step | 없음 | `mv prod.yml→yml`, `nginx.prod.conf→nginx.conf` |
| Public health check | 없음 (localhost만) | `https://api.rehearse.co.kr/actuator/health` |
| `system prune -af` | 있음 (디스크 회수) | **생략** (prod에서 공격적 prune은 의존 컨테이너 영향 위험, 별도 주기 cron으로 분리) |

### 롤백 경로

1. `main` 브랜치에서 이전 커밋으로 revert PR 머지 → 자동 재배포 (권장)
2. 긴급 시: plan-05의 ECR 태그 수동 재지정 (`:prod`를 이전 `prod-<sha>`로 이동) + EC2에서 `docker compose pull && up -d` 수동 실행 — 빠르지만 git 상태와 불일치

### `workflow_dispatch` 추가 이유

- 긴급 핫픽스 후 즉시 재배포 가능 (`main` push 없이 manual 재실행)
- 여전히 `environment: production`의 reviewer 승인은 필요 → 안전성 유지

## 담당 에이전트

- Implement: `devops-engineer` — 워크플로우 작성
- Review: `architect-reviewer` — 파이프라인 구조, paths-filter, concurrency
- Review: `code-reviewer` — 보안(SSH 키, ECR 로그인), 인증서 노출

## 검증

- `actionlint .github/workflows/deploy-prod.yml` → 에러 없음
- Repository에 merge 전 dry-run: `workflow_dispatch` 실행 → reviewer approval 대기 확인
- approval 후 job 순서대로 진행: changes → backend-test → frontend-build → deploy
- 최초 배포 성공 후: `curl https://api.rehearse.co.kr/actuator/health` 200
- `curl https://rehearse.co.kr` SPA 로드 확인
- ECR에 `:prod-<sha>` + `:prod` 태그 동시 존재 확인
- `progress.md` Task 11 → Completed
