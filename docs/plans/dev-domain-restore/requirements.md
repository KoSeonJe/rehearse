# Dev 프론트 도메인 `dev.rehearse.co.kr` 재이전 + OAuth https 픽스 — 요구사항 정의

> 상태: Draft (v3, 2026-04-08 개정)
> 작성일: 2026-04-08
> 선행 문서: `docs/plans/cicd/plan-06-custom-domain-https.md`

## Why

현재 dev 환경의 프론트엔드는 CloudFront 배포 `d2n8xljv54hfw0`가 서빙하고 있으며, **이 배포에는 `rehearse.co.kr`(apex)과 `dev.rehearse.co.kr`(서브도메인)이 이미 둘 다 alternate domain으로 등록되어 있다**. DNS/CNAME도 양쪽 모두 같은 CloudFront로 가리키고 있다 (2026-04-08 실측).

문제는 **백엔드 `FRONTEND_URL`이 apex를 기본값으로 사용**하고 있어 OAuth 로그인 성공 시 사용자가 apex로 리다이렉트된다는 점이다. 곧 **별도의 prod 서버/배포를 신규로 띄울 예정**인데, apex가 dev 트래픽을 실사용하고 있으면 prod 구축 시 apex를 자유롭게 재할당할 수 없다.

따라서 **runtime `FRONTEND_URL`을 dev로 전환한 뒤 apex를 CloudFront alternate에서 제거**해 prod용으로 회수하는 것이 본 작업의 핵심이다. 인프라 측 준비는 이미 대부분 되어 있으므로 리스크는 낮은 편이지만, CORS / OAuth 화이트리스트 / JWT 쿠키(host-only) 같은 세부 사항이 얽혀 있어 순서를 정확히 지켜야 한다.

### Decision Framework

**1. Why?**
곧 프로덕션 서버를 신규 구축할 예정. 현재 apex(`rehearse.co.kr`)가 dev 배포로 향하고 있어 prod용으로 회수할 수 없다. dev 진입점을 서브도메인(`dev.rehearse.co.kr`)으로 "고정 선언"하고 apex 연결을 걷어내야 prod 구축 시 apex를 자유롭게 할당할 수 있다.

**2. Goal**
- 프론트 진입점(runtime redirect 기준): `https://rehearse.co.kr` → `https://dev.rehearse.co.kr`
- 백엔드 API: `https://api-dev.rehearse.co.kr` (변경 없음)
- GitHub / Google OAuth 로그인이 `dev.rehearse.co.kr`에서 정상 동작
- apex(`rehearse.co.kr`)는 CloudFront alternate domain / 가비아 DNS / Google OAuth JS origins / 백엔드 CORS에서 완전히 분리되어 prod용으로 비워짐
- 전환 중 서비스 사용자에게 에러 페이지 노출 0건 (기존 로그인 세션의 host-only 쿠키는 무효화됨 — 후술)

**성공 기준**
- `curl -I https://dev.rehearse.co.kr` → 200 OK + CloudFront 헤더 (이미 충족)
- `dev.rehearse.co.kr`에서 GitHub/Google 로그인 → JWT 쿠키 세팅 + 메인 리다이렉트 성공
- 브라우저 DevTools Network 탭에 apex 요청 0건 (SPA 내부 링크가 전부 상대경로인지 확인)
- `curl -I https://rehearse.co.kr` → CloudFront에서 해당 alias를 제거한 후 NXDOMAIN / 미연결 / 또는 prod 준비 상태

**3. Evidence (2026-04-08 실측)**

#### 코드
- `backend/src/main/resources/application-dev.yml:60` — `frontend-url: ${FRONTEND_URL:https://rehearse.co.kr}` (apex 기본값)
- `backend/src/main/resources/application-dev.yml:63` — `allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}` (localhost만)
- `backend/docker-compose.yml:27` — `SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}` (dev 프로파일 활성)
- `.github/workflows/deploy-dev.yml:104` — `VITE_API_URL: https://api-dev.rehearse.co.kr` (이미 올바름)
- `frontend/src/components/ui/login-modal.tsx:25-30` — OAuth 진입점은 `${VITE_API_URL}/oauth2/authorization/{provider}`
- `backend/.../OAuth2SuccessHandler.java:23,46-58` — OAuth 성공 시 `${app.frontend-url}`로 리다이렉트
- `backend/.../OAuth2SuccessHandler.java:38-43` — JWT 쿠키가 **host-only**(Domain 미설정), `SameSite=Lax`, `Secure` dynamic → apex와 dev 쿠키 jar 분리, 자동 이전 불가
- `backend/.../CorsConfig.java:31` — `setMaxAge(3600L)` = **CORS preflight 캐시 1시간**
- `frontend/src/lib/api-client.ts`, `hooks/use-tts.ts`, `hooks/use-interviews.ts`, `login-modal.tsx` — 전부 `VITE_API_URL` 기반, 하드코딩 없음
- ⚠️ `frontend/src/components/interview/interviewer-video.tsx:8-11` — **`https://dev.rehearse.co.kr/assets/interviewer/{mood}.mp4` 4개 하드코딩**. 현재 S3에 해당 파일이 없어 SPA `index.html`로 404 fallback됨 (`x-cache: Error from cloudfront` + `content-type: text/html`). 즉 **면접관 비디오 기능은 현재 작동하지 않는 상태** — 본 플랜 범위 밖이지만 도메인 하드코딩이 존재한다는 사실은 문서화 필요

#### EC2 런타임 (실측, SSH 조회)
- `~/rehearse/backend/.env`에 `SPRING_PROFILES_ACTIVE=dev`, `CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr,https://rehearse.co.kr` 존재
- **`FRONTEND_URL`이 `.env`에 없음** → `application-dev.yml:60`의 fallback `https://rehearse.co.kr`이 실제 사용 중 → OAuth 성공 시 apex로 리다이렉트
- 즉 "남은 작업"의 핵심은 EC2 `.env`에 `FRONTEND_URL=https://dev.rehearse.co.kr` **추가**

#### OAuth https 이슈 (실측, curl)
- `curl /oauth2/authorization/google` → `Location: ...&redirect_uri=http://api-dev.rehearse.co.kr/login/oauth2/code/google` (HTTP)
- 원인: `backend/nginx/nginx.conf:36` `proxy_set_header X-Forwarded-Proto $scheme` ✅ 있음, but `application-dev.yml`에 `server.forward-headers-strategy` 미설정 → Spring이 X-Forwarded-Proto 무시
- 현재 Google OAuth Client에 `http://` callback이 등록돼 있어 작동 중 (사용자 확인)
- 보안 경계: 평문 callback에 인증 코드 노출 → MITM 위험. Google이 단계적 폐기 중
- 픽스: `application-dev.yml`에 `server.forward-headers-strategy: framework` 1줄 추가 (plan-04에 통합)

#### 인프라 실측
- `dig dev.rehearse.co.kr` → CNAME `d2n8xljv54hfw0.cloudfront.net` (TTL 929s) — **이미 존재**
- `dig rehearse.co.kr` → 같은 CloudFront 배포 IP로 해석 — **이미 존재**
- `curl -I https://dev.rehearse.co.kr` → HTTP/2 200 + CloudFront 헤더 — **이미 정상 응답**
- 즉, CloudFront `d2n8xljv54hfw0`에는 **apex와 dev가 모두 alternate domain으로 등록돼 있고**, ACM 인증서도 두 도메인을 모두 커버하는 상태여야만 이런 응답이 가능
- 결론: **"새 도메인 추가"는 이미 완료된 상태**. 남은 일은 (a) 백엔드가 dev로 리다이렉트하도록 설정 전환, (b) apex를 걷어내는 정리 작업뿐

**4. Trade-offs**
- **prod 프로파일(`application-prod.yml`)은 손대지 않음** — 추후 신규 prod 서버 구축 시 그대로 재사용
- CloudFront 배포는 **기존 것을 그대로 사용**하고 alias만 축소 → 다운타임·ACM 재발급·OAuth 재등록 전부 불필요
- 전환 중 CORS는 apex + dev 병행 허용 → 전환 직후 apex 접속자도 깨지지 않음. plan-06에서 축소
- OAuth callback 도메인은 백엔드(`api-dev.rehearse.co.kr`)라서 **provider 콘솔 callback URL은 불변** → 리스크 대폭 감소
- **기존 apex 로그인 세션은 무효화된다** (host-only 쿠키). 대안은 쿠키 Domain을 `.rehearse.co.kr`로 확장하는 것이지만 보안 경계가 넓어져 기각. 강제 재로그인은 수용 가능한 비용으로 판단

## 목표

위 "Goal" 참조.

## 아키텍처 / 설계

### 현재 (2026-04-08 실측)
```
사용자 브라우저
   │
   ├─→ https://rehearse.co.kr (apex) ──┐
   │                                   ├──→ CloudFront d2n8xljv54hfw0 ──→ S3 (frontend SPA)
   ├─→ https://dev.rehearse.co.kr ─────┘       (alternate domain: apex + dev 둘 다 등록)
   │
   └─→ https://api-dev.rehearse.co.kr ──→ Nginx (EC2 54.180.188.135) ──→ Spring Boot
                                                 │
                                                 └─→ OAuth2SuccessHandler
                                                        redirects to FRONTEND_URL (=apex, 문제)
```

### 목표 (After)
```
사용자 브라우저
   │
   └─→ https://dev.rehearse.co.kr ──→ CloudFront d2n8xljv54hfw0 ──→ S3 (frontend SPA)
                                          (alternate domain: dev만)
   │
   └─→ https://api-dev.rehearse.co.kr ──→ (변경 없음)
                                                 │
                                                 └─→ OAuth2SuccessHandler
                                                        redirects to FRONTEND_URL (=dev.rehearse.co.kr)

(apex `rehearse.co.kr`는 DNS / CloudFront / OAuth / CORS 어디에도 등록 없음 → prod용으로 회수 가능)
```

### 전환 전략 — "전환 → 안정화 → 분리"
기존 plan v1은 "widen → switch → narrow"였지만, 인프라가 이미 "widen"된 상태라 실제 단계는 다음과 같다:

1. **전환(Switch)**: 백엔드 `FRONTEND_URL`을 dev로 교체 + CORS 병행 허용 → 이후 로그인은 dev로, apex 접속자 API 호출도 깨지지 않음
2. **안정화(Stabilize)**: 최소 CORS maxAge 1시간 + 권장 24시간 동안 apex 요청이 실사용자로부터 0에 수렴하는지 CloudFront 로그로 확인
3. **분리(Narrow)**: CloudFront alternate에서 apex 제거, 가비아 DNS에서 apex 제거, Google OAuth JS origins에서 apex 제거, EC2 CORS에서 apex 제거

## Scope

### In
- `backend/src/main/resources/application-dev.yml` 기본값 수정 (`frontend-url`, `cors.allowed-origins`) + **`server.forward-headers-strategy: framework` 추가**
- `backend/.env.example` 일관성 정리
- EC2 `~/rehearse/backend/.env`에 `FRONTEND_URL=https://dev.rehearse.co.kr` 추가
- **기존** CloudFront 배포 `d2n8xljv54hfw0`의 alternate domain에서 apex 제거 (dev는 유지)
- 가비아 DNS에서 apex 레코드 제거 (`dev` CNAME은 유지)
- Google OAuth Client:
  - Authorized JavaScript origins에서 apex 제거 (dev는 유지/확인)
  - **Authorized redirect URIs에 `https://` callback 추가 (사전) → `http://` callback 제거 (사후)**
- **GitHub OAuth App callback URL을 `http://` → `https://`로 교체**
- EC2 CORS에서 전환 중 병행 허용한 apex 제거
- CloudFront cache invalidation (`/*`) — alias 축소 후 1회
- `docs/plans/cicd/plan-06-custom-domain-https.md` 상태 → `Completed`

### Out
- `application-prod.yml` (신규 prod 서버용으로 보존)
- `application-local.yml` (localhost 전용, 하지만 `L7-12` OAuth secret 하드코딩 이슈는 별건으로 후속 처리 필요)
- 백엔드 Nginx / Certbot / EC2 인프라 — `api-dev.rehearse.co.kr` 그대로
- `.github/workflows/deploy-dev.yml` — `VITE_API_URL` 이미 정답
- Lambda 함수 — env 기반, 도메인 하드코딩 없음
- **신규 prod 서버 구축** — 후속 프로젝트
- **`interviewer-video.tsx`의 mp4 하드코딩 + S3 asset 부재** — 별건. 본 플랜은 도메인 전환만 다루고, 이 파일의 URL은 전환 후에도 그대로 `dev.rehearse.co.kr`을 가리키므로 자연히 정답이 된다. 다만 S3에 실제 파일이 없는 건 별건 이슈
- **새 ACM 인증서 발급 / DNS CNAME 신규 생성** — 이미 존재하므로 불필요

## 제약조건 / 환경

### 환경변수 (dev 기준)
| 변수 | 위치 | Before | After |
|---|---|---|---|
| `FRONTEND_URL` | EC2 `~/rehearse/backend/.env` | `https://rehearse.co.kr`(또는 unset → yml 기본값 apex) | `https://dev.rehearse.co.kr` |
| `CORS_ALLOWED_ORIGINS` | EC2 `~/rehearse/backend/.env` | `https://rehearse.co.kr` | `https://dev.rehearse.co.kr,https://rehearse.co.kr`(전환 중) → `https://dev.rehearse.co.kr`(분리 후) |
| `VITE_API_URL` | `.github/workflows/deploy-dev.yml:104` | `https://api-dev.rehearse.co.kr` | 동일 (변경 없음) |

### 인프라 제약
- CloudFront distribution `d2n8xljv54hfw0.cloudfront.net` — alias 수정 시 ~15분 전파
- CloudFront CustomErrorResponses (403/404 → `/index.html` 200) 가 SPA 라우팅에 필수 → alias 수정 시 **드롭되지 않도록 확인**
- ACM 인증서는 us-east-1 리전 (이미 존재, SAN 변경 불필요)
- 가비아 DNS TTL 3600 → apex 제거 후 전파에 최대 1시간
- EC2: `ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135`
- CORS preflight maxAge = **3600s (1시간)** → plan-06의 안정화 게이트는 최소 1시간 이상 필수, 24시간 권장

### 보안 요구사항
- **OAuth callback 도메인(`api-dev.rehearse.co.kr`)은 절대 변경 금지**
- JWT 쿠키는 host-only → 전환 후 apex 세션은 무효화, dev에서 재로그인 필요 (문서화 필수)
- 커밋 히스토리에 노출된 `application-local.yml`의 OAuth secret은 별건 후속 처리

## Task 분할

| # | Plan 문서 | 태스크 | 의존 |
|---|---|---|---|
| 1 | `plan-01-infra-survey.md` | 현재 CloudFront alias / ACM SAN / DNS / OAuth / EC2 .env 스냅샷 확보 (CustomErrorResponses 포함) | — |
| 2 | `plan-02-cloudfront-verify.md` | CloudFront·ACM·DNS가 이미 dev를 커버함을 공식 확인, invalidation 집행 | 1 |
| 3 | `plan-03-oauth-console.md` | Google/GitHub OAuth 콘솔 상태 확인 + 필요 시 dev origin 추가 | 1 |
| 4 | `plan-04-backend-config.md` | `application-dev.yml` 기본값 변경 + PR 머지 | 1 |
| 5 | `plan-05-ec2-runtime-switch.md` | EC2 `.env` 교체 + 백엔드 재기동 + E2E 검증 (면접관 비디오 제외) | 2, 3, 4 |
| 6 | `plan-06-apex-decommission.md` | CloudFront/DNS/OAuth/CORS에서 apex 제거 + invalidation + 문서 완료 | 5 (최소 24h 안정화) |

Task 2·3·4는 병렬 가능 (`[parallel]`). Task 5는 `[blocking]`. Task 6은 Task 5 완료 후 최소 CORS maxAge(1h) 경과 — 권장 24h.

## 변경 이력
- **v1 (2026-04-08 초안)** — 인프라 준비가 안 된 상태를 전제로 6단계 작성
- **v2 (2026-04-08 개정)** — 실측 결과 CloudFront/DNS/ACM가 이미 dev를 커버함을 확인. plan-02 역할이 "신규 생성"에서 "현 상태 검증 + invalidation"으로 축소. 면접관 비디오 mp4 하드코딩 / S3 부재 / 쿠키 host-only / CORS maxAge 1h 등 critic 리뷰 지적사항 반영
- **v3 (2026-04-08 개정)** — EC2 SSH 실측으로 `FRONTEND_URL`이 `.env`에 없어 fallback이 사용 중임 확인 / OAuth `redirect_uri`가 `http://`로 생성되는 이슈 발견 (`X-Forwarded-Proto` 무시). plan-04에 `server.forward-headers-strategy: framework` 추가, plan-03에 Google https callback 사전 등록 단계 추가, plan-05에 GitHub OAuth callback 즉시 교체 단계 추가, plan-06에 Google http callback 제거 단계 추가
