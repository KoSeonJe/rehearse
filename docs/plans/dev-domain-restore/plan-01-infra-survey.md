# Plan 01: 인프라/OAuth 현재 상태 조사 (read-only)

> 상태: Draft (v2, 2026-04-08 개정)
> 작성일: 2026-04-08

## Why

도메인 전환을 시작하기 전에 **현재 AWS/DNS/OAuth 콘솔 상태**를 정확히 파악해야 한다. CloudFront alternate domain, ACM 인증서 ARN + SAN, 가비아 DNS 레코드, CloudFront CustomErrorResponses, OAuth provider callback/origins는 코드에 없는 정보라서 조사 없이는 되돌리기(rollback) 시 원상복구가 불가능하다. 이 태스크는 모든 조회를 read-only로 수행하여 스냅샷(`survey-snapshot.md`)을 남긴다.

**실측 사전 정보 (2026-04-08)**: `dev.rehearse.co.kr`은 이미 CloudFront `d2n8xljv54hfw0`로 응답 중이고 apex와 같은 배포를 공유한다. 본 태스크는 이 사실을 공식 데이터로 기록하는 것도 포함.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/dev-domain-restore/survey-snapshot.md` | 조사 결과를 기록 (신규 생성) |

## 상세

### 1. CloudFront 배포 상태
```bash
# 배포 ID 조회
aws cloudfront list-distributions \
  --query "DistributionList.Items[?DomainName=='d2n8xljv54hfw0.cloudfront.net'].{Id:Id,Aliases:Aliases.Items,Cert:ViewerCertificate.ACMCertificateArn}"

# 상세 — CustomErrorResponses, DefaultRootObject, Logging 포함
aws cloudfront get-distribution --id <ID> \
  --query "Distribution.{
    Status:Status,
    Aliases:DistributionConfig.Aliases.Items,
    Cert:DistributionConfig.ViewerCertificate.ACMCertificateArn,
    ErrorResponses:DistributionConfig.CustomErrorResponses.Items,
    DefaultRoot:DistributionConfig.DefaultRootObject,
    Logging:DistributionConfig.Logging
  }"
```
**기록 항목**
- Distribution ID, Status
- 현재 Aliases(alternate domain names) 전체 목록
- ViewerCertificate.ACMCertificateArn
- **CustomErrorResponses 전체 내용** — plan-06에서 보존되어야 할 기준점
- DefaultRootObject
- Logging.Enabled — plan-06 안정화 게이트의 apex 요청 모니터링에 필수. `false`라면 별건으로 활성화 결정 필요

### 2. ACM 인증서 상태 (us-east-1)
```bash
aws acm list-certificates --region us-east-1 \
  --query "CertificateSummaryList[?contains(DomainName,'rehearse.co.kr')]"

# 각 인증서 상세
aws acm describe-certificate --region us-east-1 --certificate-arn <ARN> \
  --query "Certificate.{Domain:DomainName,SANs:SubjectAlternativeNames,Status:Status,InUseBy:InUseBy}"
```
**기록 항목**: 기존 인증서 ARN, DomainName/SANs, `dev.rehearse.co.kr` 포함 여부

### 3. 가비아 DNS 레코드
가비아 콘솔 → DNS 관리 → `rehearse.co.kr` → 전체 레코드 스크린샷/표로 기록:

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A / ALIAS | `@` | ? | ? |
| CNAME | `www` | ? | ? |
| CNAME | `dev` | ? | ? |
| A | `api-dev` | ? | ? |
| 기타 ACM 검증 CNAME 등 | ... | ... | ... |

### 4. OAuth provider 콘솔

**Google OAuth Client** (GCP Console → APIs & Services → Credentials → OAuth 2.0 Client IDs)
- Authorized JavaScript origins 목록
- Authorized redirect URIs 목록 (→ `https://api-dev.rehearse.co.kr/login/oauth2/code/google` 존재 확인)

**GitHub OAuth App** (GitHub Settings → Developer settings → OAuth Apps)
- Homepage URL
- Authorization callback URL (→ `https://api-dev.rehearse.co.kr/login/oauth2/code/github` 존재 확인)

### 5. EC2 런타임 환경 확인
```bash
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135 \
  "grep -E '^(FRONTEND_URL|CORS_ALLOWED_ORIGINS|SPRING_PROFILES_ACTIVE)=' ~/rehearse/backend/.env"
```
**기록 항목**: 현재 `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE` 값

### 6. 스냅샷 파일 작성
위 모든 결과를 `docs/plans/dev-domain-restore/survey-snapshot.md`에 기록. 이 파일은 **rollback 기준점**이자 plan-02/03/06의 입력이 된다.

## 담당 에이전트

- Implement: `devops-engineer` — AWS CLI / 콘솔 조회, 스냅샷 작성
- Review: `architect-reviewer` — 조사 누락 항목이 없는지, rollback 가능한 수준으로 기록되었는지 검증

## 검증

- [ ] `survey-snapshot.md`에 6개 섹션(CloudFront / ACM / DNS / Google OAuth / GitHub OAuth / EC2 .env) 모두 기록
- [ ] CloudFront Distribution ID, 현재 Aliases, ACM ARN이 명시됨
- [ ] **CustomErrorResponses 원본이 스냅샷에 기록됨** (plan-06 비교용 기준점)
- [ ] **CloudFront access Logging 활성화 여부** 기록 (plan-06 안정화 게이트 전제)
- [ ] ACM SAN에 apex와 `dev.rehearse.co.kr`이 포함되는지 명시
- [ ] OAuth callback URL에 `api-dev.rehearse.co.kr`이 이미 등록되어 있는지 확인
- [ ] Google OAuth Authorized JS origins에 dev가 이미 있는지 확인 (있다면 plan-03는 "확인만"으로 스킵)
- [ ] EC2 `.env`의 현재 `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` 값 원본 기록 (롤백용)
- [ ] `progress.md` Task 1 → Completed
