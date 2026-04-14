# Rehearse 운영서버(prod) 구축 — 진행 상황

> 최종 업데이트: 2026-04-12
> 선행 문서: [`requirements.md`](./requirements.md)
> **선행 프로젝트**: [`../s3-key-schema-redesign/`](../s3-key-schema-redesign/progress.md) — 본 프로젝트 착수 전 완료 필수

## ⚠️ 선행 프로젝트 상태

| 항목 | 상태 |
|---|---|
| [`s3-key-schema-redesign`](../s3-key-schema-redesign/progress.md) | Draft (착수 대기) |
| `docs/architecture/s3-key-schema.md` (SSOT) | 미작성 |
| dev 환경 신규 스키마 E2E 검증 | 미수행 |
| dev 24h 안정화 완료 | — |

**본 프로젝트 모든 Task는 위 항목이 전부 Completed된 후 착수한다.** 특히 Task 3·4·14는 신규 스키마를 전제로 작성되어 있다.

## 태스크 상태

| # | 태스크 | 플랜 문서 | 태그 | 의존 | 상태 | 담당 | 비고 |
|---|---|---|---|---|---|---|---|
| 1 | `application-prod.yml` OAuth/JWT/Flyway/server 보강 | [plan-01](./plan-01-application-prod-yml.md) | `[parallel]` | — | Draft | backend / architect-reviewer | |
| 2 | prod EC2 인스턴스·보안그룹·IAM Role·EIP 생성 | [plan-02](./plan-02-ec2-provisioning.md) | `[parallel]` | — | Draft | devops-engineer / architect-reviewer | `t4g.small` ARM64 |
| 3 | `rehearse-videos-prod` S3 (v1.0 스키마) + EventBridge (prefix `interviews/raw/`) + DLQ | [plan-03](./plan-03-s3-eventbridge-mediaconvert.md) | `[parallel]` | **선행** s3-key-schema-redesign | Draft | devops-engineer / architect-reviewer | SSOT: `docs/architecture/s3-key-schema.md` |
| 4 | `rehearse-{analysis,convert}-prod` Lambda + alias + 배포 스크립트 확장 | [plan-04](./plan-04-lambda-prod-functions.md) | `[blocking]` | 3, **선행** s3-key-schema-redesign | Draft | devops-engineer / code-reviewer | 신규 파서 코드 기반 |
| 5 | ECR 태그 전략 + Lifecycle Policy | [plan-05](./plan-05-ecr-image-tagging.md) | `[parallel]` | — | Draft | devops-engineer / code-reviewer | `:prod` + `:prod-<sha>` |
| 6 | `docker-compose.prod.yml` + `nginx.prod.conf` | [plan-06](./plan-06-docker-compose-prod.md) | `[parallel]` | 1 | Draft | devops-engineer / architect-reviewer | |
| 7 | `rehearse-frontend-prod` S3 + 신규 CloudFront distribution | [plan-07](./plan-07-cloudfront-frontend.md) | `[blocking]` | 8 | Draft | devops-engineer / architect-reviewer | OAC 기반 |
| 8 | ACM us-east-1 `www` 1장 + 가비아 DNS (apex/www/api) + Let's Encrypt 멀티도메인 | [plan-08](./plan-08-route53-acm.md) | `[blocking]` | — | Draft | devops-engineer / architect-reviewer | **Route53 미사용**, 가비아 유지. apex는 EC2 nginx 301 리디렉션 |
| 9 | Google / GitHub OAuth prod client 등록 | [plan-09](./plan-09-oauth-console-registration.md) | `[parallel]` | — | Draft | devops-engineer / code-reviewer | |
| 10 | GitHub Environments 분리 + secrets 재구성 | [plan-10](./plan-10-github-secrets-environments.md) | `[parallel]` | 2, 5, 7 | Draft | devops-engineer / code-reviewer | required reviewer gate |
| 11 | `deploy-prod.yml` 워크플로우 작성 | [plan-11](./plan-11-deploy-prod-workflow.md) | `[blocking]` | 5, 6, 10 | Draft | devops-engineer / architect-reviewer + code-reviewer | |
| 12 | prod DB Flyway 최초 마이그레이션 | [plan-12](./plan-12-flyway-initial-migration.md) | `[blocking]` | 2, 6 | Draft | backend / architect-reviewer | baseline 검증 |
| 13 | 최초 컷오버 + E2E 스모크 테스트 | [plan-13](./plan-13-cutover-smoke-test.md) | `[blocking]` | 1~12 전부 | Draft | devops-engineer + qa / architect-reviewer | 최대 30분 다운타임 |
| 14 | Observability + 운영 Runbook | [plan-14](./plan-14-observability-runbook.md) | `[parallel]` | — | Draft | devops-engineer / architect-reviewer | CloudWatch + mysqldump |
| 15 | 질문 풀 시드 데이터 개선 (모범답변 퀄리티 + TTS 텍스트) | [plan-15](./plan-15-seed-data-improvement.md) | `[blocking]` | 12 | Draft | backend / code-reviewer | 1,438개 일괄 업데이트 |
| 16 | BETA 배지 로고 근처 노출 | [plan-16](./plan-16-beta-badge.md) | `[parallel]` | — | **Completed** | frontend | 4개 사용처 일괄 적용 |
| 17 | 개인정보 처리방침 정적 페이지 + /privacy 라우트 + footer 링크 | [plan-17](./plan-17-privacy-policy-page.md) | `[parallel]` | — | **Completed** | frontend | OAuth consent screen 심사 자료 |

### 병렬 가능 초기 배치
Task 1, 2, 3, 5, 8, 9, 14 (선행 의존 없음)

### 블로커 체인
- Task 4 ← 3
- Task 6 ← 1
- Task 7 ← 8
- Task 10 ← 2, 5, 7
- Task 11 ← 5, 6, 10
- Task 12 ← 2, 6
- Task 15 ← 12
- Task 13 ← 1~12, 15 전부

## 진행 로그

### 2026-04-14 (Phase A 코드 변경 완료)
- **완료 Task**: 1 (application-prod.yml 보강), 6 (docker-compose.prod.yml + nginx.prod.conf + nginx.cert-init.conf), 11 (deploy-prod.yml), 16 (BETA 배지), 17 (개인정보 처리방침 페이지)
- **신규 Task 추가**: 16, 17 — 런칭 전 필수 UX/법적 보강
- **선행 프로젝트 갱신**: s3-key-schema-redesign Task 4 Completed 반영
- **다음 단계**: Phase B — AWS CLI 인프라 생성 (Task 2, 3, 4, 5, 7, 8, 14 일부 자동 실행)

### 2026-04-12 (리뷰 피드백 반영)
- **Critical/High 이슈 수정 완료** (2차 Draft)
- 주요 변경:
  - **plan-06**: `nginx.cert-init.conf`(80-only 최초 발급용) 산출물 추가, AWS credentials 경로를 EC2 IAM Role 단일 경로로 확정, certbot daemon은 renew만 수행
  - **plan-14**: mysqldump를 `--defaults-extra-file` 방식으로 변경(root 비밀번호 프로세스 노출 제거), nginx 주간 reload host cron 추가(인증서 갱신 반영)
  - **plan-13**: Step 2/3 cert-init → prod.conf 교체 절차 명확화, Rollback Decision Tree(Case A~E) + 유지보수 페이지 사전 준비 체크리스트 추가
  - **plan-05**: ECR 롤백 시 `:prod` 태그 `batch-delete-image` 선행 절차 추가(`ImageAlreadyExistsException` 회피)
  - **plan-04**: MediaConvert Role prod 버킷 권한 보강을 **별도 태스크**로 명시 + IAM simulate 검증, Lambda On-failure Destination(DLQ) 설정 추가
  - **plan-03**: EventBridge Rule DLQ + Lambda async DLQ 2계층 정책 명확화 + SQS 리소스 정책 샘플
  - **plan-01**: Flyway `baseline-version` 미설정 경고 코멘트 추가
  - **plan-02**: Key Pair 1Password 백업 명시
  - **plan-10**: IAM user 권한을 `PowerUser` 관리형 → Resource ARN 기반 custom 정책으로 축소
  - **plan-11**: backend health check를 `docker exec curl` → `host → container IP` 방식으로 변경(이미지 curl 의존성 회피)
  - **plan-12**: `.env` source 및 `${VAR:?}` 검증 구문 추가

### 2026-04-12 (1차 Draft)
- **플랜 문서 세트 작성 완료** (Draft)
- 생성 파일:
  - `docs/plans/prod-environment-setup/requirements.md`
  - `docs/plans/prod-environment-setup/plan-01-application-prod-yml.md`
  - `docs/plans/prod-environment-setup/plan-02-ec2-provisioning.md`
  - `docs/plans/prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md`
  - `docs/plans/prod-environment-setup/plan-04-lambda-prod-functions.md`
  - `docs/plans/prod-environment-setup/plan-05-ecr-image-tagging.md`
  - `docs/plans/prod-environment-setup/plan-06-docker-compose-prod.md`
  - `docs/plans/prod-environment-setup/plan-07-cloudfront-frontend.md`
  - `docs/plans/prod-environment-setup/plan-08-route53-acm.md`
  - `docs/plans/prod-environment-setup/plan-09-oauth-console-registration.md`
  - `docs/plans/prod-environment-setup/plan-10-github-secrets-environments.md`
  - `docs/plans/prod-environment-setup/plan-11-deploy-prod-workflow.md`
  - `docs/plans/prod-environment-setup/plan-12-flyway-initial-migration.md`
  - `docs/plans/prod-environment-setup/plan-13-cutover-smoke-test.md`
  - `docs/plans/prod-environment-setup/plan-14-observability-runbook.md`
  - `docs/plans/prod-environment-setup/progress.md`
- 사용자 결정사항 반영:
  - DB: EC2 Docker MySQL 유지 (RDS 기각)
  - API 도메인: `api.rehearse.co.kr`
  - EC2: `t4g.small` ARM64 (dev와 동일 스펙)
  - 배포 트리거: `main` push + GitHub Environments required reviewer

## 다음 단계

1. 팀 리뷰 (architect-reviewer / code-reviewer) 진행
2. 리뷰 피드백 반영 후 `Draft` → `In Progress`
3. 병렬 태스크(1, 2, 3, 5, 8, 9, 14)부터 개별 PR로 실행
