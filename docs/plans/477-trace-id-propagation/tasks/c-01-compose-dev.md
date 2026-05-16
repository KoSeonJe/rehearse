# Task C1 — backend/docker-compose.yml dev awslogs driver

> **PR**: PR-C
> **영역**: infra
> **선행 Task**: PR-A 머지 (WARN 메시지 패턴 정합)

---

## 변경 파일

- `backend/docker-compose.yml` — **변경**. service `backend` 에 `logging.driver: awslogs` + options 추가.

> **참고**: tech-spec.md §Evidence — dev compose = `backend/docker-compose.yml` (검증 완료).
> **사용 범위 (결정됨)**: `backend/docker-compose.yml` = **dev EC2 전용**. 로컬 개발자 환경 = `backend/docker-compose.local.yml` (MySQL 단독). 로컬 개발자가 본 compose 를 사용하지 않으므로 awslogs driver 적용 시 로컬 영향 없음. compose override 분리 패턴 불필요.

---

## 핵심 로직

```yaml
# backend/docker-compose.yml (변경부)
services:
  backend:
    # ... 기존 설정 ...
    logging:
      driver: awslogs
      options:
        awslogs-region: ap-northeast-2
        awslogs-group: /rehearse/backend/dev
        awslogs-stream: ec2-dev
        awslogs-create-group: "true"
```

> **주의**: dev compose = **dev EC2 전용** (결정됨). 로컬 개발자는 `docker-compose.local.yml` 사용 → IAM 권한 부재 환경에서 본 compose 가동 시나리오 없음. compose override 분리 불필요.

---

## 의존

- 선행: PR-A 머지 (WARN 메시지 텍스트가 metric filter 패턴 대상)
- 외부: dev EC2 IAM Role 권한 (`logs:CreateLogStream`, `logs:PutLogEvents`) — C3 에서 처리

---

## Verification Hook

- 명령: 자동 명령 없음 (인프라). 운영자 직접:
  - dev EC2 SSH 후 `docker compose -f backend/docker-compose.yml up -d` (또는 운영 절차)
  - `aws logs describe-log-groups --log-group-name-prefix /rehearse/backend/dev` → 그룹 생성 확인
  - `aws logs filter-log-events --log-group-name /rehearse/backend/dev --max-items 10` → 부팅 로그 라인 확인
- 통과 기준: dev log group 생성 + 적재 동작

---

## 커밋 메시지 (예상)

```
chore(infra): dev compose 에 awslogs driver 적용
```
