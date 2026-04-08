# Dev 프론트 도메인 `dev.rehearse.co.kr` 재이전 + OAuth https 픽스 — 진행 상황

> **상태: Completed (2026-04-09)**

## 태스크 상태

| # | 태스크 | 상태 | 비고 |
|---|--------|------|------|
| 1 | 현재 인프라/OAuth 상태 조사 + 스냅샷 | ✅ Completed | `survey-snapshot.md` |
| 2 | CloudFront + ACM + DNS 현 상태 공식 확인 + invalidation | ✅ Completed | Distribution `E2FQDE3SA90LO8` |
| 3 | Google OAuth https callback 사전 등록 + JS origins 확인 | ✅ Completed | 사용자 수동 |
| 4 | `application-dev.yml` fallback 정리 + `forward-headers-strategy` 추가 | ✅ Completed | PR #263 머지 |
| 5 | EC2 `.env`에 `FRONTEND_URL` 추가 + 재기동 + OAuth https 검증 | ✅ Completed | |
| 6 | apex 분리 (CloudFront/DNS/CORS/Google) + 문서 완료 처리 | ✅ Completed | |

## 최종 상태 (2026-04-09 기준)

### 인프라
- CloudFront `E2FQDE3SA90LO8` Aliases: **`[dev.rehearse.co.kr]`** (apex 제거됨)
- ACM `536869da...` (SAN: apex + dev) — InUse 유지, prod 구축 시 재활용 가능
- 가비아 DNS: `dev` CNAME + `api-dev` A 유지, **apex A 레코드 제거됨**

### 런타임
- EC2 `~/rehearse/backend/.env`:
  - `FRONTEND_URL=https://dev.rehearse.co.kr` (추가됨)
  - `CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr` (apex 제거됨)
- Spring Boot: `server.forward-headers-strategy=framework` 활성 → OAuth `redirect_uri`가 `https://`로 생성
- Nginx (`api-dev.rehearse.co.kr`): 변경 없음, Let's Encrypt SSL 유지

### OAuth
- **Google OAuth Client**
  - Authorized JavaScript origins: `https://dev.rehearse.co.kr` (apex 제거)
  - Authorized redirect URIs: `https://api-dev.rehearse.co.kr/login/oauth2/code/google` (http 버전 제거)
- **GitHub OAuth App**
  - Authorization callback URL: `https://api-dev.rehearse.co.kr/login/oauth2/code/github`

### 최종 검증 (2026-04-09)
- ✅ `curl https://rehearse.co.kr` → 연결 실패 (완전 분리)
- ✅ `curl https://dev.rehearse.co.kr` → HTTP/2 200
- ✅ SPA 404 fallback (`/nonexistent-route`) → HTTP 200 (CustomErrorResponses 보존)
- ✅ `curl /oauth2/authorization/google` → `redirect_uri=https://api-dev.../login/oauth2/code/google`
- ✅ CORS: dev Origin → Allow, apex Origin → 403 Block

## 진행 로그

### 2026-04-09 (plan-06 완료)
- CloudFront alias에서 apex 제거 (`E3UN6WX5RRO2AG` → update-distribution) → `Deployed` → invalidation `/*` 완료
- EC2 `.env` CORS에서 apex 제거 → 백엔드 force-recreate → 헬스 OK
- 가비아 DNS apex A 레코드 제거 (사용자) → 즉시 전파
- Google OAuth Client apex JS origin + http redirect URI 제거 (사용자)
- 최종 검증 전체 통과
- `docs/plans/cicd/plan-06-custom-domain-https.md` → Completed
- `docs/plans/dev-domain-restore/requirements.md` → Completed

### 2026-04-08 (plan-04, plan-05 실행)
- PR #263 생성 → CI pass → develop 머지 (`782e225..52c7be2`)
- `application-dev.yml` 3줄 변경: frontend-url fallback / cors fallback / forward-headers-strategy
- `backend/.env.example`에 `FRONTEND_URL` 추가
- deploy-dev 워크플로우 success → EC2 새 이미지 배포
- EC2 `~/rehearse/backend/.env`에 `FRONTEND_URL=https://dev.rehearse.co.kr` 추가 + force-recreate
- curl 검증: Google/GitHub redirect_uri 둘 다 https 확인
- GitHub OAuth App callback https 작동 확인 (GitHub 로그인 페이지 정상 반환)

### 2026-04-08 (plan 문서화, v3 개정)
- **OAuth https 마이그레이션을 본 플랜에 통합** (별건 분리 비효율)
- 실측: EC2 SSH 결과 `FRONTEND_URL` 없음 → fallback 사용 중
- 실측: `redirect_uri=http://...` → `forward-headers-strategy` 미설정 원인
- 사용자 확인: Google OAuth Client에 `http://` callback 등록 확인 → `https://` 전환 결정

### 2026-04-08 (v2 개정)
- 실측: `dev.rehearse.co.kr` 이미 CloudFront에서 응답, apex와 병행 상태
- plan-02 역할 축소: "신규 생성" → "현 상태 검증 + invalidation"
- critic 리뷰 반영: CustomErrorResponses 보존, CORS maxAge 1h, 쿠키 host-only, interviewer-video mp4 하드코딩

## 별건 (본 플랜 Out, 후속 처리 필요)

- `backend/docker-compose.yml` `environment:` 섹션에 `FRONTEND_URL` 누락 — 현재는 `application-dev.yml` fallback으로 우연히 동작 중. 한 줄 추가 권장
- `backend/src/main/resources/application-local.yml:7-12` — GitHub/Google OAuth client secret 하드코딩 (커밋 히스토리 노출) → secret 로테이션 + env 분리 필요
- `frontend/src/components/interview/interviewer-video.tsx:8-11` — `dev.rehearse.co.kr/assets/interviewer/*.mp4` 4개 하드코딩, S3에 실제 mp4 파일 부재 → 면접관 비디오 기능 복구 필요
