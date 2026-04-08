# Plan 06: apex(`rehearse.co.kr`) 분리 및 문서 완료 처리

> 상태: Draft (v2, 2026-04-08 개정)
> 작성일: 2026-04-08
> 의존: plan-05 완료 후 최소 **1시간**(CORS preflight maxAge) 권장 **24시간** 안정화 기간

## Why

plan-05까지 완료되면 dev 프론트는 `dev.rehearse.co.kr`로 정상 동작하지만, apex(`rehearse.co.kr`)는 여전히 CloudFront/DNS/OAuth/CORS에 참조된 채 남아 있다. 이 상태로는 **신규 prod 서버를 구축할 때 apex를 재할당할 수 없다**. 이 태스크는 dev와 관련된 모든 apex 참조를 걷어내 prod용으로 회수하는 마지막 단계다.

**안정화 기간을 두는 이유**
- CORS preflight maxAge = 3600s (`CorsConfig.java:31`): 브라우저가 apex origin에 대한 preflight 응답을 최대 1시간 캐시. apex를 CORS에서 너무 빨리 제거하면 이 캐시가 만료되기 전까지는 ALLOW가 유지되므로 결과적으로 문제 없지만, 제거 이후의 실제 차단 효과는 1h+ 후에 확정됨
- 실제 사용자 트래픽에서 apex 참조가 없음을 CloudFront 로그로 확인하는 데는 최소 1 비즈니스데이 권장
- **하드 게이트: plan-05 완료 시각 + 1h**, **권장 게이트: +24h**

## 생성/수정 파일

| 리소스 / 파일 | 작업 |
|------|------|
| CloudFront Distribution `d2n8xljv54hfw0` | Alternate domain names에서 `rehearse.co.kr`, `www.rehearse.co.kr`(있다면) 제거 |
| 가비아 DNS | apex `@` A/ALIAS 레코드 제거 (또는 prod 구축 대기 상태로 비워둠) |
| ACM (us-east-1) | apex 포함 인증서는 **유지** (prod용으로 재활용) |
| EC2 `~/rehearse/backend/.env` | `CORS_ALLOWED_ORIGINS`에서 `https://rehearse.co.kr` 제거 |
| Google OAuth Client | Authorized JavaScript origins에서 apex 제거 |
| GitHub OAuth App | Homepage URL 정리 (선택) |
| `docs/plans/cicd/plan-06-custom-domain-https.md` | 상태 → `Completed` |
| `docs/plans/dev-domain-restore/requirements.md` | 상태 → `Completed` |
| `docs/plans/dev-domain-restore/progress.md` | 모든 태스크 Completed, 최종 로그 추가 |

## 상세

### 0. 안정화 확인 (Gate)
```bash
# 필수 게이트: plan-05 완료 후 최소 1시간 경과 (CORS preflight maxAge=3600s)
# 권장 게이트: 24시간 경과 + CloudFront access log 확인

# CloudFront access log 활성화 여부 확인 (plan-01 스냅샷 기준)
# 활성화되어 있다면 최근 1h/24h 기준 apex Host 헤더 요청 비율 확인
# 판단 기준: 실사용자 트래픽이 전체 요청 중 < 1%
```
apex로 여전히 의미 있는 트래픽이 있으면, 한시적 apex→dev 301 리다이렉트(별도 plan) 선행 여부 결정. 없음이 확인되면 아래 진행.

### 1. CloudFront alternate domain names 제거 — **콘솔 권장**
- CloudFront → Distribution `d2n8xljv54hfw0` → General 탭 → Edit
- Alternate domain names에서 `rehearse.co.kr`, `www.rehearse.co.kr`(등록된 경우) **제거**
- **CustomErrorResponses(403/404 → /index.html) 설정은 건드리지 않음** — plan-01/02 스냅샷과 비교해 Save 후에도 동일한지 확인
- ViewerCertificate는 SAN에 apex가 포함된 인증서를 그대로 유지해도 무방 (CloudFront는 등록된 Alias만 검증). 인증서 교체 불필요
- Save → `aws cloudfront wait distribution-deployed --id $DIST_ID`

**CLI 사용을 피하는 이유**: `aws cloudfront update-distribution`은 전체 `DistributionConfig` 객체를 요구하고, 실수로 `CustomErrorResponses`나 캐시 정책을 드롭할 위험이 있음. alias 한두 개 제거는 콘솔이 더 안전.

### 1-bis. 제거 직후 invalidation
```bash
aws cloudfront create-invalidation --distribution-id $DIST_ID --paths "/*"
```

### 2. 가비아 DNS apex 제거
- `@` (apex) A/ALIAS 레코드 **삭제**
- `www` CNAME이 있었다면 같이 삭제
- `dev` CNAME → CloudFront는 **유지**
- `api-dev` A 레코드 → **유지** (백엔드)
- 추후 prod 구축 시 apex에 새 CloudFront 배포 또는 ALB를 할당할 예정

### 3. ACM 인증서 정리
- apex가 포함된 기존 인증서는 **삭제하지 말 것** — prod 서버 구축 시 SAN을 확장하거나 그대로 재사용
- dev 전용 인증서와 apex 인증서가 분리돼 있다면, apex 인증서는 `InUseBy`가 비어도 삭제 보류
- 인증서 태그/이름에 `reserved-for-prod` 같은 메모 추가 권장

### 4. EC2 CORS 정리
```bash
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135
cd ~/rehearse/backend

cp .env .env.bak-$(date +%Y%m%d-%H%M%S)
# .env 수정
# Before: CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr,https://rehearse.co.kr
# After:  CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr

vi .env
docker compose --env-file .env up -d backend
```
헬스체크 통과 확인 후:
```bash
curl -i -X OPTIONS https://api-dev.rehearse.co.kr/api/interviews \
  -H "Origin: https://rehearse.co.kr" \
  -H "Access-Control-Request-Method: GET"
# 기대: CORS 차단 (Access-Control-Allow-Origin 헤더 없음)

curl -i -X OPTIONS https://api-dev.rehearse.co.kr/api/interviews \
  -H "Origin: https://dev.rehearse.co.kr" \
  -H "Access-Control-Request-Method: GET"
# 기대: Access-Control-Allow-Origin: https://dev.rehearse.co.kr
```

### 5. Google OAuth Client 정리
**Authorized JavaScript origins**
- `https://rehearse.co.kr`(apex) 제거
- `https://dev.rehearse.co.kr`만 남김

**Authorized redirect URIs** — http 버전 제거
- `http://api-dev.rehearse.co.kr/login/oauth2/code/google` **제거** (plan-04 배포 이후 더 이상 사용되지 않음)
- `https://api-dev.rehearse.co.kr/login/oauth2/code/google` **유지** (현재 사용 중)

저장 후 `dev.rehearse.co.kr`에서 Google 로그인 재확인 — 정상이어야 함.

### 6. GitHub OAuth App 정리
- Homepage URL이 `https://dev.rehearse.co.kr`인지 확인 (선택)
- Authorization callback URL은 plan-05 Step 3-ter에서 이미 `https://api-dev.rehearse.co.kr/login/oauth2/code/github`로 교체됨 → 변경 없음, 확인만

### 7. 최종 검증 — apex가 완전히 분리되었는지
```bash
# DNS — SOA/NS는 남아도 OK, A/ALIAS/CNAME은 없어야 함
dig rehearse.co.kr A
dig rehearse.co.kr CNAME
# 기대: answer section에 A/CNAME 레코드 없음

# HTTPS
curl -I --max-time 5 https://rehearse.co.kr
# 기대: Could not resolve host / 연결 실패 (prod 미구축 상태)

# 프론트
# 브라우저에서 https://dev.rehearse.co.kr 접속 → 모든 기능 정상
# DevTools Network에 rehearse.co.kr 요청 0건
# CustomErrorResponses 동작 확인: https://dev.rehearse.co.kr/nonexistent-route → SPA 404 렌더(200 + index.html)
```

### 8. 문서 갱신
- `docs/plans/cicd/plan-06-custom-domain-https.md` 상단 상태 헤더 `In Progress` → `Completed`, 완료일 기록
- `docs/plans/dev-domain-restore/requirements.md` 상태 `Draft` → `Completed`
- `docs/plans/dev-domain-restore/progress.md`
  - Task 1~6 모두 Completed
  - 진행 로그에 실제 수행일/발견된 이슈/롤백 발생 여부 기록
- 선행 문서인 `plan-06-custom-domain-https.md`의 작업 내용은 이미 반영된 상태이므로 `Completed`로 마감

### 9. 커밋 (문서 변경만)
```
docs: dev 프론트 도메인 dev.rehearse.co.kr 재이전 완료 처리

plan-06-custom-domain-https 및 dev-domain-restore 상태를 Completed로 갱신.
apex는 향후 신규 prod 서버용으로 예약.

Constraint: apex 인증서/DNS 슬롯은 prod용으로 예약 — dev 리소스에서 재참조 금지
Confidence: high
Scope-risk: narrow
```

## 담당 에이전트

- Implement: `devops-engineer` — CloudFront/DNS/OAuth 콘솔 정리, EC2 CORS 축소
- Implement: `writer` — 문서 상태 갱신, progress 로그 작성
- Review: `qa` — 최종 E2E 검증, apex 분리 완전성 확인
- Review: `architect-reviewer` — prod 회수 가능성 검증 (apex 슬롯이 깨끗하게 비었는지)

## 검증

- [ ] `dig rehearse.co.kr A/CNAME` → answer section에 레코드 없음 (SOA/NS는 무관)
- [ ] CloudFront Alternate domain names에 apex 없음
- [ ] 가비아 DNS에 apex 레코드 없음 (dev, api-dev는 유지)
- [ ] ACM apex 인증서는 prod용으로 보관 (삭제 금지)
- [ ] EC2 `.env`의 CORS에서 apex 제거됨, 백엔드 재기동 후 CORS 차단 확인
- [ ] Google OAuth origins에 apex 없음
- [ ] Google OAuth redirect URIs에서 `http://` 버전 제거됨, `https://`만 존재
- [ ] GitHub OAuth callback URL이 `https://` (plan-05에서 교체 완료)
- [ ] `dev.rehearse.co.kr`에서 전체 E2E(로그인/인터뷰/분석/피드백) 정상
- [ ] CustomErrorResponses 동작 확인: `dev.rehearse.co.kr/<존재하지않는경로>` → SPA 렌더
- [ ] DevTools Network에 apex 요청 0건
- [ ] 모든 관련 문서(`plan-06-custom-domain-https.md`, `dev-domain-restore/requirements.md`, `progress.md`) 상태 Completed
- [ ] `progress.md` Task 6 → Completed + 전체 프로젝트 완료 선언
