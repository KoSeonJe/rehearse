# Rehearse 운영서버(prod) 구축 — 요구사항 정의

> 상태: Draft (선행 작업 대기)
> 작성일: 2026-04-12
> 선행 문서: `docs/plans/dev-domain-restore/requirements.md` (Completed 2026-04-09)
> **선행 프로젝트**: [`docs/plans/s3-key-schema-redesign/`](../s3-key-schema-redesign/requirements.md) — dev에서 S3 키 스키마 v1.0 전환 완료 후 착수

---

## ⚠️ 선행 프로젝트 완료 전제

본 프로젝트는 [`s3-key-schema-redesign`](../s3-key-schema-redesign/requirements.md) **완료 후** 착수한다. 이유:

- prod S3 버킷을 깨끗한 신규 스키마로 시작해야 EventBridge 규칙·Lifecycle 정책이 단순해짐
- 현재 dev 키(`videos/{interviewId}/qs_{questionSetId}.webm`)는 원본·파생 혼재, 날짜 파티션 부재, UUID 부재 등 7가지 약점 보유
- prod를 레거시 스키마로 먼저 올리면 후속 전환 시 dev·prod 동시 마이그레이션 필요 → 리스크 2배
- 사용자 결정: **Lambda는 신규 스키마만 처리**, 레거시 파서 미존재 → dev에서 드레인 검증 필수

**선행 프로젝트 완료 조건**:
- [ ] `s3-key-schema-redesign` Task 1~4 전부 Completed
- [ ] `docs/architecture/s3-key-schema.md` SSOT 문서 머지
- [ ] dev 환경에서 신규 스키마 E2E 성공 + 24h 안정화
- [ ] dev `file_metadata`에 레거시 키 보유 행 0건

본 프로젝트의 모든 S3 키 설계(`plan-03`, `plan-14`의 DB 백업 경로 등)는 [`docs/architecture/s3-key-schema.md`](../../architecture/s3-key-schema.md)를 **SSOT로 참조**한다.

## Why

현재 Rehearse는 dev 환경 하나로만 운영되고 있다 (`dev.rehearse.co.kr` 프론트 + `api-dev.rehearse.co.kr` 백엔드 + EC2 `54.180.188.135`). 실사용자 유입이 시작되면 dev 서버 위에서 다음 리스크가 동시다발로 발생한다:

- **실데이터와 개발 데이터 혼재** — QA/디버깅 시 사용자 면접 데이터 오염 가능
- **Lambda/MediaConvert/OpenAI 한도 공유** — 개발자 실험으로 유료 API 쿼터 소진 시 실사용자 장애
- **OAuth 콜백 혼동** — dev 로그 확인 중 실사용자 세션까지 영향
- **DB 마이그레이션 리스크** — dev에서 Flyway 실험하다 실운영 스키마 깨짐

선행 작업인 `dev-domain-restore` (2026-04-09 Completed)로 apex 도메인 `rehearse.co.kr`이 dev CloudFront alias에서 분리되어 **이제 prod용으로 할당 가능**한 상태가 되었다. 이 시점에 운영서버를 신규 구축해 dev/prod를 물리적으로 완전히 분리한다.

### Decision Framework

**1. Why?**

dev 단일 환경으로는 실사용자 트래픽을 안전하게 받을 수 없다. 데이터·쿼터·OAuth·DB 마이그레이션이 모두 dev와 뒤섞여 장애 전파 경로가 된다. dev-domain-restore 완료로 도메인·인증서·OAuth 콘솔 제약이 해소되었으므로, 지금이 prod 구축 최적 시점이다.

**2. Goal**

- **프론트 진입점**: `https://rehearse.co.kr` (apex) + `https://www.rehearse.co.kr`
- **백엔드 API**: `https://api.rehearse.co.kr`
- **dev 환경 유지**: `https://dev.rehearse.co.kr`, `https://api-dev.rehearse.co.kr` (변경 없음)
- **물리 분리**: EC2 인스턴스, S3 버킷, Lambda 함수, CloudFront distribution, DB, ECR 태그, IAM 자격 모두 분리
- **배포 파이프라인**: `main` 브랜치 push + manual approval gate로 prod 자동 배포
- **최초 컷오버 다운타임 ≤ 30분** 허용 (단일 EC2 최초 기동이므로 불가피)

**성공 기준**

- `curl -I https://rehearse.co.kr` → 200 OK + prod CloudFront 헤더
- `curl -I https://api.rehearse.co.kr/actuator/health` → 200 OK
- GitHub/Google OAuth 로그인 → JWT 쿠키 세팅 + 메인 진입
- 면접 1회 E2E 완주: 녹화 → `rehearse-videos-prod` 업로드 → `rehearse-analysis-prod` 호출 → 피드백 생성 → UI 렌더
- `flyway_schema_history`에 V1~Vn 전체 Success 기록
- dev 환경 회귀 없음 (dev CloudFront / EC2 / Lambda 모두 정상)

**3. Evidence (2026-04-12 실측)**

#### 코드/설정
- `backend/src/main/resources/application-prod.yml` — **OAuth/JWT/Flyway/server 설정 전부 누락**. 현재 상태로 prod 프로파일 기동 시 인증 불가. `hikari`·`s3 bucket`·`frontend-url`·`cors` 기본값만 존재 (`application-prod.yml:1-69`)
- `backend/src/main/resources/application-dev.yml:1-78` — 보강 기준 (OAuth 2개 provider, JWT, Flyway, multipart, forward-headers-strategy 전부 포함)
- `backend/docker-compose.yml:22,44` — 이미지 태그 `:latest`와 `S3_BUCKET=rehearse-videos-dev` 하드코딩 → prod 분리 필요
- `.github/workflows/deploy-dev.yml` — dev 배포 워크플로우만 존재 (prod 없음). `runs-on: ubuntu-24.04-arm`으로 이미 ARM64 빌드 중이라 `t4g.small` 대상 빌드 가능
- `lambda/analysis/config.py:8`, `lambda/convert/config.py:9` — 기본값 `"rehearse-videos-dev"` (환경변수로 오버라이드 가능)
- `lambda/lambda-safe-deploy.sh` — alias 기반 무중단 배포 스크립트 존재, prod 함수명으로 확장 필요

#### 인프라 실측
- **IaC 없음**: `terraform/`, `cdk/`, `infra/` 디렉토리 부재 → 전부 AWS 콘솔/CLI 수동 구성
- dev 자원 (재사용 불가, 복제 대상):
  - EC2: `54.180.188.135` (t4g.small, Elastic IP, ARM64)
  - S3 videos: `rehearse-videos-dev`
  - S3 frontend: dev용 버킷 + CloudFront `d2n8xljv54hfw0`
  - Lambda: `rehearse-analysis-dev`, `rehearse-convert-dev`
  - EventBridge: `rehearse-video-uploaded-dev`
  - ECR: `776735194358.dkr.ecr.ap-northeast-2.amazonaws.com/rehearse-backend:latest`
- dev-domain-restore 완료 상태: CloudFront `d2n8xljv54hfw0`에서 apex alias 제거 완료. prod CloudFront용 us-east-1 ACM 인증서는 `www.rehearse.co.kr` 1장만 신규 발급 (apex는 가비아 apex CNAME 미지원으로 CloudFront에 바인딩 불가 → EC2 nginx 리디렉션 방식, plan-08 참조)
- **DNS는 가비아 유지** (Route53 이관 기각, 사용자 결정). 가비아 apex 제약은 EC2 nginx 301 리디렉션으로 해결

#### 도메인 가용성
- `rehearse.co.kr` — 회수 완료, prod 할당 대기
- `www.rehearse.co.kr` — 신규 CNAME 가능
- `api.rehearse.co.kr` — 신규 A 레코드 가능 (사용자 결정사항)

**4. Trade-offs**

- **RDS 기각 → EC2 Docker MySQL 유지** — 비용 최소화 (RDS는 월 $15+ 추가). 트레이드오프: 자동 백업/PITR/Multi-AZ 없음. 수동 `mysqldump` 운영 필요. 트래픽 증가 시 별건으로 RDS 이관 계획 수립 예정
- **IaC 기각 → 수동 구성 유지** — Terraform 도입은 학습 비용·러닝커브·초기 작업량 큼. 대안: 문서화(AGENTS.md 스타일)로 재현 가능성 확보. 미래에 자원 복잡도 임계치 넘으면 별건 도입
- **`:prod` + `:prod-<sha>` 듀얼 태그** — `latest`는 dev 전용으로 고정. prod는 sha 기반 불변 태그 + `:prod` 최신 포인터 → 롤백 시 이전 sha 재푸시 가능
- **단일 EC2 유지 (로드밸런서/오토스케일링 없음)** — MVP 단계 트래픽 전제. 단점: 최초 컷오버 시 30분 다운타임 수용. 대안: Blue/Green 2대 구성은 비용 2배 + 운영 복잡도 증가로 MVP에 부적합
- **GitHub Environments required reviewer** — CI는 `main` push 자동 트리거지만 실제 배포는 승인 게이트 통과해야만 실행. 대안: Git tag 트리거는 승인 단계 없이 엄격하지만 핫픽스 속도 저하
- **OAuth 앱 prod 전용 신규 생성** — Google/GitHub OAuth client를 prod 별도 발급해 자격 격리. 대안: dev와 공유는 로그/쿼터 혼동 + 보안 경계 모호
- **WAF/Shield Out of Scope** — MVP 단계 비용 대비 효용 낮음. 별건 후속 프로젝트로 이관
- **컨테이너 내장 MySQL 볼륨명 분리** (`mysql_data_prod`) — dev/prod 동시 로컬 테스트 시 충돌 방지

## 목표

위 Goal 섹션 참조.

## 아키텍처 / 설계

### 현재 (2026-04-12)

```
사용자
  └─→ https://dev.rehearse.co.kr ──→ CloudFront d2n8xljv54hfw0 ──→ S3 (dev frontend)
  └─→ https://api-dev.rehearse.co.kr ──→ EC2 54.180.188.135 ──→ Nginx ──→ Spring Boot(dev)
                                                                      │
                                                                      ├─→ MySQL 8.0 (container, mysql_data)
                                                                      ├─→ rehearse-videos-dev (S3)
                                                                      │        │
                                                                      │        └─→ EventBridge ──→ rehearse-analysis-dev (Lambda)
                                                                      │                       └─→ rehearse-convert-dev (Lambda)
                                                                      │
                                                                      └─→ ECR rehearse-backend:latest

apex(rehearse.co.kr) — 회수 완료, 미할당
```

### 목표 (After)

```
사용자
  ├─→ https://rehearse.co.kr (apex)
  │     └─→ 가비아 DNS A → prod EC2 EIP
  │          └─→ Nginx 443 (apex server block, Let's Encrypt SAN)
  │               └─→ 301 Redirect → https://www.rehearse.co.kr/...
  │
  ├─→ https://www.rehearse.co.kr (프론트 primary)
  │     └─→ 가비아 DNS CNAME → prod CloudFront [신규]
  │          └─→ S3 rehearse-frontend-prod (OAC)
  │
  ├─→ https://api.rehearse.co.kr (백엔드 API)
  │     └─→ 가비아 DNS A → prod EC2 EIP (t4g.small, ARM64)
  │          └─→ Nginx 443 → Spring Boot(prod, :8080)
  │               ├─→ MySQL 8.0 (container, mysql_data_prod)
  │               ├─→ rehearse-videos-prod (S3, v1.0 스키마)
  │               │        └─→ EventBridge rehearse-video-uploaded-prod
  │               │                  ├─→ rehearse-analysis-prod (Lambda)
  │               │                  └─→ rehearse-convert-prod (Lambda)
  │               └─→ ECR rehearse-backend:prod-<sha>
  │
  └─→ (dev 경로: dev.rehearse.co.kr / api-dev.rehearse.co.kr 기존 유지, 변경 없음)
```

**핵심**: 가비아 apex CNAME 미지원 제약을 해결하기 위해 apex는 CloudFront에 직결하지 않고 **EC2 nginx가 301 리디렉션**으로 `www`로 전환한다 (plan-08 참조). www만 CloudFront로 직접 서빙.

### 분리 원칙

| 축 | dev | prod |
|---|---|---|
| 도메인 프론트 (primary) | `dev.rehearse.co.kr` | `www.rehearse.co.kr` (CloudFront), `rehearse.co.kr`은 EC2 nginx가 301 리디렉션 |
| 도메인 API | `api-dev.rehearse.co.kr` | `api.rehearse.co.kr` |
| DNS | 가비아 (기존) | **가비아 유지** (Route53 미사용) |
| EC2 | `54.180.188.135` (기존) | 신규 Elastic IP |
| CloudFront | `d2n8xljv54hfw0` | 신규 distribution |
| S3 videos | `rehearse-videos-dev` | `rehearse-videos-prod` |
| S3 frontend | dev 기존 버킷 | `rehearse-frontend-prod` |
| Lambda | `rehearse-{analysis,convert}-dev` | `rehearse-{analysis,convert}-prod` |
| EventBridge | `rehearse-video-uploaded-dev` | `rehearse-video-uploaded-prod` |
| ECR 태그 | `:latest` | `:prod`, `:prod-<sha>` |
| DB 볼륨 | `mysql_data` | `mysql_data_prod` |
| GitHub Env | `development` | `production` (required reviewer) |
| IAM | dev 자격 | prod 전용 신규 자격 |
| OAuth App | dev client | prod client (신규) |

## Scope

### In
- `backend/src/main/resources/application-prod.yml` 보강 (OAuth/JWT/Flyway/server/multipart)
- AWS 자원 신규 생성: EC2, S3 (videos + frontend), CloudFront, Lambda, EventBridge, ACM 인증서 1장 (us-east-1 `www.rehearse.co.kr`)
- 가비아 DNS 레코드 추가 (apex A / www CNAME / api A) — **Route53 미사용**
- `backend/docker-compose.prod.yml` 신규 작성
- `backend/nginx/nginx.prod.conf` 신규 (domain `api.rehearse.co.kr`)
- `.github/workflows/deploy-prod.yml` 신규
- `lambda/lambda-safe-deploy.sh` 확장 또는 prod 전용 스크립트 분기
- Google/GitHub OAuth 콘솔 prod 등록
- GitHub Environments (`development` / `production`) 분리 및 secrets 재구성
- Flyway baseline 적용 (빈 prod DB 기준)
- 컷오버 스모크 테스트 체크리스트
- Observability/runbook 가이드 문서
- 런칭 전 프론트 보강 (plan-16 BETA 배지 / plan-17 개인정보 처리방침 페이지)

### Out
- **RDS 이관** — 별건 프로젝트 (비용·학습 트레이드오프). 트래픽 임계치 도달 시 재평가
- **Terraform/CDK 도입** — 별건
- **WAF / Shield / GuardDuty** — 별건
- **로그 집계 시스템 (Loki/Datadog/ELK)** — plan-14에서 CloudWatch 수준만 가이드
- **모바일 앱, 결제, 회사별 질문 DB** — `CLAUDE.md` MVP DON'T
- **Blue/Green 2대 구성, 오토스케일링** — 별건
- **`interviewer-video.tsx`의 mp4 하드코딩 자산 문제** — 별건 (전환 후에도 동일 이슈)
- **`application-local.yml` OAuth secret 커밋 이슈** — 별건 후속 조치

## 제약조건 / 환경

### 환경변수 (prod 기준)

| 변수 | 위치 | 값 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | EC2-prod `.env` | `prod` |
| `DB_URL` | EC2-prod `.env` | `jdbc:mysql://db:3306/rehearse?...` (컨테이너 네트워크 내) |
| `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | EC2-prod `.env` | prod 전용 신규 |
| `CLAUDE_API_KEY` | EC2-prod `.env` | prod 전용 키 (Anthropic 콘솔 발급) |
| `OPENAI_API_KEY` | EC2-prod `.env` | prod 전용 키 |
| `INTERNAL_API_KEY` | EC2-prod `.env` + Lambda env | prod 전용 신규 (32+ chars) |
| `JWT_SECRET` | EC2-prod `.env` | prod 전용 신규 (256bit) |
| `FRONTEND_URL` | EC2-prod `.env` | `https://rehearse.co.kr` |
| `CORS_ALLOWED_ORIGINS` | EC2-prod `.env` | `https://rehearse.co.kr,https://www.rehearse.co.kr` |
| `AWS_S3_BUCKET` | EC2-prod `.env` | `rehearse-videos-prod` |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | EC2-prod `.env` | prod OAuth App 신규 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | EC2-prod `.env` | prod OAuth App 신규 |
| `ADMIN_PASSWORD` | EC2-prod `.env` | prod 전용 |
| `VITE_API_URL` | `deploy-prod.yml:build step` | `https://api.rehearse.co.kr` |

### 인프라 제약
- **Region**: `ap-northeast-2` (ACM for CloudFront는 `us-east-1`)
- **ARM64 통일**: EC2 `t4g.small` → `ubuntu-24.04-arm` 빌더에서 single-arch push
- **CloudFront 전파**: Alias/DNS 변경 후 ~15분
- **CORS preflight maxAge**: 3600s (기존 `CorsConfig.java:31`) → 전환 창 최소 1시간 고려
- **ACM us-east-1 validation**: DNS CNAME validation 필요 (가비아 DNS 조작)
- **EC2 IAM Role**: ECR Read, S3(rehearse-videos-prod) R/W, CloudWatch Logs Write
- **certbot 발급**: `api.rehearse.co.kr` A 레코드 전파 후에만 성공

### 보안 요구사항
- **dev/prod IAM 완전 분리** — prod 자격이 dev 자원 접근 금지, 역도 금지
- **OAuth callback 도메인 고정**: `https://api.rehearse.co.kr/login/oauth2/code/{github,google}`
- **Backend `.env` Git 미추적** — EC2 scp 수동 관리. Secret Manager 이관은 별건
- **GitHub Environment `production` required reviewer ≥ 1명** — 자동 배포 방지
- **`gcp-credentials.json`** — EC2 수동 업로드, 리포 미포함

### 비용 상한
- 목표 월 $50 이내 (EC2 t4g.small ~$15 + Elastic IP ~$4 + S3/CloudFront/Lambda 사용량 기반)

## Task 분할

**선행 필수**: 모든 Task는 [`s3-key-schema-redesign`](../s3-key-schema-redesign/progress.md) 완료 후 착수. 특히 Task 3·4는 신규 스키마(v1.0)를 기반으로 S3/Lambda/EventBridge 자원을 구성한다.

| # | Plan 문서 | 태스크 | 태그 | 의존 |
|---|---|---|---|---|
| 1 | `plan-01-application-prod-yml.md` | `application-prod.yml` OAuth/JWT/Flyway/server 설정 보강 | `[parallel]` | — |
| 2 | `plan-02-ec2-provisioning.md` | prod EC2 인스턴스·보안그룹·IAM Role·Elastic IP 생성 | `[parallel]` | — |
| 3 | `plan-03-s3-eventbridge-mediaconvert.md` | `rehearse-videos-prod` S3 (신규 v1.0 스키마) + `interviews/raw/` prefix 기반 EventBridge 규칙 + 5-prefix Lifecycle | `[parallel]` | **선행** s3-key-schema-redesign |
| 4 | `plan-04-lambda-prod-functions.md` | `rehearse-{analysis,convert}-prod` Lambda 함수 (신규 파서 코드 기반) + alias + 배포 스크립트 | `[blocking]` | 3, **선행** s3-key-schema-redesign |
| 5 | `plan-05-ecr-image-tagging.md` | ECR 태그 전략 + Lifecycle Policy | `[parallel]` | — |
| 6 | `plan-06-docker-compose-prod.md` | `docker-compose.prod.yml` + `nginx.prod.conf` 작성 | `[parallel]` | 1 |
| 7 | `plan-07-cloudfront-frontend.md` | `rehearse-frontend-prod` S3 + 신규 CloudFront distribution | `[blocking]` | 8 (ACM) |
| 8 | `plan-08-route53-acm.md` | ACM us-east-1 `www.rehearse.co.kr` 1장 + 가비아 DNS 레코드(apex/www/api) + Let's Encrypt 멀티 도메인(`api`+`apex`) | `[blocking]` | — |
| 9 | `plan-09-oauth-console-registration.md` | Google/GitHub OAuth prod client 등록 | `[parallel]` | — |
| 10 | `plan-10-github-secrets-environments.md` | GitHub Environments `production` + secrets 재구성 | `[parallel]` | 2, 5, 7 |
| 11 | `plan-11-deploy-prod-workflow.md` | `deploy-prod.yml` 작성 (main push + approval gate) | `[blocking]` | 5, 6, 10 |
| 12 | `plan-12-flyway-initial-migration.md` | prod DB 최초 Flyway 기동 + baseline 검증 | `[blocking]` | 2, 6 |
| 13 | `plan-13-cutover-smoke-test.md` | 최초 컷오버 체크리스트 + E2E 스모크 테스트 | `[blocking]` | 1~12 전부 |
| 14 | `plan-14-observability-runbook.md` | CloudWatch 알람 + 일일 점검 체크리스트 + 인시던트 템플릿 | `[parallel]` | — |

**병렬 실행 가능**: Task 1, 2, 3, 5, 8, 9, 14 (초기 단계)
**블로커**: Task 13은 1~12 완료 후. Task 11은 5·6·10 완료 후. Task 7은 8 완료 후. Task 4는 3 완료 후. Task 12는 2·6 완료 후.
