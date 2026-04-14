# Plan 10: GitHub Environments (`development` / `production`) + Secrets 재구성

> 상태: Draft
> 작성일: 2026-04-12

## Why

현재 `.github/workflows/deploy-dev.yml`은 **Repository secrets**에서 직접 자격을 참조한다 (`${{ secrets.AWS_ACCESS_KEY_ID }}`, `${{ secrets.EC2_HOST }}` 등). prod 워크플로우가 추가되면 이 방식으로는:

- dev/prod 자격이 같은 네임스페이스에 혼재 → 오기·오용 가능
- prod 배포에 **manual approval gate**를 걸 수 없음 (Environment 단위에서만 required reviewer 지원)
- 감사 로그가 환경별로 구분되지 않음

GitHub Environments로 분리하면 (1) secret 네임스페이스 격리, (2) required reviewer, (3) branch protection, (4) Environment-level audit log 전부 확보된다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| GitHub Environment `development` | 신규 생성 + dev secrets 이관 |
| GitHub Environment `production` | 신규 생성 + prod secrets 추가 + required reviewer |
| `.github/workflows/deploy-dev.yml` | `environment: development` 추가 |
| Repository secrets | 이관 후 정리 (중복 제거) |
| `docs/architecture/infrastructure-status.md` | Environment 구조 기록 |

## 상세

### GitHub Environments 생성

Repository Settings → Environments → New environment.

#### 1. `development`

| 설정 | 값 |
|---|---|
| Name | `development` |
| Required reviewers | **없음** (자동 배포) |
| Wait timer | 0 min |
| Deployment branches | `develop` only |
| Environment secrets | dev 자격 (아래 표) |

**dev secrets** (현재 Repository secrets에서 이관):

| 키 | 값 | 비고 |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | 기존 dev IAM user | |
| `AWS_SECRET_ACCESS_KEY` | 기존 dev IAM user | |
| `AWS_REGION` | `ap-northeast-2` | |
| `ECR_REGISTRY` | `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com` | prod와 동일 |
| `S3_BUCKET_NAME` | dev frontend 버킷명 | |
| `CLOUDFRONT_DISTRIBUTION_ID` | `d2n8xljv54hfw0`의 ID | |
| `EC2_HOST` | `54.180.188.135` | |
| `EC2_USERNAME` | `ubuntu` | |
| `EC2_SSH_KEY` | dev `rehearse-key.pem` 내용 | |

#### 2. `production`

| 설정 | 값 |
|---|---|
| Name | `production` |
| **Required reviewers** | 최소 1명 (관리자 계정들 등록) |
| Wait timer | 0 min (reviewer 응답 즉시) |
| Deployment branches | `main` only |
| Environment secrets | prod 자격 (아래 표) |

**prod secrets**:

| 키 | 값 | 비고 |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | **prod 전용 IAM user** 신규 발급 | dev와 분리 필수 |
| `AWS_SECRET_ACCESS_KEY` | prod IAM user | |
| `AWS_REGION` | `ap-northeast-2` | |
| `ECR_REGISTRY` | `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com` | dev와 동일 (ECR 레포 공용) |
| `S3_BUCKET_NAME` | `rehearse-frontend-prod` | plan-07 산출 |
| `CLOUDFRONT_DISTRIBUTION_ID` | plan-07 산출 distribution ID | |
| `EC2_HOST` | prod EC2 Elastic IP (plan-02 산출) | |
| `EC2_USERNAME` | `ubuntu` | |
| `EC2_SSH_KEY` | `rehearse-prod-key.pem` 내용 | plan-02 생성 |

**주의**: 같은 변수명을 사용한다 (`AWS_ACCESS_KEY_ID` 등). 워크플로우 파일에서 `environment: production` 을 지정하면 자동으로 해당 Environment의 값으로 치환된다 → `.github/workflows/deploy-prod.yml`은 변수명만 참조하면 됨(값은 Environment가 주입).

### Prod IAM User 권한

신규 IAM user `rehearse-prod-ci` 생성:

**커스텀 정책 `rehearse-prod-ci-policy`** (관리형 PowerUser 대신 최소 권한):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuthToken",
      "Effect": "Allow",
      "Action": ["ecr:GetAuthorizationToken"],
      "Resource": "*"
    },
    {
      "Sid": "EcrPushPullRehearseBackend",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "arn:aws:ecr:ap-northeast-2:776735194358:repository/rehearse-backend"
    },
    {
      "Sid": "S3FrontendProd",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:DeleteObject", "s3:ListBucket", "s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::rehearse-frontend-prod",
        "arn:aws:s3:::rehearse-frontend-prod/*"
      ]
    },
    {
      "Sid": "CloudFrontInvalidation",
      "Effect": "Allow",
      "Action": ["cloudfront:CreateInvalidation"],
      "Resource": "arn:aws:cloudfront::776735194358:distribution/<prod-dist-id>"
    }
  ]
}
```

**PowerUser 기각 이유**: `AmazonEC2ContainerRegistryPowerUser`는 리전 내 **모든 ECR 리포** 대상 권한을 부여한다. MVP 단계에서 리포가 `rehearse-backend` 하나뿐이라도, 추후 신규 리포 생성 시 prod CI가 자동으로 쓰기 권한을 얻게 되어 blast radius 확장. Resource ARN 기반 custom 정책으로 고정.

**명시적 Deny (권장)**: dev 자원 접근 차단 정책을 attachment로 추가해 오접근 방지.

**ECR는 공용**이므로 dev IAM user 정책도 `rehearse-backend` 레포 전체를 허용하지만, 이미지 태그는 네임스페이스로 분리(`:latest` vs `:prod*`)되어 논리적 격리 유지.

### 워크플로우 참조 변경

`.github/workflows/deploy-dev.yml` 의 `jobs.deploy`에 추가:
```yaml
deploy:
  name: Deploy
  environment: development    # ← 신규 추가
  needs: [changes, backend-test, frontend-build]
  if: always() && !failure() && !cancelled()
  runs-on: ubuntu-24.04-arm
```

`.github/workflows/deploy-prod.yml`(plan-11에서 작성)에도 동일하게:
```yaml
deploy-prod:
  name: Deploy to Production
  environment: production    # ← required reviewer gate 발생
  runs-on: ubuntu-24.04-arm
```

**Effect**: `environment: production` 선언 시 GitHub가 배포 run을 **대기 상태**로 보류하고, reviewer가 승인해야 job이 실행됨. 이 승인은 Actions run 페이지에서 표시됨.

### Repository secrets 정리

이관 완료 후 Repository-level secrets에서 dev/prod 관련 키 **삭제** → secret 이중 관리 방지. ECR_REGISTRY처럼 양쪽 공통인 것도 Environment별로 복제 (Repository-level에 남기면 Environment secrets가 override되지만 혼동 소지).

## 담당 에이전트

- Implement: `devops-engineer` — Environment 생성, IAM user, secret 이관
- Review: `code-reviewer` — 보안 경계, required reviewer 정책

## 검증

- Repository Settings → Environments에 `development`, `production` 2개 존재
- `production` Environment에 required reviewer 최소 1명 등록
- `production` Environment의 `Deployment branches` → `main` only
- `deploy-dev.yml` 실행 시 `environment: development`가 run summary에 표시됨
- `deploy-prod.yml` 실행 시 reviewer approval 대기 상태 진입 (plan-11 완료 후 실테스트)
- AWS IAM → `rehearse-prod-ci` user 존재, attached policies 확인
- `aws s3 ls s3://rehearse-videos-dev --profile rehearse-prod-ci` → AccessDenied (격리 확인)
- `progress.md` Task 10 → Completed
