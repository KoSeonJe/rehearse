# Plan 05: ECR 이미지 태깅 전략 + Lifecycle Policy

> 상태: Draft
> 작성일: 2026-04-12

## Why

현재 `.github/workflows/deploy-dev.yml:163` 는 `rehearse-backend:latest` 단일 태그로 이미지를 푸시한다. prod가 동일 레지스트리를 쓰면서 태그가 하나뿐이면 다음 문제가 발생한다:

1. **롤백 불가** — `:latest`는 mutable이라 이전 sha 이미지를 구분할 수 없음
2. **혼용 리스크** — dev/prod가 같은 태그를 덮어쓰면 경합 조건에서 prod가 개발 코드로 기동될 수 있음
3. **감사 추적 부재** — 배포 시점과 이미지의 git sha 매핑이 기록되지 않음

태그 전략을 명시적으로 분리하고 ECR Lifecycle Policy로 비용 증가를 방어한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| ECR repo `rehearse-backend` | 태그 전략 문서화 (신규 리소스 생성 없음) |
| ECR Lifecycle Policy | 신규 작성/적용 |
| `.github/workflows/deploy-dev.yml` | 변경 없음 (기존 `:latest` 유지) |
| `.github/workflows/deploy-prod.yml` | plan-11에서 `:prod-<sha>` + `:prod` 태그 푸시 |
| `docs/architecture/infrastructure-status.md` | ECR 태그 규칙 섹션 추가 |

## 상세

### 태그 규칙

| 태그 | 의미 | 작성 주체 | Mutable |
|---|---|---|---|
| `latest` | dev 최신 빌드 | `deploy-dev.yml` (기존) | Yes |
| `prod` | prod 최신 빌드 (배포 대상 포인터) | `deploy-prod.yml` | Yes |
| `prod-<git-sha>` | prod 특정 커밋 빌드 (롤백용 불변 스냅샷) | `deploy-prod.yml` | No (정책상) |
| `dev-<git-sha>` | (선택) dev 롤백용 스냅샷 | `deploy-dev.yml` 확장 시 | No |

**초기 범위**: `prod` + `prod-<sha>` 2개만 도입. `dev-<sha>`는 별건으로 나중에 검토 (MVP에선 불필요).

### Prod 빌드 푸시 커맨드 (plan-11에서 사용)

```yaml
- name: Build and push backend image (prod)
  working-directory: backend
  run: |
    TAG_SHA="prod-${{ github.sha }}"
    docker buildx build \
      --cache-from type=gha \
      --cache-to type=gha,mode=max \
      -t ${{ secrets.ECR_REGISTRY }}/rehearse-backend:$TAG_SHA \
      -t ${{ secrets.ECR_REGISTRY }}/rehearse-backend:prod \
      --push .
```

`docker-compose.prod.yml`은 `:prod`를 pull → CD가 항상 최신 포인터 유지. 롤백 시 `prod-<이전sha>`를 다시 `:prod`로 태그 재이동.

### 롤백 절차

```bash
# 1. 이전 커밋의 태그 확인
aws ecr describe-images --repository-name rehearse-backend \
  --query 'imageDetails[?contains(imageTags, `prod-`)].[imagePushedAt, imageTags]' \
  --output table

# 2. 기존 :prod 태그 제거 (put-image ImageAlreadyExistsException 회피)
aws ecr batch-delete-image --repository-name rehearse-backend \
  --image-ids imageTag=prod

# 3. 이전 sha의 manifest를 가져와 :prod로 재태깅
MANIFEST=$(aws ecr batch-get-image --repository-name rehearse-backend \
  --image-ids imageTag=prod-<이전sha> --query 'images[0].imageManifest' --output text)

aws ecr put-image --repository-name rehearse-backend \
  --image-tag prod --image-manifest "$MANIFEST"

# 4. EC2에서 pull & up
ssh ubuntu@<prod-EIP> 'cd ~/rehearse/backend && docker compose --env-file .env pull backend && docker compose --env-file .env up -d backend'
```

**주의**: Step 2 생략 시 `ImageAlreadyExistsException: The specified image is already associated with tag 'prod'` 발생. `batch-delete-image`는 manifest는 유지하고 태그만 제거하므로 데이터 손실 없음 (실제 image는 `prod-<sha>` 태그로 여전히 접근 가능).

이 절차는 `docs/guides/rollback-runbook.md`(plan-14에서 신규 작성)로 이전.

### ECR Lifecycle Policy

`rehearse-backend` 리포에 다음 정책 적용:

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 10 prod-<sha> tagged images",
      "selection": {
        "tagStatus": "tagged",
        "tagPrefixList": ["prod-"],
        "countType": "imageCountMoreThan",
        "countNumber": 10
      },
      "action": { "type": "expire" }
    },
    {
      "rulePriority": 2,
      "description": "Expire untagged images after 14 days",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 14
      },
      "action": { "type": "expire" }
    }
  ]
}
```

**규칙 설명**:
- Rule 1: `prod-*` 태그 이미지는 최근 10개만 유지 (약 10회 전 버전까지 롤백 가능)
- Rule 2: `:latest`, `:prod`는 항상 mutable이라 이 규칙 대상 아님. untagged(이전 덮어쓴 manifest)는 14일 후 자동 삭제

**`:latest` / `:prod` 보호**: Lifecycle Policy는 tagged 이미지를 prefix 기반으로만 대상화 → mutable 포인터는 영향 없음.

### ECR 권한 재확인

- `deploy-dev.yml` 배포 자격: dev IAM user/role → `ecr:PutImage`, `ecr:BatchCheckLayerAvailability` 등 필요
- `deploy-prod.yml` 배포 자격 (plan-10에서 분리): prod IAM user → 동일 액션 + **같은 리포 대상**
- ECR 리포는 dev/prod 공용 단일 레포(`rehearse-backend`) 유지 — 레포 자체를 분리하지 않는 이유: 단일 코드베이스, 빌드 cache 공유 이점, 비용 동일

## 담당 에이전트

- Implement: `devops-engineer` — Lifecycle Policy 적용, 태그 규칙 문서화
- Review: `code-reviewer` — 롤백 절차 검증, 태그 전략 무결성

## 검증

- `aws ecr get-lifecycle-policy --repository-name rehearse-backend` → 위 JSON과 일치
- `aws ecr describe-images --repository-name rehearse-backend --query 'imageDetails[*].[imageTags[0],imagePushedAt]' --output table` → 현황 확인
- dev 배포 1회 실행 후 `:latest` 정상 업데이트 확인 (회귀 없음)
- prod 배포 1회 실행 후 `:prod` + `:prod-<sha>` 동시 존재 확인 (plan-11 완료 후)
- 롤백 절차 dry-run (이전 태그로 `:prod` 재지정 후 `docker pull` 확인)
- `progress.md` Task 5 → Completed
