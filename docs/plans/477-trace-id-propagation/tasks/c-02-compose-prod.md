# Task C2 — backend/docker-compose.prod.yml prod awslogs driver

> **PR**: PR-C
> **영역**: infra
> **선행 Task**: PR-A 머지

---

## 변경 파일

- `backend/docker-compose.prod.yml` — **변경**. service `backend` 에 `logging.driver: awslogs` + options 추가.

---

## 핵심 로직

```yaml
# backend/docker-compose.prod.yml (변경부)
services:
  backend:
    # ... 기존 설정 ...
    logging:
      driver: awslogs
      options:
        awslogs-region: ap-northeast-2
        awslogs-group: /rehearse/backend/prod
        awslogs-stream: ec2-prod
        awslogs-create-group: "true"
```

---

## 의존

- 선행: PR-A 머지, C1 (dev 검증 선행)
- 외부: prod EC2 IAM Role 권한 — C3 에서 처리

---

## Verification Hook

- 명령: 운영자 직접:
  - prod EC2 SSH 후 컨테이너 재기동 (다운타임 < 1분, nginx 502 잠시)
  - `aws logs describe-log-groups --log-group-name-prefix /rehearse/backend/prod` → 그룹 생성 확인
- 통과 기준: prod log group 생성 + 적재 동작
- 관찰 가능 동작: prod 1회 사용자 요청 → CloudWatch `/rehearse/backend/prod` 에 stdout 라인 출력

---

## 커밋 메시지 (예상)

```
chore(infra): prod compose 에 awslogs driver 적용
```
